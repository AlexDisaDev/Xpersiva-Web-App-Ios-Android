import { useCallback, useEffect, useRef, useState } from 'react';
import type { JoinTarget } from './joinLink';
import { socketUrl } from './joinLink';
import type { Profile } from './profile';

/**
 * Conexión con el plugin `websocket` del juego.
 *
 * Protocolo (el mismo que el mando web):
 *   app → juego  {"type":"hello","name","code","email?"}
 *   juego → app  {"type":"joined","id",...}
 *   app → juego  {"type":"input","steer","throttle","brake"}  hasta 30 veces por segundo
 */
export type ControllerInput = { steer: number; throttle: number; brake: number };
export type SocketStatus = 'connecting' | 'connected' | 'joined' | 'retrying';

const SEND_INTERVAL_MS = 33;
const RETRY_MS = 1500;

export function useGameSocket(target: JoinTarget, profile: Profile) {
  const [status, setStatus] = useState<SocketStatus>('connecting');
  const [playerId, setPlayerId] = useState<string | null>(null);
  const [lastMessage, setLastMessage] = useState<string>('');

  const ws = useRef<WebSocket | null>(null);
  const input = useRef<ControllerInput>({ steer: 0, throttle: 0, brake: 0 });

  useEffect(() => {
    let alive = true;
    let retry: ReturnType<typeof setTimeout> | undefined;

    const connect = () => {
      setStatus((s) => (s === 'retrying' ? s : 'connecting'));
      const socket = new WebSocket(socketUrl(target));
      ws.current = socket;

      socket.onopen = () => {
        setStatus('connected');
        socket.send(
          JSON.stringify({ type: 'hello', name: profile.name, code: target.code, email: profile.email }),
        );
      };
      socket.onmessage = (event) => {
        const text = String(event.data);
        setLastMessage(text);
        try {
          const msg = JSON.parse(text);
          if (msg?.type === 'joined') {
            setPlayerId(String(msg.id ?? ''));
            setStatus('joined');
          }
        } catch {
          // Texto plano del juego: lo dejamos en lastMessage.
        }
      };
      socket.onclose = () => {
        if (!alive) return;
        setPlayerId(null);
        setStatus('retrying');
        retry = setTimeout(connect, RETRY_MS);
      };
      socket.onerror = () => socket.close();
    };

    connect();

    // Solo mandamos cuando algo cambió, igual que el mando web.
    let lastSent = '';
    const timer = setInterval(() => {
      const socket = ws.current;
      if (!socket || socket.readyState !== WebSocket.OPEN) return;
      const payload = JSON.stringify({ type: 'input', ...input.current });
      if (payload !== lastSent) {
        lastSent = payload;
        socket.send(payload);
      }
    }, SEND_INTERVAL_MS);

    return () => {
      alive = false;
      clearTimeout(retry);
      clearInterval(timer);
      ws.current?.close();
    };
  }, [target.host, target.code, target.secure, profile.name, profile.email]);

  const setInput = useCallback((patch: Partial<ControllerInput>) => {
    input.current = { ...input.current, ...patch };
  }, []);

  return { status, playerId, lastMessage, setInput };
}

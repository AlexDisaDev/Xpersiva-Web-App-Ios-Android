import * as Haptics from 'expo-haptics';
import { router, useLocalSearchParams } from 'expo-router';
import { type ComponentRef, useEffect, useRef, useState } from 'react';
import { type GestureResponderEvent, type LayoutRectangle, StyleSheet, Text, View } from 'react-native';
import { type JoinTarget, makeTarget } from '../lib/joinLink';
import { type Profile, loadProfile } from '../lib/profile';
import { colors } from '../lib/theme';
import { Button, Screen } from '../lib/ui';
import { type SocketStatus, useGameSocket } from '../lib/useGameSocket';

const STATUS_TEXT: Record<SocketStatus, string> = {
  connecting: 'conectando…',
  connected: 'conectado, esperando a la experiencia…',
  joined: 'dentro',
  retrying: 'sin conexión, reintentando…',
};

export default function Play() {
  const { code, h, s } = useLocalSearchParams<{ code: string; h: string; s?: string }>();
  const target = makeTarget(String(code ?? ''), h ? String(h) : null, s === '1');
  const [profile, setProfile] = useState<Profile | null>(null);

  useEffect(() => {
    loadProfile().then(setProfile);
  }, []);

  if (!target || !profile) return <Screen>{null}</Screen>;
  return <Controller target={target} profile={profile} />;
}

type Zone = 'wheel' | 'brake' | 'gas';

/**
 * Mando multitáctil: toda la zona es un único responder y cada dedo se
 * asigna a la zona que pisa, así se puede girar y acelerar a la vez.
 */
function Controller({ target, profile }: { target: JoinTarget; profile: Profile }) {
  const { status, setInput } = useGameSocket(target, profile);
  const [steer, setSteer] = useState(0);
  const [held, setHeld] = useState({ brake: false, gas: false });

  const area = useRef<ComponentRef<typeof View>>(null);
  const origin = useRef({ x: 0, y: 0 });
  const zones = useRef<Partial<Record<Zone, LayoutRectangle>>>({});
  const pedalRow = useRef({ x: 0, y: 0 });
  const prevHeld = useRef(held);

  const measureArea = () =>
    area.current?.measureInWindow((x, y) => {
      origin.current = { x, y };
    });
  const onZoneLayout = (zone: Zone) => (e: { nativeEvent: { layout: LayoutRectangle } }) => {
    zones.current[zone] = e.nativeEvent.layout;
    measureArea();
  };

  const zoneAt = (x: number, y: number): Zone | null => {
    for (const z of ['wheel', 'brake', 'gas'] as Zone[]) {
      const local = zones.current[z];
      if (!local) continue;
      // Los pedales miden su layout respecto a su fila: lo pasamos a la zona táctil.
      const off = z === 'wheel' ? { x: 0, y: 0 } : pedalRow.current;
      const r = { ...local, x: local.x + off.x, y: local.y + off.y };
      if (x >= r.x && x <= r.x + r.width && y >= r.y && y <= r.y + r.height) return z;
    }
    return null;
  };

  const update = (touches: GestureResponderEvent['nativeEvent']['touches']) => {
    let nextSteer = 0;
    let steering = false;
    const next = { brake: false, gas: false };

    for (const t of touches) {
      const x = t.pageX - origin.current.x;
      const y = t.pageY - origin.current.y;
      const z = zoneAt(x, y);
      if (z === 'wheel' && !steering) {
        const r = zones.current.wheel!;
        nextSteer = Math.max(-1, Math.min(1, ((x - r.x) / r.width) * 2 - 1));
        steering = true;
      } else if (z === 'brake') next.brake = true;
      else if (z === 'gas') next.gas = true;
    }

    if ((next.gas && !prevHeld.current.gas) || (next.brake && !prevHeld.current.brake)) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
    prevHeld.current = next;
    setSteer(nextSteer);
    setHeld(next);
    setInput({ steer: nextSteer, throttle: next.gas ? 1 : 0, brake: next.brake ? 1 : 0 });
  };

  const onTouches = (e: GestureResponderEvent) => update(e.nativeEvent.touches);
  const onAllUp = () => update([]);

  return (
    <Screen>
      <View style={styles.header}>
        <View style={[styles.dot, status === 'joined' && { backgroundColor: colors.ok }]} />
        <Text style={styles.status}>
          {profile.name} · {STATUS_TEXT[status]}
        </Text>
      </View>

      <View
        ref={area}
        style={{ flex: 1 }}
        onLayout={measureArea}
        onStartShouldSetResponder={() => true}
        onMoveShouldSetResponder={() => true}
        onResponderTerminationRequest={() => false}
        onResponderGrant={onTouches}
        onResponderStart={onTouches}
        onResponderMove={onTouches}
        onResponderEnd={onTouches}
        onResponderRelease={onAllUp}
        onResponderTerminate={onAllUp}
      >
        <View style={styles.wheel} onLayout={onZoneLayout('wheel')} pointerEvents="none">
          <Text style={styles.hint}>arrastra para girar</Text>
          <View style={[styles.needle, { left: `${((steer + 1) / 2) * 100}%` }]} />
        </View>
        <View
          style={styles.pedals}
          pointerEvents="box-none"
          onLayout={(e) => {
            pedalRow.current = { x: e.nativeEvent.layout.x, y: e.nativeEvent.layout.y };
          }}
        >
          <View style={[styles.pedal, held.brake && styles.pedalHeld]} onLayout={onZoneLayout('brake')} pointerEvents="none">
            <Text style={[styles.pedalText, held.brake && { color: '#fff' }]}>FRENO</Text>
          </View>
          <View style={[styles.pedal, held.gas && styles.pedalHeld]} onLayout={onZoneLayout('gas')} pointerEvents="none">
            <Text style={[styles.pedalText, held.gas && { color: '#fff' }]}>GAS</Text>
          </View>
        </View>
      </View>

      <Button kind="ghost" label="Salir" onPress={() => router.replace('/')} />
    </Screen>
  );
}

const PEDAL_GAP = 12;

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 },
  dot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.bad },
  status: { color: colors.dim, fontSize: 13 },
  wheel: {
    flex: 1,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  hint: { color: colors.dim },
  needle: {
    position: 'absolute',
    top: 10,
    bottom: 10,
    width: 4,
    marginLeft: -2,
    borderRadius: 2,
    backgroundColor: colors.accent,
  },
  pedals: { flexDirection: 'row', gap: 12, height: '42%', marginTop: PEDAL_GAP, marginBottom: 12 },
  pedal: {
    flex: 1,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 16,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pedalHeld: { backgroundColor: colors.accent, borderColor: colors.accent },
  pedalText: { color: colors.text, fontWeight: '700', letterSpacing: 1 },
});

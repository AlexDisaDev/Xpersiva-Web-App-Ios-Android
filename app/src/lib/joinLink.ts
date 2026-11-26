/**
 * Lee lo que trae un QR o un enlace y saca a qué juego hay que unirse.
 *
 * Formatos aceptados:
 *   https://<web xpersiva>/j/F1-7K2Q?h=192.168.0.17:8080   (QR recomendado: abre la app o la tienda)
 *   xpersiva://j/F1-7K2Q?h=192.168.0.17:8080               (esquema propio de la app)
 *   http://192.168.0.17:8080                            (QR actual del plugin, sin código)
 */
import { APP_SCHEME, SITE_HOST } from './config';

export type JoinTarget = {
  /** Código del juego, por ejemplo F1-7K2Q. "LAN" si el QR no traía ninguno. */
  code: string;
  /** host:puerto del PC que corre el juego. */
  host: string;
  /** true si hay que usar wss:// (juego detrás de un túnel o dominio con TLS). */
  secure: boolean;
};

const HOST_RE = /^[a-z0-9.-]+(:\d{1,5})?$/i;
const CODE_RE = /^[A-Z0-9][A-Z0-9-]{1,31}$/i;

export function parseJoinLink(raw: string): JoinTarget | null {
  let url: URL;
  try {
    url = new URL(raw.trim());
  } catch {
    return null;
  }

  const isOurLink =
    url.protocol === `${APP_SCHEME}:` ||
    (url.protocol === 'https:' && url.host === SITE_HOST);

  if (isOurLink) {
    // En xpersiva://j/CODE el "j" llega como hostname; en https llega en la ruta.
    const parts = [url.protocol === `${APP_SCHEME}:` ? url.hostname : '', ...url.pathname.split('/')]
      .filter(Boolean);
    if (parts[0] !== 'j' || !parts[1]) return null;
    return makeTarget(parts[1], url.searchParams.get('h'), url.searchParams.get('s') === '1');
  }

  // QR antiguo del plugin: la propia URL es el servidor del juego. Solo lo
  // aceptamos si apunta a la red local, para no conectarnos a cualquier web.
  if ((url.protocol === 'http:' || url.protocol === 'https:') && isPrivateIPv4(url.hostname)) {
    return makeTarget('LAN', url.host, url.protocol === 'https:');
  }
  return null;
}

function isPrivateIPv4(host: string): boolean {
  const m = /^(\d+)\.(\d+)\.\d+\.\d+$/.exec(host);
  if (!m) return false;
  const a = Number(m[1]);
  const b = Number(m[2]);
  return a === 10 || (a === 192 && b === 168) || (a === 172 && b >= 16 && b <= 31);
}

export function makeTarget(code: string, host: string | null, secure = false): JoinTarget | null {
  if (!host || !HOST_RE.test(host) || !CODE_RE.test(code)) return null;
  return { code: code.toUpperCase(), host, secure };
}

export function socketUrl(t: JoinTarget): string {
  return `${t.secure ? 'wss' : 'ws'}://${t.host}`;
}

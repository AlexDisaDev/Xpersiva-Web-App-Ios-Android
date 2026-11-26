import { SITE_URL } from './config';

/**
 * Una experiencia publicada en la web de Xpersiva. La app no trae ninguna
 * escrita: las lee de SITE_URL/experiences.json, así que crear una nueva es
 * editar ese archivo en la web, sin pasar por la revisión de las tiendas.
 */
export type Experience = {
  id: string;          // también es la URL: SITE_URL/e/<id>
  name: string;
  brand?: string;
  venue: string;
  city?: string;
  lat: number;
  lng: number;
  startsAt?: string;   // ISO 8601
  endsAt?: string;
  description?: string;
  tags?: string[];
};

export type Coords = { latitude: number; longitude: number };
export type ExperienceWithDistance = Experience & { km?: number };

export const EXPERIENCES_URL = `${SITE_URL}/experiences.json`;

export async function fetchExperiences(): Promise<Experience[]> {
  const res = await fetch(EXPERIENCES_URL, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const body = await res.json();
  const list: unknown[] = Array.isArray(body) ? body : body?.experiences ?? [];
  return list.filter(isExperience).filter((e) => !hasEnded(e));
}

function isExperience(e: any): e is Experience {
  return (
    e && typeof e.id === 'string' && typeof e.name === 'string' && typeof e.venue === 'string' &&
    Number.isFinite(e.lat) && Number.isFinite(e.lng)
  );
}

function hasEnded(e: Experience, now = Date.now()): boolean {
  return !!e.endsAt && Date.parse(e.endsAt) < now;
}

export function isLiveNow(e: Experience, now = Date.now()): boolean {
  const start = e.startsAt ? Date.parse(e.startsAt) : -Infinity;
  const end = e.endsAt ? Date.parse(e.endsAt) : Infinity;
  return start <= now && now <= end;
}

/** Distancia en km sobre la esfera (haversine). */
export function distanceKm(a: Coords, b: Coords): number {
  const R = 6371;
  const rad = (d: number) => (d * Math.PI) / 180;
  const dLat = rad(b.latitude - a.latitude);
  const dLng = rad(b.longitude - a.longitude);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Minúsculas y sin tildes, para que "madrid" encuentre "Madrid" y "carrera" "Carrera". */
const norm = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/** Todas las palabras de la búsqueda tienen que aparecer en algún campo. */
export function matches(e: Experience, query: string): boolean {
  const words = norm(query).split(/\s+/).filter(Boolean);
  if (!words.length) return true;
  const hay = norm([e.name, e.brand, e.venue, e.city, e.description, ...(e.tags ?? [])].filter(Boolean).join(' '));
  return words.every((w) => hay.includes(w));
}

/**
 * Con ubicación: de más cerca a más lejos. Sin ella: primero las que están
 * en marcha y luego por fecha de inicio.
 */
export function sortExperiences(list: Experience[], here: Coords | null): ExperienceWithDistance[] {
  if (here) {
    return list
      .map((e) => ({ ...e, km: distanceKm(here, { latitude: e.lat, longitude: e.lng }) }))
      .sort((a, b) => a.km - b.km);
  }
  return [...list].sort((a, b) => {
    const live = Number(isLiveNow(b)) - Number(isLiveNow(a));
    if (live) return live;
    return (Date.parse(a.startsAt ?? '') || 0) - (Date.parse(b.startsAt ?? '') || 0);
  });
}

export function formatKm(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)} m`;
  if (km < 10) return `${km.toFixed(1)} km`;
  return `${Math.round(km)} km`;
}

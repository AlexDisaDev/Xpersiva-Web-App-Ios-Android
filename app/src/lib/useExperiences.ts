import * as Location from 'expo-location';
import { useCallback, useEffect, useState } from 'react';
import { type Coords, type Experience, fetchExperiences } from './experiences';

export type LocState = 'unknown' | 'asking' | 'granted' | 'denied';

/** Carga las experiencias de la web y, si el usuario quiere, su ubicación. */
export function useExperiences() {
  const [items, setItems] = useState<Experience[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [here, setHere] = useState<Coords | null>(null);
  const [loc, setLoc] = useState<LocState>('unknown');

  const reload = useCallback(() => {
    setError(null);
    return fetchExperiences()
      .then(setItems)
      .catch(() => setError('No hemos podido cargar las experiencias. Revisa tu conexión.'));
  }, []);

  const locate = useCallback(async () => {
    setLoc('granted');
    try {
      // Basta con saber la zona para ordenar: precisión media, más rápida.
      const last = await Location.getLastKnownPositionAsync();
      if (last) setHere(last.coords);
      const now = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      setHere(now.coords);
    } catch {
      // GPS apagado o sin señal: seguimos sin ordenar por distancia.
    }
  }, []);

  const askLocation = useCallback(async () => {
    setLoc('asking');
    const p = await Location.requestForegroundPermissionsAsync();
    if (p.granted) locate();
    else setLoc('denied');
  }, [locate]);

  useEffect(() => {
    void Promise.resolve().then(reload);
    // Si ya dio permiso otra vez, no volvemos a preguntar.
    Location.getForegroundPermissionsAsync().then((p) => {
      if (p.granted) locate();
      else setLoc(p.canAskAgain ? 'unknown' : 'denied');
    });
  }, [reload, locate]);

  return { items, error, here, loc, askLocation, reload };
}

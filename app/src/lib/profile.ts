import * as SecureStore from 'expo-secure-store';

/** Lo que el jugador nos deja saber de él. El correo solo con su permiso. */
export type Profile = {
  name: string;
  email?: string;
  /** Aceptó recibir comunicaciones de la marca del evento (RGPD). */
  marketingConsent?: boolean;
};

const KEY = 'xpersiva.profile.v1';

export async function loadProfile(): Promise<Profile | null> {
  try {
    const raw = await SecureStore.getItemAsync(KEY);
    return raw ? (JSON.parse(raw) as Profile) : null;
  } catch {
    return null;
  }
}

export async function saveProfile(profile: Profile): Promise<void> {
  await SecureStore.setItemAsync(KEY, JSON.stringify(profile));
}

export async function clearProfile(): Promise<void> {
  await SecureStore.deleteItemAsync(KEY);
}

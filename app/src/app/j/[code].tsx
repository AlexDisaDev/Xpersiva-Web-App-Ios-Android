import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { makeTarget } from '../../lib/joinLink';
import { type Profile, loadProfile, saveProfile } from '../../lib/profile';
import { colors } from '../../lib/theme';
import { Button, Dim, Screen, Spacer, Title } from '../../lib/ui';

/**
 * Pantalla de entrada a un juego. Llega aquí tanto el escáner interno como el
 * enlace del QR (https://<web xpersiva>/j/CODIGO?h=IP:PUERTO) cuando la app ya
 * está instalada.
 */
export default function Join() {
  const { code, h, s } = useLocalSearchParams<{ code: string; h?: string; s?: string }>();
  const target = makeTarget(String(code ?? ''), h ? String(h) : null, s === '1');

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [consent, setConsent] = useState(false);

  useEffect(() => {
    loadProfile().then((p) => {
      if (!p) return;
      setName(p.name);
      setEmail(p.email ?? '');
      setConsent(!!p.marketingConsent);
    });
  }, []);

  if (!target) {
    return (
      <Screen>
        <View style={{ flex: 1, justifyContent: 'center' }}>
          <Title>Enlace incompleto</Title>
          <Spacer h={8} />
          <Dim>Al QR le falta la dirección del juego. Vuelve a escanearlo desde la pantalla del evento.</Dim>
        </View>
        <Button label="Escanear de nuevo" onPress={() => router.replace('/scan')} />
      </Screen>
    );
  }

  const trimmed = name.trim();
  const validEmail = email.trim() === '' || /^\S+@\S+\.\S+$/.test(email.trim());

  const enter = async () => {
    const profile: Profile = {
      name: trimmed.slice(0, 24),
      email: email.trim() || undefined,
      marketingConsent: email.trim() ? consent : false,
    };
    await saveProfile(profile);
    router.replace({
      pathname: '/play',
      params: { code: target.code, h: target.host, s: target.secure ? '1' : '0' },
    });
  };

  return (
    <Screen>
      <View style={{ flex: 1, justifyContent: 'center' }}>
        <Text style={styles.code}>EXPERIENCIA {target.code}</Text>
        <Spacer h={6} />
        <Title>¿Cómo te llamas?</Title>
        <Spacer h={20} />
        <TextInput
          value={name}
          onChangeText={setName}
          placeholder="Tu nombre en la partida"
          placeholderTextColor={colors.dim}
          maxLength={24}
          autoFocus={!name}
          returnKeyType="next"
          style={styles.input}
        />
        <Spacer h={12} />
        <TextInput
          value={email}
          onChangeText={setEmail}
          placeholder="Correo (opcional, para premios)"
          placeholderTextColor={colors.dim}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
          style={styles.input}
        />
        {email.trim() ? (
          <View style={styles.consent}>
            <Switch value={consent} onValueChange={setConsent} />
            <Text style={styles.consentText}>Acepto que la marca del evento me escriba a este correo.</Text>
          </View>
        ) : null}
      </View>
      <Button label="Entrar al juego" disabled={!trimmed || !validEmail} onPress={enter} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  code: { color: colors.accent, fontWeight: '800', letterSpacing: 2 },
  input: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: 12,
    color: colors.text,
    fontSize: 17,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  consent: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 12 },
  consentText: { color: colors.dim, flex: 1, fontSize: 13 },
});

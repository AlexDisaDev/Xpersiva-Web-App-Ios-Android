import { router, useLocalSearchParams } from 'expo-router';
import { Linking, Platform, ScrollView, StyleSheet, Text } from 'react-native';
import { formatKm, isLiveNow, sortExperiences } from '../../lib/experiences';
import { colors } from '../../lib/theme';
import { Button, Dim, Screen, Spacer, Title } from '../../lib/ui';
import { useExperiences } from '../../lib/useExperiences';

/** Ficha de una experiencia. También se abre con SITE_URL/e/<id>. */
export default function ExperienceScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { items, here, error } = useExperiences();
  const item = items ? sortExperiences(items, here).find((e) => e.id === id) : undefined;

  if (!item) {
    return (
      <Screen>
        <Dim>{error ?? (items ? 'Esta experiencia ya no está disponible.' : 'Cargando…')}</Dim>
        <Spacer />
        <Button kind="ghost" label="Volver" onPress={() => (router.canGoBack() ? router.back() : router.replace('/'))} />
      </Screen>
    );
  }

  const when = [item.startsAt, item.endsAt]
    .filter(Boolean)
    .map((d) => new Date(d!).toLocaleDateString(undefined, { day: 'numeric', month: 'short' }))
    .join(' – ');

  const openMap = () => {
    const ll = `${item.lat},${item.lng}`;
    const label = encodeURIComponent(item.name);
    Linking.openURL(Platform.OS === 'ios' ? `https://maps.apple.com/?ll=${ll}&q=${label}` : `geo:${ll}?q=${ll}(${label})`);
  };

  return (
    <Screen>
      <ScrollView style={{ flex: 1 }}>
        {item.brand ? <Text style={styles.brand}>{item.brand.toUpperCase()}</Text> : null}
        <Title>{item.name}</Title>
        <Spacer h={8} />
        <Text style={styles.meta}>
          {[item.venue, item.city, item.km !== undefined ? formatKm(item.km) : null].filter(Boolean).join(' · ')}
        </Text>
        {when ? <Text style={styles.meta}>{when}{isLiveNow(item) ? ' · ahora' : ''}</Text> : null}
        <Spacer />
        {item.description ? <Dim>{item.description}</Dim> : null}
      </ScrollView>
      <Button kind="ghost" label="Cómo llegar" onPress={openMap} />
      <Spacer h={12} />
      <Button label="Estoy allí · escanear QR" onPress={() => router.push('/scan')} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  brand: { color: colors.accent, fontWeight: '800', letterSpacing: 2, marginBottom: 6 },
  meta: { color: colors.text, fontSize: 15, marginTop: 2 },
});

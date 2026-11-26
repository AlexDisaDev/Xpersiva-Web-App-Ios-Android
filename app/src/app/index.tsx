import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, Image, Linking, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { type ExperienceWithDistance, formatKm, isLiveNow, matches, sortExperiences } from '../lib/experiences';
import { colors } from '../lib/theme';
import { Button, Dim, Screen, Spacer } from '../lib/ui';
import { useExperiences } from '../lib/useExperiences';

/** Inicio: buscador de experiencias, de más cerca a más lejos. */
export default function Home() {
  const { items, error, here, loc, askLocation, reload } = useExperiences();
  const [query, setQuery] = useState('');

  const list = useMemo(
    () => (items ? sortExperiences(items.filter((e) => matches(e, query)), here) : []),
    [items, query, here],
  );

  return (
    <Screen>
      <Image source={require('../../assets/logo-header.png')} style={styles.logo} resizeMode="contain" accessibilityLabel="Xpersiva" />
      <Spacer h={16} />
      <Text style={styles.eyebrow}>EXPERIENCIAS EN VIVO</Text>
      <Text style={styles.heading}>Entra en la experiencia.</Text>
      <Spacer h={18} />
      <TextInput
        value={query}
        onChangeText={setQuery}
        placeholder="Busca una experiencia, marca o ciudad"
        placeholderTextColor={colors.dim}
        returnKeyType="search"
        clearButtonMode="while-editing"
        autoCorrect={false}
        style={styles.search}
      />
      <Spacer h={12} />

      {loc !== 'granted' ? (
        <Pressable
          onPress={loc === 'denied' ? () => Linking.openSettings() : askLocation}
          disabled={loc === 'asking'}
          style={styles.locChip}
        >
          <Text style={styles.locText}>
            {loc === 'denied'
              ? 'Activa la ubicación en Ajustes para ver primero lo que tienes cerca'
              : 'Ver primero lo que tengo cerca'}
          </Text>
        </Pressable>
      ) : (
        <Dim>{here ? 'De más cerca a más lejos' : 'Buscando tu ubicación…'}</Dim>
      )}
      <Spacer h={12} />

      {error ? (
        <>
          <Dim>{error}</Dim>
          <Spacer h={8} />
          <Button kind="ghost" label="Reintentar" onPress={reload} />
        </>
      ) : null}
      {!items && !error ? <ActivityIndicator color={colors.accent} /> : null}

      <FlatList
        data={list}
        keyExtractor={(e) => e.id}
        keyboardShouldPersistTaps="handled"
        ItemSeparatorComponent={() => <Spacer h={10} />}
        ListEmptyComponent={
          items ? <Dim>{query ? 'Ninguna experiencia coincide con tu búsqueda.' : 'Aún no hay experiencias publicadas.'}</Dim> : undefined
        }
        renderItem={({ item }) => <Row item={item} />}
        style={{ flex: 1 }}
      />

      <Spacer h={12} />
      <Button label="Escanear QR" onPress={() => router.push('/scan')} />
    </Screen>
  );
}

function Row({ item }: { item: ExperienceWithDistance }) {
  return (
    <Pressable
      onPress={() => router.push({ pathname: '/e/[id]', params: { id: item.id } })}
      style={({ pressed }) => [styles.row, pressed && { opacity: 0.7 }]}
    >
      <View style={{ flex: 1 }}>
        <Text style={styles.rowTitle}>{item.name}</Text>
        <Text style={styles.rowSub} numberOfLines={1}>
          {[item.brand, item.venue, item.city].filter(Boolean).join(' · ')}
        </Text>
      </View>
      <View style={{ alignItems: 'flex-end' }}>
        {item.km !== undefined ? <Text style={styles.km}>{formatKm(item.km)}</Text> : null}
        {isLiveNow(item) ? <Text style={styles.live}>AHORA</Text> : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  logo: { width: 176, height: 52, alignSelf: 'flex-start' },
  eyebrow: { color: colors.accent, fontSize: 11, fontWeight: '800', letterSpacing: 2 },
  heading: { color: colors.text, fontSize: 28, fontWeight: '800', letterSpacing: -0.8, marginTop: 4 },
  search: {
    backgroundColor: colors.surface,
    borderRadius: 14,
    color: colors.text,
    fontSize: 17,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  locChip: {
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 999,
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  locText: { color: colors.text, fontSize: 14, fontWeight: '600' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderBottomWidth: 1,
    borderColor: colors.border,
    paddingVertical: 14,
  },
  rowTitle: { color: colors.text, fontSize: 16, fontWeight: '700' },
  rowSub: { color: colors.dim, fontSize: 13, marginTop: 2 },
  km: { color: colors.text, fontWeight: '700' },
  live: { color: colors.ok, fontSize: 11, fontWeight: '800', letterSpacing: 1, marginTop: 2 },
});

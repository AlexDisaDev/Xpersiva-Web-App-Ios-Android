import { type BarcodeScanningResult, CameraView, useCameraPermissions } from 'expo-camera';
import * as Haptics from 'expo-haptics';
import { router } from 'expo-router';
import { useRef, useState } from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';
import { parseJoinLink } from '../lib/joinLink';
import { colors } from '../lib/theme';
import { Button, Dim, Screen, Spacer, Title } from '../lib/ui';

export default function Scan() {
  const [permission, requestPermission] = useCameraPermissions();
  const [error, setError] = useState<string | null>(null);
  const handled = useRef(false);

  if (!permission) return <Screen>{null}</Screen>;

  // Primera vez: explicamos por qué antes de que salga el aviso del sistema.
  if (!permission.granted) {
    return (
      <Screen>
        <View style={{ flex: 1, justifyContent: 'center' }}>
          <Title>Necesitamos la cámara</Title>
          <Spacer h={8} />
          <Dim>Solo para leer el código QR del juego. No guardamos fotos ni vídeo.</Dim>
        </View>
        {permission.canAskAgain ? (
          <Button label="Permitir cámara" onPress={requestPermission} />
        ) : (
          <Button label="Abrir ajustes" onPress={() => Linking.openSettings()} />
        )}
        <Spacer h={12} />
        <Button kind="ghost" label="Volver" onPress={() => router.back()} />
      </Screen>
    );
  }

  const onScanned = ({ data }: BarcodeScanningResult) => {
    if (handled.current) return;
    const target = parseJoinLink(data);
    if (!target) {
      setError('Este QR no es de una experiencia Xpersiva.');
      return;
    }
    handled.current = true;
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    router.replace({
      pathname: '/j/[code]',
      params: { code: target.code, h: target.host, s: target.secure ? '1' : '0' },
    });
  };

  return (
    <View style={styles.full}>
      <CameraView
        style={StyleSheet.absoluteFill}
        facing="back"
        barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
        onBarcodeScanned={onScanned}
      />
      <View style={styles.frame} pointerEvents="none" />
      <View style={styles.bottom}>
        <Text style={styles.hint}>{error ?? 'Apunta al QR de la pantalla'}</Text>
        <Spacer h={12} />
        <Button kind="ghost" label="Cancelar" onPress={() => router.back()} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  full: { flex: 1, backgroundColor: '#000' },
  frame: {
    position: 'absolute',
    alignSelf: 'center',
    top: '25%',
    width: 240,
    height: 240,
    borderWidth: 3,
    borderColor: colors.accent,
    borderRadius: 24,
  },
  bottom: { position: 'absolute', left: 24, right: 24, bottom: 48 },
  hint: { color: '#fff', textAlign: 'center', fontSize: 16, fontWeight: '600' },
});

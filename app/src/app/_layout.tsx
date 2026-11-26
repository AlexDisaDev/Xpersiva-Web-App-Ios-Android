import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useState } from 'react';
import { View } from 'react-native';
import { StartupIntro } from '../lib/StartupIntro';
import { colors } from '../lib/theme';

export default function RootLayout() {
  const [showIntro, setShowIntro] = useState(true);
  const finishIntro = useCallback(() => setShowIntro(false), []);

  return (
    <View style={{ flex: 1, backgroundColor: '#090B1E' }}>
      <StatusBar style={showIntro ? 'light' : 'dark'} animated />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.bg },
          animation: 'fade',
        }}
      />
      {showIntro && <StartupIntro onFinish={finishIntro} />}
    </View>
  );
}

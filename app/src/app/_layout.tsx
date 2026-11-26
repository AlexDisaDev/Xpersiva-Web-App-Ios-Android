import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { StartupIntro } from '../lib/StartupIntro';
import { colors } from '../lib/theme';

export default function RootLayout() {
  const [showIntro, setShowIntro] = useState(true);
  return (
    <>
      <StatusBar style={showIntro ? 'light' : 'dark'} />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.bg },
          animation: 'fade',
        }}
      />
      {showIntro && <StartupIntro onFinish={() => setShowIntro(false)} />}
    </>
  );
}

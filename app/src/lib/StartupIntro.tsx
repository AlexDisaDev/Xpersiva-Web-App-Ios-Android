import { useEffect, useState } from 'react';
import { AccessibilityInfo, Animated, Easing, StyleSheet, Text, View } from 'react-native';

const BACKGROUND = '#090B1E';

export function StartupIntro({ onFinish }: { onFinish: () => void }) {
  const [symbolOpacity] = useState(() => new Animated.Value(0));
  const [symbolScale] = useState(() => new Animated.Value(0.9));
  const [rotation] = useState(() => new Animated.Value(0));
  const [nameOpacity] = useState(() => new Animated.Value(0));
  const [taglineOpacity] = useState(() => new Animated.Value(0));
  const [screenOpacity] = useState(() => new Animated.Value(1));

  useEffect(() => {
    let active = true;
    let animation: Animated.CompositeAnimation | undefined;

    AccessibilityInfo.isReduceMotionEnabled().catch(() => false).then((reduceMotion) => {
      if (!active) return;

      const reveal = (value: Animated.Value, duration: number) =>
        Animated.timing(value, {
          toValue: 1,
          duration,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        });

      animation = reduceMotion
        ? Animated.sequence([
            Animated.parallel([
              reveal(symbolOpacity, 200),
              reveal(symbolScale, 200),
              reveal(nameOpacity, 200),
              reveal(taglineOpacity, 200),
            ]),
            Animated.delay(500),
            Animated.timing(screenOpacity, { toValue: 0, duration: 200, useNativeDriver: true }),
          ])
        : Animated.sequence([
            Animated.delay(180),
            Animated.parallel([
              reveal(symbolOpacity, 620),
              reveal(symbolScale, 620),
              Animated.sequence([
                Animated.delay(650),
                Animated.timing(rotation, {
                  toValue: 1,
                  duration: 1350,
                  easing: Easing.out(Easing.cubic),
                  useNativeDriver: true,
                }),
              ]),
              Animated.sequence([Animated.delay(650), reveal(nameOpacity, 600)]),
              Animated.sequence([Animated.delay(1300), reveal(taglineOpacity, 550)]),
            ]),
            Animated.delay(150),
            Animated.timing(screenOpacity, {
              toValue: 0,
              duration: 380,
              easing: Easing.inOut(Easing.cubic),
              useNativeDriver: true,
            }),
          ]);

      animation.start(({ finished }) => {
        if (finished && active) onFinish();
      });
    });

    return () => {
      active = false;
      animation?.stop();
    };
  }, [nameOpacity, onFinish, rotation, screenOpacity, symbolOpacity, symbolScale, taglineOpacity]);

  const spin = rotation.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '720deg'] });

  return (
    <Animated.View style={[styles.overlay, { opacity: screenOpacity }]}>
      <View style={styles.content}>
        <Animated.Image
          accessibilityLabel="Xpersiva"
          source={require('../../assets/startup-portal.png')}
          resizeMode="contain"
          style={[styles.symbol, { opacity: symbolOpacity, transform: [{ scale: symbolScale }, { rotate: spin }] }]}
        />
        <Animated.Text style={[styles.name, { opacity: nameOpacity }]}>xpersiva</Animated.Text>
        <Animated.View style={[styles.message, { opacity: taglineOpacity }]}>
          <View style={styles.rule} />
          <Text style={styles.headline}>VIVE EL EVENTO</Text>
          <Text style={styles.headline}>DESDE DENTRO</Text>
          <Text style={styles.subline}>Escanea · Entra · Juega</Text>
        </Animated.View>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    zIndex: 10,
    elevation: 10,
    backgroundColor: BACKGROUND,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: { alignItems: 'center', width: '100%', paddingHorizontal: 24 },
  symbol: { width: 272, height: 272 },
  name: { color: '#FFFFFF', fontSize: 52, fontWeight: '800', letterSpacing: -2, marginTop: 25 },
  message: { alignItems: 'center', marginTop: 26 },
  rule: { width: 52, height: 3, borderRadius: 2, backgroundColor: '#42E3F2', marginBottom: 20 },
  headline: { color: '#A993FF', fontSize: 19, fontWeight: '800', letterSpacing: 1.6, lineHeight: 27 },
  subline: { color: '#C0C5DC', fontSize: 14, marginTop: 27 },
});

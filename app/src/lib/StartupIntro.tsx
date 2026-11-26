import { useEventListener } from 'expo';
import { useVideoPlayer, VideoView } from 'expo-video';
import { useCallback, useEffect, useRef, useState } from 'react';
import { AccessibilityInfo, Animated, Image, StyleSheet, View } from 'react-native';

const source = require('../../assets/startup-intro.mp4');

export function StartupIntro({ onFinish }: { onFinish: () => void }) {
  const [opacity] = useState(() => new Animated.Value(1));
  const finishing = useRef(false);
  const [showFallback, setShowFallback] = useState(false);
  const player = useVideoPlayer(source, (video) => {
    video.loop = false;
    video.muted = true;
  });

  const finish = useCallback(() => {
    if (finishing.current) return;
    finishing.current = true;
    Animated.timing(opacity, { toValue: 0, duration: 260, useNativeDriver: true }).start(onFinish);
  }, [opacity, onFinish]);

  useEventListener(player, 'playToEnd', finish);
  useEventListener(player, 'statusChange', ({ status }) => {
    if (status === 'error') {
      setShowFallback(true);
      finish();
    }
  });

  useEffect(() => {
    let active = true;
    AccessibilityInfo.isReduceMotionEnabled()
      .then((reduced) => {
        if (!active) return;
        if (reduced) finish();
        else player.play();
      })
      .catch(() => { if (active) player.play(); });
    // Never trap the user on a broken video decoder.
    const timeout = setTimeout(() => {
      setShowFallback(true);
      finish();
    }, 5500);
    return () => { active = false; clearTimeout(timeout); };
  }, [finish, player]);

  return (
    <Animated.View style={[styles.overlay, { opacity }]} pointerEvents="auto">
      {showFallback ? (
        <View style={styles.fallback}>
          <Image source={require('../../assets/startup-symbol.png')} style={styles.symbol} resizeMode="contain" />
        </View>
      ) : (
        <VideoView player={player} style={styles.video} contentFit="contain" nativeControls={false} />
      )}
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  overlay: { ...StyleSheet.absoluteFill, zIndex: 20, backgroundColor: '#090B1E' },
  video: { width: '100%', height: '100%' },
  fallback: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  symbol: { width: 150, height: 150 },
});

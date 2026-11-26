import * as Haptics from 'expo-haptics';
import { DeviceMotion } from 'expo-sensors';
import { router, useLocalSearchParams } from 'expo-router';
import { type ComponentRef, useEffect, useRef, useState } from 'react';
import { type GestureResponderEvent, type LayoutRectangle, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { type JoinTarget, makeTarget } from '../lib/joinLink';
import { type Profile, loadProfile } from '../lib/profile';
import { colors } from '../lib/theme';
import { Screen } from '../lib/ui';
import { type SocketStatus, useGameSocket } from '../lib/useGameSocket';

const STATUS_TEXT: Record<SocketStatus, string> = {
  connecting: 'conectando…',
  connected: 'conectado, esperando a la experiencia…',
  joined: 'dentro',
  retrying: 'sin conexión, reintentando…',
};

export default function Play() {
  const { code, h, s } = useLocalSearchParams<{ code: string; h: string; s?: string }>();
  const target = makeTarget(String(code ?? ''), h ? String(h) : null, s === '1');
  const [profile, setProfile] = useState<Profile | null>(null);

  useEffect(() => {
    loadProfile().then(setProfile);
  }, []);

  if (!target || !profile) return <Screen>{null}</Screen>;
  return <Controller target={target} profile={profile} />;
}

type Zone = 'wheel' | 'brake' | 'gas';

/**
 * Mando multitáctil: toda la zona es un único responder y cada dedo se
 * asigna a la zona que pisa, así se puede girar y acelerar a la vez.
 */
function Controller({ target, profile }: { target: JoinTarget; profile: Profile }) {
  const { status, setInput } = useGameSocket(target, profile);
  const [steer, setSteer] = useState(0);
  const [held, setHeld] = useState({ brake: false, gas: false });
  const [motion, setMotion] = useState<'idle' | 'active' | 'unavailable' | 'denied'>('idle');
  const [inverted, setInverted] = useState(false);
  const angle = useRef<number | null>(null);
  const neutral = useRef<number | null>(null);
  const invertedRef = useRef(false);
  const motionRef = useRef(false);
  const heldRef = useRef(held);
  const steerRef = useRef(0);
  const subscription = useRef<ReturnType<typeof DeviceMotion.addListener> | null>(null);

  useEffect(() => () => subscription.current?.remove(), []);

  const enableMotion = async () => {
    try {
      if (!(await DeviceMotion.isAvailableAsync())) {
        setMotion('unavailable');
        return;
      }
      const permission = await DeviceMotion.requestPermissionsAsync();
      if (!permission.granted) {
        setMotion('denied');
        return;
      }
      DeviceMotion.setUpdateInterval(33);
      neutral.current = null;
      subscription.current?.remove();
      subscription.current = DeviceMotion.addListener(({ rotation }) => {
        if (!rotation || !Number.isFinite(rotation.alpha)) return;
        angle.current = rotation.alpha;
        if (neutral.current === null) neutral.current = rotation.alpha;
        const delta = Math.atan2(Math.sin(rotation.alpha - neutral.current), Math.cos(rotation.alpha - neutral.current));
        const raw = Math.max(-1, Math.min(1, delta / 0.55)) * (invertedRef.current ? -1 : 1);
        const next = Math.abs(raw) < 0.04 ? 0 : raw;
        steerRef.current = next;
        setSteer(next);
        setInput({ steer: next, throttle: heldRef.current.gas ? 1 : 0, brake: heldRef.current.brake ? 1 : 0 });
      });
      motionRef.current = true;
      setMotion('active');
    } catch {
      setMotion('unavailable');
    }
  };

  const calibrate = () => {
    neutral.current = angle.current;
    steerRef.current = 0;
    setSteer(0);
    setInput({ steer: 0, throttle: heldRef.current.gas ? 1 : 0, brake: heldRef.current.brake ? 1 : 0 });
    Haptics.selectionAsync();
  };

  const area = useRef<ComponentRef<typeof View>>(null);
  const origin = useRef({ x: 0, y: 0 });
  const zones = useRef<Partial<Record<Zone, LayoutRectangle>>>({});
  const pedalRow = useRef({ x: 0, y: 0 });
  const prevHeld = useRef(held);

  const measureArea = () =>
    area.current?.measureInWindow((x, y) => {
      origin.current = { x, y };
    });
  const zoneAt = (x: number, y: number): Zone | null => {
    for (const z of ['wheel', 'brake', 'gas'] as Zone[]) {
      const local = zones.current[z];
      if (!local) continue;
      // Los pedales miden su layout respecto a su fila: lo pasamos a la zona táctil.
      const off = z === 'wheel' ? { x: 0, y: 0 } : pedalRow.current;
      const r = { ...local, x: local.x + off.x, y: local.y + off.y };
      if (x >= r.x && x <= r.x + r.width && y >= r.y && y <= r.y + r.height) return z;
    }
    return null;
  };

  const update = (touches: GestureResponderEvent['nativeEvent']['touches']) => {
    let nextSteer = motionRef.current ? steerRef.current : 0;
    let steering = false;
    const next = { brake: false, gas: false };

    for (const t of touches) {
      const x = t.pageX - origin.current.x;
      const y = t.pageY - origin.current.y;
      const z = zoneAt(x, y);
      if (z === 'wheel' && !steering && !motionRef.current) {
        const r = zones.current.wheel!;
        nextSteer = Math.max(-1, Math.min(1, ((x - r.x) / r.width) * 2 - 1));
        steering = true;
      } else if (z === 'brake') next.brake = true;
      else if (z === 'gas') next.gas = true;
    }

    if ((next.gas && !prevHeld.current.gas) || (next.brake && !prevHeld.current.brake)) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
    prevHeld.current = next;
    heldRef.current = next;
    steerRef.current = nextSteer;
    setSteer(nextSteer);
    setHeld(next);
    setInput({ steer: nextSteer, throttle: next.gas ? 1 : 0, brake: next.brake ? 1 : 0 });
  };

  const onTouches = (e: GestureResponderEvent) => update(e.nativeEvent.touches);
  const onAllUp = () => update([]);

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="light" />
      <View style={styles.header}>
        <View style={[styles.dot, status === 'joined' && { backgroundColor: colors.ok }]} />
        <Text style={styles.status}>
          {STATUS_TEXT[status]}
        </Text>
        <Pressable accessibilityRole="button" onPress={() => router.replace('/')} style={styles.close}>
          <Text style={styles.closeText}>Salir</Text>
        </Pressable>
      </View>

      <Text style={styles.kicker}>XPERSIVA / F1</Text>
      <Text style={styles.title}>Tú llevas el control.</Text>
      <Text style={styles.subtitle}>{profile.name} · Gira el móvil como un volante.</Text>

      <View
        ref={area}
        style={{ flex: 1 }}
        onLayout={measureArea}
        onStartShouldSetResponder={() => true}
        onMoveShouldSetResponder={() => true}
        onResponderTerminationRequest={() => false}
        onResponderGrant={onTouches}
        onResponderStart={onTouches}
        onResponderMove={onTouches}
        onResponderEnd={onTouches}
        onResponderRelease={onAllUp}
        onResponderTerminate={onAllUp}
      >
        <View style={styles.wheel} onLayout={(e) => { zones.current.wheel = e.nativeEvent.layout; measureArea(); }} pointerEvents="none">
          <Text style={styles.wheelIcon}>✦</Text>
          <Text style={styles.wheelTitle}>DIRECCIÓN</Text>
          <Text style={styles.hint}>{motion === 'active' ? 'Gira el móvil para dirigir' : 'Desliza aquí para dirigir'}</Text>
          <View style={styles.track}>
            <View style={styles.centerMark} />
            <View style={[styles.needle, { left: `${((steer + 1) / 2) * 100}%` }]} />
          </View>
          <View style={styles.directionLabels}><Text style={styles.direction}>IZQUIERDA</Text><Text style={styles.direction}>DERECHA</Text></View>
        </View>
        <View
          style={styles.pedals}
          pointerEvents="box-none"
          onLayout={(e) => {
            pedalRow.current = { x: e.nativeEvent.layout.x, y: e.nativeEvent.layout.y };
          }}
        >
          <View style={[styles.pedal, held.brake && styles.pedalHeld]} onLayout={(e) => { zones.current.brake = e.nativeEvent.layout; measureArea(); }} pointerEvents="none">
            <Text style={styles.pedalSymbol}>−</Text>
            <Text style={styles.pedalText}>FRENO</Text>
          </View>
          <View style={[styles.pedal, styles.gasPedal, held.gas && styles.gasHeld]} onLayout={(e) => { zones.current.gas = e.nativeEvent.layout; measureArea(); }} pointerEvents="none">
            <Text style={[styles.pedalSymbol, styles.gasSymbol]}>+</Text>
            <Text style={[styles.pedalText, styles.gasText]}>ACELERAR</Text>
          </View>
        </View>
      </View>
      <View style={styles.controls}>
        <Pressable accessibilityRole="button" onPress={motion === 'active' ? calibrate : enableMotion} style={styles.motionButton}>
          <Text style={styles.motionButtonText}>{motion === 'active' ? '◎  Calibrar giro' : '◎  Activar giro'}</Text>
        </Pressable>
        {motion === 'active' && <Pressable accessibilityRole="button" onPress={() => { invertedRef.current = !invertedRef.current; setInverted(invertedRef.current); }} style={styles.invertButton}><Text style={styles.invertText}>{inverted ? '↔  Invertido' : '↔  Invertir'}</Text></Pressable>}
      </View>
      {motion === 'denied' && <Text style={styles.note}>Permite el movimiento en Ajustes o usa la dirección táctil.</Text>}
      {motion === 'unavailable' && <Text style={styles.note}>Este dispositivo usa la dirección táctil.</Text>}
    </SafeAreaView>
  );
}

const PEDAL_GAP = 12;

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#090B1E', paddingHorizontal: 20, paddingVertical: 12 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 24 },
  dot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.bad },
  status: { color: '#B4B7D0', fontSize: 12, flex: 1 },
  close: { borderWidth: 1, borderColor: '#454A67', borderRadius: 18, paddingHorizontal: 13, paddingVertical: 7 },
  closeText: { color: '#F7F7FF', fontWeight: '700', fontSize: 12 },
  kicker: { color: '#37D6E7', fontSize: 11, letterSpacing: 2, fontWeight: '800' },
  title: { color: '#F7F7FF', fontSize: 27, fontWeight: '800', marginTop: 6 },
  subtitle: { color: '#AEB2CD', fontSize: 13, marginTop: 6, marginBottom: 22 },
  wheel: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#353B5D',
    backgroundColor: '#111630',
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  wheelIcon: { color: '#4ADDE8', fontSize: 44, marginBottom: 8 },
  wheelTitle: { color: '#FFF', fontSize: 15, fontWeight: '800', letterSpacing: 2 },
  hint: { color: '#AEB2CD', fontSize: 13, marginTop: 6 },
  track: { height: 8, width: '100%', backgroundColor: '#303657', borderRadius: 5, marginTop: 28, marginBottom: 12 },
  centerMark: { position: 'absolute', width: 2, height: 18, backgroundColor: '#7980A5', top: -5, left: '50%' },
  needle: {
    position: 'absolute',
    top: -8,
    width: 24,
    height: 24,
    marginLeft: -12,
    borderRadius: 12,
    backgroundColor: '#44DCE8',
  },
  directionLabels: { flexDirection: 'row', justifyContent: 'space-between', width: '100%' },
  direction: { color: '#7E86A7', fontSize: 10, fontWeight: '700', letterSpacing: 1 },
  pedals: { flexDirection: 'row', gap: 12, height: '42%', marginTop: PEDAL_GAP, marginBottom: 12 },
  pedal: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#665380',
    borderRadius: 24,
    backgroundColor: '#342440',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pedalHeld: { backgroundColor: '#704A78', borderColor: '#C193CC' },
  gasPedal: { backgroundColor: '#173E49', borderColor: '#32B9CC' },
  gasHeld: { backgroundColor: '#237E8C', borderColor: '#4DE8F1' },
  pedalSymbol: { color: '#F2BCD5', fontSize: 42, fontWeight: '300' },
  gasSymbol: { color: '#70EFF3' },
  pedalText: { color: '#F2BCD5', fontWeight: '800', letterSpacing: 2, fontSize: 12, marginTop: 8 },
  gasText: { color: '#70EFF3' },
  controls: { flexDirection: 'row', gap: 10 },
  motionButton: { flex: 1, borderColor: '#5846A7', borderWidth: 1, backgroundColor: '#221D47', borderRadius: 14, padding: 13, alignItems: 'center' },
  motionButtonText: { color: '#E4DEFF', fontWeight: '700', fontSize: 13 },
  invertButton: { borderColor: '#454A67', borderWidth: 1, borderRadius: 14, padding: 13, alignItems: 'center' },
  invertText: { color: '#D2D3E5', fontWeight: '700', fontSize: 13 },
  note: { color: '#AEB2CD', fontSize: 12, textAlign: 'center', marginTop: 8 },
});

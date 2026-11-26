# Xpersiva · Android (rama `android/v1`)

App nativa en **Kotlin + Jetpack Compose**, Android 8+ (minSdk 26). Lee las
experiencias de la web (`https://xpersiva.netlify.app/experiences.json`, rama
`web/v1`), así que crear una experiencia no requiere versión nueva.

`startsAt`/`endsAt` son la campaña (puede durar meses); `dailyOpensAt`/
`dailyClosesAt` son el horario del día dentro de ella, opcionales. Sin
horario diario, `estaEnMarcha()` (`experiencias/Experiencia.kt`) marcaría "en
directo" las 24 horas mientras dure la campaña, así que el filtro "En
directo" del inicio no filtraría nada: con horario diario, solo lo hace
dentro de él, y además se enseña como dato ("10:00 – 20:00") en la tarjeta y
en la ficha.

## Abrir y compilar

Abrir la carpeta del repo (esta rama) en Android Studio y darle a Run. O por terminal:

```

./gradlew assembleDebug        # APK de prueba en app/build/outputs/apk/debug
./gradlew bundleRelease        # AAB para Google Play (configura antes la firma)
```

## Estructura (`app/src/main/java/app/xpersiva/player/`)

- `MainActivity.kt`: navegación y enlaces entrantes (QR `/j/` y web `/e/`).
- `nucleo/`: configuración, lectura de enlaces del QR, nombre del jugador.
- `experiencias/`: modelo, catálogo desde la web y ubicación (solo al pulsar).
- `juego/`: conexión WebSocket (OkHttp) con el plugin del juego.
- `ui/`: pantallas en Compose: inicio con buscador, ficha, escáner (CameraX + ML Kit), entrada y mando multitáctil.

## Protocolo con el juego

```
app → juego  {"type":"hello","name":"Ana","code":"F1"}
juego → app  {"type":"joined","id":"<ClientID>"}
app → juego  {"type":"input","steer":-1..1,"throttle":0|1,"brake":0|1}
```

El juego va por `ws://` en la red local, por eso `network_security_config.xml`
permite tráfico sin cifrar.

## Tipo de control por experiencia (sin actualizar la app)

El QR puede llevar un parámetro `c` opcional para elegir qué pantalla de
mando se muestra, por ejemplo:

```
https://xpersiva.netlify.app/j/F1?h=192.168.0.17:8080               → volante + pedales (por defecto, si se omite c)
https://xpersiva.netlify.app/j/F1?h=192.168.0.17:8080&c=boton       → un único botón grande
https://xpersiva.netlify.app/j/F1?h=192.168.0.17:8080&c=giroscopio  → volante por giroscopio (pantalla horizontal)
```

Esto lo decide `nucleo/EnlaceUnion.kt` (`TipoControl`), no la versión de la
app: cada evento elige entre las plantillas que YA están en la app sin
pedir una actualización ni pasar revisión otra vez. Para generar el QR con
`c=boton`/`c=giroscopio` desde el Blueprint de Unreal, basta con añadir ese
parámetro a la "Public URL" que se usa al crear el código QR del plugin
`websocket`.

**`c=giroscopio`:** `juego/VolanteGiroscopio.kt` usa el sensor de gravedad
(o el acelerómetro si no hay) y mira hacia dónde cae la gravedad dentro del
plano de la pantalla: se gira el móvil en horizontal como un volante, e
inclinarlo hacia delante o atrás no mueve el coche. `PantallaMando.kt` pone
`requestedOrientation = SCREEN_ORIENTATION_SENSOR_LANDSCAPE` solo mientras
esa pantalla está abierta (la actividad declara `configChanges` para no
recrearse al girar) y deja gas/freno como dos botones táctiles, uno a cada
lado.

Añadir una plantilla de control genuinamente nueva (un cuarto caso, no solo
elegir entre las que ya existen) sí sigue necesitando código nuevo en
`PantallaMando.kt` y una nueva versión de la app.

## Marca

Sale de la rama `brand` (`brand/app-ready/`, los definitivos; `brand/v2-portal/`
solo se usó como referencia visual):

- Colores en `ui/Tema.kt` (`Colores`): violeta `#6D4AFF`, cian `#42E3F2`, azul
  noche `#090B1E` y superficies claras. Los mismos en `Nucleo/Marca.swift` (`ios/v1`).
- Icono adaptable (`res/mipmap-anydpi-v26/`): fondo azul noche, símbolo de
  `android-icon-foreground.png` y capa monocroma para los iconos temáticos de Android 13+.
- Arranque: la ventana y la splash del sistema (Android 12+, `values-v31/themes.xml`)
  son azul noche, igual que el primer fotograma de `res/raw/startup_intro.mp4`.
  `ui/IntroApertura.kt` reproduce ese vídeo una vez por arranque (sin sonido,
  tocar lo salta) y luego enseña la app. Con "Quitar animaciones" activado en
  Accesibilidad sale solo el símbolo un momento. Un enlace del QR que abre la app
  espera a que acabe la intro y aterriza en su pantalla como siempre.
- Logo de cabecera: `res/drawable-nodpi/logo_cabecera.png` (recorte de
  `logo-header.png`, para fondo claro).

## Antes de publicar

- Dominio definitivo en `nucleo/Config.kt` y `AndroidManifest.xml`.
- Huella SHA-256 de la firma de Google Play en `web/.well-known/assetlinks.json` (rama `web/v1`).
- Icono ya aplicado (ver "Marca" arriba); capturas de pantalla pendientes.

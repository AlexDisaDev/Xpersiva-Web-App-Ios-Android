# Xpersiva · Android (rama `android/v1`)

App nativa en **Kotlin + Jetpack Compose**, Android 8+ (minSdk 26). Lee las
experiencias de la web (`https://xpersiva.netlify.app/experiences.json`, rama
`web/v1`), así que crear una experiencia no requiere versión nueva.

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

**`c=giroscopio`:** `juego/VolanteGiroscopio.kt` usa el sensor
`TYPE_ROTATION_VECTOR` y toma la postura del teléfono al entrar como
"centro", así da igual cómo lo sujete cada uno. `PantallaMando.kt` pone
`requestedOrientation = SCREEN_ORIENTATION_SENSOR_LANDSCAPE` solo mientras
esa pantalla está abierta (la actividad declara `configChanges` para no
recrearse al girar) y deja gas/freno como dos botones táctiles, uno a cada
lado.

Añadir una plantilla de control genuinamente nueva (un cuarto caso, no solo
elegir entre las que ya existen) sí sigue necesitando código nuevo en
`PantallaMando.kt` y una nueva versión de la app.

## Antes de publicar

- Dominio definitivo en `nucleo/Config.kt` y `AndroidManifest.xml`.
- Huella SHA-256 de la firma de Google Play en `web/.well-known/assetlinks.json` (rama `web/v1`).
- Icono (ahora usa uno genérico del sistema) y capturas.

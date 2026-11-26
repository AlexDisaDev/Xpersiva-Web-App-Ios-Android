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

## Antes de publicar

- Dominio definitivo en `nucleo/Config.kt` y `AndroidManifest.xml`.
- Huella SHA-256 de la firma de Google Play en `web/.well-known/assetlinks.json` (rama `web/v1`).
- Icono (ahora usa uno genérico del sistema) y capturas.

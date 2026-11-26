# Xpersiva · iOS (rama `ios/v1`)

App nativa en **Swift 6 + SwiftUI**, iOS 17+. Lee las experiencias de la web
(`https://xpersiva.netlify.app/experiences.json`, rama `web/v1`), así que crear
una experiencia no requiere versión nueva.

## Abrir en Xcode

El proyecto se genera con [XcodeGen](https://github.com/yonaskolb/XcodeGen) para
no versionar el `.xcodeproj`:

```
brew install xcodegen
cd ios
xcodegen
open Xpersiva.xcodeproj
```

Pon tu Team ID en `DEVELOPMENT_TEAM` de `project.yml` (o en Xcode → Signing).

## Estructura

- `App/`: arranque, navegación y enlaces entrantes (`onOpenURL`).
- `Nucleo/`: configuración, lectura de enlaces del QR, nombre del jugador.
- `Experiencias/`: catálogo desde la web, ubicación (solo al pulsar), inicio con buscador y ficha.
- `Escaner/`: cámara + lectura de QR (AVFoundation), con explicación antes del permiso.
- `Juego/`: entrada con nombre, conexión WebSocket con el plugin del juego y mando multitáctil.

## Protocolo con el juego

```
app → juego  {"type":"hello","name":"Ana","code":"F1"}
juego → app  {"type":"joined","id":"<ClientID>"}
app → juego  {"type":"input","steer":-1..1,"throttle":0|1,"brake":0|1}
```

## Antes de publicar

- Dominio definitivo en `Nucleo/Config.swift` y `Xpersiva.entitlements`.
- Team ID en `web/.well-known/apple-app-site-association` (rama `web/v1`).
- Icono (Assets.xcassets → AppIcon) y capturas.

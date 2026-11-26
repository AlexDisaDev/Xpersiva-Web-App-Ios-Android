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

- `App/`: arranque, navegación, enlaces entrantes (`onOpenURL`) y el bloqueo de
  orientación (`OrientacionApp.swift`) que usa el mando por giroscopio.
- `Nucleo/`: configuración, lectura de enlaces del QR, nombre del jugador, colores de marca (`Marca.swift`).
- `Experiencias/`: catálogo desde la web, ubicación (solo al pulsar), inicio con buscador y ficha.
- `Escaner/`: cámara + lectura de QR (AVFoundation), con explicación antes del permiso.
- `Juego/`: entrada con nombre, conexión WebSocket con el plugin del juego y mando multitáctil.

## Protocolo con el juego

```
app → juego  {"type":"hello","name":"Ana","code":"F1"}
juego → app  {"type":"joined","id":"<ClientID>"}
app → juego  {"type":"input","steer":-1..1,"throttle":0|1,"brake":0|1}
```

## Tipo de control por experiencia (sin actualizar la app)

El QR puede llevar un parámetro `c` opcional para elegir qué pantalla de
mando se muestra, por ejemplo:

```
https://xpersiva.netlify.app/j/F1?h=192.168.0.17:8080               → volante + pedales (por defecto, si se omite c)
https://xpersiva.netlify.app/j/F1?h=192.168.0.17:8080&c=boton       → un único botón grande
https://xpersiva.netlify.app/j/F1?h=192.168.0.17:8080&c=giroscopio  → volante por giroscopio (pantalla horizontal)
```

Esto lo decide `Nucleo/EnlaceUnion.swift` (`TipoControl`), no la versión de
la app: cada evento elige entre las plantillas que YA están en la app sin
pedir una actualización ni pasar revisión otra vez. Para generar el QR con
`c=boton`/`c=giroscopio` desde el Blueprint de Unreal, basta con añadir ese
parámetro a la "Public URL" que se usa al crear el código QR del plugin
`websocket`.

**`c=giroscopio`:** `VolanteGiroscopio.swift` usa CoreMotion (`CMDeviceMotion`)
y toma la postura del teléfono al entrar como "centro", así da igual cómo lo
sujete cada uno. Esta pantalla gira a horizontal solo mientras está activa
(`OrientacionApp.swift` + `requestGeometryUpdate`); el resto de la app sigue
en vertical. Gas y freno quedan como dos botones táctiles, uno a cada lado.

Añadir una plantilla de control genuinamente nueva (un cuarto caso, no solo
elegir entre las que ya existen) sí sigue necesitando código nuevo en
`VistaMando.swift` y una nueva versión de la app.

## Marca

Colores en `Nucleo/Marca.swift` (acento morado, verde de "conectado", rojo de
error), iguales a los de `Colores.kt` en `android/v1`. El tint global de la
app (`.tint(.acento)` en `XpersivaApp.swift`) y `AccentColor` en
`Assets.xcassets` usan el mismo morado `#6D4AFF`.

El icono (`Assets.xcassets/AppIcon`) sale del isotipo que ya tenía la primera
versión Expo del proyecto (`assets/icon.png`), recortado y puesto sobre el
morado de marca en vez del fondo con las guías de diseño que traía ese PNG.
Los otros assets de esa carpeta (`splash-icon.png`, `android-icon-background.png`)
tienen esas mismas guías de cuadrícula incrustadas en el propio PNG, no
sirven como están: si quieres usarlos, pide un reexport limpio.

## Antes de publicar

- Dominio definitivo en `Nucleo/Config.swift` y `Xpersiva.entitlements`.
- Team ID en `web/.well-known/apple-app-site-association` (rama `web/v1`).
- Icono ya aplicado (ver "Marca" arriba); capturas de pantalla pendientes.

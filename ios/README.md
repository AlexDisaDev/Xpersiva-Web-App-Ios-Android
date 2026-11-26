# Xpersiva · iOS (rama `ios/v1`)

App nativa en **Swift 6 + SwiftUI**, iOS 17+. Lee las experiencias de la web
(`https://xpersiva.netlify.app/experiences.json`, rama `web/v1`), así que crear
una experiencia no requiere versión nueva.

`startsAt`/`endsAt` son la campaña (puede durar meses); `dailyOpensAt`/
`dailyClosesAt` son el horario del día dentro de ella, opcionales. Sin
horario diario, `estaEnMarcha()` (`Experiencias/Experiencia.swift`) marcaría
"en directo" las 24 horas mientras dure la campaña, así que el filtro "En
directo" del inicio no filtraría nada: con horario diario, solo lo hace
dentro de él, y además se enseña como dato ("10:00 – 20:00") en la tarjeta y
en la ficha.

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

**`c=giroscopio`:** `VolanteGiroscopio.swift` usa la gravedad de CoreMotion
(`CMDeviceMotion.gravity`) dentro del plano de la pantalla: se gira el móvil
en horizontal como un volante, e inclinarlo hacia delante o atrás no mueve el
coche. Esta pantalla gira a horizontal solo mientras está activa
(`OrientacionApp.swift` + `requestGeometryUpdate`); el resto de la app sigue
en vertical. Gas y freno quedan como dos botones táctiles, uno a cada lado.

Añadir una plantilla de control genuinamente nueva (un cuarto caso, no solo
elegir entre las que ya existen) sí sigue necesitando código nuevo en
`VistaMando.swift` y una nueva versión de la app.

## Marca

Sale de la rama `brand` (`brand/app-ready/`, los definitivos; `brand/v2-portal/`
solo se usó como referencia visual):

- Colores en `Nucleo/Marca.swift`: violeta `#6D4AFF`, cian `#42E3F2`, azul noche
  `#090B1E` y superficies claras (la app va siempre en modo claro,
  `UIUserInterfaceStyle: Light`). Los mismos que `ui/Tema.kt` en `android/v1`.
  Ahí está también `BotonPrincipal`, el botón violeta de "ESCANEAR QR".
- Icono: `Assets.xcassets/AppIcon` (`icon.png` de la marca, 1024 px, sin transparencia).
- Arranque: la pantalla de arranque del sistema es el color `FondoArranque`
  (azul noche), igual que el primer fotograma de `Recursos/startup-intro.mp4`.
  `App/IntroApertura.swift` reproduce ese vídeo una vez por arranque (sin sonido
  y sin cortar la música, tocar lo salta) y luego enseña la app. Con "Reducir
  movimiento" sale solo el símbolo (`SimboloArranque`) un momento. Un enlace del
  QR que abre la app espera a que acabe la intro y aterriza en su pantalla como siempre.
- Logo de cabecera: `LogoCabecera` (recorte de `logo-header.png`, para fondo claro).

## Antes de publicar

- Dominio definitivo en `Nucleo/Config.swift` y `Xpersiva.entitlements`.
- Team ID en `web/.well-known/apple-app-site-association` (rama `web/v1`).
- Icono ya aplicado (ver "Marca" arriba); capturas de pantalla pendientes.

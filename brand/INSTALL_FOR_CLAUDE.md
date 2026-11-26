# Guía para colocar la marca en la app

**Estado:** los archivos ya están instalados en `app/assets/` en la rama `brand`. `app/src/lib/StartupIntro.tsx` reproduce el vídeo y `app/src/app/_layout.tsx` lo muestra al arrancar iOS y Android. Esta guía conserva el mapa de archivos para mantenimiento.

Usa **`brand/app-ready/`** como carpeta de entrega. Sus nombres ya coinciden con los destinos de Expo; no hace falta adivinar qué archivo es cada icono. La carpeta `brand/v2-portal/` conserva los originales de diseño y las versiones anteriores.

## Copias exactas

| Origen en esta rama | Destino en la app | Uso |
| --- | --- | --- |
| `brand/app-ready/icon.png` | `app/assets/icon.png` | Icono principal de iOS y Expo (`expo.icon` en `app/app.json`). |
| `brand/app-ready/android-icon-background.png` | `app/assets/android-icon-background.png` | Fondo del icono adaptativo Android. |
| `brand/app-ready/android-icon-foreground.png` | `app/assets/android-icon-foreground.png` | Capa frontal del icono adaptativo Android. |
| `brand/app-ready/android-icon-monochrome.png` | `app/assets/android-icon-monochrome.png` | Icono monocromo Android. |
| `brand/app-ready/favicon.png` | `app/assets/favicon.png` | Favicon de la versión web; configurar `expo.web.favicon` si se usa. |
| `brand/app-ready/splash-icon.png` | `app/assets/splash-icon.png` | Imagen estática de respaldo para splash; opcional si el splash nativo es solo fondo. |
| `brand/app-ready/logo-header.png` | `app/assets/logo-header.png` | Logo horizontal sobre fondo claro; mostrar en la cabecera si se sustituye el texto actual. |
| `brand/app-ready/startup-symbol.png` | `app/assets/startup-symbol.png` | Símbolo transparente para una animación nativa alternativa o respaldo. |
| `brand/app-ready/startup-intro.mp4` | `app/assets/startup-intro.mp4` | Vídeo definitivo de apertura, 3,4 s, vertical y sin sonido. |

## Configuración que debe acompañar a los archivos

- Mantener las rutas de `expo.icon` y `expo.android.adaptiveIcon` apuntando a los nombres de la tabla.
- Usar `#090B1E` como fondo del splash nativo y del contenedor del vídeo. El primer y el último fotograma tienen ese color.
- El MP4 **no se reproduce solo por copiarlo** a `app/assets/`. Crear una pantalla o capa de arranque que reproduzca `startup-intro.mp4` una vez y, al terminar, muestre la ruta que ya se estaba abriendo. No interrumpir los enlaces profundos.
- No añadir el mockup `xpersiva-app-concept.png` como pantalla de la app: es solo una vista de diseño.
- Si el usuario tiene activada la reducción de movimiento, omitir el vídeo o mostrar una versión estática breve.
- Antes de usar APIs de Expo, consultar la documentación correspondiente al SDK indicado en `app/package.json` y seguir `app/AGENTS.md`.

## Mensaje breve para Claude

> Integra los archivos de `brand/app-ready/` en la app siguiendo la tabla de `brand/INSTALL_FOR_CLAUDE.md`. Usa `startup-intro.mp4` como vídeo de apertura antes de mostrar la pantalla real, con fondo `#090B1E`. Reproduce el vídeo una sola vez por arranque, respeta los enlaces profundos y la opción de reducir movimiento. Actualiza `app/app.json` para los iconos y verifica TypeScript, lint y una compilación de desarrollo. No uses los vídeos anteriores ni el mockup como pantalla real.

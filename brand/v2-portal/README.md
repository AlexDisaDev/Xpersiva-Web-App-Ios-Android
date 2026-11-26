# Xpersiva Portal — segunda propuesta

**Archivo recomendado para integrar antes de la app:** [`xpersiva-intro-para-app-suave.mp4`](xpersiva-intro-para-app-suave.mp4). Las otras versiones MP4 de esta carpeta documentan las iteraciones anteriores.

La X original se mantiene como núcleo. Un anillo dividido y en movimiento la convierte en una entrada visual a la experiencia. El cian añade energía y guía la atención; el violeta `#6D4AFF` conserva la relación con el producto actual. Sobre fondo oscuro, el símbolo gana presencia en vídeo y como icono de app. A tamaño pequeño, la X sigue siendo la forma dominante.

## Entregables

- `xpersiva-portal-app-icon.png`: icono de app, 1024 × 1024.
- `xpersiva-portal-symbol.svg`: símbolo vectorial para web y material publicitario.
- `xpersiva-portal-wordmark-light.png` y `xpersiva-portal-wordmark-dark.png`: marca horizontal para fondo claro y oscuro.
- `xpersiva-portal-android-foreground.png`, `xpersiva-portal-android-background.png`, `xpersiva-portal-android-monochrome.png`: capas del icono adaptativo.
- `xpersiva-portal-intro-vertical.mp4`: teaser vertical de 8,7 s, 1080 × 1920, H.264/AAC.
- `xpersiva-portal-video-cover.png`: portada.
- `xpersiva-app-concept.png`: propuesta visual de la pantalla inicial; no es una captura de la app implementada.
- `xpersiva-intro-para-app.mp4`: vídeo de 3,4 s para colocarlo antes de la app. Empieza y termina en `#090B1E`; el logo aparece con fade, gira dos veces y se desvanece. No incluye ninguna pantalla de la app.
- `xpersiva-intro-para-app-suave.mp4`: versión corregida para usar en la app, sin puntos en el anillo y con bordes suavizados mediante render a mayor resolución.
- `xpersiva-portal-startup-symbol.png`: símbolo transparente usado por la animación nativa de la app.

## Aplicación

Para probar el icono en el proyecto Expo, copia el icono principal a `app/assets/icon.png` y las tres capas Android a los archivos equivalentes de `app/assets/`. En `app/app.json`, el fondo del icono adaptativo debe ser `#090B1E`. El mockup muestra cómo podría trasladarse la identidad a la interfaz; implementarlo requiere cambios en el código de la app.

El vídeo conserva la secuencia de escanear y entrar de la primera propuesta. El nuevo cierre revela el portal, la X y el mensaje «Vive el evento desde dentro». Los bloques de la primera escena representan un QR de forma decorativa y no son escaneables.

Para regenerar: `python brand/generate_v2.py`.

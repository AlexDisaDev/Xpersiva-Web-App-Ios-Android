# Identidad inicial de Xpersiva

## Versión recomendada

La identidad más reciente está en [`v2-portal/`](v2-portal/). Para el inicio de la app, usa [`xpersiva-intro-para-app-suave.mp4`](v2-portal/xpersiva-intro-para-app-suave.mp4): tiene el logo con bordes suavizados, sin puntos decorativos, dos giros y fade de entrada y salida sobre `#090B1E`. El [`icono de app`](v2-portal/xpersiva-portal-app-icon.png) y el [`símbolo vectorial`](v2-portal/xpersiva-portal-symbol.svg) corresponden a esa misma versión. Los archivos de esta carpeta raíz conservan la primera propuesta.

La rama `brand` guarda los recursos y los scripts fuente. Las imágenes de revisión generadas durante el render se excluyen con `.gitignore`.

Basada en la documentación del repositorio [Xpersiva-Web-App-Ios-Android](https://github.com/AlexDisaDev/Xpersiva-Web-App-Ios-Android): experiencias inmersivas en eventos donde el usuario escanea un QR y usa el móvil como mando. Mantiene el violeta `#6D4AFF` que ya usa la aplicación. El símbolo reúne cuatro piezas en una X y deja un pequeño espacio central como referencia a la entrada a una experiencia.

## Archivos

- `xpersiva-symbol.svg` y `xpersiva-symbol.png`: símbolo, fondo transparente.
- `xpersiva-wordmark.svg` y `xpersiva-wordmark.png`: símbolo y nombre, fondo transparente. La variante PNG usa texto azul oscuro y está pensada para fondo claro.
- `xpersiva-app-icon.png`: icono cuadrado de 1024 × 1024 para iOS y otros usos.
- `xpersiva-android-foreground.png`, `xpersiva-android-background.png` y `xpersiva-android-monochrome.png`: capas para el icono adaptativo de Android.
- `xpersiva-splash-icon.png`: símbolo violeta con fondo transparente para pantalla de carga.
- `xpersiva-intro-vertical.mp4`: teaser vertical de 8,7 s, 1080 × 1920, H.264 y audio AAC. Listo para subir como Reel, Story o vídeo vertical.
- `xpersiva-video-cover.png`: portada del vídeo.

## Aplicación Expo

El repositorio ya apunta a archivos en `app/assets/`. Para usar la identidad, copia los PNG al proyecto con los nombres actuales:

| Archivo nuevo | Ruta en la app |
| --- | --- |
| `xpersiva-app-icon.png` | `app/assets/icon.png` |
| `xpersiva-android-foreground.png` | `app/assets/android-icon-foreground.png` |
| `xpersiva-android-background.png` | `app/assets/android-icon-background.png` |
| `xpersiva-android-monochrome.png` | `app/assets/android-icon-monochrome.png` |
| `xpersiva-splash-icon.png` | `app/assets/splash-icon.png` |

Para que la capa de Android combine correctamente, cambia en `app/app.json` los dos valores de fondo del icono adaptativo a `#6D4AFF`: `android.adaptiveIcon.backgroundColor` y, si se decide usar una imagen de fondo, conserva `backgroundImage` apuntando al PNG violeta.

El vídeo es una presentación conceptual del producto descrito en el README. Los cuadros que parecen un QR son decorativos y no se pueden escanear.

Para regenerar todo: `python brand/generate.py` (requiere Pillow, NumPy e imageio-ffmpeg).

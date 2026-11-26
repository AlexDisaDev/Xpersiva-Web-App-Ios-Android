# Propuesta visual: mando F1 con giroscopio

Diseño de pantalla. La app implementa una adaptación vertical en `app/src/app/play.tsx` para su orientación actual: el móvil se gira como volante y los pedales se pulsan con los pulgares. El mockup horizontal queda como referencia para una futura orientación horizontal.

## Archivos

- `mando-f1-horizontal.png`: propuesta principal, 1920 × 1080.
- `mando-f1-vertical.png`: adaptación vertical, 1080 × 1920.
- `generate.py`: fuente reproducible de ambas imágenes.

## Intención de interacción

- **Giro:** el móvil funciona como volante. La pantalla vertical también permite deslizar la zona central si no se activa el sensor.
- **Freno:** gran zona izquierda, roja y rotulada. Actúa solo mientras se mantiene pulsada.
- **Acelerar:** gran zona derecha, verde y rotulada. Actúa solo mientras se mantiene pulsada.
- **Centrar volante:** recalibra la posición neutral antes de jugar o cuando el usuario cambia la postura.
- **Conexión y salida:** visibles pero fuera de las zonas principales de control.

El protocolo de la app solo envía `steer`, `throttle` y `brake`; por eso la pantalla no inventa velocidad, marcha ni tiempos de vuelta. La implementación usa `expo-sensors` y conserva ese protocolo.

## Colores

Fondo `#090B1E`, violeta `#6D4AFF`, cian `#42E3F2`, freno `#FF6878`, acelerar `#39DEB4`. Los pedales tienen texto e iconos además del color para distinguirlos con facilidad.

# Propuesta visual: mando F1 con giroscopio

Diseño de pantalla, **sin implementación en la app**. La opción recomendada es horizontal: se sostiene el móvil con ambas manos, se inclina para girar y se pulsan los pedales con los pulgares. La versión vertical muestra cómo se adapta la misma jerarquía a la orientación actual de la app.

## Archivos

- `mando-f1-horizontal.png`: propuesta principal, 1920 × 1080.
- `mando-f1-vertical.png`: adaptación vertical, 1080 × 1920.
- `generate.py`: fuente reproducible de ambas imágenes.

## Intención de interacción

- **Giro:** el móvil funciona como volante. El círculo central comunica la inclinación y el estado neutral; no es una zona táctil de dirección.
- **Freno:** gran zona izquierda, roja y rotulada. Actúa solo mientras se mantiene pulsada.
- **Acelerar:** gran zona derecha, verde y rotulada. Actúa solo mientras se mantiene pulsada.
- **Centrar volante:** recalibra la posición neutral antes de jugar o cuando el usuario cambia la postura.
- **Conexión y salida:** visibles pero fuera de las zonas principales de control.

El protocolo actual de la app solo envía `steer`, `throttle` y `brake`; por eso el diseño no inventa velocidad, marcha ni tiempos de vuelta. La implementación del giroscopio requeriría cambiar la forma de calcular `steer` en `app/src/app/play.tsx`. Esta carpeta no modifica esa pantalla ni el protocolo.

## Colores

Fondo `#090B1E`, violeta `#6D4AFF`, cian `#42E3F2`, freno `#FF6878`, acelerar `#39DEB4`. Los pedales tienen texto e iconos además del color para distinguirlos con facilidad.

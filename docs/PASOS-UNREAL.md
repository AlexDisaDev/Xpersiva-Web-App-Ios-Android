# Generar el QR de Xpersiva en el juego (plugin websocket)

No hay que tocar el plugin: `Start Server` pinta el QR de la URL que le pases.

## 1. Arrancar el servidor con la URL de Xpersiva

En el Blueprint del GameMode, en **BeginPlay**:

1. **Get Game Instance Subsystem** → clase **WebSocket Server**.
2. Construir la URL con **Format Text** (o `Append`):
   ```
   https://xpersiva.netlify.app/j/{Codigo}?h={IP}:{Puerto}
   ```
   - `Codigo`: el de esta experiencia, por ejemplo `F1`. Mejor como variable editable.
   - `IP`: nodo **Get Local Host Address** del plugin.
   - `Puerto`: `8080`.
3. **Start Server** con `Port = 8080` y `Public URL = esa URL`. Devuelve `false`
   si el puerto está ocupado.

## 2. Pintar el QR

4. **Bind** a **On QR Code Generated**: llega `QR Code Texture`.
5. En el widget: **Set Brush from Texture** sobre un `Image`.

(Si alguna vez quieres otro QR sin reiniciar el servidor: **Generate QR Code Texture**
con cualquier URL.)

## 3. Dar la bienvenida al móvil

6. **On Client Connected** → **Send To Client** con `Client ID` y el texto:
   ```
   {"type":"joined","id":"<ClientID>"}
   ```
   (Format Text: `{"type":"joined","id":"{id}"}`). Sin esto el móvil se queda
   en "esperando a la experiencia".

## 4. Recibir lo que manda el móvil

7. **On Web Socket Signal Received** → llega `Client ID` y el JSON.
   Parsearlo (nodos Json Blueprint Utilities) y mirar `type`:
   - `hello`: trae `name` → spawnear o asignar el coche a ese `Client ID` y
     mostrar el nombre.
   - `input`: trae `steer` (-1 a 1), `throttle` y `brake` (0 o 1) → al coche de
     ese `Client ID` (`SetSteeringInput`, `SetThrottleInput`, `SetBrakeInput`).
8. **On Client Disconnected** → quitar o parar el coche de ese `Client ID`.

## Requisitos en el evento

- El PC y los móviles en la **misma Wi-Fi**.
- Permitir el puerto 8080 en el Firewall de Windows (la primera vez Windows lo pregunta).
- Esto es para la build de **Windows**. En la build web (Wasm) el navegador no puede
  hacer de servidor, así que ahí el mando necesita un relay externo.

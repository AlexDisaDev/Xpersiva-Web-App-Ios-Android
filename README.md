# Xpersiva

App generalista de "escanea y juega" para experiencias inmersivas en eventos,
más su web. Android e iOS con un solo código (Expo SDK 58).

- **`app/`:** buscador de experiencias, ordenadas por cercanía si el usuario
  da permiso de ubicación (solo al pulsar el botón; si no, por fecha).
- **`web/`:** la web de Xpersiva. De ahí lee la app las experiencias, así que
  **crear una experiencia nueva no requiere sacar versión de la app** ni pasar revisión.

## Crear una experiencia nueva

1. Añadir una entrada a `web/experiences.json`:
   ```json
   { "id": "mi-evento-2027", "name": "Nombre", "brand": "Marca", "venue": "Recinto",
     "city": "Ciudad", "lat": 40.41, "lng": -3.70,
     "startsAt": "2027-03-01T10:00:00+01:00", "endsAt": "2027-03-03T20:00:00+01:00",
     "description": "Texto corto", "tags": ["carreras"] }
   ```
2. Hacer push: si el sitio de Netlify está conectado a este repo (`netlify.toml` publica `web/`), se actualiza solo.
3. Ya aparece en la app, y tiene su propia página en `https://<web>/e/mi-evento-2027`.

## URLs de la web

| URL | Qué es |
| --- | --- |
| `/` | Portada de Xpersiva con todas las experiencias |
| `/e/<id>` | Página de una experiencia (si tienes la app, abre su ficha en la app) |
| `/j/<CODIGO>?h=<IP>:<PUERTO>` | Destino del QR del juego: abre la app o manda a la tienda |
| `/experiences.json` | La lista que lee la app |
| `/.well-known/*` | Lo que hace que iOS y Android abran la app con estos enlaces |

## QR del juego: qué pasa al escanearlo

- **Tiene la app:** se abre directamente en la pantalla de entrada de esa experiencia.
- **No la tiene:** se abre `/j/...`, que en 3 segundos le lleva a App Store o Google Play
  según su móvil, con la opción "Jugar sin instalar" (el mando web que sirve el plugin).
- **Tras instalar en Android**, Google Play le pasa el código a la app (pendiente de
  leerlo en la app). En iOS vuelve a escanear el QR.

## Protocolo con el juego (plugin websocket)

```
app → juego  {"type":"hello","name":"Ana","code":"F1"}
juego → app  {"type":"joined","id":"<ClientID>"}
app → juego  {"type":"input","steer":-1..1,"throttle":0|1,"brake":0|1}   (≤30/s, solo si cambia)
```

Pasos en Unreal: ver [`docs/PASOS-UNREAL.md`](docs/PASOS-UNREAL.md).

## Probar la app

```
cd app
npm install
npx eas-cli@latest build --profile development --platform android   # o ios, sin Mac
```

No funciona en Expo Go (cámara y enlaces necesitan build propia). Mientras no
esté publicada, el salto automático QR → app no se puede probar; el escáner de
dentro de la app sí.

## Antes de publicar

- Dominio definitivo (ahora `xpersiva.netlify.app`) en `app/src/lib/config.ts` y `app/app.json`.
- `TEAMID` en `web/.well-known/apple-app-site-association`, huella SHA-256 en
  `web/.well-known/assetlinks.json`, y enlaces reales de tienda en `web/site.js`.
- Icono y capturas.
- Login con Google: aparcado (26 nov 2026), de momento solo nombre.

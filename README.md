# Xpersiva · Web (rama `web/v1`)

Web estática de Xpersiva. La publica Netlify desde esta rama (`netlify.toml` → carpeta `web/`).

| URL | Qué es |
| --- | --- |
| `/` | Portada con todas las experiencias |
| `/e/<id>` | Página de una experiencia (si tienes la app, abre su ficha en la app) |
| `/j/<CODIGO>?h=<IP>:<PUERTO>` | Destino del QR del juego: abre la app o manda a la tienda |
| `/experiences.json` | La lista que leen las apps de iOS y Android |
| `/.well-known/*` | Lo que hace que iOS y Android abran la app con estos enlaces |

## Crear una experiencia nueva

Añadir una entrada a `web/experiences.json` y hacer push a `web/v1`:

```json
{ "id": "mi-evento-2027", "name": "Nombre", "brand": "Marca", "venue": "Recinto",
  "city": "Ciudad", "lat": 40.41, "lng": -3.70,
  "startsAt": "2027-03-01T10:00:00+01:00", "endsAt": "2027-03-03T20:00:00+01:00",
  "description": "Texto corto", "tags": ["carreras"] }
```

Aparece en las dos apps sin publicar versión nueva, y tiene su página en `/e/mi-evento-2027`.

## Ramas del proyecto

- `web/v1`: esta web.
- `ios/v1`: app de iOS en Swift + SwiftUI.
- `android/v1`: app de Android en Kotlin + Jetpack Compose.

Las tres comparten solo el contrato: el formato de `experiences.json`, las rutas `/j/` y `/e/`, y el protocolo con el juego.

## Antes de publicar las apps

- `TEAMID` en `web/.well-known/apple-app-site-association`.
- Huella SHA-256 de la firma de Google Play en `web/.well-known/assetlinks.json`.
- Enlaces reales de tienda en `web/site.js`.

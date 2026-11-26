/**
 * Web de Xpersiva. De aquí salen las experiencias y los enlaces de los QR.
 * Cambiar a dominio propio ANTES de publicar en las tiendas: queda grabado
 * en la app (app.json → associatedDomains / intentFilters).
 */
export const SITE_URL = (process.env.EXPO_PUBLIC_SITE_URL || 'https://xpersiva.netlify.app').replace(/\/$/, '');
export const SITE_HOST = new URL(SITE_URL).host;
export const APP_SCHEME = 'xpersiva';

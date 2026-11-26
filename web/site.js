'use strict';
// Configuración común de la web de Xpersiva. Cambiar aquí los enlaces de tienda
// cuando la app esté publicada.
window.XPERSIVA = {
  appStore: 'https://apps.apple.com/app/idAPPSTORE_ID',
  googlePlay: 'https://play.google.com/store/apps/details?id=app.xpersiva.player',
  platform: /iPhone|iPad|iPod/i.test(navigator.userAgent) ? 'ios'
          : /Android/i.test(navigator.userAgent) ? 'android' : 'desktop',

  async experiences() {
    const res = await fetch('/experiences.json', { cache: 'no-cache' });
    const body = await res.json();
    const now = Date.now();
    return (body.experiences || []).filter((e) => !e.endsAt || Date.parse(e.endsAt) >= now);
  },

  isLive(e) {
    const now = Date.now();
    return (!e.startsAt || Date.parse(e.startsAt) <= now) && (!e.endsAt || now <= Date.parse(e.endsAt));
  },

  // Botones de tienda: en el móvil, solo el de su sistema.
  storeButtons(container, extraQuery) {
    const p = this.platform;
    const items = [];
    if (p !== 'android') items.push(['App Store', this.appStore]);
    if (p !== 'ios') items.push(['Google Play', this.googlePlay + (extraQuery || '')]);
    for (const [label, href] of items) {
      const a = document.createElement('a');
      a.className = 'btn';
      a.href = href;
      a.textContent = 'Descargar en ' + label;
      container.appendChild(a);
    }
  },

  text(el, value) { el.textContent = value == null ? '' : String(value); },
};

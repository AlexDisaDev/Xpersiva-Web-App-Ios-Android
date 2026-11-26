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

  // En marcha: dentro de startsAt/endsAt (la campaña, puede durar meses) y, si
  // tiene horario diario, dentro de él. Sin dailyOpensAt/dailyClosesAt una
  // experiencia con endsAt lejano saldría "AHORA" permanentemente las 24h.
  isLive(e) {
    const now = new Date();
    if (e.startsAt && Date.parse(e.startsAt) > now.getTime()) return false;
    if (e.endsAt && now.getTime() > Date.parse(e.endsAt)) return false;
    if (e.dailyOpensAt && minutosDelDia(now) < minutosDeHora(e.dailyOpensAt)) return false;
    if (e.dailyClosesAt && minutosDelDia(now) > minutosDeHora(e.dailyClosesAt)) return false;
    return true;
  },

  // "10:00 – 20:00", o null si la experiencia no tiene horario diario.
  horario(e) {
    if (!e.dailyOpensAt && !e.dailyClosesAt) return null;
    return [e.dailyOpensAt, e.dailyClosesAt].filter(Boolean).join(' – ');
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

function minutosDelDia(fecha) { return fecha.getHours() * 60 + fecha.getMinutes(); }
function minutosDeHora(hhmm) {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + (m || 0);
}

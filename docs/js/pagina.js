/* Coffer · página de descarga
   1. Versiones reales desde GitHub Releases (oficial = última release, staging = última pre-release).
   2. Selector de estados del zorro.
   3. Entradas y salidas de cada sección con el scroll.
   4. Movimiento continuo: encabezado, logo, cifras flotantes y teléfonos que se inclinan y recorren su pantalla. */
(function () {
  const root = document.documentElement;
  root.classList.add('motion');
  window.cofferReady = true;

  /* ---------- 1. Versiones ---------- */
  const repo = (root.dataset.repo || '').trim();
  const configured = /^[\w.-]+\/[\w.-]+$/.test(repo) && !repo.startsWith('TU-USUARIO');

  const releasesUrl = `https://github.com/${repo}/releases`;
  if (configured) {
    document.querySelectorAll('[data-releases]').forEach((a) => (a.href = releasesUrl));
    document.querySelectorAll('[data-official="link"]').forEach((a) => (a.href = `${releasesUrl}/latest/download/coffer.apk`));
    document.querySelectorAll('[data-staging="link"]').forEach((a) => (a.href = releasesUrl));
  }

  const fmtDate = (iso) =>
    new Date(iso).toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
  const fmtSize = (bytes) =>
    (bytes / 1048576).toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + ' MB';
  const apkOf = (r) => (r.assets || []).find((a) => /\.apk$/i.test(a.name));
  const cleanTag = (t) => String(t).replace(/^v(?=\d)/i, '');

  function fill(kind, release) {
    const apk = apkOf(release);
    if (!apk) return;
    const version = cleanTag(release.tag_name || release.name);
    document.querySelectorAll(`[data-${kind}="link"]`).forEach((a) => {
      a.href = apk.browser_download_url;
      a.setAttribute('download', '');
    });
    if (kind === 'official') {
      const m = document.querySelector('[data-official="meta"]');
      if (m) m.textContent = `Coffer ${version} · ${fmtDate(release.published_at)} · ${fmtSize(apk.size)}`;
    } else {
      const v = document.querySelector('[data-staging="version"]');
      if (v) v.textContent = `Versión ${version}, del ${fmtDate(release.published_at)}. `;
    }
  }

  if (configured) {
    fetch(`https://api.github.com/repos/${repo}/releases?per_page=30`, { headers: { Accept: 'application/vnd.github+json' } })
      .then((r) => (r.ok ? r.json() : []))
      .then((list) => {
        const live = list.filter((r) => !r.draft && apkOf(r));
        const official = live.find((r) => !r.prerelease);
        const staging = live.find((r) => r.prerelease);
        if (official) fill('official', official);
        if (staging) fill('staging', staging);
      })
      .catch(() => { /* sin red o límite de la API: quedan los enlaces a la página de versiones */ });
  }

  /* ---------- 2. Zorro ---------- */
  // Mensajes de la app (componente «Estado del zorro» en Figma); los de descanso y bienvenida no tienen texto allí.
  const MESSAGES = {
    normal: ['Todo en orden', 'Tus gastos van como de costumbre este mes.'],
    good: ['¡Vas muy bien!', 'Llevas 8,4 % más que en septiembre. Sigue así.'],
    goal: ['¡Mes cerrado en positivo!', 'Ahorraste $650.000 en octubre.'],
    attentive: ['Ojo con el arriendo', 'Vence mañana y son $900.000.'],
    worried: ['Gastas más de lo que entra', 'Gastos $2.480.000 · ingresos $2.100.000 este mes.'],
    debt: ['Tu saldo está en rojo', 'Debes $1.630.000 en la tarjeta Visa. Prioriza ese pago.'],
    resting: ['Todavía no hay movimientos', 'Registra el primero con el botón +.'],
  };
  const pickerFox = document.querySelector('[data-fox-picker]');
  const msg = document.querySelector('.foxsec__msg');
  const title = document.querySelector('[data-fox-title]');
  const body = document.querySelector('[data-fox-body]');
  document.querySelectorAll('.picker input').forEach((input) => {
    input.addEventListener('change', () => {
      const s = input.value;
      const fox = (window.cofferFoxes || []).find((f) => f.el === pickerFox);
      if (fox) fox.set(s);
      if (MESSAGES[s]) {
        title.textContent = MESSAGES[s][0];
        body.textContent = MESSAGES[s][1];
        msg.classList.remove('swap');
        void msg.offsetWidth;
        msg.classList.add('swap');
      }
    });
  });

  /* ---------- 3. Entradas y salidas ---------- */
  // Cada pieza entra al aparecer y sale por el borde por donde se va: arriba si el scroll la deja atrás, abajo si vuelves.
  const reveals = Array.from(document.querySelectorAll('.reveal'));
  const io = new IntersectionObserver((entries) => {
    // Escalonado dentro de cada bloque que entra a la vez: 70 ms entre piezas
    let i = 0;
    entries.forEach((e) => {
      const el = e.target;
      if (e.isIntersecting) {
        el.style.setProperty('--d', `${i++ * 70}ms`);
        el.classList.remove('above');
        el.classList.add('in');
      } else if (el.classList.contains('in')) {
        el.classList.toggle('above', e.boundingClientRect.top < (e.rootBounds ? e.rootBounds.top : 0));
        el.classList.remove('in');
      }
    });
  }, { rootMargin: '-10% 0px -8% 0px', threshold: 0 });
  reveals.forEach((el) => io.observe(el));

  /* ---------- 4. Movimiento con el scroll y el puntero ---------- */
  const bar = document.querySelector('.bar');
  const syncBar = () => bar && bar.classList.toggle('is-stuck', window.scrollY > 4);
  window.addEventListener('scroll', syncBar, { passive: true });
  syncBar();

  /* ---------- 5. Zorros ---------- */
  // Cada zorro se arma cuando su lugar se acerca a la vista y actúa cuando entra; los de una sola función
  // (asomarse, sobresalto, celebrar) se reinician solo después de haber salido, para que no salten a la vista.
  const B = window.FoxBehaviors;
  const spots = Array.from(document.querySelectorAll('[data-fox-spot]')).filter((el) => el.offsetParent !== null);
  if (B && spots.length) {
    const made = new Map();
    const peekOpts = (el) => { const w = el.querySelector('.phone').offsetWidth; const size = Math.round(w * 0.72); return { size, x: Math.round(w * 0.5 - size * 0.53) }; };
    const make = (el) => {
      const kind = el.dataset.foxSpot;
      if (kind === 'look') {
        const fox = B.look(el);
        // El zorro del pie mira el puntero en toda la página, no solo sobre su caja
        window.addEventListener('pointermove', (e) => el.dispatchEvent(new PointerEvent('pointermove', { clientX: e.clientX, clientY: e.clientY })), { passive: true });
        return fox;
      }
      return kind === 'peek' ? B.peek(el, peekOpts(el)) : B[kind](el);
    };
    const once = { peek: true, startle: true, celebrate: true };
    const io2 = new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        const el = e.target;
        const kind = el.dataset.foxSpot;
        if (!made.has(el) && e.intersectionRatio > 0) made.set(el, { fox: make(el), played: false });
        const m = made.get(el);
        if (!m) return;
        if (e.intersectionRatio >= 0.5 && !m.played) {
          m.played = true;
          setTimeout(() => m.fox.run(), kind === 'rest' ? 900 : 350);
        } else if (e.intersectionRatio === 0 && once[kind]) {
          m.played = false;
        }
      });
    }, { threshold: [0, 0.5] });
    spots.forEach((el) => io2.observe(el));

    // Asomarse se mide en píxeles: si cambia el ancho, se vuelve a armar a la medida del teléfono
    let rw = window.innerWidth;
    window.addEventListener('resize', () => {
      if (Math.abs(window.innerWidth - rw) < 40) return;
      rw = window.innerWidth;
      made.forEach((m, el) => {
        if (el.dataset.foxSpot !== 'peek') return;
        el.querySelectorAll('.peek-fox, .peek-paws').forEach((n) => n.remove());
        m.fox = B.peek(el, peekOpts(el));
        m.played = false;
      });
    });

    // Celebra también cuando el puntero llega al botón de descarga de su tarjeta
    const cel = document.querySelector('[data-fox-spot="celebrate"]');
    const celBtn = cel && cel.parentElement.querySelector('.btn');
    let celAt = 0;
    if (celBtn) celBtn.addEventListener('pointerenter', () => {
      const m = made.get(cel);
      if (m && Date.now() - celAt > 3400) { celAt = Date.now(); m.fox.run(); }
    });
  }

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
  const smooth = (a, b, v) => { const t = clamp((v - a) / (b - a), 0, 1); return t * t * (3 - 2 * t); };
  // Posición en el documento sin contar transformaciones, para que el propio movimiento no se retroalimente
  const docTop = (el) => { let t = 0; for (let n = el; n; n = n.offsetParent) t += n.offsetTop; return t; };

  const hero = document.querySelector('.hero');
  const logo = document.querySelector('.hero__logo');
  const tags = Array.from(document.querySelectorAll('.tag')).map((el) => ({ el, depth: +el.dataset.depth || 16, x: 0, y: 0 }));
  // data-tilt va en el teléfono o en su envoltorio cuando lleva un zorro encima (así el zorro se mueve con él)
  const phones = Array.from(document.querySelectorAll('[data-tilt]')).map((el) => ({
    el, frame: el.classList.contains('phone') ? el : el.querySelector('.phone'), img: el.querySelector('.phone img'),
    speed: el.classList.contains('phone--b') ? 1.7 : 1,
    top: 0, h: 0, travel: 0, tilt: 0, y: 0, shot: 0,
  }));
  const pointer = { x: 0, y: 0 };
  let heroH = 0, vh = 0, small = false, logoK = 0, lastY = window.scrollY, vel = 0;
  // Todo lo que se desplaza cae en píxeles físicos enteros: una fracción obliga a remuestrear y la captura se ve borrosa
  const snap = (v) => { const r = window.devicePixelRatio || 1; return Math.round(v * r) / r; };

  const measure = () => {
    vh = window.innerHeight;
    small = window.innerWidth <= 860;
    heroH = hero ? hero.offsetHeight : 0;
    phones.forEach((p) => {
      p.top = docTop(p.el);
      p.h = p.el.offsetHeight;
      const pad = parseFloat(getComputedStyle(p.frame).paddingTop) || 0;
      p.travel = p.img ? Math.max(0, p.img.offsetHeight - (p.frame.offsetHeight - pad * 2)) : 0;
    });
  };

  // Cada valor persigue su objetivo con un poco de inercia (amortiguado, sin rebote) y el ciclo se apaga al asentarse
  const follow = (cur, target, k = 0.14) => (Math.abs(target - cur) < 0.02 ? target : cur + (target - cur) * k);
  let running = false;
  const frame = () => {
    const sy = window.scrollY;
    let moving = false;
    const step = (cur, target, k) => { const n = follow(cur, target, k); if (n !== target) moving = true; return n; };

    // El logo se aleja un poco mientras la primera pantalla sale de vista
    if (logo && heroH) {
      logoK = step(logoK, clamp(sy / (heroH * 0.75), 0, 1), 0.2);
      logo.style.scale = (1 - 0.06 * logoK).toFixed(4);
      logo.style.opacity = (1 - 0.7 * logoK).toFixed(3);
    }

    // Cifras: siguen al puntero según su profundidad y suben más rápido que el scroll
    tags.forEach((t) => {
      t.x = step(t.x, -pointer.x * t.depth, 0.08);
      t.y = step(t.y, -pointer.y * t.depth * 0.6 - sy * t.depth / 90, 0.12);
      t.el.style.translate = `${t.x.toFixed(2)}px ${t.y.toFixed(2)}px`;
    });

    // Teléfonos: se inclinan según la velocidad del scroll y se enderezan del todo al detenerse,
    // para que en reposo la captura quede plana y nítida; la pantalla se recorre por dentro
    vel = follow(vel, sy - lastY, 0.25);
    lastY = sy;
    if (vel !== 0) moving = true;
    const lean = clamp(vel * 0.32, -7, 7) * (small ? 0.6 : 1);
    phones.forEach((p) => {
      const center = p.top + p.h / 2 - sy;
      const d = clamp((center - vh / 2) / vh, -1.2, 1.2);
      const tilt = lean;
      const y = d * 36 * p.speed;
      const q = smooth(0.2, 0.85, (sy + vh - p.top) / (vh + p.h));
      p.tilt = step(p.tilt, tilt, 0.12);
      p.y = step(p.y, y, 0.12);
      p.shot = step(p.shot, q * p.travel, 0.1);
      p.el.style.translate = `0 ${snap(p.y)}px`;
      p.el.style.rotate = Math.abs(p.tilt) < 0.05 ? 'none' : `x ${p.tilt.toFixed(3)}deg`;
      if (p.img) p.img.style.transform = `translateY(${snap(-p.shot)}px)`;
    });

    running = moving;
    if (running) requestAnimationFrame(frame);
  };
  const kick = () => { if (!running) { running = true; requestAnimationFrame(frame); } };

  window.addEventListener('scroll', kick, { passive: true });
  window.addEventListener('resize', () => { measure(); kick(); });
  if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
    window.addEventListener('pointermove', (e) => {
      pointer.x = (e.clientX / window.innerWidth - 0.5) * 2;
      pointer.y = (e.clientY / window.innerHeight - 0.5) * 2;
      kick();
    }, { passive: true });
  }
  // Las capturas cargan tarde (lazy): al llegar cambian cuánto hay que recorrer
  phones.forEach((p) => p.img && p.img.addEventListener('load', () => { measure(); kick(); }));
  window.addEventListener('load', () => { measure(); kick(); });
  measure();
  kick();
})();

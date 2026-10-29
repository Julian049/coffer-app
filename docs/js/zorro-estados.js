/* Zorro de Coffer en la web.
   Port de icons/mascot-animated/README.md (§3 capas, §4 animaciones) con Web Animations API.
   Lienzo virtual 1024×1024; posiciones en % del lienzo; solo transform y opacidad. */
(function () {
  const M = window.FOX_MANIFEST;
  if (!M) return;

  const BASE = 'img/zorro/capas/';
  const C = M.canvas;
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');

  const io = 'ease-in-out';
  const pct = (v) => (v / C) * 100 + '%';

  // Duración del latido de las rayitas por estado (§4.4 pulse)
  const PULSE = { goal: 1600, good: 2000, welcome: 1400, attentive: 2000 };
  // Respiración del grupo body (§4.3)
  const BREATH = {
    debt: { d: 2300, x: 1.006, y: 1.014 },
    resting: { d: 4800, x: 1.012, y: 1.026 },
  };
  const DAY_DELAY = { day1: 350, day2: 480, day4: 610, day5: 740, day6: 870 };
  const Z_DELAY = { z1: 0, z2: 600, z3: 1200 };

  // Fotogramas clave con curva por tramo: [[offset, props], ...]
  function kf(frames, easing) {
    return frames.map(([offset, p]) => Object.assign({ offset, easing: easing || io }, p));
  }
  const rot = (frames) => kf(frames.map(([o, d]) => [o, { transform: `rotate(${d}deg)` }]));

  // Cada clave devuelve una lista de [destino, keyframes, opciones]; destino 'img' gira o escala
  // con el pivote de la capa; 'wrap' se desplaza en % del lienzo.
  const ANIMS = {
    wave: () => [['img', rot([[0, 0], [0.1, -9], [0.21, 2], [0.32, -8], [0.43, 1], [0.52, 0], [1, 0]]), { duration: 3800, delay: 500, iterations: Infinity }]],
    toast: () => [['img', rot([[0, 0], [0.5, 0], [0.62, -8], [0.7, -4], [0.78, -7], [0.9, 0], [1, 0]]), { duration: 3600, delay: 600, iterations: Infinity }]],
    blink: () => [['img', kf([[0, { transform: 'scaleY(1)' }], [0.9, { transform: 'scaleY(1)' }], [0.92, { transform: 'scaleY(0.1)' }], [0.94, { transform: 'scaleY(1)' }], [1, { transform: 'scaleY(1)' }]], 'linear'), { duration: 4600, delay: 1200, iterations: Infinity }]],
    floatZ: (l) => {
      const delay = Z_DELAY[l.layer] || 0;
      return [
        ['wrap', [{ transform: 'translate(0%, 2%)' }, { transform: 'translate(1.5%, -5%)' }], { duration: 3600, delay, iterations: Infinity, easing: 'ease-out', fill: 'backwards' }],
        ['img', kf([[0, { opacity: 0, transform: 'scale(0.8)' }], [0.25, { opacity: 1, transform: 'scale(0.8625)' }], [0.8, { opacity: 1, transform: 'scale(1)' }], [1, { opacity: 0, transform: 'scale(1.05)' }]], 'linear'), { duration: 3600, delay, iterations: Infinity, easing: 'ease-out', fill: 'backwards' }],
      ];
    },
    twinkle: (l) => [['img', kf([[0, { opacity: 0.55, transform: 'scale(0.82) rotate(-6deg)' }], [0.5, { opacity: 1, transform: 'scale(1.06) rotate(4deg)' }], [1, { opacity: 0.55, transform: 'scale(0.82) rotate(-6deg)' }]]), { duration: 2200, delay: l.layer === 'sparkle2' ? -1100 : 0, iterations: Infinity }]],
    pulse: (l, state) => [['img', kf([[0, { opacity: 0.35, transform: 'scale(0.94)' }], [0.5, { opacity: 1, transform: 'scale(1.04)' }], [1, { opacity: 0.35, transform: 'scale(0.94)' }]]), { duration: PULSE[state] || 2000, iterations: Infinity }]],
    // La gota cae con ease-in a propósito: así lo pide el handoff del PO (§4.4 drip)
    drip: () => [ // taste-skill-ignore
      ['img', kf([[0, { opacity: 0 }], [0.15, { opacity: 0 }], [0.3, { opacity: 1 }], [0.8, { opacity: 1 }], [1, { opacity: 0 }]], 'ease-in'), { duration: 3400, iterations: Infinity }],
      ['wrap', kf([[0, { transform: 'translateY(-1%)' }], [0.15, { transform: 'translateY(-1%)' }], [0.3, { transform: 'translateY(0%)' }], [0.8, { transform: 'translateY(2.4%)' }], [1, { transform: 'translateY(3.4%)' }]], 'ease-in'), { duration: 3400, iterations: Infinity }],
    ],
    daySweep: (l) => [['img', kf([[0, { opacity: 0 }], [0.45, { opacity: 1 }], [1, { opacity: 0 }]]), { duration: 500, delay: DAY_DELAY[l.layer] || 0, fill: 'both' }]],
    dueDay: () => [['img', [{ offset: 0, opacity: 0, transform: 'scale(1)' }, { offset: 0.4, opacity: 1, transform: 'scale(1.18)' }, { offset: 1, opacity: 1, transform: 'scale(1)' }], { duration: 600, delay: 1100, easing: 'cubic-bezier(0.3, 1.6, 0.6, 1)', fill: 'both' }]],
  };

  // Pose de reposo para «reducir movimiento» (§4.5)
  function restPose(l, img) {
    if (l.anim === 'daySweep') img.style.opacity = '0';
  }

  function build(state) {
    const layers = M.states[state];
    const stage = document.createElement('div');
    stage.className = 'fox__stage';
    const groups = {};
    const plan = [];

    for (const l of layers) {
      let parent = stage;
      if (l.group === 'body' || l.group === 'calendar') {
        if (!groups[l.group]) {
          const g = document.createElement('div');
          g.className = 'fox__group';
          const [px, py] = M.groupPivots[l.group];
          g.style.transformOrigin = `${pct(px)} ${pct(py)}`;
          stage.appendChild(g);
          groups[l.group] = g;
        }
        parent = groups[l.group];
      }
      const wrap = document.createElement('div');
      wrap.className = 'fox__layer';
      const img = document.createElement('img');
      img.src = BASE + l.drawable + '.webp';
      img.alt = '';
      img.decoding = 'async';
      img.draggable = false;
      img.style.left = pct(l.x);
      img.style.top = pct(l.y);
      img.style.width = pct(l.w);
      img.style.height = pct(l.h);
      img.style.transformOrigin = `${l.pivotFraction[0] * 100}% ${l.pivotFraction[1] * 100}%`;
      wrap.appendChild(img);
      parent.appendChild(wrap);
      restPose(l, img);
      if (l.anim && ANIMS[l.anim]) plan.push([l, wrap, img]);
    }
    return { stage, groups, plan };
  }

  function animate(state, built) {
    const out = [];
    const b = BREATH[state] || { d: 3600, x: 1.006, y: 1.014 };
    if (built.groups.body) {
      out.push(built.groups.body.animate(
        [{ transform: 'scale(1, 1)' }, { transform: `scale(${b.x}, ${b.y})` }, { transform: 'scale(1, 1)' }],
        { duration: b.d, iterations: Infinity, easing: io }
      ));
    }
    if (built.groups.calendar) {
      out.push(built.groups.calendar.animate(
        rot([[0, 0], [0.72, 0], [0.76, -4], [0.8, 3.5], [0.84, -2.5], [0.88, 1.5], [0.92, 0], [1, 0]]),
        { duration: 4500, delay: 1600, iterations: Infinity }
      ));
    }
    for (const [l, wrap, img] of built.plan) {
      for (const [target, frames, opts] of ANIMS[l.anim](l, state)) {
        out.push((target === 'wrap' ? wrap : img).animate(frames, opts));
      }
    }
    return out;
  }

  class Fox {
    constructor(el) {
      this.el = el;
      this.state = null;
      this.anims = [];
      this.visible = false;
      this.entered = false;
      this.set(el.dataset.fox || 'normal');
      this.observe();
    }

    set(state) {
      if (!M.states[state] || state === this.state) return;
      const prev = this.el.querySelector('.fox__stage');
      this.state = state;
      const built = build(state);
      this.el.appendChild(built.stage);
      this.el.setAttribute('data-state', state);

      this.anims.forEach((a) => a.cancel());
      this.anims = [];
      if (reduce.matches) {
        if (prev) prev.remove();
        return;
      }
      this.anims = animate(state, built);
      if (prev) {
        // Cambio de estado (§4.2): entra 420 ms FastOutSlowIn, el anterior se funde en 250 ms
        built.stage.animate([{ opacity: 0, transform: 'scale(0.96)' }, { opacity: 1, transform: 'scale(1)' }], { duration: 420, easing: 'cubic-bezier(0.4, 0, 0.2, 1)' });
        prev.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 250, easing: 'ease-out', fill: 'forwards' }).finished.then(() => prev.remove());
      }
      if (!this.visible) this.anims.forEach((a) => a.pause());
    }

    enter() {
      // Entrada a la pantalla (§4.1): 520 ms con un rebote pequeño
      if (this.entered || reduce.matches) return;
      this.entered = true;
      this.el.animate(
        [{ opacity: 0, transform: 'translateY(4%) scale(0.88)' }, { opacity: 1, transform: 'translateY(0) scale(1)' }],
        { duration: 520, easing: 'cubic-bezier(0.3, 1.35, 0.6, 1)', fill: 'backwards' }
      );
    }

    observe() {
      // Sin trabajo de animación fuera de pantalla
      const ob = new IntersectionObserver((entries) => {
        for (const e of entries) {
          this.visible = e.isIntersecting;
          if (e.isIntersecting) { this.enter(); this.anims.forEach((a) => a.play()); }
          else this.anims.forEach((a) => a.pause());
        }
      }, { rootMargin: '80px' });
      ob.observe(this.el);
    }
  }

  window.CofferFox = Fox;
  window.cofferFoxes =Array.from(document.querySelectorAll('[data-fox]')).map((el) => new Fox(el));
})();

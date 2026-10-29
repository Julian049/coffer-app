/* Comportamientos del zorro hechos con capas del arte original (lienzo de 1024).
   Cada uno arma sus capas dentro de un escenario y expone run() para reproducirlo desde cero. */
(function () {
  const K = window.FoxKit;
  const E = K.ease;
  const MA = K.BASE + 'capas/';
  const P = K.BASE + 'piezas/';
  const pct = (v) => (v / K.C) * 100 + '%';

  function box(stage, cls) {
    const b = document.createElement('div');
    b.className = 'fox-box ' + (cls || '');
    stage.appendChild(b);
    return b;
  }
  function group(parent, cls) {
    const g = document.createElement('div');
    g.className = 'fx-layer ' + (cls || '');
    parent.appendChild(g);
    return g;
  }
  function shadow(parent, x, y, w, h) {
    const s = document.createElement('div');
    s.className = 'floor-shadow';
    Object.assign(s.style, { left: pct(x), top: pct(y), width: pct(w), height: pct(h) });
    parent.appendChild(s);
    return s;
  }
  const rot = (d) => ({ transform: `rotate(${d}deg)` });

  /**
   * Al terminar su acción el zorro no se congela: sigue con un reposo en bucle (respira, parpadea, mueve una oreja)
   * que parte de su pose final. Se corta al volver a llamar run(). idle() devuelve las pistas del reposo.
   */
  function withIdle(idle) {
    let current = null, rest = null;
    return {
      stop() {
        if (current) current.cancel();
        if (rest) rest.cancel();
        current = rest = null;
      },
      start(tracks) {
        this.stop();
        const mine = (current = K.play(tracks));
        mine.done.then(() => { if (current === mine) rest = K.play(idle()); });
      },
    };
  }
  const forever = (duration, extra) => Object.assign({ duration, iterations: Infinity }, extra);
  const breathe = (from, to, duration) => [{ transform: from }, { transform: to }];

  /* ------------------------------------------------------------ 1 · Asomarse */
  const PAW_SVG = '<svg viewBox="0 0 40 50"><path d="M5 38 C4 24 12 16 20 16 C29 16 36 24 35 38 C35 45 30 48 20 48 C10 48 5 45 5 38 Z" fill="#45200F"/><path d="M14 40 L14 46 M20 41 L20 47 M26 40 L26 46" stroke="#6E3A22" stroke-width="2.2" stroke-linecap="round"/></svg>';

  /**
   * stage: contenedor posicionado. Dentro debe haber un elemento .peek-card (la tarjeta tras la que se esconde);
   * el zorro y las patas se crean solos. Opciones: x (px desde la izquierda de la tarjeta), size (px).
   */
  function peek(stage, opts) {
    const o = Object.assign({ x: 56, size: 250 }, opts);
    const card = stage.querySelector('.peek-card');
    const edge = card.offsetTop;
    const b = box(stage, 'peek-fox');
    Object.assign(b.style, { width: o.size + 'px', left: card.offsetLeft + o.x + 'px', top: edge + 4 + 'px', zIndex: 1 });
    card.style.zIndex = card.style.zIndex || 2;
    const paws = document.createElement('div');
    paws.className = 'peek-paws';
    Object.assign(paws.style, { width: o.size + 'px', left: card.offsetLeft + o.x + 'px', top: edge - 38 + 'px' });
    const k = o.size / 250;
    paws.innerHTML = `<div class="paw" data-paw="l" style="left:${82 * k}px">${PAW_SVG}</div><div class="paw" data-paw="r" style="left:${152 * k}px">${PAW_SVG}</div>`;
    stage.appendChild(paws);
    const rise = group(b, 'peek-rise');
    // Las orejas viven dentro del grupo de la cabeza: acompañan sus giros y suman su propio gesto
    const head = group(rise);
    const earL = K.layer(head, P + 'peek-ear-l.webp');
    const earR = K.layer(head, P + 'peek-ear-r.webp');
    K.layer(head, P + 'peek-head.webp');
    const eyeL = K.rectLayer(head, MA + 'fox_welcome_eye_l.webp', 476, 326, 45, 55);
    const eyeR = K.rectLayer(head, MA + 'fox_welcome_eye_r.webp', 596, 302, 45, 54);
    K.origin(earL, 428, 285);
    K.origin(earR, 610, 222);
    K.origin(head, 560, 560);
    const pawL = paws.querySelector('[data-paw="l"]');
    const pawR = paws.querySelector('[data-paw="r"]');

    // Niveles de altura (en % del lienzo): escondido, puntas de las orejas, ojos, hombros
    const H = { hidden: 'translateY(0%)', ears: 'translateY(-15%)', eyes: 'translateY(-40%)', up: 'translateY(-56%)' };
    const T = 4800;

    // Reposo asomado: respira, parpadea, mira de reojo y mueve las orejas sin soltar el borde
    const R = 7200;
    const life = withIdle(() => {
      const at = (f) => K.at(R, f);
      const eyeAt = (x, sy) => ({ transform: `translate(${x}%,0) scaleY(${sy})` });
      return [
        [rise, breathe(H.up, 'translateY(-54.8%)'), { duration: 2400, iterations: Infinity, direction: 'alternate', easing: E.soft }],
        [head, at([[0, rot(0)], [3600, rot(0)], [4000, rot(3), E.inOut], [5400, rot(3)], [5800, rot(0), E.inOut], [R, rot(0)]]), forever(R)],
        [earR, at([[0, rot(0)], [1400, rot(0)], [1500, rot(-12), E.out], [1700, rot(0), E.out], [R, rot(0)]]), forever(R)],
        [earL, at([[0, rot(0)], [6000, rot(0)], [6100, rot(9), E.out], [6320, rot(0), E.out], [R, rot(0)]]), forever(R)],
        ...[eyeL, eyeR].map((eye) => [eye, at([
          [0, eyeAt(0, 1)], [2400, eyeAt(0, 1)], [2470, eyeAt(0, 0.12)], [2560, eyeAt(0, 1)],      // parpadeo
          [4000, eyeAt(0, 1)], [4180, eyeAt(14, 1), E.out], [5400, eyeAt(14, 1)], [5580, eyeAt(0, 1), E.out],   // de reojo
          [R, eyeAt(0, 1)],
        ]), forever(R)]),
      ];
    });

    function run() {
      life.stop();
      if (K.reduce.matches) {
        rise.style.transform = H.eyes;
        return;
      }
      const at = (f) => K.at(T, f);
      life.start([
        [rise, at([
          [0, { transform: H.hidden }],
          [520, { transform: H.ears }, E.out],                 // asoma las orejas
          [1000, { transform: H.ears }, E.inOut],
          [1550, { transform: H.eyes }, E.out],                // con cuidado, hasta los ojos
          [3050, { transform: H.eyes }, E.inOut],
          [3480, { transform: H.up }, E.out],                  // se anima, sube los hombros y se queda ahí
          [T, { transform: H.up }],
        ]), { duration: T }],
        [earR, at([
          [0, rot(0)], [640, rot(0)], [740, rot(-13), E.out], [900, rot(0), E.out],     // primera reacción
          [3400, rot(0)], [3560, rot(-7), E.out], [4300, rot(-7)], [4560, rot(0), E.out],
          [T, rot(0)],
        ]), { duration: T }],
        [earL, at([
          [0, rot(0)], [1950, rot(0)], [2050, rot(9), E.out], [2260, rot(0), E.out],    // escucha a la izquierda
          [3400, rot(0)], [3560, rot(7), E.out], [4300, rot(7)], [4560, rot(0), E.out],
          [T, rot(0)],
        ]), { duration: T }],
        // Mira a un lado y al otro: los ojos se adelantan y la cabeza los acompaña
        [head, at([
          [0, rot(0)], [1700, rot(0)], [1950, rot(-4), E.out], [2250, rot(-4)],
          [2450, rot(4), E.inOut], [2750, rot(4)], [2950, rot(0), E.out],
          [3500, rot(0)], [3700, rot(2.5), E.out], [4400, rot(2.5)], [4700, rot(0), E.out], [T, rot(0)],
        ]), { duration: T }],
        ...[eyeL, eyeR].map((eye) => [eye, at([
          [0, { transform: 'translate(0,0) scaleY(1)' }],
          [1700, { transform: 'translate(0,0) scaleY(1)' }],
          [1850, { transform: 'translate(-18%,2%) scaleY(1)' }, E.out],
          [2280, { transform: 'translate(-18%,2%) scaleY(1)' }],
          [2420, { transform: 'translate(18%,0%) scaleY(1)' }, E.out],
          [2800, { transform: 'translate(18%,0%) scaleY(1)' }],
          [2920, { transform: 'translate(0,0) scaleY(1)' }, E.out],
          [3000, { transform: 'translate(0,0) scaleY(0.12)' }],        // parpadeo
          [3080, { transform: 'translate(0,0) scaleY(1)' }],
          [T, { transform: 'translate(0,0) scaleY(1)' }],
        ]), { duration: T }]),
        // Las patas suben desde detrás del borde (el contenedor recorta todo lo que queda bajo el borde)
        // y, ya arriba, el recorte se abre para que los dedos cuelguen sobre el frente de la tarjeta
        [paws, at([
          [0, { clipPath: 'inset(0 0 12px 0)' }], [3640, { clipPath: 'inset(0 0 12px 0)' }],
          [3660, { clipPath: 'inset(0 0 0px 0)' }], [T, { clipPath: 'inset(0 0 0px 0)' }],
        ]), { duration: T }],
        ...[[pawL, 0], [pawR, 70]].map(([paw, d]) => [paw, at([
          [0, { transform: 'translateY(46px)' }],
          [3260 + d, { transform: 'translateY(46px)' }],
          [3560 + d, { transform: 'translateY(-5px)' }, E.out],
          [3780 + d, { transform: 'translateY(0px)' }, E.out],    // se asientan sobre el borde y ahí se quedan
          [T, { transform: 'translateY(0px)' }],
        ]), { duration: T }]),
      ]);
    }
    return { run };
  }

  /* ------------------------------------------------------------ 3 · Celebrar */
  function celebrate(stage) {
    const b = box(stage, 'center-fox');
    const floor = shadow(b, 300, 888, 470, 52);
    const jump = group(b, 'jump');
    K.origin(jump, 540, 910);                          // pivote en los pies
    const sit = K.layer(jump, P + 'jump-sit.webp');
    const goal = K.layer(jump, P + 'jump-goal.webp');
    goal.style.transform = 'translateX(5.8%)';         // alinea el cuerpo de ambas poses
    goal.style.opacity = '0';
    const fx = group(b);
    const L = window.FOX_MANIFEST.states.goal;
    const lay = (name) => L.find((l) => l.layer === name);
    const lines = K.rectLayer(fx, MA + lay('lines').drawable + '.webp', lay('lines').x + 60, lay('lines').y, lay('lines').w, lay('lines').h);
    const sp1 = K.rectLayer(fx, MA + lay('sparkle1').drawable + '.webp', lay('sparkle1').x + 60, lay('sparkle1').y, lay('sparkle1').w, lay('sparkle1').h);
    const sp2 = K.rectLayer(fx, MA + lay('sparkle2').drawable + '.webp', lay('sparkle2').x + 60, lay('sparkle2').y, lay('sparkle2').w, lay('sparkle2').h);
    [lines, sp1, sp2].forEach((n) => (n.style.opacity = '0'));
    const T = 3200;
    const life = withIdle(() => [
      [jump, breathe('translateY(0%) scale(1, 1)', 'translateY(0%) scale(1.006, 1.018)'), { duration: 2200, iterations: Infinity, direction: 'alternate', easing: E.soft }],
    ]);

    function run() {
      life.stop();
      if (K.reduce.matches) {
        sit.style.opacity = '0'; goal.style.opacity = '1';
        [lines, sp1, sp2].forEach((n) => (n.style.opacity = '1'));
        return;
      }
      const at = (f) => K.at(T, f);
      const tf = (y, sx, sy) => ({ transform: `translateY(${y}%) scale(${sx}, ${sy})` });
      life.start([
        [jump, at([
          [0, tf(0, 1, 1)],
          [170, tf(1.2, 1.045, 0.9), E.out],              // anticipación: se agacha
          [230, tf(0, 0.97, 1.07), E.launch],             // impulso: se estira
          [520, tf(-22, 1, 1.02), E.out],                 // sube frenando
          [610, tf(-23, 1, 1)],                           // cuelga en el ápice
          [850, tf(0, 1.02, 0.98), E.gravity],            // cae con gravedad
          [930, tf(0, 1.055, 0.915), E.out],              // aterriza: absorbe
          [1120, tf(0, 0.99, 1.02), E.out],               // recupera el equilibrio
          [1320, tf(0, 1, 1), E.out],
          [2500, tf(0, 1, 1)],
          [2620, tf(0.6, 1.01, 0.985), E.out],            // se vuelve a sentar
          [2850, tf(0, 1, 1), E.out],
          [T, tf(0, 1, 1)],
        ]), { duration: T }],
        [sit, at([[0, { opacity: 1 }], [200, { opacity: 1 }], [260, { opacity: 0 }], [2560, { opacity: 0 }], [2760, { opacity: 1 }], [T, { opacity: 1 }]]), { duration: T }],
        [goal, at([[0, { opacity: 0 }], [200, { opacity: 0 }], [260, { opacity: 1 }], [2560, { opacity: 1 }], [2760, { opacity: 0 }], [T, { opacity: 0 }]]), { duration: T }],
        // La sombra se queda en el piso: se achica y aclara con la altura
        [floor, at([
          [0, { transform: 'scale(1)', opacity: 1 }], [170, { transform: 'scale(1.06)', opacity: 1 }],
          [560, { transform: 'scale(0.62)', opacity: 0.45 }, E.out], [610, { transform: 'scale(0.6)', opacity: 0.42 }],
          [850, { transform: 'scale(1)', opacity: 1 }, E.gravity], [930, { transform: 'scale(1.08)', opacity: 1 }, E.out],
          [1200, { transform: 'scale(1)', opacity: 1 }], [T, { transform: 'scale(1)', opacity: 1 }],
        ]), { duration: T }],
        // Al aterrizar: las rayitas laten y los destellos titilan
        [lines, at([
          [0, { opacity: 0, transform: 'scale(0.9)' }], [880, { opacity: 0, transform: 'scale(0.9)' }],
          [1080, { opacity: 1, transform: 'scale(1.04)' }, E.out], [1500, { opacity: 0.4, transform: 'scale(0.96)' }, E.soft],
          [1900, { opacity: 1, transform: 'scale(1.03)' }, E.soft], [2400, { opacity: 0, transform: 'scale(0.96)' }, E.out], [T, { opacity: 0, transform: 'scale(0.96)' }],
        ]), { duration: T }],
        ...[[sp1, 0], [sp2, 160]].map(([s, d]) => [s, at([
          [0, { opacity: 0, transform: 'scale(0.6) rotate(-10deg)' }], [600 + d, { opacity: 0, transform: 'scale(0.6) rotate(-10deg)' }],
          [900 + d, { opacity: 1, transform: 'scale(1.08) rotate(6deg)' }, E.out],
          [1350 + d, { opacity: 0.55, transform: 'scale(0.84) rotate(-4deg)' }, E.soft],
          [1800 + d, { opacity: 1, transform: 'scale(1.04) rotate(4deg)' }, E.soft],
          [2450, { opacity: 0, transform: 'scale(0.8) rotate(-6deg)' }, E.out], [T, { opacity: 0, transform: 'scale(0.8) rotate(-6deg)' }],
        ]), { duration: T }]),
      ]);
    }
    return { run };
  }

  /* ------------------------------------------------------------ 4 · Curiosear con el calendario */
  function calendar(stage) {
    const b = box(stage, 'center-fox cal-fox');
    b.style.translate = '0 7px';   // apoya las patas en el borde
    shadow(b, 90, 868, 520, 44);
    const body = group(b);
    K.layer(body, P + 'cal-body.webp');
    // La oreja vive dentro del grupo de la cabeza: hereda la inclinación y suma su propio gesto
    const head = group(body);
    const ear = K.layer(head, P + 'cal-ear.webp');
    K.layer(head, P + 'cal-head.webp');
    K.origin(head, 462, 520);
    K.origin(ear, 355, 305);
    K.origin(body, 380, 880);
    const L = window.FOX_MANIFEST.states.attentive;
    const cal = group(b);
    const [cx, cy] = window.FOX_MANIFEST.groupPivots.calendar;
    K.origin(cal, cx, cy);
    const days = {};
    L.filter((l) => l.group === 'calendar').forEach((l) => {
      const n = K.rectLayer(cal, MA + l.drawable + '.webp', l.x, l.y, l.w, l.h);
      if (l.layer !== 'calendar') { n.style.opacity = '0'; days[l.layer] = n; K.origin(n, l.x + l.w / 2, l.y + l.h / 2); }
    });
    const ln = L.find((l) => l.layer === 'lines');
    const lines = K.rectLayer(b, MA + ln.drawable + '.webp', ln.x, ln.y, ln.w, ln.h);
    lines.style.opacity = '0';
    const T = 5600;
    const R = 6800;
    const life = withIdle(() => {
      const at = (f) => K.at(R, f);
      return [
        [body, breathe('rotate(0deg) scale(1, 1)', 'rotate(0deg) scale(1.006, 1.02)'), { duration: 2400, iterations: Infinity, direction: 'alternate', easing: E.soft }],
        [head, at([[0, rot(0)], [1800, rot(0)], [2600, rot(4), E.inOut], [4200, rot(4)], [5000, rot(0), E.inOut], [R, rot(0)]]), forever(R)],
        [ear, at([[0, rot(0)], [5600, rot(0)], [5700, rot(-8), E.out], [5900, rot(0), E.out], [R, rot(0)]]), forever(R)],
      ];
    });

    function run() {
      life.stop();
      if (K.reduce.matches) {
        Object.values(days).forEach((d) => (d.style.opacity = '0'));
        days.day3.style.opacity = '1';
        return;
      }
      const at = (f) => K.at(T, f);
      // Recorre los días con la mirada (pequeños asentimientos sincronizados con cada día)
      const sweep = { day1: 350, day2: 480, day4: 610, day5: 740, day6: 870 };
      life.start([
        ...Object.entries(sweep).map(([k, d]) => [days[k], at([[0, { opacity: 0 }], [d, { opacity: 0 }], [d + 225, { opacity: 1 }], [d + 500, { opacity: 0 }], [T, { opacity: 0 }]]), { duration: T }]),
        [days.day3, at([
          [0, { opacity: 0, transform: 'scale(1)' }], [1100, { opacity: 0, transform: 'scale(1)' }],
          [1340, { opacity: 1, transform: 'scale(1.18)' }, 'cubic-bezier(0.3, 1.6, 0.6, 1)'],
          [1700, { opacity: 1, transform: 'scale(1)' }, E.out], [T, { opacity: 1, transform: 'scale(1)' }],
        ]), { duration: T }],
        [head, at([
          [0, rot(0)],
          [320, rot(2), E.out],
          [480, rot(3.2), E.soft], [610, rot(2.4), E.soft], [740, rot(3.6), E.soft], [870, rot(2.8), E.soft], [1020, rot(3.4), E.soft],
          [1180, rot(-1.5), E.out],                         // ¡ese día! levanta la cabeza
          [1600, rot(-1.5)],
          [2300, rot(8), E.inOut],                          // inclinación curiosa
          [3000, rot(8)],
          [3160, rot(5.5), E.out], [3320, rot(8), E.out], [3480, rot(5.5), E.out], [3640, rot(7.5), E.out],   // dos asentimientos
          [4500, rot(7.5)],
          [5000, rot(0), E.inOut], [T, rot(0)],
        ]), { duration: T }],
        [ear, at([
          [0, rot(0)], [1150, rot(0)], [1260, rot(-6), E.out], [1500, rot(-3.5), E.out],   // oreja en alerta
          [2300, rot(-3.5)], [2420, rot(-1), E.out], [2520, rot(-4), E.out], [2700, rot(-2.5), E.out],
          [4500, rot(-2.5)], [4900, rot(0), E.inOut], [T, rot(0)],
        ]), { duration: T }],
        // El cuerpo se inclina apenas hacia el calendario
        [body, at([[0, rot(0)], [600, rot(0.8), E.inOut], [2300, rot(1.6), E.inOut], [4500, rot(1.6)], [5000, rot(0), E.inOut], [T, rot(0)]]), { duration: T }],
        [cal, at([
          [0, rot(0)], [3700, rot(0)], [3880, rot(-4), E.soft], [4060, rot(3.5), E.soft], [4240, rot(-2.5), E.soft], [4420, rot(1.5), E.soft], [4600, rot(0), E.soft], [T, rot(0)],
        ]), { duration: T }],
        [lines, at([
          [0, { opacity: 0, transform: 'scale(0.94)' }], [1200, { opacity: 0, transform: 'scale(0.94)' }],
          [1400, { opacity: 1, transform: 'scale(1.04)' }, E.out], [1900, { opacity: 0.35, transform: 'scale(0.94)' }, E.soft],
          [2400, { opacity: 0, transform: 'scale(0.94)' }, E.out], [T, { opacity: 0, transform: 'scale(0.94)' }],
        ]), { duration: T }],
      ]);
    }
    return { run };
  }

  /* ------------------------------------------------------------ 5 · Descansar */
  function rest(stage) {
    const b = box(stage, 'center-fox rest-fox');
    shadow(b, 110, 770, 800, 60);
    const body = group(b);
    K.origin(body, 470, 800);
    const ear = K.layer(body, P + 'rest-ear.webp');
    K.layer(body, P + 'rest-body.webp');
    K.origin(ear, 300, 462);
    const Z = window.FOX_MANIFEST.states.resting.filter((l) => l.anim === 'floatZ');
    const zs = Z.map((l) => {
      const wrap = group(b);
      const img = K.rectLayer(wrap, MA + l.drawable + '.webp', l.x, l.y, l.w, l.h);
      K.origin(img, l.x + l.w / 2, l.y + l.h / 2);
      return { wrap, img, delay: { z1: 0, z2: 600, z3: 1200 }[l.layer] };
    });
    let current = null;

    function run() {
      if (current) current.cancel();
      if (K.reduce.matches) return;
      const T = 9600;   // dos respiraciones lentas por ciclo y orejas en momentos distintos
      const at = (f) => K.at(T, f);
      const br = (sx, sy) => ({ transform: `scale(${sx}, ${sy})` });
      const tracks = [
        [body, at([
          [0, br(1, 1)], [2000, br(1.007, 1.024), E.soft], [4400, br(1, 1), E.soft],
          [6600, br(1.01, 1.034), E.soft],                 // suspiro más hondo
          [7200, br(1.01, 1.034)], [9600, br(1, 1), E.soft],
        ]), { duration: T, iterations: Infinity }],
        [ear, at([
          [0, rot(0)], [2900, rot(0)], [3020, rot(-15), E.out], [3300, rot(0), E.out],
          [7600, rot(0)], [7700, rot(-11), E.out], [7860, rot(-2), E.out], [7960, rot(-12), E.out], [8300, rot(0), E.out],
          [T, rot(0)],
        ]), { duration: T, iterations: Infinity }],
      ];
      zs.forEach((z) => {
        tracks.push([z.wrap, [{ transform: 'translate(0%, 2%)' }, { transform: 'translate(1.5%, -5%)' }], { duration: 3600, delay: z.delay, iterations: Infinity, easing: 'ease-out' }]);
        tracks.push([z.img, [
          { offset: 0, opacity: 0, transform: 'scale(0.8)' }, { offset: 0.25, opacity: 1, transform: 'scale(0.86)' },
          { offset: 0.8, opacity: 1, transform: 'scale(1)' }, { offset: 1, opacity: 0, transform: 'scale(1.05)' },
        ], { duration: 3600, delay: z.delay, iterations: Infinity, easing: 'ease-out' }]);
      });
      current = K.play(tracks);
    }
    return { run };
  }

  /* ------------------------------------------------------------ Rig sentado con cabeza aparte */
  function sitRig(stage, cls) {
    const b = box(stage, 'center-fox ' + (cls || ''));
    const floor = shadow(b, 180, 884, 600, 50);
    const all = group(b);
    K.origin(all, 520, 910);
    K.layer(all, P + 'sit-body.webp');
    const head = group(all);
    const ear = K.layer(head, P + 'walk-ear.webp');
    K.layer(head, P + 'walk-head.webp');
    K.origin(head, 600, 452);
    K.origin(ear, 520, 250);
    return { b, all, head, ear, floor };
  }

  /* ------------------------------------------------------------ 6 · Seguir con la mirada */
  function look(stage) {
    const r = sitRig(stage, 'look-fox');
    // Resorte críticamente amortiguado: la cabeza persigue al cursor con inercia, sin rebotar
    let angle = 0, vel = 0, target = 0, lift = 0, liftV = 0, liftT = 0, ear = 0, earT = 0;
    let idleTimer = 0;
    const k = 90, c = 2 * Math.sqrt(k) * 0.9;
    function aim(e) {
      const rect = r.b.getBoundingClientRect();
      const hx = rect.left + rect.width * (600 / 1024), hy = rect.top + rect.height * (330 / 1024);
      const dx = e.clientX - hx, dy = e.clientY - hy;
      // Solo cabecea según la altura del puntero (arriba = hocico arriba) y nunca pasa de unos pocos grados;
      // si el puntero queda detrás del zorro la mirada se suaviza en vez de torcer la cabeza
      const side = dx < 0 ? 0.5 : 1;
      target = Math.max(-6, Math.min(5, (Math.atan2(dy, Math.max(120, Math.abs(dx))) * 180) / Math.PI * 0.3)) * side;
      liftT = Math.max(-0.6, Math.min(0.6, -dy / 300)) * side;
      earT = -4;
      idleTimer = 0;
    }
    stage.addEventListener('pointermove', aim);
    stage.addEventListener('pointerleave', () => { target = 0; liftT = 0; earT = 0; });
    if (K.reduce.matches) return { run() {} };
    K.loop((dt) => {
      idleTimer += dt;
      if (idleTimer > 2.4) { target = 0; liftT = 0; earT = 0; }
      vel += (k * (target - angle) - c * vel) * dt; angle += vel * dt;
      liftV += (k * (liftT - lift) - c * liftV) * dt; lift += liftV * dt;
      ear += (earT - ear) * Math.min(1, dt * 10);
      r.head.style.transform = `translateY(${(-lift).toFixed(2)}%) rotate(${angle.toFixed(2)}deg)`;
      r.ear.style.transform = `rotate(${ear.toFixed(2)}deg)`;
    });
    return { run() {} };
  }

  /* ------------------------------------------------------------ 7 · Sobresalto */
  function startle(stage) {
    const r = sitRig(stage, 'startle-fox');
    r.b.style.transform = 'translateY(7px)';
    const toast = stage.querySelector('.toast');   // opcional: el aviso que lo sobresalta
    // Cara de sorpresa sobre la cabeza: ojo apenas más grande con brillo y boca pequeña en «o» (tapa la sonrisa)
    const face = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    face.setAttribute('viewBox', '0 0 1024 1024');
    face.classList.add('fx-layer');
    face.style.opacity = '0';
    face.innerHTML = '<ellipse cx="738" cy="372" rx="28" ry="12" fill="#FEDDBC"/><ellipse cx="738" cy="375" rx="8" ry="11" fill="#6B2A18"/>'
      + '<circle cx="665" cy="318" r="25" fill="#2A0F08"/><circle cx="672" cy="311" r="6" fill="#fff"/>';
    r.head.appendChild(face);
    const T = 2600;
    const R = 6400;
    const life = withIdle(() => {
      const at = (f) => K.at(R, f);
      return [
        [r.all, breathe('translate(0%, 0%) rotate(0deg) scale(1, 1)', 'translate(0%, 0%) rotate(0deg) scale(1.006, 1.016)'), { duration: 2200, iterations: Infinity, direction: 'alternate', easing: E.soft }],
        [r.head, at([[0, rot(0)], [2000, rot(0)], [2600, rot(-3), E.inOut], [3800, rot(-3)], [4400, rot(0), E.inOut], [R, rot(0)]]), forever(R)],
        [r.ear, at([[0, rot(0)], [1000, rot(0)], [1100, rot(-6), E.out], [1300, rot(0), E.out], [5200, rot(0)], [5300, rot(-5), E.out], [5500, rot(0), E.out], [R, rot(0)]]), forever(R)],
      ];
    });
    function run() {
      life.stop();
      if (K.reduce.matches) return;
      const at = (f) => K.at(T, f);
      const tf = (x, y, sx, sy, r = 0) => ({ transform: `translate(${x}%, ${y}%) rotate(${r}deg) scale(${sx}, ${sy})` });
      life.start([
        ...(toast ? [[toast, at([
          [0, { opacity: 0, transform: 'translateY(-12px) scale(0.96)' }], [120, { opacity: 0, transform: 'translateY(-12px) scale(0.96)' }],
          [380, { opacity: 1, transform: 'translateY(0) scale(1)' }, E.out], [2200, { opacity: 1, transform: 'translateY(0) scale(1)' }],
          [2400, { opacity: 0, transform: 'translateY(-6px) scale(0.98)' }, E.out], [T, { opacity: 0, transform: 'translateY(-6px) scale(0.98)' }],
        ]), { duration: T }]] : []),
        [r.all, at([
          [0, tf(0, 0, 1, 1)], [300, tf(0, 0, 1, 1)],
          [370, tf(0, 1.2, 1.04, 0.92), E.out],              // se encoge del susto
          [520, tf(-1, -9, 0.98, 1.05, -4), E.launch],       // brinca recto hacia arriba, echando el torso atrás
          [600, tf(-1, -9.5, 1, 1, -4)],                     // cuelga un instante
          [780, tf(-1, 0.8, 1.04, 0.94, -1.5), E.gravity],   // cae
          [900, tf(-1, 0, 1, 1, -1), E.out],                 // absorbe el aterrizaje
          [1900, tf(-1, 0, 1, 1, -1)],
          [2300, tf(0, 0, 1, 1), E.inOut],                   // vuelve a su postura con calma
          [T, tf(0, 0, 1, 1)],
        ]), { duration: T }],
        [face, at([[0, { opacity: 0 }], [340, { opacity: 0 }], [400, { opacity: 1 }], [1500, { opacity: 1 }], [1800, { opacity: 0 }], [T, { opacity: 0 }]]), { duration: T }],
        // La sombra se queda en el piso y se achica con la altura
        [r.floor, at([
          [0, { transform: 'scale(1)', opacity: 1 }], [370, { transform: 'scale(1.05)', opacity: 1 }],
          [560, { transform: 'scale(0.7)', opacity: 0.5 }, E.out], [600, { transform: 'scale(0.68)', opacity: 0.48 }],
          [780, { transform: 'scale(1.05)', opacity: 1 }, E.gravity], [900, { transform: 'scale(1)', opacity: 1 }, E.out],
          [T, { transform: 'scale(1)', opacity: 1 }],
        ]), { duration: T }],
        [r.head, at([
          [0, rot(0)], [300, rot(0)], [420, rot(-5), E.out],           // mira hacia la notificación
          [1200, rot(-5)], [1500, rot(-2), E.inOut], [1700, rot(3), E.inOut],   // la estudia, ladea la cabeza
          [2000, rot(3)], [2350, rot(0), E.inOut], [T, rot(0)],
        ]), { duration: T }],
        [r.ear, at([
          [0, rot(0)], [300, rot(0)], [380, rot(-6), E.out],           // la oreja se para
          [1200, rot(-5)], [1700, rot(-2), E.inOut], [1800, rot(-4), E.out], [2000, rot(-2), E.out], [2350, rot(0), E.inOut], [T, rot(0)],
        ]), { duration: T }],
      ]);
    }
    return { run };
  }

  window.FoxBehaviors = { peek, celebrate, calendar, rest, look, startle };
})();

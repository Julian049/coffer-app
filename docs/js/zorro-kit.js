/* Utilidades compartidas del laboratorio del zorro.
   Todas las piezas viven en un lienzo virtual de 1024×1024 (el mismo de icons/mascot-animated),
   así que se apilan con posiciones en % sin calcular desplazamientos. */
(function () {
  const C = 1024;
  // Carpeta img/zorro/, calculada desde este script: las rutas funcionan desde cualquier página
  const BASE = new URL('../img/zorro/', document.currentScript.src).href;
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');

  // Velocidad global (1, 0.5, 0.25) para revisar en cámara lenta
  let speed = 1;
  const listeners = new Set();

  const Kit = {
    C,
    BASE,
    part: (name) => BASE + 'piezas/' + name + '.webp',
    capa: (drawable) => BASE + 'capas/' + drawable + '.webp',
    reduce,
    get speed() { return speed; },
    setSpeed(v) {
      speed = v;
      listeners.forEach((fn) => fn(v));
    },
    onSpeed(fn) { listeners.add(fn); },

    /** Capa de imagen a lienzo completo (1024×1024) dentro de `parent`. */
    layer(parent, src, cls) {
      const wrap = document.createElement('div');
      wrap.className = 'fx-layer' + (cls ? ' ' + cls : '');
      const img = document.createElement('img');
      img.src = src;
      img.alt = '';
      img.decoding = 'async';
      img.draggable = false;
      wrap.appendChild(img);
      parent.appendChild(wrap);
      return wrap;
    },

    /** Capa recortada colocada en su rectángulo del lienzo (como las del manifest). */
    rectLayer(parent, src, x, y, w, h, cls) {
      const img = document.createElement('img');
      img.src = src;
      img.alt = '';
      img.className = 'fx-rect' + (cls ? ' ' + cls : '');
      img.draggable = false;
      Object.assign(img.style, { left: (x / C) * 100 + '%', top: (y / C) * 100 + '%', width: (w / C) * 100 + '%', height: (h / C) * 100 + '%' });
      parent.appendChild(img);
      return img;
    },

    /** transform-origin a partir de coordenadas del lienzo. */
    origin(el, x, y) { el.style.transformOrigin = `${(x / C) * 100}% ${(y / C) * 100}%`; },

    /**
     * Reproduce pistas sincronizadas: [[elemento, fotogramas, opciones], ...].
     * Todas comparten la velocidad global y se cancelan juntas al repetir.
     */
    play(tracks) {
      const anims = tracks.map(([el, frames, opts]) => {
        const a = el.animate(frames, Object.assign({ fill: 'both' }, opts));
        a.playbackRate = speed;
        return a;
      });
      const off = (v) => anims.forEach((a) => (a.playbackRate = v));
      listeners.add(off);
      const done = Promise.all(anims.map((a) => a.finished.catch(() => {}))).then(() => listeners.delete(off));
      return { anims, done, cancel() { anims.forEach((a) => a.cancel()); listeners.delete(off); } };
    },

    /** Fotogramas con tiempos en ms dentro de una duración total: [[ms, props, easing?], ...]. */
    at(total, frames) {
      return frames.map(([ms, props, easing]) => Object.assign({ offset: Math.min(1, ms / total) }, props, easing ? { easing } : {}));
    },

    ease: {
      out: 'cubic-bezier(0.23, 1, 0.32, 1)',
      inOut: 'cubic-bezier(0.77, 0, 0.175, 1)',
      soft: 'cubic-bezier(0.45, 0, 0.55, 1)',      // sinusoidal: respiración, balanceos
      gravity: 'cubic-bezier(0.55, 0, 1, 0.45)',   // caída libre (aceleración de la gravedad)
      launch: 'cubic-bezier(0, 0.55, 0.45, 1)',    // subida que frena hasta el ápice
    },

    /** Bucle con requestAnimationFrame que respeta la velocidad global; devuelve stop(). */
    loop(fn) {
      let raf = 0;
      let last = performance.now();
      let alive = true;
      const tick = (now) => {
        if (!alive) return;
        const dt = Math.min(0.05, (now - last) / 1000) * speed;
        last = now;
        if (fn(dt) === false) { alive = false; return; }
        raf = requestAnimationFrame(tick);
      };
      raf = requestAnimationFrame(tick);
      return () => { alive = false; cancelAnimationFrame(raf); };
    },

    wait(ms) { return new Promise((r) => setTimeout(r, ms / speed)); },
  };

  window.FoxKit = Kit;
})();

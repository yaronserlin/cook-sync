/*
 * Accessibility menu: text size, high contrast, underline links.
 * Settings are saved in localStorage on the visitor's device (covered in
 * privacy.html, section 5) and apply on every page of this site.
 * The button sits bottom-right and lifts above the closing links when they
 * scroll into view, so it never covers them.
 */
(function () {
  'use strict';

  var FONT_STEPS = [100, 110, 125, 150];
  var DEFAULTS = { fontStep: 0, highContrast: false, underlineLinks: false };
  var STORAGE_KEY = 'cooksync-docs:accessibility-preferences';
  var prefs = load();

  function load() {
    var p = { fontStep: 0, highContrast: false, underlineLinks: false };
    try {
      var saved = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || '{}');
      var step = Number(saved.fontStep);
      if (step >= 0 && step < FONT_STEPS.length && Math.floor(step) === step) p.fontStep = step;
      p.highContrast = saved.highContrast === true;
      p.underlineLinks = saved.underlineLinks === true;
    } catch (e) { /* bad or unavailable storage - use defaults */ }
    return p;
  }

  function save() {
    try { window.localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs)); } catch (e) { /* session only */ }
  }
  var GAP = 16;

  var css = [
    '.a11y-high-contrast body { filter: contrast(1.3); }',
    '.a11y-underline-links a { text-decoration: underline !important; }',
    '.a11y-fab { position: fixed; right: 16px; bottom: 16px; z-index: 50; width: 48px; height: 48px; border-radius: 50%; border: 0;',
    '  background: #8C491A; color: #F5EAD8; display: flex; align-items: center; justify-content: center; cursor: pointer;',
    '  box-shadow: 0 4px 14px rgba(29, 33, 41, 0.25); transition: bottom 0.15s ease-out; }',
    '.a11y-fab svg { width: 24px; height: 24px; }',
    '.a11y-panel { position: fixed; right: 16px; z-index: 51; width: 240px; max-width: calc(100vw - 32px); padding: 14px;',
    '  background: #fff; color: #201E1D; border: 1px solid #D9C7A8; border-radius: 12px; box-shadow: 0 10px 25px rgba(29, 33, 41, 0.15);',
    '  font: 14px/1.4 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }',
    '.a11y-panel[hidden] { display: none; }',
    '.a11y-panel h2 { margin: 0 0 10px; font-size: 16px; }',
    '.a11y-panel .a11y-label { color: #5b6170; font-size: 12px; margin-bottom: 4px; }',
    '.a11y-panel .a11y-size { display: flex; margin-bottom: 12px; }',
    '.a11y-panel button.a11y-btn { flex: 1; padding: 6px 8px; border: 1px solid #D9C7A8; background: #fff; color: #201E1D; font-weight: 600; cursor: pointer; font-size: 14px; }',
    '.a11y-panel .a11y-size button:first-child { border-radius: 8px 0 0 8px; }',
    '.a11y-panel .a11y-size button:last-child { border-radius: 0 8px 8px 0; border-left: 0; }',
    '.a11y-panel button.a11y-btn:hover:not([disabled]) { background: #faf8f5; }',
    '.a11y-panel button.a11y-btn[disabled] { color: #a3a7b0; cursor: default; }',
    '.a11y-panel label { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; cursor: pointer; }',
    '.a11y-panel input { width: 16px; height: 16px; accent-color: #8C491A; }',
    '.a11y-panel .a11y-reset { width: 100%; border-radius: 8px; margin-top: 4px; }',
    '.a11y-fab:focus-visible, .a11y-panel button:focus-visible, .a11y-panel input:focus-visible { outline: 2px solid #8C491A; outline-offset: 2px; }'
  ].join('\n');

  var ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="4" r="1.6"/><path d="M5 8.5l7 1.5 7-1.5M12 10v4.5M12 14.5l-3 6M12 14.5l3 6"/></svg>';
  var CLOSE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>';

  function apply() {
    var root = document.documentElement;
    root.style.fontSize = FONT_STEPS[prefs.fontStep] + '%';
    root.classList.toggle('a11y-high-contrast', prefs.highContrast);
    root.classList.toggle('a11y-underline-links', prefs.underlineLinks);
  }

  function init() {
    var style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);

    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'a11y-fab';
    btn.setAttribute('aria-label', 'Open accessibility menu');
    btn.setAttribute('aria-expanded', 'false');
    btn.setAttribute('aria-controls', 'a11y-panel');
    btn.innerHTML = ICON;

    var panel = document.createElement('div');
    panel.id = 'a11y-panel';
    panel.className = 'a11y-panel';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Accessibility options');
    panel.hidden = true;
    panel.innerHTML =
      '<h2>Accessibility</h2>' +
      '<div class="a11y-label" id="a11y-size-label">Text size <span data-a="value"></span></div>' +
      '<div class="a11y-size" role="group" aria-labelledby="a11y-size-label">' +
      '<button type="button" class="a11y-btn" data-a="smaller" aria-label="Decrease text size">A-</button>' +
      '<button type="button" class="a11y-btn" data-a="larger" aria-label="Increase text size">A+</button></div>' +
      '<label><input type="checkbox" data-a="contrast"> High contrast</label>' +
      '<label><input type="checkbox" data-a="underline"> Underline links</label>' +
      '<button type="button" class="a11y-btn a11y-reset" data-a="reset">Reset</button>';

    document.body.appendChild(panel);
    document.body.appendChild(btn);

    function q(name) { return panel.querySelector('[data-a="' + name + '"]'); }

    function sync() {
      q('value').textContent = '(' + FONT_STEPS[prefs.fontStep] + '%)';
      q('smaller').disabled = prefs.fontStep === 0;
      q('larger').disabled = prefs.fontStep === FONT_STEPS.length - 1;
      q('contrast').checked = prefs.highContrast;
      q('underline').checked = prefs.underlineLinks;
    }

    function update(patch) {
      for (var k in patch) prefs[k] = patch[k];
      apply();
      save();
      sync();
      place();
    }

    // Lift the button above the closing links / footer when they are on screen.
    function place() {
      var vh = window.innerHeight;
      var covered = 0;
      var els = document.querySelectorAll('footer, .closing');
      for (var i = 0; i < els.length; i++) {
        var r = els[i].getBoundingClientRect();
        if (r.height > 0 && r.bottom > 0 && r.top < vh) covered = Math.max(covered, vh - r.top);
      }
      var bottom = covered + GAP;
      btn.style.bottom = bottom + 'px';
      panel.style.bottom = (bottom + 56) + 'px';
      panel.style.maxHeight = 'calc(100vh - ' + (bottom + 72) + 'px)';
      panel.style.overflowY = 'auto';
    }

    function setOpen(open) {
      panel.hidden = !open;
      btn.setAttribute('aria-expanded', String(open));
      btn.setAttribute('aria-label', open ? 'Close accessibility menu' : 'Open accessibility menu');
      btn.innerHTML = open ? CLOSE : ICON;
      if (open) { place(); q('larger').focus(); }
    }

    btn.addEventListener('click', function () { setOpen(panel.hidden); });
    q('smaller').addEventListener('click', function () { update({ fontStep: Math.max(0, prefs.fontStep - 1) }); });
    q('larger').addEventListener('click', function () { update({ fontStep: Math.min(FONT_STEPS.length - 1, prefs.fontStep + 1) }); });
    q('contrast').addEventListener('change', function (e) { update({ highContrast: e.target.checked }); });
    q('underline').addEventListener('change', function (e) { update({ underlineLinks: e.target.checked }); });
    q('reset').addEventListener('click', function () { update({ fontStep: DEFAULTS.fontStep, highContrast: DEFAULTS.highContrast, underlineLinks: DEFAULTS.underlineLinks }); });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && !panel.hidden) { setOpen(false); btn.focus(); }
    });
    window.addEventListener('scroll', place, { passive: true });
    window.addEventListener('resize', place);

    apply();
    sync();
    place();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();

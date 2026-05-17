/* Eduksha — analytics.
   Loads Microsoft Clarity + Meta Pixel unconditionally on every visit.
   Both are configured to mask PII (Clarity masks form fields by default;
   Pixel sends only anonymous page/event signals, not form contents).
   No cookie banner is shown — see /privacy for full disclosure. */
(function () {
  // ── Tracking IDs ─────────────────────────────────────────────────────
  var CLARITY_PROJECT_ID = 'woyrms707j';
  var META_PIXEL_ID      = '955088110833254';
  // ────────────────────────────────────────────────────────────────────

  // ── Microsoft Clarity ────────────────────────────────────────────────
  if (CLARITY_PROJECT_ID) {
    (function (c, l, a, r, i, t, y) {
      c[a] = c[a] || function () { (c[a].q = c[a].q || []).push(arguments); };
      t = l.createElement(r); t.async = 1; t.src = 'https://www.clarity.ms/tag/' + i;
      y = l.getElementsByTagName(r)[0]; y.parentNode.insertBefore(t, y);
    })(window, document, 'clarity', 'script', CLARITY_PROJECT_ID);
  }

  // ── Meta Pixel ───────────────────────────────────────────────────────
  if (META_PIXEL_ID) {
    !function (f, b, e, v, n, t, s) {
      if (f.fbq) return;
      n = f.fbq = function () { n.callMethod ? n.callMethod.apply(n, arguments) : n.queue.push(arguments); };
      if (!f._fbq) f._fbq = n;
      n.push = n; n.loaded = !0; n.version = '2.0';
      n.queue = [];
      t = b.createElement(e); t.async = !0;
      t.src = v;
      s = b.getElementsByTagName(e)[0];
      s.parentNode.insertBefore(t, s);
    }(window, document, 'script', 'https://connect.facebook.net/en_US/fbevents.js');
    window.fbq('init', META_PIXEL_ID);
    window.fbq('track', 'PageView');
  }

  // ── Public helper for tracking custom events from the rest of the
  //    site (e.g. CTA clicks, language toggle, waitlist submit). Maps
  //    internal event names to Meta standard events so the auction
  //    algorithm can optimise on them. Anything not in the map fires
  //    as a Custom event. ────────────────────────────────────────────
  var META_STANDARD = {
    waitlist_submit:   'Lead',
    hero_cta_click:    'InitiateCheckout',
    signup_sheet_open: 'InitiateCheckout',
    quiz_open:         'InitiateCheckout',
    quiz_complete:     'CompleteRegistration',
    form_field_focus:  'AddToCart',
    feature_view:      'ViewContent',
    faq_open:          'ViewContent'
  };

  window.eduTrack = function (eventName, params) {
    if (typeof window.clarity === 'function') {
      window.clarity('event', eventName);
    }
    if (typeof window.fbq === 'function') {
      var standard = META_STANDARD[eventName];
      if (standard) {
        window.fbq('track', standard, params || {});
      } else {
        window.fbq('trackCustom', eventName, params || {});
      }
    }
  };
})();

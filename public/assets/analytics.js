/* Eduksha — analytics + consent gating.
   Loads Microsoft Clarity + Google Analytics 4 only after the visitor
   accepts the cookie banner. Choice is persisted in localStorage so we
   never re-prompt the same browser. Hindi pages get a Hindi banner. */
(function () {
  var KEY = 'eduksha-consent';

  // ── Tracking IDs ─────────────────────────────────────────────────────
  var CLARITY_PROJECT_ID = 'woyrms707j';
  var GA4_MEASUREMENT_ID = 'G-SGYF6XQPBH';
  var META_PIXEL_ID      = '955088110833254';
  // ────────────────────────────────────────────────────────────────────

  var isHi = location.pathname === '/hi' ||
             location.pathname.indexOf('/hi/') === 0 ||
             document.documentElement.lang === 'hi';

  var saved = null;
  try { saved = localStorage.getItem(KEY); } catch (_) { /* private mode */ }

  if (saved === 'accept') {
    loadAnalytics();
    return;
  }
  if (saved === 'reject') return;

  // First visit — show banner once page is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', showBanner);
  } else {
    setTimeout(showBanner, 800);
  }

  function showBanner() {
    var copy = isHi
      ? {
          h: 'Cookies की अनुमति?',
          body: 'हम traffic समझने के लिए Google Analytics और Microsoft Clarity का use करते हैं। आपकी email या personal info नहीं ली जाती। पूरी जानकारी हमारी <a href="/hi/privacy">privacy policy</a> में है।',
          accept: 'OK, accept करें',
          reject: 'No, थैंक्स'
        }
      : {
          h: 'Cookies & analytics',
          body: 'We use Google Analytics and Microsoft Clarity to understand how visitors use the site. No personal info is collected. Read more in our <a href="/privacy">privacy policy</a>.',
          accept: 'Accept',
          reject: 'Decline'
        };

    var html =
      '<div class="cookie-banner" role="dialog" aria-label="Cookie consent">' +
        '<div>' +
          '<h4>' + copy.h + '</h4>' +
          '<p>' + copy.body + '</p>' +
        '</div>' +
        '<div class="cookie-actions">' +
          '<button class="reject" data-consent="reject" type="button">' + copy.reject + '</button>' +
          '<button class="accept" data-consent="accept" type="button">' + copy.accept + '</button>' +
        '</div>' +
      '</div>';

    document.body.insertAdjacentHTML('beforeend', html);

    var banner = document.querySelector('.cookie-banner');
    banner.querySelector('[data-consent="accept"]').addEventListener('click', function () {
      try { localStorage.setItem(KEY, 'accept'); } catch (_) {}
      banner.remove();
      loadAnalytics();
    });
    banner.querySelector('[data-consent="reject"]').addEventListener('click', function () {
      try { localStorage.setItem(KEY, 'reject'); } catch (_) {}
      banner.remove();
    });
  }

  function loadAnalytics() {
    // ── Microsoft Clarity (only if real ID is set) ───────────────────
    if (CLARITY_PROJECT_ID && CLARITY_PROJECT_ID.indexOf('PLACEHOLDER') !== 0) {
      (function (c, l, a, r, i, t, y) {
        c[a] = c[a] || function () { (c[a].q = c[a].q || []).push(arguments); };
        t = l.createElement(r); t.async = 1; t.src = 'https://www.clarity.ms/tag/' + i;
        y = l.getElementsByTagName(r)[0]; y.parentNode.insertBefore(t, y);
      })(window, document, 'clarity', 'script', CLARITY_PROJECT_ID);
    }

    // ── Google Analytics 4 (only if real ID is set) ──────────────────
    if (GA4_MEASUREMENT_ID && GA4_MEASUREMENT_ID.indexOf('PLACEHOLDER') !== 0) {
      var s = document.createElement('script');
      s.async = true;
      s.src = 'https://www.googletagmanager.com/gtag/js?id=' + GA4_MEASUREMENT_ID;
      document.head.appendChild(s);

      window.dataLayer = window.dataLayer || [];
      window.gtag = function () { window.dataLayer.push(arguments); };
      window.gtag('js', new Date());
      window.gtag('config', GA4_MEASUREMENT_ID, {
        anonymize_ip: true,
        page_path: location.pathname,
        custom_map: { lang: isHi ? 'hi' : 'en' }
      });
    }

    // ── Meta Pixel (only if real ID is set) ──────────────────────────
    if (META_PIXEL_ID && META_PIXEL_ID.indexOf('PLACEHOLDER') !== 0) {
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
  }

  // ── Public helper for tracking custom events from the rest of the
  //    site (e.g. CTA clicks, language toggle, waitlist submit). Safe
  //    to call before consent is given — the call queues silently. ──
  // Map our internal event names to Meta's standard event vocabulary
  // so its optimisation algo can use them. Anything not in the map
  // fires as a Custom event.
  var META_STANDARD = {
    waitlist_submit: 'Lead',
    hero_cta_click:  'InitiateCheckout',
    feature_view:    'ViewContent'
  };

  window.eduTrack = function (eventName, params) {
    if (typeof window.gtag === 'function') {
      window.gtag('event', eventName, params || {});
    }
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

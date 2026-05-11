/* Eduksha — waitlist form submission.
   Writes to Firestore /waitlist/{auto-id} from the marketing site.
   No backend needed; security is handled in firestore.rules. */
import { initializeApp, getApps }            from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js';
import { getFirestore, collection, addDoc,
         serverTimestamp }                   from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';

let dbPromise = null;

async function getDb() {
  if (dbPromise) return dbPromise;
  dbPromise = (async () => {
    const res = await fetch('/__/firebase/init.json');
    if (!res.ok) throw new Error('Firebase config not available');
    const config = await res.json();
    const app = getApps().length ? getApps()[0] : initializeApp(config);
    return getFirestore(app);
  })();
  return dbPromise;
}

function copyFor(isHi) {
  return isHi ? {
    invalidName:  'अपना नाम डालें',
    invalidPhone: 'सही 10-digit मोबाइल नंबर डालें',
    invalidEmail: 'सही email address डालें',
    failed:       'कुछ गड़बड़ हुई। थोड़ी देर बाद फिर से try करें।',
    successTitle: 'जुड़ गए! 🎉',
    successBody:  'जब Eduksha launch होगा, हम आपको सबसे पहले बता देंगे।'
  } : {
    invalidName:  "Please enter the parent's name",
    invalidPhone: 'Please enter a valid 10-digit phone number',
    invalidEmail: 'Please enter a valid email',
    failed:       'Something went wrong. Please try again in a moment.',
    successTitle: "You're in! 🎉",
    successBody:  "We'll be in touch the moment Eduksha is live on the Play Store."
  };
}

function ensureErrorEl(form) {
  let el = form.querySelector('.form-error');
  if (!el) {
    el = document.createElement('div');
    el.className = 'form-error';
    el.setAttribute('role', 'alert');
    form.appendChild(el);
  }
  return el;
}

function showSuccess(form, copy) {
  const wrap = document.createElement('div');
  wrap.className = 'waitlist-success';
  wrap.innerHTML =
    '<div class="check">✓</div>' +
    '<h4>' + copy.successTitle + '</h4>' +
    '<p>'  + copy.successBody  + '</p>';
  form.replaceWith(wrap);
}

function init() {
  const isHi = document.documentElement.lang === 'hi';
  const copy = copyFor(isHi);

  document.querySelectorAll('form.waitlist-form').forEach(form => {
    // Restrict phone input to digits only as the user types — strips
    // letters, spaces, +, -, etc. Pasted text is sanitised the same way.
    const phoneInput = form.querySelector('input[name="phone"]');
    if (phoneInput) {
      phoneInput.addEventListener('input', () => {
        // Strip non-digits, then if the result has the +91/91/0 country
        // prefix (common with browser autofill), remove it. Cap at 10.
        let cleaned = phoneInput.value.replace(/\D/g, '');
        if (cleaned.length === 12 && cleaned.startsWith('91')) cleaned = cleaned.slice(2);
        else if (cleaned.length === 11 && cleaned.startsWith('0')) cleaned = cleaned.slice(1);
        cleaned = cleaned.slice(0, 10);
        if (cleaned !== phoneInput.value) phoneInput.value = cleaned;
      });
    }

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const errorEl = ensureErrorEl(form);
      errorEl.textContent = '';

      // Detect which fields the form actually has. The hero form is
      // phone-only; the bottom form has name + phone + email + class.
      const hasParentName = !!form.querySelector('input[name="parentName"]');
      const hasPhone      = !!form.querySelector('input[name="phone"]');
      const hasEmail      = !!form.querySelector('input[name="email"]');

      const data = new FormData(form);
      const parentName = (data.get('parentName') || '').toString().trim().replace(/\s+/g, ' ');
      const phoneRaw   = (data.get('phone')      || '').toString();
      const email      = (data.get('email')      || '').toString().trim().toLowerCase();
      const childClass = (data.get('childClass') || '').toString().trim();
      const source     = form.dataset.source || 'unknown';
      const locale     = isHi ? 'hi' : 'en';

      if (hasParentName && (!parentName || parentName.length < 2 || parentName.length > 60)) {
        errorEl.textContent = copy.invalidName;
        return;
      }

      // Strip everything except digits, accept 10 digits or 12 starting with 91.
      let phone = '';
      if (hasPhone) {
        const digits = phoneRaw.replace(/\D/g, '');
        if (digits.length === 10) phone = digits;
        else if (digits.length === 12 && digits.startsWith('91')) phone = digits.slice(2);
        else if (digits.length === 11 && digits.startsWith('0')) phone = digits.slice(1);
        if (!phone || !/^[6-9]\d{9}$/.test(phone)) {
          errorEl.textContent = copy.invalidPhone;
          return;
        }
      }

      if (hasEmail && (!email || !/.+@.+[.].+/.test(email) || email.length > 100)) {
        errorEl.textContent = copy.invalidEmail;
        return;
      }

      form.classList.add('is-loading');

      try {
        const db = await getDb();
        await addDoc(collection(db, 'waitlist'), {
          parentName,
          phone,
          email,
          childClass,
          locale,
          source,
          createdAt: serverTimestamp(),
        });
        if (typeof window.eduTrack === 'function') {
          window.eduTrack('waitlist_submit', { locale, childClass: childClass || 'unspecified' });
        }
        showSuccess(form, copy);
      } catch (err) {
        console.error('[waitlist]', err);
        errorEl.textContent = copy.failed;
        form.classList.remove('is-loading');
      }
    });
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}

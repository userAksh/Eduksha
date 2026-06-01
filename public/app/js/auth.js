import { getAuthInstance } from './firebase-init.js';
import { getDb } from './firebase-init.js';
import {
  RecaptchaVerifier, signInWithPhoneNumber, signOut
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js';
import {
  doc, getDoc, setDoc, collection, getDocs
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import { state, setState, navigate, setScreen, showError, showLoading, hideLoading, $, IC_BACK } from './utils.js';

let confirmationResult = null;
let recaptchaVerifier = null;

// ── Login Screen ─────────────────────────────────────────────────────────────
export function renderLogin() {
  setScreen(`
    <div class="auth-screen">
      <div class="auth-brand">
        <img src="/assets/logo.png" alt="Eduksha" class="auth-logo" />
        <h1 class="auth-title">Eduksha</h1>
        <p class="auth-sub">A daily learning ritual for Indian families</p>
      </div>
      <div class="auth-card">
        <h2>Parent sign in</h2>
        <p class="auth-hint">Enter your mobile number. We'll send an OTP.</p>
        <div class="input-group">
          <div class="input-prefix">+91</div>
          <input id="phone-input" type="tel" inputmode="numeric" maxlength="10"
            placeholder="10-digit mobile number" autocomplete="tel" />
        </div>
        <div id="recaptcha-container"></div>
        <button id="send-otp-btn" class="btn btn-primary btn-full" disabled>Send OTP</button>
        <div id="auth-error" class="auth-error" hidden></div>
      </div>
      <div class="auth-footer">
        <p>Child joining a parent? <a href="#" id="child-join-link">Enter pairing PIN →</a></p>
      </div>
    </div>
  `);

  const input = $('#phone-input');
  const btn = $('#send-otp-btn');
  const errEl = $('#auth-error');

  input.addEventListener('input', () => {
    const val = input.value.replace(/\D/g, '');
    input.value = val;
    btn.disabled = val.length !== 10;
  });

  btn.addEventListener('click', async () => {
    const phone = '+91' + input.value.trim();
    btn.disabled = true;
    errEl.hidden = true;
    showLoading('Sending OTP…');
    try {
      if (!recaptchaVerifier) {
        recaptchaVerifier = new RecaptchaVerifier(getAuthInstance(), 'recaptcha-container', {
          size: 'invisible',
          callback: () => {}
        });
      }
      confirmationResult = await signInWithPhoneNumber(getAuthInstance(), phone, recaptchaVerifier);
      hideLoading();
      navigate('otp', { phone: input.value.trim() });
    } catch (e) {
      hideLoading();
      btn.disabled = false;
      errEl.hidden = false;
      errEl.textContent = friendlyAuthError(e);
      if (recaptchaVerifier) { try { recaptchaVerifier.clear(); } catch(_) {} recaptchaVerifier = null; }
    }
  });

  $('#child-join-link')?.addEventListener('click', e => {
    e.preventDefault();
    navigate('child-select');
  });
}

// ── OTP Screen ───────────────────────────────────────────────────────────────
export function renderOtp({ phone }) {
  setScreen(`
    <div class="auth-screen">
      <div class="auth-back">
        <button class="icon-btn" id="otp-back">${IC_BACK}</button>
      </div>
      <div class="auth-brand">
        <img src="/assets/logo.png" alt="Eduksha" class="auth-logo" />
      </div>
      <div class="auth-card">
        <h2>Enter OTP</h2>
        <p class="auth-hint">We sent a 6-digit code to <strong>+91 ${phone}</strong></p>
        <div class="otp-inputs" id="otp-inputs">
          ${[0,1,2,3,4,5].map(i => `<input class="otp-box" type="tel" inputmode="numeric" maxlength="1" data-idx="${i}" />`).join('')}
        </div>
        <button id="verify-btn" class="btn btn-primary btn-full" disabled>Verify & continue</button>
        <button id="resend-btn" class="btn btn-ghost btn-full" style="margin-top:10px">Resend OTP</button>
        <div id="otp-error" class="auth-error" hidden></div>
      </div>
    </div>
  `);

  $('#otp-back').addEventListener('click', () => navigate('login'));

  const boxes = Array.from($$('.otp-box'));
  const verifyBtn = $('#verify-btn');
  const errEl = $('#otp-error');

  function getOtp() { return boxes.map(b => b.value).join(''); }
  function checkReady() { verifyBtn.disabled = getOtp().length !== 6; }

  boxes.forEach((box, i) => {
    box.addEventListener('input', () => {
      box.value = box.value.replace(/\D/g, '').slice(-1);
      if (box.value && i < 5) boxes[i+1].focus();
      checkReady();
    });
    box.addEventListener('keydown', e => {
      if (e.key === 'Backspace' && !box.value && i > 0) boxes[i-1].focus();
    });
  });

  function $$(sel, root) { return Array.from((root||document).querySelectorAll(sel)); }

  verifyBtn.addEventListener('click', async () => {
    const otp = getOtp();
    if (!confirmationResult) { showError('Session expired. Go back and request a new OTP.'); return; }
    verifyBtn.disabled = true;
    errEl.hidden = true;
    showLoading('Verifying…');
    try {
      const result = await confirmationResult.confirm(otp);
      const user = result.user;
      const db = getDb();
      const parentRef = doc(db, 'parents', user.uid);
      const snap = await getDoc(parentRef);
      if (!snap.exists()) {
        await setDoc(parentRef, { name: '', phone: phone });
      }
      const name = snap.data()?.name || '';
      setState({ parentId: user.uid, parentName: name });
      hideLoading();
      if (!name) {
        navigate('setup-profile');
      } else {
        navigate('parent-home');
      }
    } catch (e) {
      hideLoading();
      verifyBtn.disabled = false;
      errEl.hidden = false;
      errEl.textContent = friendlyAuthError(e);
    }
  });

  $('#resend-btn').addEventListener('click', async () => {
    navigate('login');
  });

  // Auto-focus first box
  boxes[0]?.focus();
}

// ── Profile Setup (first time) ───────────────────────────────────────────────
export function renderSetupProfile() {
  setScreen(`
    <div class="auth-screen">
      <div class="auth-card">
        <h2>What's your name?</h2>
        <p class="auth-hint">Used on your parent dashboard.</p>
        <input id="name-input" type="text" class="input-full" placeholder="Your name" maxlength="60" autocomplete="name" />
        <button id="save-profile-btn" class="btn btn-primary btn-full" style="margin-top:16px" disabled>Continue →</button>
        <div id="profile-error" class="auth-error" hidden></div>
      </div>
    </div>
  `);

  const input = $('#name-input');
  const btn = $('#save-profile-btn');
  const errEl = $('#profile-error');

  input.addEventListener('input', () => { btn.disabled = !input.value.trim(); });

  btn.addEventListener('click', async () => {
    const name = input.value.trim();
    btn.disabled = true;
    showLoading('Saving…');
    try {
      const db = getDb();
      await setDoc(doc(db, 'parents', state.parentId), { name }, { merge: true });
      setState({ parentName: name });
      hideLoading();
      navigate('add-child');
    } catch(e) {
      hideLoading();
      btn.disabled = false;
      errEl.hidden = false;
      errEl.textContent = 'Failed to save. Please try again.';
    }
  });

  input.focus();
}

// ── Child Mode: select which child ──────────────────────────────────────────
export function renderChildSelect() {
  setScreen(`
    <div class="auth-screen">
      <div class="auth-back"><button class="icon-btn" id="cs-back">${IC_BACK}</button></div>
      <div class="auth-brand">
        <img src="/assets/logo.png" alt="Eduksha" class="auth-logo" />
        <h1 class="auth-title">Child mode</h1>
      </div>
      <div class="auth-card">
        <h2>Enter pairing code</h2>
        <p class="auth-hint">Ask your parent to open Eduksha and share a child code. Enter the 4-digit PIN for that child.</p>
        <div class="input-group">
          <input id="pairing-parent-id" type="text" class="input-full" placeholder="Parent ID (from parent's profile)" />
        </div>
        <div class="input-group" style="margin-top:10px">
          <input id="pairing-child-id" type="text" class="input-full" placeholder="Child ID" />
        </div>
        <button id="pairing-next-btn" class="btn btn-primary btn-full" style="margin-top:16px">Next →</button>
        <div id="pairing-error" class="auth-error" hidden></div>
      </div>
    </div>
  `);

  $('#cs-back').addEventListener('click', () => navigate('login'));

  $('#pairing-next-btn').addEventListener('click', async () => {
    const parentId = $('#pairing-parent-id').value.trim();
    const childId = $('#pairing-child-id').value.trim();
    if (!parentId || !childId) { showError('Enter both IDs'); return; }
    showLoading('Looking up child…');
    try {
      const db = getDb();
      const snap = await getDoc(doc(db, 'parents', parentId, 'children', childId));
      hideLoading();
      if (!snap.exists()) { $('#pairing-error').hidden = false; $('#pairing-error').textContent = 'Child not found. Check the IDs.'; return; }
      setState({ parentId });
      navigate('child-pin', { childId, parentId });
    } catch(e) {
      hideLoading();
      showError('Could not connect. Check your internet.');
    }
  });
}

// ── Child PIN entry ──────────────────────────────────────────────────────────
export function renderChildPin({ childId, parentId: overrideParentId }) {
  const pid = overrideParentId || state.parentId;
  setScreen(`
    <div class="auth-screen">
      <div class="auth-back"><button class="icon-btn" id="pin-back">${IC_BACK}</button></div>
      <div class="auth-card" style="text-align:center">
        <div class="pin-avatar">🧒</div>
        <h2>Enter child PIN</h2>
        <p class="auth-hint">4-digit PIN set when the child was added</p>
        <div class="pin-row" id="pin-dots">
          ${[0,1,2,3].map(() => `<div class="pin-dot"></div>`).join('')}
        </div>
        <div class="pin-keypad">
          ${[1,2,3,4,5,6,7,8,9,'',0,'⌫'].map(k => `
            <button class="pin-key ${k==='' ? 'pin-key-empty' : ''}" data-key="${k}">${k}</button>
          `).join('')}
        </div>
        <div id="pin-error" class="auth-error" hidden>Incorrect PIN. Try again.</div>
      </div>
    </div>
  `);

  let pin = '';
  const dots = Array.from(document.querySelectorAll('.pin-dot'));
  const errEl = $('#pin-error');

  function updateDots() {
    dots.forEach((d,i) => d.classList.toggle('filled', i < pin.length));
  }

  async function checkPin() {
    showLoading();
    try {
      const db = getDb();
      const snap = await getDoc(doc(db, 'parents', pid, 'children', childId));
      hideLoading();
      if (!snap.exists()) { showError('Child not found'); return; }
      const data = snap.data();
      if (data.pin === pin) {
        setState({ childId, childName: data.name, childGrade: data.grade, childGender: data.gender });
        navigate('child-home', { childId });
      } else {
        errEl.hidden = false;
        pin = '';
        updateDots();
      }
    } catch(e) {
      hideLoading();
      showError('Could not verify PIN. Check your internet.');
    }
  }

  document.querySelectorAll('.pin-key').forEach(btn => {
    const key = btn.dataset.key;
    if (!key) return;
    btn.addEventListener('click', () => {
      errEl.hidden = true;
      if (key === '⌫') { pin = pin.slice(0,-1); }
      else if (pin.length < 4) { pin += key; }
      updateDots();
      if (pin.length === 4) checkPin();
    });
  });

  $('#pin-back').addEventListener('click', () => history.back());
}

// ── Sign out ─────────────────────────────────────────────────────────────────
export async function doSignOut() {
  await signOut(getAuthInstance());
  setState({ parentId: null, parentName: null, childId: null, childName: null });
  sessionStorage.clear();
  navigate('login');
}

function friendlyAuthError(e) {
  const code = (e.code || e.message || '').toUpperCase();
  if (code.includes('TOO_MANY_REQUESTS') || code.includes('QUOTA')) return 'Too many attempts. Wait a few minutes and try again.';
  if (code.includes('NETWORK')) return 'No internet connection. Check your network.';
  if (code.includes('INVALID_PHONE')) return 'Please enter a valid 10-digit mobile number.';
  if (code.includes('INVALID_CODE') || code.includes('INVALID_VERIFICATION_CODE')) return 'Incorrect OTP. Check the code and try again.';
  if (code.includes('SESSION_EXPIRED') || code.includes('CODE_EXPIRED')) return 'OTP expired. Go back and request a new one.';
  return 'Something went wrong. Please try again.';
}

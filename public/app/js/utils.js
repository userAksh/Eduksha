// ── SVG icons (shared) ───────────────────────────────────────────────────────
export const IC_BACK = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>`;

// ── State ────────────────────────────────────────────────────────────────────
export const state = {
  parentId: null,
  parentName: null,
  // Active child in child mode
  childId: null,
  childName: null,
  childGrade: null,
  childGender: null,
  // Listeners to clean up on navigation
  _unsubscribers: []
};

export function setState(updates) {
  Object.assign(state, updates);
  try { sessionStorage.setItem('edu_state', JSON.stringify({
    parentId: state.parentId, parentName: state.parentName,
    childId: state.childId, childName: state.childName,
    childGrade: state.childGrade, childGender: state.childGender
  })); } catch(e) {}
}

export function restoreState() {
  try {
    const s = JSON.parse(sessionStorage.getItem('edu_state') || '{}');
    Object.assign(state, s);
  } catch(e) {}
}

export function addUnsubscriber(fn) { state._unsubscribers.push(fn); }

export function cleanupListeners() {
  state._unsubscribers.forEach(fn => { try { fn(); } catch(e) {} });
  state._unsubscribers = [];
}

// ── Router ───────────────────────────────────────────────────────────────────
export function navigate(route, params = {}) {
  cleanupListeners();
  const qs = new URLSearchParams(params).toString();
  window.location.hash = '#' + route + (qs ? '?' + qs : '');
}

export function getRoute() {
  const hash = window.location.hash.slice(1) || '';
  const [route, qs] = hash.split('?');
  const params = Object.fromEntries(new URLSearchParams(qs || ''));
  return { route: route || 'login', params };
}

// ── DOM helpers ──────────────────────────────────────────────────────────────
export function $(sel, root) { return (root || document).querySelector(sel); }
export function $$(sel, root) { return Array.from((root || document).querySelectorAll(sel)); }

export function setScreen(html) {
  const screen = document.getElementById('screen');
  screen.innerHTML = html;
  screen.scrollTop = 0;
}

export function showLoading(msg = '') {
  const overlay = document.getElementById('global-loading');
  if (overlay) {
    overlay.querySelector('.loading-msg').textContent = msg;
    overlay.hidden = false;
  }
}
export function hideLoading() {
  const overlay = document.getElementById('global-loading');
  if (overlay) overlay.hidden = true;
}

export function showToast(msg, type = 'info', duration = 3000) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = msg;
  container.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('is-visible'));
  setTimeout(() => {
    toast.classList.remove('is-visible');
    setTimeout(() => toast.remove(), 400);
  }, duration);
}

export function showError(msg) { showToast(msg, 'error', 4000); }
export function showSuccess(msg) { showToast(msg, 'success'); }

// ── Format helpers ───────────────────────────────────────────────────────────
export function formatDate(ts) {
  if (!ts) return '';
  const d = new Date(typeof ts === 'number' ? ts : ts.toMillis ? ts.toMillis() : ts);
  const now = new Date();
  const diffMs = now - d;
  const diffMin = Math.floor(diffMs / 60000);
  const diffHr = Math.floor(diffMs / 3600000);
  const diffDay = Math.floor(diffMs / 86400000);
  if (diffMin < 1) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHr < 24) return `${diffHr}h ago`;
  if (diffDay === 1) return 'Yesterday';
  if (diffDay < 7) return `${diffDay}d ago`;
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export function formatTs(ts) {
  if (!ts) return '';
  const d = new Date(typeof ts === 'number' ? ts : ts.toMillis ? ts.toMillis() : ts);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: '2-digit' });
}

export function subjectColor(subject) {
  const map = {
    'Mathematics': '#2f7dff', 'Science': '#22c55e', 'English': '#f59e0b',
    'Social Science': '#8b5cf6', 'Hindi': '#ef4444', 'Sanskrit': '#ec4899'
  };
  return map[subject] || '#6b7a99';
}

export function subjectIcon(subject) {
  const map = {
    'Mathematics': '📐', 'Science': '🔬', 'English': '📚',
    'Social Science': '🌍', 'Hindi': '🇮🇳', 'Sanskrit': '🕉️'
  };
  return map[subject] || '📖';
}

export function genderEmoji(gender) {
  return gender === 'girl' ? '👧' : gender === 'other' ? '🧒' : '👦';
}

export function streakEmoji(days) {
  if (days >= 30) return '🏆';
  if (days >= 14) return '⭐';
  if (days >= 7) return '🔥';
  if (days >= 3) return '✨';
  return '🌱';
}

// ── Streak calculation (mirrors Android ChildEngagementRepository) ───────────
export function calcStreakFromTimestamps(timestamps) {
  if (!timestamps || !timestamps.length) return 0;
  const epochDay = ts => {
    const d = new Date(ts);
    d.setHours(0, 0, 0, 0);
    return Math.floor(d.getTime() / 86400000);
  };
  const days = new Set(timestamps.filter(t => t > 0).map(epochDay));
  const today = epochDay(Date.now());
  let d = days.has(today) ? today : days.has(today - 1) ? today - 1 : today;
  let streak = 0;
  while (days.has(d)) { streak++; d--; }
  return streak;
}

export function calcEngagement(uploads, subjectives, quizzes) {
  const now = Date.now();
  const todayStart = (() => { const d = new Date(); d.setHours(0,0,0,0); return d.getTime(); })();
  const monday = (() => {
    const d = new Date(); d.setHours(0,0,0,0);
    const day = d.getDay(); // 0=Sun
    const diff = (day === 0) ? 6 : day - 1;
    d.setDate(d.getDate() - diff);
    return d.getTime();
  })();

  const allTs = [
    ...uploads,
    ...subjectives.map(s => s.timestamp),
    ...quizzes.map(q => q.timestamp)
  ];

  const dayKey = ts => { const d = new Date(ts); return `${d.getFullYear()}-${d.getDayOfYear?.() ?? d.toDateString()}`; };
  const daysThisWeek = new Set(allTs.filter(t => t >= monday && t <= now).map(t => {
    const d = new Date(t); return d.toDateString();
  })).size;

  const isActiveToday = allTs.some(t => t >= todayStart && t <= now);
  const streak = calcStreakFromTimestamps(allTs);

  const totalCorrect = subjectives.reduce((s,r) => s + r.correct, 0) + quizzes.reduce((s,r) => s + r.correct, 0);
  const totalAttempted = subjectives.reduce((s,r) => s + r.total, 0) + quizzes.reduce((s,r) => s + r.total, 0);
  const accuracyPct = totalAttempted > 0 ? Math.round((totalCorrect / totalAttempted) * 100) : null;

  const subjectStats = {};
  [...subjectives, ...quizzes].filter(r => r.subject && r.total > 0).forEach(r => {
    if (!subjectStats[r.subject]) subjectStats[r.subject] = { correct: 0, total: 0 };
    subjectStats[r.subject].correct += r.correct;
    subjectStats[r.subject].total += r.total;
  });
  const qualified = Object.entries(subjectStats).filter(([,v]) => v.total >= 3);
  let weakSubject = '', weakAccuracy = null;
  if (qualified.length) {
    const [ws, wv] = qualified.reduce((a,b) => (a[1].correct/a[1].total) <= (b[1].correct/b[1].total) ? a : b);
    weakSubject = ws;
    weakAccuracy = Math.round((wv.correct / wv.total) * 100);
  }

  return { daysActiveThisWeek: Math.min(daysThisWeek, 7), isActiveToday, streak, accuracyPct, weakSubject, weakAccuracy, lastActivityMs: Math.max(0, ...allTs) };
}

// ── Curated questions ────────────────────────────────────────────────────────
let _qBank = null;
export async function getQuestionsBank() {
  if (_qBank) return _qBank;
  const res = await fetch('/app/data/questions.json');
  _qBank = await res.json();
  return _qBank;
}

export function getCuratedQuestions(unitId, conceptId) {
  if (!_qBank) return [];
  if (conceptId && _qBank.byConcept[conceptId]) return _qBank.byConcept[conceptId];
  if (unitId && _qBank.byUnit[unitId]) return _qBank.byUnit[unitId];
  return [];
}

// ── Selections encode/decode (mirrors Android) ───────────────────────────────
export function encodeSelections(map) {
  return Object.entries(map).map(([q,o]) => `${q}:${o}`).join(',');
}
export function decodeSelections(str) {
  if (!str) return {};
  return Object.fromEntries(str.split(',').map(e => {
    const [q,o] = e.split(':');
    return [parseInt(q), parseInt(o)];
  }).filter(([q,o]) => !isNaN(q) && !isNaN(o)));
}

// ── Task status helpers ──────────────────────────────────────────────────────
export const TASK_STATUS = { REQUESTED:'REQUESTED', ASSIGNED:'ASSIGNED', PENDING_REVIEW:'PENDING_REVIEW', ACCEPTED:'ACCEPTED', REJECTED:'REJECTED' };

export function taskStatusLabel(status) {
  return { REQUESTED:'Requested', ASSIGNED:'Assigned', PENDING_REVIEW:'Pending Review', ACCEPTED:'Accepted', REJECTED:'Redo' }[status] || status;
}
export function taskStatusColor(status) {
  return { REQUESTED:'#f59e0b', ASSIGNED:'#2f7dff', PENDING_REVIEW:'#8b5cf6', ACCEPTED:'#22c55e', REJECTED:'#ef4444' }[status] || '#6b7a99';
}

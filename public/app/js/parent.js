import { getDb } from './firebase-init.js';
import {
  doc, collection, onSnapshot, query, where, addDoc, setDoc, updateDoc,
  getDocs, getDoc, deleteDoc, orderBy, limit
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import {
  state, setState, navigate, setScreen, showError, showSuccess, showLoading, hideLoading,
  $, $$, calcEngagement, formatDate, formatTs, subjectColor, subjectIcon, genderEmoji,
  addUnsubscriber, taskStatusLabel, taskStatusColor, TASK_STATUS, IC_BACK
} from './utils.js';
import { doSignOut } from './auth.js';

// ── SVG icons (Material Design, matching Android) ─────────────────────────────
const IC_PEOPLE    = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>`;
const IC_DASHBOARD = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>`;
const IC_PERSON    = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`;
const IC_CHECK     = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>`;
const IC_CLOCK     = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm.01 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>`;
const IC_ADD       = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>`;
const IC_CHEVRON   = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6-6-6z"/></svg>`;
const IC_SHIELD    = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>`;
const IC_BELL      = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>`;
const IC_SCHOOL    = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/></svg>`;
const IC_EDIT_NOTE = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 14H7v-1.41l3.96-3.96 1.41 1.41L8.83 16H12v1zm4.29-9.29L14.88 9l-4.17 4.17-1.41-1.42 4.17-4.17-1.29-1.29L15.29 5 19 8.71l-1.71 1.58z"/></svg>`;
const IC_BRAIN     = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M13 2a4 4 0 014 4 4 4 0 01-.08.8A4 4 0 0120 10a4 4 0 01-2 3.46V20a1 1 0 01-1 1H7a1 1 0 01-1-1v-6.54A4 4 0 014 10a4 4 0 013.08-3.2A4 4 0 0111 2h2m0 2h-2a2 2 0 00-2 2 2 2 0 002 2h2a2 2 0 002-2 2 2 0 00-2-2m0 6h-2c-1.1 0-2-.9-2-2H7a2 2 0 002 2v8h6v-8a2 2 0 002-2h-2a2 2 0 01-2 2z"/></svg>`;
const IC_BARCHART  = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z"/></svg>`;
const IC_AUTORENEW = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 6v3l4-4-4-4v3c-4.42 0-8 3.58-8 8 0 1.57.46 3.03 1.24 4.26L6.7 14.8A5.87 5.87 0 016 12c0-3.31 2.69-6 6-6zm6.76 1.74L17.3 9.2c.44 1.25.7 2.5.7 2.8 0 3.31-2.69 6-6 6v-3l-4 4 4 4v-3c4.42 0 8-3.58 8-8 0-1.57-.46-3.03-1.24-4.26z"/></svg>`;
const IC_CALENDAR  = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z"/></svg>`;
const IC_SPARKLE   = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2z"/></svg>`;
const IC_BOOK      = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z"/></svg>`;
const IC_EDIT      = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 000-1.41l-2.34-2.34a1 1 0 00-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>`;
const IC_MINUS     = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 13H5v-2h14v2z"/></svg>`;
const IC_LOCK      = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>`;
const IC_LOGOUT    = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5-5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>`;
const IC_HELP      = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z"/></svg>`;
const IC_SUPPORT   = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 12.22C21 6.73 16.74 3 12 3c-4.69 0-9 3.65-9 9.28-.6.34-1 .98-1 1.72v2c0 1.1.9 2 2 2h1v-6.1c0-3.87 3.13-7 7-7s7 3.13 7 7V19h-8v2h8c1.1 0 2-.9 2-2v-1.22c.59-.31 1-.92 1-1.64v-2.3c0-.7-.41-1.31-1-1.62z"/></svg>`;
const IC_PHONE     = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>`;
const IC_GPS       = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0013 3.06V1h-2v2.06A8.994 8.994 0 003.06 11H1v2h2.06A8.994 8.994 0 0011 20.94V23h2v-2.06A8.994 8.994 0 0020.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg>`;
const IC_LANGUAGE  = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 00-1.38-3.56A8.03 8.03 0 0118.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.987 7.987 0 015.08 16zm2.95-8H5.08a7.987 7.987 0 014.33-3.56A15.65 15.65 0 008.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 01-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>`;
const IC_CHECK_CIRCLE = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>`;
const IC_ASSIGNMENT = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 3c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm7 13H5v-.23c0-.62.28-1.2.76-1.58C7.47 15.82 9.64 15 12 15s4.53.82 6.24 2.19c.48.38.76.97.76 1.58V19z"/></svg>`;

// ── Avatar images (cycle through 3 avatars) ───────────────────────────────────
const AVATARS = [
  '/assets/dashboard_avatar_ritika.png',
  '/assets/dashboard_avatar_aarav.png',
  '/assets/dashboard_avatar_vivaan.png',
];

// ── Parent Home (3 tabs) ──────────────────────────────────────────────────────
export function renderParentHome() {
  setScreen(`
    <div class="ph-shell">
      <div class="ph-tab-content" id="ph-tab-content"></div>
      <nav class="ph-bottom-nav" id="ph-bottom-nav">
        <button class="ph-nav-tab active" data-tab="0">
          <span class="ph-nav-icon">${IC_PEOPLE}</span>
          <span class="ph-nav-label">Children</span>
        </button>
        <button class="ph-nav-tab" data-tab="1">
          <span class="ph-nav-icon">${IC_DASHBOARD}</span>
          <span class="ph-nav-label">Dashboard</span>
        </button>
        <button class="ph-nav-tab" data-tab="2">
          <span class="ph-nav-icon">${IC_PERSON}</span>
          <span class="ph-nav-label">Profile</span>
        </button>
      </nav>
    </div>
  `);

  let currentTab = 0;
  const tabContent = $('#ph-tab-content');

  function switchTab(idx) {
    currentTab = idx;
    $$('.ph-nav-tab').forEach(t => t.classList.toggle('active', +t.dataset.tab === idx));
    if (idx === 0) renderChildrenTab(tabContent);
    else if (idx === 1) renderDashboardTab(tabContent);
    else renderSettingsTab(tabContent);
  }

  $$('.ph-nav-tab').forEach(btn => {
    btn.addEventListener('click', () => switchTab(+btn.dataset.tab));
  });

  switchTab(0);
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 0: CHILDREN
// ═══════════════════════════════════════════════════════════════════════════════

function renderChildrenTab(container) {
  const name = state.parentName || '';
  container.innerHTML = `
    <div class="ph-children-scroll">

      <!-- Header row: greeting + illustration + bell -->
      <div class="ph-header-row">
        <div style="flex:1;padding-right:8px">
          <div class="ph-greeting">Hello, ${name || 'there'}! <span style="display:inline-block">👋</span></div>
          <div class="ph-greeting-sub">Track your children's learning</div>
        </div>
        <img src="/assets/parent_home_illustration.png" class="ph-header-illustration" alt="" />
        <div class="ph-bell-wrap">
          <div class="ph-bell-btn">${IC_BELL}</div>
          <div class="ph-bell-dot"></div>
        </div>
      </div>

      <!-- Tasks needing attention -->
      <div id="ph-tasks-card"></div>

      <!-- Children section header -->
      <div class="ph-section-header">
        <div class="ph-section-left">
          <div class="ph-section-title">Your Children</div>
          <div class="ph-section-sub">View progress, insights and help them grow</div>
        </div>
        <div class="ph-profile-chip" id="ph-profile-count">
          ${IC_PEOPLE}<span id="ph-count-label">…</span>
        </div>
      </div>

      <!-- Child cards -->
      <div id="ph-children-list">
        ${skeletonCard()}${skeletonCard()}
      </div>

      <!-- Add more children card -->
      <div class="ph-add-more-card clickable" id="ph-add-child-card">
        <div class="ph-add-icon-box">${IC_SHIELD}</div>
        <div class="ph-add-text">
          <div class="ph-add-title">Add more children</div>
          <div class="ph-add-sub">Track another child's learning journey</div>
        </div>
        <button class="ph-add-pill-btn">${IC_ADD} Add child</button>
      </div>

    </div>
  `;

  $('#ph-add-child-card').addEventListener('click', () => navigate('add-child'));
  subscribeChildren(container);
}

function skeletonCard() {
  return `<div class="ph-skeleton-card"><div class="ph-sk-top"></div><div class="ph-sk-bottom"></div></div>`;
}

function subscribeChildren(container) {
  const db = getDb();
  const parentId = state.parentId;
  if (!parentId) return;

  let children = [];
  let engagements = {};

  const unsub = onSnapshot(
    collection(db, 'parents', parentId, 'children'),
    snap => {
      children = snap.docs.map(d => ({ id: d.id, ...d.data() }));
      const countLabel = document.getElementById('ph-count-label');
      if (countLabel) countLabel.textContent = `${children.length} profile${children.length !== 1 ? 's' : ''}`;
      renderChildCards(children, engagements);
      children.forEach((child, idx) => {
        subscribeChildEngagement(db, child.id, idx, eng => {
          engagements[child.id] = eng;
          renderChildCards(children, engagements);
        });
      });
    }
  );
  addUnsubscriber(unsub);

  const taskUnsub = onSnapshot(
    query(collection(db, 'parentTaskRequests'), where('parentId', '==', parentId)),
    snap => {
      const tasks = snap.docs.map(d => ({ id: d.id, ...d.data() }));
      renderPendingTasksCard(tasks, children);
    }
  );
  addUnsubscriber(taskUnsub);
}

function subscribeChildEngagement(db, childId, childIndex, callback) {
  const now = Date.now();
  const todayStart = (() => { const d = new Date(); d.setHours(0,0,0,0); return d.getTime(); })();
  const monday = (() => {
    const d = new Date(); d.setHours(0,0,0,0);
    const day = d.getDay(); const diff = day === 0 ? 6 : day - 1;
    d.setDate(d.getDate() - diff); return d.getTime();
  })();
  const epochDay = ts => { const d = new Date(ts); d.setHours(0,0,0,0); return Math.floor(d.getTime()/86400000); };

  let data = { uploads: [], unitDocs: [], conceptDocs: [] };
  let called = 0;

  function merge() {
    called++;
    if (called < 3) return;
    const allTs = [
      ...data.uploads.map(d => d.ts),
      ...data.unitDocs.map(d => d.lastUpdated||0),
      ...data.conceptDocs.map(d => d.lastUpdated||0),
    ].filter(Boolean);

    const daysThisWeek = new Set(allTs.filter(t => t >= monday && t <= now).map(t => new Date(t).toDateString())).size;
    const isActiveToday = allTs.some(t => t >= todayStart && t <= now);
    const lastActivityMs = allTs.length ? Math.max(...allTs) : 0;

    const days = new Set(allTs.filter(t => t > 0).map(epochDay));
    const today = epochDay(Date.now());
    let d = days.has(today) ? today : days.has(today-1) ? today-1 : today;
    let streak = 0; while (days.has(d)) { streak++; d--; }

    const allAttempts = [...data.unitDocs, ...data.conceptDocs];
    const totalCorrect = allAttempts.reduce((s,r) => s + (r.lastCorrect||0), 0);
    const totalAttempted = allAttempts.reduce((s,r) => s + (r.lastTotal||0), 0);
    const accuracyPct = totalAttempted > 0 ? Math.round((totalCorrect/totalAttempted)*100) : null;

    // Weak subject: subject with most wrong answers
    const subjMap = {};
    allAttempts.forEach(r => {
      if (!r.subject || !r.lastTotal) return;
      if (!subjMap[r.subject]) subjMap[r.subject] = { correct: 0, total: 0 };
      subjMap[r.subject].correct += r.lastCorrect||0;
      subjMap[r.subject].total += r.lastTotal;
    });
    let weakSubject = '—', weakPct = null;
    Object.entries(subjMap).forEach(([subj, stat]) => {
      const pct = Math.round((stat.correct/stat.total)*100);
      if (weakPct === null || pct < weakPct) { weakSubject = subj; weakPct = pct; }
    });

    // Week pattern for dashboard
    const weekPattern = buildWeekPattern(data.uploads, allAttempts);

    callback({
      daysActiveThisWeek: Math.min(daysThisWeek, 7),
      isActiveToday, streak, streakDays: streak,
      accuracyPct, lastActivityMs, weakSubject,
      weakSubjectAccuracyPct: weakPct,
      weekPattern,
      childIndex
    });
  }

  const u1 = onSnapshot(query(collection(db,'uploads'), where('childId','==',childId)), snap => {
    data.uploads = snap.docs.map(d => ({ ts: d.data().timestamp||0, ...d.data() }));
    merge();
  });
  const u2 = onSnapshot(collection(db,'quiz_attempts',childId,'units'), snap => {
    data.unitDocs = snap.docs.map(d => d.data());
    merge();
  });
  const u3 = onSnapshot(collection(db,'quiz_attempts',childId,'concepts'), snap => {
    data.conceptDocs = snap.docs.map(d => d.data());
    merge();
  });
  addUnsubscriber(u1); addUnsubscriber(u2); addUnsubscriber(u3);
}

function buildWeekPattern(uploads, quizAttempts) {
  const monday = (() => {
    const d = new Date(); d.setHours(0,0,0,0);
    const day = d.getDay(); const diff = day === 0 ? 6 : day - 1;
    d.setDate(d.getDate() - diff); return d.getTime();
  })();
  const MS_DAY = 86400000;

  const revisions = Array.from({length:7}, () => []);
  const homeworks = Array.from({length:7}, () => []);

  uploads.forEach(u => {
    const ts = u.timestamp || u.ts || 0;
    if (!ts || ts < monday || ts >= monday + 7*MS_DAY) return;
    const idx = Math.floor((ts - monday) / MS_DAY);
    const item = { title: u.topic || u.unitName || u.conceptName || 'Upload', subject: u.subject || '', typeTag: 'Upload' };
    const kind = u.kind || u.uploadKind || '';
    if (kind === 'HOMEWORK' || kind === 'TUITION_HW' || u.category === 'home_work') {
      homeworks[idx].push(item);
    } else {
      revisions[idx].push(item);
    }
  });

  quizAttempts.forEach(q => {
    const ts = q.lastUpdated || 0;
    if (!ts || ts < monday || ts >= monday + 7*MS_DAY) return;
    const idx = Math.floor((ts - monday) / MS_DAY);
    revisions[idx].push({ title: q.displayName || 'Quiz', subject: q.subject || '', typeTag: 'Quiz' });
  });

  return Array.from({length:7}, (_, i) => ({
    revisions: revisions[i],
    homeworks: homeworks[i],
    kind: revisions[i].length > 0 && homeworks[i].length > 0 ? 'BOTH'
        : revisions[i].length > 0 ? 'REVISION'
        : homeworks[i].length > 0 ? 'HOMEWORK'
        : 'NONE'
  }));
}

function renderChildCards(children, engagements) {
  const list = document.getElementById('ph-children-list');
  if (!list) return;
  if (!children.length) {
    list.innerHTML = `<div class="ph-empty">No children added yet.</div>`;
    return;
  }
  list.innerHTML = children.map((child, idx) => childStatCard(child, engagements[child.id], idx)).join('');
  list.querySelectorAll('[data-child-id]').forEach(card => {
    card.addEventListener('click', () => {
      navigate('child-pin', { childId: card.dataset.childId, parentId: state.parentId });
    });
  });
  // Wire action buttons
  list.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const id = btn.dataset.id;
      if (btn.dataset.action === 'insights') navigate('insights', { childId: id });
      else navigate('parent-tasks', { childId: id });
    });
  });
}

function childStatCard(child, eng, index) {
  const emoji = genderEmoji(child.gender);
  const grade = child.grade || '—';
  const board = child.board || 'CBSE';
  const daysActive = eng?.daysActiveThisWeek ?? 0;
  const isActiveToday = eng?.isActiveToday ?? false;
  const accuracyPct = eng?.accuracyPct ?? null;
  const streakDays = eng?.streakDays ?? 0;

  // Progress ring SVG
  const pct = accuracyPct ?? 0;
  const radius = 22, stroke = 5;
  const circ = 2 * Math.PI * radius;
  const filled = circ * (pct / 100);
  const progressRing = `
    <svg width="54" height="54" viewBox="0 0 54 54">
      <circle cx="27" cy="27" r="${radius}" fill="none" stroke="#E6EAF5" stroke-width="${stroke}"/>
      ${pct > 0 ? `<circle cx="27" cy="27" r="${radius}" fill="none" stroke="#2F7DFF" stroke-width="${stroke}"
        stroke-dasharray="${filled.toFixed(2)} ${circ.toFixed(2)}"
        stroke-dashoffset="${(circ * 0.25).toFixed(2)}" stroke-linecap="round"/>` : ''}
      <text x="27" y="31" text-anchor="middle" font-size="11" font-weight="700" fill="#1A2B4A" font-family="SFPro,sans-serif">
        ${accuracyPct !== null ? `${pct}%` : '—'}
      </text>
    </svg>`;

  // 7 weekly dots
  const dots = Array.from({length:7}, (_,i) =>
    `<div class="ph-week-dot ${i < daysActive ? 'active' : ''}"></div>`
  ).join('');

  // Tasks tile
  const taskCount = 0; // computed from parent tasks, done at card level
  const hasTasks = false; // simplified — full count computed in renderChildCards
  const tasksColor = hasTasks ? '#EF4444' : '#22C55E';
  const tasksBg   = hasTasks ? '#FFE4E6' : '#DCFCE7';

  // Today tile
  const todayColor = isActiveToday ? '#22C55E' : '#F59E0B';
  const todayBg    = isActiveToday ? '#DCFCE7' : '#FFF3D6';
  const todayIcon  = isActiveToday ? IC_CHECK : IC_CLOCK;
  const todayLabel = isActiveToday ? 'Done' : 'Not yet';

  // Status badge (like Android's streak-based status)
  const statusBg = streakDays >= 3 ? '#DCFCE7' : streakDays > 0 ? '#FFEFCC' : '#FFE4E6';
  const statusFg = streakDays >= 3 ? '#16A34A' : streakDays > 0 ? '#EA580C' : '#EF4444';
  const statusTxt = streakDays >= 3 ? 'Learning well' : streakDays > 0 ? 'Needs consistency' : 'Needs attention';

  return `
    <div class="ph-child-card clickable" data-child-id="${child.id}">
      <div class="ph-card-top">
        <div class="ph-avatar">${emoji}</div>
        <div class="ph-child-info">
          <div class="ph-child-name">${child.name || 'Child'}</div>
          <div class="ph-chips">
            <span class="ph-chip">Class ${grade}</span>
            <span class="ph-chip">${board}</span>
          </div>
          <div class="ph-status-pill" style="background:${statusBg};color:${statusFg}">
            <div class="ph-status-dot" style="background:${statusFg}"></div>
            ${statusTxt}
          </div>
        </div>
        <div class="ph-card-right">
          ${progressRing}
          <div class="ph-chevron">${IC_CHEVRON}</div>
        </div>
      </div>

      <div class="ph-divider"></div>

      <div class="ph-stats-row">
        <!-- Tasks tile (left) -->
        <div class="ph-stat-tile">
          <div class="ph-stat-icon-row">
            <div class="ph-stat-icon-circle" style="background:${tasksBg};color:${tasksColor}">
              ${IC_CHECK}
            </div>
            <div class="ph-stat-val" style="color:#1A2B4A">All done</div>
          </div>
          <div class="ph-stat-label">Tasks by parents</div>
        </div>
        <div class="ph-stat-sep"></div>
        <!-- Weekly streak (centre) -->
        <div class="ph-stat-tile">
          <div class="ph-stat-dots">${dots}</div>
          <div class="ph-stat-val">${daysActive}/7</div>
          <div class="ph-stat-label">Days active this week</div>
        </div>
        <div class="ph-stat-sep"></div>
        <!-- Today (right) -->
        <div class="ph-stat-tile">
          <div class="ph-stat-icon-row">
            <div class="ph-stat-icon-circle" style="background:${todayBg};color:${todayColor}">
              ${todayIcon}
            </div>
            <div class="ph-stat-val">${todayLabel}</div>
          </div>
          <div class="ph-stat-label">Today's Learning</div>
        </div>
      </div>

      <!-- Quick-action row (Insights / Tasks) -->
      <div class="ph-quick-actions mt-12">
        <button class="ph-quick-btn" data-action="insights" data-id="${child.id}">
          ${IC_BARCHART} Insights
        </button>
        <button class="ph-quick-btn" data-action="tasks" data-id="${child.id}">
          ${IC_ASSIGNMENT} Tasks
        </button>
      </div>
    </div>
  `;
}

function renderPendingTasksCard(tasks, children) {
  const card = document.getElementById('ph-tasks-card');
  if (!card) return;
  const now = Date.now();
  const twentyFourH = 24 * 60 * 60 * 1000;
  const actionable = tasks.filter(t => {
    if (t.status === 'REQUESTED' || t.status === 'PENDING_REVIEW') return true;
    if (t.status === 'ASSIGNED' && t.assignedAtMillis > 0 && now - t.assignedAtMillis >= twentyFourH) return true;
    return false;
  });
  if (!actionable.length) { card.innerHTML = ''; return; }

  const t = actionable[0];
  const moreCount = actionable.length - 1;
  const childMap = {};
  children.forEach(c => { childMap[c.id] = c; });
  const childName = childMap[t.childId]?.name || '';

  const { accentBg, accentFg, badgeLabel } = taskBadge(t);
  const headline = parentTaskHeadline(t);
  const sub = [childName, t.isUnitScope ? 'Lesson' : 'Concept', t.subject].filter(Boolean).join(' · ');

  card.innerHTML = `
    <div class="ph-tasks-card">
      <div class="ph-tasks-header">
        <span class="ph-tasks-title">Tasks needing attention</span>
        ${moreCount > 0 ? `<span class="ph-tasks-viewall" id="ph-tasks-view">View all ${IC_CHEVRON}</span>` : ''}
      </div>
      <div class="ph-task-row clickable" id="ph-task-row">
        <div class="ph-task-icon" style="background:${accentBg}">${t.subjectIcon || '📚'}</div>
        <div class="ph-task-info">
          <div class="ph-task-name">${headline}</div>
          <div class="ph-task-sub">${sub}</div>
        </div>
        <div class="ph-task-badge" style="background:${accentBg};color:${accentFg}">${badgeLabel}</div>
      </div>
    </div>
  `;
  card.querySelector('#ph-task-row')?.addEventListener('click', () => navigate('task-review', { taskId: t.id }));
  card.querySelector('#ph-tasks-view')?.addEventListener('click', () => navigate('parent-tasks', { childId: t.childId }));
}

function taskBadge(t) {
  switch(t.status) {
    case 'REQUESTED':     return { accentBg:'#FFF4E0', accentFg:'#B45309', badgeLabel:'New request' };
    case 'PENDING_REVIEW':return { accentBg:'#EFF6FF', accentFg:'#2F7DFF', badgeLabel:'Needs review' };
    case 'ASSIGNED':      return { accentBg:'#FFE4E6', accentFg:'#BE123C', badgeLabel:'Send reminder' };
    default:              return { accentBg:'#F1F5F9', accentFg:'#6B7A99', badgeLabel:'' };
  }
}

function parentTaskHeadline(t) {
  const scope = t.displayName || 'a topic';
  switch(t.status) {
    case 'REQUESTED':      return `Wants to study: ${scope}`;
    case 'PENDING_REVIEW': return `${t.type === 'OUR_QUESTIONS' ? 'Quiz done' : 'Submitted'}: ${scope}`;
    case 'ASSIGNED':       return `Not done yet: ${scope}`;
    default:               return scope;
  }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 1: DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════════

function renderDashboardTab(container) {
  container.innerHTML = `<div class="ph-dash-shell"><div class="loading-placeholder" style="padding:40px;text-align:center">Loading dashboard…</div></div>`;

  const db = getDb();
  const parentId = state.parentId;
  if (!parentId) return;

  let children = [];
  let engagements = {};
  let selectedChildId = null;

  const unsub = onSnapshot(collection(db, 'parents', parentId, 'children'), snap => {
    children = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    if (!selectedChildId && children.length) selectedChildId = children[0].id;
    children.forEach((child, idx) => {
      subscribeChildEngagement(db, child.id, idx, eng => {
        engagements[child.id] = eng;
        drawDashboard();
      });
    });
    drawDashboard();
  });
  addUnsubscriber(unsub);

  function drawDashboard() {
    const shell = container.querySelector('.ph-dash-shell');
    if (!shell || !children.length) return;
    const selected = children.find(c => c.id === selectedChildId) || children[0];
    const eng = engagements[selected.id] || {};

    shell.innerHTML = dashboardHTML(children, selected, eng, selectedChildId);

    // Draw zigzag SVG connector once layout is complete
    requestAnimationFrame(() => drawWeekConnector(shell));

    // Child switcher clicks
    shell.querySelectorAll('[data-switch-child]').forEach(el => {
      el.addEventListener('click', () => {
        selectedChildId = el.dataset.switchChild;
        drawDashboard();
      });
    });
    shell.querySelector('#dash-add-child')?.addEventListener('click', () => navigate('add-child'));
    shell.querySelector('#dash-view-report')?.addEventListener('click', () => navigate('insights', { childId: selected.id }));

    // Activity category tiles
    shell.querySelectorAll('[data-activity]').forEach(el => {
      el.addEventListener('click', () => navigate('activity', { childId: selected.id, category: el.dataset.activity }));
    });

    // Day strip nodes
    shell.querySelectorAll('[data-day-idx]').forEach(el => {
      el.addEventListener('click', () => {
        const idx = +el.dataset.dayIdx;
        const week = eng.weekPattern || buildWeekPattern([], []);
        showDaySheet(week[idx], idx, shell);
      });
    });
  }
}

function dashboardHTML(children, selected, eng, selectedChildId) {
  const streakDays = eng.streakDays || 0;
  const daysStudied = Math.min(eng.daysActiveThisWeek || 0, 7);
  const accuracyPct = eng.accuracyPct;
  const lastActivityMs = eng.lastActivityMs || 0;
  const weakSubject = eng.weakSubject || '—';
  const weakPct = eng.weakSubjectAccuracyPct;
  const week = eng.weekPattern || buildWeekPattern([], []);

  const lastRevisedLabel = (() => {
    if (!lastActivityMs) return 'No activity yet';
    const daysAgo = Math.floor((Date.now() - lastActivityMs) / 86400000);
    return daysAgo === 0 ? 'Today' : daysAgo === 1 ? 'Yesterday' : `${daysAgo} days ago`;
  })();

  const studiedHint = daysStudied >= 5 ? 'Great consistency!' : daysStudied >= 3 ? 'Keep building the habit' : 'Aim for daily practice';
  const lastRevisedHint = lastActivityMs ? 'Keep the streak going!' : 'Start revising to build the habit.';
  const attentionDetail = weakPct !== null ? `${weakPct}% accuracy` : lastActivityMs ? lastRevisedLabel : 'No activity yet';

  const { statusBg, statusFg, statusTxt } = dashStatus(streakDays);

  return `
    <div class="ph-dash-header" style="background:linear-gradient(180deg,#FDF6F0 ${176/260*100}%,rgba(214,230,247,0.55) 100%)">
      <img src="/assets/dashboard_header_bg.png" class="ph-dash-header-bg" alt="" />
      <div class="ph-dash-greeting">
        <div class="ph-dash-hello">Good day, ${state.parentName || 'there'}! 👋</div>
        <div class="ph-dash-hello-sub">Here's how your children are doing</div>
      </div>
      <!-- Child switcher pill -->
      <div class="ph-switcher-card">
        ${children.map((c, i) => `
          <div class="ph-switcher-item ${c.id === selectedChildId ? 'active' : ''}" data-switch-child="${c.id}">
            <div class="ph-switcher-avatar ${c.id === selectedChildId ? 'selected' : ''}">
              <img src="${AVATARS[i % 3]}" alt="${c.name}" />
            </div>
            <div class="ph-switcher-name" style="color:${c.id===selectedChildId?'#3B7BFF':'#16213E'}">${c.name.split(' ')[0]}</div>
            ${c.id === selectedChildId ? '<div class="ph-switcher-indicator"></div>' : '<div style="height:9px"></div>'}
          </div>
          ${i < children.length - 1 ? `<div class="ph-switcher-divider"></div>` : ''}
        `).join('')}
        <div class="ph-switcher-divider"></div>
        <div class="ph-switcher-item" id="dash-add-child">
          <div class="ph-switcher-add-btn">${IC_ADD}</div>
          <div class="ph-switcher-name">Add child</div>
          <div style="height:9px"></div>
        </div>
      </div>
    </div>

    <div class="ph-dash-body">

      <!-- Child summary card -->
      <div class="ph-dash-card">
        <div class="ph-dash-student-header">
          <div style="flex:1">
            <div class="ph-dash-student-name">${selected.name.split(' ')[0]}</div>
            <div class="ph-dash-student-sub">Class ${selected.grade} · ${selected.board || 'CBSE'}</div>
            <div class="ph-status-pill mt-10" style="background:${statusBg};color:${statusFg};display:inline-flex">
              <div class="ph-status-dot" style="background:${statusFg}"></div>
              ${statusTxt}
            </div>
          </div>
          <img src="/assets/dashboard_hero.png" class="ph-dash-hero-img" alt="" />
        </div>

        <!-- 3 stat tiles -->
        <div class="ph-dash-tiles">
          ${insightTile(`${daysStudied}/7`, 'Studied this week', studiedHint, IC_CALENDAR, '#16A34A', '#DCFCE7', '#EFFAF1', '#16A34A')}
          ${insightTile('Last active', lastRevisedLabel, lastRevisedHint, IC_AUTORENEW, '#7C5CF8', '#E8E4FF', '#F1EEFF', '#7C5CF8')}
          ${insightTile('Needs attention', weakSubject, attentionDetail, IC_BOOK, '#F59E0B', '#FFE7B5', '#FFF1DA', '#F59E0B')}
        </div>

        <!-- Parent insight block -->
        ${parentInsightBlock(streakDays, daysStudied)}

        <div class="ph-dash-divider"></div>

        <!-- View full report -->
        <div class="ph-dash-report-row clickable" id="dash-view-report">
          <div class="ph-dash-report-icon">${IC_BARCHART}</div>
          <span class="ph-dash-report-text">View full report</span>
          <span class="ph-dash-report-chevron">${IC_CHEVRON}</span>
        </div>
      </div>

      <!-- Activity categories -->
      <div class="ph-dash-card mt-16">
        <div class="ph-section-title" style="font-size:15px">Explore activity</div>
        <div class="ph-section-sub" style="margin-top:2px;margin-bottom:14px">Tap to see detailed activity logs</div>
        <div class="ph-activity-grid">
          <div class="ph-activity-tile clickable" data-activity="school_work" style="background:#D0E4FF">
            <div class="ph-activity-icon" style="background:white;color:#2F7DFF">${IC_SCHOOL}</div>
            <div class="ph-activity-label">School work</div>
          </div>
          <div class="ph-activity-tile clickable" data-activity="home_work" style="background:#FEF3C7">
            <div class="ph-activity-icon" style="background:white;color:#D97706">${IC_EDIT_NOTE}</div>
            <div class="ph-activity-label">Homework</div>
          </div>
          <div class="ph-activity-tile clickable" data-activity="self_learning" style="background:#DCFCE7">
            <div class="ph-activity-icon" style="background:white;color:#16A34A">${IC_BRAIN}</div>
            <div class="ph-activity-label">Self learning</div>
          </div>
        </div>
      </div>

      <!-- Weekly learning pattern -->
      <div class="ph-dash-card mt-16">
        ${weeklyPatternHTML(week, selected.name.split(' ')[0])}
      </div>

    </div>
  `;
}

function dashStatus(streakDays) {
  if (streakDays >= 3) return { statusBg:'#DCFCE7', statusFg:'#16A34A', statusTxt:'Learning well' };
  if (streakDays > 0)  return { statusBg:'#FFEFCC', statusFg:'#EA580C', statusTxt:'Needs consistency' };
  return { statusBg:'#FFE4E6', statusFg:'#EF4444', statusTxt:'Needs attention' };
}

function insightTile(title, subtitle, hint, icon, tint, tintBg, cardBg, titleColor) {
  return `
    <div class="ph-insight-tile" style="background:${cardBg}">
      <div class="ph-insight-icon" style="background:${tintBg};color:${tint}">${icon}</div>
      <div class="ph-insight-title" style="color:${titleColor}">${title}</div>
      <div class="ph-insight-subtitle">${subtitle}</div>
      <div class="ph-insight-hint">${hint}</div>
    </div>
  `;
}

function parentInsightBlock(streakDays, daysStudied) {
  let bg, accent, iconHtml, title, body;
  if (streakDays >= 3 || daysStudied >= 5) {
    bg = '#E6F8EC'; accent = '#16A34A';
    iconHtml = `<div class="ph-insight-block-icon" style="background:#D4F5DF;color:#16A34A">${IC_SPARKLE}</div>`;
    title = 'Great learning balance this week';
    body = 'Homework and revision are both consistent. Keep encouraging this habit!';
  } else if (daysStudied >= 3) {
    bg = '#EEF3FF'; accent = '#2F7DFF';
    iconHtml = `<div class="ph-insight-block-icon" style="background:#D6E4FF;color:#2F7DFF">${IC_BRAIN}</div>`;
    title = 'Building this week\'s picture';
    body = 'As uploads and quizzes come in, insights will appear here.';
  } else {
    bg = '#FFF4DD'; accent = '#B45309';
    iconHtml = `<div class="ph-insight-block-icon" style="background:#FCE7B5;color:#B45309">${IC_HELP}</div>`;
    title = 'Several skipped days';
    body = 'Try encouraging one small revision or homework upload daily.';
  }
  return `
    <div class="ph-insight-block" style="background:${bg}">
      <div class="ph-insight-block-row">
        ${iconHtml}
        <div style="flex:1;min-width:0">
          <div class="ph-insight-block-title" style="color:${accent}">${title}</div>
          <div class="ph-insight-block-body">${body}</div>
        </div>
      </div>
    </div>
  `;
}

function weeklyPatternHTML(week, firstName) {
  const DAY_LABELS = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
  const today = new Date();
  const offsetFromMonday = (today.getDay() + 6) % 7;
  const dates = DAY_LABELS.map((_, i) => {
    const d = new Date(today);
    d.setDate(today.getDate() - offsetFromMonday + i);
    return `${d.getDate()} ${d.toLocaleString('default',{month:'short'})}`;
  });

  const nodeHTML = week.map((day, i) => {
    const isEven = i % 2 === 0;
    const { bg, icon, iconTint, ring, ringColor } = dayNodeStyle(day.kind);
    const nodeEl = ring
      ? `<div class="ph-day-node ph-day-node-ringed" style="border-color:${ringColor};background:${bg}" data-day-idx="${i}">${icon}</div>`
      : `<div class="ph-day-node" style="background:${bg};color:${iconTint}" data-day-idx="${i}">${icon}</div>`;
    return `
      <div class="ph-day-col" style="align-items:center;justify-content:${isEven?'flex-start':'flex-end'}">
        ${nodeEl}
      </div>
    `;
  }).join('');

  const legendItems = [
    { kind:'REVISION', label:'Revision', desc:'Quiz, topic upload' },
    { kind:'HOMEWORK', label:'Homework', desc:'School/tuition HW' },
    { kind:'BOTH',     label:'Both',     desc:'Revision + HW' },
    { kind:'NONE',     label:'No activity', desc:'Nothing recorded' },
  ];

  const { patternTitle, patternBody } = derivePatternInsight(week);

  return `
    <div class="ph-week-header">
      <div class="ph-week-title">This week's learning pattern ${IC_HELP}</div>
      <div class="ph-week-sub">${firstName}'s activity this week — tap any day for details</div>
    </div>
    <!-- Day labels + dates -->
    <div class="ph-day-labels">
      ${DAY_LABELS.map((l,i) => `
        <div class="ph-day-label-col">
          <div class="ph-day-label-name">${l}</div>
          <div class="ph-day-label-date">${dates[i].split(' ')[0]}</div>
        </div>
      `).join('')}
    </div>
    <!-- Zigzag node strip -->
    <div class="ph-day-strip">
      <svg class="ph-day-connector" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
        <polyline class="ph-connector-line" points="" />
      </svg>
      <div class="ph-day-nodes">
        ${week.map((day, i) => {
          const { bg, icon, iconTint, ring, ringColor } = dayNodeStyle(day.kind);
          return `<div class="ph-day-col-wrap ${i%2===0?'top':'bottom'}">
            <div class="ph-day-node${ring?' ph-day-node-ringed':''}" data-day-idx="${i}"
              style="${ring?`border-color:${ringColor};background:${bg}`:``}background:${bg};color:${iconTint}">
              ${icon}
            </div>
          </div>`;
        }).join('')}
      </div>
    </div>
    <!-- Pattern insight banner -->
    <div class="ph-pattern-banner">
      <div class="ph-pattern-banner-icon">${IC_SPARKLE}</div>
      <div style="flex:1;min-width:0">
        <div class="ph-pattern-banner-title">${patternTitle}</div>
        <div class="ph-pattern-banner-body">${patternBody}</div>
      </div>
      <div style="color:#2F7DFF">${IC_ASSIGNMENT}</div>
    </div>
    <!-- Legend -->
    <div class="ph-legend">
      ${legendItems.map(l => {
        const { bg, icon, iconTint, ring, ringColor } = dayNodeStyle(l.kind);
        return `<div class="ph-legend-cell">
          <div class="ph-legend-node${ring?' ph-day-node-ringed':''}"
            style="${ring?`border-color:${ringColor};background:${bg}`:``}background:${bg};color:${iconTint}">
            ${icon}
          </div>
          <div class="ph-legend-label">${l.label}</div>
          <div class="ph-legend-desc">${l.desc}</div>
        </div>`;
      }).join('')}
    </div>
  `;
}

function drawWeekConnector(container) {
  const strip = container.querySelector('.ph-day-strip');
  const polyline = container.querySelector('.ph-connector-line');
  if (!strip || !polyline) return;
  const W = strip.offsetWidth;
  const H = strip.offsetHeight || 90;
  const topY = 8 + 18;          // paddingTop + nodeRadius
  const botY = H - 8 - 18;      // H - paddingBottom - nodeRadius
  const pts = Array.from({length: 7}, (_, i) => {
    const x = ((i + 0.5) / 7 * W).toFixed(1);
    const y = i % 2 === 0 ? topY : botY;
    return `${x},${y}`;
  });
  polyline.setAttribute('points', pts.join(' '));
}

function dayNodeStyle(kind) {
  switch(kind) {
    case 'REVISION': return { bg:'#2F7DFF', icon:IC_EDIT, iconTint:'white', ring:false };
    case 'HOMEWORK': return { bg:'#22C55E', icon:IC_BOOK, iconTint:'white', ring:false };
    case 'BOTH':     return { bg:'#22C55E', icon:IC_BOOK, iconTint:'white', ring:true, ringColor:'#2F7DFF' };
    default:         return { bg:'#E5E7EB', icon:IC_MINUS, iconTint:'#9CA3AF', ring:false };
  }
}

function derivePatternInsight(week) {
  const DAY_LABELS = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
  const bothDays = week.filter(d => d.kind === 'BOTH');
  const revOnly  = week.filter(d => d.kind === 'REVISION');
  const hwOnly   = week.filter(d => d.kind === 'HOMEWORK');
  const skipped  = week.filter(d => d.kind === 'NONE');
  if (bothDays.length >= 2) return { patternTitle:'Great balance this week!', patternBody:`${bothDays.length} balanced learning days. Keep it up!` };
  if (revOnly.length >= 4)  return { patternTitle:'Strong revision habit', patternBody:`Revision on ${revOnly.length} days — try adding a homework day.` };
  if (hwOnly.length >= 4)   return { patternTitle:'Homework on track', patternBody:'Most days had homework. Add a short revision to balance learning.' };
  if (skipped.length >= 4)  return { patternTitle:'Several skipped days', patternBody:`${skipped.length} days had no activity. Try one small revision daily.` };
  if (bothDays.length + revOnly.length + hwOnly.length === 0) return { patternTitle:'Nothing yet', patternBody:'No activity recorded this week.' };
  return { patternTitle:'Mixed week', patternBody:'Keep building daily habits — even short sessions help.' };
}

function showDaySheet(day, dayIndex, container) {
  const DAY_FULL = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'];
  const rev = day?.revisions || [];
  const hw  = day?.homeworks  || [];
  const existing = container.querySelector('.ph-day-sheet-overlay');
  if (existing) existing.remove();

  const overlay = document.createElement('div');
  overlay.className = 'ph-day-sheet-overlay';
  overlay.innerHTML = `
    <div class="ph-day-sheet-backdrop"></div>
    <div class="ph-day-sheet">
      <div class="ph-day-sheet-handle"></div>
      <div class="ph-day-sheet-title">${DAY_FULL[dayIndex] || 'Day'}</div>
      <div class="ph-day-sheet-sub">${rev.length + hw.length === 0 ? 'No activity recorded.' :
        [rev.length ? `${rev.length} revision${rev.length>1?'s':''}` : '',
         hw.length  ? `${hw.length} homework${hw.length>1?'s':''}` : ''].filter(Boolean).join(' • ')
      }</div>
      <div class="ph-day-sheet-content">
        ${rev.length === 0 && hw.length === 0 ? `
          <div class="ph-day-empty">
            <div style="color:#9CA3AF;font-size:28px;margin-bottom:8px">${IC_MINUS}</div>
            <div style="font-weight:600;color:#1A2B4A">No activity</div>
            <div style="font-size:12px;color:#6B7A99;margin-top:4px">No learning recorded this day</div>
          </div>
        ` : ''}
        ${rev.length ? `
          <div class="ph-day-section">
            <div class="ph-day-section-header" style="color:#2F7DFF">
              <div class="ph-day-section-dot" style="background:#DBEAFE;color:#2F7DFF">${IC_EDIT}</div>
              Revision
            </div>
            ${rev.map(item => dayItemRow(item)).join('')}
          </div>
        ` : ''}
        ${rev.length && hw.length ? '<div style="height:18px"></div>' : ''}
        ${hw.length ? `
          <div class="ph-day-section">
            <div class="ph-day-section-header" style="color:#16A34A">
              <div class="ph-day-section-dot" style="background:#DCFCE7;color:#16A34A">${IC_BOOK}</div>
              Homework
            </div>
            ${hw.map(item => dayItemRow(item)).join('')}
          </div>
        ` : ''}
      </div>
    </div>
  `;
  container.appendChild(overlay);
  overlay.querySelector('.ph-day-sheet-backdrop').addEventListener('click', () => overlay.remove());
  setTimeout(() => overlay.querySelector('.ph-day-sheet').classList.add('open'), 10);
}

function dayItemRow(item) {
  const tagColors = {
    'Quiz':         { fg:'#3B7BFF', bg:'#EAF0FF' },
    'Write Answer': { fg:'#7C3AED', bg:'#F3EEFF' },
    'Class Work':   { fg:'#0891B2', bg:'#E0F7FA' },
    'Self Study':   { fg:'#059669', bg:'#E6F7F1' },
    'Homework':     { fg:'#D97706', bg:'#FFF3E0' },
    'Upload':       { fg:'#6B7A99', bg:'#F3F4F6' },
  };
  const tag = tagColors[item.typeTag] || tagColors['Upload'];
  return `
    <div class="ph-day-item">
      <div style="flex:1;min-width:0">
        <div class="ph-day-item-title">${item.title}</div>
        <div class="ph-day-item-sub">
          ${item.subject ? `<span>${item.subject}</span>` : ''}
          ${item.typeTag ? `<span class="ph-day-item-tag" style="color:${tag.fg};background:${tag.bg}">${item.typeTag}</span>` : ''}
        </div>
      </div>
      <div style="color:#9BA8C0">${IC_CHEVRON}</div>
    </div>
  `;
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 2: PROFILE / SETTINGS
// ═══════════════════════════════════════════════════════════════════════════════

function renderSettingsTab(container) {
  const db = getDb();
  const parentId = state.parentId;

  // Load children for the switcher
  const unsub = onSnapshot(collection(db, 'parents', parentId, 'children'), snap => {
    const children = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    drawSettings(container, children);
  });
  addUnsubscriber(unsub);
}

function drawSettings(container, children) {
  const name    = state.parentName || 'Your name';
  const verified = !!name && name !== 'Your name';

  container.innerHTML = `
    <div class="ph-settings-shell">

      <!-- Profile card -->
      <div class="ph-profile-big-card">
        <div style="position:relative;display:inline-block">
          <div class="ph-profile-big-avatar">
            <div style="color:#B45309;width:48px;height:48px">${IC_PERSON}</div>
          </div>
          <div class="ph-profile-edit-badge">${IC_EDIT}</div>
        </div>
        <div style="margin-left:14px;flex:1;min-width:0">
          <div class="ph-profile-big-name">${name}</div>
          <div class="ph-profile-big-sub">Eduksha Parent Account</div>
          <div class="ph-profile-badge ${verified ? 'verified' : 'unverified'}">
            ${verified ? IC_SHIELD : IC_PERSON}
            ${verified ? 'Verified parent account' : 'Complete your profile'}
          </div>
        </div>
        <div style="color:#A0ADC2">${IC_CHEVRON}</div>
      </div>

      <!-- Children switcher -->
      <div class="ph-settings-section-label">Children</div>
      <div class="ph-children-switcher-card">
        ${children.map((c, i) => `
          <div class="ph-switcher-item" style="cursor:default">
            <div class="ph-switcher-avatar selected">
              <img src="${AVATARS[i % 3]}" alt="${c.name}" />
            </div>
            <div class="ph-switcher-name" style="color:#16213E">${c.name.split(' ')[0]}</div>
            <div style="height:9px"></div>
          </div>
          ${i < children.length - 1 ? `<div class="ph-switcher-divider"></div>` : ''}
        `).join('')}
        ${children.length ? '<div class="ph-switcher-divider"></div>' : ''}
        <div class="ph-switcher-item" id="settings-add-child" style="cursor:pointer">
          <div class="ph-switcher-add-btn">${IC_ADD}</div>
          <div class="ph-switcher-name">Add child</div>
          <div style="height:9px"></div>
        </div>
      </div>

      <!-- Learning settings -->
      <div class="ph-settings-section-label">Learning</div>
      <div class="ph-settings-group">
        <div class="ph-settings-row" id="s-study-goals">
          <div class="ph-settings-icon" style="background:#EAE3FF;color:#7C5CF8">${IC_GPS}</div>
          <div class="ph-settings-text">
            <div class="ph-settings-row-title">Study Goals</div>
            <div class="ph-settings-row-sub">Set and track learning goals for each child</div>
          </div>
          <div style="color:#A0ADC2">${IC_CHEVRON}</div>
        </div>
        <div class="ph-settings-divider"></div>
        <div class="ph-settings-row" id="s-language">
          <div class="ph-settings-icon" style="background:#EEF5FF;color:#3B7BFF">${IC_LANGUAGE}</div>
          <div class="ph-settings-text">
            <div class="ph-settings-row-title">Language</div>
            <div class="ph-settings-row-sub">App language preference</div>
          </div>
          <span style="font-size:13px;font-weight:600;color:#3B7BFF;margin-right:6px">English</span>
          <div style="color:#A0ADC2">${IC_CHEVRON}</div>
        </div>
      </div>

      <!-- Account settings -->
      <div class="ph-settings-section-label">Account</div>
      <div class="ph-settings-group">
        <div class="ph-settings-row" id="s-privacy">
          <div class="ph-settings-icon" style="background:#E6F7EC;color:#16A34A">${IC_LOCK}</div>
          <div class="ph-settings-text">
            <div class="ph-settings-row-title">Privacy &amp; Data</div>
            <div class="ph-settings-row-sub">Manage your data and privacy settings</div>
          </div>
          <div style="color:#A0ADC2">${IC_CHEVRON}</div>
        </div>
        <div class="ph-settings-divider"></div>
        <div class="ph-settings-row" id="s-faq">
          <div class="ph-settings-icon" style="background:#EEF5FF;color:#3B7BFF">${IC_HELP}</div>
          <div class="ph-settings-text">
            <div class="ph-settings-row-title">FAQ</div>
            <div class="ph-settings-row-sub">Frequently asked questions</div>
          </div>
          <div style="color:#A0ADC2">${IC_CHEVRON}</div>
        </div>
        <div class="ph-settings-divider"></div>
        <div class="ph-settings-row" id="s-support">
          <div class="ph-settings-icon" style="background:#FFEFCC;color:#EA580C">${IC_SUPPORT}</div>
          <div class="ph-settings-text">
            <div class="ph-settings-row-title">Help &amp; Support</div>
            <div class="ph-settings-row-sub">Contact us for help</div>
          </div>
          <div style="color:#A0ADC2">${IC_CHEVRON}</div>
        </div>
        <div class="ph-settings-divider"></div>
        <div class="ph-settings-row" id="s-signout">
          <div class="ph-settings-icon" style="background:#FEE2E2;color:#DC2626">${IC_LOGOUT}</div>
          <div class="ph-settings-text">
            <div class="ph-settings-row-title" style="color:#DC2626">Sign Out</div>
            <div class="ph-settings-row-sub">Sign out of your account</div>
          </div>
          <div style="color:#A0ADC2">${IC_CHEVRON}</div>
        </div>
      </div>

      <!-- Trust footer -->
      <div class="ph-trust-footer">
        <div style="width:12px;height:12px;color:#A0ADC2">${IC_CHECK_CIRCLE}</div>
        <span>Your data is encrypted and never shared</span>
      </div>

    </div>
  `;

  container.querySelector('#settings-add-child')?.addEventListener('click', () => navigate('add-child'));
  container.querySelector('#s-privacy')?.addEventListener('click', () => window.open('https://eduksha.in/privacy.html','_blank'));
  container.querySelector('#s-faq')?.addEventListener('click', () => window.open('https://eduksha.in/#faq','_blank'));
  container.querySelector('#s-support')?.addEventListener('click', () => window.open('https://eduksha.in/support.html','_blank'));
  container.querySelector('#s-signout')?.addEventListener('click', () => { if(confirm('Sign out?')) doSignOut(); });
}

// ═══════════════════════════════════════════════════════════════════════════════
// DETAIL SCREENS (Add Child, Task Review, Tasks List, Create Task, Insights, Activity)
// ═══════════════════════════════════════════════════════════════════════════════

export function renderAddChild({ editChildId } = {}) {
  const isEdit = !!editChildId;
  setScreen(`
    <div class="detail-screen" style="background:#F3F6FD">
      <div class="app-bar" style="background:#F3F6FD">
        <button class="icon-btn" id="ac-back">${IC_BACK}</button>
        <h2>${isEdit ? 'Edit Child' : 'Add a Child'}</h2>
      </div>
      <div class="add-child-form">
        <div class="form-group">
          <label class="form-label">Child's name</label>
          <input id="ac-name" type="text" class="input-full" placeholder="e.g. Rahul" maxlength="40" />
        </div>
        <div class="form-group">
          <label class="form-label">Gender</label>
          <div class="chip-group">
            <button class="chip" data-gender="boy">👦 Boy</button>
            <button class="chip" data-gender="girl">👧 Girl</button>
            <button class="chip" data-gender="other">🧒 Other</button>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Class / Grade</label>
          <div class="chip-group">
            ${[1,2,3,4,5,6,7,8,9,10].map(g => `
              <button class="chip ${g === 6 || g === 7 ? '' : 'chip-disabled'}" data-grade="${g}"
                ${g !== 6 && g !== 7 ? 'disabled title="Coming soon"' : ''}>${g}</button>
            `).join('')}
          </div>
          <div class="form-hint mt-8">Classes 6 &amp; 7 supported. More coming soon.</div>
        </div>
        <div class="form-group">
          <label class="form-label">Board</label>
          <div class="chip-group">
            <button class="chip chip-active" data-board="CBSE">CBSE</button>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">4-digit PIN</label>
          <div class="pin-setup">
            <input class="pin-digit" type="tel" maxlength="1" inputmode="numeric" data-pin="0" />
            <input class="pin-digit" type="tel" maxlength="1" inputmode="numeric" data-pin="1" />
            <input class="pin-digit" type="tel" maxlength="1" inputmode="numeric" data-pin="2" />
            <input class="pin-digit" type="tel" maxlength="1" inputmode="numeric" data-pin="3" />
          </div>
          <div class="form-hint mt-8">Child uses this PIN to log in on their device.</div>
        </div>
        <button class="btn btn-primary btn-full btn-lg mt-8" id="ac-save">
          ${isEdit ? 'Save Changes' : 'Add Child →'}
        </button>
      </div>
    </div>
  `);

  $('#ac-back').addEventListener('click', () => history.back());

  let selectedGender = null, selectedGrade = null, selectedBoard = 'CBSE';

  $$('[data-gender]').forEach(btn => btn.addEventListener('click', () => {
    $$('[data-gender]').forEach(b => b.classList.remove('chip-active'));
    btn.classList.add('chip-active');
    selectedGender = btn.dataset.gender;
  }));
  $$('[data-grade]').forEach(btn => btn.addEventListener('click', () => {
    if (btn.disabled) return;
    $$('[data-grade]').forEach(b => b.classList.remove('chip-active'));
    btn.classList.add('chip-active');
    selectedGrade = +btn.dataset.grade;
  }));

  // PIN auto-advance
  const pinInputs = $$('.pin-digit');
  pinInputs.forEach((inp, i) => {
    inp.addEventListener('input', () => {
      if (inp.value && i < pinInputs.length - 1) pinInputs[i+1].focus();
    });
    inp.addEventListener('keydown', e => {
      if (e.key === 'Backspace' && !inp.value && i > 0) pinInputs[i-1].focus();
    });
  });

  $('#ac-save').addEventListener('click', async () => {
    const name = $('#ac-name').value.trim();
    const pin = pinInputs.map(i => i.value).join('');
    if (!name) return showError('Enter child\'s name');
    if (!selectedGender) return showError('Select gender');
    if (!selectedGrade) return showError('Select class');
    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) return showError('Enter a 4-digit PIN');

    showLoading('Saving…');
    try {
      const db = getDb();
      const parentId = state.parentId;
      const data = { name, gender: selectedGender, grade: selectedGrade, board: selectedBoard, pin };

      if (isEdit) {
        await setDoc(doc(db, 'parents', parentId, 'children', editChildId), data, { merge: true });
      } else {
        await addDoc(collection(db, 'parents', parentId, 'children'), data);
      }
      hideLoading();
      showSuccess(isEdit ? 'Child updated!' : 'Child added!');
      history.back();
    } catch(e) {
      hideLoading();
      showError('Could not save. Try again.');
    }
  });
}

export function renderTaskReview({ taskId } = {}) {
  if (!taskId) { history.back(); return; }
  setScreen(`
    <div class="detail-screen" style="background:#F3F6FD">
      <div class="app-bar" style="background:#F3F6FD">
        <button class="icon-btn" id="tr-back">${IC_BACK}</button>
        <h2>Task Review</h2>
      </div>
      <div id="tr-content" class="detail-content">
        <div class="loading-placeholder">Loading…</div>
      </div>
    </div>
  `);
  $('#tr-back').addEventListener('click', () => history.back());

  const db = getDb();
  const unsub = onSnapshot(doc(db, 'parentTaskRequests', taskId), snap => {
    const content = $('#tr-content');
    if (!content) return;
    if (!snap.exists()) { content.innerHTML = '<p class="text-secondary p-20">Task not found.</p>'; return; }
    const t = { id: snap.id, ...snap.data() };
    reviewTask(t, content);
  });
  addUnsubscriber(unsub);
}

function reviewTask(t, container) {
  const scope = t.displayName || (t.isUnitScope ? t.unitName : t.conceptName) || 'topic';

  const photoGrid = t.photoUrls?.length ? `
    <div class="photo-grid mt-16">
      ${t.photoUrls.map(url => `<img src="${url}" class="photo-thumb" onclick="window.open('${url}','_blank')" />`).join('')}
    </div>` : '';

  container.innerHTML = `
    <div class="card mb-16">
      <div class="d-flex align-center gap-12">
        <div class="task-icon-circle" style="background:#FFF1E0;width:52px;height:52px;font-size:26px">${t.subjectIcon || '🎯'}</div>
        <div class="flex-1">
          <div class="fw-bold" style="font-size:16px">${scope}</div>
          <div class="text-secondary">${t.subject || ''}</div>
        </div>
      </div>
      ${t.customNote ? `<div class="mt-12 p-12 rounded" style="background:#F8FAFF;font-size:13px">${t.customNote}</div>` : ''}
      ${photoGrid}
    </div>

    ${t.status === 'PENDING_REVIEW' ? `
      <div class="card" style="background:#FEF3C7;border-color:#FDE68A;margin-bottom:16px">
        <div class="fw-semibold">📋 Submitted for review</div>
        <div class="text-secondary mt-4">Child has submitted this task. Review and respond below.</div>
      </div>
      <div class="form-group">
        <label class="form-label">Feedback for child (optional)</label>
        <textarea id="tr-note" class="input-full" rows="3" placeholder="Great work! / Try again because…" style="resize:none"></textarea>
      </div>
      <button class="btn btn-primary btn-full" id="tr-accept">✓ Accept submission</button>
      <button class="btn btn-ghost btn-full mt-8" id="tr-reject">✗ Needs redo</button>
    ` : ''}

    ${t.status === 'REQUESTED' ? `
      <div class="card" style="background:#FFF7ED;border-color:#FED7AA;margin-bottom:16px">
        <div class="fw-semibold">📩 Task request from child</div>
        <div class="text-secondary mt-4">Your child wants to study this topic. Assign it as a task!</div>
      </div>
      <button class="btn btn-primary btn-full" id="tr-assign">✓ Assign this task</button>
    ` : ''}

    ${t.status === 'ACCEPTED' ? `
      <div class="card" style="background:#DCFCE7;border-color:#BBF7D0">
        <div class="fw-semibold" style="color:#15803D">✓ Accepted — great job!</div>
        ${t.parentReviewNote ? `<div class="text-secondary mt-4">${t.parentReviewNote}</div>` : ''}
      </div>
    ` : ''}
  `;

  const db = getDb();
  container.querySelector('#tr-accept')?.addEventListener('click', async () => {
    const note = container.querySelector('#tr-note')?.value.trim() || '';
    showLoading('Saving…');
    try {
      await updateDoc(doc(db, 'parentTaskRequests', t.id), { status: 'ACCEPTED', parentReviewNote: note, reviewedAtMillis: Date.now() });
      hideLoading(); showSuccess('Task accepted!'); history.back();
    } catch(e) { hideLoading(); showError('Could not update.'); }
  });
  container.querySelector('#tr-reject')?.addEventListener('click', async () => {
    const note = container.querySelector('#tr-note')?.value.trim() || '';
    showLoading('Saving…');
    try {
      await updateDoc(doc(db, 'parentTaskRequests', t.id), { status: 'REJECTED', parentReviewNote: note, reviewedAtMillis: Date.now() });
      hideLoading(); showSuccess('Sent back for redo.'); history.back();
    } catch(e) { hideLoading(); showError('Could not update.'); }
  });
  container.querySelector('#tr-assign')?.addEventListener('click', async () => {
    showLoading('Saving…');
    try {
      await updateDoc(doc(db, 'parentTaskRequests', t.id), { status: 'ASSIGNED', assignedAtMillis: Date.now() });
      hideLoading(); showSuccess('Task assigned!'); history.back();
    } catch(e) { hideLoading(); showError('Could not assign.'); }
  });
}

export function renderParentTasks({ childId } = {}) {
  const cid = childId || state.childId;
  setScreen(`
    <div class="detail-screen" style="background:#F3F6FD">
      <div class="app-bar" style="background:#F3F6FD">
        <button class="icon-btn" id="pt-back">${IC_BACK}</button>
        <h2>Tasks</h2>
      </div>
      <div class="tab-bar" id="pt-tabs">
        <button class="tab-btn active" data-status="">All</button>
        <button class="tab-btn" data-status="ASSIGNED">To do</button>
        <button class="tab-btn" data-status="PENDING_REVIEW">Review</button>
        <button class="tab-btn" data-status="ACCEPTED">Done</button>
      </div>
      <div id="pt-list" class="detail-content"></div>
    </div>
  `);
  $('#pt-back').addEventListener('click', () => history.back());

  let allTasks = [], activeFilter = '';

  function renderList() {
    const list = $('#pt-list');
    if (!list) return;
    const filtered = activeFilter ? allTasks.filter(t => t.status === activeFilter) : allTasks;
    if (!filtered.length) {
      list.innerHTML = '<div class="empty-state p-20"><p class="text-secondary">No tasks here yet.</p></div>';
      return;
    }
    list.innerHTML = filtered.map(t => `
      <div class="card task-summary-card mb-12 clickable" data-id="${t.id}">
        <div class="d-flex align-center gap-12">
          <div class="task-icon-circle" style="background:#FFF1E0">${t.subjectIcon || '🎯'}</div>
          <div class="flex-1">
            <div class="fw-semibold">${t.displayName || t.unitName || t.conceptName || 'Task'}</div>
            <div class="text-secondary" style="font-size:12px">${t.subject || ''}</div>
          </div>
          <span class="badge" style="background:${taskStatusColor(t.status)}22;color:${taskStatusColor(t.status)}">${taskStatusLabel(t.status)}</span>
        </div>
      </div>
    `).join('');
    list.querySelectorAll('[data-id]').forEach(el => {
      el.addEventListener('click', () => navigate('task-review', { taskId: el.dataset.id }));
    });
  }

  $$('#pt-tabs .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      $$('#pt-tabs .tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeFilter = btn.dataset.status;
      renderList();
    });
  });

  const db = getDb();
  const q = cid
    ? query(collection(db, 'parentTaskRequests'), where('childId', '==', cid))
    : query(collection(db, 'parentTaskRequests'), where('parentId', '==', state.parentId));
  const unsub = onSnapshot(q, snap => {
    allTasks = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    allTasks.sort((a,b) => (b.requestedAtMillis||0) - (a.requestedAtMillis||0));
    renderList();
  });
  addUnsubscriber(unsub);
}

export function renderCreateTask({ childId } = {}) {
  setScreen(`
    <div class="detail-screen" style="background:#F3F6FD">
      <div class="app-bar" style="background:#F3F6FD">
        <button class="icon-btn" id="ct-back">${IC_BACK}</button>
        <h2>Create Task</h2>
      </div>
      <div class="detail-content">
        <p class="text-secondary">Use the concept browser to assign a task to your child.</p>
        <button class="btn btn-primary btn-full mt-16" id="ct-browse">📚 Browse Concepts →</button>
      </div>
    </div>
  `);
  $('#ct-back').addEventListener('click', () => history.back());
  $('#ct-browse').addEventListener('click', () => navigate('concept-list'));
}

export function renderInsights({ childId } = {}) {
  const cid = childId || state.childId;
  setScreen(`
    <div class="detail-screen" style="background:#F3F6FD">
      <div class="app-bar" style="background:#F3F6FD">
        <button class="icon-btn" id="ins-back">${IC_BACK}</button>
        <h2>Insights</h2>
      </div>
      <div id="ins-content" class="detail-content">
        <div class="loading-placeholder">Loading…</div>
      </div>
    </div>
  `);
  $('#ins-back').addEventListener('click', () => history.back());
  if (!cid) return;

  const db = getDb();
  Promise.all([
    new Promise(res => { const u = onSnapshot(query(collection(db,'uploads'), where('childId','==',cid)), s => { res(s.docs.map(d=>d.data())); u(); }); }),
    new Promise(res => { const u = onSnapshot(collection(db,'quiz_attempts',cid,'units'), s => { res(s.docs.map(d=>d.data())); u(); }); }),
    new Promise(res => { const u = onSnapshot(collection(db,'quiz_attempts',cid,'concepts'), s => { res(s.docs.map(d=>d.data())); u(); }); }),
  ]).then(([uploads, units, concepts]) => {
    const content = $('#ins-content');
    if (!content) return;

    const allAttempts = [...units, ...concepts].filter(a => a.lastTotal > 0);
    allAttempts.sort((a,b) => (b.lastUpdated||0) - (a.lastUpdated||0));

    const totalCorrect = allAttempts.reduce((s,r) => s+(r.lastCorrect||0), 0);
    const totalAttempted = allAttempts.reduce((s,r) => s+(r.lastTotal||0), 0);
    const accuracy = totalAttempted > 0 ? Math.round((totalCorrect/totalAttempted)*100) : null;

    const subjMap = {};
    allAttempts.forEach(r => {
      if (!r.subject) return;
      if (!subjMap[r.subject]) subjMap[r.subject] = { correct:0, total:0 };
      subjMap[r.subject].correct += r.lastCorrect||0;
      subjMap[r.subject].total  += r.lastTotal||0;
    });

    content.innerHTML = `
      <div class="insight-summary card mb-16">
        <div class="fw-bold mb-8" style="font-size:16px">Learning Summary</div>
        <div class="insight-row">
          <span class="insight-label">Total uploads</span>
          <span class="fw-bold">${uploads.length}</span>
        </div>
        <div class="insight-row">
          <span class="insight-label">Quizzes taken</span>
          <span class="fw-bold">${allAttempts.length}</span>
        </div>
        <div class="insight-row">
          <span class="insight-label">Overall accuracy</span>
          <span class="fw-bold" style="color:${accuracy !== null && accuracy >= 70 ? '#16A34A':'#EF4444'}">${accuracy !== null ? accuracy+'%' : '—'}</span>
        </div>
      </div>

      ${Object.keys(subjMap).length ? `
        <div class="section-header mt-8"><span class="section-title">By Subject</span></div>
        ${Object.entries(subjMap).map(([subj, stat]) => {
          const pct = Math.round((stat.correct/stat.total)*100);
          return `
            <div class="card mb-8">
              <div class="d-flex align-center gap-12">
                <div class="subj-icon-sm">${subjectIcon(subj)}</div>
                <div class="flex-1">
                  <div class="fw-semibold">${subj}</div>
                  <div class="text-secondary" style="font-size:12px">${stat.correct}/${stat.total} correct</div>
                </div>
                <div class="fw-bold" style="color:${pct>=70?'#16A34A':'#EF4444'}">${pct}%</div>
              </div>
              <div class="progress-track mt-8"><div class="progress-fill" style="width:${pct}%;background:${pct>=70?'#16A34A':'#EF4444'}"></div></div>
            </div>
          `;
        }).join('')}
      ` : ''}
    `;
  });
}

export function renderActivity({ childId, category } = {}) {
  const cid = childId || state.childId;
  const catLabel = { school_work:'School Work', home_work:'Homework', self_learning:'Self Learning' }[category] || 'Activity';

  setScreen(`
    <div class="detail-screen" style="background:#F3F6FD">
      <div class="app-bar" style="background:#F3F6FD">
        <button class="icon-btn" id="act-back">${IC_BACK}</button>
        <h2>${catLabel}</h2>
      </div>
      <div id="act-content" class="detail-content">
        <div class="loading-placeholder">Loading…</div>
      </div>
    </div>
  `);
  $('#act-back').addEventListener('click', () => history.back());
  if (!cid) return;

  const db = getDb();
  const unsub = onSnapshot(
    query(collection(db,'uploads'), where('childId','==',cid), orderBy('timestamp','desc'), limit(30)),
    snap => {
      const content = $('#act-content');
      if (!content) return;
      const uploads = snap.docs.map(d => d.data());

      const filtered = uploads.filter(u => {
        const kind = u.kind || u.uploadKind || '';
        if (category === 'home_work') return kind === 'HOMEWORK' || kind === 'TUITION_HW' || u.category === 'home_work';
        if (category === 'school_work') return kind === 'SCHOOL_WORK' || kind === 'TUITION_WORK' || u.category === 'school_work';
        return true;
      });

      if (!filtered.length) {
        content.innerHTML = `<div class="empty-state"><p class="text-secondary">No ${catLabel.toLowerCase()} recorded yet.</p></div>`;
        return;
      }

      content.innerHTML = filtered.map(u => `
        <div class="card mb-12">
          <div class="d-flex align-center gap-12">
            <div class="subj-icon-sm">${subjectIcon(u.subject||'')}</div>
            <div class="flex-1">
              <div class="fw-semibold">${u.topic || u.unitName || u.conceptName || 'Upload'}</div>
              <div class="text-secondary" style="font-size:12px">${u.subject || ''} · ${formatDate(u.timestamp)}</div>
            </div>
            ${u.photoUrls?.[0] ? `<img src="${u.photoUrls[0]}" class="upload-thumb" />` : ''}
          </div>
        </div>
      `).join('');
    }
  );
  addUnsubscriber(unsub);
}

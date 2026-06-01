import { getDb, getStorageInstance } from './firebase-init.js';
import {
  collection, doc, getDoc, onSnapshot, query, where, orderBy, updateDoc
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import {
  state, setState, navigate, addUnsubscriber, setScreen,
  showLoading, hideLoading, showError, showSuccess, showToast,
  $, $$, formatDate, subjectIcon, subjectColor, genderEmoji,
  streakEmoji, calcStreakFromTimestamps, TASK_STATUS, taskStatusLabel, taskStatusColor,
  IC_BACK
} from './utils.js';

// ── SVG icons ─────────────────────────────────────────────────────────────────
const IC_HOME   = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`;
const IC_BOOK   = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/></svg>`;
const IC_PERSON = `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`;

// ── Child Home (3 tabs) ───────────────────────────────────────────────────────
export function renderChildHome({ childId } = {}) {
  const cid = childId || state.childId;
  const name = state.childName || 'there';
  const gender = state.childGender || 'other';

  setScreen(`
    <div class="child-shell">
      <div id="child-tab-content" class="child-tab-content"></div>
      <nav class="child-bottom-nav">
        <button class="nav-tab active" data-tab="0">
          <span class="nav-icon">${IC_HOME}</span>
          <span class="nav-label">Home</span>
        </button>
        <button class="nav-tab" data-tab="1">
          <span class="nav-icon">${IC_BOOK}</span>
          <span class="nav-label">My Learnings</span>
        </button>
        <button class="nav-tab" data-tab="2">
          <span class="nav-icon">${IC_PERSON}</span>
          <span class="nav-label">Profile</span>
        </button>
      </nav>
    </div>
  `);

  let currentTab = 0;
  const tabContent = $('#child-tab-content');

  function switchTab(idx) {
    currentTab = idx;
    $$('.nav-tab').forEach(t => t.classList.toggle('active', +t.dataset.tab === idx));
    if (idx === 0) renderHomeTab(tabContent, cid, name);
    else if (idx === 1) renderLearningsTab(tabContent, cid);
    else renderProfileTab(tabContent, cid);
  }

  $$('.nav-tab').forEach(btn => {
    btn.addEventListener('click', () => switchTab(+btn.dataset.tab));
  });

  switchTab(0);
}

// ── Tab 0: Home ───────────────────────────────────────────────────────────────
function renderHomeTab(container, childId, name) {
  container.innerHTML = `
    <div class="child-home-scroll" id="child-home-scroll">
      <div class="child-greeting">
        <h2>Hi, ${name}! 👋</h2>
        <p class="text-secondary">Keep up the great learning!</p>
      </div>
      <div id="assigned-task-bar"></div>
      <div id="streak-banner-area"></div>
      <div id="revision-modes-area"></div>
      <div class="mt-20">
        <button id="start-learning-btn" class="btn btn-primary btn-full btn-lg">
          ▶ Start Learning
        </button>
      </div>
      <div class="section-header mt-28">
        <span class="section-title">Tasks from Parents</span>
        <span class="link" id="see-all-tasks-link">See all</span>
      </div>
      <div id="parent-tasks-area"></div>
    </div>
  `;

  $('#start-learning-btn').addEventListener('click', () => navigate('concept-list'));
  $('#see-all-tasks-link').addEventListener('click', () => navigate('child-tasks', { childId }));

  renderRevisionModes(childId);
  subscribeChildHomeData(childId);
}

function renderRevisionModes(childId) {
  const area = $('#revision-modes-area');
  if (!area) return;

  const modes = [
    { emoji: '🏫', title: 'School\nWork', sub: 'Revise from class', color: '#DBEAFE', accent: '#2563EB',
      options: [
        { emoji: '📖', title: 'Practise a Lesson', sub: 'Go through a school lesson step by step', route: 'concept-list' },
        { emoji: '📝', title: 'Log Homework', sub: 'Mark today\'s homework as completed', route: 'homework-log' },
        { emoji: '📋', title: 'Class Test Prep', sub: 'Review and get ready for an upcoming test', route: 'concept-list' },
      ]
    },
    { emoji: '📐', title: 'Tuition\nWork', sub: 'Revise from tutor', color: '#EDE9FE', accent: '#7C3AED',
      options: [
        { emoji: '📚', title: 'Upload Tuition Work', sub: 'Show what you did in today\'s class', route: 'upload', params: { kind: 'TUITION_WORK' } },
        { emoji: '📝', title: 'Upload Tuition Homework', sub: 'Share homework given by your tutor', route: 'upload', params: { kind: 'TUITION_HW' } },
        { emoji: '🤔', title: 'Tuition Problems', sub: 'Share your problems with your parents', route: 'upload', params: { kind: 'TUITION_PROBLEM' } },
      ]
    },
    { emoji: '✨', title: 'Self\nRevision', sub: 'Study on your own', color: '#F0FDF4', accent: '#16A34A',
      options: [
        { emoji: '🔁', title: 'Revisit Past Topics', sub: 'Go back to topics you\'ve completed', route: 'revisit-topics' },
        { emoji: '⚠️', title: 'Need Revision', sub: 'Topics you might be forgetting', route: 'concept-list' },
        { emoji: '🧠', title: 'Test Yourself', sub: 'Practice with quizzes from your textbook', route: 'concept-list' },
      ]
    }
  ];

  area.innerHTML = `
    <p class="section-title mb-10">What would you like to revise?</p>
    <div class="revision-mode-row">
      ${modes.map((m, i) => `
        <button class="revision-card" data-mode="${i}" style="background:${m.color}">
          <div class="revision-icon">${m.emoji}</div>
          <div class="revision-title" style="color:${m.accent}">${m.title.replace('\n','<br>')}</div>
          <div class="revision-sub">${m.sub}</div>
        </button>
      `).join('')}
    </div>
    <div id="revision-dialog-overlay" hidden>
      <div class="revision-dialog-backdrop"></div>
      <div class="revision-dialog" id="revision-dialog"></div>
    </div>
  `;

  $$('.revision-card').forEach(btn => {
    btn.addEventListener('click', () => {
      const idx = +btn.dataset.mode;
      const m = modes[idx];
      const overlay = $('#revision-dialog-overlay');
      const dialog = $('#revision-dialog');
      dialog.innerHTML = `
        <div class="rd-header">
          <div class="rd-icon" style="background:${m.color}">${m.emoji}</div>
          <div>
            <div class="rd-title">${m.title.replace('\n',' ')}</div>
            <div class="text-secondary" style="font-size:13px">What would you like to do?</div>
          </div>
        </div>
        <div class="rd-options">
          ${m.options.map((o, oi) => `
            <button class="rd-option" data-opt="${oi}" data-route="${o.route}" data-params='${JSON.stringify(o.params||{})}'>
              <div class="rd-opt-icon" style="background:${m.color}">${o.emoji}</div>
              <div class="rd-opt-text">
                <div class="rd-opt-title">${o.title}</div>
                <div class="rd-opt-sub">${o.sub}</div>
              </div>
              <span style="color:${m.accent}">›</span>
            </button>
          `).join('')}
        </div>
        <button class="rd-cancel">Cancel</button>
      `;
      overlay.hidden = false;
      dialog.querySelectorAll('.rd-option').forEach(ob => {
        ob.addEventListener('click', () => {
          overlay.hidden = true;
          const params = JSON.parse(ob.dataset.params || '{}');
          navigate(ob.dataset.route, params);
        });
      });
      dialog.querySelector('.rd-cancel').addEventListener('click', () => { overlay.hidden = true; });
      $('#revision-dialog-backdrop')?.addEventListener('click', () => { overlay.hidden = true; });
    });
  });
}

function subscribeChildHomeData(childId) {
  const db = getDb();

  // Streak: from uploads + quiz_attempts
  let allTimestamps = { uploads: [], units: [], concepts: [] };
  function recomputeStreak() {
    const ts = [...allTimestamps.uploads, ...allTimestamps.units, ...allTimestamps.concepts];
    const streak = calcStreakFromTimestamps(ts);
    renderStreakBanner(streak);
  }

  const unsub1 = onSnapshot(
    query(collection(db, 'uploads'), where('childId', '==', childId)),
    snap => {
      allTimestamps.uploads = snap.docs.map(d => d.data().timestamp || 0).filter(Boolean);
      recomputeStreak();
    }
  );

  const unsub2 = onSnapshot(
    collection(db, 'quiz_attempts', childId, 'units'),
    snap => {
      allTimestamps.units = snap.docs.map(d => d.data().lastUpdated || 0).filter(Boolean);
      recomputeStreak();
    }
  );

  const unsub3 = onSnapshot(
    collection(db, 'quiz_attempts', childId, 'concepts'),
    snap => {
      allTimestamps.concepts = snap.docs.map(d => d.data().lastUpdated || 0).filter(Boolean);
      recomputeStreak();
    }
  );

  // Parent tasks
  const unsub4 = onSnapshot(
    query(collection(db, 'parentTaskRequests'), where('childId', '==', childId)),
    snap => {
      const tasks = snap.docs.map(d => ({ id: d.id, ...d.data() }));
      renderParentTasksSection(tasks);
      renderAssignedTaskBar(tasks);
    }
  );

  addUnsubscriber(unsub1);
  addUnsubscriber(unsub2);
  addUnsubscriber(unsub3);
  addUnsubscriber(unsub4);
}

function renderStreakBanner(streakDays) {
  const area = $('#streak-banner-area');
  if (!area) return;

  const labels = ['M','T','W','T','F','S','S'];
  const todayIdx = (new Date().getDay() + 6) % 7; // 0=Mon
  const dots = labels.map((ch, idx) => {
    const active = idx <= todayIdx && (todayIdx - idx) < Math.min(streakDays, 7);
    return `<div class="streak-day-col">
      <div class="streak-dot ${active ? 'active' : ''}">🔥</div>
      <div class="streak-day-label ${active ? 'active' : ''}">${ch}</div>
    </div>`;
  }).join('');

  area.innerHTML = `
    <div class="streak-banner">
      <div class="streak-flame-bg">🔥</div>
      <div class="streak-top">
        <div class="streak-icon-circle">🔥</div>
        <div class="streak-text">
          <div class="streak-title">${streakDays > 0 ? `${streakDays}-Day Streak` : 'Start Your Streak!'}</div>
          <div class="streak-sub">${streakDays > 0 ? "You're on fire! Keep it going. 🔥" : 'Upload today\'s chapter to light up day 1 🔥'}</div>
        </div>
      </div>
      <div class="streak-days-row">${dots}</div>
      <div class="streak-actions">
        <button class="streak-action-btn" id="streak-invite-btn">👥 Invite Friend</button>
        <button class="streak-action-btn" id="streak-dashboard-btn">📈 Dashboard</button>
      </div>
    </div>
  `;

  $('#streak-invite-btn')?.addEventListener('click', () => navigate('invite-friend'));
  $('#streak-dashboard-btn')?.addEventListener('click', () => navigate('streak-dashboard'));
}

function renderAssignedTaskBar(tasks) {
  const bar = $('#assigned-task-bar');
  if (!bar) return;
  const assigned = tasks.filter(t => t.status === 'ASSIGNED');
  if (!assigned.length) { bar.innerHTML = ''; return; }
  const t = assigned[0];
  const more = assigned.length - 1;
  bar.innerHTML = `
    <div class="assigned-task-bar card mb-16">
      <div class="section-header">
        <span class="fw-bold">Tasks needing your attention</span>
        ${more > 0 ? `<span class="link" id="atb-see-all">View all</span>` : ''}
      </div>
      <div class="task-row clickable mt-12" id="atb-task-row">
        <div class="task-icon-circle" style="background:#FFF4E0">${t.subjectIcon || '📚'}</div>
        <div class="flex-1">
          <div class="fw-semibold" style="font-size:14px">${taskTitle(t)}</div>
          <div class="text-secondary" style="font-size:12px">${(t.isUnitScope ? 'Lesson' : 'Concept') + (t.subject ? ' · ' + t.subject : '')}</div>
        </div>
        <span class="badge badge-assigned">New task</span>
      </div>
    </div>
  `;
  bar.querySelector('#atb-task-row')?.addEventListener('click', () => navigate('child-task-detail', { taskId: t.id }));
  bar.querySelector('#atb-see-all')?.addEventListener('click', () => navigate('child-tasks'));
}

function renderParentTasksSection(tasks) {
  const area = $('#parent-tasks-area');
  if (!area) return;

  const pending = tasks.filter(t => ['ASSIGNED','PENDING_REVIEW','REJECTED'].includes(t.status));
  const requested = tasks.filter(t => t.status === 'REQUESTED');

  if (pending.length) {
    renderTasksSummaryBanner(area, pending);
  } else if (requested.length) {
    renderTasksSummaryBanner(area, requested);
  } else {
    area.innerHTML = `
      <div class="earn-reward-card" id="earn-reward-card">
        <div class="er-sparkles">✨ ⭐</div>
        <div class="er-gift">🎁</div>
        <div class="er-heading">Confident with your studies?</div>
        <div class="er-sub">Ask your parents to give you a task and win an assured reward upon submission.</div>
        <div class="er-pill">🏆 Earn your first reward</div>
      </div>
    `;
    $('#earn-reward-card')?.addEventListener('click', () => navigate('earn-reward'));
  }
}

function renderTasksSummaryBanner(container, tasks) {
  const t = tasks[0];
  const more = tasks.length - 1;
  const actionNeeded = tasks.filter(t => ['ASSIGNED','REJECTED'].includes(t.status)).length;

  container.innerHTML = `
    <div class="card task-summary-card clickable" id="ts-card">
      <div class="d-flex align-center gap-12">
        <div class="task-icon-circle" style="background:#FFF1E0">${t.subjectIcon || '🎯'}</div>
        <div class="flex-1">
          <div class="fw-semibold">${taskTitle(t)}</div>
          <div class="text-secondary" style="font-size:12px">${t.subject || 'Task'}</div>
        </div>
        <div>
          <span class="badge" style="background:${taskStatusColor(t.status)}22;color:${taskStatusColor(t.status)}">${taskStatusLabel(t.status)}</span>
          ${more > 0 ? `<div class="text-primary fw-semibold mt-4" style="font-size:10px">+${more} more</div>` : ''}
        </div>
      </div>
      ${tasks.length > 1 ? `
        <div class="ts-footer mt-12 clickable" id="ts-footer" style="background:${actionNeeded > 0 ? '#FFF7E6':'#F8FAFF'}">
          <span style="color:${actionNeeded > 0 ? '#B45309':'#15803D'};font-weight:600;font-size:12px">
            ${actionNeeded > 0 ? `⚡ ${actionNeeded} task${actionNeeded>1?'s':''} need${actionNeeded===1?'s':''} your attention` : `✓ All ${tasks.length} tasks completed`}
          </span>
          <span style="color:${actionNeeded > 0 ? '#B45309':'#15803D'}">›</span>
        </div>
      ` : ''}
    </div>
  `;
  container.querySelector('#ts-card')?.addEventListener('click', () => navigate('child-task-detail', { taskId: t.id }));
  container.querySelector('#ts-footer')?.addEventListener('click', (e) => { e.stopPropagation(); navigate('child-tasks'); });
}

// ── Tab 1: My Learnings ───────────────────────────────────────────────────────
function renderLearningsTab(container, childId) {
  container.innerHTML = `
    <div class="child-home-scroll">
      <div class="page-header">
        <h2>My Learnings</h2>
      </div>
      <div id="learnings-content"></div>
    </div>
  `;

  const db = getDb();
  const lc = $('#learnings-content');
  if (!lc) return;
  lc.innerHTML = '<div class="loading-placeholder">Loading your learnings…</div>';

  Promise.all([
    // quiz attempts
    new Promise(res => {
      const unsub = onSnapshot(collection(db, 'quiz_attempts', childId, 'units'), snap => {
        res(snap.docs.map(d => ({ id: d.id, scope: 'unit', ...d.data() })));
        unsub();
      });
    }),
    new Promise(res => {
      const unsub = onSnapshot(collection(db, 'quiz_attempts', childId, 'concepts'), snap => {
        res(snap.docs.map(d => ({ id: d.id, scope: 'concept', ...d.data() })));
        unsub();
      });
    }),
    new Promise(res => {
      const unsub = onSnapshot(query(collection(db, 'uploads'), where('childId', '==', childId)), snap => {
        res(snap.docs.map(d => d.data()));
        unsub();
      });
    })
  ]).then(([units, concepts, uploads]) => {
    const allAttempts = [...units, ...concepts].filter(a => a.lastTotal > 0);
    allAttempts.sort((a,b) => (b.lastUpdated||0) - (a.lastUpdated||0));

    if (!allAttempts.length && !uploads.length) {
      lc.innerHTML = `
        <div class="empty-state">
          <div style="font-size:48px">📚</div>
          <h3>Nothing yet</h3>
          <p class="text-secondary">Complete a quiz or upload work to see your learnings here.</p>
          <button class="btn btn-primary mt-16" id="start-now-btn">Start Learning →</button>
        </div>
      `;
      lc.querySelector('#start-now-btn')?.addEventListener('click', () => navigate('concept-list'));
      return;
    }

    lc.innerHTML = `
      ${allAttempts.length ? `
        <div class="section-header"><span class="section-title">Recent Quizzes</span></div>
        <div class="learnings-list">
          ${allAttempts.slice(0,10).map(a => `
            <div class="card learning-item clickable" data-scope="${a.scope}" data-id="${a.id}">
              <div class="d-flex align-center gap-12">
                <div class="subj-icon-sm">${subjectIcon(a.subject||'')}</div>
                <div class="flex-1">
                  <div class="fw-semibold">${a.displayName || a.id}</div>
                  <div class="text-secondary" style="font-size:12px">${a.subject || ''}</div>
                </div>
                <div class="text-right">
                  <div class="fw-bold" style="color:${a.lastCorrect/a.lastTotal >= 0.7 ? '#16A34A':'#EF4444'}">
                    ${Math.round((a.lastCorrect/a.lastTotal)*100)}%
                  </div>
                  <div class="text-secondary" style="font-size:11px">${a.lastCorrect}/${a.lastTotal}</div>
                </div>
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}
      ${uploads.length ? `
        <div class="section-header mt-20"><span class="section-title">Uploaded Work</span></div>
        <div class="learnings-list">
          ${uploads.slice(0,5).map(u => `
            <div class="card learning-item">
              <div class="d-flex align-center gap-12">
                <div class="subj-icon-sm">${subjectIcon(u.subject||'')}</div>
                <div class="flex-1">
                  <div class="fw-semibold">${u.topic || u.unitName || u.conceptName || 'Uploaded work'}</div>
                  <div class="text-secondary" style="font-size:12px">${formatDate(u.timestamp)}</div>
                </div>
                ${u.photoUrls?.[0] ? `<img src="${u.photoUrls[0]}" class="upload-thumb" />` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}
    `;

    lc.querySelectorAll('.learning-item[data-scope]').forEach(el => {
      el.addEventListener('click', () => {
        const { scope, id } = el.dataset;
        navigate('quiz', { [scope === 'unit' ? 'unitId' : 'conceptId']: id });
      });
    });
  });
}

// ── Tab 2: Profile ────────────────────────────────────────────────────────────
function renderProfileTab(container, childId) {
  container.innerHTML = `
    <div class="child-home-scroll">
      <div class="page-header">
        <h2>My Profile</h2>
      </div>
      <div class="profile-card card">
        <div class="profile-avatar">${genderEmoji(state.childGender || 'other')}</div>
        <div class="profile-name">${state.childName || 'Child'}</div>
        <div class="profile-grade text-secondary">Class ${state.childGrade || '—'}</div>
      </div>
      <div class="settings-list mt-20">
        <button class="settings-row" id="invite-friend-btn">
          <span>👥 Invite a Friend</span><span>›</span>
        </button>
        <button class="settings-row" id="switch-profile-btn">
          <span>🔄 Switch to Parent View</span><span>›</span>
        </button>
      </div>
    </div>
  `;
  $('#invite-friend-btn')?.addEventListener('click', () => navigate('invite-friend'));
  $('#switch-profile-btn')?.addEventListener('click', () => navigate('login'));
}

// ── Child Task Detail ─────────────────────────────────────────────────────────
export function renderChildTaskDetail({ taskId }) {
  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="ctd-back">${IC_BACK}</button>
        <h2>Task Detail</h2>
      </div>
      <div id="ctd-content" class="detail-content">
        <div class="loading-placeholder">Loading task…</div>
      </div>
    </div>
  `);

  $('#ctd-back').addEventListener('click', () => history.back());

  const db = getDb();
  const unsub = onSnapshot(doc(db, 'parentTaskRequests', taskId), snap => {
    const content = $('#ctd-content');
    if (!content) return;
    if (!snap.exists()) { content.innerHTML = '<p class="text-secondary p-20">Task not found.</p>'; return; }
    const t = { id: snap.id, ...snap.data() };

    content.innerHTML = `
      <div class="card mb-16">
        <div class="d-flex align-center gap-12">
          <div class="task-icon-circle" style="background:#FFF1E0;width:52px;height:52px;font-size:26px">${t.subjectIcon || '🎯'}</div>
          <div>
            <div class="fw-bold" style="font-size:16px">${taskTitle(t)}</div>
            <div class="text-secondary">${t.subject || ''}</div>
          </div>
        </div>
        <div class="mt-12">
          <span class="badge" style="background:${taskStatusColor(t.status)}22;color:${taskStatusColor(t.status)}">${taskStatusLabel(t.status)}</span>
        </div>
        ${t.customNote ? `<div class="mt-12 p-12 rounded" style="background:#F8FAFF;font-size:13px">${t.customNote}</div>` : ''}
        ${t.parentReviewNote ? `
          <div class="mt-12 p-12 rounded" style="background:#FFF4E0;font-size:13px">
            <strong>Parent note:</strong> ${t.parentReviewNote}
          </div>
        ` : ''}
      </div>

      ${t.status === 'ASSIGNED' ? renderTaskActions(t) : ''}
      ${t.status === 'PENDING_REVIEW' ? `
        <div class="card" style="background:#FEF3C7;border-color:#FDE68A">
          <div class="fw-semibold">⏳ Submitted for review</div>
          <div class="text-secondary mt-4">Your parent is reviewing your submission.</div>
        </div>
      ` : ''}
      ${t.status === 'ACCEPTED' ? `
        <div class="card" style="background:#DCFCE7;border-color:#BBF7D0">
          <div class="fw-semibold" style="color:#15803D">✓ Accepted! Great job!</div>
          ${t.parentReviewNote ? `<div class="text-secondary mt-4">${t.parentReviewNote}</div>` : ''}
        </div>
      ` : ''}
      ${t.status === 'REJECTED' ? `
        <div class="card" style="background:#FEE2E2;border-color:#FECACA">
          <div class="fw-semibold" style="color:#B91C1C">Please redo this task</div>
          ${t.parentReviewNote ? `<div class="text-secondary mt-4">${t.parentReviewNote}</div>` : ''}
        </div>
      ` : ''}
    `;

    // Wire up ASSIGNED task action buttons
    if (t.status === 'ASSIGNED') {
      if (t.type === 'OUR_QUESTIONS') {
        content.querySelector('#task-quiz-btn')?.addEventListener('click', () => {
          const p = t.isUnitScope ? { unitId: t.unitId, taskId } : { conceptId: t.conceptId, taskId };
          navigate('quiz', p);
        });
      } else {
        content.querySelector('#task-upload-btn')?.addEventListener('click', () => {
          navigate('upload', { taskId, kind: t.type, unitId: t.unitId || '', conceptId: t.conceptId || '', subject: t.subject || '' });
        });
      }
    }
  });
  addUnsubscriber(unsub);
}

function renderTaskActions(t) {
  if (t.type === 'OUR_QUESTIONS') {
    return `
      <button class="btn btn-primary btn-full" id="task-quiz-btn">
        🧠 Answer ${t.mcqCount || 10} Questions
      </button>
    `;
  }
  return `
    <button class="btn btn-primary btn-full" id="task-upload-btn">
      📷 Upload Work Photos
    </button>
  `;
}

// ── Child Tasks List ──────────────────────────────────────────────────────────
export function renderChildTasks({ childId } = {}) {
  const cid = childId || state.childId;
  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="ct-back">${IC_BACK}</button>
        <h2>My Tasks</h2>
      </div>
      <div class="tab-bar" id="ct-tabs">
        <button class="tab-btn active" data-status="">All</button>
        <button class="tab-btn" data-status="ASSIGNED">To do</button>
        <button class="tab-btn" data-status="PENDING_REVIEW">Submitted</button>
        <button class="tab-btn" data-status="ACCEPTED">Done</button>
      </div>
      <div id="ct-list" class="detail-content"></div>
    </div>
  `);

  $('#ct-back').addEventListener('click', () => history.back());

  let allTasks = [];
  let activeFilter = '';

  function renderList() {
    const list = $('#ct-list');
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
            <div class="fw-semibold">${taskTitle(t)}</div>
            <div class="text-secondary" style="font-size:12px">${t.subject || 'Task'}</div>
          </div>
          <span class="badge" style="background:${taskStatusColor(t.status)}22;color:${taskStatusColor(t.status)}">${taskStatusLabel(t.status)}</span>
        </div>
      </div>
    `).join('');
    list.querySelectorAll('[data-id]').forEach(el => {
      el.addEventListener('click', () => navigate('child-task-detail', { taskId: el.dataset.id }));
    });
  }

  $$('#ct-tabs .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      $$('#ct-tabs .tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeFilter = btn.dataset.status;
      renderList();
    });
  });

  const db = getDb();
  const unsub = onSnapshot(
    query(collection(db, 'parentTaskRequests'), where('childId', '==', cid)),
    snap => {
      allTasks = snap.docs.map(d => ({ id: d.id, ...d.data() }));
      allTasks.sort((a,b) => (b.requestedAtMillis||0) - (a.requestedAtMillis||0));
      renderList();
    }
  );
  addUnsubscriber(unsub);
}

// ── Revisit Topics ────────────────────────────────────────────────────────────
export function renderRevisitTopics() {
  const childId = state.childId;
  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="rt-back">${IC_BACK}</button>
        <h2>Revisit Past Topics</h2>
      </div>
      <div id="rt-content" class="detail-content">
        <div class="loading-placeholder">Loading…</div>
      </div>
    </div>
  `);
  $('#rt-back').addEventListener('click', () => history.back());

  const db = getDb();
  const content = $('#rt-content');

  Promise.all([
    new Promise(res => {
      const u = onSnapshot(query(collection(db,'uploads'), where('childId','==',childId)), snap => {
        res(snap.docs.map(d => d.data())); u();
      });
    })
  ]).then(([uploads]) => {
    if (!content) return;
    const byDate = {};
    uploads.forEach(u => {
      const dateKey = new Date(u.timestamp).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'2-digit' });
      if (!byDate[dateKey]) byDate[dateKey] = [];
      byDate[dateKey].push(u);
    });

    if (!uploads.length) {
      content.innerHTML = `<div class="empty-state"><p class="text-secondary">No uploads yet. Upload your work to see it here.</p><button class="btn btn-primary mt-16" id="rt-upload">Upload Now</button></div>`;
      content.querySelector('#rt-upload')?.addEventListener('click', () => navigate('upload'));
      return;
    }

    content.innerHTML = Object.entries(byDate).map(([date, items]) => `
      <div class="section-header mt-20"><span class="section-title">${date}</span></div>
      ${items.map(u => `
        <div class="card mb-8">
          <div class="d-flex align-center gap-12">
            <div class="subj-icon-sm">${subjectIcon(u.subject||'')}</div>
            <div class="flex-1">
              <div class="fw-semibold">${u.topic || u.unitName || u.conceptName || 'Upload'}</div>
              <div class="text-secondary" style="font-size:12px">${u.subject || ''} · ${u.kind || ''}</div>
            </div>
            ${u.photoUrls?.[0] ? `<img src="${u.photoUrls[0]}" class="upload-thumb" />` : ''}
          </div>
          ${u.photoUrls?.length > 1 ? `<div class="photo-row mt-8">${u.photoUrls.slice(1).map(url=>`<img src="${url}" class="upload-thumb" />`).join('')}</div>` : ''}
        </div>
      `).join('')}
    `).join('');
  });
}

// ── Homework Log ──────────────────────────────────────────────────────────────
export function renderHomeworkLog() {
  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="hl-back">${IC_BACK}</button>
        <h2>Log Homework</h2>
      </div>
      <div class="detail-content">
        <div class="card">
          <p class="text-secondary">Mark today's homework as completed. Upload a photo as proof.</p>
        </div>
        <button class="btn btn-primary btn-full mt-16" id="hl-upload">📷 Upload Homework Photo</button>
      </div>
    </div>
  `);
  $('#hl-back').addEventListener('click', () => history.back());
  $('#hl-upload').addEventListener('click', () => navigate('upload', { kind: 'HOMEWORK' }));
}

// ── Earn Reward ───────────────────────────────────────────────────────────────
export function renderEarnReward() {
  const childId = state.childId;
  const parentId = state.parentId;
  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="er-back">${IC_BACK}</button>
        <h2>Earn a Reward</h2>
      </div>
      <div class="detail-content">
        <div class="earn-reward-card">
          <div class="er-gift" style="font-size:64px">🎁</div>
          <div class="er-heading">Choose a topic to get a task</div>
          <div class="er-sub">Your parent will assign you a task on this topic. Complete it to earn your reward!</div>
        </div>
        <button class="btn btn-primary btn-full mt-16" id="er-pick">📚 Pick a Topic →</button>
      </div>
    </div>
  `);
  $('#er-back').addEventListener('click', () => history.back());
  $('#er-pick').addEventListener('click', () => navigate('concept-list', { mode: 'request-task' }));
}

// ── Helper ────────────────────────────────────────────────────────────────────
function taskTitle(t) {
  const scope = t.displayName || (t.isUnitScope ? t.unitName : t.conceptName) || 'your topic';
  if (t.status === 'REQUESTED') return `Waiting for parent: ${scope}`;
  switch (t.type) {
    case 'REVISE_CHAPTER': return `Revise: ${scope}`;
    case 'WRITE_QA': return `Write Q&A: ${scope}`;
    case 'OUR_QUESTIONS': return `${t.mcqCount || 10} questions: ${scope}`;
    case 'CUSTOM': return `Task on: ${scope}`;
    default: return scope;
  }
}

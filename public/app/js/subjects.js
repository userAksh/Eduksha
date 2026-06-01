import { getDb } from './firebase-init.js';
import {
  collection, query, where, orderBy, getDocs, onSnapshot
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import {
  state, navigate, addUnsubscriber, setScreen, showLoading, hideLoading,
  $, $$, subjectColor, subjectIcon, IC_BACK
} from './utils.js';

// ── Concept List (subject / unit / concept browser) ───────────────────────────
export async function renderConceptList({ mode } = {}) {
  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="cl-back">${IC_BACK}</button>
        <h2>${mode === 'request-task' ? 'Pick a Topic' : 'Start Learning'}</h2>
      </div>
      <div id="cl-content" class="detail-content">
        <div class="loading-placeholder">Loading subjects…</div>
      </div>
    </div>
  `);

  $('#cl-back').addEventListener('click', () => history.back());

  const db = getDb();
  const grade = state.childGrade || 6;

  try {
    const snap = await getDocs(
      query(collection(db, 'concepts'), where('grade', '==', grade))
    );
    const concepts = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderSubjectAccordion($('#cl-content'), concepts, mode);
  } catch(e) {
    const content = $('#cl-content');
    if (content) content.innerHTML = `<div class="empty-state"><p class="text-secondary">Could not load subjects. Check your internet.</p></div>`;
  }
}

function renderSubjectAccordion(container, concepts, mode) {
  if (!container) return;

  // Group by subject → unit → concepts
  const subjects = {};
  concepts.forEach(c => {
    const subj = c.subject || 'Other';
    if (!subjects[subj]) subjects[subj] = {};
    const unit = c.unit || 'General';
    if (!subjects[subj][unit]) subjects[subj][unit] = { unitId: c.unitId || '', concepts: [] };
    subjects[subj][unit].concepts.push(c);
  });

  if (!Object.keys(subjects).length) {
    container.innerHTML = `
      <div class="empty-state">
        <div style="font-size:48px">📚</div>
        <p class="text-secondary">No subjects found for Class ${state.childGrade || 6}.<br>Content is being added soon!</p>
      </div>
    `;
    return;
  }

  container.innerHTML = Object.entries(subjects).map(([subj, units]) => {
    const color = subjectColor(subj);
    const icon = subjectIcon(subj);
    return `
      <div class="subject-section">
        <button class="subject-header accordion-toggle" data-subject="${subj}">
          <div class="d-flex align-center gap-10">
            <span class="subj-icon-md" style="background:${color}22">${icon}</span>
            <span class="fw-bold" style="color:${color}">${subj}</span>
          </div>
          <span class="accordion-arrow">▼</span>
        </button>
        <div class="subject-body" id="subj-${subj.replace(/\s/g,'-')}">
          ${Object.entries(units).map(([unitName, unitData]) => `
            <div class="unit-section">
              <button class="unit-header accordion-toggle" data-unit="${unitData.unitId}">
                <span class="fw-semibold text-primary">${unitName}</span>
                <div class="d-flex align-center gap-8">
                  ${mode !== 'request-task' ? `<button class="btn btn-sm btn-outline" data-unit-quiz="${unitData.unitId}" data-unit-name="${unitName}" data-subj="${subj}">Quiz unit</button>` : ''}
                  <span class="accordion-arrow" style="font-size:12px">▼</span>
                </div>
              </button>
              <div class="unit-body" id="unit-${unitData.unitId}">
                ${unitData.concepts.map(c => `
                  <div class="concept-row clickable" data-concept-id="${c.id}" data-concept-name="${c.name}" data-unit-id="${unitData.unitId}" data-subj="${subj}">
                    <div>
                      <div class="fw-semibold" style="font-size:14px">${c.name}</div>
                      ${c.description ? `<div class="text-secondary" style="font-size:12px">${c.description}</div>` : ''}
                    </div>
                    <span class="text-primary" style="font-size:18px">›</span>
                  </div>
                `).join('')}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }).join('');

  // Accordion toggle for subjects
  container.querySelectorAll('.subject-header').forEach(btn => {
    const body = document.getElementById(`subj-${btn.dataset.subject.replace(/\s/g,'-')}`);
    if (body) body.style.display = 'none'; // collapsed by default
    btn.addEventListener('click', () => {
      const open = body && body.style.display !== 'none';
      body.style.display = open ? 'none' : 'block';
      btn.querySelector('.accordion-arrow').textContent = open ? '▼' : '▲';
    });
  });

  // Accordion toggle for units
  container.querySelectorAll('.unit-header').forEach(btn => {
    const unitId = btn.dataset.unit;
    const body = document.getElementById(`unit-${unitId}`);
    if (body) body.style.display = 'none';
    btn.addEventListener('click', (e) => {
      if (e.target.closest('[data-unit-quiz]')) return; // handled separately
      const open = body && body.style.display !== 'none';
      body.style.display = open ? 'none' : 'block';
      btn.querySelector('.accordion-arrow').textContent = open ? '▼' : '▲';
    });
  });

  // Unit quiz buttons
  container.querySelectorAll('[data-unit-quiz]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (mode === 'request-task') {
        requestTask({ unitId: btn.dataset.unitQuiz, unitName: btn.dataset.unitName, isUnitScope: true, subject: btn.dataset.subj });
      } else {
        navigate('quiz', { unitId: btn.dataset.unitQuiz, displayName: btn.dataset.unitName, subject: btn.dataset.subj });
      }
    });
  });

  // Concept rows
  container.querySelectorAll('.concept-row[data-concept-id]').forEach(el => {
    el.addEventListener('click', () => {
      if (mode === 'request-task') {
        requestTask({ conceptId: el.dataset.conceptId, conceptName: el.dataset.conceptName, unitId: el.dataset.unitId, isUnitScope: false, subject: el.dataset.subj });
      } else {
        navigate('quiz', { conceptId: el.dataset.conceptId, displayName: el.dataset.conceptName, subject: el.dataset.subj });
      }
    });
  });
}

async function requestTask({ unitId, unitName, conceptId, conceptName, isUnitScope, subject }) {
  const db = getDb();
  const { addDoc } = await import('https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js');
  const { showError: showErr } = await import('./utils.js');

  showLoading('Requesting task…');
  try {
    const now = Date.now();
    await addDoc(collection(db, 'parentTaskRequests'), {
      parentId: state.parentId || '',
      childId: state.childId,
      unitId: unitId || '',
      unitName: unitName || '',
      conceptId: conceptId || '',
      conceptName: conceptName || '',
      isUnitScope: !!isUnitScope,
      subject: subject || '',
      subjectColor: subjectColor(subject || ''),
      subjectIcon: subjectIcon(subject || ''),
      status: 'REQUESTED',
      requestedAtMillis: now
    });
    hideLoading();
    navigate('child-home', { childId: state.childId });
  } catch(e) {
    hideLoading();
    showErr('Could not request task. Try again.');
  }
}

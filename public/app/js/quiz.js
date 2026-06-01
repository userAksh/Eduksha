import { getDb } from './firebase-init.js';
import {
  doc, getDoc, collection, runTransaction
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import {
  state, navigate, setScreen, showLoading, hideLoading, showError,
  $, $$, getQuestionsBank, getCuratedQuestions, encodeSelections, decodeSelections,
  subjectColor, subjectIcon, IC_BACK
} from './utils.js';

// ── Quiz Flow: intro → questions → results ────────────────────────────────────
export async function renderQuiz({ unitId, conceptId, displayName, subject, taskId } = {}) {
  showLoading('Loading quiz…');

  const scope = conceptId ? 'concepts' : 'units';
  const targetId = conceptId || unitId;
  const childId = state.childId;

  // Load questions from curated bank
  await getQuestionsBank();
  let questions = getCuratedQuestions(unitId, conceptId);

  // If no curated questions, fall back to Firestore unit/concept questions array
  if (!questions.length && targetId) {
    try {
      const db = getDb();
      const refPath = conceptId ? `concepts/${conceptId}` : `units/${unitId}`;
      const snap = await getDoc(doc(db, ...refPath.split('/')));
      const data = snap.data();
      if (data?.questions?.length) questions = data.questions;
    } catch(_) {}
  }

  hideLoading();

  if (!questions.length) {
    setScreen(`
      <div class="detail-screen">
        <div class="app-bar">
          <button class="icon-btn" id="q-back">${IC_BACK}</button>
          <h2>Quiz</h2>
        </div>
        <div class="detail-content empty-state">
          <div style="font-size:48px">🧠</div>
          <h3>No questions yet</h3>
          <p class="text-secondary">Questions for this topic are being added soon.</p>
          <button class="btn btn-primary mt-16" id="q-back2">← Go Back</button>
        </div>
      </div>
    `);
    $('#q-back,#q-back2').forEach(b => b?.addEventListener('click', () => history.back()));
    return;
  }

  // Check last attempt
  let previousSelections = {};
  try {
    const db = getDb();
    if (childId && targetId) {
      const snap = await getDoc(doc(db, 'quiz_attempts', childId, scope, targetId));
      if (snap.exists()) {
        previousSelections = decodeSelections(snap.data().selections || '');
      }
    }
  } catch(_) {}

  renderQuizIntro({ questions, unitId, conceptId, targetId, scope, displayName, subject, taskId, previousSelections });
}

function renderQuizIntro({ questions, unitId, conceptId, targetId, scope, displayName, subject, taskId, previousSelections }) {
  const hasAttempt = Object.keys(previousSelections).length > 0;
  const name = displayName || (conceptId ? 'Concept Quiz' : 'Unit Quiz');
  const icon = subjectIcon(subject || '');
  const color = subjectColor(subject || '');

  setScreen(`
    <div class="detail-screen quiz-intro">
      <div class="app-bar">
        <button class="icon-btn" id="qi-back">${IC_BACK}</button>
        <h2>Quiz</h2>
      </div>
      <div class="detail-content">
        <div class="quiz-intro-card">
          <div class="quiz-intro-icon" style="background:${color}22">${icon}</div>
          <h2 class="mt-12">${name}</h2>
          ${subject ? `<div class="text-secondary">${subject}</div>` : ''}
          <div class="quiz-meta mt-16">
            <div class="quiz-meta-item"><span class="quiz-meta-val">${questions.length}</span><span>Questions</span></div>
            <div class="quiz-meta-item"><span class="quiz-meta-val">MCQ</span><span>Format</span></div>
          </div>
          ${hasAttempt ? `
            <div class="mt-12 p-12 rounded" style="background:#FFF4E0;font-size:13px">
              You've attempted this before. This will be a new attempt.
            </div>
          ` : ''}
        </div>
        <button class="btn btn-primary btn-full mt-20" id="start-quiz-btn">Start Quiz →</button>
        <button class="btn btn-ghost btn-full mt-8" id="qi-back2">← Go Back</button>
      </div>
    </div>
  `);

  $('#qi-back,#qi-back2').forEach(b => b?.addEventListener('click', () => history.back()));
  $('#start-quiz-btn').addEventListener('click', () => {
    renderQuizQuestions({ questions, unitId, conceptId, targetId, scope, displayName, subject, taskId });
  });
}

function renderQuizQuestions({ questions, unitId, conceptId, targetId, scope, displayName, subject, taskId }) {
  let currentIdx = 0;
  const selections = {};

  function renderQ(idx) {
    const q = questions[idx];
    const selected = selections[idx];
    const progress = Math.round(((idx + 1) / questions.length) * 100);

    setScreen(`
      <div class="quiz-screen">
        <div class="quiz-header">
          <button class="icon-btn" id="qq-back">${IC_BACK}</button>
          <div class="quiz-progress-wrap">
            <div class="quiz-progress-bar">
              <div class="quiz-progress-fill" style="width:${progress}%"></div>
            </div>
            <span class="quiz-progress-label">${idx + 1} / ${questions.length}</span>
          </div>
        </div>
        <div class="quiz-body">
          <div class="quiz-question-card">
            <div class="quiz-q-num text-secondary">Question ${idx + 1}</div>
            <div class="quiz-q-text">${q.text}</div>
          </div>
          <div class="quiz-options" id="quiz-options">
            ${q.options.map((opt, oi) => `
              <button class="quiz-option ${selected === oi ? 'selected' : ''}" data-opt="${oi}">
                <span class="quiz-opt-letter">${String.fromCharCode(65+oi)}</span>
                <span class="quiz-opt-text">${opt}</span>
              </button>
            `).join('')}
          </div>
        </div>
        <div class="quiz-footer">
          <button class="btn btn-ghost" id="qq-prev" ${idx === 0 ? 'disabled' : ''}>← Prev</button>
          <button class="btn btn-primary" id="qq-next">
            ${idx === questions.length - 1 ? 'Submit →' : 'Next →'}
          </button>
        </div>
      </div>
    `);

    // Back from quiz
    $('#qq-back').addEventListener('click', () => {
      if (idx > 0) { currentIdx--; renderQ(currentIdx); }
      else history.back();
    });

    // Prev
    $('#qq-prev')?.addEventListener('click', () => {
      if (idx > 0) { currentIdx--; renderQ(currentIdx); }
    });

    // Option selection
    $$('.quiz-option').forEach(btn => {
      btn.addEventListener('click', () => {
        const oi = +btn.dataset.opt;
        selections[idx] = oi;
        $$('.quiz-option').forEach(b => b.classList.toggle('selected', +b.dataset.opt === oi));
      });
    });

    // Next / Submit
    $('#qq-next').addEventListener('click', () => {
      if (idx === questions.length - 1) {
        submitQuiz({ questions, selections, unitId, conceptId, targetId, scope, displayName, subject, taskId });
      } else {
        currentIdx++;
        renderQ(currentIdx);
      }
    });
  }

  renderQ(0);
}

async function submitQuiz({ questions, selections, unitId, conceptId, targetId, scope, displayName, subject, taskId }) {
  showLoading('Saving results…');
  const childId = state.childId;

  let correctCount = 0;
  questions.forEach((q, i) => {
    if (selections[i] === q.correctIndex) correctCount++;
  });

  try {
    if (childId && targetId) {
      const db = getDb();
      const docRef = doc(db, 'quiz_attempts', childId, scope, targetId);
      await runTransaction(db, async tx => {
        const snap = await tx.get(docRef);
        const current = snap.exists() ? (snap.data().attempts || 0) : 0;
        const existingDisplay = snap.exists() ? (snap.data().displayName || '') : '';
        const existingSubject = snap.exists() ? (snap.data().subject || '') : '';
        tx.set(docRef, {
          attempts: current + 1,
          selections: encodeSelections(selections),
          lastCorrect: correctCount,
          lastTotal: questions.length,
          lastUpdated: Date.now(),
          displayName: displayName || existingDisplay,
          subject: subject || existingSubject,
          lastQuestions: questions.map(q => ({
            id: q.id, text: q.text, options: q.options,
            correctIndex: q.correctIndex, explanation: q.explanation || ''
          }))
        });
      });
    }
    hideLoading();
    renderQuizResults({ questions, selections, correctCount, unitId, conceptId, targetId, scope, displayName, subject, taskId });
  } catch(e) {
    hideLoading();
    // Still show results even if save fails
    renderQuizResults({ questions, selections, correctCount, unitId, conceptId, targetId, scope, displayName, subject, taskId });
    showError('Could not save results. Check your internet.');
  }
}

function renderQuizResults({ questions, selections, correctCount, unitId, conceptId, targetId, scope, displayName, subject, taskId }) {
  const total = questions.length;
  const pct = Math.round((correctCount / total) * 100);
  const isPassing = pct >= 70;
  const name = displayName || 'Quiz';

  setScreen(`
    <div class="detail-screen quiz-results">
      <div class="quiz-header">
        <h2>Quiz Results</h2>
      </div>
      <div class="detail-content">
        <div class="result-score-card ${isPassing ? 'result-pass' : 'result-fail'}">
          <div class="result-pct">${pct}%</div>
          <div class="result-label">${isPassing ? '🎉 Great job!' : '💪 Keep practising!'}</div>
          <div class="result-sub">${correctCount} out of ${total} correct</div>
        </div>

        <div class="result-answers">
          ${questions.map((q, i) => {
            const chosen = selections[i];
            const correct = q.correctIndex;
            const isCorrect = chosen === correct;
            return `
              <div class="result-q-card ${isCorrect ? 'correct' : 'wrong'}">
                <div class="result-q-top">
                  <span class="result-q-num">${i + 1}</span>
                  <span class="result-q-text">${q.text}</span>
                  <span class="result-q-icon">${isCorrect ? '✓' : '✗'}</span>
                </div>
                ${q.options.map((opt, oi) => `
                  <div class="result-opt ${oi === correct ? 'correct-opt' : ''} ${oi === chosen && !isCorrect ? 'wrong-opt' : ''}">
                    ${String.fromCharCode(65+oi)}. ${opt}
                    ${oi === correct ? ' ✓' : ''}
                    ${oi === chosen && !isCorrect ? ' ✗' : ''}
                  </div>
                `).join('')}
                ${q.explanation ? `<div class="result-explanation">💡 ${q.explanation}</div>` : ''}
              </div>
            `;
          }).join('')}
        </div>

        <div class="result-actions">
          <button class="btn btn-primary btn-full" id="redo-quiz-btn">🔄 Try Again</button>
          <button class="btn btn-ghost btn-full mt-8" id="done-quiz-btn">← Back to Learning</button>
        </div>
      </div>
    </div>
  `);

  $('#redo-quiz-btn').addEventListener('click', () => {
    renderQuiz({ unitId, conceptId, displayName, subject, taskId });
  });

  $('#done-quiz-btn').addEventListener('click', () => {
    // If this was a task, navigate back to task detail
    if (taskId) navigate('child-task-detail', { taskId });
    else navigate('child-home', { childId: state.childId });
  });
}

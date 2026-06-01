import { initFirebase, getAuthInstance } from './firebase-init.js';
import { onAuthStateChanged } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js';
import {
  state, restoreState, setState, navigate, getRoute,
  showLoading, hideLoading, $, IC_BACK
} from './utils.js';

// ── Screen imports (lazy, but pre-imported for simplicity) ────────────────────
import { renderLogin, renderOtp, renderSetupProfile, renderChildSelect, renderChildPin, doSignOut } from './auth.js';
import { renderParentHome, renderAddChild, renderTaskReview, renderParentTasks, renderCreateTask, renderInsights, renderActivity } from './parent.js';
import {
  renderChildHome, renderChildTaskDetail, renderChildTasks, renderRevisitTopics, renderHomeworkLog, renderEarnReward
} from './child.js';
import { renderConceptList } from './subjects.js';
import { renderQuiz } from './quiz.js';
import { renderUpload } from './upload.js';
import { renderDailyNews } from './news.js';

// ── Bootstrap ─────────────────────────────────────────────────────────────────
async function boot() {
  showLoading('Loading…');
  try {
    await initFirebase();
  } catch(e) {
    hideLoading();
    document.getElementById('screen').innerHTML = `
      <div style="padding:40px;text-align:center">
        <h2>Could not connect</h2>
        <p>Check your internet and reload.</p>
        <button onclick="location.reload()" style="margin-top:16px;padding:12px 24px;background:#2f7dff;color:white;border:none;border-radius:10px;cursor:pointer">Reload</button>
      </div>
    `;
    return;
  }

  restoreState();

  const auth = getAuthInstance();
  onAuthStateChanged(auth, user => {
    if (user) {
      setState({ parentId: user.uid });
    }
    hideLoading();
    routeToCurrentHash();
  });

  window.addEventListener('hashchange', routeToCurrentHash);
}

// ── Router ────────────────────────────────────────────────────────────────────
function routeToCurrentHash() {
  const { route, params } = getRoute();
  dispatchRoute(route, params);
}

function dispatchRoute(route, params) {
  switch (route) {
    // Auth
    case 'login':           return renderLogin();
    case 'otp':             return renderOtp(params);
    case 'setup-profile':   return renderSetupProfile();
    case 'child-select':    return renderChildSelect();
    case 'child-pin':       return renderChildPin(params);

    // Parent
    case 'parent-home':     return renderParentHome();
    case 'add-child':       return renderAddChild(params);
    case 'task-review':     return renderTaskReview(params);
    case 'parent-tasks':    return renderParentTasks(params);
    case 'create-task':     return renderCreateTask(params);
    case 'insights':        return renderInsights(params);
    case 'activity':        return renderActivity(params);

    // Child
    case 'child-home':      return renderChildHome(params);
    case 'child-task-detail': return renderChildTaskDetail(params);
    case 'child-tasks':     return renderChildTasks(params);
    case 'revisit-topics':  return renderRevisitTopics();
    case 'homework-log':    return renderHomeworkLog();
    case 'earn-reward':     return renderEarnReward();

    // Learning
    case 'concept-list':    return renderConceptList(params);
    case 'quiz':            return renderQuiz(params);
    case 'upload':          return renderUpload(params);
    case 'daily-news':      return renderDailyNews();

    // Invite / streak
    case 'invite-friend':   return renderInviteFriend();
    case 'streak-dashboard': return renderStreakDashboard();

    // Default
    default:
      if (state.parentId && !state.childId) navigate('parent-home');
      else if (state.childId) navigate('child-home', { childId: state.childId });
      else navigate('login');
  }
}

// ── Invite Friend ─────────────────────────────────────────────────────────────
function renderInviteFriend() {
  const { setScreen } = { setScreen: (html) => { document.getElementById('screen').innerHTML = html; } };
  document.getElementById('screen').innerHTML = `
    <div class="detail-screen">
      <div class="app-bar">
        <button onclick="history.back()" class="icon-btn">${IC_BACK}</button>
        <h2>Invite a Friend</h2>
      </div>
      <div class="detail-content">
        <div class="card" style="text-align:center">
          <div style="font-size:48px">👥</div>
          <h3 class="mt-12">Invite your friends</h3>
          <p class="text-secondary mt-8">Share the Eduksha app with your friends and keep streaks going together!</p>
          <button class="btn btn-primary btn-full mt-20" id="share-btn">📤 Share Eduksha</button>
        </div>
      </div>
    </div>
  `;
  document.getElementById('share-btn')?.addEventListener('click', async () => {
    try {
      if (navigator.share) {
        await navigator.share({
          title: 'Join me on Eduksha!',
          text: "Join me on Eduksha! Let's keep our study streaks going together. 🔥",
          url: 'https://eduksha.in'
        });
      } else {
        await navigator.clipboard.writeText('https://eduksha.in');
        alert('Link copied to clipboard!');
      }
    } catch(e) {}
  });
}

// ── Streak Dashboard ──────────────────────────────────────────────────────────
function renderStreakDashboard() {
  document.getElementById('screen').innerHTML = `
    <div class="detail-screen">
      <div class="app-bar">
        <button onclick="history.back()" class="icon-btn">${IC_BACK}</button>
        <h2>Streak Dashboard</h2>
      </div>
      <div class="detail-content">
        <div class="card" style="text-align:center">
          <div style="font-size:64px">🔥</div>
          <h2 class="mt-8">Keep your streak going!</h2>
          <p class="text-secondary mt-8">Upload your work or complete a quiz every day to maintain your streak.</p>
        </div>
        <div class="card mt-16">
          <div class="fw-bold mb-8">How streaks work</div>
          <div class="streak-guide-row"><span>🌱</span><span>1–2 days: Starting out</span></div>
          <div class="streak-guide-row"><span>✨</span><span>3–6 days: Building habit</span></div>
          <div class="streak-guide-row"><span>🔥</span><span>7–13 days: On fire!</span></div>
          <div class="streak-guide-row"><span>⭐</span><span>14–29 days: Champion</span></div>
          <div class="streak-guide-row"><span>🏆</span><span>30+ days: Legend</span></div>
        </div>
      </div>
    </div>
  `;
}

// ── Start ─────────────────────────────────────────────────────────────────────
boot();

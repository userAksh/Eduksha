import { getFunctionsInstance } from './firebase-init.js';
import { httpsCallable } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-functions.js';
import {
  state, navigate, setScreen, showLoading, hideLoading, showError,
  $, formatDate, IC_BACK
} from './utils.js';

// ── Daily News (Eduksha Times) ────────────────────────────────────────────────
export async function renderDailyNews() {
  setScreen(`
    <div class="detail-screen news-screen">
      <div class="app-bar">
        <button class="icon-btn" id="news-back">${IC_BACK}</button>
        <h2>Eduksha Times</h2>
        <span class="news-date">${new Date().toLocaleDateString('en-IN', { day:'numeric', month:'long' })}</span>
      </div>
      <div id="news-content" class="detail-content news-loading">
        <div class="news-newspaper-fold">
          <div style="font-size:48px">📰</div>
          <div class="fw-bold mt-8">Fetching today's news…</div>
        </div>
      </div>
    </div>
  `);

  $('#news-back').addEventListener('click', () => history.back());

  try {
    const functions = getFunctionsInstance();
    const getDailyNews = httpsCallable(functions, 'getDailyNews');
    const result = await getDailyNews({ grade: state.childGrade || 6 });
    const news = result.data;
    renderNewsContent(news);
  } catch(e) {
    const content = $('#news-content');
    if (content) content.innerHTML = `
      <div class="empty-state">
        <div style="font-size:48px">📰</div>
        <h3>Could not load today's news</h3>
        <p class="text-secondary">Check your internet and try again.</p>
        <button class="btn btn-primary mt-16" id="news-retry">Try Again</button>
      </div>
    `;
    $('#news-retry')?.addEventListener('click', () => renderDailyNews());
  }
}

function renderNewsContent(news) {
  const content = $('#news-content');
  if (!content) return;

  if (!news || (!news.headline && !news.articles?.length)) {
    content.innerHTML = `
      <div class="empty-state">
        <div style="font-size:48px">📰</div>
        <h3>No news today</h3>
        <p class="text-secondary">Come back tomorrow for your daily digest!</p>
      </div>
    `;
    return;
  }

  const articles = news.articles || [];
  const headline = news.headline || '';
  const subHeadline = news.subHeadline || '';
  const did_you_know = news.didYouKnow || news.did_you_know || '';

  content.innerHTML = `
    <div class="news-paper">
      <!-- Masthead -->
      <div class="news-masthead">
        <div class="news-masthead-title">The Eduksha Times</div>
        <div class="news-masthead-date">${new Date().toLocaleDateString('en-IN', { weekday:'long', day:'numeric', month:'long', year:'numeric' })}</div>
        <div class="news-masthead-divider"></div>
      </div>

      <!-- Headline -->
      ${headline ? `
        <div class="news-headline-section">
          <h1 class="news-headline">${headline}</h1>
          ${subHeadline ? `<p class="news-subheadline">${subHeadline}</p>` : ''}
        </div>
      ` : ''}

      <!-- Articles -->
      <div class="news-articles">
        ${articles.map((art, i) => `
          <div class="news-article ${i === 0 ? 'news-lead' : ''}">
            ${art.category ? `<div class="news-category">${art.category}</div>` : ''}
            ${art.title ? `<div class="news-art-title">${art.title}</div>` : ''}
            <div class="news-art-body">${art.body || art.content || ''}</div>
            ${art.funFact ? `<div class="news-fun-fact">🌟 ${art.funFact}</div>` : ''}
          </div>
        `).join('<div class="news-divider"></div>')}
      </div>

      <!-- Did you know -->
      ${did_you_know ? `
        <div class="news-dyk">
          <div class="news-dyk-header">📡 Did You Know?</div>
          <div class="news-dyk-body">${did_you_know}</div>
        </div>
      ` : ''}

      <div class="news-footer">— End of Today's Eduksha Times —</div>
    </div>
  `;
}

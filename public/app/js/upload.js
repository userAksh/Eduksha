import { getDb, getStorageInstance } from './firebase-init.js';
import {
  collection, doc, addDoc
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import {
  ref, uploadBytes, getDownloadURL
} from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js';
import {
  state, navigate, setScreen, showLoading, hideLoading, showError, showSuccess,
  $, subjectIcon, subjectColor, IC_BACK
} from './utils.js';

// ── Upload Work Photos ────────────────────────────────────────────────────────
export function renderUpload({ kind, unitId, conceptId, subject, taskId } = {}) {
  const kindLabel = {
    TUITION_WORK: 'Tuition Work',
    TUITION_HW: 'Tuition Homework',
    TUITION_PROBLEM: 'Tuition Problem',
    HOMEWORK: 'Homework',
    REVISE_CHAPTER: 'Revised Chapter',
    WRITE_QA: 'Written Q&A',
    CUSTOM: 'Task Work',
  }[kind] || 'Work';

  const subjects = ['Mathematics','Science','English','Social Science','Hindi','Sanskrit'];
  const childGrade = state.childGrade || 6;

  setScreen(`
    <div class="detail-screen">
      <div class="app-bar">
        <button class="icon-btn" id="ul-back">${IC_BACK}</button>
        <h2>Upload ${kindLabel}</h2>
      </div>
      <div class="detail-content">
        <div class="upload-form">
          <div class="form-group">
            <label class="form-label">Subject</label>
            <div class="chip-group" id="subj-chips">
              ${subjects.map(s => `
                <button class="chip ${s === (subject||'') ? 'chip-active' : ''}" data-val="${s}">${subjectIcon(s)} ${s}</button>
              `).join('')}
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Topic / Chapter (optional)</label>
            <input id="ul-topic" type="text" class="input-full" placeholder="e.g. Chapter 3 – Light" maxlength="80" />
          </div>

          <div class="form-group">
            <label class="form-label">Photos</label>
            <div class="upload-drop-zone" id="drop-zone">
              <div class="upload-drop-inner">
                <div style="font-size:40px">📷</div>
                <div class="fw-semibold mt-8">Tap to add photos</div>
                <div class="text-secondary" style="font-size:13px">Up to 5 photos</div>
              </div>
              <input type="file" id="photo-input" accept="image/*" multiple hidden />
            </div>
            <div id="photo-previews" class="photo-row mt-8"></div>
          </div>

          <div id="ul-error" class="auth-error" hidden></div>
          <button class="btn btn-primary btn-full mt-16" id="ul-submit" disabled>📤 Upload</button>
        </div>
      </div>
    </div>
  `);

  $('#ul-back').addEventListener('click', () => history.back());

  let selectedSubject = subject || '';
  let selectedFiles = [];

  // Subject chips
  $$('#subj-chips .chip').forEach(chip => {
    chip.addEventListener('click', () => {
      $$('#subj-chips .chip').forEach(c => c.classList.remove('chip-active'));
      chip.classList.add('chip-active');
      selectedSubject = chip.dataset.val;
      checkSubmitReady();
    });
  });

  // Drop zone click → file input
  $('#drop-zone').addEventListener('click', () => $('#photo-input').click());
  $('#photo-input').addEventListener('change', e => {
    const files = Array.from(e.target.files || []);
    selectedFiles = [...selectedFiles, ...files].slice(0, 5);
    renderPreviews();
    checkSubmitReady();
  });

  function renderPreviews() {
    const container = $('#photo-previews');
    if (!container) return;
    container.innerHTML = selectedFiles.map((f, i) => `
      <div class="photo-preview-wrap" style="position:relative;display:inline-block;margin:4px">
        <img src="${URL.createObjectURL(f)}" class="upload-thumb" style="width:72px;height:72px;object-fit:cover;border-radius:8px" />
        <button class="remove-photo" data-idx="${i}" style="position:absolute;top:-6px;right:-6px;background:#ef4444;color:white;border:none;border-radius:50%;width:20px;height:20px;font-size:12px;cursor:pointer">×</button>
      </div>
    `).join('');
    container.querySelectorAll('.remove-photo').forEach(btn => {
      btn.addEventListener('click', () => {
        selectedFiles.splice(+btn.dataset.idx, 1);
        renderPreviews();
        checkSubmitReady();
      });
    });
  }

  function checkSubmitReady() {
    const btn = $('#ul-submit');
    if (btn) btn.disabled = selectedFiles.length === 0;
  }

  $('#ul-submit').addEventListener('click', async () => {
    if (!selectedFiles.length) { showError('Add at least one photo.'); return; }
    const topic = $('#ul-topic')?.value.trim() || '';
    await doUpload({ kind: kind || 'SCHOOL_WORK', subject: selectedSubject, topic, files: selectedFiles, unitId, conceptId, taskId });
  });
}

async function doUpload({ kind, subject, topic, files, unitId, conceptId, taskId }) {
  showLoading('Uploading photos…');
  const childId = state.childId;
  const db = getDb();
  const storage = getStorageInstance();

  try {
    const photoUrls = await Promise.all(files.map(async (file, i) => {
      const ext = file.name.split('.').pop() || 'jpg';
      const storageRef = ref(storage, `uploads/${childId}/${Date.now()}_${i}.${ext}`);
      await uploadBytes(storageRef, file);
      return getDownloadURL(storageRef);
    }));

    const now = Date.now();
    const docRef = await addDoc(collection(db, 'uploads'), {
      childId,
      kind,
      subject: subject || '',
      subjectColor: subjectColor(subject || ''),
      subjectIcon: subjectIcon(subject || ''),
      unitId: unitId || '',
      unitName: '',
      conceptId: conceptId || '',
      conceptName: '',
      topic: topic || '',
      photoUrls,
      timestamp: now,
      createdAt: now
    });

    // If this is a task submission, update the task status
    if (taskId) {
      const { updateDoc, doc: docFn } = await import('https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js');
      await updateDoc(docFn(db, 'parentTaskRequests', taskId), {
        status: 'PENDING_REVIEW',
        submittedAtMillis: now,
        submissionPhotoUrls: photoUrls,
        uploadId: docRef.id
      });
    }

    hideLoading();
    showSuccess('Photos uploaded!');
    if (taskId) navigate('child-task-detail', { taskId });
    else navigate('child-home', { childId });
  } catch(e) {
    hideLoading();
    showError('Upload failed. Check your internet and try again.');
    console.error(e);
  }
}

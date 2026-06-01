import { initializeApp, getApps } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js';
import { getAuth } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js';
import { getFirestore } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
import { getFunctions } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-functions.js';
import { getStorage } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-storage.js';

let _app, _db, _auth, _functions, _storage;

export async function initFirebase() {
  if (_app) return { app: _app, db: _db, auth: _auth, functions: _functions, storage: _storage };
  const res = await fetch('/__/firebase/init.json');
  const config = await res.json();
  _app = getApps().length ? getApps()[0] : initializeApp(config);
  _auth = getAuth(_app);
  _db = getFirestore(_app);
  _functions = getFunctions(_app);
  _storage = getStorage(_app);
  return { app: _app, db: _db, auth: _auth, functions: _functions, storage: _storage };
}

export function getApp() { return _app; }
export function getDb()  { return _db; }
export function getAuthInstance() { return _auth; }
export function getFunctionsInstance() { return _functions; }
export function getStorageInstance() { return _storage; }

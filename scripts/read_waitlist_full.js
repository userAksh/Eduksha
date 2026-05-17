const admin = require('firebase-admin');
const serviceAccount = require('/Users/akshitsharma/AndroidStudioProjects/Dobi2/scripts/serviceAccountKey.json');

if (!admin.apps.length) {
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}
const db = admin.firestore();

(async () => {
  const snap = await db.collection('waitlist').orderBy('createdAt', 'desc').get();
  console.log(`Total: ${snap.size}`);
  console.log('---');
  snap.forEach(doc => {
    const d = doc.data();
    const t = d.createdAt?.toDate?.()?.toISOString?.() || '?';
    console.log(JSON.stringify({
      time: t,
      name: d.parentName || '',
      phone: d.phone || '',
      email: d.email || '',
      class: d.childClass || '',
      locale: d.locale || '',
      source: d.source || ''
    }));
  });
  process.exit(0);
})();

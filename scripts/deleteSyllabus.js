/**
 * deleteSyllabus.js
 *
 * Deletes a previously uploaded syllabus from Firestore — all units under
 * /subjects/{subjectId}/units/* and all concepts in /concepts where
 * grade and subject match.
 *
 * Usage:
 *   node deleteSyllabus.js data/cbse_grade7_science.json
 *
 * Reads only metadata (grade/board/subject) from the JSON to figure out
 * what to delete; the unit/concept lists themselves are not used, so this
 * cleans up orphans from older versions of the same syllabus.
 */

const path = require("path");
const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

const dataArg = process.argv[2];
if (!dataArg) {
  console.error("Usage: node deleteSyllabus.js <path-to-syllabus.json>");
  process.exit(1);
}
const dataPath = path.isAbsolute(dataArg) ? dataArg : path.resolve(__dirname, dataArg);
const { metadata } = require(dataPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: serviceAccount.project_id,
});

const db = admin.firestore();

async function deleteCollectionDocs(query, label) {
  const snap = await query.get();
  if (snap.empty) {
    console.log(`  (no ${label} to delete)`);
    return 0;
  }
  // Firestore batch limit is 500 ops.
  const docs = snap.docs;
  let deleted = 0;
  for (let i = 0; i < docs.length; i += 499) {
    const batch = db.batch();
    docs.slice(i, i + 499).forEach((d) => batch.delete(d.ref));
    await batch.commit();
    deleted += Math.min(499, docs.length - i);
  }
  console.log(`  Deleted ${deleted} ${label}.`);
  return deleted;
}

async function deleteSyllabus() {
  const subjectDocId = `${metadata.board.toLowerCase()}_grade${metadata.grade}_${metadata.subject.toLowerCase()}`;

  console.log(`\nDeleting ${metadata.board} Grade ${metadata.grade} ${metadata.subject}...`);

  const unitsRef = db.collection("subjects").doc(subjectDocId).collection("units");
  await deleteCollectionDocs(unitsRef, "units");

  const conceptsRef = db
    .collection("concepts")
    .where("grade", "==", metadata.grade)
    .where("board", "==", metadata.board)
    .where("subject", "==", metadata.subject);
  await deleteCollectionDocs(conceptsRef, "concepts");

  console.log(`\n✅ Cleanup done for ${subjectDocId}.\n`);
}

deleteSyllabus().catch((err) => {
  console.error("❌ Delete failed:", err);
  process.exit(1);
});

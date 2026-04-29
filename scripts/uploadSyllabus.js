/**
 * uploadSyllabus.js
 *
 * Uploads a syllabus JSON to Firestore.
 *
 * Setup:
 *   1. npm install firebase-admin
 *   2. Firebase Console → Project Settings → Service Accounts → Generate new private key
 *   3. Save the downloaded file as scripts/serviceAccountKey.json
 *
 * Usage:
 *   node uploadSyllabus.js                                # defaults to data/cbse_grade6_science.json
 *   node uploadSyllabus.js data/cbse_grade7_science.json  # explicit file
 */

const path = require("path");
const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

const dataArg = process.argv[2] || "data/cbse_grade6_science.json";
const dataPath = path.isAbsolute(dataArg) ? dataArg : path.resolve(__dirname, dataArg);
const syllabusData = require(dataPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: serviceAccount.project_id,
});

const db = admin.firestore();

// ── Helpers ────────────────────────────────────────────────────────────────────

function chunkArray(array, size) {
  const chunks = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
}

async function commitBatch(operations) {
  const batch = db.batch();
  for (const { ref, data } of operations) {
    batch.set(ref, data);
  }
  await batch.commit();
}

// ── Main upload ────────────────────────────────────────────────────────────────

async function uploadSyllabus() {
  const { metadata, units } = syllabusData;
  const allOperations = [];

  console.log(`\nUploading ${metadata.board} Grade ${metadata.grade} ${metadata.subject}...\n`);

  // 1. Upload each unit as a document in /subjects/{subjectId}/units/{unitId}
  for (const unit of units) {
    const unitRef = db
      .collection("subjects")
      .doc(`${metadata.board.toLowerCase()}_grade${metadata.grade}_${metadata.subject.toLowerCase()}`)
      .collection("units")
      .doc(unit.id);

    allOperations.push({
      ref: unitRef,
      data: {
        id: unit.id,
        name: unit.name,
        order: unit.order,
        grade: metadata.grade,
        board: metadata.board,
        subject: metadata.subject,
        conceptCount: unit.concepts.length,
      },
    });

    // 2. Upload each concept as a document in /concepts/{conceptId}
    for (const concept of unit.concepts) {
      const conceptRef = db.collection("concepts").doc(concept.id);
      allOperations.push({
        ref: conceptRef,
        data: {
          id: concept.id,
          name: concept.name,
          description: concept.description,
          subject: metadata.subject,
          subjectColor: metadata.subjectColor,
          subjectIcon: metadata.subjectIcon,
          unit: unit.name,
          unitId: unit.id,
          unitOrder: unit.order,
          grade: metadata.grade,
          board: metadata.board,
          expectedDifficulty: concept.expectedDifficulty,
          aliases: concept.aliases,
          prerequisites: concept.prerequisites,
          order: concept.order,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        },
      });
    }
  }

  // Firestore batch limit = 500 writes. Chunk into batches of 499.
  const chunks = chunkArray(allOperations, 499);
  let totalUploaded = 0;

  for (let i = 0; i < chunks.length; i++) {
    await commitBatch(chunks[i]);
    totalUploaded += chunks[i].length;
    console.log(`  Batch ${i + 1}/${chunks.length} committed (${totalUploaded} docs so far)`);
  }

  console.log(`\n✅ Done! Uploaded ${totalUploaded} documents total.`);
  console.log(`   → ${units.length} units`);
  console.log(`   → ${allOperations.length - units.length} concepts\n`);
}

uploadSyllabus().catch((err) => {
  console.error("❌ Upload failed:", err);
  process.exit(1);
});

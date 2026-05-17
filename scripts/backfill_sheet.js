/**
 * One-time backfill script: dumps all existing waitlist entries into the
 * Google Sheet that the Cloud Function will keep in sync going forward.
 *
 * Usage:
 *   1. Set SHEET_ID at the top
 *   2. Make sure the sheet is shared (Editor) with dobi-ac085@appspot.gserviceaccount.com
 *   3. Make sure the Sheets API is enabled for project dobi-ac085
 *   4. Run: node backfill_sheet.js
 *
 * The script writes a header row + every doc currently in /waitlist.
 * It's safe to re-run (it clears + rewrites Sheet1 each time).
 */

const admin = require("firebase-admin");
const { google } = require("googleapis");
const serviceAccount = require("./serviceAccountKey.json");

const SHEET_ID = "13P9xZ8LnIoCiZw-I-FseZs7yw957rbekrqsSQ0WbU3U";
const RANGE_ALL = "Sheet1!A:J";
const HEADER_ROW = [
  "Time (UTC)",
  "Name",
  "Phone",
  "Email",
  "Class",
  "Locale",
  "Source",
  "Firestore docId",
  "Status",
  "Comment",
];
const STATUS_VALUES = ["Connected", "Not picked", "Wrong number"];

async function main() {
  if (!SHEET_ID || SHEET_ID === "REPLACE_WITH_SHEET_ID") {
    console.error("Set SHEET_ID at top of file first.");
    process.exit(1);
  }

  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  const db = admin.firestore();

  const auth = new google.auth.GoogleAuth({
    keyFile: __dirname + "/serviceAccountKey.json",
    scopes: ["https://www.googleapis.com/auth/spreadsheets"],
  });
  const sheets = google.sheets({ version: "v4", auth });

  const snap = await db.collection("waitlist").orderBy("createdAt", "asc").get();
  console.log(`Fetched ${snap.size} waitlist docs from Firestore.`);

  const rows = [HEADER_ROW];
  snap.forEach((doc) => {
    const d = doc.data();
    const t = d.createdAt?.toDate?.()?.toISOString?.() || "";
    rows.push([
      t,
      d.parentName || "",
      d.phone || "",
      d.email || "",
      d.childClass || "",
      d.locale || "",
      d.source || "",
      doc.id,
    ]);
  });

  // Wipe Sheet1 (keep tab structure), then write the full block.
  await sheets.spreadsheets.values.clear({
    spreadsheetId: SHEET_ID,
    range: RANGE_ALL,
  });
  await sheets.spreadsheets.values.update({
    spreadsheetId: SHEET_ID,
    range: "Sheet1!A1",
    valueInputOption: "RAW",
    requestBody: { values: rows },
  });

  // Look up the first sheet's numeric sheetId (usually 0 for "Sheet1", but
  // safer to read it).
  const meta = await sheets.spreadsheets.get({ spreadsheetId: SHEET_ID });
  const firstSheetId = meta.data.sheets?.[0]?.properties?.sheetId ?? 0;

  // Apply: (1) freeze header row, (2) dropdown validation on Status (col I,
  // 0-indexed 8) for the data range we expect to fill over time.
  await sheets.spreadsheets.batchUpdate({
    spreadsheetId: SHEET_ID,
    requestBody: {
      requests: [
        {
          updateSheetProperties: {
            properties: {
              sheetId: firstSheetId,
              gridProperties: { frozenRowCount: 1 },
            },
            fields: "gridProperties.frozenRowCount",
          },
        },
        {
          setDataValidation: {
            range: {
              sheetId: firstSheetId,
              startRowIndex: 1, // skip header
              endRowIndex: 10000,
              startColumnIndex: 8, // column I
              endColumnIndex: 9,
            },
            rule: {
              condition: {
                type: "ONE_OF_LIST",
                values: STATUS_VALUES.map((v) => ({ userEnteredValue: v })),
              },
              showCustomUi: true,
              strict: false,
            },
          },
        },
      ],
    },
  });

  console.log(
    `Wrote ${rows.length - 1} signup rows + header to sheet ${SHEET_ID}.`
  );
  console.log("Status dropdown + header freeze applied.");
  process.exit(0);
}

main().catch((err) => {
  console.error("Backfill failed:", err);
  process.exit(1);
});

/**
 * Cloud Functions for Eduksha parent-task push notifications.
 *
 * Triggers:
 *   onWritten on parentTaskRequests/{taskId}
 *     - Created (status REQUESTED)        → notify parent
 *     - REQUESTED → ASSIGNED              → notify child
 *     - ASSIGNED  → PENDING_REVIEW        → notify parent
 *     - PENDING_REVIEW → ACCEPTED         → notify child ("Great job!")
 *     - PENDING_REVIEW → REJECTED         → notify child ("Please redo")
 *
 *   pendingReviewReminder (scheduled every 60 min)
 *     - Scans PENDING_REVIEW docs older than 24h since the last reminder
 *       (or since submission if no prior reminder), sends a reminder push
 *       to the parent, and stamps `lastReviewReminderAtMillis` so we don't
 *       buzz the same parent every hour.
 *
 * FCM token paths:
 *   - Parent device: parents/{parentId}.fcmToken
 *   - Child device:  parents/{parentId}/children/{childId}.fcmToken
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const logger = require("firebase-functions/logger");

// Daily-news Gemini key. Set with:
//   firebase functions:secrets:set GEMINI_API_KEY
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

initializeApp();
const db = getFirestore();
const fcm = getMessaging();

const REMINDER_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24h

exports.onParentTaskWrite = onDocumentWritten(
  "parentTaskRequests/{taskId}",
  async (event) => {
    const before = event.data?.before?.data() || null;
    const after = event.data?.after?.data() || null;
    if (!after) return; // doc deleted, nothing to do

    const taskId = event.params.taskId;
    const beforeStatus = before?.status || null;
    const afterStatus = after.status;

    const transition = `${beforeStatus || "(new)"} → ${afterStatus}`;
    logger.info(`parentTask ${taskId}: ${transition}`);

    // Decide who to notify and what to say.
    let payload = null;
    if (!before && afterStatus === "REQUESTED") {
      payload = {
        target: "parent",
        title: "New task request",
        body: childTopicLine(after, "wants to study"),
      };
    } else if (beforeStatus === "REQUESTED" && afterStatus === "ASSIGNED") {
      payload = {
        target: "child",
        title: "You have a new task!",
        body: assignmentLine(after),
      };
    } else if (
      beforeStatus === "ASSIGNED" &&
      afterStatus === "PENDING_REVIEW"
    ) {
      payload = {
        target: "parent",
        title: "Submission ready to review",
        body: childTopicLine(after, "submitted"),
      };
    } else if (
      beforeStatus === "PENDING_REVIEW" &&
      afterStatus === "ACCEPTED"
    ) {
      payload = {
        target: "child",
        title: "Great job! 🎉",
        body: after.parentReviewNote
          ? `${after.parentReviewNote}`
          : `Your task on "${displayName(after)}" was accepted.`,
      };
    } else if (
      beforeStatus === "PENDING_REVIEW" &&
      afterStatus === "REJECTED"
    ) {
      payload = {
        target: "child",
        title: "Your parent asked for a redo",
        body: after.parentReviewNote || "Please try this task again.",
      };
    }

    if (!payload) return;

    const recipientToken = await resolveToken(after, payload.target);
    if (!recipientToken) {
      logger.warn(`No FCM token for ${payload.target} on task ${taskId}`);
      return;
    }

    await sendPush(recipientToken, payload, taskId);
  }
);

exports.pendingReviewReminder = onSchedule(
  { schedule: "every 60 minutes", timeZone: "Etc/UTC" },
  async () => {
    const cutoff = Date.now() - REMINDER_INTERVAL_MS;
    // Pending submissions where the parent hasn't reviewed in 24h+.
    // We reuse `submittedAtMillis` for the first reminder; once we've sent
    // a reminder we stamp `lastReviewReminderAtMillis` so we don't repeat
    // every hour.
    const snap = await db
      .collection("parentTaskRequests")
      .where("status", "==", "PENDING_REVIEW")
      .where("submittedAtMillis", "<=", cutoff)
      .get();

    let sent = 0;
    for (const doc of snap.docs) {
      const data = doc.data();
      const lastReminder = data.lastReviewReminderAtMillis || 0;
      const reference = Math.max(lastReminder, data.submittedAtMillis || 0);
      if (Date.now() - reference < REMINDER_INTERVAL_MS) continue;

      const token = await resolveToken(data, "parent");
      if (!token) continue;
      await sendPush(
        token,
        {
          target: "parent",
          title: "Pending review",
          body: childTopicLine(data, "is waiting on your review of"),
        },
        doc.id
      );
      await doc.ref.update({
        lastReviewReminderAtMillis: FieldValue.serverTimestamp().toMillis
          ? Date.now()
          : Date.now(),
      });
      sent += 1;
    }
    logger.info(`pendingReviewReminder: sent ${sent} reminders`);
  }
);

// ── Helpers ────────────────────────────────────────────────────────────────

async function resolveToken(taskData, target) {
  const { parentId, childId } = taskData;
  if (target === "parent") {
    const parent = await db.collection("parents").doc(parentId).get();
    return parent.data()?.fcmToken || null;
  }
  if (target === "child") {
    const child = await db
      .collection("parents")
      .doc(parentId)
      .collection("children")
      .doc(childId)
      .get();
    return child.data()?.fcmToken || null;
  }
  return null;
}

function displayName(task) {
  return task.isUnitScope ? task.unitName : task.conceptName;
}

function childTopicLine(task, verb) {
  const name = displayName(task) || "a topic";
  return `${verb} "${name}"`;
}

function assignmentLine(task) {
  const scope = displayName(task) || "your topic";
  switch (task.type) {
    case "REVISE_CHAPTER":
      return `Revise "${scope}" and upload your notes.`;
    case "WRITE_QA":
      return `Write Q&A from "${scope}" and upload a photo.`;
    case "OUR_QUESTIONS":
      return `Answer ${task.mcqCount || 5} questions on "${scope}".`;
    case "CUSTOM":
      return task.customNote
        ? `${task.customNote}`
        : `Open Eduksha to see what to do for "${scope}".`;
    default:
      return `Tap to start your task on "${scope}".`;
  }
}

async function sendPush(token, { title, body, target }, taskId) {
  const role = target === "parent" ? "parent" : "child";
  await fcm.send({
    token,
    notification: { title, body },
    data: { taskId, role, title, body },
    android: {
      priority: "high",
      notification: { channelId: "dobi_default", clickAction: "" },
    },
  });
}

// ── Friend invites ─────────────────────────────────────────────────────────
//
// createInvite — caller is the inviter's parent device. We trust auth.uid as
// parentId and require the child to live under that parent. Returns a short
// code that the app embeds into the Play Store install-referrer URL.
//
// claimInvite — caller is the invitee's parent device, after a brand-new
// install. The Play Install Referrer surfaced `invite_{code}` to the app.
// We:
//   1. validate the code + claimer in a transaction
//   2. write reciprocal `friends` entries on both child docs
//   3. record the claim so re-invocations are idempotent
//   4. push an FCM "joined" notification to the inviter's parent

const INVITE_CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"; // no 0/o/1/l/i
const INVITE_CODE_LENGTH = 8;
const INVITE_TTL_MS = 90 * 24 * 60 * 60 * 1000; // 90 days

function generateInviteCode() {
  let code = "";
  for (let i = 0; i < INVITE_CODE_LENGTH; i++) {
    code += INVITE_CODE_ALPHABET.charAt(
      Math.floor(Math.random() * INVITE_CODE_ALPHABET.length)
    );
  }
  return code;
}

exports.createInvite = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const parentId = request.auth.uid;
  const { childId } = request.data || {};
  if (!childId) {
    throw new HttpsError("invalid-argument", "childId required");
  }

  const childRef = db.collection("parents").doc(parentId)
    .collection("children").doc(childId);
  const childSnap = await childRef.get();
  if (!childSnap.exists) {
    throw new HttpsError("not-found", "Child not found under caller");
  }
  const child = childSnap.data();
  const code = generateInviteCode();
  const now = Date.now();

  await db.collection("invites").doc(code).set({
    code,
    inviterParentId: parentId,
    inviterChildId: childId,
    inviterName: child.name || "",
    inviterGrade: child.grade || 0,
    inviterGender: child.gender || "",
    createdAtMillis: now,
    expiresAtMillis: now + INVITE_TTL_MS,
    claimedBy: [],
  });
  return { code };
});

exports.claimInvite = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const claimerParentId = request.auth.uid;
  const { code, childId: claimerChildId } = request.data || {};
  if (!code || !claimerChildId) {
    throw new HttpsError("invalid-argument", "code and childId required");
  }

  const inviteRef = db.collection("invites").doc(code);
  const claimerRef = db.collection("parents").doc(claimerParentId)
    .collection("children").doc(claimerChildId);

  const result = await db.runTransaction(async (tx) => {
    const inviteSnap = await tx.get(inviteRef);
    if (!inviteSnap.exists) {
      throw new HttpsError("not-found", "Invite not found");
    }
    const invite = inviteSnap.data();
    if (Date.now() > (invite.expiresAtMillis || 0)) {
      throw new HttpsError("deadline-exceeded", "Invite expired");
    }

    // No-op if a child tries to claim their own invite.
    if (
      invite.inviterParentId === claimerParentId &&
      invite.inviterChildId === claimerChildId
    ) {
      return { skipped: "self" };
    }

    const claimerSnap = await tx.get(claimerRef);
    if (!claimerSnap.exists) {
      throw new HttpsError("not-found", "Claimer child not found");
    }
    const claimer = claimerSnap.data();

    // Idempotent re-claim: app may retry if FCM isn't ready, etc.
    const alreadyClaimed = (invite.claimedBy || []).some(
      (c) => c.parentId === claimerParentId && c.childId === claimerChildId
    );
    if (alreadyClaimed) {
      return { skipped: "already-claimed" };
    }

    const inviterRef = db.collection("parents").doc(invite.inviterParentId)
      .collection("children").doc(invite.inviterChildId);
    const inviterSnap = await tx.get(inviterRef);
    if (!inviterSnap.exists) {
      // Inviter child was deleted between create and claim. Mark the
      // claim so we don't keep retrying, but skip the friend writes.
      tx.update(inviteRef, {
        claimedBy: FieldValue.arrayUnion({
          parentId: claimerParentId,
          childId: claimerChildId,
          name: claimer.name || "",
          claimedAtMillis: Date.now(),
          status: "inviter-missing",
        }),
      });
      return { skipped: "inviter-missing" };
    }
    const inviter = inviterSnap.data();
    const now = Date.now();

    // Reciprocal friend edges.
    tx.set(inviterRef, {
      friends: FieldValue.arrayUnion({
        parentId: claimerParentId,
        childId: claimerChildId,
        name: claimer.name || "",
        grade: claimer.grade || 0,
        gender: claimer.gender || "",
        addedAtMillis: now,
      }),
    }, { merge: true });

    tx.set(claimerRef, {
      friends: FieldValue.arrayUnion({
        parentId: invite.inviterParentId,
        childId: invite.inviterChildId,
        name: invite.inviterName || inviter.name || "",
        grade: invite.inviterGrade || inviter.grade || 0,
        gender: invite.inviterGender || inviter.gender || "",
        addedAtMillis: now,
      }),
    }, { merge: true });

    tx.update(inviteRef, {
      claimedBy: FieldValue.arrayUnion({
        parentId: claimerParentId,
        childId: claimerChildId,
        name: claimer.name || "",
        claimedAtMillis: now,
        status: "claimed",
      }),
    });

    return {
      claimed: true,
      inviterParentId: invite.inviterParentId,
      inviterChildName: invite.inviterName || inviter.name || "your child",
      claimerName: claimer.name || "A new friend",
    };
  });

  if (result.claimed && result.inviterParentId) {
    const parent = await db.collection("parents").doc(result.inviterParentId).get();
    const token = parent.data()?.fcmToken;
    if (token) {
      try {
        await fcm.send({
          token,
          notification: {
            title: "🎉 New friend joined!",
            body: `${result.claimerName} joined Eduksha from ${result.inviterChildName}'s invite.`,
          },
          data: { kind: "friend_joined", role: "parent" },
          android: {
            priority: "high",
            notification: { channelId: "dobi_default" },
          },
        });
      } catch (err) {
        logger.warn(`friend_joined push failed: ${err.message}`);
      }
    }
  }

  return { ok: true, status: result.skipped || "claimed" };
});

// ── Daily kid-safe news ────────────────────────────────────────────────────
//
// getDailyNews — caller is any signed-in device (parent or child). We keep
// ONE global cache doc at `dobi_meta/dailyNews`; if it's < 24h old we return
// it directly, otherwise we ask Gemini (with Google Search grounding) to
// curate 5 motivational + knowledge stories from the last day, save the
// result, and return it. Net cost: ~1 Gemini call/day for the entire user
// base, regardless of how many kids open the newspaper.

const NEWS_TTL_MS = 24 * 60 * 60 * 1000;

const NEWS_PROMPT_VERSION = "india-only-v1";

const NEWS_SYSTEM_INSTRUCTION = `You curate a daily "Did You Know?" newspaper for the Eduksha learning app, read by kids aged 8 to 13.

ALL stories MUST be from INDIA ONLY. The story, the people, the place, or the institution involved must be Indian. Do not pick stories from other countries. Do not pick stories where the only India connection is a quote or a tweet. If you cannot find 5 Indian stories from the last 1-2 days, pick lighter India-focused "Did you know?" facts (e.g. an Indian wildlife fact, a heritage fact, a famous Indian invention, an ISRO milestone) to round out the 5.

You MUST refuse and replace any story that contains:
- violence, war, terrorism, accidents, deaths, injuries, crime
- politics, elections, religion, communal topics
- celebrity gossip, scandals, anything sexual or romantic
- scary, sad, fear-inducing or hopeless content

You ONLY pick stories that are:
- motivational, uplifting, knowledge-building, or "wow that's cool"
- examples: ISRO / space missions, Indian scientific discoveries, Indian
  environment wins, Indian young achievers, Indians winning awards or
  competitions, friendly animal/wildlife news from India, Indian inventions
  for good, Indian sports records (no controversies), new museums or
  exhibits in India, Indian medical breakthroughs, Indian world records,
  India-specific fun facts and heritage trivia
- written in warm, simple English a 10-year-old understands

Tone: like a fun teacher reading aloud from a kids' newspaper.

OUTPUT: JSON only. No markdown fences, no commentary, no surrounding text.
Schema:
{
  "stories": [
    {
      "id": "story-1",
      "category": "Space" | "Science" | "Sports" | "Nature" | "Technology" | "India" | "Fun fact",
      "emoji": "one emoji that fits the story",
      "headline": "max 10 words, punchy, no clickbait, no all-caps",
      "body": "2 to 3 short sentences a 10-year-old can read in 15 seconds, ending with a fun takeaway"
    }
  ]
}
Always return exactly 5 India-focused stories spanning at least 3 different categories.`;

exports.getDailyNews = onCall(
  { secrets: [GEMINI_API_KEY], timeoutSeconds: 60 },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in required");
    }

    const cacheRef = db.collection("dobi_meta").doc("dailyNews");
    const snap = await cacheRef.get();
    const cached = snap.exists ? snap.data() : null;
    const now = Date.now();

    // Refresh whenever the prompt version changes so old (international)
    // caches don't stick around for 24h after a deploy.
    const isFresh =
      cached &&
      Array.isArray(cached.stories) &&
      cached.stories.length > 0 &&
      (cached.expiresAtMillis || 0) > now &&
      cached.promptVersion === NEWS_PROMPT_VERSION;

    if (isFresh) {
      return {
        stories: cached.stories,
        generatedAtMillis: cached.generatedAtMillis,
        cached: true,
      };
    }

    let stories;
    try {
      stories = await fetchDailyNewsFromGemini();
    } catch (err) {
      logger.error("getDailyNews: Gemini refresh failed", err);
      // Fall back to stale cache if anything is there.
      if (cached && Array.isArray(cached.stories) && cached.stories.length) {
        return {
          stories: cached.stories,
          generatedAtMillis: cached.generatedAtMillis,
          cached: true,
          stale: true,
        };
      }
      throw new HttpsError("unavailable", "Daily news unavailable");
    }

    await cacheRef.set({
      stories,
      generatedAtMillis: now,
      expiresAtMillis: now + NEWS_TTL_MS,
      refreshedBy: request.auth.uid,
      promptVersion: NEWS_PROMPT_VERSION,
    });

    return { stories, generatedAtMillis: now, cached: false };
  }
);

async function fetchDailyNewsFromGemini() {
  const { GoogleGenAI } = require("@google/genai");
  const ai = new GoogleGenAI({ apiKey: GEMINI_API_KEY.value() });

  const today = new Date().toISOString().slice(0, 10);
  const prompt = `Today's date is ${today}. Use Google Search to find the most interesting child-safe stories from INDIA over the last 1 to 2 days. EVERY story must be from India — Indian people, Indian places, Indian institutions. Do NOT include international news. If fewer than 5 fresh Indian stories exist, fill the remainder with India-focused "did you know?" facts (heritage, wildlife, science, ISRO milestones). Return exactly 5 India-only stories as JSON, following the schema in the system instructions. Output ONLY the JSON object.`;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      systemInstruction: NEWS_SYSTEM_INSTRUCTION,
      tools: [{ googleSearch: {} }],
      temperature: 0.7,
    },
  });

  const raw = (response.text || "").trim();
  const jsonMatch = raw.match(/\{[\s\S]*\}/);
  if (!jsonMatch) {
    throw new Error("Gemini response had no JSON object");
  }
  const parsed = JSON.parse(jsonMatch[0]);
  const stories = Array.isArray(parsed.stories) ? parsed.stories : [];
  const cleaned = stories
    .slice(0, 5)
    .map((s, i) => ({
      id: typeof s.id === "string" && s.id.length ? s.id : `story-${i + 1}`,
      category: typeof s.category === "string" && s.category.length
        ? s.category
        : "Fun fact",
      emoji: typeof s.emoji === "string" && s.emoji.length ? s.emoji : "💡",
      headline: typeof s.headline === "string" ? s.headline.trim() : "",
      body: typeof s.body === "string" ? s.body.trim() : "",
    }))
    .filter((s) => s.headline.length > 0 && s.body.length > 0);

  if (cleaned.length === 0) {
    throw new Error("Gemini returned no usable stories");
  }
  return cleaned;
}

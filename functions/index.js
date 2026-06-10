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

const { onDocumentWritten, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { getAuth } = require("firebase-admin/auth");
const { getStorage } = require("firebase-admin/storage");
const logger = require("firebase-functions/logger");
const { google } = require("googleapis");
const { GoogleGenAI } = require("@google/genai");
const Razorpay = require("razorpay");

// Daily-news Gemini key. Set with:
//   firebase functions:secrets:set GEMINI_API_KEY
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

// Razorpay keys. Set with:
//   firebase functions:secrets:set RAZORPAY_KEY_ID
//   firebase functions:secrets:set RAZORPAY_KEY_SECRET
const RAZORPAY_KEY_ID     = defineSecret("RAZORPAY_KEY_ID");
const RAZORPAY_KEY_SECRET = defineSecret("RAZORPAY_KEY_SECRET");

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
    } else if (!before && afterStatus === "ASSIGNED") {
      // Parent assigned directly (Detailed Learning Report → Assign Practice
      // skips the REQUESTED stage and creates the doc straight as ASSIGNED).
      payload = {
        target: "child",
        title: "You have a new task!",
        body: assignmentLine(after),
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
    } else if (
      // Manual reminder: parent tapped "Send reminder" on an ASSIGNED task
      // that's been sitting for 24h+. Bumps reminderSentAtMillis, no status
      // change. Push to the child.
      beforeStatus === "ASSIGNED" &&
      afterStatus === "ASSIGNED" &&
      (before?.reminderSentAtMillis || 0) !== (after.reminderSentAtMillis || 0) &&
      (after.reminderSentAtMillis || 0) > 0
    ) {
      payload = {
        target: "child",
        title: "Reminder from your parent",
        body: `Don't forget: ${assignmentLine(after)}`,
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
const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

// ── Referral reward config — edit here to change reward values without an app release ──
const PARENT_REFERRAL_REWARDS = {
  1: { label: "Welcome gift 🎁", value: "EDUKSHA10" },   // stage 1: referee adds first child
  2: { label: "Week reward 🏆",  value: "PREMIUM7DAYS" }, // stage 2: referee active for 7 days
};

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
  const { childId, inviterType = "child" } = request.data || {};
  const code = generateInviteCode();
  const now = Date.now();
  const base = {
    code,
    inviterType,
    inviterParentId: parentId,
    createdAtMillis: now,
    expiresAtMillis: now + INVITE_TTL_MS,
    claimedBy: [],
  };

  if (inviterType === "parent") {
    // Fetch all children so we can do a grade+board match at claim time.
    const childrenSnap = await db.collection("parents").doc(parentId)
      .collection("children").get();
    const inviterChildren = childrenSnap.docs.map((d) => ({
      childId: d.id,
      name: d.data().name || "",
      grade: d.data().grade || 0,
      board: (d.data().board || "").trim().toUpperCase(),
      gender: d.data().gender || "",
    }));
    await db.collection("invites").doc(code).set({
      ...base,
      inviterChildren,
    });
  } else {
    // Child invite — existing behaviour.
    if (!childId) throw new HttpsError("invalid-argument", "childId required for child invite");
    const childSnap = await db.collection("parents").doc(parentId)
      .collection("children").doc(childId).get();
    if (!childSnap.exists) throw new HttpsError("not-found", "Child not found under caller");
    const child = childSnap.data();
    await db.collection("invites").doc(code).set({
      ...base,
      inviterChildId: childId,
      inviterName: child.name || "",
      inviterGrade: child.grade || 0,
      inviterGender: child.gender || "",
    });
  }
  return { code };
});

exports.claimInvite = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const claimerParentId = request.auth.uid;
  const { code, childId: claimerChildId } = request.data || {};
  if (!code) throw new HttpsError("invalid-argument", "code required");

  // Read invite first to determine type before requiring childId.
  const inviteRef = db.collection("invites").doc(code);
  const inviteSnap = await inviteRef.get();
  if (!inviteSnap.exists) throw new HttpsError("not-found", "Invite not found");
  const invite = inviteSnap.data();
  if (Date.now() > (invite.expiresAtMillis || 0)) {
    throw new HttpsError("deadline-exceeded", "Invite expired");
  }

  // ── Parent-type invite: store pendingReferral, no friend edges yet ──────────
  if (invite.inviterType === "parent") {
    if (invite.inviterParentId === claimerParentId) return { ok: true, status: "self" };
    const alreadyClaimed = (invite.claimedBy || []).some(
      (c) => c.parentId === claimerParentId
    );
    if (alreadyClaimed) return { ok: true, status: "already-claimed" };

    const now = Date.now();
    await db.runTransaction(async (tx) => {
      tx.set(db.collection("parents").doc(claimerParentId), {
        pendingReferral: {
          code,
          inviterParentId: invite.inviterParentId,
          inviterChildren: invite.inviterChildren || [],
          claimedAtMillis: now,
          childAddedAtMillis: null,
          weekMilestoneDone: false,
        },
      }, { merge: true });
      tx.update(inviteRef, {
        claimedBy: FieldValue.arrayUnion({
          parentId: claimerParentId,
          claimedAtMillis: now,
          status: "claimed",
        }),
      });
    });
    return { ok: true, status: "claimed" };
  }

  // ── Child-type invite: existing reciprocal friend-edge behaviour ─────────────
  if (!claimerChildId) throw new HttpsError("invalid-argument", "childId required for child invite");
  const claimerRef = db.collection("parents").doc(claimerParentId)
    .collection("children").doc(claimerChildId);

  const result = await db.runTransaction(async (tx) => {
    if (
      invite.inviterParentId === claimerParentId &&
      invite.inviterChildId === claimerChildId
    ) return { skipped: "self" };

    const claimerSnap = await tx.get(claimerRef);
    if (!claimerSnap.exists) throw new HttpsError("not-found", "Claimer child not found");
    const claimer = claimerSnap.data();

    const alreadyClaimed = (invite.claimedBy || []).some(
      (c) => c.parentId === claimerParentId && c.childId === claimerChildId
    );
    if (alreadyClaimed) return { skipped: "already-claimed" };

    const inviterRef = db.collection("parents").doc(invite.inviterParentId)
      .collection("children").doc(invite.inviterChildId);
    const inviterSnap = await tx.get(inviterRef);
    if (!inviterSnap.exists) {
      tx.update(inviteRef, {
        claimedBy: FieldValue.arrayUnion({
          parentId: claimerParentId, childId: claimerChildId,
          name: claimer.name || "", claimedAtMillis: Date.now(), status: "inviter-missing",
        }),
      });
      return { skipped: "inviter-missing" };
    }
    const inviter = inviterSnap.data();
    const now = Date.now();

    tx.set(inviterRef, {
      friends: FieldValue.arrayUnion({
        parentId: claimerParentId, childId: claimerChildId,
        name: claimer.name || "", grade: claimer.grade || 0,
        gender: claimer.gender || "", addedAtMillis: now,
      }),
    }, { merge: true });
    tx.set(claimerRef, {
      friends: FieldValue.arrayUnion({
        parentId: invite.inviterParentId, childId: invite.inviterChildId,
        name: invite.inviterName || inviter.name || "",
        grade: invite.inviterGrade || inviter.grade || 0,
        gender: invite.inviterGender || inviter.gender || "",
        addedAtMillis: now,
      }),
    }, { merge: true });
    tx.update(inviteRef, {
      claimedBy: FieldValue.arrayUnion({
        parentId: claimerParentId, childId: claimerChildId,
        name: claimer.name || "", claimedAtMillis: now, status: "claimed",
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
          android: { priority: "high", notification: { channelId: "dobi_default" } },
        });
      } catch (err) {
        logger.warn(`friend_joined push failed: ${err.message}`);
      }
    }
  }
  return { ok: true, status: result.skipped || "claimed" };
});

// ── applyReferralMatch ─────────────────────────────────────────────────────
//
// Called by the referee's app after they add their first child.
// Checks grade+board match against the inviter's children. If matched:
//   1. Writes reciprocal friend edges between the matched children.
//   2. Unlocks Stage 1 reward for the inviter parent.
//   3. Sets pendingReferral.childAddedAtMillis (enables week-milestone check).
//   4. Sends FCM to inviter.

exports.applyReferralMatch = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in required");
  const refereeParentId = request.auth.uid;
  const { childId: refereeChildId, grade: refereeGrade, board: refereeBoard } = request.data || {};
  if (!refereeChildId || !refereeGrade || !refereeBoard) {
    throw new HttpsError("invalid-argument", "childId, grade and board required");
  }

  const refereeParentRef = db.collection("parents").doc(refereeParentId);
  const refereeParentSnap = await refereeParentRef.get();
  const pendingReferral = refereeParentSnap.data()?.pendingReferral;
  if (!pendingReferral || !pendingReferral.inviterParentId) {
    return { ok: true, status: "no-pending-referral" };
  }
  if (pendingReferral.childAddedAtMillis) {
    return { ok: true, status: "already-applied" };
  }

  const normalizedBoard = (refereeBoard || "").trim().toUpperCase();
  const matchedInviterChild = (pendingReferral.inviterChildren || []).find(
    (c) => c.grade === refereeGrade && (c.board || "").trim().toUpperCase() === normalizedBoard
  );

  const now = Date.now();
  const inviterParentId = pendingReferral.inviterParentId;
  const rewardCfg = PARENT_REFERRAL_REWARDS[1];
  const rewardId = db.collection("parents").doc(inviterParentId)
    .collection("referralRewards").doc().id;

  await db.runTransaction(async (tx) => {
    // Mark childAdded on pendingReferral regardless of grade match.
    tx.set(refereeParentRef, {
      pendingReferral: { ...pendingReferral, childAddedAtMillis: now },
    }, { merge: true });

    // Stage 1 reward for inviter.
    tx.set(
      db.collection("parents").doc(inviterParentId)
        .collection("referralRewards").doc(rewardId),
      {
        id: rewardId,
        stage: 1,
        referredParentId: refereeParentId,
        referredParentName: refereeParentSnap.data()?.name || "",
        unlockedAtMillis: now,
        scratchedAtMillis: null,
        rewardLabel: rewardCfg.label,
        rewardValue: rewardCfg.value,
      }
    );

    if (matchedInviterChild) {
      // Reciprocal friend edges between the grade-matched children.
      const inviterChildRef = db.collection("parents").doc(inviterParentId)
        .collection("children").doc(matchedInviterChild.childId);
      const refereeChildRef = db.collection("parents").doc(refereeParentId)
        .collection("children").doc(refereeChildId);
      const [inviterChildSnap, refereeChildSnap] = await Promise.all([
        tx.get(inviterChildRef), tx.get(refereeChildRef),
      ]);
      if (inviterChildSnap.exists && refereeChildSnap.exists) {
        const ic = inviterChildSnap.data();
        const rc = refereeChildSnap.data();
        tx.set(inviterChildRef, {
          friends: FieldValue.arrayUnion({
            parentId: refereeParentId, childId: refereeChildId,
            name: rc.name || "", grade: rc.grade || 0,
            gender: rc.gender || "", addedAtMillis: now,
          }),
        }, { merge: true });
        tx.set(refereeChildRef, {
          friends: FieldValue.arrayUnion({
            parentId: inviterParentId, childId: matchedInviterChild.childId,
            name: ic.name || "", grade: ic.grade || 0,
            gender: ic.gender || "", addedAtMillis: now,
          }),
        }, { merge: true });
      }
    }
  });

  // FCM to inviter.
  try {
    const inviterParentSnap = await db.collection("parents").doc(inviterParentId).get();
    const token = inviterParentSnap.data()?.fcmToken;
    if (token) {
      await fcm.send({
        token,
        notification: {
          title: "🎁 Scratch card unlocked!",
          body: matchedInviterChild
            ? "Your referral joined and your kids are now study friends!"
            : "Your referral joined Eduksha — scratch your reward!",
        },
        data: { kind: "referral_stage1", role: "parent" },
        android: { priority: "high", notification: { channelId: "dobi_default" } },
      });
    }
  } catch (err) {
    logger.warn(`referral_stage1 push failed: ${err.message}`);
  }

  return { ok: true, status: matchedInviterChild ? "matched" : "no-grade-match" };
});

// ── recordReferralWeekActivity ─────────────────────────────────────────────
//
// Called by the referee's app once per session after 7+ days since
// childAddedAtMillis. Unlocks Stage 2 reward for the inviter and marks the
// referral complete so it won't be checked again.

exports.recordReferralWeekActivity = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in required");
  const refereeParentId = request.auth.uid;

  const refereeParentRef = db.collection("parents").doc(refereeParentId);
  const refereeParentSnap = await refereeParentRef.get();
  const pendingReferral = refereeParentSnap.data()?.pendingReferral;
  if (!pendingReferral?.inviterParentId) return { ok: true, status: "no-pending-referral" };
  if (pendingReferral.weekMilestoneDone) return { ok: true, status: "already-done" };
  if (!pendingReferral.childAddedAtMillis) return { ok: true, status: "child-not-added-yet" };
  if (Date.now() - pendingReferral.childAddedAtMillis < WEEK_MS) {
    return { ok: true, status: "too-early" };
  }

  const now = Date.now();
  const inviterParentId = pendingReferral.inviterParentId;
  const rewardCfg = PARENT_REFERRAL_REWARDS[2];
  const rewardId = db.collection("parents").doc(inviterParentId)
    .collection("referralRewards").doc().id;

  await db.runTransaction(async (tx) => {
    tx.set(refereeParentRef, {
      pendingReferral: { ...pendingReferral, weekMilestoneDone: true },
    }, { merge: true });
    tx.set(
      db.collection("parents").doc(inviterParentId)
        .collection("referralRewards").doc(rewardId),
      {
        id: rewardId,
        stage: 2,
        referredParentId: refereeParentId,
        referredParentName: refereeParentSnap.data()?.name || "",
        unlockedAtMillis: now,
        scratchedAtMillis: null,
        rewardLabel: rewardCfg.label,
        rewardValue: rewardCfg.value,
      }
    );
  });

  try {
    const inviterParentSnap = await db.collection("parents").doc(inviterParentId).get();
    const token = inviterParentSnap.data()?.fcmToken;
    if (token) {
      await fcm.send({
        token,
        notification: {
          title: "🏆 Week reward unlocked!",
          body: "Your referral has been active for a week — scratch your bonus reward!",
        },
        data: { kind: "referral_stage2", role: "parent" },
        android: { priority: "high", notification: { channelId: "dobi_default" } },
      });
    }
  } catch (err) {
    logger.warn(`referral_stage2 push failed: ${err.message}`);
  }
  return { ok: true, status: "unlocked" };
});

// ── scratchReward ──────────────────────────────────────────────────────────
//
// Marks a referral reward as scratched and returns the rewardValue.
// The value is intentionally only returned at scratch-time so it can't be
// read from the client before the user interacts.

exports.scratchReward = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in required");
  const parentId = request.auth.uid;
  const { rewardId } = request.data || {};
  if (!rewardId) throw new HttpsError("invalid-argument", "rewardId required");

  const rewardRef = db.collection("parents").doc(parentId)
    .collection("referralRewards").doc(rewardId);
  const snap = await rewardRef.get();
  if (!snap.exists) throw new HttpsError("not-found", "Reward not found");
  const reward = snap.data();
  if (!reward.scratchedAtMillis) {
    await rewardRef.update({ scratchedAtMillis: Date.now() });
  }
  return { rewardValue: reward.rewardValue, rewardLabel: reward.rewardLabel };
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
  { timeoutSeconds: 60 },
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
  const ai = new GoogleGenAI({ vertexai: true, project: "dobi-ac085", location: "us-central1" });

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

// ── Account deletion ───────────────────────────────────────────────────────
//
// deleteAccount — caller must be authenticated; we use auth.uid as the
// parentId. Wipes every Firestore doc and Storage object connected to the
// parent and their children, then deletes the Auth user. Required by the
// Google Play User Data policy.
//
// Order matters: Firestore + Storage first (we need the auth token to be
// valid for cross-collection queries), Auth user last.

// ── Generate MCQ quiz questions via Gemini ─────────────────────────────────────

exports.generateMcq = onCall(
  { timeoutSeconds: 60 },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in required");
    }
    const { conceptId, conceptName, conceptDescription, subject, grade, unitName, targetCount = 10 } = request.data || {};
    if (!subject || !unitName) {
      throw new HttpsError("invalid-argument", "subject and unitName required");
    }

    const ai = new GoogleGenAI({ vertexai: true, project: "dobi-ac085", location: "us-central1" });

    const systemInstruction = `You are an Indian school-textbook tutor. The user message names a
specific lesson or concept from the NCERT / CBSE syllabus. Your job
is to write multiple-choice questions that match the difficulty and
tone of the CBSE textbook for that grade and subject.

Rules:
- Write exactly the number of questions the user asks for.
- 4 options per question, exactly one correct.
- Wrong options must be plausible misconceptions a real student that
  age would have — not random unrelated text.
- Cover the topic broadly (recall, understanding, application).
- Each question carries a one-line kid-friendly explanation.
- Do NOT invent facts that wouldn't appear in a Class 6 / 7 NCERT
  textbook for this subject.
- Output ONLY valid JSON with a "questions" array where each item has:
  "id", "text", "options" (array of 4 strings), "correctIndex" (0-based integer), "explanation".`;

    const scope = conceptName
      ? `Lesson: ${unitName}\nConcept (focus narrowly on this): ${conceptName}${conceptDescription ? `\nConcept description: ${conceptDescription}` : ""}`
      : `Lesson: ${unitName}`;

    const userPrompt = `You are an Indian school-textbook tutor. Write ${targetCount} NCERT-style
multiple-choice questions for a Class ${grade || 6} ${subject} student about the
topic below. The questions should match the difficulty and tone of the
CBSE textbook for this grade.

${scope}

Rules:
- Exactly ${targetCount} questions, each with 4 options.
- One option clearly correct; wrong options must be plausible
  misconceptions a real student that age would have — not random text.
- Cover the topic broadly: include some recall, some understanding,
  some application questions.
- Each question carries a one-line kid-friendly explanation.
- Output JSON only: { "questions": [ { "id": "q1", "text": "...", "options": ["A","B","C","D"], "correctIndex": 0, "explanation": "..." } ] }`;

    let raw;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        systemInstruction,
        contents: [{ role: "user", parts: [{ text: userPrompt }] }],
        config: {
          temperature: 0.3,
          responseMimeType: "application/json",
          responseSchema: {
            type: "object",
            properties: {
              questions: {
                type: "array",
                items: {
                  type: "object",
                  properties: {
                    id:           { type: "string" },
                    text:         { type: "string" },
                    options:      { type: "array", items: { type: "string" } },
                    correctIndex: { type: "integer" },
                    explanation:  { type: "string" },
                  },
                },
              },
            },
          },
        },
      });
      raw = (response.text || "").trim();
    } catch (err) {
      logger.error("generateMcq: Gemini error", { message: err.message, stack: err.stack, status: err.status });
      throw new HttpsError("unavailable", "Question generation failed. Please try again.");
    }

    const jsonMatch = raw.match(/\{[\s\S]*\}/);
    if (!jsonMatch) throw new HttpsError("internal", "Invalid response from AI");
    let parsed;
    try {
      parsed = JSON.parse(jsonMatch[0]);
    } catch (err) {
      throw new HttpsError("internal", "Could not parse AI response");
    }
    const questions = (parsed.questions || []).filter(
      (q) => q.text && Array.isArray(q.options) && q.options.length === 4
             && typeof q.correctIndex === "number" && q.correctIndex >= 0 && q.correctIndex <= 3
    );
    if (questions.length === 0) {
      throw new HttpsError("internal", "No valid questions generated");
    }
    return { questions };
  }
);

// ── Delete account ──────────────────────────────────────────────────────────────

exports.deleteAccount = onCall({ timeoutSeconds: 540 }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const parentId = request.auth.uid;
  logger.info(`deleteAccount: starting for parent=${parentId}`);

  // 1. Discover the parent's children
  const childrenSnap = await db.collection("parents").doc(parentId)
    .collection("children").get();
  const childIds = childrenSnap.docs.map((d) => d.id);
  logger.info(`deleteAccount: found ${childIds.length} children`);

  // 2. Delete top-level docs keyed by parentId
  await deleteByQuery(
    db.collection("parentTaskRequests").where("parentId", "==", parentId)
  );
  await deleteByQuery(
    db.collection("invites").where("inviterParentId", "==", parentId)
  );

  // 3. Per-child cleanup
  for (const childId of childIds) {
    // Top-level child-keyed collections
    await deleteByQuery(
      db.collection("subjective_results").where("childId", "==", childId)
    );
    await deleteByQuery(
      db.collection("uploads").where("childId", "==", childId)
    );
    await deleteByQuery(
      db.collection("homeworks").where("childId", "==", childId)
    );

    // quiz_attempts/{childId}/{units|concepts}/* — nested subcollections
    const quizDocRef = db.collection("quiz_attempts").doc(childId);
    for (const sub of ["units", "concepts"]) {
      const snap = await quizDocRef.collection(sub).get();
      await Promise.all(snap.docs.map((d) => d.ref.delete()));
    }
    await quizDocRef.delete().catch(() => { /* doc may not exist */ });

    // Storage objects under the child
    try {
      const bucket = getStorage().bucket();
      await bucket.deleteFiles({ prefix: `uploads/${childId}/` });
      await bucket.deleteFiles({ prefix: `homework_photos/${childId}/` });
    } catch (err) {
      logger.warn(
        `deleteAccount: storage cleanup failed for child=${childId}: ${err.message}`
      );
    }

    // Drop the child profile last
    await db.collection("parents").doc(parentId)
      .collection("children").doc(childId).delete();
  }

  // 4. The parent record itself
  await db.collection("parents").doc(parentId).delete();

  // 5. The Auth user — last, because deleting it invalidates the token.
  try {
    await getAuth().deleteUser(parentId);
  } catch (err) {
    logger.warn(`deleteAccount: deleteUser failed: ${err.message}`);
  }

  logger.info(`deleteAccount: finished for parent=${parentId}`);
  return { ok: true, childrenDeleted: childIds.length };
});

async function deleteByQuery(query) {
  const snap = await query.get();
  if (snap.empty) return;
  // Batch in chunks of 400 (Firestore batch ceiling is 500).
  const chunks = [];
  for (let i = 0; i < snap.docs.length; i += 400) {
    chunks.push(snap.docs.slice(i, i + 400));
  }
  for (const chunk of chunks) {
    const batch = db.batch();
    chunk.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }
}

// ── Waitlist → Google Sheet sync ────────────────────────────────
// Mirrors every new `waitlist/{docId}` write into a Google Sheet so the
// marketing team has a live view of signups. The sheet ID is hard-coded
// below — change SHEET_ID and redeploy if you switch sheets.
//
// Setup once:
//   1. Create a Google Sheet
//   2. Share it (Editor) with: dobi-ac085@appspot.gserviceaccount.com
//   3. Enable the Sheets API at:
//        https://console.cloud.google.com/apis/library/sheets.googleapis.com?project=dobi-ac085
//   4. Set SHEET_ID below to the sheet's ID (the long string between /d/ and /edit in the URL)
//   5. Deploy: firebase deploy --only functions:mirrorWaitlistToSheet
const WAITLIST_SHEET_ID = "13P9xZ8LnIoCiZw-I-FseZs7yw957rbekrqsSQ0WbU3U";
const WAITLIST_SHEET_RANGE = "Sheet1!A:H"; // columns: time | name | phone | email | class | locale | source | docId

exports.mirrorWaitlistToSheet = onDocumentCreated(
  "waitlist/{docId}",
  async (event) => {
    if (!WAITLIST_SHEET_ID || WAITLIST_SHEET_ID === "REPLACE_WITH_SHEET_ID") {
      logger.warn("mirrorWaitlistToSheet: SHEET_ID not configured; skipping");
      return;
    }

    const data = event.data?.data();
    if (!data) return;

    const docId = event.params.docId;
    const createdAt =
      data.createdAt?.toDate?.()?.toISOString?.() || new Date().toISOString();

    const row = [
      createdAt,
      data.parentName || "",
      data.phone || "",
      data.email || "",
      data.childClass || "",
      data.locale || "",
      data.source || "",
      docId,
    ];

    try {
      const auth = new google.auth.GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/spreadsheets"],
      });
      const sheets = google.sheets({ version: "v4", auth });
      await sheets.spreadsheets.values.append({
        spreadsheetId: WAITLIST_SHEET_ID,
        range: WAITLIST_SHEET_RANGE,
        valueInputOption: "RAW",
        insertDataOption: "INSERT_ROWS",
        requestBody: { values: [row] },
      });
      logger.info(`mirrorWaitlistToSheet: appended ${docId} (phone=${data.phone || "—"})`);
    } catch (err) {
      logger.error(`mirrorWaitlistToSheet: failed for ${docId}: ${err.message}`, err);
    }
  }
);

// ── generateSubjectiveQuestions ───────────────────────────────────────────────
// Generates short subjective questions for a given topic scope (text-only).
// Input:  { scopeText, subject, grade, targetCount? }
// Output: { questions: [{ id, text, expected_keywords[], max_marks }] }
exports.generateSubjectiveQuestions = onCall(
  { timeoutSeconds: 60 },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in required");
    }
    const { scopeText, subject, grade = 6, targetCount = 5 } = request.data || {};
    if (!scopeText || !subject) {
      throw new HttpsError("invalid-argument", "scopeText and subject required");
    }

    logger.info(`generateSubjectiveQuestions: subject=${subject} grade=${grade} count=${targetCount}`);

    const ai = new GoogleGenAI({ vertexai: true, project: "dobi-ac085", location: "us-central1" });

    const systemInstruction =
      "You are an Indian school-textbook tutor. Generate short subjective " +
      "questions for the named lesson(s)/concept(s) at the named grade level. " +
      "Avoid pure recall; prefer questions that ask the student to explain, " +
      "compare, give an example, or apply the idea. " +
      "Output ONLY valid JSON: { \"questions\": [ { \"id\": \"q1\", \"text\": \"...\", " +
      "\"expected_keywords\": [\"...\"], \"max_marks\": 2 } ] }. " +
      "If the topic input doesn't make sense, return { \"error\": \"<reason>\" }.";

    const userPrompt =
      `${scopeText}\nSubject: ${subject}\nGrade: ${grade}\n\n` +
      `Generate exactly ${targetCount} short subjective questions covering the topic(s) above. ` +
      `Mix difficulty: roughly half short-answer (1–2 marks) and half slightly deeper (3 marks). ` +
      `Avoid pure recall — prefer "explain", "why", "how", "give an example". ` +
      `Output JSON only: { "questions": [ { "id": "q1", "text": "...", "expected_keywords": ["..."], "max_marks": 2 } ] }`;

    let raw;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: userPrompt,
        config: {
          temperature: 0.15,
          systemInstruction,
        },
      });
      raw = (response.text || "").trim();
    } catch (err) {
      logger.error("generateSubjectiveQuestions: Gemini error", err);
      throw new HttpsError("unavailable", "Question generation failed. Please try again.");
    }

    const jsonMatch = raw.match(/\{[\s\S]*\}/);
    if (!jsonMatch) throw new HttpsError("internal", "Invalid response from AI");
    let parsed;
    try {
      parsed = JSON.parse(jsonMatch[0]);
    } catch (_) {
      throw new HttpsError("internal", "Could not parse AI response");
    }
    if (parsed.error) throw new HttpsError("internal", parsed.error);

    const questions = (parsed.questions || [])
      .filter((q) => q.text)
      .map((q, i) => ({
        id: q.id || `q${i + 1}`,
        text: q.text,
        expected_keywords: Array.isArray(q.expected_keywords) ? q.expected_keywords : [],
        max_marks: typeof q.max_marks === "number" ? q.max_marks : 2,
      }));

    if (questions.length === 0) {
      throw new HttpsError("internal", "No valid questions generated");
    }
    return { questions };
  }
);

// ── gradeNotebookAnswers ───────────────────────────────────────────────────────
// Grades a child's freeform notebook photos against a topic scope.
// Uses Vertex AI + gemini-2.5-flash (same as validateImageScope).
// Input:  { scopeText, subject, grade, images: [base64jpeg] }
// Output: { is_on_topic_overall, off_topic_reason, grades: [...] }
exports.gradeNotebookAnswers = onCall(
  { region: "us-central1", timeoutSeconds: 120, memory: "512MiB", invoker: "public" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Must be signed in.");
    }

    const { scopeText = "", subject = "", grade = 6, images = [] } = request.data;
    if (!images.length) {
      throw new HttpsError("invalid-argument", "No images provided.");
    }
    if (!scopeText || !subject) {
      throw new HttpsError("invalid-argument", "scopeText and subject required");
    }

    logger.info(`gradeNotebookAnswers: subject=${subject} grade=${grade} images=${images.length}`);

    const ai = new GoogleGenAI({
      vertexai: true,
      project: "dobi-ac085",
      location: "us-central1",
    });

    const systemPrompt =
      "You are an Indian school-textbook tutor reviewing a child's handwritten answers. " +
      "The lesson/concept(s) being revised are given in the user message.\n\n" +
      "STEP 1 — Decide if the uploaded photos are about the named topic(s).\n" +
      "  (a) on-topic — proceed\n" +
      "  (b) different chapter — return is_on_topic_overall=false with off_topic_reason naming topic + what you saw\n" +
      "  (c) mixed paper/mock test — return is_on_topic_overall=false (mention 'mixed paper')\n" +
      "  (d) not a textbook/notebook page — return is_on_topic_overall=false (describe what you see)\n" +
      "  (e) unreadable — return is_on_topic_overall=false ('the writing is too faint to read')\n" +
      "Return grades=[] in any refusal case.\n\n" +
      "STEP 2 — When on-topic, GRADE each answer:\n" +
      "  - question_id: unique string per detected answer\n" +
      "  - question_seen: short snippet of the question the student wrote (or inferred)\n" +
      "  - answer_seen: short snippet of the student's answer\n" +
      "  - is_attempted: true if student wrote anything\n" +
      "  - is_on_topic: true if the answer is about the question\n" +
      "  - score: 0.0–1.0 based on correctness + completeness\n" +
      "  - is_correct: true iff score >= 0.6\n" +
      "  - soft_verdict: 5–10 kid-friendly words ('Spot on!', 'Good try!')\n" +
      "  - strict_feedback: 1 sentence honest assessment for parent dashboard\n\n" +
      "Output ONLY valid JSON.";

    const userPromptText =
      `${scopeText}\nSubject: ${subject}  Grade: ${grade}\n\n` +
      "MODE: freeform-notebook. The student practised in their own notebook and uploaded photo(s) of their answers. " +
      "Find each answer the student wrote, infer what question they were answering (or what concept the answer covers), and grade it. " +
      "Return one entry per detected answer in the 'grades' array.\n\n" +
      "If the notebook page is NOT about the topic(s) above, set is_on_topic_overall=false, give a short off_topic_reason, and return grades=[].";

    const parts = [
      { text: userPromptText },
      ...images.map((b64) => ({
        inlineData: { mimeType: "image/jpeg", data: b64 },
      })),
    ];

    let rawText;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        systemInstruction: systemPrompt,
        contents: [{ role: "user", parts }],
        config: {
          temperature: 0.15,
          responseMimeType: "application/json",
          responseSchema: {
            type: "object",
            properties: {
              is_on_topic_overall: { type: "boolean" },
              off_topic_reason:    { type: "string" },
              grades: {
                type: "array",
                items: {
                  type: "object",
                  properties: {
                    question_id:      { type: "string" },
                    question_seen:    { type: "string" },
                    answer_seen:      { type: "string" },
                    is_attempted:     { type: "boolean" },
                    is_on_topic:      { type: "boolean" },
                    score:            { type: "number" },
                    is_correct:       { type: "boolean" },
                    soft_verdict:     { type: "string" },
                    strict_feedback:  { type: "string" },
                  },
                },
              },
            },
            required: ["is_on_topic_overall", "grades"],
          },
        },
      });
      rawText = response.text;
      logger.info(`gradeNotebookAnswers: rawText=${rawText}`);
    } catch (err) {
      logger.error(`gradeNotebookAnswers: model error: ${err.message}`, err);
      throw new HttpsError("internal", `Model error: ${err.message}`);
    }

    try {
      const verdict = JSON.parse(rawText);
      if (verdict.is_on_topic_overall === false) {
        return {
          is_on_topic_overall: false,
          off_topic_reason: verdict.off_topic_reason || "",
          grades: [],
        };
      }
      const grades = (verdict.grades || []).map((g) => ({
        question_id:     g.question_id     || String(Math.random()),
        question_seen:   g.question_seen   || "",
        answer_seen:     g.answer_seen     || "",
        is_attempted:    g.is_attempted    === true,
        is_on_topic:     g.is_on_topic     !== false,
        score:           Math.min(1, Math.max(0, Number(g.score) || 0)),
        is_correct:      g.is_correct      === true,
        soft_verdict:    g.soft_verdict    || "",
        strict_feedback: g.strict_feedback || "",
      }));
      return { is_on_topic_overall: true, off_topic_reason: "", grades };
    } catch (_) {
      logger.warn(`gradeNotebookAnswers: could not parse: ${rawText}`);
      throw new HttpsError("internal", "Could not parse AI grading response");
    }
  }
);

// ── generateMcqFromImage ───────────────────────────────────────────────────────
// Generates MCQ questions from uploaded textbook/notes photos.
// Mirrors Android's McqGenerationRepositoryImpl.generate() exactly.
// Input:  { scopeText, subject, grade, images: [base64jpeg],
//           targetCount?, bloomMix?: {recall, understand, apply, analyze} }
// Output: { questions: [...] } OR { error: "kid-friendly refusal" }
exports.generateMcqFromImage = onCall(
  { region: "us-central1", timeoutSeconds: 120, memory: "512MiB", invoker: "public" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Must be signed in.");
    }

    const {
      scopeText = "",
      subject = "",
      grade = 6,
      images = [],
      targetCount = 8,
      bloomMix = { recall: 2, understand: 2, apply: 2, analyze: 2 },
    } = request.data;

    if (!images.length) {
      throw new HttpsError("invalid-argument", "No images provided.");
    }
    if (!scopeText || !subject) {
      throw new HttpsError("invalid-argument", "scopeText and subject required");
    }

    logger.info(`generateMcqFromImage: subject=${subject} grade=${grade} images=${images.length} count=${targetCount}`);

    const ai = new GoogleGenAI({
      vertexai: true,
      project: "dobi-ac085",
      location: "us-central1",
    });

    // Exact Android SYSTEM_PROMPT — 4-step classification
    const systemPrompt =
      "You are an Indian school-textbook tutor. A student has uploaded photos of\n" +
      "their textbook pages. The student has ALREADY told you which lesson /\n" +
      "concept(s) they want to revise (provided in the user message). Your job\n" +
      "is to generate multiple-choice questions ONLY when the uploaded content\n" +
      "actually belongs to those exact topics.\n\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "STEP 1 — CLASSIFY EACH PHOTO INDIVIDUALLY (you must do this first)\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "The user may upload one OR multiple photos. Number them 1, 2, 3 … in\n" +
      "the order they were sent. For EACH photo independently, decide which\n" +
      "bucket it falls into:\n" +
      "  (a) A textbook chapter / notes from a SINGLE chapter that matches the\n" +
      "      named lesson/concept.\n" +
      "  (b) A textbook chapter / notes from a SINGLE chapter that is NOT the\n" +
      "      named lesson/concept (a different chapter from the same subject).\n" +
      "  (c) A mixed multi-chapter document — e.g. a sample paper, mock test,\n" +
      "      exam paper, question bank, or revision worksheet that pulls\n" +
      "      questions from many different chapters at once.\n" +
      "  (d) Not from a school textbook at all (random photo, screenshot,\n" +
      "      selfie, handwritten notes from another subject).\n" +
      "  (e) Unreadable / blurry / torn.\n\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "STEP 2 — DECIDE FOR THE WHOLE BATCH\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "✅ ONLY when EVERY photo is (a) → proceed to generation.\n" +
      "❌ If ANY photo is (b), (c), (d), or (e) → REFUSE THE ENTIRE BATCH.\n" +
      "  Do NOT silently use only the matching photos and ignore the rest.\n\n" +
      "Critically: case (c) — mixed sample papers / mock tests / question banks\n" +
      "— MUST be refused even if a couple of the questions on the page happen\n" +
      "to mention something related to the named lesson. When in doubt between\n" +
      "(a) and (c), choose (c) and refuse.\n\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "STEP 3 — REFUSAL FORMAT\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "When refusing, return ONLY:\n" +
      '  {"error": "<one short kid-friendly sentence>"}\n' +
      "with no questions.\n\n" +
      "For SINGLE-photo uploads, the sentence MUST mention the lesson the\n" +
      "student picked AND describe what you actually saw.\n" +
      "For MULTI-photo uploads where SOME match and SOME don't, name the\n" +
      "offending photo by 1-based index and say what it actually is.\n\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "STEP 4 — GENERATION (only when STEP 2 said proceed)\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "- Generate exactly the count the user asked for.\n" +
      "- 4 options per question.\n" +
      "- Wrong options must reflect plausible misconceptions.\n" +
      "- Do NOT invent facts that aren't visible on the uploaded page.\n" +
      "- Each question carries a one-line explanation AND a verbatim\n" +
      "  source_quote copied EXACTLY from the page.\n\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      "OUTPUT\n" +
      "────────────────────────────────────────────────────────────────────────\n" +
      'Output only valid JSON. Either "questions" with all required fields,\n' +
      'OR "error" — never both, never neither.';

    // Exact Android buildUserPrompt() format
    const userPromptText =
      `${scopeText}\nSubject: ${subject}\nGrade: ${grade}\n\n` +
      "Apply the 4-step procedure from the system prompt now:\n" +
      "1. Classify EACH photo independently as (a/b/c/d/e). Number them in\n" +
      "   upload order: photo 1, photo 2, …\n" +
      "2. Decide for the whole batch. Generate ONLY if EVERY photo is (a).\n" +
      "   If even one photo is (b)/(c)/(d)/(e), refuse the entire batch and\n" +
      "   name the offending photo(s) by index — do not silently proceed using\n" +
      "   only the matching photos.\n" +
      "3. If refusing, mention the topic(s) above by name, list which photo(s)\n" +
      "   are off-topic by 1-based index, and describe what each off-topic\n" +
      "   photo actually is.\n" +
      `4. Only when every photo is (a), generate exactly ${targetCount}\n` +
      `   questions with Bloom mix recall=${bloomMix.recall || 2},\n` +
      `   understand=${bloomMix.understand || 2}, apply=${bloomMix.apply || 2},\n` +
      `   analyze=${bloomMix.analyze || 2}.\n\n` +
      "Reminder: a page that mixes Q&A from many chapters is case (c) and must\n" +
      "be refused, even if one or two of its items happen to relate to the\n" +
      "topic(s) above. Default to refusing when uncertain.";

    const parts = [
      { text: userPromptText },
      ...images.map((b64) => ({
        inlineData: { mimeType: "image/jpeg", data: b64 },
      })),
    ];

    let rawText;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        systemInstruction: systemPrompt,
        contents: [{ role: "user", parts }],
        config: {
          temperature: 0.15,
          responseMimeType: "application/json",
          responseSchema: {
            type: "object",
            properties: {
              questions: {
                type: "array",
                items: {
                  type: "object",
                  properties: {
                    text:         { type: "string" },
                    options:      { type: "array", items: { type: "string" } },
                    correct_index: { type: "integer" },
                    explanation:  { type: "string" },
                    bloom_level:  { type: "string" },
                    source_quote: { type: "string" },
                  },
                },
              },
              error: { type: "string" },
            },
          },
        },
      });
      rawText = response.text;
      logger.info(`generateMcqFromImage: rawText=${rawText?.slice(0, 200)}`);
    } catch (err) {
      logger.error(`generateMcqFromImage: model error: ${err.message}`, err);
      throw new HttpsError("internal", `Model error: ${err.message}`);
    }

    try {
      const parsed = JSON.parse(rawText);
      if (parsed.error) {
        // Model refused (off-topic/random photo) — surface as HttpsError so iOS
        // can display the kid-friendly message as uploadError inline.
        throw new HttpsError("failed-precondition", parsed.error);
      }
      const questions = (parsed.questions || [])
        .filter((q) => q.text && Array.isArray(q.options) && q.options.length === 4)
        .map((q) => ({
          text:         q.text,
          options:      q.options,
          correct_index: typeof q.correct_index === "number" ? q.correct_index : 0,
          explanation:  q.source_quote
            ? `${q.explanation || ""}\n\nFrom your page: "${q.source_quote}"`
            : (q.explanation || ""),
          bloom_level:  q.bloom_level || "",
          source_quote: q.source_quote || "",
        }));
      if (questions.length === 0) {
        throw new HttpsError("internal", "No valid questions generated. Please try again.");
      }
      return { questions };
    } catch (err) {
      if (err.code) throw err; // re-throw HttpsError
      logger.warn(`generateMcqFromImage: could not parse: ${rawText}`);
      throw new HttpsError("internal", "Could not parse AI response");
    }
  }
);

// ── gradeStructuredAnswers ────────────────────────────────────────────────────
// Grades per-question handwritten answers with their photos.
// Mirrors Android's WriteAnswerRepositoryImpl.gradeStructured().
// Input:  { scopeText, subject, grade,
//           questions: [{id, text, max_marks}],
//           photosByQuestionId: { [qId]: [base64jpeg] } }
// Output: { is_on_topic_overall, off_topic_reason, grades: [...] }
exports.gradeStructuredAnswers = onCall(
  { region: "us-central1", timeoutSeconds: 120, memory: "512MiB", invoker: "public" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Must be signed in.");
    }

    const {
      scopeText = "",
      subject = "",
      grade = 6,
      questions = [],
      photosByQuestionId = {},
    } = request.data;

    if (!scopeText || !subject) {
      throw new HttpsError("invalid-argument", "scopeText and subject required");
    }
    if (!questions.length) {
      throw new HttpsError("invalid-argument", "No questions provided");
    }

    logger.info(`gradeStructuredAnswers: subject=${subject} grade=${grade} questions=${questions.length}`);

    const ai = new GoogleGenAI({
      vertexai: true,
      project: "dobi-ac085",
      location: "us-central1",
    });

    const systemPrompt =
      "You are an Indian school-textbook tutor reviewing a child's handwritten " +
      "answers to subjective questions. The lesson/concept(s) being revised " +
      "are given in the user message.\n\n" +
      "STEP 1 — Decide if the uploaded photos are about the named topic(s).\n" +
      "  (a) on-topic single chapter — proceed\n" +
      "  (b) different chapter — return is_on_topic_overall=false with off_topic_reason\n" +
      "  (c) mixed paper/mock test — return is_on_topic_overall=false (mention 'mixed paper')\n" +
      "  (d) not a textbook/notebook page — return is_on_topic_overall=false (describe what you see)\n" +
      "  (e) unreadable — return is_on_topic_overall=false ('the writing is too faint to read')\n" +
      "Return grades=[] in any refusal case.\n\n" +
      "STEP 2 — When on-topic, GRADE each answer:\n" +
      "  - question_id: use the id field from the question header\n" +
      "  - question_seen: short verbatim snippet of the question\n" +
      "  - answer_seen: short verbatim snippet of student's answer\n" +
      "  - is_attempted: true if student wrote anything at all\n" +
      "  - is_on_topic: true if the answer addresses the question\n" +
      "  - score: 0.0–1.0 based on correctness + completeness\n" +
      "  - is_correct: true iff score >= 0.6\n" +
      "  - soft_verdict: 5–10 kid-friendly words ('Spot on!', 'Almost there!')\n" +
      "  - strict_feedback: 1 sentence honest assessment for parent dashboard\n" +
      "Be strict on correctness, soft on tone. Output only valid JSON.";

    // Build interleaved parts matching Android's gradeStructured() prompt exactly.
    const parts = [];
    let sb =
      `${scopeText}\nSubject: ${subject}  Grade: ${grade}\n\n` +
      `MODE: structured. Below are ${questions.length} questions.\n` +
      "For each question I'll show the question, then the student's " +
      "answer photo(s). Grade each one. If a question has no photo, mark " +
      "it is_attempted=false.\n";

    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      sb += `\nQ${i + 1} (id=${q.id}, max_marks=${q.max_marks || 2}):\n${q.text}\n`;
      const photos = photosByQuestionId[q.id] || [];
      if (photos.length === 0) {
        sb += "[No photo uploaded — mark is_attempted=false.]\n";
      } else {
        sb += `[Student answer photo(s) for Q${i + 1} follow.]\n`;
        for (const b64 of photos) {
          parts.push({ text: sb });
          sb = "";
          parts.push({ inlineData: { mimeType: "image/jpeg", data: b64 } });
        }
      }
    }
    if (sb.length > 0) parts.push({ text: sb });

    let rawText;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        systemInstruction: systemPrompt,
        contents: [{ role: "user", parts }],
        config: {
          temperature: 0.15,
          responseMimeType: "application/json",
          responseSchema: {
            type: "object",
            properties: {
              is_on_topic_overall: { type: "boolean" },
              off_topic_reason:    { type: "string" },
              grades: {
                type: "array",
                items: {
                  type: "object",
                  properties: {
                    question_id:      { type: "string" },
                    question_seen:    { type: "string" },
                    answer_seen:      { type: "string" },
                    is_attempted:     { type: "boolean" },
                    is_on_topic:      { type: "boolean" },
                    score:            { type: "number" },
                    is_correct:       { type: "boolean" },
                    soft_verdict:     { type: "string" },
                    strict_feedback:  { type: "string" },
                  },
                },
              },
            },
            required: ["is_on_topic_overall", "grades"],
          },
        },
      });
      rawText = response.text;
      logger.info(`gradeStructuredAnswers: rawText=${rawText}`);
    } catch (err) {
      logger.error(`gradeStructuredAnswers: model error: ${err.message}`, err);
      throw new HttpsError("internal", `Model error: ${err.message}`);
    }

    try {
      const verdict = JSON.parse(rawText);
      if (verdict.is_on_topic_overall === false) {
        return {
          is_on_topic_overall: false,
          off_topic_reason: verdict.off_topic_reason || "",
          grades: [],
        };
      }
      const grades = (verdict.grades || []).map((g) => ({
        question_id:     g.question_id     || "",
        question_seen:   g.question_seen   || "",
        answer_seen:     g.answer_seen     || "",
        is_attempted:    g.is_attempted    === true,
        is_on_topic:     g.is_on_topic     !== false,
        score:           Math.min(1, Math.max(0, Number(g.score) || 0)),
        is_correct:      g.is_correct      === true,
        soft_verdict:    g.soft_verdict    || "",
        strict_feedback: g.strict_feedback || "",
      }));
      // Fill in any questions that had no photo as not-attempted (matches Android).
      const gradeMap = Object.fromEntries(grades.map((g) => [g.question_id, g]));
      const finalGrades = questions.map((q) =>
        gradeMap[q.id] || {
          question_id:     q.id,
          question_seen:   q.text || "",
          answer_seen:     "",
          is_attempted:    false,
          is_on_topic:     true,
          score:           0,
          is_correct:      false,
          soft_verdict:    "Not yet answered",
          strict_feedback: "No answer photo uploaded.",
        }
      );
      return { is_on_topic_overall: true, off_topic_reason: "", grades: finalGrades };
    } catch (_) {
      logger.warn(`gradeStructuredAnswers: could not parse: ${rawText}`);
      throw new HttpsError("internal", "Could not parse AI grading response");
    }
  }
);

// ── validateImageScope ─────────────────────────────────────────────────────────
// Callable that mirrors Android's ImageScopeValidator.
// Runs on Google Cloud with Application Default Credentials → Vertex AI access.
// iOS calls this via FirebaseFunctions SDK (no API key needed on client).
exports.validateImageScope = onCall(
  { region: "us-central1", timeoutSeconds: 120, memory: "512MiB", invoker: "public" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Must be signed in.");
    }

    const {
      subject = "",
      grade = 6,
      topic = "",
      lessonContext = "",
      artefactLabel = "class work",
      images = [],
    } = request.data;

    if (!images.length) {
      throw new HttpsError("invalid-argument", "No images provided.");
    }

    logger.info(`validateImageScope: subject=${subject} grade=${grade} topic=${topic} images=${images.length}`);

    const ai = new GoogleGenAI({
      vertexai: true,
      project: "dobi-ac085",
      location: "us-central1",
    });

    const systemPrompt =
      "You are an Indian school-textbook tutor verifying whether a child's " +
      "uploaded photos belong to a stated topic. You only output JSON. " +
      "Be strict — if even one photo is off-topic, not school work, or " +
      "unreadable, refuse the whole batch with a kid-friendly reason that " +
      "names the topic and says what you saw.";

    const ctx =
      lessonContext && lessonContext !== topic
        ? `Lesson: ${lessonContext}\nTopic: ${topic}`
        : `Topic: ${topic}`;
    const cap = artefactLabel.charAt(0).toUpperCase() + artefactLabel.slice(1);

    const userPrompt =
      `A student in India says they uploaded photos of their ${artefactLabel} ` +
      `for Class ${grade} ${subject}.\n${ctx}\n\n` +
      `Decide whether EVERY photo actually shows ${artefactLabel} for ` +
      `this topic. ${cap} can be handwritten notes, worksheets, or ` +
      `textbook pages — anything educational. It is NOT random objects, ` +
      `screenshots, selfies, or pages from a totally different chapter.\n\n` +
      `Reply with ONLY this JSON:\n` +
      `  { "ok": true } — when every photo fits\n` +
      `  { "ok": false, "reason": "<one short kid-friendly sentence>" }\n\n` +
      `The reason MUST mention the topic by name and describe what you saw ` +
      `in the off-topic photo (e.g. "This looks like a purse, not ${subject} ` +
      `class work for '${topic}'."). Be strict. If you're unsure, refuse.`;

    const parts = [
      { text: userPrompt },
      ...images.map((b64) => ({
        inlineData: { mimeType: "image/jpeg", data: b64 },
      })),
    ];

    let rawText;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash-lite",
        systemInstruction: systemPrompt,
        contents: [{ role: "user", parts }],
        config: {
          temperature: 0,
          responseMimeType: "application/json",
          responseSchema: {
            type: "object",
            properties: {
              ok:     { type: "boolean" },
              reason: { type: "string" },
            },
            required: ["ok"],
          },
        },
      });
      rawText = response.text;
      logger.info(`validateImageScope: rawText=${rawText}`);
    } catch (err) {
      logger.error(`validateImageScope: model error: ${err.message}`, err);
      throw new HttpsError("internal", `Model error: ${err.message}`);
    }

    try {
      const verdict = JSON.parse(rawText);
      return { ok: verdict.ok === true, reason: verdict.reason || "" };
    } catch (_) {
      logger.warn(`validateImageScope: could not parse: ${rawText}`);
      return { ok: false, reason: "" };
    }
  }
);

// ─────────────────────────────────────────────────────────────────────────────
// RAZORPAY SUBSCRIPTION FUNCTIONS
// ─────────────────────────────────────────────────────────────────────────────

const { onRequest } = require("firebase-functions/v2/https");
const crypto = require("crypto");

const PLAN_MONTHLY    = "plan_SzRWiVex69GbLB"; // ₹149/month
const PLAN_QUARTERLY  = "plan_SzRWik76iUw6lz"; // ₹249/3 months
const PLAN_HALFYEARLY = "plan_SzRWiyEVfEO6Iu"; // ₹449/6 months
const PLAN_TRIAL      = "plan_SzRqTUAERvbITe"; // ₹1 trial (1 billing cycle, then webhook creates monthly)

// ── createSubscription ────────────────────────────────────────────────────────
// Called by the app after the user picks a plan. Creates a Razorpay subscription
// and returns the subscription_id + key_id to open the Razorpay checkout SDK.
//
// Request body: { planType: "trial" | "monthly" | "quarterly" | "halfyearly", userId: string }
// Response:     { subscriptionId, keyId }
exports.createSubscription = onCall(
  { region: "asia-south1", secrets: [RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET] },
  async (request) => {
    const { planType, userId } = request.data;
    if (!userId) throw new HttpsError("unauthenticated", "userId required");
    if (!["trial", "monthly", "quarterly", "halfyearly"].includes(planType)) {
      throw new HttpsError("invalid-argument", "invalid planType");
    }

    const rzp = new Razorpay({
      key_id:     RAZORPAY_KEY_ID.value(),
      key_secret: RAZORPAY_KEY_SECRET.value(),
    });

    const isTrial = planType === "trial";
    // Trial uses a dedicated ₹1 plan (1 billing cycle).
    // After the cycle completes, the webhook creates the ₹149/month subscription.
    const planId  = isTrial            ? PLAN_TRIAL
                  : planType === "quarterly"  ? PLAN_QUARTERLY
                  : planType === "halfyearly" ? PLAN_HALFYEARLY
                  : PLAN_MONTHLY;

    // Run Firebase Auth lookup and Firestore customer-ID read in parallel
    const userDocRef = db.collection("subscriptions").doc(userId);
    const [userRecord, userDoc] = await Promise.all([
      getAuth().getUser(userId),
      userDocRef.get(),
    ]);
    const phone = userRecord.phoneNumber ?? "";
    const email = `${userId}@eduksha.app`;
    let razorpayCustomerId = userDoc.exists ? userDoc.data()?.razorpayCustomerId : null;

    if (!razorpayCustomerId) {
      try {
        const customer = await rzp.customers.create({
          name:    "Eduksha User",
          email,
          contact: phone,
        });
        razorpayCustomerId = customer.id;
      } catch (err) {
        // Razorpay throws 400 "Customer already exists" even with fail_existing:0.
        // Fall back to searching by contact to recover the existing customer ID.
        const isAlreadyExists = err?.error?.description?.toLowerCase().includes("already exists")
          || err?.statusCode === 400;
        if (isAlreadyExists && phone) {
          try {
            const list = await rzp.customers.all({ contact: phone });
            razorpayCustomerId = list?.items?.[0]?.id ?? null;
          } catch (_) { /* fall through to error below */ }
        }
        if (!razorpayCustomerId) {
          const detail = err?.error?.description || err?.error?.code || err?.message || JSON.stringify(err);
          logger.error("createSubscription: customer create error", { detail });
          throw new HttpsError("internal", detail);
        }
      }
      await userDocRef.set({ razorpayCustomerId }, { merge: true });
    }

    const subPayload = {
      plan_id:         planId,
      customer_id:     razorpayCustomerId,
      customer_notify: 0,
      // Trial: 1 cycle at ₹1. After completion the webhook auto-creates the monthly sub.
      total_count:     isTrial ? 1 : (planType === "quarterly" ? 4 : planType === "halfyearly" ? 2 : 12),
      quantity:        1,
      notes:           { userId, planType },
    };

    let sub;
    try {
      sub = await rzp.subscriptions.create(subPayload);
    } catch (err) {
      const detail = err?.error?.description
        || err?.error?.code
        || err?.message
        || JSON.stringify(err?.error ?? err);
      logger.error("createSubscription: razorpay error", { detail, raw: JSON.stringify(err) });
      throw new HttpsError("internal", detail);
    }

    // Persist subscription record — fire-and-forget so the client gets the
    // subscription ID immediately without waiting for the Firestore write.
    db.collection("subscriptions").doc(userId).set({
      userId,
      planType,
      planId,
      razorpaySubscriptionId: sub.id,
      status:    "created",
      createdAt: FieldValue.serverTimestamp(),
    }, { merge: true }).catch(err =>
      logger.error("createSubscription: Firestore write error", { err })
    );

    logger.info(`createSubscription: userId=${userId} planType=${planType} subId=${sub.id}`);
    return { subscriptionId: sub.id, keyId: RAZORPAY_KEY_ID.value() };
  }
);

// ── razorpayWebhook ───────────────────────────────────────────────────────────
// Razorpay calls this URL on every subscription event.
// Verifies the webhook signature then updates Firestore subscription status.
//
// Dashboard → Settings → Webhooks → add this function's URL
// Events to enable: subscription.activated, subscription.charged,
//                   subscription.cancelled, subscription.halted
exports.razorpayWebhook = onRequest(
  { secrets: [RAZORPAY_KEY_SECRET] },
  async (req, res) => {
    if (req.method !== "POST") { res.status(405).send("Method Not Allowed"); return; }

    const webhookSecret = RAZORPAY_KEY_SECRET.value();
    const signature     = req.headers["x-razorpay-signature"] || "";
    const rawBody       = req.rawBody;

    const expectedSig = crypto
      .createHmac("sha256", webhookSecret)
      .update(rawBody)
      .digest("hex");

    if (signature !== expectedSig) {
      logger.warn("razorpayWebhook: invalid signature");
      res.status(400).send("Invalid signature");
      return;
    }

    const event   = req.body.event;
    const payload = req.body.payload?.subscription?.entity || {};
    const subId   = payload.id;
    const notes   = payload.notes || {};
    const userId  = notes.userId;

    logger.info(`razorpayWebhook: event=${event} subId=${subId} userId=${userId}`);

    if (!userId || !subId) { res.status(200).send("ok"); return; }

    const subRef = db.collection("subscriptions").doc(userId);

    switch (event) {
      case "subscription.activated":
      case "subscription.charged": {
        const chargeAt   = payload.charge_at   ? new Date(payload.charge_at * 1000)   : null;
        const currentEnd = payload.current_end ? new Date(payload.current_end * 1000) : null;
        await subRef.set({
          status:         "active",
          isPremium:      true,
          razorpaySubscriptionId: subId,
          currentPeriodEnd: currentEnd,
          nextChargeAt:     chargeAt,
          activatedAt:    FieldValue.serverTimestamp(),
          updatedAt:      FieldValue.serverTimestamp(),
        }, { merge: true });

        // Also stamp isPremium on the parent doc for easy app-side reads
        const snap = await subRef.get();
        const planType = snap.data()?.planType || "monthly";
        logger.info(`razorpayWebhook: activated userId=${userId} planType=${planType}`);
        break;
      }

      case "subscription.halted":
        await subRef.set({
          status:    "halted",
          isPremium: false,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
        break;

      case "subscription.cancelled":
        await subRef.set({
          status:    "cancelled",
          isPremium: false,
          cancelledAt: FieldValue.serverTimestamp(),
          updatedAt:   FieldValue.serverTimestamp(),
        }, { merge: true });
        break;

      case "subscription.completed": {
        // Trial subscription (1 cycle at ₹1) finished — auto-create the monthly
        // subscription so the user seamlessly transitions to ₹149/month.
        // Razorpay will send the customer a payment notification to authorize it.
        const snap = await subRef.get();
        const planType = snap.data()?.planType;
        const razorpayCustomerId = snap.data()?.razorpayCustomerId;
        if (planType === "trial" && razorpayCustomerId) {
          const Razorpay = require("razorpay");
          const rzp = new Razorpay({
            key_id:     RAZORPAY_KEY_ID.value(),
            key_secret: RAZORPAY_KEY_SECRET.value(),
          });
          const newSub = await rzp.subscriptions.create({
            plan_id:         PLAN_MONTHLY,
            customer_id:     razorpayCustomerId,
            customer_notify: 1, // Razorpay notifies user to authorize ₹149/month
            total_count:     12,
            notes:           { userId, planType: "monthly", fromTrial: "true" },
          });
          await subRef.set({
            planType: "monthly",
            razorpaySubscriptionId: newSub.id,
            status:   "created",
            updatedAt: FieldValue.serverTimestamp(),
          }, { merge: true });
          logger.info(`razorpayWebhook: trial→monthly userId=${userId} newSubId=${newSub.id}`);
        } else {
          // Non-trial subscription completed naturally (all cycles billed)
          await subRef.set({
            status:    "completed",
            isPremium: false,
            updatedAt: FieldValue.serverTimestamp(),
          }, { merge: true });
        }
        break;
      }

      default:
        logger.info(`razorpayWebhook: unhandled event=${event}`);
    }

    res.status(200).send("ok");
  }
);

// ── cancelSubscription ────────────────────────────────────────────────────────
// Called by the app when the user taps "Cancel subscription".
// Cancels at period end (user keeps access until current period expires).
exports.cancelSubscription = onCall(
  { secrets: [RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET] },
  async (request) => {
    const { userId } = request.data;
    if (!userId) throw new HttpsError("unauthenticated", "userId required");

    const snap = await db.collection("subscriptions").doc(userId).get();
    if (!snap.exists) throw new HttpsError("not-found", "No subscription found");

    const { razorpaySubscriptionId, status } = snap.data();
    if (!razorpaySubscriptionId) throw new HttpsError("not-found", "No Razorpay subscription ID");
    if (status === "cancelled") throw new HttpsError("already-exists", "Already cancelled");

    const Razorpay = require("razorpay");
    const rzp = new Razorpay({
      key_id:     RAZORPAY_KEY_ID.value(),
      key_secret: RAZORPAY_KEY_SECRET.value(),
    });

    try {
      // cancel_at_cycle_end=1 means user keeps access until current period ends
      await rzp.subscriptions.cancel(razorpaySubscriptionId, true);
    } catch (err) {
      logger.error("cancelSubscription: razorpay error", err);
      throw new HttpsError("internal", err.error?.description || err.message);
    }

    await db.collection("subscriptions").doc(userId).set({
      status:           "cancel_scheduled",
      cancelledAt:      FieldValue.serverTimestamp(),
      updatedAt:        FieldValue.serverTimestamp(),
    }, { merge: true });

    logger.info(`cancelSubscription: userId=${userId} subId=${razorpaySubscriptionId}`);
    return { success: true };
  }
);

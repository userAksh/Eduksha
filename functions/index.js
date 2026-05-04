/**
 * Cloud Functions for Dobi parent-task push notifications.
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
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const logger = require("firebase-functions/logger");

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
        : `Open Dobi to see what to do for "${scope}".`;
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

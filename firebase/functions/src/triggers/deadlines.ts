import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import { sendNotificationToUser } from "../notifications/sendNotification";

export const sendDeadlineReminders = onSchedule("every 5 minutes", async () => {
  const now = Date.now();
  const oneHour = 60 * 60 * 1000;

  const snapshot = await admin.firestore()
    .collectionGroup("tasks")
    .where("dueDate", "<=", now + oneHour)
    .where("dueDate", ">", now)
    .get();

  for (const doc of snapshot.docs) {
    const task = doc.data();

    if (task.isCompleted) continue;

    const userId = task.userId;

    await sendNotificationToUser(userId, {
      type: "TASK_DEADLINE",
      title: "Task Due Soon",
      body: `Your task "${task.title}" is due soon.`,
      payload: {
        taskId: doc.id,
        dueDate: task.dueDate,
      },
    });
  }
});

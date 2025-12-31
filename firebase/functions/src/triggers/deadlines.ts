import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import { sendNotificationToUser } from "../notifications/sendNotification";

export const sendDeadlineReminders = onSchedule("every 5 minutes", async () => {
  console.log("Deadline reminders function running");

  const now = Date.now();
  const oneHourFromNow = now + 60 * 60 * 1000;

  const snapshot = await admin
    .firestore()
    .collectionGroup("tasks")
    .where("dueDate", ">", now)
    .where("dueDate", "<=", oneHourFromNow)
    .where("isCompleted", "==", false)
    .get();

  console.log(`Found ${snapshot.size} tasks due within 1 hour`);

  for (const doc of snapshot.docs) {
    const task = doc.data();

    // Prevent duplicate notifications
    if (task.deadlineNotified === true) {
      console.log(`Skipping already notified task ${doc.id}`);
      continue;
    }

    const userId = task.userId;
    if (!userId) {
      console.warn(`Task ${doc.id} missing userId`);
      continue;
    }

    console.log(`Sending reminder for task ${doc.id} to user ${userId}`);

    await sendNotificationToUser(userId, {
      type: "TASK_DEADLINE",
      title: "Task Due Soon",
      body: `Your task "${task.title}" is due soon.`,
      payload: {
        taskId: doc.id,
        dueDate: task.dueDate,
      },
    });

    // Mark task as notified so it won't trigger again
    await doc.ref.update({
      deadlineNotified: true,
    });
  }

  console.log("Deadline reminder run complete");
});

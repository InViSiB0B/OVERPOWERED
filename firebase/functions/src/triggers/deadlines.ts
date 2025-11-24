import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import { sendNotificationToUser } from "../notifications/sendNotification";

export const sendDeadlineReminders = onSchedule("every 5 minutes", async () => {
  console.log("Deadline reminders function running...");

  const now = Date.now();
  const oneHour = 60 * 60 * 1000;

  console.log("Time now:", now, "Onehour:", oneHour);

  const snapshot = await admin.firestore()
    .collectionGroup("tasks")
    .where("dueDate", "<=", now + oneHour)
    .where("dueDate", ">", now)
    .get();

  console.log(`Found ${snapshot.size} tasks due within 1 hour`);

  for (const doc of snapshot.docs) {
    const task = doc.data();
    console.log("Task:", task);

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

import * as admin from "firebase-admin";
import { NotificationPayload } from "./types";

export async function sendNotificationToUser(userId: string, data: NotificationPayload) {
  const userDoc = await admin.firestore().collection("users").doc(userId).get();
  const fcmToken = userDoc.get("fcmToken");

  if (!fcmToken) {
    console.log(`User ${userId} has no FCM token`);
    return;
  }

  const message = {
    token: fcmToken,
    data: {
      type: data.type,
      title: data.title,
      body: data.body,
      payload: JSON.stringify(data.payload || {}),
    },
  };

  try {
    await admin.messaging().send(message);
    console.log(`Notification sent to user ${userId}: ${data.type}`);
  } catch (err) {
    console.error("Error sending notification:", err);
  }
}

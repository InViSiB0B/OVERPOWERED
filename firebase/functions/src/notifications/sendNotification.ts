import * as admin from "firebase-admin";
import { NotificationPayload } from "./types";

export async function sendNotificationToUser(userId: string, data: NotificationPayload) {
  console.log("Looking up token for user:", userId);

  const userDoc = await admin.firestore().collection("users").doc(userId).get();
  const fcmToken = userDoc.get("fcmToken");

  console.log("Retrieved FCM token:", fcmToken);

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

  console.log("Sending FCM message:", message);

  try {
    //await admin.messaging().send(message);
    //console.log(`Notification sent to user ${userId}: ${data.type}`);
    const response = await admin.messaging().send(message);
    console.log("FCM response:", response);
  } catch (err) {
    console.error("Error sending notification:", err);
  }
}

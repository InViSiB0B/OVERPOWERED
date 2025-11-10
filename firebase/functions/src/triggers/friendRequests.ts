import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { sendNotificationToUser } from "../notifications/sendNotification";

export const onFriendRequestCreated = onDocumentCreated(
  "users/{userId}/friendRequests/{requestId}",
  async (event) => {
    const userId = event.params.userId;
    const request = event.data?.data();

    if (!request) return;

    await sendNotificationToUser(userId, {
      type: "FRIEND_REQUEST",
      title: "New Friend Request",
      body: `${request.fromUserName} wants to connect with you`,
      payload: {
        requestId: event.params.requestId,
        fromUserId: request.fromUserId,
        fromUserName: request.fromUserName,
        toUserId: request.toUserId,
      },
    });
  }
);

import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { sendNotificationToUser } from "../notifications/sendNotification";

export const onFriendRequestCreated = onDocumentCreated(
  "users/{userId}/friendRequests/{requestId}",
  async (event) => {
    console.log("Friend request trigger fired:", event.params);

    const userId = event.params.userId;
    const request = event.data?.data();

    console.log("Request data:", request);

    if (!request) {
        console.log("No request data found, aborting");
        return;
    }

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

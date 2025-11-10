import { setGlobalOptions } from "firebase-functions";
import * as admin from "firebase-admin";

setGlobalOptions({ maxInstances: 10 });

admin.initializeApp();

export { onFriendRequestCreated } from "./triggers/friendRequests";
export { sendDeadlineReminders } from "./triggers/deadlines";

export interface NotificationPayload {
  type: string;      // e.g. "FRIEND_REQUEST", "TASK_DEADLINE"
  title: string;
  body: string;
  payload?: any;     // Extra data for the client
}

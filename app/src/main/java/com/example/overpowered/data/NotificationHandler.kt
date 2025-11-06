package com.example.overpowered.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.overpowered.R

class NotificationHandler(private val context: Context) {

    fun handle(data: Map<String, String>) {
        val type = data["type"] ?: return

        when (type) {
            "FRIEND_REQUEST" -> handleFriendRequest(data)
            "TASK_DEADLINE" -> handleTaskDeadline(data)
            else -> handleGeneric(data)
        }
    }

    private fun handleFriendRequest(data: Map<String, String>) {
        showNotification(
            title = data["title"] ?: "New Friend Request",
            body = data["body"] ?: "",
            channelId = "friend_request"
        )
    }

    private fun handleTaskDeadline(data: Map<String, String>) {
        showNotification(
            title = data["title"] ?: "Task Due Soon",
            body = data["body"] ?: "",
            channelId = "task_deadline"
        )
    }

    private fun handleGeneric(data: Map<String, String>) {
        showNotification(
            title = data["title"] ?: "Notification",
            body = data["body"] ?: "",
            channelId = "general"
        )
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val mgr = NotificationManagerCompat.from(context)

        // Create notification channel only once (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mgr.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(body)
            //.setSmallIcon(R.drawable.ic_notification) // TODO: set app icon
            .setAutoCancel(true)
            .build()


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            mgr.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
package com.taskraze.myapplication.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskraze.myapplication.R

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Event"
        val id = intent.getStringExtra("id") ?: "N/A"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "event_channel"
        val channelName = "Event Notifications"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val clickIntent = Intent(context, com.taskraze.myapplication.view.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingClickIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle("Upcoming Event")
            .setContentText(title)
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setAutoCancel(true)
            .setContentIntent(pendingClickIntent)
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }
}


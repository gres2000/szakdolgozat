package com.taskraze.myapplication.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.receiver.EventNotificationReceiver

class NotificationViewModel : ViewModel() {

    fun scheduleAllNotifications(context: Context, events: List<EventData>) {
        Log.d("NotificationMINE", "Scheduled notification for event $events")
        events.forEach { event ->
            Log.d("NotificationMINE", "Scheduled notification for event $event")
            if (event.notificationMinutesBefore != null) {
                scheduleEventNotification(context, event)
            }
        }
    }

    fun scheduleEventNotification(context: Context, event: EventData) {

        val minutesBefore = event.notificationMinutesBefore ?: return

        val alarmTime = event.startTime.time - minutesBefore * 60 * 1000

        if (alarmTime < System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("title", event.title)
            putExtra("id", event.id)
        }

        val requestCode = event.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
            Log.d("NotificationMINE", "Scheduled notification for event ${event.title} at $alarmTime")
        } catch (e: SecurityException) {
            Log.e("NotificationMINE", "Cannot schedule exact alarm", e)
        }
    }

    fun cancelEventNotification(context: Context, event: EventData) {
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("title", event.title)
            putExtra("id", event.id)
        }

        val requestCode = event.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(requestCode)

        Log.d("NotificationMINE", "Cancelled notification and alarm for event ${event.title}")
    }

}

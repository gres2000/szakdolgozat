package com.taskraze.myapplication.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.receiver.EventNotificationReceiver
import java.util.Calendar
import java.util.Locale

class NotificationViewModel : ViewModel() {

    fun scheduleAllNotifications(context: Context, events: List<EventData>) {
        events.forEach { event ->
            Log.d("SCHEDULEDasd", "Scheduling LOOOP for $event")
            if (event.notificationMinutesBefore != null) {
                scheduleEventNotification(context, event)
            }
        }
    }

    fun scheduleEventNotification(context: Context, event: EventData) {
        Log.d("SCHEDULEDasd", "Scheduling notifications for $event")
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

    fun scheduleTaskNotification(context: Context, task: TaskData) {
        val minutesBefore = task.notificationMinutesBefore ?: return

        // parse task.time ("HH:mm") to today's Date
        val calendar = Calendar.getInstance().apply {
            val parts = task.time?.split(":") ?: return
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmTime = calendar.timeInMillis - minutesBefore * 60 * 1000
        if (alarmTime < System.currentTimeMillis()) return

        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("id", task.taskId)
        }

        val requestCode = task.taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)

            val alarmDate = java.util.Date(alarmTime)
            val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            val alarmTimeString = dateFormat.format(alarmDate)

            Toast.makeText(
                context,
                "Notification will appear at $alarmTimeString",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: SecurityException) {
            Log.e("NotificationMINE", "Cannot schedule exact alarm", e)
        }
    }

    fun cancelTaskNotification(context: Context, task: TaskData) {
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("id", task.taskId)
        }

        val requestCode = task.taskId.hashCode()
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

        Log.d("NotificationMINE", "Cancelled notification and alarm for task ${task.title}")
    }

}

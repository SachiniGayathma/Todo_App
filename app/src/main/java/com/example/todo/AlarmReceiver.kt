package com.example.todo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderMessage = intent.getStringExtra("reminder_message") ?: "Task Reminder"
        val taskName = intent.getStringExtra("task_name") ?: "Your Task"

        // Show a notification when the alarm triggers
        showNotification(context, taskName, reminderMessage)

        // Play alarm sound
        val mp = MediaPlayer.create(context, R.raw.alarmtone)
        mp.start()

        // Optionally, show a toast
        Toast.makeText(context, reminderMessage, Toast.LENGTH_LONG).show()
    }

    private fun showNotification(context: Context, taskName: String, message: String) {
        val channelId = "task_reminder_channel"
        val notificationId = 1

        // Create a notification channel for API 26+ (Android O and above)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create a pending intent to open the app when the notification is clicked
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build and display the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Task Reminder: $taskName")
            .setContentText(message)
            .setSmallIcon(R.drawable.notification) // Make sure to add your notification icon in res/drawable
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss the notification when clicked

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}

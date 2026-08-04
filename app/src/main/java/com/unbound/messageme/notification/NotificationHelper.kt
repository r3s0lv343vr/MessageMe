package com.unbound.messageme.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unbound.messageme.MainActivity
import com.unbound.messageme.R
import com.unbound.messageme.data.local.ReminderType
import com.unbound.messageme.data.local.TaskEntity

object NotificationHelper {
    const val CHANNEL_ID = "scheduled_messages"
    const val ACTION_COMPLETE = "com.unbound.messageme.ACTION_COMPLETE"
    const val ACTION_SNOOZE = "com.unbound.messageme.ACTION_SNOOZE"
    const val ACTION_OPEN = "com.unbound.messageme.ACTION_OPEN"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_scheduled_messages_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_scheduled_messages_desc)
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            lightColor = Color.parseColor("#E4572E")
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        task: TaskEntity,
        body: String,
        reminderId: String,
        type: ReminderType
    ) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
            action = ACTION_OPEN
        }
        val contentPending = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val color = when (type) {
            ReminderType.UNACKED_1, ReminderType.UNACKED_2, ReminderType.UNACKED_3 ->
                Color.parseColor("#E4572E")
            else -> Color.parseColor("#F4A261")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_envelope)
            .setContentTitle("MessageMe ✉️")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setColor(color)
            .setGroup("messageme_reminders")
            .setContentIntent(contentPending)

        builder.addAction(
            0,
            "Complete",
            broadcast(context, ACTION_COMPLETE, task.id, reminderId, task.id.hashCode() + 1)
        )
        builder.addAction(
            0,
            "Snooze 10m",
            broadcast(context, ACTION_SNOOZE, task.id, reminderId, task.id.hashCode() + 2)
        )
        builder.addAction(
            0,
            "Open",
            contentPending
        )

        NotificationManagerCompat.from(context)
            .notify((task.id + reminderId).hashCode(), builder.build())
    }

    private fun broadcast(
        context: Context,
        action: String,
        taskId: String,
        reminderId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

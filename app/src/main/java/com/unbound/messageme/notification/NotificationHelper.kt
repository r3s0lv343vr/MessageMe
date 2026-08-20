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
import androidx.core.app.Person
import com.unbound.messageme.MainActivity
import com.unbound.messageme.R
import com.unbound.messageme.data.local.ReminderType
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.domain.NotificationCopy

object NotificationHelper {
    const val CHANNEL_ID = "messages_from_you"
    const val ACTION_ACKNOWLEDGE = "com.unbound.messageme.ACTION_ACKNOWLEDGE"
    const val ACTION_SNOOZE = "com.unbound.messageme.ACTION_SNOOZE"
    const val ACTION_REWRITE = "com.unbound.messageme.ACTION_REWRITE"
    const val ACTION_OPEN = "com.unbound.messageme.ACTION_OPEN"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_messages_from_you_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_messages_from_you_desc)
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            lightColor = Color.parseColor("#E4572E")
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        task: TaskEntity,
        reminderId: String,
        type: ReminderType
    ) {
        ensureChannel(context)
        val note = NotificationCopy.personalNote(task)
        val you = Person.Builder()
            .setName(NotificationCopy.SENDER)
            .setImportant(true)
            .build()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            action = ACTION_OPEN
        }
        val contentPending = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rewritePending = PendingIntent.getActivity(
            context,
            task.id.hashCode() + 3,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_REMINDER_ID, reminderId)
                action = ACTION_REWRITE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val color = when (type) {
            ReminderType.UNACKED_1, ReminderType.UNACKED_2, ReminderType.UNACKED_3 ->
                Color.parseColor("#E4572E")
            else -> Color.parseColor("#F4A261")
        }

        val messaging = NotificationCompat.MessagingStyle(you)
            .addMessage(note, System.currentTimeMillis(), you)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_envelope)
            .setContentTitle(NotificationCopy.SENDER)
            .setContentText(note)
            .setStyle(messaging)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setColor(color)
            .setGroup("messageme_messages")
            .setContentIntent(contentPending)
            .addAction(
                0,
                context.getString(R.string.notification_action_acknowledged),
                broadcast(context, ACTION_ACKNOWLEDGE, task.id, reminderId, task.id.hashCode() + 1)
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_snooze),
                broadcast(context, ACTION_SNOOZE, task.id, reminderId, task.id.hashCode() + 2)
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_rewrite),
                rewritePending
            )

        NotificationManagerCompat.from(context)
            .notify(NotificationCopy.notificationId(task.id, reminderId), builder.build())
    }

    fun cancel(context: Context, taskId: String, reminderId: String) {
        NotificationManagerCompat.from(context)
            .cancel(NotificationCopy.notificationId(taskId, reminderId))
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

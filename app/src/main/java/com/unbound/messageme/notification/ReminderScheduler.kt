package com.unbound.messageme.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.unbound.messageme.MainActivity
import com.unbound.messageme.data.local.ReminderType
import com.unbound.messageme.data.local.ScheduledReminderEntity
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.domain.TimeDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: ScheduledReminderEntity) {
        if (reminder.cancelled || reminder.delivered) return
        if (reminder.triggerAtEpochMillis <= TimeDefaults.nowMillis()) return

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminder.id)
            putExtra(NotificationHelper.EXTRA_TASK_ID, reminder.taskId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact && reminder.type == ReminderType.AT_DUE) {
            val showApp = PendingIntent.getActivity(
                context,
                reminder.id.hashCode() + 17,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(reminder.triggerAtEpochMillis, showApp),
                pending
            )
        } else if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtEpochMillis,
                pending
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtEpochMillis,
                pending
            )
        }
    }

    fun cancel(reminderId: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun cancelReminders(reminders: List<ScheduledReminderEntity>) {
        reminders.forEach { cancel(it.id) }
    }

    fun notifyUser(task: TaskEntity, reminder: ScheduledReminderEntity) {
        NotificationHelper.showReminder(
            context = context,
            task = task,
            reminderId = reminder.id,
            type = reminder.type
        )
    }
}

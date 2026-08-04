package com.unbound.messageme.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unbound.messageme.data.repository.MessageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.deliverReminder(reminderId)
            } finally {
                pending.finish()
            }
        }
    }
}

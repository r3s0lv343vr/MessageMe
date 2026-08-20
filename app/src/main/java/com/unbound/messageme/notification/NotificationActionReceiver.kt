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
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID).orEmpty()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_ACKNOWLEDGE -> repository.acknowledge(taskId)
                    NotificationHelper.ACTION_SNOOZE -> repository.snooze(taskId, minutes = 10)
                }
            } finally {
                if (reminderId.isNotEmpty()) {
                    NotificationHelper.cancel(context, taskId, reminderId)
                }
                pending.finish()
            }
        }
    }
}

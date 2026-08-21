package com.unbound.messageme.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.unbound.messageme.MainActivity
import com.unbound.messageme.R
import com.unbound.messageme.data.local.ChatMessageDao
import com.unbound.messageme.data.local.TaskDao
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.domain.UnreadLetterLogic
import com.unbound.messageme.domain.UnreadLetterSnapshot
import com.unbound.messageme.notification.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class UnreadLetterWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        UnreadLetterViews.applyPlaceholder(context, appWidgetManager, appWidgetIds)
        val pending = goAsync()
        widgetScope.launch {
            try {
                UnreadLetterViews.push(context, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }
}

internal object UnreadLetterViews {
    fun applyPlaceholder(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val empty = UnreadLetterSnapshot(
            unreadCount = 0,
            preview = UnreadLetterLogic.EMPTY_PREVIEW
        )
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, remoteViews(context, empty, id))
        }
    }
    suspend fun push(context: Context, appWidgetIds: IntArray? = null) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = appWidgetIds ?: manager.getAppWidgetIds(
            ComponentName(context, UnreadLetterWidgetReceiver::class.java)
        )
        if (ids.isEmpty()) return
        val snapshot = loadSnapshot(context)
        ids.forEach { id ->
            manager.updateAppWidget(id, remoteViews(context, snapshot, id))
        }
    }

    private fun remoteViews(
        context: Context,
        snapshot: UnreadLetterSnapshot,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_unread_letter_preview)
        views.setTextViewText(R.id.unread_letter_sender, UnreadLetterLogic.SENDER)
        views.setTextViewText(R.id.unread_letter_preview, snapshot.preview)
        if (snapshot.hasUnread) {
            views.setViewVisibility(R.id.unread_letter_count, View.VISIBLE)
            views.setTextViewText(R.id.unread_letter_count, snapshot.countLabel)
        } else {
            views.setViewVisibility(R.id.unread_letter_count, View.GONE)
        }
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = NotificationHelper.ACTION_OPEN
            putExtra(
                NotificationHelper.EXTRA_DAY,
                snapshot.dayIso ?: LocalDate.now(TimeDefaults.zoneId()).toString()
            )
            snapshot.messageId?.let { putExtra(NotificationHelper.EXTRA_MESSAGE_ID, it) }
            snapshot.taskId?.let { putExtra(NotificationHelper.EXTRA_TASK_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            appWidgetId,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.unread_letter_root, pending)
        return views
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UnreadLetterEntryPoint {
    fun chatMessageDao(): ChatMessageDao
    fun taskDao(): TaskDao
}

internal suspend fun loadSnapshot(context: Context): UnreadLetterSnapshot {
    return runCatching {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            UnreadLetterEntryPoint::class.java
        )
        UnreadLetterLogic.snapshot(
            messages = entry.chatMessageDao().getAll(),
            tasks = entry.taskDao().getActive()
        )
    }.getOrElse {
        UnreadLetterSnapshot(unreadCount = 0, preview = UnreadLetterLogic.EMPTY_PREVIEW)
    }
}

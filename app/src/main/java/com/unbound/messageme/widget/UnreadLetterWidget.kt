package com.unbound.messageme.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.unbound.messageme.MainActivity
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

class UnreadLetterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        provideContent {
            UnreadLetterBubble(context = context, snapshot = snapshot)
        }
    }
}

class UnreadLetterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UnreadLetterWidget()
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

@Composable
private fun UnreadLetterBubble(context: Context, snapshot: UnreadLetterSnapshot) {
    val foam = ColorProvider(AndroidColor.parseColor("#FFFDF8"))
    val orange = ColorProvider(AndroidColor.parseColor("#F4A261"))
    val red = ColorProvider(AndroidColor.parseColor("#E4572E"))
    val openIntent = Intent(context, MainActivity::class.java).apply {
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

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(actionStartActivity(openIntent))
            .cornerRadius(22.dp)
            .background(orange)
            .padding(16.dp)
    ) {
        Text(
            text = UnreadLetterLogic.SENDER,
            style = TextStyle(
                color = foam,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (snapshot.hasUnread) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = snapshot.countLabel,
                style = TextStyle(
                    color = foam,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .cornerRadius(12.dp)
                    .background(red)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = snapshot.preview,
            maxLines = 3,
            style = TextStyle(
                color = foam,
                fontSize = 16.sp
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}

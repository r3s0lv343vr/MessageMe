package com.unbound.messageme.ui.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.ui.theme.AccentOrange
import com.unbound.messageme.ui.theme.BubbleSelf
import com.unbound.messageme.ui.theme.Foam
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.PastelYellow
import com.unbound.messageme.ui.theme.ReminderOrange
import com.unbound.messageme.ui.theme.ReminderRed
import com.unbound.messageme.ui.theme.WaterBlue
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    task: TaskEntity?,
    onAcknowledge: () -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onReschedule: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isReminder = message.isReminderStyle ||
        message.kind == MessageKind.REMINDER ||
        message.kind == MessageKind.DAYTIME_REMINDER ||
        message.kind == MessageKind.FOLLOW_UP_UNACKED

    val bubbleColor = when {
        message.kind == MessageKind.FOLLOW_UP_UNACKED -> ReminderRed
        isReminder -> ReminderOrange
        message.kind == MessageKind.SYSTEM -> PastelYellow.copy(alpha = 0.85f)
        else -> BubbleSelf
    }
    val textColor = if (message.kind == MessageKind.SYSTEM) Ink else Foam
    val shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    val timeText = remember(message.sentAtEpochMillis) {
        DateTimeFormatter.ofPattern("MMM d · h:mm a")
            .withZone(TimeDefaults.zoneId())
            .format(Instant.ofEpochMilli(message.sentAtEpochMillis))
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 4 }) {
            Column(
                Modifier
                    .widthIn(max = 340.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .then(
                        if (message.isUnread) Modifier.border(1.5.dp, ReminderRed, shape) else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(message.body, color = textColor, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(timeText, color = textColor.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (message.requiresAck && message.taskId != null) {
            TextButton(onClick = onAcknowledge) { Text("Acknowledged / delivery received", color = WaterBlue) }
        }
        if (message.requiresCompletionAnswer && message.taskId != null) {
            Row {
                TextButton(onClick = onComplete) { Text("Yes, completed", color = WaterBlue) }
                TextButton(onClick = onReschedule) { Text("Not yet", color = AccentOrange) }
            }
        }
        if (message.requiresReschedule && message.taskId != null) {
            Row {
                TextButton(onClick = onReschedule) { Text("Reschedule", color = AccentOrange) }
                TextButton(onClick = onComplete) { Text("Complete", color = WaterBlue) }
                TextButton(onClick = onDismiss) { Text("Dismiss", color = ReminderRed) }
            }
        }
        if (task != null && !task.deleted && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.DISMISSED) {
            Row {
                TextButton(onClick = onEdit) { Text("Edit", color = WaterBlue) }
                TextButton(onClick = onSnooze) { Text("Snooze 10m", color = AccentOrange) }
                TextButton(onClick = onDelete) { Text("Delete", color = ReminderRed) }
            }
        }
    }
}

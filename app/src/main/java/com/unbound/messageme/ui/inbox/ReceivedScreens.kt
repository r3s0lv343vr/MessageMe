package com.unbound.messageme.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.domain.InboxLogic
import com.unbound.messageme.domain.NotificationCopy
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.Foam
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.ReminderOrange
import com.unbound.messageme.ui.theme.ReminderRed
import com.unbound.messageme.ui.theme.WaterBlue
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivedDayScreen(
    date: LocalDate,
    messages: List<ChatMessageEntity>,
    tasks: List<TaskEntity>,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenMessage: (ChatMessageEntity) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val dayMessages = remember(messages, date) { InboxLogic.receivedOn(messages, date) }
    val heading = remember(date) { date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }

    WatercolorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Received", style = MaterialTheme.typography.headlineMedium, color = Ink)
                            Text(heading, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.75f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenCalendar) {
                            Icon(Icons.Default.Menu, contentDescription = "Todo calendar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onPrevDay) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day", tint = WaterBlue)
                        }
                        IconButton(onClick = onNextDay) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next day", tint = WaterBlue)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (dayMessages.isEmpty()) {
                Text(
                    "No messages received on this day.",
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    color = Ink.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dayMessages, key = { it.id }) { message ->
                        val task = tasks.find { it.id == message.taskId }
                        ReceivedRow(message = message, task = task, onClick = { onOpenMessage(message) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedRow(
    message: ChatMessageEntity,
    task: TaskEntity?,
    onClick: () -> Unit
) {
    val preview = task?.let { NotificationCopy.personalNote(it) } ?: message.body
    val timeText = remember(message.sentAtEpochMillis) {
        DateTimeFormatter.ofPattern("h:mm a")
            .withZone(TimeDefaults.zoneId())
            .format(java.time.Instant.ofEpochMilli(message.sentAtEpochMillis))
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ReminderOrange.copy(alpha = 0.92f))
            .then(if (message.isUnread) Modifier.border(1.5.dp, ReminderRed, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            preview,
            color = Foam,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(timeText, color = Foam.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivedMessageScreen(
    date: LocalDate,
    message: ChatMessageEntity?,
    task: TaskEntity?,
    onBack: () -> Unit,
    onAcknowledge: (String) -> Unit,
    onComplete: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onReschedule: (String, LocalDate, LocalTime?) -> Unit,
    onSnooze: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val heading = remember(date) { date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }

    WatercolorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Message", style = MaterialTheme.typography.headlineMedium, color = Ink)
                            Text(heading, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.75f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to this day's messages")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (message == null) {
                    Text("This message is no longer available.", color = Ink)
                } else {
                    MessageBubble(
                        message = message,
                        task = task,
                        onAcknowledge = { message.taskId?.let(onAcknowledge) },
                        onComplete = { message.taskId?.let(onComplete) },
                        onDismiss = { message.taskId?.let(onDismiss) },
                        onReschedule = { showDatePicker = true },
                        onSnooze = { message.taskId?.let(onSnooze) },
                        onEdit = { message.taskId?.let(onEdit) },
                        onDelete = { message.taskId?.let(onDelete) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val todayUtcMillis = TimeDefaults.utcMillisFromLocalDate(LocalDate.now(TimeDefaults.zoneId()))
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = TimeDefaults.utcMillisFromLocalDate(date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val picked = TimeDefaults.localDateFromUtcMillis(millis)
                        message?.taskId?.let { onReschedule(it, picked, null) }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dateState) }
    }
}

package com.unbound.messageme.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.Recurrence
import com.unbound.messageme.domain.AiScheduleSuggestions
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.AccentOrange
import com.unbound.messageme.ui.theme.BubbleSelf
import com.unbound.messageme.ui.theme.Foam
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.PastelYellow
import com.unbound.messageme.ui.theme.ReminderOrange
import com.unbound.messageme.ui.theme.ReminderRed
import com.unbound.messageme.ui.theme.WaterBlue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessageEntity>,
    suggestions: List<AiScheduleSuggestions.Suggestion>,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onSend: (title: String, body: String, date: LocalDate, time: LocalTime?, priority: Priority, category: String, recurrence: Recurrence) -> Unit,
    onAcknowledge: (String) -> Unit,
    onComplete: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onReschedule: (String, LocalDate, LocalTime?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now(TimeDefaults.zoneId())) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var category by remember { mutableStateOf("General") }
    var recurrence by remember { mutableStateOf(Recurrence.NONE) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var rescheduleTaskId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    WatercolorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "MessageMe",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenCalendar) {
                            Icon(Icons.Default.Menu, contentDescription = "Todo calendar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            ) {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                text = "Message yourself a task.\nAdd a title, pick a date — time is optional (defaults to 3:00 AM).",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ink.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onAcknowledge = { message.taskId?.let(onAcknowledge) },
                            onComplete = { message.taskId?.let(onComplete) },
                            onDismiss = { message.taskId?.let(onDismiss) },
                            onReschedule = {
                                rescheduleTaskId = message.taskId
                                showDatePicker = true
                            }
                        )
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.take(3).forEach { suggestion ->
                            TextButton(
                                onClick = { selectedTime = suggestion.time }, Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PastelYellow.copy(alpha = 0.7f))
                            ) {
                                Text("${suggestion.label}: ${suggestion.time}", color = Ink)
                            }
                        }
                    }
                }

                Composer(
                    title = title,
                    onTitle = { title = it },
                    body = body,
                    onBody = { body = it },
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    priority = priority,
                    category = category,
                    recurrence = recurrence,
                    menuExpanded = menuExpanded,
                    onMenu = { menuExpanded = it },
                    onPickDate = { showDatePicker = true; menuExpanded = false },
                    onPickTime = { showTimePicker = true; menuExpanded = false },
                    onClearTime = { selectedTime = null; menuExpanded = false },
                    onPriority = { priority = it; menuExpanded = false },
                    onCategory = { category = it; menuExpanded = false },
                    onRecurrence = { recurrence = it; menuExpanded = false },
                    onSend = {
                        onSend(title, body, selectedDate, selectedTime, priority, category, recurrence)
                        title = ""
                        body = ""
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false; rescheduleTaskId = null },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val taskId = rescheduleTaskId
                        if (taskId != null) {
                            onReschedule(taskId, date, selectedTime)
                            rescheduleTaskId = null
                        } else {
                            selectedDate = date
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false; rescheduleTaskId = null }) { Text("Cancel") }
            }
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: 9,
            initialMinute = selectedTime?.minute ?: 0
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            title = { Text("Task time") },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    onAcknowledge: () -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onReschedule: () -> Unit
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
    }
}

@Composable
private fun Composer(
    title: String,
    onTitle: (String) -> Unit,
    body: String,
    onBody: (String) -> Unit,
    selectedDate: LocalDate,
    selectedTime: LocalTime?,
    priority: Priority,
    category: String,
    recurrence: Recurrence,
    menuExpanded: Boolean,
    onMenu: (Boolean) -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    onPriority: (Priority) -> Unit,
    onCategory: (String) -> Unit,
    onRecurrence: (Recurrence) -> Unit,
    onSend: () -> Unit
) {
    val scheduleLabel = buildString {
        append(selectedDate.format(DateTimeFormatter.ofPattern("MMM d")))
        append(if (selectedTime != null) " · ${selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))}" else " · 3:00 AM default")
        append(" · ${priority.name.lowercase()} · $category · ${recurrence.name.lowercase()}")
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Foam.copy(alpha = 0.92f))
            .padding(12.dp)
    ) {
        Text(scheduleLabel, color = WaterBlue, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitle, Modifier.fillMaxWidth(),
            placeholder = { Text("Title") },
            singleLine = true
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = body,
                onValueChange = onBody, Modifier.weight(1f),
                placeholder = { Text("Message yourself…") },
                maxLines = 3
            )
            Box {
                IconButton(onClick = { onMenu(true) }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Schedule options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenu(false) }) {
                    DropdownMenuItem(text = { Text("Pick date") }, onClick = onPickDate)
                    DropdownMenuItem(text = { Text("Pick time") }, onClick = onPickTime)
                    DropdownMenuItem(text = { Text("Use 3:00 AM default") }, onClick = onClearTime)
                    DropdownMenuItem(text = { Text("Priority: Low") }, onClick = { onPriority(Priority.LOW) })
                    DropdownMenuItem(text = { Text("Priority: Normal") }, onClick = { onPriority(Priority.NORMAL) })
                    DropdownMenuItem(text = { Text("Priority: High") }, onClick = { onPriority(Priority.HIGH) })
                    DropdownMenuItem(text = { Text("Category: Work") }, onClick = { onCategory("Work") })
                    DropdownMenuItem(text = { Text("Category: Personal") }, onClick = { onCategory("Personal") })
                    DropdownMenuItem(text = { Text("Repeat: None") }, onClick = { onRecurrence(Recurrence.NONE) })
                    DropdownMenuItem(text = { Text("Repeat: Daily") }, onClick = { onRecurrence(Recurrence.DAILY) })
                    DropdownMenuItem(text = { Text("Repeat: Weekly") }, onClick = { onRecurrence(Recurrence.WEEKLY) })
                    DropdownMenuItem(text = { Text("Repeat: Monthly") }, onClick = { onRecurrence(Recurrence.MONTHLY) })
                }
            }
            Button(
                onClick = onSend,
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WaterBlue, contentColor = Foam)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

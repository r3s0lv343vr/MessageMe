package com.unbound.messageme.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.Recurrence
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
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
    tasks: List<TaskEntity>,
    editingTask: TaskEntity?,
    suggestions: List<AiScheduleSuggestions.Suggestion>,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateReminder: (title: String, body: String, date: LocalDate, time: LocalTime?, priority: Priority, category: String, recurrence: Recurrence) -> Unit,
    onSaveEdit: (taskId: String, title: String, body: String, date: LocalDate, time: LocalTime?, priority: Priority, category: String, recurrence: Recurrence) -> Unit,
    onBeginEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onAcknowledge: (String) -> Unit,
    onComplete: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onReschedule: (String, LocalDate, LocalTime?) -> Unit,
    onSnooze: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now(TimeDefaults.zoneId())) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var category by remember { mutableStateOf("Personal") }
    var recurrence by remember { mutableStateOf(Recurrence.NONE) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var rescheduleTaskId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingTask?.id) {
        val task = editingTask ?: return@LaunchedEffect
        title = task.title
        body = task.body
        val due = Instant.ofEpochMilli(task.dueAtEpochMillis).atZone(TimeDefaults.zoneId())
        selectedDate = due.toLocalDate()
        selectedTime = if (task.timeWasExplicitlyChosen) due.toLocalTime() else null
        priority = task.priority
        category = task.category
        recurrence = task.recurrence
    }

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
                        val task = tasks.find { it.id == message.taskId }
                        MessageBubble(
                            message = message,
                            task = task,
                            onAcknowledge = { message.taskId?.let(onAcknowledge) },
                            onComplete = { message.taskId?.let(onComplete) },
                            onDismiss = { message.taskId?.let(onDismiss) },
                            onReschedule = {
                                rescheduleTaskId = message.taskId
                                showDatePicker = true
                            },
                            onSnooze = { message.taskId?.let(onSnooze) },
                            onEdit = { message.taskId?.let(onBeginEdit) },
                            onDelete = { message.taskId?.let(onDelete) }
                        )
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        suggestions.take(3).forEach { suggestion ->
                            TextButton(
                                onClick = { selectedTime = suggestion.time },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PastelYellow.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    "${suggestion.label}: ${suggestion.time.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                                    color = Ink
                                )
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
                    onPickDate = { showDatePicker = true },
                    onPickTime = { showTimePicker = true },
                    onClearTime = { selectedTime = null },
                    onPriority = { priority = it },
                    onCategory = { category = it },
                    onRecurrence = { recurrence = it },
                    isEditing = editingTask != null,
                    onCancelEdit = {
                        onCancelEdit()
                        title = ""
                        body = ""
                    },
                    onSend = {
                        val current = editingTask
                        if (current != null) {
                            onSaveEdit(current.id, title, body, selectedDate, selectedTime, priority, category, recurrence)
                        } else {
                            onCreateReminder(title, body, selectedDate, selectedTime, priority, category, recurrence)
                        }
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

private enum class ComposerMenu { Date, Time, Priority, Category, Repeat }

private fun priorityLabel(priority: Priority): String = when (priority) {
    Priority.LOW -> "Low"
    Priority.NORMAL -> "Medium"
    Priority.HIGH -> "High"
}

private fun recurrenceLabel(recurrence: Recurrence): String = when (recurrence) {
    Recurrence.NONE -> "None"
    Recurrence.DAILY -> "Daily"
    Recurrence.WEEKLY -> "Weekly"
    Recurrence.MONTHLY -> "Monthly"
    Recurrence.CUSTOM -> "Custom"
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
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    onPriority: (Priority) -> Unit,
    onCategory: (String) -> Unit,
    onRecurrence: (Recurrence) -> Unit,
    isEditing: Boolean,
    onCancelEdit: () -> Unit,
    onSend: () -> Unit
) {
    var openMenu by remember { mutableStateOf<ComposerMenu?>(null) }
    var showCustomCategory by remember { mutableStateOf(false) }
    var customCategory by remember { mutableStateOf("") }
    val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
    val timeLabel = selectedTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "3:00 AM"

    Column(
        Modifier
            .fillMaxWidth()
            .background(Foam.copy(alpha = 0.92f))
            .padding(12.dp)
    ) {
        if (isEditing) {
            Text(
                "Editing reminder — send to save, or cancel.",
                color = AccentOrange,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickedValueMenu(
                value = dateLabel,
                contentDescription = "Pick date",
                expanded = openMenu == ComposerMenu.Date,
                onExpand = { openMenu = ComposerMenu.Date },
                onDismiss = { openMenu = null }
            ) {
                DropdownMenuItem(
                    text = { Text("Pick date") },
                    onClick = {
                        openMenu = null
                        onPickDate()
                    }
                )
            }
            PickedValueMenu(
                value = timeLabel,
                contentDescription = "Pick time",
                expanded = openMenu == ComposerMenu.Time,
                onExpand = { openMenu = ComposerMenu.Time },
                onDismiss = { openMenu = null }
            ) {
                DropdownMenuItem(
                    text = { Text("Pick time") },
                    onClick = {
                        openMenu = null
                        onPickTime()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Use 3:00 AM default") },
                    onClick = {
                        openMenu = null
                        onClearTime()
                    }
                )
            }
            PickedValueMenu(
                value = priorityLabel(priority),
                contentDescription = "Priority",
                expanded = openMenu == ComposerMenu.Priority,
                onExpand = { openMenu = ComposerMenu.Priority },
                onDismiss = { openMenu = null }
            ) {
                DropdownMenuItem(text = { Text("Low") }, onClick = { onPriority(Priority.LOW); openMenu = null })
                DropdownMenuItem(text = { Text("Medium") }, onClick = { onPriority(Priority.NORMAL); openMenu = null })
                DropdownMenuItem(text = { Text("High") }, onClick = { onPriority(Priority.HIGH); openMenu = null })
            }
            PickedValueMenu(
                value = category,
                contentDescription = "Category",
                expanded = openMenu == ComposerMenu.Category,
                onExpand = { openMenu = ComposerMenu.Category },
                onDismiss = { openMenu = null }
            ) {
                DropdownMenuItem(text = { Text("Home") }, onClick = { onCategory("Home"); openMenu = null })
                DropdownMenuItem(text = { Text("Work") }, onClick = { onCategory("Work"); openMenu = null })
                DropdownMenuItem(text = { Text("Personal") }, onClick = { onCategory("Personal"); openMenu = null })
                DropdownMenuItem(
                    text = { Text("Custom…") },
                    onClick = {
                        openMenu = null
                        customCategory = if (category in listOf("Home", "Work", "Personal")) "" else category
                        showCustomCategory = true
                    }
                )
            }
            PickedValueMenu(
                value = recurrenceLabel(recurrence),
                contentDescription = "Repeat",
                expanded = openMenu == ComposerMenu.Repeat,
                onExpand = { openMenu = ComposerMenu.Repeat },
                onDismiss = { openMenu = null }
            ) {
                DropdownMenuItem(text = { Text("None") }, onClick = { onRecurrence(Recurrence.NONE); openMenu = null })
                DropdownMenuItem(text = { Text("Daily") }, onClick = { onRecurrence(Recurrence.DAILY); openMenu = null })
                DropdownMenuItem(text = { Text("Weekly") }, onClick = { onRecurrence(Recurrence.WEEKLY); openMenu = null })
                DropdownMenuItem(text = { Text("Monthly") }, onClick = { onRecurrence(Recurrence.MONTHLY); openMenu = null })
            }
        }
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
            Button(
                onClick = onSend,
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WaterBlue, contentColor = Foam)
            ) {
                Icon(Icons.Default.Send, contentDescription = if (isEditing) "Update reminder" else "Send")
            }
            if (isEditing) {
                TextButton(onClick = onCancelEdit) { Text("Cancel") }
            }
        }
    }

    if (showCustomCategory) {
        AlertDialog(
            onDismissRequest = { showCustomCategory = false },
            title = { Text("Custom category") },
            text = {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Errands") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val defined = customCategory.trim()
                        if (defined.isNotEmpty()) onCategory(defined)
                        showCustomCategory = false
                    },
                    enabled = customCategory.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategory = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PickedValueMenu(
    value: String,
    contentDescription: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box {
        TextButton(onClick = onExpand) {
            Text(
                value,
                color = Ink,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = contentDescription, tint = WaterBlue)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = content)
    }
}

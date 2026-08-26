package com.unbound.messageme.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SelectableDates
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unbound.messageme.R
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.Recurrence
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.domain.AiScheduleSuggestions
import com.unbound.messageme.domain.EnvelopeHour
import com.unbound.messageme.domain.InboxLogic
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.AccentOrange
import com.unbound.messageme.ui.theme.BubbleSelf
import com.unbound.messageme.ui.theme.Foam
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.PastelYellow
import com.unbound.messageme.ui.theme.WaterBlue
import com.unbound.messageme.ui.theme.readableOutlinedTextFieldColors
import com.unbound.messageme.widget.UnreadLetterPin
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
    onReschedule: (String, LocalDate, LocalTime?) -> Unit,
    onSnooze: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    envelopeHour: EnvelopeHour = EnvelopeHour.DEFAULT,
    showEnvelopeHint: Boolean = false,
    onDismissEnvelopeHint: () -> Unit = {}
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

    val scheduled = remember(tasks, messages) { InboxLogic.scheduledTasks(tasks, messages) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var letterOnHome by remember { mutableStateOf(UnreadLetterPin.isPlaced(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                letterOnHome = UnreadLetterPin.isPlaced(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(scheduled.size) {
        if (scheduled.isNotEmpty()) listState.animateScrollToItem(scheduled.lastIndex)
    }

    WatercolorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Scheduled",
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
                    .imePadding()
            ) {
                if (showEnvelopeHint) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Skip the clock to send an overnight letter. It arrives at your envelope hour. Change that hour in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink.copy(alpha = 0.8f)
                        )
                        TextButton(onClick = onDismissEnvelopeHint) {
                            Text("Got it")
                        }
                    }
                }
                if (!letterOnHome) {
                    Button(
                        onClick = { UnreadLetterPin.requestPin(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = Foam
                        )
                    ) {
                        Text(stringResource(R.string.widget_add_to_home))
                    }
                }
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (scheduled.isEmpty()) {
                        item {
                            Text(
                                text = "Nothing waiting to send.\nWrite a note below — it stays here until the time you chose, then it appears in Received.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ink.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    items(scheduled, key = { it.id }) { task ->
                        ScheduledCard(
                            task = task,
                            onOpen = { onOpenTask(task.id) },
                            onEdit = { onBeginEdit(task.id) },
                            onReschedule = {
                                rescheduleTaskId = task.id
                                showDatePicker = true
                            },
                            onSnooze = { onSnooze(task.id) },
                            onDelete = { onDelete(task.id) }
                        )
                    }
                }

                val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                if (suggestions.isNotEmpty() && !imeVisible) {
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
                    envelopeHour = envelopeHour,
                    priority = priority,
                    category = category,
                    recurrence = recurrence,
                    onPickDate = { showDatePicker = true },
                    onPickTime = { showTimePicker = true },
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
        val todayUtcMillis = TimeDefaults.utcMillisFromLocalDate(LocalDate.now(TimeDefaults.zoneId()))
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = TimeDefaults.utcMillisFromLocalDate(selectedDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false; rescheduleTaskId = null },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val date = TimeDefaults.localDateFromUtcMillis(millis)
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
            initialHour = selectedTime?.hour ?: envelopeHour.hour,
            initialMinute = selectedTime?.minute ?: envelopeHour.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            title = { Text("When should this arrive?") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        selectedTime = null
                        showTimePicker = false
                    }) {
                        Text("Send an Overnight Letter")
                    }
                }
            }
        )
    }
}

@Composable
private fun ScheduledCard(
    task: TaskEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    val dueText = remember(task.dueAtEpochMillis, task.timeWasExplicitlyChosen) {
        val due = Instant.ofEpochMilli(task.dueAtEpochMillis).atZone(TimeDefaults.zoneId())
        val time = if (task.timeWasExplicitlyChosen) {
            due.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))
        } else {
            "Overnight letter · ${due.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))}"
        }
        "${due.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d"))} · $time"
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BubbleSelf)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(Modifier.clickable(onClick = onOpen)) {
            Text(task.title, color = Foam, style = MaterialTheme.typography.bodyLarge)
            if (task.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(task.body, color = Foam.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text("Sends $dueText", color = Foam.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            TextButton(onClick = onEdit) { Text("Edit", color = Foam) }
            TextButton(onClick = onReschedule) { Text("Reschedule", color = Foam) }
            TextButton(onClick = onSnooze) { Text("Snooze 10m", color = Foam) }
            TextButton(onClick = onDelete) { Text("Delete", color = Foam) }
        }
    }
}

private enum class ComposerMenu { Priority, Category, Repeat }

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
    envelopeHour: EnvelopeHour,
    priority: Priority,
    category: String,
    recurrence: Recurrence,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
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
    val timeLabel = selectedTime?.format(DateTimeFormatter.ofPattern("h:mm a"))
        ?: envelopeHour.clockLabel()

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
            PickedValueButton(
                value = dateLabel,
                contentDescription = "Pick date",
                onClick = onPickDate
            )
            PickedValueButton(
                value = timeLabel,
                contentDescription = "Pick time",
                onClick = onPickTime
            )
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
        if (selectedTime == null) {
            Text(
                EnvelopeHour.overnightCaption(envelopeHour),
                color = Ink.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        val fieldColors = readableOutlinedTextFieldColors()
        OutlinedTextField(
            value = title,
            onValueChange = onTitle, Modifier.fillMaxWidth(),
            placeholder = { Text("Title", color = Ink.copy(alpha = 0.45f)) },
            singleLine = true,
            colors = fieldColors
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = body,
                onValueChange = onBody, Modifier.weight(1f),
                placeholder = { Text("Message yourself…", color = Ink.copy(alpha = 0.45f)) },
                maxLines = 6,
                colors = fieldColors
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
                    placeholder = { Text("e.g. Errands", color = Ink.copy(alpha = 0.45f)) },
                    singleLine = true,
                    colors = readableOutlinedTextFieldColors()
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
private fun PickedValueButton(
    value: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            value,
            color = Ink,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = contentDescription, tint = WaterBlue)
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

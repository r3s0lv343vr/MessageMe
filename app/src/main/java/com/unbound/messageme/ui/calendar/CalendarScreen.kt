package com.unbound.messageme.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unbound.messageme.data.local.CalendarDayStatus
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import com.unbound.messageme.domain.CalendarColorLogic
import com.unbound.messageme.domain.NotificationCopy
import com.unbound.messageme.domain.SelfHonestyLogic
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.DayAcknowledged
import com.unbound.messageme.ui.theme.DayCompleted
import com.unbound.messageme.ui.theme.DayFree
import com.unbound.messageme.ui.theme.DayMixedBottom
import com.unbound.messageme.ui.theme.DayMixedTop
import com.unbound.messageme.ui.theme.DayOverdue
import com.unbound.messageme.ui.theme.DayPending
import com.unbound.messageme.ui.theme.DayUnopened
import com.unbound.messageme.ui.theme.Foam
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.WaterBlue
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class CalendarMode { MONTH, WEEK, DAY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    tasks: List<TaskEntity>,
    messages: List<ChatMessageEntity>,
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    onAcknowledge: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onOpenLetter: (ChatMessageEntity) -> Unit,
    onOpenTask: (String) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now(TimeDefaults.zoneId())) }
    var selectedDate by remember { mutableStateOf(LocalDate.now(TimeDefaults.zoneId())) }
    var mode by remember { mutableStateOf(CalendarMode.MONTH) }
    var filter by remember { mutableStateOf("all") }

    val weekHonesty = remember(messages, tasks, selectedDate) {
        SelfHonestyLogic.weekHonesty(messages, tasks, selectedDate)
    }
    val unopenedToday = remember(messages, tasks, selectedDate) {
        SelfHonestyLogic.unopenedLetters(messages, tasks, selectedDate)
    }
    val ackedToday = remember(messages, tasks, selectedDate) {
        SelfHonestyLogic.acknowledgedUnfinished(tasks, selectedDate)
    }
    val dayTasks = remember(tasks, selectedDate, filter) {
        tasks.filter {
            Instant.ofEpochMilli(it.dueAtEpochMillis)
                .atZone(TimeDefaults.zoneId())
                .toLocalDate() == selectedDate &&
                when (filter) {
                    "acked" -> it.status == TaskStatus.ACKNOWLEDGED
                    "done" -> it.status == TaskStatus.COMPLETED
                    "unopened" -> false
                    else -> true
                }
        }.sortedBy { it.dueAtEpochMillis }
    }
    val lettersToShow = if (filter == "all" || filter == "unopened") unopenedToday else emptyList()
    val tasksToShow = if (filter == "unopened") emptyList() else dayTasks

    WatercolorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Calendar", color = Ink) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    SelfHonestyLogic.WEEK_HEADLINE,
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    weekHonesty.body(),
                    color = Ink.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CalendarMode.MONTH, CalendarMode.WEEK, CalendarMode.DAY).forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m.name.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                when (mode) {
                    CalendarMode.MONTH -> {
                        MonthHeader(month, { month = month.minusMonths(1) }, { month = month.plusMonths(1) })
                        WeekdayHeader()
                        MonthGrid(month, tasks, messages, selectedDate) { selectedDate = it }
                    }
                    CalendarMode.WEEK -> WeekStrip(selectedDate, tasks, messages) { selectedDate = it }
                    CalendarMode.DAY -> Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy")),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ink
                    )
                }
                Spacer(Modifier.height(8.dp))
                LegendRow()
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("All") })
                    FilterChip(selected = filter == "unopened", onClick = { filter = "unopened" }, label = { Text("Unopened") })
                    FilterChip(selected = filter == "acked", onClick = { filter = "acked" }, label = { Text("Not finished") })
                    FilterChip(selected = filter == "done", onClick = { filter = "done" }, label = { Text("Done") })
                }
                Spacer(Modifier.height(8.dp))
                if (lettersToShow.isEmpty() && tasksToShow.isEmpty()) {
                    Text(
                        when (filter) {
                            "unopened" -> "No unopened letters on this day."
                            "acked" -> "Nothing acknowledged and unfinished on this day."
                            "done" -> "Nothing finished on this day."
                            else -> "No mail on this day."
                        },
                        color = Ink.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lettersToShow, key = { "letter-${it.id}" }) { letter ->
                            UnopenedLetterRow(letter, tasks) { onOpenLetter(letter) }
                        }
                        items(tasksToShow, key = { it.id }) { task ->
                            TaskRow(
                                task,
                                { onComplete(task.id) },
                                { onAcknowledge(task.id) },
                                { onDelete(task.id) },
                                { onEdit(task.id) },
                                { onOpenTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "Prev") }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.headlineMedium, color = Ink)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Ink.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    tasks: List<TaskEntity>,
    messages: List<ChatMessageEntity>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val sundayFirstOffset = firstDay.dayOfWeek.value % 7
    val cells = buildList {
        repeat(sundayFirstOffset) { add(null as LocalDate?) }
        for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
        while (size % 7 != 0) add(null)
    }
    Column {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val status = CalendarColorLogic.statusForDay(date, tasks, messages = messages)
                            Box(Modifier.size(36.dp)) {
                                DayCell(date.dayOfMonth, status, date == selectedDate) { onSelect(date) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(
    selected: LocalDate,
    tasks: List<TaskEntity>,
    messages: List<ChatMessageEntity>,
    onSelect: (LocalDate) -> Unit
) {
    val start = SelfHonestyLogic.weekStartSunday(selected)
    Row(Modifier.fillMaxWidth()) {
        (0L..6L).forEach { offset ->
            val date = start.plusDays(offset)
            val status = CalendarColorLogic.statusForDay(date, tasks, messages = messages)
            Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                DayCell(date.dayOfMonth, status, date == selected) { onSelect(date) }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, status: CalendarDayStatus, selected: Boolean, onClick: () -> Unit) {
    val brush = when (status) {
        CalendarDayStatus.MIXED -> Brush.verticalGradient(listOf(DayMixedTop, DayMixedBottom))
        CalendarDayStatus.FREE -> Brush.linearGradient(listOf(DayFree, DayFree))
        CalendarDayStatus.HAS_PENDING -> Brush.linearGradient(listOf(DayPending, DayPending))
        CalendarDayStatus.COMPLETED -> Brush.linearGradient(listOf(DayCompleted, DayCompleted))
        CalendarDayStatus.OVERDUE -> Brush.linearGradient(listOf(DayOverdue, DayOverdue))
        CalendarDayStatus.UNOPENED -> Brush.linearGradient(listOf(DayUnopened, DayUnopened))
        CalendarDayStatus.ACKNOWLEDGED_UNFINISHED -> Brush.linearGradient(listOf(DayAcknowledged, DayAcknowledged))
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(brush)
            .then(if (selected) Modifier.border(2.dp, Ink, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(day.toString(), color = Foam, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Legend(DayUnopened, "Unopened")
        Legend(DayAcknowledged, "Not finished")
        Legend(DayOverdue, "Still waiting")
        Legend(DayCompleted, "Done")
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(10.dp).padding(end = 4.dp).clip(CircleShape).background(color).padding(5.dp))
        Text(label, color = Ink.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun UnopenedLetterRow(
    message: ChatMessageEntity,
    tasks: List<TaskEntity>,
    onOpen: () -> Unit
) {
    val task = message.taskId?.let { id -> tasks.find { it.id == id } }
    val preview = when {
        task != null -> NotificationCopy.personalNote(task)
        else -> message.body.removePrefix("✉️").trim()
    }.ifBlank { "Letter from you" }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Foam.copy(alpha = 0.85f))
            .clickable(onClick = onOpen)
            .padding(12.dp)
    ) {
        Text("Unopened letter", color = DayUnopened, style = MaterialTheme.typography.labelMedium)
        Text(preview, color = Ink, style = MaterialTheme.typography.titleMedium, maxLines = 3)
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onComplete: () -> Unit,
    onAcknowledge: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onOpen: () -> Unit
) {
    val time = Instant.ofEpochMilli(task.dueAtEpochMillis)
        .atZone(TimeDefaults.zoneId())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("h:mm a"))
    val statusLabel = when (task.status) {
        TaskStatus.ACKNOWLEDGED -> "Acknowledged, not finished"
        TaskStatus.COMPLETED -> "Finished"
        TaskStatus.PENDING -> "Waiting"
        TaskStatus.SHELVED_UNACKNOWLEDGED -> "Still waiting"
        TaskStatus.NEEDS_RESCHEDULE -> "Needs a new day"
        TaskStatus.DISMISSED -> "Set aside"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Foam.copy(alpha = 0.85f))
            .padding(12.dp)
    ) {
        Text(
            task.title,
            color = Ink,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clickable(onClick = onOpen)
        )
        Text(
            "$time · $statusLabel",
            color = Ink.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        Row {
            if (task.status == TaskStatus.PENDING) {
                TextButton(onClick = onAcknowledge) { Text("Acknowledge", color = WaterBlue) }
            }
            if (task.status != TaskStatus.COMPLETED) {
                TextButton(onClick = onComplete) { Text("Mark finished", color = WaterBlue) }
                TextButton(onClick = onEdit) { Text("Edit", color = WaterBlue) }
            }
            TextButton(onClick = onDelete) { Text("Delete", color = DayOverdue) }
        }
    }
}

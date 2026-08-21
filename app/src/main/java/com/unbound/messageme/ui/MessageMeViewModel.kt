package com.unbound.messageme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unbound.messageme.data.export.BackupExporter
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.Recurrence
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.preferences.UserPreferences
import com.unbound.messageme.data.repository.MessageRepository
import com.unbound.messageme.data.sync.CloudSync
import com.unbound.messageme.domain.AiScheduleSuggestions
import com.unbound.messageme.domain.EnvelopeHour
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class UiNotice(val message: String, val isError: Boolean = false)

@HiltViewModel
class MessageMeViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val preferences: UserPreferences,
    private val exporter: BackupExporter,
    private val cloudSync: CloudSync
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = repository.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedCount = repository.observeCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val openCount = repository.observeOpenCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val notificationsEnabled = preferences.internalNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val themeMode = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    val envelopeHour = preferences.envelopeHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EnvelopeHour.DEFAULT)

    val seenEnvelopeHint = preferences.seenEnvelopeHint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hasAskedPermission = preferences.hasAskedNotificationPermission
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _notice = MutableStateFlow<UiNotice?>(null)
    val notice = _notice.asStateFlow()

    private val _lastExport = MutableStateFlow<File?>(null)
    val lastExport = _lastExport.asStateFlow()

    private val _pendingPermissionPrompt = MutableStateFlow(false)
    val pendingPermissionPrompt = _pendingPermissionPrompt.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask = _editingTask.asStateFlow()

    val firebaseConfigured: Boolean get() = cloudSync.isConfigured

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                repository.deliverOverdueReminders()
            }
        }
    }

    fun suggestions(): List<AiScheduleSuggestions.Suggestion> =
        AiScheduleSuggestions.suggest(tasks.value)

    fun createReminder(
        title: String,
        body: String,
        date: LocalDate,
        time: LocalTime?,
        priority: Priority,
        category: String,
        recurrence: Recurrence,
        customDays: Int?
    ) {
        viewModelScope.launch {
            repository.createReminder(title, body, date, time, priority, category, recurrence, customDays)
                .onSuccess {
                    _notice.value = UiNotice("Reminder scheduled")
                    if (time == null) preferences.setSeenEnvelopeHint(true)
                    maybePromptNotifications()
                }
                .onFailure { _notice.value = UiNotice(it.message ?: "Failed", isError = true) }
        }
    }

    fun editReminder(
        taskId: String,
        title: String,
        body: String,
        date: LocalDate,
        time: LocalTime?,
        priority: Priority,
        category: String,
        recurrence: Recurrence,
        customDays: Int?
    ) {
        viewModelScope.launch {
            repository.editReminder(taskId, title, body, date, time, priority, category, recurrence, customDays)
                .onSuccess {
                    _editingTask.value = null
                    _notice.value = UiNotice("Reminder updated")
                }
                .onFailure { _notice.value = UiNotice(it.message ?: "Update failed", true) }
        }
    }

    fun delete(taskId: String) = viewModelScope.launch { repository.deleteReminder(taskId) }
    fun acknowledge(taskId: String) = viewModelScope.launch { repository.acknowledge(taskId) }
    fun complete(taskId: String) = viewModelScope.launch { repository.markCompleted(taskId) }
    fun dismiss(taskId: String) = viewModelScope.launch { repository.dismiss(taskId) }
    fun reschedule(taskId: String, date: LocalDate, time: LocalTime?) =
        viewModelScope.launch { repository.reschedule(taskId, date, time) }
    fun snooze(taskId: String) = viewModelScope.launch { repository.snooze(taskId, 10) }

    fun beginEdit(taskId: String) {
        _editingTask.value = tasks.value.find { it.id == taskId && !it.deleted }
    }

    fun cancelEdit() {
        _editingTask.value = null
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setInternalNotificationsEnabled(enabled)
    }

    fun setThemeMode(mode: String) = viewModelScope.launch { preferences.setThemeMode(mode) }

    fun setEnvelopeHour(hour: Int, minute: Int) = viewModelScope.launch {
        repository.setEnvelopeHour(hour, minute)
    }

    fun markEnvelopeHintSeen() = viewModelScope.launch {
        preferences.setSeenEnvelopeHint(true)
    }

    fun markPermissionAsked() = viewModelScope.launch {
        preferences.setAskedNotificationPermission(true)
        _pendingPermissionPrompt.value = false
    }

    fun clearNotice() { _notice.value = null }

    fun markMessageRead(messageId: String) = viewModelScope.launch {
        repository.markMessageRead(messageId)
    }

    fun exportJson() = viewModelScope.launch {
        _lastExport.value = exporter.exportJson(tasks.value)
        _notice.value = UiNotice("JSON backup created")
    }

    fun exportCsv() = viewModelScope.launch {
        _lastExport.value = exporter.exportCsv(tasks.value)
        _notice.value = UiNotice("CSV export created")
    }

    fun exportPdf() = viewModelScope.launch {
        _lastExport.value = exporter.exportPdf(tasks.value)
        _notice.value = UiNotice("PDF export created")
    }

    fun importJson(json: String) = viewModelScope.launch {
        runCatching {
            val payload = exporter.importJson(json)
            repository.replaceAllFromBackup(payload.tasks)
        }.onSuccess {
            _notice.value = UiNotice("Backup restored")
        }.onFailure {
            _notice.value = UiNotice(it.message ?: "Restore failed", true)
        }
    }

    fun syncNow() = viewModelScope.launch {
        repository.syncNow()
            .onSuccess { _notice.value = UiNotice("Synced with Firebase") }
            .onFailure { _notice.value = UiNotice(it.message ?: "Sync failed", true) }
    }

    fun refreshAlarms() = viewModelScope.launch {
        repository.deliverOverdueReminders()
        repository.rescheduleAllPendingAlarms()
    }

    private suspend fun maybePromptNotifications() {
        if (!hasAskedPermission.value) {
            _pendingPermissionPrompt.value = true
        }
    }
}

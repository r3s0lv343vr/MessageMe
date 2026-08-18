package com.unbound.messageme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unbound.messageme.ui.MessageMeViewModel
import com.unbound.messageme.ui.calendar.CalendarScreen
import com.unbound.messageme.ui.chat.ChatScreen
import com.unbound.messageme.ui.settings.SettingsScreen
import com.unbound.messageme.ui.theme.MessageMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var showInAppPermissionDialog by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // User response handled; preferences already marked asked by ViewModel.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MessageMeViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MessageMeTheme(darkTheme = dark) {
                AppNav(
                    viewModel = viewModel,
                    systemNotificationsBlocked = !NotificationManagerCompat.from(this).areNotificationsEnabled(),
                    showPermissionDialog = showInAppPermissionDialog ||
                        viewModel.pendingPermissionPrompt.collectAsStateWithLifecycle().value,
                    onAllowNotifications = {
                        viewModel.markPermissionAsked()
                        showInAppPermissionDialog = false
                        requestSystemNotificationPermission()
                    },
                    onDeferNotifications = {
                        viewModel.markPermissionAsked()
                        showInAppPermissionDialog = false
                    }
                )
            }
        }

        // Restore alarms after process start
        // ViewModel refresh is triggered from composition via LaunchedEffect in AppNav
    }

    private fun requestSystemNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun AppNav(
    viewModel: MessageMeViewModel,
    systemNotificationsBlocked: Boolean,
    showPermissionDialog: Boolean,
    onAllowNotifications: () -> Unit,
    onDeferNotifications: () -> Unit
) {
    val navController = rememberNavController()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val openCount by viewModel.openCount.collectAsStateWithLifecycle()
    val lastExport by viewModel.lastExport.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshAlarms()
    }

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                messages = messages,
                tasks = tasks,
                editingTask = editingTask,
                suggestions = viewModel.suggestions(),
                onOpenCalendar = { navController.navigate("calendar") },
                onOpenSettings = { navController.navigate("settings") },
                onCreateReminder = { title, body, date, time, priority, category, recurrence ->
                    viewModel.createReminder(title, body, date, time, priority, category, recurrence, null)
                },
                onSaveEdit = { taskId, title, body, date, time, priority, category, recurrence ->
                    viewModel.editReminder(taskId, title, body, date, time, priority, category, recurrence, null)
                },
                onBeginEdit = viewModel::beginEdit,
                onCancelEdit = viewModel::cancelEdit,
                onAcknowledge = viewModel::acknowledge,
                onComplete = viewModel::complete,
                onDismiss = viewModel::dismiss,
                onReschedule = viewModel::reschedule,
                onSnooze = viewModel::snooze,
                onDelete = viewModel::delete
            )
        }
        composable("calendar") {
            CalendarScreen(
                tasks = tasks,
                completedCount = completedCount,
                openCount = openCount,
                onBack = { navController.popBackStack() },
                onComplete = viewModel::complete,
                onAcknowledge = viewModel::acknowledge,
                onDelete = viewModel::delete,
                onEdit = { taskId ->
                    viewModel.beginEdit(taskId)
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                notificationsEnabled = notificationsEnabled,
                systemNotificationsBlocked = systemNotificationsBlocked,
                themeMode = themeMode,
                firebaseConfigured = viewModel.firebaseConfigured,
                lastExport = lastExport,
                onBack = { navController.popBackStack() },
                onToggleNotifications = viewModel::setNotificationsEnabled,
                onThemeMode = viewModel::setThemeMode,
                onExportJson = viewModel::exportJson,
                onExportCsv = viewModel::exportCsv,
                onExportPdf = viewModel::exportPdf,
                onImportJson = viewModel::importJson,
                onSyncNow = viewModel::syncNow
            )
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = onDeferNotifications,
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_message)) },
            confirmButton = {
                TextButton(onClick = onAllowNotifications) {
                    Text(stringResource(R.string.permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeferNotifications) {
                    Text(stringResource(R.string.permission_not_now))
                }
            }
        )
    }

    notice?.let { n ->
        AlertDialog(
            onDismissRequest = viewModel::clearNotice,
            title = { Text(if (n.isError) "Notice" else "MessageMe") },
            text = { Text(n.message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearNotice) { Text("OK") }
            }
        )
    }
}

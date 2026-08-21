package com.unbound.messageme

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.unbound.messageme.domain.InboxLogic
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.notification.NotificationHelper
import com.unbound.messageme.ui.MessageMeViewModel
import com.unbound.messageme.ui.calendar.CalendarScreen
import com.unbound.messageme.ui.chat.ChatScreen
import com.unbound.messageme.ui.inbox.ReceivedDayScreen
import com.unbound.messageme.ui.inbox.ReceivedMessageScreen
import com.unbound.messageme.ui.settings.SettingsScreen
import com.unbound.messageme.ui.theme.MessageMeTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

data class OpenInboxTarget(val dateIso: String, val messageId: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var showInAppPermissionDialog by mutableStateOf(false)
    private var rewriteTaskId by mutableStateOf<String?>(null)
    private var openInbox by mutableStateOf<OpenInboxTarget?>(null)
    private var openReceivedDay by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // User response handled; preferences already marked asked by ViewModel.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeNotificationIntent(intent)

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
                    openInbox = openInbox,
                    onOpenInboxConsumed = { openInbox = null },
                    openReceivedDay = openReceivedDay,
                    onOpenReceivedDayConsumed = { openReceivedDay = null },
                    rewriteTaskId = rewriteTaskId,
                    onRewriteConsumed = { rewriteTaskId = null },
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)
        val messageId = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE_ID)
        val day = intent.getStringExtra(NotificationHelper.EXTRA_DAY)
            ?: LocalDate.now(TimeDefaults.zoneId()).toString()
        when (intent.action) {
            NotificationHelper.ACTION_REWRITE -> {
                if (taskId == null) return
                rewriteTaskId = taskId
                if (reminderId != null) {
                    NotificationHelper.cancel(this, taskId, reminderId)
                }
            }
            NotificationHelper.ACTION_OPEN -> {
                if (messageId != null) {
                    openInbox = OpenInboxTarget(dateIso = day, messageId = messageId)
                } else {
                    openReceivedDay = day
                }
            }
        }
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
    openInbox: OpenInboxTarget?,
    onOpenInboxConsumed: () -> Unit,
    openReceivedDay: String?,
    onOpenReceivedDayConsumed: () -> Unit,
    rewriteTaskId: String?,
    onRewriteConsumed: () -> Unit,
    onAllowNotifications: () -> Unit,
    onDeferNotifications: () -> Unit
) {
    val navController = rememberNavController()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val lastExport by viewModel.lastExport.collectAsStateWithLifecycle()
    val envelopeHour by viewModel.envelopeHour.collectAsStateWithLifecycle()
    val seenEnvelopeHint by viewModel.seenEnvelopeHint.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val today = LocalDate.now(TimeDefaults.zoneId()).toString()
    val showBottomBar = currentRoute == "scheduled" ||
        (currentRoute?.startsWith("received/") == true && currentRoute?.contains("/message/") != true)

    LaunchedEffect(Unit) {
        viewModel.refreshAlarms()
    }

    LaunchedEffect(rewriteTaskId) {
        val taskId = rewriteTaskId ?: return@LaunchedEffect
        viewModel.beginEdit(taskId)
        navController.navigate("scheduled") {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onRewriteConsumed()
    }

    LaunchedEffect(openInbox) {
        val target = openInbox ?: return@LaunchedEffect
        viewModel.markMessageRead(target.messageId)
        navController.navigate("received/${target.dateIso}") {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
        navController.navigate("received/${target.dateIso}/message/${target.messageId}")
        onOpenInboxConsumed()
    }

    LaunchedEffect(openReceivedDay) {
        val day = openReceivedDay ?: return@LaunchedEffect
        navController.navigate("received/$day") {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onOpenReceivedDayConsumed()
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "scheduled",
                        onClick = {
                            navController.navigate("scheduled") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        label = { Text("Scheduled") }
                    )
                    NavigationBarItem(
                        selected = currentRoute?.startsWith("received/") == true,
                        onClick = {
                            navController.navigate("received/$today") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                        label = { Text("Received") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "scheduled",
            modifier = Modifier.padding(padding)
        ) {
            composable("scheduled") {
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
                    onReschedule = viewModel::reschedule,
                    onSnooze = viewModel::snooze,
                    onDelete = viewModel::delete,
                    envelopeHour = envelopeHour,
                    showEnvelopeHint = !seenEnvelopeHint,
                    onDismissEnvelopeHint = viewModel::markEnvelopeHintSeen
                )
            }
            composable(
                "received/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { entry ->
                val date = LocalDate.parse(entry.arguments?.getString("date") ?: today)
                ReceivedDayScreen(
                    date = date,
                    messages = messages,
                    tasks = tasks,
                    onPrevDay = {
                        navController.navigate("received/${date.minusDays(1)}") {
                            popUpTo("received/{date}") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNextDay = {
                        navController.navigate("received/${date.plusDays(1)}") {
                            popUpTo("received/{date}") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenMessage = { message ->
                        viewModel.markMessageRead(message.id)
                        navController.navigate("received/$date/message/${message.id}")
                    },
                    onOpenCalendar = { navController.navigate("calendar") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable(
                "received/{date}/message/{messageId}",
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType },
                    navArgument("messageId") { type = NavType.StringType }
                )
            ) { entry ->
                val date = LocalDate.parse(entry.arguments?.getString("date") ?: today)
                val messageId = entry.arguments?.getString("messageId")
                val message = messages.find { it.id == messageId }
                val task = tasks.find { it.id == message?.taskId }
                LaunchedEffect(messageId) {
                    if (messageId != null) viewModel.markMessageRead(messageId)
                }
                ReceivedMessageScreen(
                    date = date,
                    message = message,
                    task = task,
                    onBack = { navController.popBackStack() },
                    onAcknowledge = viewModel::acknowledge,
                    onComplete = viewModel::complete,
                    onDismiss = viewModel::dismiss,
                    onReschedule = viewModel::reschedule,
                    onSnooze = viewModel::snooze,
                    onEdit = { taskId ->
                        viewModel.beginEdit(taskId)
                        navController.navigate("scheduled") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onDelete = viewModel::delete
                )
            }
            composable("calendar") {
                CalendarScreen(
                    tasks = tasks,
                    messages = messages,
                    onBack = { navController.popBackStack() },
                    onComplete = viewModel::complete,
                    onAcknowledge = viewModel::acknowledge,
                    onDelete = viewModel::delete,
                    onEdit = { taskId ->
                        viewModel.beginEdit(taskId)
                        navController.navigate("scheduled") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenLetter = { message ->
                        viewModel.markMessageRead(message.id)
                        val day = InboxLogic.dayOf(message).toString()
                        navController.navigate("received/$day") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                        navController.navigate("received/$day/message/${message.id}")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    notificationsEnabled = notificationsEnabled,
                    systemNotificationsBlocked = systemNotificationsBlocked,
                    themeMode = themeMode,
                    envelopeHour = envelopeHour,
                    firebaseConfigured = viewModel.firebaseConfigured,
                    lastExport = lastExport,
                    onBack = { navController.popBackStack() },
                    onToggleNotifications = viewModel::setNotificationsEnabled,
                    onThemeMode = viewModel::setThemeMode,
                    onEnvelopeHour = viewModel::setEnvelopeHour,
                    onExportJson = viewModel::exportJson,
                    onExportCsv = viewModel::exportCsv,
                    onExportPdf = viewModel::exportPdf,
                    onImportJson = viewModel::importJson,
                    onSyncNow = viewModel::syncNow
                )
            }
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

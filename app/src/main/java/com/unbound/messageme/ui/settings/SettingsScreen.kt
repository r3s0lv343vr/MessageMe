package com.unbound.messageme.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unbound.messageme.R
import com.unbound.messageme.domain.EnvelopeHour
import com.unbound.messageme.domain.OnboardingValidation
import com.unbound.messageme.domain.ProfileOnboarding
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.ui.theme.readableOutlinedTextFieldColors
import com.unbound.messageme.widget.UnreadLetterPin
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    systemNotificationsBlocked: Boolean,
    themeMode: String,
    envelopeHour: EnvelopeHour,
    firebaseConfigured: Boolean,
    lastExport: File?,
    firstName: String,
    lastName: String,
    email: String,
    onBack: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onAllowOsNotifications: () -> Unit,
    onThemeMode: (String) -> Unit,
    onEnvelopeHour: (Int, Int) -> Unit,
    onSaveProfile: (String, String, String) -> OnboardingValidation,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onImportJson: (String) -> Unit,
    onSyncNow: () -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var showEnvelopePicker by remember { mutableStateOf(false) }
    var editFirst by remember(firstName) { mutableStateOf(firstName) }
    var editLast by remember(lastName) { mutableStateOf(lastName) }
    var editEmail by remember(email) { mutableStateOf(email) }
    var firstError by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var profileSaved by remember { mutableStateOf(false) }
    val fieldColors = readableOutlinedTextFieldColors()

    WatercolorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", color = Ink) },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("You", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "No password. This is how MessageMe knows who the letters are from.",
                    color = Ink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = editFirst,
                    onValueChange = { editFirst = it; firstError = null; profileSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("First name") },
                    isError = firstError != null,
                    supportingText = firstError?.let { { Text(it) } },
                    singleLine = true,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = editLast,
                    onValueChange = { editLast = it; lastError = null; profileSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Last name") },
                    isError = lastError != null,
                    supportingText = lastError?.let { { Text(it) } },
                    singleLine = true,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it; emailError = null; profileSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    singleLine = true,
                    colors = fieldColors
                )
                Button(onClick = {
                    val result = ProfileOnboarding.validate(editFirst, editLast, editEmail)
                    firstError = result.firstNameError
                    lastError = result.lastNameError
                    emailError = result.emailError
                    if (result.ok) {
                        onSaveProfile(editFirst, editLast, editEmail)
                        profileSaved = true
                    }
                }) { Text("Save details") }
                if (profileSaved) {
                    Text("Saved.", color = Ink.copy(alpha = 0.7f))
                }

                Spacer(Modifier.height(8.dp))
                Text("Notifications", style = MaterialTheme.typography.titleLarge, color = Ink)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deliver letters as notifications", color = Ink)
                    Switch(checked = notificationsEnabled, onCheckedChange = onToggleNotifications)
                }
                Text(
                    "Recommended on, so overnight letters reach your lock screen. Turn this off here any time.",
                    color = Ink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (systemNotificationsBlocked) {
                    Text(
                        "Your phone is still blocking MessageMe notifications.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onAllowOsNotifications) { Text("Allow notifications") }
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }) { Text("Open phone Settings") }
                }

                Spacer(Modifier.height(8.dp))
                Text("Envelope hour", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "When overnight letters arrive if you don’t pick a time.",
                    color = Ink.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { showEnvelopePicker = true }) {
                    Text(envelopeHour.clockLabel())
                }

                Spacer(Modifier.height(8.dp))
                Text("Home screen", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "The unread letter is a widget, not the round blue app icon. Add it here if it does not appear under Widgets.",
                    color = Ink.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { UnreadLetterPin.requestPin(context) }) {
                    Text(stringResource(R.string.widget_add_to_home))
                }

                Spacer(Modifier.height(8.dp))
                Text("Theme", style = MaterialTheme.typography.titleLarge, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "light", "dark").forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            label = { Text(mode.replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Backup & export", style = MaterialTheme.typography.titleLarge, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExportJson) { Text("JSON") }
                    Button(onClick = onExportCsv) { Text("CSV") }
                    Button(onClick = onExportPdf) { Text("PDF") }
                }
                if (lastExport != null) {
                    Text("Last export: ${lastExport.absolutePath}", color = Ink.copy(alpha = 0.7f))
                }
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it }, Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = { Text("Paste JSON backup to restore") },
                    colors = fieldColors
                )
                Button(
                    onClick = { onImportJson(importText); importText = "" },
                    enabled = importText.isNotBlank()
                ) { Text("Restore from JSON") }

                Spacer(Modifier.height(8.dp))
                Text("Cloud sync", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    if (firebaseConfigured) {
                        "Firebase is configured. Sync pushes/pulls Room tasks via Firestore."
                    } else {
                        "Firebase not configured. Add app/google-services.json from your Firebase Console to enable Auth + Firestore sync. The app remains fully offline until then."
                    },
                    color = Ink.copy(alpha = 0.75f)
                )
                Button(onClick = onSyncNow, enabled = firebaseConfigured) { Text("Sync now") }
            }
        }
    }

    if (showEnvelopePicker) {
        val timeState = rememberTimePickerState(
            initialHour = envelopeHour.hour,
            initialMinute = envelopeHour.minute
        )
        AlertDialog(
            onDismissRequest = { showEnvelopePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEnvelopeHour(timeState.hour, timeState.minute)
                    showEnvelopePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEnvelopePicker = false }) { Text("Cancel") }
            },
            title = { Text("Envelope hour") },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}

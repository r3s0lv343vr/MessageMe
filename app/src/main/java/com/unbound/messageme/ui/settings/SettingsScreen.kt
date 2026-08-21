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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.unbound.messageme.ui.components.WatercolorBackground
import com.unbound.messageme.ui.theme.Ink
import com.unbound.messageme.widget.UnreadLetterPin
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    systemNotificationsBlocked: Boolean,
    themeMode: String,
    firebaseConfigured: Boolean,
    lastExport: File?,
    onBack: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onThemeMode: (String) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onImportJson: (String) -> Unit,
    onSyncNow: () -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

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
                Text("Notifications", style = MaterialTheme.typography.titleLarge, color = Ink)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Internal notifications", color = Ink)
                    Switch(checked = notificationsEnabled, onCheckedChange = onToggleNotifications)
                }
                Text(
                    "When off, reminders stay saved but delivery pauses.",
                    color = Ink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (systemNotificationsBlocked) {
                    Text(
                        "Notifications are blocked in your phone settings. Enable them to receive your scheduled messages.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }) { Text("Open Settings") }
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
                    placeholder = { Text("Paste JSON backup to restore") }
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
}

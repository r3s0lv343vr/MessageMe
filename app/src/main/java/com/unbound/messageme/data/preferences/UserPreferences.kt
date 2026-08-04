package com.unbound.messageme.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("messageme_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationsEnabledKey = booleanPreferencesKey("internal_notifications_enabled")
    private val askedPermissionKey = booleanPreferencesKey("asked_notification_permission")
    private val themeKey = stringPreferencesKey("theme_mode") // system|light|dark
    private val cloudBackupKey = booleanPreferencesKey("cloud_backup_enabled")

    val internalNotificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsEnabledKey] ?: true }

    val hasAskedNotificationPermission: Flow<Boolean> =
        context.dataStore.data.map { it[askedPermissionKey] ?: false }

    val themeMode: Flow<String> =
        context.dataStore.data.map { it[themeKey] ?: "system" }

    val cloudBackupEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[cloudBackupKey] ?: false }

    suspend fun setInternalNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsEnabledKey] = enabled }
    }

    suspend fun setAskedNotificationPermission(asked: Boolean) {
        context.dataStore.edit { it[askedPermissionKey] = asked }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeKey] = mode }
    }

    suspend fun setCloudBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[cloudBackupKey] = enabled }
    }
}

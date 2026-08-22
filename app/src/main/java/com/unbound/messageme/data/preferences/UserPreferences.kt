package com.unbound.messageme.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unbound.messageme.domain.EnvelopeHour
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
    private val envelopeHourKey = intPreferencesKey("envelope_hour")
    private val envelopeMinuteKey = intPreferencesKey("envelope_minute")
    private val seenEnvelopeHintKey = booleanPreferencesKey("seen_envelope_hint")
    private val firstNameKey = stringPreferencesKey("profile_first_name")
    private val lastNameKey = stringPreferencesKey("profile_last_name")
    private val emailKey = stringPreferencesKey("profile_email")
    private val onboardedKey = booleanPreferencesKey("profile_onboarded")

    val internalNotificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsEnabledKey] ?: true }

    val hasAskedNotificationPermission: Flow<Boolean> =
        context.dataStore.data.map { it[askedPermissionKey] ?: false }

    val themeMode: Flow<String> =
        context.dataStore.data.map { it[themeKey] ?: "system" }

    val cloudBackupEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[cloudBackupKey] ?: false }

    val envelopeHour: Flow<EnvelopeHour> =
        context.dataStore.data.map {
            EnvelopeHour(
                hour = it[envelopeHourKey] ?: EnvelopeHour.DEFAULT_HOUR,
                minute = it[envelopeMinuteKey] ?: EnvelopeHour.DEFAULT_MINUTE
            )
        }

    val seenEnvelopeHint: Flow<Boolean> =
        context.dataStore.data.map { it[seenEnvelopeHintKey] ?: false }

    val firstName: Flow<String> =
        context.dataStore.data.map { it[firstNameKey] ?: "" }

    val lastName: Flow<String> =
        context.dataStore.data.map { it[lastNameKey] ?: "" }

    val email: Flow<String> =
        context.dataStore.data.map { it[emailKey] ?: "" }

    val hasCompletedOnboarding: Flow<Boolean> =
        context.dataStore.data.map { it[onboardedKey] ?: false }

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

    suspend fun setEnvelopeHour(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[envelopeHourKey] = hour.coerceIn(0, 23)
            it[envelopeMinuteKey] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setSeenEnvelopeHint(seen: Boolean) {
        context.dataStore.edit { it[seenEnvelopeHintKey] = seen }
    }

    suspend fun setOnboardingProfile(firstName: String, lastName: String, email: String) {
        context.dataStore.edit {
            it[firstNameKey] = firstName
            it[lastNameKey] = lastName
            it[emailKey] = email
            it[onboardedKey] = true
        }
    }
}

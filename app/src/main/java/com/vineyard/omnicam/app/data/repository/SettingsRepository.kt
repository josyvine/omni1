package com.vineyard.omnicam.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vineyard.omnicam.app.core.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omnicam_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_AUTO_CLEANUP_ENABLED = booleanPreferencesKey("auto_cleanup_enabled")
        val KEY_AUTO_CLEANUP_DAYS = intPreferencesKey("auto_cleanup_days")
        val KEY_MAX_CLIP_DURATION = intPreferencesKey("max_clip_duration")
        val KEY_CUSTOM_FIREBASE_JSON = stringPreferencesKey("custom_firebase_json")
        val KEY_ACTIVE_GUEST_SHARE_TOKEN = stringPreferencesKey("active_guest_share_token")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val isAutoCleanupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CLEANUP_ENABLED] ?: true
    }

    val autoCleanupDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CLEANUP_DAYS] ?: 30
    }

    val maxClipDuration: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAX_CLIP_DURATION] ?: 20
    }

    val customFirebaseJson: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_FIREBASE_JSON]
    }

    val activeGuestShareToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_GUEST_SHARE_TOKEN]
    }

    /**
     * Synchronous getter for active guest token (used during app launch and auth restore).
     */
    fun getActiveGuestShareToken(): String? = runBlocking(Dispatchers.IO) {
        try {
            context.dataStore.data.firstOrNull()?.get(KEY_ACTIVE_GUEST_SHARE_TOKEN)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves or clears the active guest token.
     */
    fun saveActiveGuestShareToken(token: String) {
        runBlocking(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                if (token.isBlank()) {
                    prefs.remove(KEY_ACTIVE_GUEST_SHARE_TOKEN)
                } else {
                    prefs[KEY_ACTIVE_GUEST_SHARE_TOKEN] = token
                }
            }
        }
    }

    /**
     * Synchronous getter for the custom Firebase JSON configuration (used by QR generators).
     */
    fun getCustomFirebaseJson(): String? = runBlocking(Dispatchers.IO) {
        try {
            context.dataStore.data.firstOrNull()?.get(KEY_CUSTOM_FIREBASE_JSON)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setAutoCleanupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_CLEANUP_ENABLED] = enabled
        }
    }

    suspend fun setAutoCleanupDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_CLEANUP_DAYS] = days
        }
    }

    suspend fun setMaxClipDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_CLIP_DURATION] = seconds
        }
    }

    suspend fun saveCustomFirebaseJson(json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_FIREBASE_JSON] = json
        }
    }

    suspend fun clearCustomFirebaseJson() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_CUSTOM_FIREBASE_JSON)
        }
    }
}
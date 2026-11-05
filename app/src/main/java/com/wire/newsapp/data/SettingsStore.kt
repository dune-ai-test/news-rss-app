package com.wire.newsapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wire_settings")

/**
 * Persists user preferences: appearance mode, disabled feed ids, last sync time.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")        // system | light | dark
        val DISABLED_FEEDS = stringSetPreferencesKey("disabled_feeds")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    val disabledFeedIds: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.DISABLED_FEEDS] ?: emptySet() }

    val lastSync: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setFeedEnabled(id: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DISABLED_FEEDS] ?: emptySet()
            prefs[Keys.DISABLED_FEEDS] =
                if (enabled) current - id else current + id
        }
    }

    suspend fun setLastSync(timeMillis: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = timeMillis }
    }
}

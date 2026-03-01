package com.cedd.budgettracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DARK_MODE_KEY       = booleanPreferencesKey("dark_mode")
    private val REMINDER_ENABLED    = booleanPreferencesKey("reminder_enabled")
    private val REMINDER_HOUR       = intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE     = intPreferencesKey("reminder_minute")

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_MODE_KEY] ?: false }

    val isReminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[REMINDER_ENABLED] ?: false }

    val reminderHour: Flow<Int> = context.dataStore.data
        .map { it[REMINDER_HOUR] ?: 20 }   // default 8 PM

    val reminderMinute: Flow<Int> = context.dataStore.data
        .map { it[REMINDER_MINUTE] ?: 0 }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        context.dataStore.edit {
            it[REMINDER_ENABLED] = enabled
            it[REMINDER_HOUR]    = hour
            it[REMINDER_MINUTE]  = minute
        }
    }
}

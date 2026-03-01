package com.cedd.budgettracker.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.budgettracker.data.preferences.PreferencesRepository
import com.cedd.budgettracker.notification.BudgetReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = prefsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isReminderEnabled: StateFlow<Boolean> = prefsRepository.isReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val reminderHour: StateFlow<Int> = prefsRepository.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 20)

    val reminderMinute: StateFlow<Int> = prefsRepository.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDarkMode(enabled) }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepository.setReminder(enabled, hour, minute)
            if (enabled) BudgetReminderWorker.schedule(context, hour, minute)
            else BudgetReminderWorker.cancel(context)
        }
    }
}

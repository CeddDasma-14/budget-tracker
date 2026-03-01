package com.cedd.budgettracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import com.cedd.budgettracker.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<BudgetSessionWithExpenses> = emptyList(),
    val isLoading: Boolean = true,
    /** IDs of sessions whose expense list is currently expanded in the UI. */
    val expandedSessionIds: Set<Long> = emptySet(),
    val deleteConfirmSessionId: Long? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            repository.getAllSessionsWithExpenses()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )
                .collect { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                }
        }
    }

    /** Toggles the expanded/collapsed state for a session card. */
    fun toggleExpand(sessionId: Long) {
        _uiState.update { state ->
            val ids = state.expandedSessionIds.toMutableSet()
            if (ids.contains(sessionId)) ids.remove(sessionId) else ids.add(sessionId)
            state.copy(expandedSessionIds = ids)
        }
    }

    fun requestDeleteSession(sessionId: Long) {
        _uiState.update { it.copy(deleteConfirmSessionId = sessionId) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmSessionId = null) }
    }

    fun confirmDeleteSession() {
        val sessionId = _uiState.value.deleteConfirmSessionId ?: return
        viewModelScope.launch {
            val target = _uiState.value.sessions.firstOrNull { it.session.id == sessionId } ?: return@launch
            target.expenses.forEach { expense ->
                expense.receiptPath?.let { repository.deleteReceiptImage(it) }
            }
            repository.deleteSession(target.session)
        }
        _uiState.update { it.copy(deleteConfirmSessionId = null) }
    }

    fun exportCsv(sessionId: Long) {
        viewModelScope.launch {
            val target = _uiState.value.sessions.firstOrNull { it.session.id == sessionId } ?: return@launch
            val uri = repository.exportSessionToCsv(target)
            if (uri != null) repository.shareCsvUri(uri)
        }
    }
}

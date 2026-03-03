package com.cedd.budgettracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
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
import java.util.Calendar
import javax.inject.Inject

/** Year + Month (1-indexed) pair used for grouping and filtering. */
data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth): Int =
        compareValuesBy(this, other, { it.year }, { it.month })
}

data class MonthSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val totalRemaining: Double,
    val entryCount: Int
)

data class HistoryUiState(
    val allSessions: List<BudgetSessionWithExpenses> = emptyList(),
    val isLoading: Boolean = true,
    val expandedSessionIds: Set<Long> = emptySet(),
    val deleteConfirmSessionId: Long? = null,
    /** Currently selected month filter. Null = show all. */
    val selectedYearMonth: YearMonth? = null,
    /** Search query for filtering sessions by name. */
    val searchQuery: String = "",
    /** Non-null while snackbar message is shown (templates OR edit saves). */
    val templateSavedMessage: String? = null,
    /** ID of the session currently open in the edit sheet. Null = sheet hidden. */
    val editTargetSessionId: Long? = null,
    val editSessionName: String = "",
    val editInitialBudget: String = ""
) {
    /** Live view of the edit target — auto-refreshes as DB changes propagate. */
    val editTargetSession: BudgetSessionWithExpenses?
        get() = editTargetSessionId?.let { id -> allSessions.firstOrNull { it.session.id == id } }

    /** Effective date for a session: budgetDate if set (>0), otherwise createdAt. */
    private fun effectiveDate(s: BudgetSessionWithExpenses): Long =
        if (s.session.budgetDate != 0L) s.session.budgetDate else s.session.createdAt

    private fun sessionYearMonth(s: BudgetSessionWithExpenses): YearMonth {
        val cal = Calendar.getInstance().apply { timeInMillis = effectiveDate(s) }
        return YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    /** All unique year-months present in history, newest first. */
    val availableMonths: List<YearMonth>
        get() = allSessions.map { sessionYearMonth(it) }
            .distinct()
            .sortedDescending()

    /** Sessions visible after applying month filter and search query. */
    val sessions: List<BudgetSessionWithExpenses>
        get() = allSessions
            .let { list ->
                if (selectedYearMonth == null) list
                else list.filter { sessionYearMonth(it) == selectedYearMonth }
            }
            .let { list ->
                if (searchQuery.isBlank()) list
                else list.filter { it.session.name.contains(searchQuery, ignoreCase = true) }
            }

    /** Combined totals for the selected month. Null when no filter active or no sessions match. */
    val monthlySummary: MonthSummary?
        get() {
            val filtered = sessions
            if (selectedYearMonth == null || filtered.isEmpty()) return null
            val totalBudget = filtered.sumOf { it.session.initialBudget }
            val totalSpent  = filtered.sumOf { s -> s.expenses.sumOf { it.amount } }
            return MonthSummary(
                totalBudget    = totalBudget,
                totalSpent     = totalSpent,
                totalRemaining = totalBudget - totalSpent,
                entryCount     = filtered.size
            )
        }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init { observeSessions() }

    private fun observeSessions() {
        viewModelScope.launch {
            repository.getAllSessionsWithExpenses()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )
                .collect { sessions ->
                    _uiState.update { it.copy(allSessions = sessions, isLoading = false) }
                }
        }
    }

    fun toggleExpand(sessionId: Long) {
        _uiState.update { state ->
            val ids = state.expandedSessionIds.toMutableSet()
            if (ids.contains(sessionId)) ids.remove(sessionId) else ids.add(sessionId)
            state.copy(expandedSessionIds = ids)
        }
    }

    /** Tap same month again to deselect (toggle). */
    fun selectMonth(ym: YearMonth) {
        _uiState.update { it.copy(selectedYearMonth = if (it.selectedYearMonth == ym) null else ym) }
    }

    fun clearMonthFilter() { _uiState.update { it.copy(selectedYearMonth = null) } }

    fun updateSearch(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    fun requestDeleteSession(sessionId: Long) {
        _uiState.update { it.copy(deleteConfirmSessionId = sessionId) }
    }

    fun cancelDelete() { _uiState.update { it.copy(deleteConfirmSessionId = null) } }

    fun confirmDeleteSession() {
        val sessionId = _uiState.value.deleteConfirmSessionId ?: return
        viewModelScope.launch {
            val target = _uiState.value.allSessions.firstOrNull { it.session.id == sessionId } ?: return@launch
            target.expenses.forEach { expense ->
                expense.receiptPath?.let { repository.deleteReceiptImage(it) }
            }
            repository.deleteSession(target.session)
        }
        _uiState.update { it.copy(deleteConfirmSessionId = null) }
    }

    fun exportCsv(sessionId: Long) {
        viewModelScope.launch {
            val target = _uiState.value.allSessions.firstOrNull { it.session.id == sessionId } ?: return@launch
            val uri = repository.exportSessionToCsv(target)
            if (uri != null) repository.shareCsvUri(uri)
        }
    }

    /** Saves the session's expenses as a reusable template, then shows a confirmation snackbar. */
    fun saveSessionAsTemplate(sessionId: Long) {
        viewModelScope.launch {
            val target = _uiState.value.allSessions.firstOrNull { it.session.id == sessionId } ?: return@launch
            val templateExpenses = target.expenses.map { e ->
                ExpenseEntity(
                    sessionId = 0L,
                    title = e.title,
                    amount = e.amount,
                    category = e.category,
                    isRecurring = e.isRecurring
                )
            }
            repository.saveTemplate(target.session.name, target.session.initialBudget, templateExpenses)
            _uiState.update { it.copy(templateSavedMessage = "\"${target.session.name}\" saved as template!") }
        }
    }

    fun clearTemplateSavedMessage() { _uiState.update { it.copy(templateSavedMessage = null) } }

    // ── Edit session (bottom sheet) ───────────────────────────────────────────

    fun showEditSheet(session: BudgetSessionWithExpenses) {
        val budgetStr = session.session.initialBudget
            .toBigDecimal().stripTrailingZeros().toPlainString()
        _uiState.update { it.copy(
            editTargetSessionId = session.session.id,
            editSessionName     = session.session.name,
            editInitialBudget   = budgetStr
        ) }
    }

    fun hideEditSheet() {
        _uiState.update { it.copy(
            editTargetSessionId = null,
            editSessionName     = "",
            editInitialBudget   = ""
        ) }
    }

    fun updateEditName(name: String) { _uiState.update { it.copy(editSessionName = name) } }

    fun updateEditBudget(amount: String) { _uiState.update { it.copy(editInitialBudget = amount) } }

    fun saveEditSession() {
        val target = _uiState.value.editTargetSession ?: return
        val name   = _uiState.value.editSessionName.trim()
        val budget = _uiState.value.editInitialBudget.replace(",", "").toDoubleOrNull() ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateSession(
                target.session.copy(
                    name          = name,
                    initialBudget = budget,
                    updatedAt     = System.currentTimeMillis()
                )
            )
            // Close the sheet and show a visible Snackbar confirmation
            _uiState.update { it.copy(
                editTargetSessionId  = null,
                editSessionName      = "",
                editInitialBudget    = "",
                templateSavedMessage = "\"$name\" updated successfully!"
            ) }
        }
    }

    fun addExpenseToSession(title: String, amount: Double, category: String) {
        val sessionId = _uiState.value.editTargetSessionId ?: return
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch {
            repository.saveExpenses(listOf(
                ExpenseEntity(sessionId = sessionId, title = title.trim(), amount = amount, category = category)
            ))
        }
    }

    fun toggleExpensePaidInSession(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.saveExpenses(listOf(expense.copy(isPaid = !expense.isPaid)))
        }
    }

    fun deleteExpenseInSession(expense: ExpenseEntity) {
        viewModelScope.launch {
            expense.receiptPath?.let { repository.deleteReceiptImage(it) }
            repository.deleteExpense(expense)
        }
    }

}

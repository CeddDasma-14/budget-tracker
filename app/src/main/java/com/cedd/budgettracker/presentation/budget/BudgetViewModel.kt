package com.cedd.budgettracker.presentation.budget

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.budgettracker.data.local.entity.BudgetSessionEntity
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
import com.cedd.budgettracker.data.local.relation.TemplateWithExpenses
import com.cedd.budgettracker.data.repository.BudgetRepository
import com.cedd.budgettracker.domain.model.BudgetUiState
import com.cedd.budgettracker.domain.model.ExpenseCategory
import com.cedd.budgettracker.domain.model.ExpenseSortOrder
import com.cedd.budgettracker.domain.model.ExpenseUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    val templates: StateFlow<List<TemplateWithExpenses>> = repository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Session ───────────────────────────────────────────────────────────────

    fun updateSessionName(name: String) {
        _uiState.update { it.copy(sessionName = name) }
    }

    fun updateSelectedDate(dateMillis: Long) {
        _uiState.update { it.copy(selectedDate = dateMillis) }
    }

    fun updateGoalAmount(raw: String) {
        _uiState.update { it.copy(goalAmount = raw) }
    }

    // ── Initial budget ────────────────────────────────────────────────────────

    fun updateInitialBudget(raw: String) {
        _uiState.update { state ->
            val budget = raw.replace(",", "").toDoubleOrNull() ?: 0.0
            state.copy(
                initialBudget = raw,
                remainingBalance = budget - state.expenses.sumOf { it.amountAsDouble }
            )
        }
    }

    // ── Expense rows ──────────────────────────────────────────────────────────

    fun addExpenseRow() {
        _uiState.update { state ->
            val locked = state.expenses.map { expense ->
                if (expense.hasContent && !expense.isLocked) expense.copy(isLocked = true)
                else expense
            }
            state.copy(expenses = locked + ExpenseUiModel())
        }
    }

    fun toggleExpenseLock(index: Int) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(isLocked = !updated[index].isLocked)
            state.copy(expenses = updated)
        }
    }

    fun removeExpenseRow(index: Int) {
        _uiState.update { state ->
            if (state.expenses.size <= 1) return@update state
            val deleted = state.expenses[index]
            val updated = state.expenses.toMutableList().also { it.removeAt(index) }
            state.copy(
                expenses = updated,
                remainingBalance = budgetValue(state) - updated.sumOf { it.amountAsDouble },
                recentlyDeletedExpense = deleted
            )
        }
    }

    fun undoDelete() {
        _uiState.update { state ->
            val deleted = state.recentlyDeletedExpense ?: return@update state
            val updated = state.expenses + deleted.copy(isLocked = true)
            state.copy(
                expenses = updated,
                remainingBalance = budgetValue(state) - updated.sumOf { it.amountAsDouble },
                recentlyDeletedExpense = null
            )
        }
    }

    fun clearRecentlyDeleted() {
        _uiState.update { it.copy(recentlyDeletedExpense = null) }
    }

    fun updateExpenseTitle(index: Int, title: String) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(title = title)
            state.copy(expenses = updated)
        }
    }

    fun updateExpenseAmount(index: Int, amount: String) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(amount = amount)
            state.copy(
                expenses = updated,
                remainingBalance = budgetValue(state) - updated.sumOf { it.amountAsDouble }
            )
        }
    }

    fun updateExpenseNotes(index: Int, notes: String) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(notes = notes)
            state.copy(expenses = updated)
        }
    }

    fun togglePaid(index: Int) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(isPaid = !updated[index].isPaid)
            state.copy(expenses = updated)
        }
    }

    fun updateExpenseCategory(index: Int, category: ExpenseCategory) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(category = category)
            state.copy(expenses = updated)
        }
    }

    fun toggleRecurring(index: Int) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index] = updated[index].copy(isRecurring = !updated[index].isRecurring)
            state.copy(expenses = updated)
        }
    }

    fun sortExpenses(order: ExpenseSortOrder) {
        _uiState.update { state ->
            val sorted = state.expenses.sortedWith(
                when (order) {
                    ExpenseSortOrder.AMOUNT_DESC -> compareByDescending { it.amountAsDouble }
                    ExpenseSortOrder.AMOUNT_ASC -> compareBy { it.amountAsDouble }
                    ExpenseSortOrder.CATEGORY -> compareBy { it.category.label }
                    ExpenseSortOrder.PAID_FIRST -> compareByDescending { it.isPaid }
                }
            )
            state.copy(expenses = sorted)
        }
    }

    // ── Clear session ─────────────────────────────────────────────────────────

    fun requestClearSession() { _uiState.update { it.copy(showClearConfirmDialog = true) } }
    fun dismissClearDialog()  { _uiState.update { it.copy(showClearConfirmDialog = false) } }
    fun confirmClearSession() { _uiState.value = BudgetUiState() }

    // ── Receipt handling ──────────────────────────────────────────────────────

    fun handleGlobalReceiptPicked(uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveReceiptImage(uri) ?: return@launch
            _uiState.update { it.copy(pendingReceiptPath = path) }
        }
    }

    fun handleRowReceiptPicked(expenseIndex: Int, uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveReceiptImage(uri) ?: return@launch
            _uiState.update { state ->
                val updated = state.expenses.toMutableList()
                updated[expenseIndex].receiptPath?.let { repository.deleteReceiptImage(it) }
                updated[expenseIndex] = updated[expenseIndex].copy(receiptPath = path)
                state.copy(expenses = updated)
            }
        }
    }

    fun assignPendingReceiptToExpense(expenseIndex: Int) {
        val pendingPath = _uiState.value.pendingReceiptPath ?: return
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[expenseIndex].receiptPath?.let { repository.deleteReceiptImage(it) }
            updated[expenseIndex] = updated[expenseIndex].copy(receiptPath = pendingPath)
            state.copy(expenses = updated, pendingReceiptPath = null)
        }
    }

    fun dismissReceiptAssignment() {
        val pendingPath = _uiState.value.pendingReceiptPath
        if (pendingPath != null) repository.deleteReceiptImage(pendingPath)
        _uiState.update { it.copy(pendingReceiptPath = null) }
    }

    fun removeReceiptFromExpense(index: Int) {
        _uiState.update { state ->
            val updated = state.expenses.toMutableList()
            updated[index].receiptPath?.let { repository.deleteReceiptImage(it) }
            updated[index] = updated[index].copy(receiptPath = null)
            state.copy(expenses = updated)
        }
    }

    // ── OCR ───────────────────────────────────────────────────────────────────

    fun triggerOcr(expenseIndex: Int, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOcrRunning = true) }
            val amount = repository.recognizeAmountFromImage(uri)
            _uiState.update { state ->
                if (amount != null) {
                    val updated = state.expenses.toMutableList()
                    updated[expenseIndex] = updated[expenseIndex].copy(amount = amount)
                    state.copy(
                        expenses = updated,
                        isOcrRunning = false,
                        remainingBalance = budgetValue(state) - updated.sumOf { it.amountAsDouble }
                    )
                } else {
                    state.copy(isOcrRunning = false)
                }
            }
        }
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    fun showTemplateDialog() { _uiState.update { it.copy(showTemplateDialog = true) } }
    fun hideTemplateDialog() { _uiState.update { it.copy(showTemplateDialog = false) } }

    fun loadTemplate(template: TemplateWithExpenses) {
        val expenses = template.expenses.map { te ->
            ExpenseUiModel(
                title = te.title,
                amount = te.amount.toString(),
                category = ExpenseCategory.fromName(te.category),
                isRecurring = te.isRecurring,
                isLocked = true
            )
        }
        val budget = template.template.initialBudget
        _uiState.update { state ->
            state.copy(
                initialBudget = budget.toString(),
                expenses = expenses + ExpenseUiModel(),
                remainingBalance = budget - expenses.sumOf { it.amountAsDouble },
                showTemplateDialog = false
            )
        }
    }

    fun showSaveTemplateDialog() { _uiState.update { it.copy(showSaveTemplateDialog = true) } }
    fun hideSaveTemplateDialog() { _uiState.update { it.copy(showSaveTemplateDialog = false) } }

    fun saveCurrentAsTemplate(name: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val budget = budgetValue(state)
            val expenseEntities = state.expenses.filter { it.hasContent }.map { expense ->
                ExpenseEntity(
                    sessionId = 0L,
                    title = expense.title,
                    amount = expense.amountAsDouble,
                    category = expense.category.name,
                    isRecurring = expense.isRecurring
                )
            }
            repository.saveTemplate(name.ifBlank { "My Template" }, budget, expenseEntities)
            _uiState.update { it.copy(showSaveTemplateDialog = false) }
        }
    }

    // ── Persist ───────────────────────────────────────────────────────────────

    fun saveSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, savedSuccessfully = false, errorMessage = null) }
            try {
                val state = _uiState.value
                val defaultName = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

                val sessionEntity = BudgetSessionEntity(
                    id = state.sessionId,
                    name = state.sessionName.ifBlank { defaultName },
                    initialBudget = state.initialBudget.replace(",", "").toDoubleOrNull() ?: 0.0,
                    updatedAt = System.currentTimeMillis(),
                    budgetDate = state.selectedDate,
                    goalAmount = state.goalAmount.replace(",", "").toDoubleOrNull() ?: 0.0
                )

                val newSessionId = repository.saveSession(sessionEntity)

                val expenseEntities = state.expenses
                    .filter { it.title.isNotBlank() || it.amountAsDouble > 0 }
                    .map { expense ->
                        ExpenseEntity(
                            id = expense.id,
                            sessionId = newSessionId,
                            title = expense.title,
                            amount = expense.amountAsDouble,
                            isPaid = expense.isPaid,
                            receiptPath = expense.receiptPath,
                            category = expense.category.name,
                            isRecurring = expense.isRecurring,
                            notes = expense.notes
                        )
                    }

                repository.saveExpenses(expenseEntities)

                _uiState.update {
                    it.copy(isSaving = false, savedSuccessfully = true, sessionId = newSessionId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Save failed")
                }
            }
        }
    }

    fun resetSession() {
        _uiState.value = BudgetUiState()
    }

    fun clearSavedFlag() {
        _uiState.update { it.copy(savedSuccessfully = false) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun budgetValue(state: BudgetUiState): Double =
        state.initialBudget.replace(",", "").toDoubleOrNull() ?: 0.0
}

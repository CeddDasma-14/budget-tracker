package com.cedd.budgettracker.domain.model

enum class ExpenseSortOrder { AMOUNT_DESC, AMOUNT_ASC, CATEGORY, PAID_FIRST }

/**
 * Single source of truth for the active budget editor screen.
 *
 * [remainingBalance] is derived — it is never set directly but is recalculated
 * inside the ViewModel whenever [initialBudget] or any expense [amount] changes.
 */
data class BudgetUiState(
    /** 0L = new unsaved session */
    val sessionId: Long = 0L,
    val sessionName: String = "",
    /** Raw string so the TextField can hold intermediate input (empty, partial decimals, etc.) */
    val initialBudget: String = "",
    /** Optional savings goal — user wants this much left at the end. Raw string. */
    val goalAmount: String = "",
    val expenses: List<ExpenseUiModel> = listOf(ExpenseUiModel()),   // Start with one empty row

    // ── Derived ──────────────────────────────────────────────────────────────
    val remainingBalance: Double = 0.0,

    // ── Receipt assignment flow ───────────────────────────────────────────────
    val pendingReceiptPath: String? = null,

    // ── Async / feedback ─────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,

    // ── Templates ─────────────────────────────────────────────────────────────
    val showTemplateDialog: Boolean = false,
    val showSaveTemplateDialog: Boolean = false,

    // ── OCR ───────────────────────────────────────────────────────────────────
    val isOcrRunning: Boolean = false,

    // ── Date ──────────────────────────────────────────────────────────────────
    /** User-selected budget date in millis. Defaults to today. */
    val selectedDate: Long = System.currentTimeMillis(),

    // ── Undo delete ───────────────────────────────────────────────────────────
    val recentlyDeletedExpense: ExpenseUiModel? = null,
    /** Increments on every deletion so LaunchedEffect always re-triggers, even for the same expense data */
    val deletionEventId: Int = 0,

    // ── Confirm clear ─────────────────────────────────────────────────────────
    val showClearConfirmDialog: Boolean = false,

    // ── Recurring carry ───────────────────────────────────────────────────────
    /** True while the "carry over recurring expenses?" dialog is shown. */
    val showRecurringCarryDialog: Boolean = false,
    /** Recurring expenses from the last saved session, pending user decision. */
    val pendingRecurringExpenses: List<ExpenseUiModel> = emptyList()
)

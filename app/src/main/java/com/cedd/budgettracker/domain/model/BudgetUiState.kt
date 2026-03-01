package com.cedd.budgettracker.domain.model

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
    val isOcrRunning: Boolean = false
)

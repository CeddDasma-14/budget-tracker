package com.cedd.budgettracker.domain.model

data class ExpenseUiModel(
    val id: Long = 0L,
    val title: String = "",
    val amount: String = "",
    val isPaid: Boolean = false,
    val receiptPath: String? = null,
    val isLocked: Boolean = false,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val isRecurring: Boolean = false
) {
    val amountAsDouble: Double get() = amount.replace(",", "").toDoubleOrNull() ?: 0.0
    val hasContent: Boolean get() = title.isNotBlank() || amountAsDouble > 0
}

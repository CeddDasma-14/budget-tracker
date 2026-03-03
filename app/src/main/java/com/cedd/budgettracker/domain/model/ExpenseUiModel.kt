package com.cedd.budgettracker.domain.model

import java.util.concurrent.atomic.AtomicLong

private val _stableIdCounter = AtomicLong(0)

data class ExpenseUiModel(
    val id: Long = 0L,
    /** Stable per-instance key for LazyColumn — never changes even as list indices shift */
    val stableId: Long = _stableIdCounter.incrementAndGet(),
    val title: String = "",
    val amount: String = "",
    val isPaid: Boolean = false,
    val receiptPath: String? = null,
    val isLocked: Boolean = false,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val isRecurring: Boolean = false,
    val notes: String = ""
) {
    val amountAsDouble: Double get() = amount.replace(",", "").toDoubleOrNull() ?: 0.0
    val hasContent: Boolean get() = title.isNotBlank() || amountAsDouble > 0
}

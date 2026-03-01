package com.cedd.budgettracker.presentation.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    /**
     * Philippines locale — produces ₱ symbol with comma-thousands separator.
     * e.g.  15000.5 → "₱15,000.50"
     */
    private val phpFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    }

    fun formatPhp(amount: Double): String = phpFormatter.format(amount)

    /** Safe parse — returns 0.0 for blank/invalid strings. */
    fun parseAmount(raw: String): Double =
        raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0
}

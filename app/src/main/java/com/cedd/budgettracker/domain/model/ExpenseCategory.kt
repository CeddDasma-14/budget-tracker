package com.cedd.budgettracker.domain.model

import androidx.compose.ui.graphics.Color

enum class ExpenseCategory(
    val label: String,
    val color: Color,
    val emoji: String
) {
    HOUSING("Housing",       Color(0xFF5C6BC0), "🏠"),
    FOOD("Food & Dining",    Color(0xFFEF5350), "🍔"),
    TRANSPORT("Transport",   Color(0xFF42A5F5), "🚗"),
    UTILITIES("Utilities",   Color(0xFFFFB300), "💡"),
    ENTERTAINMENT("Entertainment", Color(0xFFAB47BC), "🎬"),
    HEALTH("Health",         Color(0xFF66BB6A), "❤️"),
    SHOPPING("Shopping",     Color(0xFFFF7043), "🛍️"),
    EDUCATION("Education",   Color(0xFF26C6DA), "📚"),
    SAVINGS("Savings",       Color(0xFF26A69A), "💰"),
    OTHER("Other",           Color(0xFF78909C), "📌");

    companion object {
        fun fromName(name: String): ExpenseCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

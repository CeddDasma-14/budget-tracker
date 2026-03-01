package com.cedd.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted budget session — one row per saved budget period (e.g. "March 2026").
 */
@Entity(tableName = "budget_sessions")
data class BudgetSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val initialBudget: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** User-selected date for this budget period (millis). Falls back to createdAt if 0. Added in DB v3. */
    val budgetDate: Long = System.currentTimeMillis()
)

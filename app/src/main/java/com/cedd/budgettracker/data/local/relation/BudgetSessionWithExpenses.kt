package com.cedd.budgettracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cedd.budgettracker.data.local.entity.BudgetSessionEntity
import com.cedd.budgettracker.data.local.entity.ExpenseEntity

/**
 * Room multi-table query result — one session + all its expenses loaded in one query.
 * Room handles the JOIN automatically via @Relation.
 */
data class BudgetSessionWithExpenses(
    @Embedded val session: BudgetSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val expenses: List<ExpenseEntity>
)

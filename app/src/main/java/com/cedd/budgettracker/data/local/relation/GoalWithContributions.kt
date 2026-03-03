package com.cedd.budgettracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cedd.budgettracker.data.local.entity.GoalContributionEntity
import com.cedd.budgettracker.data.local.entity.GoalEntity

data class GoalWithContributions(
    @Embedded val goal: GoalEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "goalId"
    )
    val contributions: List<GoalContributionEntity>
) {
    val totalSaved: Double get() = contributions.sumOf { it.amount }
    val progress: Float get() = if (goal.targetAmount > 0)
        (totalSaved / goal.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val isComplete: Boolean get() = totalSaved >= goal.targetAmount
}

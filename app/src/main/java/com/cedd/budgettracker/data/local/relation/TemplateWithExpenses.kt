package com.cedd.budgettracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cedd.budgettracker.data.local.entity.TemplateEntity
import com.cedd.budgettracker.data.local.entity.TemplateExpenseEntity

data class TemplateWithExpenses(
    @Embedded val template: TemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "templateId")
    val expenses: List<TemplateExpenseEntity>
)

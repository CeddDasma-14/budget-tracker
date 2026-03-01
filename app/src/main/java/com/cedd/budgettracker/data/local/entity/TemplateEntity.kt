package com.cedd.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val initialBudget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

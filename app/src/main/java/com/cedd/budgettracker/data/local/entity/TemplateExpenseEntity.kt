package com.cedd.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_expenses",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["templateId"])]
)
data class TemplateExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val templateId: Long,
    val title: String,
    val amount: Double,
    val category: String = "OTHER",
    val isRecurring: Boolean = false
)

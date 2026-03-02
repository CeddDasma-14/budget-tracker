package com.cedd.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single expense line item belonging to a [BudgetSessionEntity].
 *
 * receiptPath — absolute path inside the app's internal storage (NOT the image bytes).
 * Storing only the path keeps the DB small and fast.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = BudgetSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE   // Deleting a session also removes its expenses
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val title: String,
    val amount: Double,
    val isPaid: Boolean = false,
    val receiptPath: String? = null,
    val category: String = "OTHER",         // ExpenseCategory.name — added in DB v2
    val isRecurring: Boolean = false,       // Added in DB v2
    val notes: String = ""                  // Added in DB v4
)

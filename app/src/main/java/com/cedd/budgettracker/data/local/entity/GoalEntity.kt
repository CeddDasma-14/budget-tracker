package com.cedd.budgettracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🎯",
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    val colorHex: String = "#38BDF8",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

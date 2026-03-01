package com.cedd.budgettracker.data.local.dao

import androidx.room.*
import com.cedd.budgettracker.data.local.entity.TemplateEntity
import com.cedd.budgettracker.data.local.entity.TemplateExpenseEntity
import com.cedd.budgettracker.data.local.relation.TemplateWithExpenses
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Transaction
    @Query("SELECT * FROM budget_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<TemplateWithExpenses>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExpenses(expenses: List<TemplateExpenseEntity>)

    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
}

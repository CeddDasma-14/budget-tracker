package com.cedd.budgettracker.data.local.dao

import androidx.room.*
import com.cedd.budgettracker.data.local.entity.BudgetSessionEntity
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetSessionDao {

    /** Observe all sessions newest-first, each pre-loaded with its expenses. */
    @Transaction
    @Query("SELECT * FROM budget_sessions ORDER BY createdAt DESC")
    fun getAllSessionsWithExpenses(): Flow<List<BudgetSessionWithExpenses>>

    /** One-shot fetch of the most recently created session with its expenses. */
    @Transaction
    @Query("SELECT * FROM budget_sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionWithExpenses(): BudgetSessionWithExpenses?

    @Query("SELECT * FROM budget_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): BudgetSessionEntity?

    /** Insert or replace — returns the new/existing row id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BudgetSessionEntity): Long

    @Update
    suspend fun updateSession(session: BudgetSessionEntity)

    @Delete
    suspend fun deleteSession(session: BudgetSessionEntity)
}

package com.cedd.budgettracker.data.local.dao

import androidx.room.*
import com.cedd.budgettracker.data.local.entity.GoalContributionEntity
import com.cedd.budgettracker.data.local.entity.GoalEntity
import com.cedd.budgettracker.data.local.relation.GoalWithContributions
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Transaction
    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoalsWithContributions(): Flow<List<GoalWithContributions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Insert
    suspend fun insertContribution(contribution: GoalContributionEntity)

    @Delete
    suspend fun deleteContribution(contribution: GoalContributionEntity)
}

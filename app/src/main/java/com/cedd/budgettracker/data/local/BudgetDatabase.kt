package com.cedd.budgettracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cedd.budgettracker.data.local.dao.BudgetSessionDao
import com.cedd.budgettracker.data.local.dao.ExpenseDao
import com.cedd.budgettracker.data.local.dao.GoalDao
import com.cedd.budgettracker.data.local.dao.TemplateDao
import com.cedd.budgettracker.data.local.entity.BudgetSessionEntity
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
import com.cedd.budgettracker.data.local.entity.GoalContributionEntity
import com.cedd.budgettracker.data.local.entity.GoalEntity
import com.cedd.budgettracker.data.local.entity.TemplateEntity
import com.cedd.budgettracker.data.local.entity.TemplateExpenseEntity

@Database(
    entities = [
        BudgetSessionEntity::class,
        ExpenseEntity::class,
        TemplateEntity::class,
        TemplateExpenseEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun budgetSessionDao(): BudgetSessionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun templateDao(): TemplateDao
    abstract fun goalDao(): GoalDao

    companion object {
        /** v4 → v5: add goals and goal_contributions tables. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '🎯',
                        targetAmount REAL NOT NULL,
                        savedAmount REAL NOT NULL DEFAULT 0.0,
                        deadline INTEGER,
                        colorHex TEXT NOT NULL DEFAULT '#38BDF8',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goal_contributions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        sessionId INTEGER,
                        amount REAL NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        date INTEGER NOT NULL,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId ON goal_contributions(goalId)")
            }
        }

        /** v3 → v4: add notes to expenses; add goalAmount to budget_sessions. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE budget_sessions ADD COLUMN goalAmount REAL NOT NULL DEFAULT 0")
            }
        }

        /** v2 → v3: add budgetDate to budget_sessions. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_sessions ADD COLUMN budgetDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v1 → v2: add category + isRecurring to expenses; add template tables. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        initialBudget REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS template_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL DEFAULT 'OTHER',
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(templateId) REFERENCES budget_templates(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_template_expenses_templateId ON template_expenses(templateId)")
            }
        }
    }
}

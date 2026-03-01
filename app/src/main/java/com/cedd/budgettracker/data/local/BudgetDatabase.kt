package com.cedd.budgettracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cedd.budgettracker.data.local.dao.BudgetSessionDao
import com.cedd.budgettracker.data.local.dao.ExpenseDao
import com.cedd.budgettracker.data.local.dao.TemplateDao
import com.cedd.budgettracker.data.local.entity.BudgetSessionEntity
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
import com.cedd.budgettracker.data.local.entity.TemplateEntity
import com.cedd.budgettracker.data.local.entity.TemplateExpenseEntity

@Database(
    entities = [
        BudgetSessionEntity::class,
        ExpenseEntity::class,
        TemplateEntity::class,
        TemplateExpenseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun budgetSessionDao(): BudgetSessionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun templateDao(): TemplateDao

    companion object {
        /** v1 → v2: add category + isRecurring to expenses; add template tables. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to existing expenses table
                db.execSQL("ALTER TABLE expenses ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")

                // Create templates table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        initialBudget REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create template_expenses table
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

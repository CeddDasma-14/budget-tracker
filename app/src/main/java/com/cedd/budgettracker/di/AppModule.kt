package com.cedd.budgettracker.di

import android.content.Context
import androidx.room.Room
import com.cedd.budgettracker.data.local.BudgetDatabase
import com.cedd.budgettracker.data.local.dao.BudgetSessionDao
import com.cedd.budgettracker.data.local.dao.ExpenseDao
import com.cedd.budgettracker.data.local.dao.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideBudgetDatabase(@ApplicationContext context: Context): BudgetDatabase =
        Room.databaseBuilder(context, BudgetDatabase::class.java, "budget_tracker.db")
            .addMigrations(BudgetDatabase.MIGRATION_1_2, BudgetDatabase.MIGRATION_2_3, BudgetDatabase.MIGRATION_3_4)
            .build()

    @Singleton @Provides
    fun provideBudgetSessionDao(db: BudgetDatabase): BudgetSessionDao = db.budgetSessionDao()

    @Singleton @Provides
    fun provideExpenseDao(db: BudgetDatabase): ExpenseDao = db.expenseDao()

    @Singleton @Provides
    fun provideTemplateDao(db: BudgetDatabase): TemplateDao = db.templateDao()
}

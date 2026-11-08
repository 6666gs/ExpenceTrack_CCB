package com.expensetrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class, SavedAnalysis::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun savedAnalysisDao(): SavedAnalysisDao

    companion object {
        /** v2 adds saved AI analyses; expense data is untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_analyses` (`periodKey` TEXT NOT NULL, " +
                        "`resultJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`fingerprint` TEXT NOT NULL, PRIMARY KEY(`periodKey`))",
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "expenses.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

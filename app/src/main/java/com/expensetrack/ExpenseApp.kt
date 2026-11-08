package com.expensetrack

import android.app.Application
import com.expensetrack.data.AppDatabase
import com.expensetrack.data.ExpenseRepository
import com.expensetrack.data.SavedAnalysisDao
import com.expensetrack.settings.SettingsStore
import com.expensetrack.summary.SummaryNotifier
import com.expensetrack.summary.SummaryScheduler

class ExpenseApp : Application() {
    private val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    val savedAnalyses: SavedAnalysisDao by lazy { database.savedAnalysisDao() }
    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        SummaryNotifier.createChannel(this)
        SummaryScheduler.schedule(this)
    }
}

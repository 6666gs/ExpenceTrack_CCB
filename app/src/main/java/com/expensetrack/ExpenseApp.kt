package com.expensetrack

import android.app.Application
import com.expensetrack.capture.KeepAliveNotification
import com.expensetrack.capture.ListenerBinding
import com.expensetrack.data.AppDatabase
import com.expensetrack.data.ExpenseRepository
import com.expensetrack.data.SavedAnalysisDao
import com.expensetrack.settings.SettingsStore
import com.expensetrack.summary.SummaryNotifier
import com.expensetrack.summary.SummaryScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseApp : Application() {
    private val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    val savedAnalyses: SavedAnalysisDao by lazy { database.savedAnalysisDao() }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        SummaryNotifier.createChannel(this)
        KeepAliveNotification.createChannel(this)
        SummaryScheduler.schedule(this)
        // Any process start (app opened, alarm, boot) reattaches a listener a cleaner unbound.
        appScope.launch { ListenerBinding.ensureBound(this@ExpenseApp) }
    }
}

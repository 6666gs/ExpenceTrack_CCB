package com.expensetrack.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensetrack.ExpenseApp
import com.expensetrack.data.Expense
import com.expensetrack.summary.SummaryNotifier
import com.expensetrack.summary.SummaryScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val expenseApp = app as ExpenseApp
    private val repository = expenseApp.repository

    /** Bumped on resume so totals roll over after midnight. */
    private val today = MutableStateFlow(LocalDate.now())

    val todayTotal: StateFlow<Long> = today
        .flatMapLatest { repository.observeDayTotal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val monthTotal: StateFlow<Long> = today
        .flatMapLatest { repository.observeMonthTotal(YearMonth.from(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val recent: StateFlow<List<Expense>> = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _summaryTime = MutableStateFlow(expenseApp.settings.summaryTime)
    val summaryTime: StateFlow<LocalTime> = _summaryTime.asStateFlow()

    private val _backgroundSetupDone = MutableStateFlow(expenseApp.settings.backgroundSetupDone)
    val backgroundSetupDone: StateFlow<Boolean> = _backgroundSetupDone.asStateFlow()

    fun markBackgroundSetupDone() {
        expenseApp.settings.backgroundSetupDone = true
        _backgroundSetupDone.value = true
    }

    fun refreshDay() {
        today.value = LocalDate.now()
    }

    fun setSummaryTime(time: LocalTime) {
        expenseApp.settings.setSummaryTime(time)
        _summaryTime.value = time
        SummaryScheduler.schedule(expenseApp)
    }

    fun addManual(amountCents: Long, note: String?) = launchSafely { repository.addManual(amountCents, note) }

    fun delete(expense: Expense) = launchSafely { repository.delete(expense) }

    fun updateExpense(expense: Expense, amountCents: Long, note: String) =
        launchSafely { repository.updateExpense(expense.id, amountCents, note) }

    fun sendSummaryNow() = launchSafely { SummaryNotifier.postToday(expenseApp) }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { Log.e("ExpenseViewModel", "Operation failed", it) }
        }
    }
}

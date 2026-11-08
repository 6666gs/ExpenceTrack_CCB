package com.expensetrack.ui.stats

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensetrack.ExpenseApp
import com.expensetrack.ai.AnalysisCodec
import com.expensetrack.ai.AnalysisFormatException
import com.expensetrack.ai.DeepSeekClient
import com.expensetrack.ai.DeepSeekException
import com.expensetrack.ai.RecordsFingerprint
import com.expensetrack.ai.SpendingAnalysis
import com.expensetrack.ai.SpendingAnalysisPrompt
import com.expensetrack.data.Expense
import com.expensetrack.data.SavedAnalysis
import com.expensetrack.stats.Granularity
import com.expensetrack.stats.PeriodStats
import com.expensetrack.stats.StatsCalculator
import com.expensetrack.stats.StatsPeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What the AI card shows for the current period. */
data class AnalysisUiState(
    /** Latest saved result for this period, if any (kept across restarts). */
    val result: SpendingAnalysis? = null,
    val analyzedAt: Long? = null,
    /** Records in the period changed since [result] was produced. */
    val isStale: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** In-flight request status, which isn't persisted. */
private data class RequestStatus(val isLoading: Boolean = false, val error: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val expenseApp = app as ExpenseApp
    private val repository = expenseApp.repository
    private val savedAnalyses = expenseApp.savedAnalyses
    private val client = DeepSeekClient()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _period = MutableStateFlow(StatsPeriod.of(Granularity.MONTH, LocalDate.now()))
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    val expenses: StateFlow<List<Expense>> = _period
        .flatMapLatest { repository.observeBetween(it.range()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<PeriodStats> = combine(_period, expenses, _today) { p, list, today ->
        StatsCalculator.compute(p, list, today)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodStats.EMPTY)

    private val _hasApiKey = MutableStateFlow(expenseApp.settings.hasDeepSeekApiKey)
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val requests = MutableStateFlow<Map<StatsPeriod, RequestStatus>>(emptyMap())

    /** Saved result and the period's live records come from one flatMap, so staleness never flickers. */
    private val saved = _period.flatMapLatest { p ->
        combine(savedAnalyses.observe(p.key), repository.observeBetween(p.range())) { row, records ->
            val result = row?.let { AnalysisCodec.decode(it.resultJson) }
            if (row != null && result == null) Log.w(TAG, "Discarding unreadable saved analysis for ${p.key}")
            AnalysisUiState(
                result = result,
                analyzedAt = row?.createdAt.takeIf { result != null },
                isStale = result != null && row?.fingerprint != RecordsFingerprint.of(records),
            )
        }
    }.flowOn(Dispatchers.Default) // JSON decoding and hashing a year of records stay off the main thread

    val analysis: StateFlow<AnalysisUiState> = combine(_period, saved, requests) { p, state, map ->
        val request = map[p] ?: RequestStatus()
        state.copy(isLoading = request.isLoading, error = request.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisUiState())
    private var analysisJob: Job? = null

    /** Called on resume: the day may have rolled over and the key may have changed in settings. */
    fun refresh() {
        _today.value = LocalDate.now()
        _hasApiKey.value = expenseApp.settings.hasDeepSeekApiKey
    }

    fun selectGranularity(granularity: Granularity) {
        _period.value = _period.value.withGranularity(granularity, _today.value)
    }

    fun shift(steps: Long) {
        _period.value = _period.value.shift(steps)
    }

    fun open(period: StatsPeriod) {
        _period.value = period
    }

    fun goToToday() {
        _period.value = StatsPeriod.of(_period.value.granularity, _today.value)
    }

    fun analyze() {
        val target = _period.value
        val settings = expenseApp.settings
        val apiKey = settings.deepSeekApiKey ?: return setRequest(target, RequestStatus(error = "请先在设置里填入 DeepSeek API Key"))
        val model = settings.deepSeekModel
            ?: return setRequest(target, RequestStatus(error = "请先在设置里点“测试连接”，再选择一个模型"))

        analysisJob?.cancel()
        setRequest(target, RequestStatus(isLoading = true))
        analysisJob = viewModelScope.launch {
            val error = try {
                val records = repository.listBetween(target.range())
                if (records.isEmpty()) {
                    "这段时间没有消费记录"
                } else {
                    val request = SpendingAnalysisPrompt.build(target.label(_today.value), records)
                    val reply = client.chat(apiKey, model, SpendingAnalysisPrompt.SYSTEM, request.userPrompt)
                    val result = SpendingAnalysisPrompt.parse(reply, request)
                    savedAnalyses.upsert(
                        SavedAnalysis(
                            periodKey = target.key,
                            resultJson = AnalysisCodec.encode(result),
                            createdAt = System.currentTimeMillis(),
                            fingerprint = RecordsFingerprint.of(records),
                        ),
                    )
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: DeepSeekException) {
                e.message.orEmpty()
            } catch (e: AnalysisFormatException) {
                Log.w(TAG, "Unparseable analysis reply", e)
                e.message.orEmpty()
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)
                "分析失败，请稍后重试"
            }
            setRequest(target, RequestStatus(error = error))
        }
    }

    private fun setRequest(period: StatsPeriod, status: RequestStatus) {
        requests.value = requests.value + (period to status)
    }

    private companion object {
        const val TAG = "StatsViewModel"
    }
}

package com.expensetrack.ui.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetrack.stats.Granularity
import com.expensetrack.stats.PeriodStats
import com.expensetrack.stats.StatsPeriod
import com.expensetrack.ui.ExpenseActions
import com.expensetrack.ui.expenseDayGroups
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(actions: ExpenseActions, onOpenSettings: () -> Unit, vm: StatsViewModel = viewModel()) {
    val period by vm.period.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val hasApiKey by vm.hasApiKey.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("统计") }) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item(key = "granularity") { GranularitySelector(period.granularity, vm::selectGranularity) }
            item(key = "navigator") {
                PeriodNavigator(
                    label = period.label(today),
                    isCurrent = today in period,
                    onPrevious = { vm.shift(-1) },
                    onNext = { vm.shift(1) },
                    onToday = vm::goToToday,
                )
            }
            item(key = "summary") { StatsSummaryCard(stats, period.granularity) }
            item(key = "chart") { PeriodChart(period, stats, today, onOpen = vm::open) }
            item(key = "analysis") {
                AnalysisCard(
                    state = analysis,
                    hasApiKey = hasApiKey,
                    hasRecords = stats.count > 0,
                    onAnalyze = vm::analyze,
                    onOpenSettings = onOpenSettings,
                )
            }
            if (expenses.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "这段时间没有消费记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                item(key = "list-title") { SectionTitle("明细 · ${stats.count} 笔") }
                expenseDayGroups(expenses, actions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GranularitySelector(selected: Granularity, onSelect: (Granularity) -> Unit) {
    val options = Granularity.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        options.forEachIndexed { index, granularity ->
            SegmentedButton(
                selected = granularity == selected,
                onClick = { onSelect(granularity) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(granularity.label) },
            )
        }
    }
}

@Composable
private fun PeriodChart(period: StatsPeriod, stats: PeriodStats, today: LocalDate, onOpen: (StatsPeriod) -> Unit) {
    when (period.granularity) {
        Granularity.WEEK, Granularity.YEAR -> {
            val unit = if (period.granularity == Granularity.WEEK) "天" else "月"
            SectionTitle("每${unit}支出 · 点柱子看详情")
            BarChart(
                buckets = stats.buckets,
                highlighted = stats.buckets.indexOfFirst { today in it.target },
                onBucketClick = { onOpen(it.target) },
            )
        }
        Granularity.MONTH -> {
            SectionTitle("日历 · 颜色越深花得越多")
            MonthCalendar(
                month = YearMonth.from(period.start),
                dailyTotals = stats.dailyTotals,
                today = today,
                onDayClick = { onOpen(StatsPeriod.of(Granularity.DAY, it)) },
            )
        }
        Granularity.DAY -> Unit
    }
}

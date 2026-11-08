package com.expensetrack.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetrack.stats.Bucket
import com.expensetrack.stats.Granularity
import com.expensetrack.stats.PeriodStats
import com.expensetrack.stats.WEEKDAY_LABELS
import com.expensetrack.util.Money
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.sqrt

private val BAR_AREA_HEIGHT = 120.dp
private const val BAR_ANIMATION_MS = 450

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun PeriodNavigator(label: String, isCurrent: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一段")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一段")
        }
    }
    if (!isCurrent) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onToday) { Text("回到今天") }
        }
    }
}

@Composable
fun StatsSummaryCard(stats: PeriodStats, granularity: Granularity) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 28.dp))
            .background(colors.primary)
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Text("总支出", style = MaterialTheme.typography.labelSmall, color = colors.onPrimary.copy(alpha = 0.7f))
        Text(
            Money.format(stats.totalCents),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
            color = colors.secondary,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            SummaryFigure("笔数", "${stats.count}")
            if (granularity != Granularity.DAY) SummaryFigure("日均", Money.format(stats.dailyAverageCents))
            SummaryFigure("最大一笔", Money.format(stats.maxSingleCents))
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = onPrimary.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = onPrimary)
    }
}

/** Vertical bars; [highlighted] (the bar containing today) is drawn in amber. */
@Composable
fun BarChart(buckets: List<Bucket>, highlighted: Int, onBucketClick: (Bucket) -> Unit) {
    val max = (buckets.maxOfOrNull { it.totalCents } ?: 0).coerceAtLeast(1)
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        buckets.forEachIndexed { index, bucket ->
            val target = bucket.totalCents.toFloat() / max
            val fraction by animateFloatAsState(target, tween(BAR_ANIMATION_MS), label = "bar")
            val barColor = if (index == highlighted) colors.secondary else colors.primary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "查看 ${bucket.label}") { onBucketClick(bucket) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (bucket.totalCents > 0) Money.formatCompact(bucket.totalCents) else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
                Box(Modifier.fillMaxWidth().height(BAR_AREA_HEIGHT).padding(horizontal = 3.dp)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleY = if (bucket.totalCents > 0) fraction.coerceAtLeast(MIN_BAR_FRACTION) else 0f
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .background(barColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    )
                }
                Text(
                    bucket.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == highlighted) colors.onSurface else colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private const val MIN_BAR_FRACTION = 0.02f

/** Month grid, Monday first; each day is shaded by how much was spent. */
@Composable
fun MonthCalendar(month: YearMonth, dailyTotals: Map<LocalDate, Long>, today: LocalDate, onDayClick: (LocalDate) -> Unit) {
    val max = (dailyTotals.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)

    Column {
        Row(Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    Box(Modifier.weight(1f).aspectRatio(0.82f).padding(2.dp)) {
                        week.getOrNull(i)?.let { day ->
                            DayCell(day, dailyTotals[day] ?: 0, max, isToday = day == today,
                                isFuture = day.isAfter(today), onClick = { onDayClick(day) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, cents: Long, max: Long, isToday: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Square root spreads out small amounts so a single big purchase doesn't wash out the rest.
    val intensity = if (cents > 0) sqrt(cents.toFloat() / max) else 0f
    val background = if (cents > 0) colors.primary.copy(alpha = 0.15f + 0.85f * intensity) else colors.surfaceVariant
    val content = when {
        intensity > 0.55f -> colors.onPrimary
        isFuture -> colors.onSurfaceVariant.copy(alpha = 0.5f)
        else -> colors.onSurface
    }
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(background)
            .then(if (isToday) Modifier.border(2.dp, colors.secondary, shape) else Modifier)
            .clickable(onClickLabel = "查看 ${day.monthValue}月${day.dayOfMonth}日", onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelMedium, color = content)
        Text(
            if (cents > 0) Money.formatCompact(cents) else "",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
            color = content,
            maxLines = 1,
            softWrap = false,
        )
    }
}

package com.expensetrack.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetrack.ai.CategoryShare
import com.expensetrack.ai.SpendingAnalysisPrompt
import com.expensetrack.util.Money
import kotlin.math.roundToInt

/**
 * Category colours: mid-tones that read on both the paper and the night background.
 * Grey is reserved for buckets that carry no real category.
 */
private val CATEGORY_COLORS = listOf(
    Color(0xFF2F6B5E), // pine
    Color(0xFFF2B544), // amber
    Color(0xFFC2603D), // terracotta
    Color(0xFF6F8FAF), // slate blue
    Color(0xFF8FC1B2), // sage
    Color(0xFFA3779D), // plum
    Color(0xFF7A8450), // olive
    Color(0xFFD9A07A), // clay
)
private val NEUTRAL = Color(0xFF9A9C94)
private val NEUTRAL_NAMES = setOf(SpendingAnalysisPrompt.UNASSIGNED_CATEGORY, SpendingAnalysisPrompt.OMITTED_CATEGORY, "未备注")

private val DONUT_SIZE = 168.dp
private val DONUT_STROKE = 28.dp
private const val GAP_DEGREES = 2f
private const val SWEEP_ANIMATION_MS = 700

/** Assigns colours in order, skipping the palette for neutral buckets so colours stay distinct. */
fun categoryColors(shares: List<CategoryShare>): List<Color> {
    var next = 0
    return shares.map { if (it.name in NEUTRAL_NAMES) NEUTRAL else CATEGORY_COLORS[next++ % CATEGORY_COLORS.size] }
}

@Composable
fun CategoryDonut(shares: List<CategoryShare>, totalCents: Long) {
    val colors = remember(shares) { categoryColors(shares) }
    val progress = remember(shares) { Animatable(0f) }
    LaunchedEffect(shares) { progress.animateTo(1f, tween(SWEEP_ANIMATION_MS, easing = FastOutSlowInEasing)) }

    val description = shares.joinToString("，") { "${it.name}${percent(it.fraction)}" }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(DONUT_SIZE).semantics { contentDescription = "消费占比：$description" },
            contentAlignment = Alignment.Center,
        ) {
            val track = MaterialTheme.colorScheme.background
            Canvas(Modifier.size(DONUT_SIZE)) {
                val stroke = DONUT_STROKE.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))

                val gap = if (shares.size > 1) GAP_DEGREES else 0f
                var start = -90f
                shares.forEachIndexed { i, share ->
                    val full = share.fraction * 360f
                    val sweep = ((full - gap) * progress.value).coerceAtLeast(0f)
                    if (sweep > 0f) {
                        drawArc(colors[i], start + gap / 2, sweep, useCenter = false, topLeft = topLeft,
                            size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    }
                    start += full
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("合计", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(totalCents), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.size(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            shares.forEachIndexed { i, share -> LegendRow(share, colors[i]) }
        }
    }
}

@Composable
private fun LegendRow(share: CategoryShare, color: Color) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(share.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp).weight(1f))
        Text(Money.format(share.totalCents), style = MaterialTheme.typography.titleSmall)
        Text(percent(share.fraction), style = MaterialTheme.typography.bodySmall, color = muted,
            textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
    }
}

private fun percent(fraction: Float): String {
    val value = (fraction * 100).roundToInt()
    return if (value == 0 && fraction > 0f) "<1%" else "$value%"
}

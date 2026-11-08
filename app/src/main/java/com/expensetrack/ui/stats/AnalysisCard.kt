package com.expensetrack.ui.stats

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.expensetrack.ai.SpendingAnalysis
import com.expensetrack.ai.SpendingAnalysisPrompt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalysisCard(
    state: AnalysisUiState,
    hasApiKey: Boolean,
    hasRecords: Boolean,
    onAnalyze: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SectionTitle("AI 消费分析")
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(18.dp)) {
            val result = state.result
            when {
                result != null -> {
                    ResultStatus(state, onAnalyze)
                    Result(result, showReanalyze = !state.isLoading && !state.isStale, onReanalyze = onAnalyze)
                }
                !hasApiKey -> MissingKey(onOpenSettings)
                state.isLoading -> Loading()
                state.error != null -> Failed(state.error, onAnalyze)
                else -> Prompt(hasRecords, onAnalyze)
            }
        }
    }
}

@Composable
private fun MissingKey(onOpenSettings: () -> Unit) {
    Text("在设置里填入 DeepSeek API Key，AI 就能根据你的备注总结这段时间钱花在了哪里。",
        style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onOpenSettings) { Text("去设置") }
}

@Composable
private fun Prompt(hasRecords: Boolean, onAnalyze: () -> Unit) {
    Text(
        if (hasRecords) "让 DeepSeek 按备注给每笔消费分类，算出分布，并给出观察和建议。"
        else "这段时间还没有记录，没什么可分析的。",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text("会把金额、时间、备注和商户名发给 DeepSeek，不发送银行通知原文。",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onAnalyze, enabled = hasRecords) { Text("开始分析") }
}

/** When the saved result was produced, plus re-analysis progress, errors, or an outdated hint. */
@Composable
private fun ResultStatus(state: AnalysisUiState, onAnalyze: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    state.analyzedAt?.let {
        Text("分析于 ${formatAnalyzedAt(it)}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
    when {
        state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("正在重新分析…", style = MaterialTheme.typography.bodySmall)
        }
        state.isStale -> Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("记录有变化，结果可能已过时", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onAnalyze) { Text("重新分析") }
        }
    }
    state.error?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(Modifier.height(12.dp))
}

private val ANALYZED_AT = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

private fun formatAnalyzedAt(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(ANALYZED_AT)

@Composable
private fun Loading() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
        Spacer(Modifier.width(14.dp))
        Text("DeepSeek 正在分析，通常需要十几秒…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onRetry) { Text("重试") }
}

@Composable
private fun Result(result: SpendingAnalysis, showReanalyze: Boolean, onReanalyze: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (result.categories.isNotEmpty()) {
        CategoryDonut(result.categories, result.categories.sumOf { it.totalCents })
        if (result.omittedCount > 0) {
            Text("记录较多，只逐笔分析了金额最大的 ${SpendingAnalysisPrompt.MAX_ENTRIES} 笔",
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
    if (result.summary.isNotBlank()) {
        Spacer(Modifier.height(18.dp))
        Text(result.summary, style = MaterialTheme.typography.titleSmall)
    }
    if (result.insights.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            result.insights.forEach { line ->
                Row {
                    Text("·", style = MaterialTheme.typography.bodyMedium, color = colors.primary, modifier = Modifier.width(14.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
    }
    result.suggestions.firstOrNull()?.let { tip ->
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.secondary.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("建议", style = MaterialTheme.typography.labelLarge, color = colors.onSurface,
                modifier = Modifier.padding(end = 10.dp))
            Text(tip, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (showReanalyze) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onReanalyze) { Text("重新分析") }
        }
    } else {
        Spacer(Modifier.height(4.dp))
    }
}

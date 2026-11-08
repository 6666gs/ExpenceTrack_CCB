package com.expensetrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetrack.data.Expense
import com.expensetrack.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

@Composable
fun TotalsHeader(todayCents: Long, monthCents: Long) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 28.dp))
            .background(colors.primary)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text("今日支出", style = MaterialTheme.typography.labelSmall, color = colors.onPrimary.copy(alpha = 0.7f))
        Text(Money.format(todayCents), style = MaterialTheme.typography.displayLarge, color = colors.secondary)
        Spacer(Modifier.height(12.dp))
        Text(
            "本月累计 ${Money.format(monthCents)}",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onPrimary,
        )
    }
}

@Composable
fun SummarySettingRow(time: LocalTime, onPickTime: () -> Unit, onSendNow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPickTime)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("每日小结提醒", style = MaterialTheme.typography.bodyLarge)
            Text("每天 ${time.format(TIME_FORMAT)} · 点击修改", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onSendNow) { Text("立即发送") }
    }
    HorizontalDivider(Modifier.padding(bottom = 8.dp))
}

@Composable
fun DayHeader(date: LocalDate, totalCents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val label = if (date == LocalDate.now()) "今天" else date.format(DATE_FORMAT)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Money.format(totalCents), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ExpenseRow(expense: Expense, onClick: () -> Unit, onDelete: () -> Unit) {
    val time = Instant.ofEpochMilli(expense.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    val colors = MaterialTheme.colorScheme
    val subtitle = buildList {
        add(time.format(TIME_FORMAT))
        if (expense.note != null && expense.merchant != null) add(expense.merchant)
        add(if (expense.isManual) "手动" else "自动捕获")
    }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "编辑备注", onClick = onClick)
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(expense.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (expense.note == null) {
                Text("+ 点击写备注", style = MaterialTheme.typography.labelSmall, color = colors.primary)
            }
        }
        Text("-${Money.format(expense.amountCents)}", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = colors.onSurfaceVariant)
        }
    }
}

/** Records grouped under a header per day, newest first. */
fun LazyListScope.expenseDayGroups(expenses: List<Expense>, actions: ExpenseActions) {
    groupByDay(expenses).forEach { (date, dayExpenses) ->
        item(key = "header-$date") { DayHeader(date, dayExpenses.sumOf { it.amountCents }) }
        items(dayExpenses, key = { it.id }) { expense ->
            ExpenseRow(expense, onClick = { actions.editing = expense }, onDelete = { actions.deleting = expense })
        }
    }
}

private fun groupByDay(expenses: List<Expense>): List<Pair<LocalDate, List<Expense>>> =
    expenses.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toList()

@Composable
fun EmptyState() {
    Text(
        "还没有记录。开启通知访问权限后，建设银行的消费通知会自动记在这里；也可以点右下角手动记一笔。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 32.dp),
    )
}

package com.expensetrack.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetrack.ExpenseApp
import com.expensetrack.setup.RomGuide
import com.expensetrack.setup.SetupIntents
import com.expensetrack.ui.PermissionIntents
import com.expensetrack.ui.PermissionState
import com.expensetrack.ui.stats.SectionTitle

/** Step-by-step setup that keeps capture working on ROMs that kill background apps. */
@Composable
fun BackgroundSetupSection() {
    val context = LocalContext.current
    val guide = remember { RomGuide.forManufacturer(Build.MANUFACTURER) }
    val settings = remember { (context.applicationContext as ExpenseApp).settings }
    val keepAlive by settings.keepAliveEnabled.collectAsStateWithLifecycle()
    var canRead by remember { mutableStateOf(PermissionState.read(context).canReadNotifications) }
    var batteryFree by remember { mutableStateOf(SetupIntents.isIgnoringBatteryOptimizations(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canRead = PermissionState.read(context).canReadNotifications
        batteryFree = SetupIntents.isIgnoringBatteryOptimizations(context)
    }

    SectionTitle("后台运行 · 防止漏记")
    GuideCard {
        Text("手机会清理后台，下面几项没开的话可能漏记、收不到每日小结。", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SetupStep(1, "读取通知", "捕获建行消费通知，必须开启", done = canRead,
            action = "去开启") { context.startActivity(PermissionIntents.notificationListenerSettings()) }
        SetupStep(2, "自启动", guide.autoStartPath, action = "去设置") { SetupIntents.openAutoStart(context, guide) }
        SetupStep(3, "后台耗电", guide.batteryPath, done = batteryFree, action = "去设置") {
            SetupIntents.openBatterySettings(context)
        }
        SetupStep(4, "建行 App 通知", "建行 App 里的消息推送和系统通知都要开着", action = "去设置") {
            SetupIntents.openBankNotificationSettings(context)
        }
        SetupStep(5, "锁定在后台", guide.lockPath, action = null, onClick = {})
        KeepAliveRow(keepAlive, onChange = settings::setKeepAliveEnabled)
        Spacer(Modifier.height(12.dp))
        Text("当前机型：${guide.brand}，路径随系统版本可能略有不同。被清掉后，重新打开一次 App 就会恢复捕获，" +
            "并补记通知栏里还留着的建行消费。",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A few lines on what the app does, for someone opening it for the first time. */
@Composable
fun HowToUseSection() {
    SectionTitle("使用说明")
    GuideCard(verticalSpacing = 10.dp) {
        UsageLine("自动记账", "建行消费通知到达后自动记一笔，App 推送和短信重复的只记一次。")
        UsageLine("编辑", "在「记录」里点一笔消费，写备注，金额不对也能改。")
        UsageLine("看统计", "「统计」按日、周、月、年查看，月视图是日历，颜色越深花得越多。")
        UsageLine("AI 分析", "填好下方 DeepSeek Key 后，在统计页点「开始分析」，结果会保存。")
        UsageLine("每日小结", "每天在「记录」页设定的时间推送当天花费。")
    }
}

@Composable
private fun GuideCard(verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(verticalSpacing)) { content() }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    hint: String,
    done: Boolean = false,
    action: String?,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (number > 1) HorizontalDivider(Modifier.padding(top = 12.dp), color = colors.outline.copy(alpha = 0.3f))
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$number", style = MaterialTheme.typography.titleSmall, color = colors.primary,
            modifier = Modifier.width(22.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        when {
            done -> Text("已开启", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            action != null -> OutlinedButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun KeepAliveRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    HorizontalDivider(Modifier.padding(top = 12.dp), color = colors.outline.copy(alpha = 0.3f))
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = 22.dp, end = 8.dp)) {
            Text("常驻通知", style = MaterialTheme.typography.bodyLarge)
            Text("通知栏显示「自动记账中 · 今日支出」，让系统不易清理", style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun UsageLine(title: String, body: String) {
    Row {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

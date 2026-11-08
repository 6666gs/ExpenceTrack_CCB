package com.expensetrack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionGuide(
    state: PermissionState,
    onOpenListenerSettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("还差几步设置", style = MaterialTheme.typography.titleSmall)
            if (!state.canReadNotifications) {
                PermissionItem("读取通知", "用来捕获建设银行的消费通知（只在本机处理）", "去开启", onOpenListenerSettings)
            }
            if (!state.canPostNotifications) {
                PermissionItem("发送通知", "用来每天傍晚告诉你今天花了多少", "允许", onRequestPostNotifications)
            }
            if (!state.canScheduleExact) {
                PermissionItem("准时提醒", "可选：不开也能提醒，但可能晚几分钟", "去开启", onOpenExactAlarmSettings)
            }
        }
    }
}

@Composable
private fun PermissionItem(title: String, reason: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onClick) { Text(action) }
    }
}

/** Shown once the permissions are granted, until the user confirms the ROM-specific steps. */
@Composable
fun BackgroundSetupHint(onOpenSettings: () -> Unit, onDone: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("最后一步：防止被系统清理", style = MaterialTheme.typography.titleSmall)
            Text("手机会清理后台导致漏记。到「设置」按步骤开启自启动和后台运行。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone) { Text("已设置好") }
                OutlinedButton(onClick = onOpenSettings) { Text("去设置") }
            }
        }
    }
}

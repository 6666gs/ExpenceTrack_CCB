package com.expensetrack.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetrack.BuildConfig
import com.expensetrack.ui.stats.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            BackgroundSetupSection()
            HowToUseSection()
            SectionTitle("AI 分析 · DeepSeek")
            ApiKeySection(state, onSave = vm::saveKey, onTest = vm::testKey, onClear = vm::clearKey)
            SectionTitle("模型")
            ModelSection(state, onSelect = vm::selectModel)
            PrivacyNote()
            SectionTitle("关于")
            Text("消费记录 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text("记录只保存在本机；只有你点“开始分析”时才会联网。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ApiKeySection(state: AiSettingsState, onSave: (String) -> Unit, onTest: (String) -> Unit, onClear: () -> Unit) {
    // Plain remember: rememberSaveable would copy the key into the Activity's saved-state Bundle.
    var input by remember(state.savedKey) { mutableStateOf(state.savedKey.orEmpty()) }
    var visible by rememberSaveable { mutableStateOf(false) }
    val changed = input.trim() != state.savedKey.orEmpty()

    OutlinedTextField(
        value = input,
        onValueChange = { input = it.trim() },
        label = { Text("API Key") },
        placeholder = { Text("sk-…") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "隐藏" else "显示") } },
        supportingText = { Text("在 platform.deepseek.com 的 API Keys 页面创建。加密保存在本机。") },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
        Button(onClick = { onSave(input) }, enabled = changed) { Text("保存") }
        OutlinedButton(onClick = { onTest(input) }, enabled = state.status != KeyStatus.Testing) { Text("测试连接") }
        if (state.savedKey != null) TextButton(onClick = onClear) { Text("清除") }
    }
    StatusLine(state.status)
}

@Composable
private fun StatusLine(status: KeyStatus) {
    val colors = MaterialTheme.colorScheme
    val (text, color) = when (status) {
        KeyStatus.Idle -> return
        KeyStatus.Saved -> "已保存" to colors.primary
        KeyStatus.Cleared -> "已清除 API Key" to colors.onSurfaceVariant
        KeyStatus.Testing -> "正在连接 DeepSeek…" to colors.onSurfaceVariant
        is KeyStatus.Valid -> "连接成功，可用模型 ${status.modelCount} 个，已列在下方。记得保存 Key。" to colors.primary
        is KeyStatus.Invalid -> status.message to colors.error
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(top = 8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelSection(state: AiSettingsState, onSelect: (String) -> Unit) {
    if (state.availableModels.isEmpty()) {
        Text("填好 API Key 后点“测试连接”，这里会列出你的账户可用的模型。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.availableModels.forEach { model ->
            FilterChip(selected = model == state.model, onClick = { onSelect(model) }, label = { Text(model) })
        }
    }
}

@Composable
private fun PrivacyNote() {
    Spacer(Modifier.height(20.dp))
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.Unspecified) {
        Column(Modifier.padding(16.dp)) {
            Text("会发送什么", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "分析时，只把所选时间段每笔消费的金额、时间、你写的备注和商户名发给 DeepSeek。" +
                    "银行通知原文（可能含卡号尾号）不会发送。费用按 DeepSeek 账户计费。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

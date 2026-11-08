package com.expensetrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.expensetrack.data.Expense
import com.expensetrack.data.MAX_NOTE_LENGTH
import com.expensetrack.util.Money
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL_TIME = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
private const val SCRIM_ALPHA = 0.45f

/**
 * Bottom sheet drawn inside the activity window, so the keyboard behaves exactly as on any other
 * screen. It rises with the keyboard via the IME insets.
 */
@Composable
fun InWindowSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                    content = content,
                )
            }
        }
    }
}

/** Focuses [focus] and raises the keyboard once the sheet's content is composed. */
@Composable
private fun AutoFocus(focus: FocusRequester, key: Any?) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(key) {
        focus.requestFocus()
        keyboard?.show()
    }
}

/** What the add and edit sheets hand back. */
data class ExpenseEdit(val amountCents: Long, val note: String, val isIncome: Boolean)

@Composable
fun ExpenseEditorSheet(expense: Expense?, onDismiss: () -> Unit, onSave: (Expense, ExpenseEdit) -> Unit) {
    // Keep showing the last record while the sheet slides away.
    var shown by remember { mutableStateOf(expense) }
    if (expense != null && expense != shown) shown = expense

    InWindowSheet(visible = expense != null, onDismiss = onDismiss) {
        shown?.let { ExpenseEditorContent(it, onDismiss, onSave) }
    }
}

/**
 * Edits the note (focused, the common case), the amount and income vs spending, for when a
 * capture got something wrong.
 */
@Composable
private fun ExpenseEditorContent(expense: Expense, onDismiss: () -> Unit, onSave: (Expense, ExpenseEdit) -> Unit) {
    val initialNote = expense.note.orEmpty()
    var note by remember(expense.id) { mutableStateOf(TextFieldValue(initialNote, TextRange(initialNote.length))) }
    var amount by remember(expense.id) { mutableStateOf(Money.toInput(expense.amountCents)) }
    var isIncome by remember(expense.id) { mutableStateOf(expense.isIncome) }
    val cents = Money.parseYuanToCents(amount)
    val focus = remember { FocusRequester() }
    AutoFocus(focus, expense.id)

    val time = Instant.ofEpochMilli(expense.timestamp).atZone(ZoneId.systemDefault()).format(FULL_TIME)
    val source = expense.merchant ?: if (expense.isManual) "手动记录" else "建设银行"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("编辑记录", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        KindSelector(isIncome, onChange = { isIncome = it })
    }
    Text("$time · $source", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = note,
        onValueChange = { note = it.copy(text = it.text.take(MAX_NOTE_LENGTH)) },
        label = { Text(if (isIncome) "什么收入" else "买了什么") },
        placeholder = { Text(if (isIncome) "例如：工资" else "例如：午饭 牛肉面") },
        supportingText = { Text("${note.text.length}/$MAX_NOTE_LENGTH") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    AmountField(amount, onChange = { amount = it }, invalid = cents == null)
    SheetButtons(confirmLabel = "保存", confirmEnabled = cents != null, onDismiss = onDismiss,
        onConfirm = { cents?.let { onSave(expense, ExpenseEdit(it, note.text, isIncome)) } })
}

/** 支出 / 收入 switch shared by both sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindSelector(isIncome: Boolean, onChange: (Boolean) -> Unit) {
    val options = listOf(false to "支出", true to "收入")
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (income, label) ->
            SegmentedButton(
                selected = isIncome == income,
                onClick = { onChange(income) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    invalid: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
) {
    val showError = invalid && value.isNotBlank()
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("金额（元）") },
        prefix = { Text("¥") },
        isError = showError,
        supportingText = { if (showError) Text("请输入有效金额，最多两位小数") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AddExpenseSheet(visible: Boolean, onDismiss: () -> Unit, onConfirm: (ExpenseEdit) -> Unit) {
    InWindowSheet(visible = visible, onDismiss = onDismiss) {
        // Fresh fields every time the sheet opens.
        if (visible) AddExpenseContent(onDismiss, onConfirm)
    }
}

@Composable
private fun AddExpenseContent(onDismiss: () -> Unit, onConfirm: (ExpenseEdit) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    val cents = Money.parseYuanToCents(amount)
    val focus = remember { FocusRequester() }
    AutoFocus(focus, Unit)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("手动记一笔", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        KindSelector(isIncome, onChange = { isIncome = it })
    }
    Spacer(Modifier.height(16.dp))
    AmountField(amount, onChange = { amount = it }, invalid = cents == null, modifier = Modifier.focusRequester(focus),
        imeAction = ImeAction.Next)
    OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(MAX_NOTE_LENGTH) },
        label = { Text(if (isIncome) "什么收入（可选）" else "买了什么（可选）") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    SheetButtons(confirmLabel = "保存", confirmEnabled = cents != null, onDismiss = onDismiss,
        onConfirm = { cents?.let { onConfirm(ExpenseEdit(it, note, isIncome)) } })
}

@Composable
private fun SheetButtons(confirmLabel: String, confirmEnabled: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss) { Text("取消") }
        Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
    }
}

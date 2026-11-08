package com.expensetrack.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.expensetrack.data.Expense
import com.expensetrack.util.Money

/**
 * UI state for record actions, hoisted to [AppRoot] so the editors can be drawn over every tab.
 * Text input lives in in-window sheets (see ExpenseSheets.kt), not Dialog windows: on some
 * Android 15 ROMs the keyboard never animates in for a Compose Dialog window.
 */
class ExpenseActions {
    var editing by mutableStateOf<Expense?>(null)
    var deleting by mutableStateOf<Expense?>(null)
    var adding by mutableStateOf(false)
}

@Composable
fun rememberExpenseActions() = remember { ExpenseActions() }

/** Confirmation only, no text input, so a regular dialog is fine here. */
@Composable
fun DeleteExpenseDialog(expense: Expense, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除这笔记录？") },
        text = { Text("${Money.format(expense.amountCents)} · ${expense.title}") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

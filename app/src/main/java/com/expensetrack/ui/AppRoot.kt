package com.expensetrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetrack.ui.settings.SettingsScreen
import com.expensetrack.ui.stats.StatsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    RECORDS("记录", Icons.Filled.Home),
    STATS("统计", Icons.Filled.DateRange),
    SETTINGS("设置", Icons.Filled.Settings),
}

@Composable
fun AppRoot(vm: ExpenseViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.RECORDS) }
    val actions = rememberExpenseActions()
    BackHandler(enabled = tab != Tab.RECORDS) { tab = Tab.RECORDS }

    Box(Modifier.fillMaxSize()) {
        AppScaffold(tab, onSelectTab = { tab = it }, actions)
        ExpenseEditorSheet(
            expense = actions.editing,
            onDismiss = { actions.editing = null },
            onSave = { expense, cents, note -> vm.updateExpense(expense, cents, note); actions.editing = null },
        )
        AddExpenseSheet(
            visible = actions.adding,
            onDismiss = { actions.adding = false },
            onConfirm = { cents, note -> vm.addManual(cents, note); actions.adding = false },
        )
        actions.deleting?.let { expense ->
            DeleteExpenseDialog(
                expense = expense,
                onDismiss = { actions.deleting = null },
                onConfirm = { vm.delete(expense); actions.deleting = null },
            )
        }
    }
}

@Composable
private fun AppScaffold(tab: Tab, onSelectTab: (Tab) -> Unit, actions: ExpenseActions) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { onSelectTab(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                Tab.RECORDS -> HomeScreen(actions, onOpenSettings = { onSelectTab(Tab.SETTINGS) })
                Tab.STATS -> StatsScreen(actions, onOpenSettings = { onSelectTab(Tab.SETTINGS) })
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

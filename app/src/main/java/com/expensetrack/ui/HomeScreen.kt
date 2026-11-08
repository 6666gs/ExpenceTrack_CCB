package com.expensetrack.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(actions: ExpenseActions, onOpenSettings: () -> Unit, vm: ExpenseViewModel = viewModel()) {
    val context = LocalContext.current
    val todayTotal by vm.todayTotal.collectAsStateWithLifecycle()
    val monthTotal by vm.monthTotal.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val summaryTime by vm.summaryTime.collectAsStateWithLifecycle()
    val backgroundSetupDone by vm.backgroundSetupDone.collectAsStateWithLifecycle()

    var permissions by remember { mutableStateOf(PermissionState.read(context)) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions = PermissionState.read(context) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = PermissionState.read(context)
        vm.refreshDay()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("消费记录") }) },
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { actions.adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "手动记一笔")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item { TotalsHeader(todayTotal, monthTotal) }
            if (!permissions.allGranted) {
                item {
                    PermissionGuide(
                        state = permissions,
                        onOpenListenerSettings = { context.startActivity(PermissionIntents.notificationListenerSettings()) },
                        onRequestPostNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenExactAlarmSettings = { context.startActivity(PermissionIntents.exactAlarmSettings(context)) },
                    )
                }
            }
            if (permissions.allGranted && !backgroundSetupDone) {
                item { BackgroundSetupHint(onOpenSettings = onOpenSettings, onDone = vm::markBackgroundSetupDone) }
            }
            item {
                SummarySettingRow(
                    time = summaryTime,
                    onPickTime = {
                        TimePickerDialog(context, { _, h, m -> vm.setSummaryTime(LocalTime.of(h, m)) },
                            summaryTime.hour, summaryTime.minute, true).show()
                    },
                    onSendNow = vm::sendSummaryNow,
                )
            }
            if (recent.isEmpty()) {
                item { EmptyState() }
            }
            expenseDayGroups(recent, actions)
        }
    }

}


package com.expensetrack.summary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires at the configured evening time: posts today's total, then schedules tomorrow. */
class SummaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SummaryNotifier.postToday(context.applicationContext)
            } catch (e: Exception) {
                Log.e("SummaryAlarmReceiver", "Failed to post daily summary", e)
            } finally {
                SummaryScheduler.schedule(context.applicationContext)
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared on reboot and skewed by clock changes, so re-register them. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The receiver is exported for system broadcasts; ignore anything else sent to it explicitly.
        if (intent.action !in HANDLED_ACTIONS) return
        SummaryScheduler.schedule(context.applicationContext)
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

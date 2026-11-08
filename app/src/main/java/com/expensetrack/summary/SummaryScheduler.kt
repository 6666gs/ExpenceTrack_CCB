package com.expensetrack.summary

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.expensetrack.ExpenseApp
import java.time.LocalTime
import java.time.ZonedDateTime

object SummaryScheduler {

    /** Next occurrence of [time] strictly after [now]. */
    fun nextTrigger(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val today = now.with(time).withSecond(0).withNano(0)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** (Re)schedules the daily summary alarm. Safe to call repeatedly. */
    fun schedule(context: Context) {
        val app = context.applicationContext as ExpenseApp
        val triggerAt = nextTrigger(ZonedDateTime.now(), app.settings.summaryTime)
            .toInstant().toEpochMilli()
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Without the exact-alarm permission the system may deliver it a few minutes late.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, SummaryAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private const val REQUEST_CODE = 1001
}

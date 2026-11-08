package com.expensetrack.summary

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expensetrack.ExpenseApp
import com.expensetrack.MainActivity
import com.expensetrack.R
import com.expensetrack.data.DaySummary
import com.expensetrack.util.Money
import java.time.LocalDate

object SummaryNotifier {
    private const val CHANNEL_ID = "daily_summary"
    private const val NOTIFICATION_ID = 2001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_daily_summary),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun message(summary: DaySummary): String =
        if (summary.count == 0) "今天还没有记录到消费"
        else "今天共消费 ${Money.format(summary.totalCents)}（${summary.count} 笔）"

    @SuppressLint("MissingPermission") // checked via areNotificationsEnabled()
    suspend fun postToday(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val summary = (context.applicationContext as ExpenseApp).repository.daySummary(LocalDate.now())
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今日消费小结")
            .setContentText(message(summary))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

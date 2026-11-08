package com.expensetrack.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.expensetrack.ExpenseApp
import com.expensetrack.MainActivity
import com.expensetrack.R
import com.expensetrack.util.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/**
 * Ongoing, silent notification that keeps the listener's process at foreground priority, so
 * one-tap cleaners and battery managers leave it alone. Shows today's total so it earns its spot.
 * The user can switch it off in Settings.
 */
object KeepAliveNotification {
    private const val TAG = "KeepAliveNotification"
    private const val CHANNEL_ID = "keep_alive"
    const val NOTIFICATION_ID = 2002
    private const val DAY_CHECK_MS = 60_000L

    /** Last total shown, so a swiped-away notification can be put back without a query. */
    @Volatile
    private var lastTotalCents: Long? = null

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_keep_alive),
            NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Runs for as long as the listener is connected; follows the setting and today's total. */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run(service: Service) {
        val app = service.application as ExpenseApp
        val today = flow {
            while (true) {
                emit(LocalDate.now())
                delay(DAY_CHECK_MS)
            }
        }.distinctUntilChanged()
        val todayTotal = today.flatMapLatest { app.repository.observeDayTotal(it) }

        combine(app.settings.keepAliveEnabled, todayTotal) { enabled, total -> enabled to total }
            .distinctUntilChanged()
            .collect { (enabled, total) -> if (enabled) show(service, total) else stop(service) }
    }

    /**
     * Android 14+ lets the user swipe away even a foreground-service notification. The service
     * stays foreground but becomes invisible; put the notification straight back.
     */
    fun restoreIfDismissed(service: Service) {
        val app = service.application as ExpenseApp
        val total = lastTotalCents ?: return
        if (app.settings.keepAliveEnabled.value) show(service, total)
    }

    fun stop(service: Service) {
        lastTotalCents = null
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun show(service: Service, todayCents: Long) {
        lastTotalCents = todayCents
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(service, NOTIFICATION_ID, build(service, todayCents), type)
        } catch (e: RuntimeException) {
            // e.g. ForegroundServiceStartNotAllowedException when bound while the app is in the background.
            Log.w(TAG, "Could not enter foreground", e)
        }
    }

    private fun build(context: Context, todayCents: Long): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("自动记账中")
            .setContentText("今日支出 ${Money.format(todayCents)}")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}

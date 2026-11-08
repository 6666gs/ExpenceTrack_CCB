package com.expensetrack.capture

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.expensetrack.ExpenseApp
import com.expensetrack.parser.CcbNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives every notification posted on the device (once the user grants notification access)
 * and records CCB spending notifications. Notification text never leaves the device.
 *
 * While connected it runs as a foreground service so ROMs are less inclined to kill it, and on
 * every (re)connection it re-reads the notifications still in the shade, so a purchase that
 * arrived while the app was killed is recorded once the listener is back.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var keepAlive: Job? = null

    override fun onListenerConnected() {
        ListenerBinding.isConnected = true
        keepAlive?.cancel()
        keepAlive = scope.launch { KeepAliveNotification.run(this@BankNotificationListener) }
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
        active.forEach(::handle)
    }

    override fun onListenerDisconnected() {
        ListenerBinding.isConnected = false
        keepAlive?.cancel()
        KeepAliveNotification.stop(this)
        // Ask the system to bind us again; it otherwise stays unbound until the next reboot.
        ListenerBinding.requestRebind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        val isOurs = sbn.packageName == packageName && sbn.id == KeepAliveNotification.NOTIFICATION_ID
        // Only user dismissals; our own stopForeground (setting switched off) reports REASON_APP_CANCEL.
        if (isOurs && reason in USER_DISMISS_REASONS) KeepAliveNotification.restoreIfDismissed(this)
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: return
        val title = extras.text(Notification.EXTRA_TITLE)
        val body = extras.text(Notification.EXTRA_BIG_TEXT) ?: extras.text(Notification.EXTRA_TEXT)
        val header = listOfNotNull(title, body).joinToString(" ")
        if (!CcbNotificationParser.isFromBank(sbn.packageName, header)) return

        // SMS apps may bundle several messages in InboxStyle lines; parse each one separately.
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it.toString() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(header)

        val repository = (application as ExpenseApp).repository
        lines.forEach { line ->
            val parsed = CcbNotificationParser.parse(line) ?: return@forEach
            // Re-reading the shade hands us the same notification again; the duplicate check
            // (same app, same text, same post time) drops it.
            scope.launch {
                runCatching { repository.recordCaptured(parsed, sbn.packageName, line, sbn.postTime) }
                    .onFailure { Log.e(TAG, "Failed to save captured expense", it) }
            }
        }
    }

    override fun onDestroy() {
        ListenerBinding.isConnected = false
        scope.cancel()
        super.onDestroy()
    }

    private fun Bundle.text(key: String): String? = getCharSequence(key)?.toString()?.ifBlank { null }

    private companion object {
        const val TAG = "BankNotificationListener"
        val USER_DISMISS_REASONS = setOf(REASON_CANCEL, REASON_CANCEL_ALL)
    }
}

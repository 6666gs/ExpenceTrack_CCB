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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives every notification posted on the device (once the user grants notification access)
 * and records CCB spending notifications. Notification text never leaves the device.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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
            scope.launch {
                runCatching { repository.recordCaptured(parsed, sbn.packageName, line, sbn.postTime) }
                    .onFailure { Log.e(TAG, "Failed to save captured expense", it) }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun Bundle.text(key: String): String? = getCharSequence(key)?.toString()?.ifBlank { null }

    private companion object {
        const val TAG = "BankNotificationListener"
    }
}

package com.expensetrack.capture

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay

/**
 * Clearing the app from recents on many ROMs force-stops it, which unbinds the notification
 * listener; the system does not bind it again on its own, even when the app is reopened.
 * Called whenever our process starts (app opened, alarm, boot) to reattach it.
 */
object ListenerBinding {
    private const val TAG = "ListenerBinding"
    private const val CONNECT_WAIT_MS = 3_000L

    /** Set by the listener itself; process-wide, so it resets to false after a kill. */
    @Volatile
    var isConnected: Boolean = false
        internal set

    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * Asks politely first; some ROMs (vivo) ignore that after a force-stop, so if the listener is
     * still not connected, briefly disable and re-enable the component, which makes the system
     * bind it again.
     */
    suspend fun ensureBound(context: Context) {
        if (!isGranted(context) || isConnected) return
        requestRebind(context)
        delay(CONNECT_WAIT_MS)
        if (isConnected || !isGranted(context)) return
        Log.i(TAG, "Listener still unbound, toggling component")
        toggleComponent(context)
    }

    fun requestRebind(context: Context) {
        if (!isGranted(context)) return
        try {
            NotificationListenerService.requestRebind(component(context))
        } catch (e: RuntimeException) {
            Log.w(TAG, "requestRebind failed", e)
        }
    }

    private fun toggleComponent(context: Context) {
        val pm = context.packageManager
        val component = component(context)
        try {
            pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Component toggle failed", e)
        }
    }

    private fun component(context: Context) = ComponentName(context, BankNotificationListener::class.java)
}

package com.expensetrack.setup

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/** Opens the system screens a user needs to keep the app alive in the background. */
object SetupIntents {
    private const val TAG = "SetupIntents"
    const val CCB_PACKAGE = "com.chinamworld.main"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Vendor auto-start screen when this ROM has one we know, else the app's system info page. */
    fun openAutoStart(context: Context, guide: RomGuide) {
        val opened = guide.autoStartScreens.any { screen ->
            tryStart(context, Intent().setComponent(ComponentName(screen.packageName, screen.className)))
        }
        if (!opened) openAppDetails(context, context.packageName)
    }

    /** Battery and background switches live on the app's info page on every ROM we target. */
    fun openBatterySettings(context: Context) = openAppDetails(context, context.packageName)

    /** The bank app's own notification switch: no bank notification, nothing to capture. */
    fun openBankNotificationSettings(context: Context): Boolean =
        tryStart(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, CCB_PACKAGE),
        ) || openAppDetails(context, CCB_PACKAGE)

    private fun openAppDetails(context: Context, packageName: String): Boolean =
        tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    /** Vendor screens may be missing or not exported on a given system version. */
    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Log.i(TAG, "Not available: ${intent.component ?: intent.action}")
        false
    } catch (e: SecurityException) {
        Log.i(TAG, "Not allowed: ${intent.component ?: intent.action}")
        false
    }
}

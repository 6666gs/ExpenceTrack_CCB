package com.expensetrack.settings

import android.content.Context
import java.time.LocalTime

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val cipher = SecretCipher()

    val summaryTime: LocalTime
        get() = LocalTime.of(
            prefs.getInt(KEY_HOUR, DEFAULT_SUMMARY_TIME.hour),
            prefs.getInt(KEY_MINUTE, DEFAULT_SUMMARY_TIME.minute),
        )

    fun setSummaryTime(time: LocalTime) {
        prefs.edit().putInt(KEY_HOUR, time.hour).putInt(KEY_MINUTE, time.minute).apply()
    }

    /** Cheap presence check that skips the Keystore decryption. */
    val hasDeepSeekApiKey: Boolean get() = prefs.contains(KEY_DEEPSEEK_KEY)

    /** Decrypted DeepSeek key, or null when none is saved. */
    val deepSeekApiKey: String?
        get() = prefs.getString(KEY_DEEPSEEK_KEY, null)?.let(cipher::decrypt)?.ifBlank { null }

    fun setDeepSeekApiKey(key: String?) {
        val trimmed = key?.trim().orEmpty()
        prefs.edit().apply {
            if (trimmed.isEmpty()) remove(KEY_DEEPSEEK_KEY) else putString(KEY_DEEPSEEK_KEY, cipher.encrypt(trimmed))
        }.apply()
    }

    /** Model picked by the user; null until a connection test has listed the account's models. */
    val deepSeekModel: String?
        get() = prefs.getString(KEY_DEEPSEEK_MODEL, null)?.ifBlank { null }

    fun setDeepSeekModel(model: String?) {
        prefs.edit().apply {
            if (model.isNullOrBlank()) remove(KEY_DEEPSEEK_MODEL) else putString(KEY_DEEPSEEK_MODEL, model.trim())
        }.apply()
    }

    /** Model ids returned by the last successful connection test. */
    val deepSeekModels: List<String>
        get() = prefs.getString(KEY_DEEPSEEK_MODELS, null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()

    fun setDeepSeekModels(models: List<String>) {
        prefs.edit().putString(KEY_DEEPSEEK_MODELS, models.joinToString(",")).apply()
    }

    /** Set once the user confirms they have gone through the background-run steps. */
    var backgroundSetupDone: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_SETUP_DONE, value).apply()

    companion object {
        /** 傍晚 by default; adjustable in the app. */
        val DEFAULT_SUMMARY_TIME: LocalTime = LocalTime.of(19, 0)
        private const val KEY_HOUR = "summary_hour"
        private const val KEY_MINUTE = "summary_minute"
        private const val KEY_DEEPSEEK_KEY = "deepseek_api_key"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        private const val KEY_DEEPSEEK_MODELS = "deepseek_models"
        private const val KEY_BACKGROUND_SETUP_DONE = "background_setup_done"
    }
}

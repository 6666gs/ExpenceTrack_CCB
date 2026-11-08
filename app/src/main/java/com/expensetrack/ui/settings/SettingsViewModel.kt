package com.expensetrack.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensetrack.ExpenseApp
import com.expensetrack.ai.DeepSeekClient
import com.expensetrack.ai.DeepSeekException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface KeyStatus {
    data object Idle : KeyStatus
    data object Saved : KeyStatus
    data object Cleared : KeyStatus
    data object Testing : KeyStatus
    data class Valid(val modelCount: Int) : KeyStatus
    data class Invalid(val message: String) : KeyStatus
}

data class AiSettingsState(
    val savedKey: String?,
    val model: String?,
    /** Models the account can use, as listed by the last successful connection test. */
    val availableModels: List<String>,
    val status: KeyStatus,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as ExpenseApp).settings
    private val client = DeepSeekClient()

    private val _state = MutableStateFlow(
        AiSettingsState(
            savedKey = settings.deepSeekApiKey,
            model = settings.deepSeekModel,
            availableModels = settings.deepSeekModels,
            status = KeyStatus.Idle,
        ),
    )
    val state: StateFlow<AiSettingsState> = _state.asStateFlow()

    fun saveKey(key: String) {
        settings.setDeepSeekApiKey(key)
        val saved = settings.deepSeekApiKey
        _state.update { it.copy(savedKey = saved, status = if (saved == null) KeyStatus.Cleared else KeyStatus.Saved) }
    }

    fun clearKey() {
        saveKey("")
        settings.setDeepSeekModels(emptyList())
        settings.setDeepSeekModel(null)
        _state.update { it.copy(availableModels = emptyList(), model = null) }
    }

    fun selectModel(model: String) {
        settings.setDeepSeekModel(model)
        _state.update { it.copy(model = model) }
    }

    /** Keeps the current choice if the account still offers it, otherwise picks the first model. */
    private fun applyModels(models: List<String>) {
        val current = _state.value.model
        val chosen = current?.takeIf { it in models } ?: models.firstOrNull()
        settings.setDeepSeekModels(models)
        settings.setDeepSeekModel(chosen)
        _state.update { it.copy(availableModels = models, model = chosen) }
    }

    /** Checks [key] (what is typed, saved or not) by listing the account's models. */
    fun testKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(status = KeyStatus.Invalid("请先输入 API Key")) }
            return
        }
        _state.update { it.copy(status = KeyStatus.Testing) }
        viewModelScope.launch {
            val status = try {
                val models = client.listModels(trimmed)
                applyModels(models)
                KeyStatus.Valid(models.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DeepSeekException) {
                KeyStatus.Invalid(e.message.orEmpty())
            }
            _state.update { it.copy(status = status) }
        }
    }
}

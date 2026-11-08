package com.expensetrack.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/** Error with a message that can be shown to the user as-is. */
class DeepSeekException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal client for DeepSeek's OpenAI-compatible API, using only the platform HTTP stack.
 * Calls block on [Dispatchers.IO].
 */
class DeepSeekClient(private val baseUrl: String = DEFAULT_BASE_URL) {

    /** Sends one system + user message and returns the assistant's reply text. */
    suspend fun chat(apiKey: String, model: String, system: String, user: String): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", user))
        val body = JSONObject().put("model", model).put("messages", messages).put("stream", false)

        val response = try {
            post(apiKey, body.put("response_format", JSONObject().put("type", "json_object")))
        } catch (e: HttpStatusException) {
            // Some models don't accept JSON mode; the prompt already asks for JSON, so retry without it.
            if (e.code != 400) throw e.toUserError()
            body.remove("response_format")
            try { post(apiKey, body) } catch (retry: HttpStatusException) { throw retry.toUserError() }
        }

        val content = JSONObject(response).optJSONArray("choices")
            ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
        return content?.ifBlank { null } ?: throw DeepSeekException("DeepSeek 没有返回内容，请重试")
    }

    /** Checks the key and returns the model ids it can use. */
    suspend fun listModels(apiKey: String): List<String> {
        val response = try {
            request("GET", "/models", apiKey, null)
        } catch (e: HttpStatusException) {
            throw e.toUserError()
        }
        val data = JSONObject(response).optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).mapNotNull { data.optJSONObject(it)?.optString("id")?.ifBlank { null } }
    }

    private suspend fun post(apiKey: String, body: JSONObject): String =
        request("POST", "/chat/completions", apiKey, body.toString())

    private suspend fun request(method: String, path: String, apiKey: String, body: String?): String =
        withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            try {
                body?.let { connection.outputStream.use { out -> out.write(it.toByteArray(Charsets.UTF_8)) } }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw HttpStatusException(code, errorBody)
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: SocketTimeoutException) {
                throw DeepSeekException("等待 DeepSeek 响应超时，请稍后重试", e)
            } catch (e: IOException) {
                throw DeepSeekException("网络连接失败，请检查网络", e)
            } finally {
                connection.disconnect()
            }
        }

    private class HttpStatusException(val code: Int, val body: String) : Exception("HTTP $code") {
        fun toUserError() = DeepSeekException(errorMessage(code, body), this)
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"

        private const val CONNECT_TIMEOUT_MS = 15_000
        /** Reasoning models can think for a while before answering. */
        private const val READ_TIMEOUT_MS = 180_000

        fun errorMessage(code: Int, body: String): String {
            val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }
                .getOrNull()?.ifBlank { null }
            return when (code) {
                401 -> "API Key 无效，请到设置里检查"
                402 -> "DeepSeek 账户余额不足，请先充值"
                429 -> "请求太频繁，请稍后再试"
                500, 502, 503 -> "DeepSeek 服务繁忙，请稍后再试"
                else -> "请求失败（$code）" + (detail?.let { "：$it" } ?: "")
            }
        }
    }
}

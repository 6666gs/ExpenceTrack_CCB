package com.expensetrack.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class DeepSeekClientTest {
    @Test
    fun `known status codes map to friendly messages`() {
        assertEquals("API Key 无效，请到设置里检查", DeepSeekClient.errorMessage(401, ""))
        assertEquals("DeepSeek 账户余额不足，请先充值", DeepSeekClient.errorMessage(402, ""))
        assertEquals("请求太频繁，请稍后再试", DeepSeekClient.errorMessage(429, ""))
    }

    @Test
    fun `other codes include the server message when present`() {
        assertEquals("请求失败（422）：Invalid model", DeepSeekClient.errorMessage(422, """{"error":{"message":"Invalid model"}}"""))
        assertEquals("请求失败（418）", DeepSeekClient.errorMessage(418, "not json"))
    }
}

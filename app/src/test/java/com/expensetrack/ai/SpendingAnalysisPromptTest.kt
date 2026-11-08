package com.expensetrack.ai

import com.expensetrack.data.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SpendingAnalysisPromptTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun expense(id: Long, cents: Long, minute: Int, note: String? = null, merchant: String? = null) = Expense(
        id = id, amountCents = cents,
        timestamp = LocalDateTime.of(2026, 11, 8, 12, minute).atZone(zone).toInstant().toEpochMilli(),
        note = note, merchant = merchant, source = "test", rawText = "【建设银行】尾号1234 消费",
    )

    private val lunch = expense(1, 2500, 0, note = "午饭 | 牛肉面")
    private val metro = expense(2, 400, 5, merchant = "地铁")
    private val unknown = expense(3, 10000, 10)

    @Test
    fun `prompt numbers entries chronologically and never includes raw bank text`() {
        val request = SpendingAnalysisPrompt.build("今天", listOf(unknown, metro, lunch), zone)

        assertEquals(listOf(lunch, metro, unknown), request.numbered)
        assertTrue(request.userPrompt.contains("1|11-8 12:00|25.00|午饭 / 牛肉面"))
        assertTrue(request.userPrompt.contains("2|11-8 12:05|4.00|地铁"))
        assertTrue(request.userPrompt.contains("3|11-8 12:10|100.00|\n"))
        assertTrue(request.userPrompt.contains("共 3 笔消费，合计 ¥129.00"))
        assertFalse(request.userPrompt.contains("尾号"))
    }

    @Test
    fun `parse sums amounts locally and puts unassigned entries in their own group`() {
        val request = SpendingAnalysisPrompt.build("今天", listOf(lunch, metro, unknown), zone)
        val reply = """```json
            {"summary":"花得不多","categories":[{"name":"餐饮","ids":[1]},{"name":"交通","ids":[2,1,99]}],
             "insights":["午饭 25 元"," "],"suggestions":["给大额消费写备注"]}
            ```"""

        val result = SpendingAnalysisPrompt.parse(reply, request)

        assertEquals("花得不多", result.summary)
        assertEquals(listOf(SpendingAnalysisPrompt.UNASSIGNED_CATEGORY, "餐饮", "交通"), result.categories.map { it.name })
        assertEquals(listOf(10000L, 2500L, 400L), result.categories.map { it.totalCents })
        assertEquals(1f, result.categories.sumOf { it.fraction.toDouble() }.toFloat(), 0.001f)
        assertEquals(listOf("午饭 25 元"), result.insights)
        assertEquals(listOf("给大额消费写备注"), result.suggestions)
    }

    @Test
    fun `entries beyond the limit are summarised instead of listed`() {
        val many = (1..SpendingAnalysisPrompt.MAX_ENTRIES + 2).map { expense(it.toLong(), it.toLong(), it % 60) }
        val request = SpendingAnalysisPrompt.build("本月", many, zone)

        assertEquals(SpendingAnalysisPrompt.MAX_ENTRIES, request.numbered.size)
        assertEquals(listOf(1L, 2L), request.omitted.map { it.amountCents }.sorted())
        assertTrue(request.userPrompt.contains("另有 2 笔小额消费"))

        val result = SpendingAnalysisPrompt.parse("""{"summary":"","categories":[]}""", request)
        assertEquals(2, result.omittedCount)
        assertTrue(result.categories.any { it.name == SpendingAnalysisPrompt.OMITTED_CATEGORY && it.count == 2 })
    }

    @Test
    fun `parse caps observations and suggestions to keep the card short`() {
        val request = SpendingAnalysisPrompt.build("今天", listOf(lunch), zone)
        val reply = """{"summary":"s","categories":[],"insights":["a","b","c"],"suggestions":["x","y"]}"""

        val result = SpendingAnalysisPrompt.parse(reply, request)

        assertEquals(listOf("a", "b"), result.insights)
        assertEquals(listOf("x"), result.suggestions)
    }

    @Test(expected = AnalysisFormatException::class)
    fun `non json reply is rejected`() {
        SpendingAnalysisPrompt.parse("抱歉，我无法完成", SpendingAnalysisPrompt.build("今天", listOf(lunch), zone))
    }
}

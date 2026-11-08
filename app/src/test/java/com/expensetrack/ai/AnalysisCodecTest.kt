package com.expensetrack.ai

import com.expensetrack.data.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisCodecTest {
    private val analysis = SpendingAnalysis(
        summary = "钱主要花在吃饭",
        categories = listOf(CategoryShare("餐饮", 2500, 2, 0.625f), CategoryShare("交通", 1500, 1, 0.375f)),
        insights = listOf("午饭 2 笔共 25 元"),
        suggestions = listOf("补上备注"),
        omittedCount = 3,
    )

    @Test
    fun `encoded analysis decodes to an equal value`() {
        assertEquals(analysis, AnalysisCodec.decode(AnalysisCodec.encode(analysis)))
    }

    @Test
    fun `unreadable or future-version data decodes to null`() {
        assertNull(AnalysisCodec.decode("not json"))
        assertNull(AnalysisCodec.decode("""{"v":99,"categories":[]}"""))
        assertNull(AnalysisCodec.decode("""{"v":1}"""))
    }

    private fun expense(id: Long, note: String? = null) =
        Expense(id = id, amountCents = 100 * id, timestamp = 1_000L * id, note = note, source = "test")

    @Test
    fun `fingerprint ignores order but notices added, removed and re-noted records`() {
        val base = listOf(expense(1), expense(2, "午饭"))
        val fingerprint = RecordsFingerprint.of(base)

        assertEquals(fingerprint, RecordsFingerprint.of(base.reversed()))
        assertNotEquals(fingerprint, RecordsFingerprint.of(base + expense(3)))
        assertNotEquals(fingerprint, RecordsFingerprint.of(base.take(1)))
        assertNotEquals(fingerprint, RecordsFingerprint.of(listOf(expense(1), expense(2, "晚饭"))))
    }
}

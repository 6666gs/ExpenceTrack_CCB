package com.expensetrack.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {

    private fun expense(source: String, raw: String?) =
        Expense(amountCents = 2500, timestamp = 0, source = source, rawText = raw)

    @Test
    fun `app push and sms for the same purchase are duplicates`() {
        val sms = expense("com.android.mms", "sms text")
        val push = expense("com.chinamworld.main", "push text")
        assertTrue(DuplicateDetector.isDuplicate(push, listOf(sms)))
    }

    @Test
    fun `re-posted identical notification is a duplicate`() {
        val first = expense("com.chinamworld.main", "same")
        assertTrue(DuplicateDetector.isDuplicate(first.copy(), listOf(first)))
    }

    @Test
    fun `two real purchases from the same app with different text are kept`() {
        val first = expense("com.chinamworld.main", "12:30 消费25.00元")
        val second = expense("com.chinamworld.main", "12:31 消费25.00元")
        assertFalse(DuplicateDetector.isDuplicate(second, listOf(first)))
    }

    @Test
    fun `manual entries never suppress captured ones`() {
        val manual = expense(SOURCE_MANUAL, null)
        assertFalse(DuplicateDetector.isDuplicate(expense("com.chinamworld.main", "x"), listOf(manual)))
    }

    @Test
    fun `no nearby records means not a duplicate`() {
        assertFalse(DuplicateDetector.isDuplicate(expense("com.chinamworld.main", "x"), emptyList()))
    }
}

package com.expensetrack.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses yuan strings into cents`() {
        assertEquals(2500L, Money.parseYuanToCents("25"))
        assertEquals(2550L, Money.parseYuanToCents("25.5"))
        assertEquals(123456L, Money.parseYuanToCents("1,234.56"))
        assertEquals(1L, Money.parseYuanToCents("0.01"))
    }

    @Test
    fun `rejects invalid or non-positive amounts`() {
        assertNull(Money.parseYuanToCents(""))
        assertNull(Money.parseYuanToCents("abc"))
        assertNull(Money.parseYuanToCents("0"))
        assertNull(Money.parseYuanToCents("-3"))
        assertNull(Money.parseYuanToCents("1.234"))
    }

    @Test
    fun `formats cents as yuan`() {
        assertEquals("¥0.00", Money.format(0))
        assertEquals("¥25.00", Money.format(2500))
        assertEquals("¥1,234.56", Money.format(123456))
    }

    @Test
    fun `input form round-trips through the parser`() {
        assertEquals("25", Money.toInput(2500))
        assertEquals("25.5", Money.toInput(2550))
        assertEquals("1234.56", Money.toInput(123456))
        assertEquals("0.01", Money.toInput(1))
        listOf(1L, 2550L, 123456L, 99_999_999L).forEach { assertEquals(it, Money.parseYuanToCents(Money.toInput(it))) }
    }

    @Test
    fun `compact format fits calendar cells`() {
        assertEquals("8", Money.formatCompact(800))
        assertEquals("12.5", Money.formatCompact(1250))
        assertEquals("10", Money.formatCompact(1000))
        assertEquals("1280", Money.formatCompact(128000))
        assertEquals("1.2万", Money.formatCompact(1_234_500))
        assertEquals("0", Money.formatCompact(0))
    }
}

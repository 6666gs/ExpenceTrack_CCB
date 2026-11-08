package com.expensetrack.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CcbNotificationParserTest {

    @Test
    fun `parses debit card spending sms`() {
        val text = "您尾号1234的储蓄卡11月7日12时30分消费支出人民币25.00元,活期余额1,234.56元。[建设银行]"
        val result = CcbNotificationParser.parse(text)
        assertEquals(2500L, result?.amountCents)
    }

    @Test
    fun `parses quick payment with thousands separator and merchant`() {
        val text = "您尾号1234的储蓄卡于11月7日12:30在财付通-微信支付快捷支付支出人民币1,025.50元，可用余额3,000.00元。【建设银行】"
        val result = CcbNotificationParser.parse(text)
        assertEquals(102550L, result?.amountCents)
        assertEquals("财付通-微信支付", result?.merchant)
    }

    @Test
    fun `parses credit card spending with one decimal`() {
        val text = "您尾号5678的龙卡信用卡11月07日18:22在美团消费人民币38.9元。"
        val result = CcbNotificationParser.parse(text)
        assertEquals(3890L, result?.amountCents)
        assertEquals("美团", result?.merchant)
    }

    @Test
    fun `parses yen sign amount`() {
        val result = CcbNotificationParser.parse("交易提醒 消费金额：¥12.00 交易时间：11月7日")
        assertEquals(1200L, result?.amountCents)
    }

    @Test
    fun `parses integer amount with yuan suffix`() {
        assertEquals(1200L, CcbNotificationParser.parse("您的账户支出12元")?.amountCents)
    }

    @Test
    fun `ignores salary income`() {
        assertNull(CcbNotificationParser.parse("您尾号1234的储蓄卡11月7日09时10分工资收入人民币8000.00元,活期余额9000.00元"))
    }

    @Test
    fun `ignores refunds and reversals`() {
        assertNull(CcbNotificationParser.parse("您尾号1234的储蓄卡11月7日退货存入人民币25.00元"))
        assertNull(CcbNotificationParser.parse("您尾号1234的信用卡消费撤销人民币25.00元"))
    }

    @Test
    fun `ignores text without currency marker`() {
        assertNull(CcbNotificationParser.parse("您尾号1234的储蓄卡已开通消费提醒"))
    }

    @Test
    fun `does not treat balance as spending`() {
        assertNull(CcbNotificationParser.parse("您的储蓄卡活期余额1,234.56元"))
    }

    @Test
    fun `recognises bank source by package or keyword`() {
        assertTrue(CcbNotificationParser.isFromBank("com.chinamworld.main", "动账提醒"))
        assertTrue(CcbNotificationParser.isFromBank("com.android.mms", "95533 您尾号1234..."))
        assertTrue(CcbNotificationParser.isFromBank("com.tencent.mm", "中国建设银行 交易提醒"))
        assertFalse(CcbNotificationParser.isFromBank("com.taobao.taobao", "您的订单已支付25.00元"))
    }
}

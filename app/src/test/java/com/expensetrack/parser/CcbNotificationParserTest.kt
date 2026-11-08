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
    fun `spending is not income`() {
        val result = CcbNotificationParser.parse("您尾号1234的储蓄账户11月8日19时3分支出人民币1.00元。点击查看>>")
        assertEquals(100L, result?.amountCents)
        assertEquals(false, result?.isIncome)
    }

    @Test
    fun `parses salary income`() {
        val result = CcbNotificationParser.parse("您尾号1234的储蓄卡11月7日09时10分工资收入人民币8,000.00元,活期余额9,000.00元")
        assertEquals(800000L, result?.amountCents)
        assertEquals(true, result?.isIncome)
    }

    @Test
    fun `parses transfers in as income`() {
        assertIncome(10000L, "您尾号1234的储蓄账户11月8日10时5分财付通转入人民币100.00元，活期余额200.00元")
        assertIncome(5050L, "您尾号1234的储蓄账户入账人民币50.50元")
        assertIncome(30000L, "您尾号1234的账户11月8日跨行汇入RMB300元")
    }

    @Test
    fun `refunds and reversals count as income, not spending`() {
        assertIncome(2500L, "您尾号1234的储蓄卡11月7日退货存入人民币25.00元")
        assertIncome(2500L, "您尾号1234的信用卡消费撤销人民币25.00元")
        assertIncome(1990L, "您尾号1234的储蓄卡收到美团退款¥19.90")
    }

    @Test
    fun `ignores card repayments, which only move money between own accounts`() {
        assertNull(CcbNotificationParser.parse("您尾号1234的储蓄卡支出人民币1000.00元，用于信用卡还款"))
        assertNull(CcbNotificationParser.parse("您尾号5678的龙卡信用卡还款入账人民币1000.00元"))
    }

    @Test
    fun `income keyword without an amount is ignored rather than read as spending`() {
        assertNull(CcbNotificationParser.parse("您已开通收入提醒，消费人民币时也会通知您"))
    }

    private fun assertIncome(cents: Long, text: String) {
        val result = CcbNotificationParser.parse(text)
        assertEquals(text, cents, result?.amountCents)
        assertEquals(text, true, result?.isIncome)
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

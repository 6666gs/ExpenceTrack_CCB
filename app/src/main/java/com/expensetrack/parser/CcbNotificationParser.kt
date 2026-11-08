package com.expensetrack.parser

import com.expensetrack.util.Money

/** One money movement read from a bank notification. Refunds and reversals count as income. */
data class ParsedTransaction(val amountCents: Long, val merchant: String?, val isIncome: Boolean = false)

/**
 * Extracts amounts from China Construction Bank (建设银行) notifications —
 * the CCB app push, the 95533 SMS, or the WeChat service account.
 *
 * Money out (消费、支付、支出…) is spending; money in (收入、存入、转入、退款、撤销…) is income.
 * Card repayments only move money between the user's own accounts, so they are ignored.
 */
object CcbNotificationParser {

    private val BANK_PACKAGES = setOf(
        "com.chinamworld.main", // 中国建设银行 App
    )
    private val BANK_KEYWORDS = listOf("建设银行", "建行", "龙卡", "95533")

    private val IGNORED_KEYWORDS = listOf("还款")

    private const val INCOME_WORDS = "收入|存入|转入|入账|汇入|退款|退货|撤销|冲正"
    private const val SPEND_WORDS = "支出|消费|支付|扣款|取款|转出|缴费"

    /** Checked before spending: "消费撤销" and "退货存入" mention spending words too. */
    private val INCOME_KEYWORD = Regex(INCOME_WORDS)

    /**
     * Keyword, up to 12 non-digit chars (never crossing into "余额"), optional currency marker,
     * the number, optional "元". A currency marker or "元" is required so dates aren't read as amounts.
     */
    private fun amountAfter(words: String) = Regex(
        """(?:$words)[^0-9余]{0,12}?(人民币|RMB|CNY|[¥￥])?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(元)?"""
    )

    private val AMOUNT_AFTER_INCOME = amountAfter(INCOME_WORDS)
    private val AMOUNT_AFTER_SPEND = amountAfter(SPEND_WORDS)

    private val MERCHANT = Regex("""在([^，,。；;\s]{1,30})(?:消费|支出|支付)""")
    private val MERCHANT_SUFFIXES = listOf("快捷支付", "网上支付", "网络支付", "进行")

    fun isFromBank(packageName: String, text: String): Boolean =
        packageName in BANK_PACKAGES || BANK_KEYWORDS.any { it in text }

    fun parse(text: String): ParsedTransaction? {
        if (IGNORED_KEYWORDS.any { it in text }) return null
        // Text that mentions income is never read as spending, even without a usable amount.
        if (INCOME_KEYWORD.containsMatchIn(text)) {
            return firstAmount(AMOUNT_AFTER_INCOME, text)?.let { ParsedTransaction(it, merchant = null, isIncome = true) }
        }
        val amountCents = firstAmount(AMOUNT_AFTER_SPEND, text) ?: return null
        return ParsedTransaction(amountCents, extractMerchant(text))
    }

    private fun firstAmount(pattern: Regex, text: String): Long? =
        pattern.findAll(text)
            .filter { it.groups[1] != null || it.groups[3] != null }
            .firstNotNullOfOrNull { Money.parseYuanToCents(it.groupValues[2]) }

    private fun extractMerchant(text: String): String? {
        val raw = MERCHANT.find(text)?.groupValues?.get(1) ?: return null
        val cleaned = MERCHANT_SUFFIXES.fold(raw) { acc, suffix -> acc.removeSuffix(suffix) }
        return cleaned.ifBlank { null }
    }
}

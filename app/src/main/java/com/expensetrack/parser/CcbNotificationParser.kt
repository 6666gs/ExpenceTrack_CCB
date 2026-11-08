package com.expensetrack.parser

import com.expensetrack.util.Money

data class ParsedExpense(val amountCents: Long, val merchant: String?)

/**
 * Extracts spending amounts from China Construction Bank (建设银行) notifications —
 * the CCB app push, the 95533 SMS, or the WeChat service account.
 *
 * Only outgoing money is recorded; income, refunds, reversals and card repayments are ignored.
 */
object CcbNotificationParser {

    private val BANK_PACKAGES = setOf(
        "com.chinamworld.main", // 中国建设银行 App
    )
    private val BANK_KEYWORDS = listOf("建设银行", "建行", "龙卡", "95533")

    private val NON_SPENDING_KEYWORDS = listOf(
        "收入", "存入", "转入", "退款", "退货", "撤销", "冲正", "还款",
    )

    /**
     * Spending keyword, up to 12 non-digit chars (never crossing into "余额"),
     * optional currency marker, the number, optional "元".
     */
    private val AMOUNT_AFTER_SPEND = Regex(
        """(?:支出|消费|支付|扣款|取款|转出|缴费)[^0-9余]{0,12}?(人民币|RMB|CNY|[¥￥])?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(元)?"""
    )

    private val MERCHANT = Regex("""在([^，,。；;\s]{1,30})(?:消费|支出|支付)""")
    private val MERCHANT_SUFFIXES = listOf("快捷支付", "网上支付", "网络支付", "进行")

    fun isFromBank(packageName: String, text: String): Boolean =
        packageName in BANK_PACKAGES || BANK_KEYWORDS.any { it in text }

    fun parse(text: String): ParsedExpense? {
        if (NON_SPENDING_KEYWORDS.any { it in text }) return null
        val amountCents = AMOUNT_AFTER_SPEND.findAll(text)
            .filter { it.groups[1] != null || it.groups[3] != null }
            .firstNotNullOfOrNull { Money.parseYuanToCents(it.groupValues[2]) }
            ?: return null
        return ParsedExpense(amountCents, extractMerchant(text))
    }

    private fun extractMerchant(text: String): String? {
        val raw = MERCHANT.find(text)?.groupValues?.get(1) ?: return null
        val cleaned = MERCHANT_SUFFIXES.fold(raw) { acc, suffix -> acc.removeSuffix(suffix) }
        return cleaned.ifBlank { null }
    }
}

package com.expensetrack.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Money {
    /** Upper bound for a single transaction, to reject obviously mis-parsed numbers. */
    private val MAX_YUAN = BigDecimal("10000000")
    private val YUAN_PATTERN = Regex("""^\d{1,3}(,\d{3})*(\.\d{1,2})?$|^\d+(\.\d{1,2})?$""")

    /** "1,234.5" -> 123450. Returns null for blank, malformed, zero, negative or huge input. */
    fun parseYuanToCents(input: String): Long? {
        val trimmed = input.trim()
        if (!YUAN_PATTERN.matches(trimmed)) return null
        val yuan = BigDecimal(trimmed.replace(",", ""))
        if (yuan.signum() <= 0 || yuan > MAX_YUAN) return null
        return yuan.movePointRight(2).longValueExact()
    }

    /** Short form for tight spaces such as calendar cells: "8", "12.5", "1280", "1.2万". */
    fun formatCompact(cents: Long): String {
        val yuan = BigDecimal.valueOf(cents, 2)
        return when {
            yuan < BigDecimal(1000) -> yuan.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            yuan < BigDecimal(10000) -> yuan.setScale(0, RoundingMode.HALF_UP).toPlainString()
            else -> yuan.movePointLeft(4).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "万"
        }
    }

    /** Plain editable form that [parseYuanToCents] accepts: 2550 -> "25.5", 2500 -> "25". */
    fun toInput(cents: Long): String = BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()

    /** "+¥12.00" for money in, "-¥12.00" for money out, "¥0.00" for nothing. */
    fun formatSigned(cents: Long): String = when {
        cents > 0 -> "+" + format(cents)
        cents < 0 -> "-" + format(-cents)
        else -> format(0)
    }

    fun format(cents: Long): String {
        val formatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        return "¥" + formatter.format(BigDecimal.valueOf(cents, 2))
    }
}

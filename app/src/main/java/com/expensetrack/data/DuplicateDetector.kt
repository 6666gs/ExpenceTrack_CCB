package com.expensetrack.data

/**
 * The same purchase often arrives twice: once as a CCB app push and once as a 95533 SMS,
 * and notifications get re-posted when updated. A captured expense counts as a duplicate when an
 * existing auto-captured record has the same amount within [WINDOW_MS] and either came from a
 * different app (push vs SMS) or has the identical text (re-posted notification).
 *
 * Income and spending of the same amount are never duplicates of each other.
 *
 * Two genuine identical purchases from the same app still differ in their timestamp text,
 * so they are both kept.
 */
object DuplicateDetector {
    const val WINDOW_MS: Long = 3 * 60 * 1000

    fun isDuplicate(candidate: Expense, nearbySameAmount: List<Expense>): Boolean =
        nearbySameAmount
            .filterNot { it.isManual }
            .filter { it.isIncome == candidate.isIncome }
            .any { it.source != candidate.source || it.rawText == candidate.rawText }
}

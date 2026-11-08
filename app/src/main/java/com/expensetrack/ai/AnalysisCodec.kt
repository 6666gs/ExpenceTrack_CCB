package com.expensetrack.ai

import com.expensetrack.data.Expense
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest

/** Encodes a [SpendingAnalysis] for the saved_analyses table. */
object AnalysisCodec {
    private const val VERSION = 1

    fun encode(analysis: SpendingAnalysis): String = JSONObject()
        .put("v", VERSION)
        .put("summary", analysis.summary)
        .put("insights", JSONArray(analysis.insights))
        .put("suggestions", JSONArray(analysis.suggestions))
        .put("omittedCount", analysis.omittedCount)
        .put(
            "categories",
            JSONArray().apply {
                analysis.categories.forEach {
                    put(
                        JSONObject()
                            .put("name", it.name)
                            .put("totalCents", it.totalCents)
                            .put("count", it.count)
                            .put("fraction", it.fraction.toDouble()),
                    )
                }
            },
        )
        .toString()

    /** Returns null for data written by an incompatible version or corrupted on disk. */
    fun decode(json: String): SpendingAnalysis? = try {
        val root = JSONObject(json)
        if (root.optInt("v") != VERSION) {
            null
        } else {
            val categories = root.getJSONArray("categories")
            SpendingAnalysis(
                summary = root.optString("summary"),
                categories = (0 until categories.length()).map { i ->
                    val c = categories.getJSONObject(i)
                    CategoryShare(c.getString("name"), c.getLong("totalCents"), c.getInt("count"),
                        c.getDouble("fraction").toFloat())
                },
                insights = root.optJSONArray("insights").strings(),
                suggestions = root.optJSONArray("suggestions").strings(),
                omittedCount = root.optInt("omittedCount"),
            )
        }
    } catch (e: JSONException) {
        null
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}

/**
 * A digest of everything the analysis depends on. Adding, deleting or re-noting a record in the
 * period changes it, which marks the saved analysis as outdated. Order-independent.
 */
object RecordsFingerprint {
    fun of(expenses: List<Expense>): String {
        val canonical = expenses.sortedBy { it.id }.joinToString("\n") {
            "${it.id}|${it.amountCents}|${it.timestamp}|${it.note.orEmpty()}|${it.merchant.orEmpty()}"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

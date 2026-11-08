package com.expensetrack.ai

import com.expensetrack.data.Expense
import com.expensetrack.util.Money
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CategoryShare(val name: String, val totalCents: Long, val count: Int, val fraction: Float)

data class SpendingAnalysis(
    val summary: String,
    val categories: List<CategoryShare>,
    val insights: List<String>,
    val suggestions: List<String>,
    /** Entries left out of the prompt because the period had more than [SpendingAnalysisPrompt.MAX_ENTRIES]. */
    val omittedCount: Int,
)

/** Prompt for the model, plus the entries in the order they were numbered (1-based). */
data class AnalysisRequest(val userPrompt: String, val numbered: List<Expense>, val omitted: List<Expense>)

/**
 * Builds the DeepSeek prompt and turns its JSON reply into a [SpendingAnalysis].
 *
 * The model only assigns entry numbers to categories; amounts per category are summed here,
 * so the distribution always adds up exactly to the real total.
 * Only amount, time, the user's note and the merchant are sent — never the raw bank text.
 */
object SpendingAnalysisPrompt {
    const val MAX_ENTRIES = 500
    const val OMITTED_CATEGORY = "其他小额（未分析）"
    const val UNASSIGNED_CATEGORY = "未分类"

    private val TIME_FORMAT = DateTimeFormatter.ofPattern("M-d HH:mm")

    val SYSTEM = """
        你是一位务实的个人理财助手，帮用户回顾一段时间的银行卡消费。
        语气直接、具体，引用真实金额和笔数，不说空话，不编造记录里没有的信息。
        只输出一个 JSON 对象，不要输出任何其他文字。
    """.trimIndent()

    fun build(periodLabel: String, expenses: List<Expense>, zone: ZoneId = ZoneId.systemDefault()): AnalysisRequest {
        val byAmount = expenses.sortedByDescending { it.amountCents }
        val included = byAmount.take(MAX_ENTRIES).sortedBy { it.timestamp }
        val omitted = byAmount.drop(MAX_ENTRIES)
        val total = expenses.sumOf { it.amountCents }

        val lines = included.mapIndexed { i, e ->
            val time = Instant.ofEpochMilli(e.timestamp).atZone(zone).format(TIME_FORMAT)
            val yuan = Money.format(e.amountCents).removePrefix("¥")
            "${i + 1}|$time|$yuan|${describe(e)}"
        }
        val omittedLine = if (omitted.isEmpty()) "" else
            "（另有 ${omitted.size} 笔小额消费，合计 ${Money.format(omitted.sumOf { it.amountCents })}，未逐笔列出）\n"

        val prompt = buildString {
            append("时间段：$periodLabel\n")
            append("共 ${expenses.size} 笔消费，合计 ${Money.format(total)}。\n")
            append(omittedLine)
            append("每行格式：编号|时间|金额(元)|描述（用户备注 / 商户，可能为空）\n")
            lines.forEach { append(it).append('\n') }
            append(
                """
                |
                |请完成：
                |1. 把每笔归入一个消费类别，例如 餐饮、交通、购物、日用、娱乐、居住、医疗、教育、人情、其他；可按实际情况自定。类别名不超过 4 个字，总数不超过 8 个。描述为空、无法判断用途的归入"未备注"。
                |2. summary：一句话总结，不超过 40 个字，点出钱主要花在哪。
                |3. insights：最多 2 条，每条不超过 25 个字，必须带具体金额或笔数。
                |4. suggestions：1 条可以马上执行的建议，不超过 25 个字。很多笔没有备注时，建议补上备注。
                |文字务必简短，不要客套话。
                |
                |输出格式：
                |{"summary":"…","categories":[{"name":"餐饮","ids":[1,5]}],"insights":["…"],"suggestions":["…"]}
                """.trimMargin(),
            )
        }
        return AnalysisRequest(prompt, included, omitted)
    }

    /** @throws AnalysisFormatException when the reply is not the JSON we asked for. */
    fun parse(reply: String, request: AnalysisRequest): SpendingAnalysis {
        val json = try {
            JSONObject(extractJsonObject(reply))
        } catch (e: JSONException) {
            throw AnalysisFormatException("AI 返回的内容无法解析，请重试", e)
        }

        val assigned = HashMap<Int, String>()
        val categoriesJson = json.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until categoriesJson.length()) {
            val category = categoriesJson.optJSONObject(i) ?: continue
            val name = category.optString("name").trim().take(MAX_CATEGORY_NAME)
            if (name.isEmpty()) continue
            val ids = category.optJSONArray("ids") ?: continue
            for (j in 0 until ids.length()) {
                val index = ids.optInt(j, 0) - 1
                if (index in request.numbered.indices) assigned.putIfAbsent(index, name)
            }
        }

        val groups = LinkedHashMap<String, MutableList<Expense>>()
        request.numbered.forEachIndexed { i, e -> groups.getOrPut(assigned[i] ?: UNASSIGNED_CATEGORY) { mutableListOf() }.add(e) }
        if (request.omitted.isNotEmpty()) groups[OMITTED_CATEGORY] = request.omitted.toMutableList()

        val grandTotal = groups.values.sumOf { list -> list.sumOf { it.amountCents } }.coerceAtLeast(1)
        val categories = groups.map { (name, list) ->
            val sum = list.sumOf { it.amountCents }
            CategoryShare(name, sum, list.size, sum.toFloat() / grandTotal)
        }.sortedByDescending { it.totalCents }

        return SpendingAnalysis(
            summary = json.optString("summary").trim(),
            categories = categories,
            // Hard caps keep the card short even if the model ignores the length rules.
            insights = json.optJSONArray("insights").toStringList().take(MAX_INSIGHTS),
            suggestions = json.optJSONArray("suggestions").toStringList().take(MAX_SUGGESTIONS),
            omittedCount = request.omitted.size,
        )
    }

    /** Models sometimes wrap JSON in ```json fences or add a sentence around it. */
    internal fun extractJsonObject(reply: String): String {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) throw JSONException("no JSON object in reply")
        return reply.substring(start, end + 1)
    }

    private fun describe(e: Expense): String =
        listOfNotNull(e.note, e.merchant).joinToString(" / ")
            .replace('|', '/')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).trim().ifBlank { null } }
    }

    private const val MAX_CATEGORY_NAME = 8
    const val MAX_INSIGHTS = 2
    const val MAX_SUGGESTIONS = 1
}

class AnalysisFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

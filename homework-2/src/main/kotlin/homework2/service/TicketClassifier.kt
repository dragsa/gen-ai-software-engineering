package homework2.service

import homework2.models.Category
import homework2.models.ClassificationDecision
import homework2.models.Priority
import homework2.models.Ticket
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Rule-based ticket classifier.
 *
 * Confidence formula (documented openly):
 *   categorySignal = matchedCategoryKeywords / totalKeywordsInWinningCategory
 *   prioritySignal = 1.0 if any priority keyword matched, else 0.5 (MEDIUM default)
 *   confidence     = (categorySignal * 0.7) + (prioritySignal * 0.3), clamped to [0.0, 1.0]
 *
 * Category is determined by the highest keyword-hit count across all category maps.
 * Priority is determined by first-match precedence: URGENT → HIGH → LOW → MEDIUM.
 *
 * All decisions are appended to an in-memory log accessible via [getDecisions].
 * The log has no public HTTP endpoint — it is internal only.
 */
class TicketClassifier(
    private val timestampProvider: () -> String = { Instant.now().toString() }
) {

    // --- keyword maps ---

    private val categoryKeywords: Map<Category, List<String>> = mapOf(
        Category.ACCOUNT_ACCESS to listOf(
            "login", "password", "2fa", "two-factor", "sign in", "sign-in",
            "locked out", "account access", "authentication", "forgot password", "reset password"
        ),
        Category.TECHNICAL_ISSUE to listOf(
            "error", "crash", "not working", "broken", "fails", "failure",
            "exception", "timeout", "slow", "performance", "down", "outage"
        ),
        Category.BILLING_QUESTION to listOf(
            "invoice", "billing", "charge", "payment", "refund", "subscription",
            "price", "cost", "receipt", "overcharged", "discount"
        ),
        Category.FEATURE_REQUEST to listOf(
            "feature", "request", "suggestion", "enhancement", "improve",
            "would like", "wish", "add support", "could you", "please add"
        ),
        Category.BUG_REPORT to listOf(
            "bug", "reproduce", "steps to reproduce", "expected behavior",
            "actual behavior", "defect", "regression", "version"
        )
    )

    private val priorityKeywords: Map<Priority, List<String>> = mapOf(
        Priority.URGENT to listOf(
            "can't access", "cannot access", "critical", "production down", "prod down",
            "security", "data loss", "urgent", "emergency", "immediately"
        ),
        Priority.HIGH to listOf(
            "important", "blocking", "blocked", "asap", "as soon as possible", "high priority"
        ),
        Priority.LOW to listOf(
            "minor", "cosmetic", "suggestion", "nice to have", "low priority", "whenever"
        )
    )

    // --- decision log ---

    private val decisions = mutableListOf<ClassificationDecision>()

    /**
     * Classify [ticket] and append the result to the internal decision log.
     * Returns the [ClassificationDecision] so the caller can persist it and respond.
     */
    fun classify(ticket: Ticket): ClassificationDecision {
        val text = "${ticket.subject} ${ticket.description}".lowercase()

        val categoryHits = categoryKeywords.mapValues { (_, keywords) ->
            keywords.filter { text.contains(it) }
        }

        val (winningCategory, matchedCategoryKeywords) = categoryHits
            .maxByOrNull { (_, hits) -> hits.size }
            ?.takeIf { (_, hits) -> hits.isNotEmpty() }
            ?.let { (cat, hits) -> cat to hits }
            ?: (Category.OTHER to emptyList())

        val (priority, matchedPriorityKeywords) = classifyPriority(text)

        val allKeywordsFound = (matchedCategoryKeywords + matchedPriorityKeywords)
            .distinct()
            .sorted()

        val categorySignal = if (winningCategory == Category.OTHER || matchedCategoryKeywords.isEmpty()) {
            0.0
        } else {
            val totalInCategory = categoryKeywords[winningCategory]?.size ?: 1
            matchedCategoryKeywords.size.toDouble() / totalInCategory
        }

        val prioritySignal = if (matchedPriorityKeywords.isEmpty()) 0.5 else 1.0

        val confidence = ((categorySignal * 0.7) + (prioritySignal * 0.3))
            .coerceIn(0.0, 1.0)
            .roundTo2Decimals()

        val reasoning = buildReasoning(winningCategory, matchedCategoryKeywords, priority, matchedPriorityKeywords)

        val decision = ClassificationDecision(
            ticketId      = ticket.id,
            category      = winningCategory,
            priority      = priority,
            confidence    = confidence,
            reasoning     = reasoning,
            keywordsFound = allKeywordsFound,
            decidedAt     = timestampProvider()
        )

        synchronized(decisions) { decisions.add(decision) }
        return decision
    }

    /** Returns a snapshot of all decisions logged so far. Internal use and tests only. */
    fun getDecisions(): List<ClassificationDecision> =
        synchronized(decisions) { decisions.toList() }

    // --- private helpers ---

    private fun classifyPriority(text: String): Pair<Priority, List<String>> {
        for (priority in listOf(Priority.URGENT, Priority.HIGH, Priority.LOW)) {
            val matched = priorityKeywords[priority]?.filter { text.contains(it) } ?: emptyList()
            if (matched.isNotEmpty()) return priority to matched
        }
        return Priority.MEDIUM to emptyList()
    }

    private fun buildReasoning(
        category: Category,
        categoryMatches: List<String>,
        priority: Priority,
        priorityMatches: List<String>
    ): String {
        val catPart = if (category == Category.OTHER || categoryMatches.isEmpty()) {
            "No category keywords matched; defaulting to '${Category.OTHER.value}'."
        } else {
            "Matched category '${category.value}' (${categoryMatches.size} keyword(s): ${categoryMatches.joinToString()})."
        }

        val priPart = if (priorityMatches.isEmpty()) {
            "No priority keywords matched; defaulting to '${Priority.MEDIUM.value}'."
        } else {
            "Priority set to '${priority.value}' (keyword(s): ${priorityMatches.joinToString()})."
        }

        return "$catPart $priPart"
    }

    private fun Double.roundTo2Decimals(): Double =
        (this * 100).roundToInt() / 100.0
}

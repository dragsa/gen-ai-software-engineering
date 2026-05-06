package homework2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * HTTP response returned by POST /tickets/{id}/auto-classify.
 * Constructed from a [ClassificationDecision] via [ClassificationResponse.from].
 */
@Serializable
data class ClassificationResponse(
    @SerialName("ticket_id")      val ticketId: String,
    val category:                 String,
    val priority:                 String,
    val confidence:               Double,
    val reasoning:                String,
    @SerialName("keywords_found") val keywordsFound: List<String>
) {
    companion object {
        fun from(decision: ClassificationDecision): ClassificationResponse =
            ClassificationResponse(
                ticketId     = decision.ticketId,
                category     = decision.category.value,
                priority     = decision.priority.value,
                confidence   = decision.confidence,
                reasoning    = decision.reasoning,
                keywordsFound = decision.keywordsFound
            )
    }
}

/**
 * Internal decision log entry — not serializable, never exposed via HTTP.
 * Stored in [homework2.service.TicketClassifier]'s in-memory log.
 */
data class ClassificationDecision(
    val ticketId:      String,
    val category:      Category,
    val priority:      Priority,
    val confidence:    Double,
    val reasoning:     String,
    val keywordsFound: List<String>,
    val decidedAt:     String
)

package homework6.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard message envelope exchanged between agents through the `shared/` directories.
 * Matches the protocol defined in agents.md.
 */
@Serializable
data class AgentMessage(
    @SerialName("message_id") val messageId: String,
    val timestamp: String,
    @SerialName("source_agent") val sourceAgent: String,
    @SerialName("target_agent") val targetAgent: String,
    @SerialName("message_type") val messageType: String,
    val data: TransactionData,
)

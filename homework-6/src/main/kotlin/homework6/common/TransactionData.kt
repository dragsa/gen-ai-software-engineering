package homework6.common

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Optional metadata carried by a raw transaction. */
@Serializable
data class Metadata(
    val channel: String? = null,
    val country: String? = null,
)

/**
 * The `data` payload of an [AgentMessage]. Holds the raw transaction fields plus the
 * pipeline-accumulated fields (status, reason, risk_*). All optional fields default to null so a
 * missing required field is detectable by the validator.
 */
@Serializable
data class TransactionData(
    @SerialName("transaction_id") val transactionId: String = "",
    val timestamp: String? = null,
    @SerialName("source_account") val sourceAccount: String? = null,
    @SerialName("destination_account") val destinationAccount: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    @SerialName("transaction_type") val transactionType: String? = null,
    val description: String? = null,
    val metadata: Metadata? = null,
    // --- pipeline-accumulated fields ---
    val status: String? = null,
    val reason: String? = null,
    @SerialName("risk_score") val riskScore: Int? = null,
    @SerialName("risk_reasons") val riskReasons: List<String> = emptyList(),
    @SerialName("near_threshold") val nearThreshold: Boolean? = null,
)

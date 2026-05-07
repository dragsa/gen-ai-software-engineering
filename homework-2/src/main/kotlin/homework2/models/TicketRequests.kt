package homework2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for POST /tickets.
 *
 * [category], [priority], and [status] are optional — omitting them applies server-side defaults
 * (see API_NOTES.md at the subproject root for the default values).
 * [metadata] is optional; omitting it applies the default metadata (see API_NOTES.md).
 */
@Serializable
data class CreateTicketRequest(
    @SerialName("customer_id")    val customerId: String,
    @SerialName("customer_email") val customerEmail: String,
    @SerialName("customer_name")  val customerName: String,
    val subject: String,
    val description: String,
    val category: String? = null,
    val priority: String? = null,
    val status: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: CreateMetadataRequest? = null
)

/**
 * Request body for PUT /tickets/:id.
 * All fields are nullable — only non-null fields are applied (partial update semantics).
 */
@Serializable
data class UpdateTicketRequest(
    @SerialName("customer_id")    val customerId: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_name")  val customerName: String? = null,
    val subject: String? = null,
    val description: String? = null,
    val category: String? = null,
    val priority: String? = null,
    val status: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    val tags: List<String>? = null,
    val metadata: UpdateMetadataRequest? = null
)

/**
 * Metadata section for ticket creation.
 * [source] is required — it must be supplied explicitly when metadata is provided.
 * [browser] and [device_type] are genuinely optional even in the domain.
 */
@Serializable
data class CreateMetadataRequest(
    val source: String,
    val browser: String? = null,
    @SerialName("device_type") val deviceType: String? = null
)

/**
 * Metadata section for ticket updates.
 * All fields are nullable — only non-null fields are applied (partial update semantics).
 */
@Serializable
data class UpdateMetadataRequest(
    val source: String? = null,
    val browser: String? = null,
    @SerialName("device_type") val deviceType: String? = null
)

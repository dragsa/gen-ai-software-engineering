package homework2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * HTTP response shape for a single ticket.
 * Constructed exclusively via [TicketResponse.from] — the single mapping point
 * from the domain [Ticket] entity.
 */
@Serializable
data class TicketResponse(
    val id: String,
    @SerialName("customer_id")    val customerId: String,
    @SerialName("customer_email") val customerEmail: String,
    @SerialName("customer_name")  val customerName: String,
    val subject: String,
    val description: String,
    val category: String,
    val priority: String,
    val status: String,
    @SerialName("created_at")  val createdAt: String,
    @SerialName("updated_at")  val updatedAt: String,
    @SerialName("resolved_at") val resolvedAt: String?,
    @SerialName("assigned_to") val assignedTo: String?,
    val tags: List<String>,
    val metadata: MetadataResponse
) {
    companion object {
        fun from(ticket: Ticket): TicketResponse = TicketResponse(
            id            = ticket.id,
            customerId    = ticket.customerId,
            customerEmail = ticket.customerEmail,
            customerName  = ticket.customerName,
            subject       = ticket.subject,
            description   = ticket.description,
            category      = ticket.category.value,
            priority      = ticket.priority.value,
            status        = ticket.status.value,
            createdAt     = ticket.createdAt,
            updatedAt     = ticket.updatedAt,
            resolvedAt    = ticket.resolvedAt,
            assignedTo    = ticket.assignedTo,
            tags          = ticket.tags,
            metadata      = MetadataResponse.from(ticket.metadata)
        )
    }
}

/**
 * Metadata section of [TicketResponse].
 */
@Serializable
data class MetadataResponse(
    val source: String,
    val browser: String?,
    @SerialName("device_type") val deviceType: String?
) {
    companion object {
        fun from(metadata: Metadata): MetadataResponse = MetadataResponse(
            source     = metadata.source.value,
            browser    = metadata.browser,
            deviceType = metadata.deviceType?.value
        )
    }
}

/**
 * Response for POST /tickets/import summarising how many records succeeded and failed.
 */
@Serializable
data class ImportSummaryResponse(
    val total: Int,
    val successful: Int,
    val failed: Int,
    val failures: List<ImportFailure>
)

/**
 * One entry in [ImportSummaryResponse.failures] describing a row that could not be imported.
 */
@Serializable
data class ImportFailure(
    val row: Int,
    val reason: String,
    @SerialName("raw_data") val rawData: String? = null
)

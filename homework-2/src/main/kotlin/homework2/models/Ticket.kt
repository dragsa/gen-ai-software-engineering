package homework2.models

/**
 * Internal domain entity. Not serializable — never exposed directly to the HTTP layer.
 * Routes map this to [TicketResponse] before sending to clients.
 */
data class Ticket(
    val id: String,
    val customerId: String,
    val customerEmail: String,
    val customerName: String,
    val subject: String,
    val description: String,
    val category: Category,
    val priority: Priority,
    val status: Status,
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String?,
    val assignedTo: String?,
    val tags: List<String>,
    val metadata: Metadata
)

/**
 * Nested metadata for a ticket. Part of the domain model — not serializable directly.
 */
data class Metadata(
    val source: Source,
    val browser: String?,
    val deviceType: DeviceType?
)

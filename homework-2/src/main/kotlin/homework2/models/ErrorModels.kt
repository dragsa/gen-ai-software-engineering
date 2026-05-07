package homework2.models

import kotlinx.serialization.Serializable

/**
 * A single field-level validation failure.
 * Used inside [ErrorResponse] for both deserialization and business-rule failures.
 */
@Serializable
data class ValidationError(val field: String, val message: String)

/**
 * Unified error response shape used across all endpoints.
 * Clients have one contract to handle regardless of error origin.
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val details: List<ValidationError> = emptyList()
)

/**
 * Internal filter parameters passed from the routing layer to [homework2.service.TicketRepository].
 * Not serializable — constructed from query parameters in the route handler.
 */
data class TicketFilter(
    val category: Category? = null,
    val priority: Priority? = null,
    val status: Status? = null,
    val assignedTo: String? = null,
    val customerId: String? = null,
    val search: String? = null
)

package homework2.validation

import homework2.models.Category
import homework2.models.CreateMetadataRequest
import homework2.models.CreateTicketRequest
import homework2.models.DeviceType
import homework2.models.Metadata
import homework2.models.Priority
import homework2.models.Source
import homework2.models.Status
import homework2.models.Ticket
import homework2.models.UpdateMetadataRequest
import homework2.models.UpdateTicketRequest
import homework2.models.ValidationError
import homework2.service.defaultMetadata

/**
 * Validates ticket request DTOs and maps them to domain entities.
 *
 * All methods return a [List] of [ValidationError] — they never throw for
 * business-rule failures. An empty list means the request is valid.
 */
class TicketValidator {

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /**
     * Validate a [CreateTicketRequest].
     * Returns all violations found — does not stop at the first error.
     */
    fun validateCreate(req: CreateTicketRequest): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (req.customerId.isBlank())
            errors += ValidationError("customer_id", "customer_id must not be blank")

        if (req.customerName.isBlank())
            errors += ValidationError("customer_name", "customer_name must not be blank")

        if (!emailRegex.matches(req.customerEmail))
            errors += ValidationError("customer_email", "customer_email must be a valid email address")

        if (req.subject.isEmpty() || req.subject.length > 200)
            errors += ValidationError("subject", "subject must be between 1 and 200 characters")

        if (req.description.length < 10 || req.description.length > 2000)
            errors += ValidationError("description", "description must be between 10 and 2000 characters")

        req.category?.let {
            if (Category.fromValue(it) == null)
                errors += ValidationError(
                    "category",
                    "category must be one of: ${Category.entries.joinToString { e -> e.value }}"
                )
        }

        req.priority?.let {
            if (Priority.fromValue(it) == null)
                errors += ValidationError(
                    "priority",
                    "priority must be one of: ${Priority.entries.joinToString { e -> e.value }}"
                )
        }

        req.status?.let {
            if (Status.fromValue(it) == null)
                errors += ValidationError(
                    "status",
                    "status must be one of: ${Status.entries.joinToString { e -> e.value }}"
                )
        }

        req.metadata?.let { errors += validateCreateMetadata(it) }

        return errors
    }

    /**
     * Validate an [UpdateTicketRequest].
     * Only non-null fields are checked — absent fields are not validated (partial update semantics).
     */
    fun validateUpdate(req: UpdateTicketRequest): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        req.customerId?.let {
            if (it.isBlank()) errors += ValidationError("customer_id", "customer_id must not be blank")
        }

        req.customerName?.let {
            if (it.isBlank()) errors += ValidationError("customer_name", "customer_name must not be blank")
        }

        req.customerEmail?.let {
            if (!emailRegex.matches(it))
                errors += ValidationError("customer_email", "customer_email must be a valid email address")
        }

        req.subject?.let {
            if (it.isEmpty() || it.length > 200)
                errors += ValidationError("subject", "subject must be between 1 and 200 characters")
        }

        req.description?.let {
            if (it.length < 10 || it.length > 2000)
                errors += ValidationError("description", "description must be between 10 and 2000 characters")
        }

        req.category?.let {
            if (Category.fromValue(it) == null)
                errors += ValidationError(
                    "category",
                    "category must be one of: ${Category.entries.joinToString { e -> e.value }}"
                )
        }

        req.priority?.let {
            if (Priority.fromValue(it) == null)
                errors += ValidationError(
                    "priority",
                    "priority must be one of: ${Priority.entries.joinToString { e -> e.value }}"
                )
        }

        req.status?.let {
            if (Status.fromValue(it) == null)
                errors += ValidationError(
                    "status",
                    "status must be one of: ${Status.entries.joinToString { e -> e.value }}"
                )
        }

        req.metadata?.let { errors += validateUpdateMetadata(it) }

        return errors
    }

    /**
     * Map a validated [CreateTicketRequest] to a domain [Ticket].
     *
     * Defaults applied when optional fields are omitted:
     *   category → OTHER
     *   priority → MEDIUM
     *   status   → NEW
     *   metadata → source=API, browser=null, device_type=null
     *
     * The [id] and [now] are supplied by the caller (repository or service).
     */
    fun toTicket(req: CreateTicketRequest, id: String, now: String): Ticket = Ticket(
        id            = id,
        customerId    = req.customerId,
        customerEmail = req.customerEmail,
        customerName  = req.customerName,
        subject       = req.subject,
        description   = req.description,
        category      = req.category?.let { Category.fromValue(it) } ?: Category.OTHER,
        priority      = req.priority?.let { Priority.fromValue(it) } ?: Priority.MEDIUM,
        status        = req.status?.let { Status.fromValue(it) } ?: Status.NEW,
        createdAt     = now,
        updatedAt     = now,
        resolvedAt    = null,
        assignedTo    = req.assignedTo,
        tags          = req.tags,
        metadata      = req.metadata?.toDomain() ?: defaultMetadata()
    )

    // --- private helpers ---

    private fun validateCreateMetadata(req: CreateMetadataRequest): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (Source.fromValue(req.source) == null)
            errors += ValidationError(
                "metadata.source",
                "metadata.source must be one of: ${Source.entries.joinToString { e -> e.value }}"
            )

        req.deviceType?.let {
            if (DeviceType.fromValue(it) == null)
                errors += ValidationError(
                    "metadata.device_type",
                    "metadata.device_type must be one of: ${DeviceType.entries.joinToString { e -> e.value }}"
                )
        }

        return errors
    }

    private fun validateUpdateMetadata(req: UpdateMetadataRequest): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        req.source?.let {
            if (Source.fromValue(it) == null)
                errors += ValidationError(
                    "metadata.source",
                    "metadata.source must be one of: ${Source.entries.joinToString { e -> e.value }}"
                )
        }

        req.deviceType?.let {
            if (DeviceType.fromValue(it) == null)
                errors += ValidationError(
                    "metadata.device_type",
                    "metadata.device_type must be one of: ${DeviceType.entries.joinToString { e -> e.value }}"
                )
        }

        return errors
    }

    private fun CreateMetadataRequest.toDomain(): Metadata = Metadata(
        source     = Source.fromValue(source) ?: Source.API,
        browser    = browser,
        deviceType = deviceType?.let { DeviceType.fromValue(it) }
    )
}

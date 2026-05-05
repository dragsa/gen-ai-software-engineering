package homework2.service

import homework2.models.Category
import homework2.models.CreateTicketRequest
import homework2.models.DeviceType
import homework2.models.ImportFailure
import homework2.models.ImportSummaryResponse
import homework2.models.Metadata
import homework2.models.Priority
import homework2.models.Source
import homework2.models.Status
import homework2.models.Ticket
import homework2.models.TicketFilter
import homework2.models.UpdateTicketRequest
import homework2.models.UpdateMetadataRequest
import homework2.utils.parsers.ParsedRow
import homework2.validation.TicketValidator
import java.time.Instant
import java.util.UUID

/**
 * Production implementation of [TicketService].
 *
 * [idGenerator] and [timestampProvider] are injected so tests can produce
 * deterministic ids and timestamps without real I/O.
 */
class TicketServiceImpl(
    private val repository: TicketRepository,
    private val validator: TicketValidator,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val timestampProvider: () -> String = { Instant.now().toString() }
) : TicketService {

    override fun createTicket(req: CreateTicketRequest): Ticket {
        val now = timestampProvider()
        val id = idGenerator()
        val ticket = validator.toTicket(req, id, now)
        return repository.create(ticket)
    }

    override fun getTicket(id: String): Ticket? = repository.findById(id)

    override fun listTickets(filter: TicketFilter): List<Ticket> = repository.findAll(filter)

    override fun updateTicket(id: String, req: UpdateTicketRequest): Ticket? =
        repository.update(id) { existing -> applyUpdate(existing, req) }

    override fun deleteTicket(id: String): Boolean = repository.delete(id)

    override fun bulkImport(parsedRows: List<ParsedRow>): ImportSummaryResponse {
        val failures = mutableListOf<ImportFailure>()
        var successful = 0

        parsedRows.forEach { parsed ->
            when (parsed) {
                is ParsedRow.Failure -> {
                    failures += ImportFailure(parsed.row, parsed.error, parsed.rawData)
                }
                is ParsedRow.Success -> {
                    val errors = validator.validateCreate(parsed.request)
                    if (errors.isNotEmpty()) {
                        failures += ImportFailure(
                            row     = parsed.row,
                            reason  = errors.joinToString("; ") { "${it.field}: ${it.message}" },
                            rawData = null
                        )
                    } else {
                        createTicket(parsed.request)
                        successful++
                    }
                }
            }
        }

        return ImportSummaryResponse(
            total      = parsedRows.size,
            successful = successful,
            failed     = failures.size,
            failures   = failures
        )
    }

    // --- private helpers ---

    /**
     * Apply non-null fields from [req] to [existing].
     * Server-managed fields (id, createdAt, resolvedAt) are never overwritten here —
     * resolvedAt lifecycle is handled by [InMemoryTicketRepository.update].
     */
    private fun applyUpdate(existing: Ticket, req: UpdateTicketRequest): Ticket = existing.copy(
        customerId    = req.customerId    ?: existing.customerId,
        customerEmail = req.customerEmail ?: existing.customerEmail,
        customerName  = req.customerName  ?: existing.customerName,
        subject       = req.subject       ?: existing.subject,
        description   = req.description   ?: existing.description,
        category      = req.category?.let { Category.fromValue(it) } ?: existing.category,
        priority      = req.priority?.let { Priority.fromValue(it) } ?: existing.priority,
        status        = req.status?.let   { Status.fromValue(it) }   ?: existing.status,
        assignedTo    = req.assignedTo    ?: existing.assignedTo,
        tags          = req.tags          ?: existing.tags,
        metadata      = req.metadata?.let { mergeMetadata(existing.metadata, it) } ?: existing.metadata
    )

    /**
     * Merge a partial [UpdateMetadataRequest] into an existing [Metadata].
     * Only non-null fields in the request replace the current values.
     */
    private fun mergeMetadata(existing: Metadata, req: UpdateMetadataRequest): Metadata = existing.copy(
        source     = req.source?.let { Source.fromValue(it) }         ?: existing.source,
        browser    = req.browser                                        ?: existing.browser,
        deviceType = req.deviceType?.let { DeviceType.fromValue(it) } ?: existing.deviceType
    )
}

package homework2.service

import homework2.models.ClassificationDecision
import homework2.models.CreateTicketRequest
import homework2.models.ImportSummaryResponse
import homework2.models.Ticket
import homework2.models.TicketFilter
import homework2.models.UpdateTicketRequest
import homework2.utils.parsers.ParsedRow

/**
 * Application-level service contract for ticket operations.
 * Routes depend on this interface — never on the implementation —
 * so tests can substitute fakes without rewiring the application.
 */
interface TicketService {

    /** Create and persist a new ticket from a validated request. */
    fun createTicket(req: CreateTicketRequest): Ticket

    /** Return the ticket with the given [id], or null if not found. */
    fun getTicket(id: String): Ticket?

    /** Return all tickets matching [filter]. */
    fun listTickets(filter: TicketFilter): List<Ticket>

    /**
     * Apply non-null fields from [req] to the ticket with the given [id] and persist.
     * Returns the updated ticket, or null if no ticket with that id exists.
     */
    fun updateTicket(id: String, req: UpdateTicketRequest): Ticket?

    /**
     * Delete the ticket with the given [id].
     * Returns true if deleted, false if not found.
     */
    fun deleteTicket(id: String): Boolean

    /**
     * Validate and persist each [ParsedRow.Success] in [parsedRows].
     * [ParsedRow.Failure] rows and rows that fail business validation are recorded
     * as import failures. Returns a summary of the whole operation.
     */
    fun bulkImport(parsedRows: List<ParsedRow>): ImportSummaryResponse

    /**
     * Run the auto-classifier on the ticket with the given [id], persist the
     * resulting category and priority onto the ticket, and return the decision.
     * Returns null if no ticket with that id exists.
     */
    fun classifyTicket(id: String): ClassificationDecision?
}

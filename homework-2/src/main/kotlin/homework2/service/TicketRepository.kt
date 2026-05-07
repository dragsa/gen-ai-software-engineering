package homework2.service

import homework2.models.Ticket
import homework2.models.TicketFilter

/**
 * Persistence contract for tickets.
 * Routes and services depend on this interface — never on a concrete implementation —
 * so tests can substitute fakes without rewiring the application.
 */
interface TicketRepository {

    /** Persist a new ticket and return it unchanged. */
    fun create(ticket: Ticket): Ticket

    /** Return the ticket with the given [id], or null if not found. */
    fun findById(id: String): Ticket?

    /**
     * Return all tickets matching [filter].
     * A filter with all-null fields returns every ticket.
     */
    fun findAll(filter: TicketFilter): List<Ticket>

    /**
     * Apply [updater] to the ticket with the given [id] and persist the result.
     * Returns the updated ticket, or null if no ticket with that id exists.
     */
    fun update(id: String, updater: (Ticket) -> Ticket): Ticket?

    /**
     * Remove the ticket with the given [id].
     * Returns true if a ticket was found and removed, false if not found.
     */
    fun delete(id: String): Boolean
}

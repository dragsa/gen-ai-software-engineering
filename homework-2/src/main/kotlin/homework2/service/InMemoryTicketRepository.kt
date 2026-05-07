package homework2.service

import homework2.models.Metadata
import homework2.models.Source
import homework2.models.Status
import homework2.models.Ticket
import homework2.models.TicketFilter
import java.time.Instant
import java.util.UUID

/**
 * Thread-safe in-memory implementation of [TicketRepository].
 *
 * [idGenerator] and [timestampProvider] are injected so tests can produce
 * deterministic ids and timestamps without real I/O.
 */
class InMemoryTicketRepository(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val timestampProvider: () -> String = { Instant.now().toString() }
) : TicketRepository {

    private val store = mutableListOf<Ticket>()

    override fun create(ticket: Ticket): Ticket {
        val now = timestampProvider()
        val persisted = ticket.copy(
            id = idGenerator(),
            createdAt = now,
            updatedAt = now,
            resolvedAt = null
        )
        synchronized(store) { store.add(persisted) }
        return persisted
    }

    override fun findById(id: String): Ticket? =
        snapshot().firstOrNull { it.id == id }

    override fun findAll(filter: TicketFilter): List<Ticket> =
        snapshot().filter { ticket ->
            matchesCategory(ticket, filter) &&
            matchesPriority(ticket, filter) &&
            matchesStatus(ticket, filter) &&
            matchesAssignedTo(ticket, filter) &&
            matchesCustomerId(ticket, filter) &&
            matchesSearch(ticket, filter)
        }

    override fun update(id: String, updater: (Ticket) -> Ticket): Ticket? =
        synchronized(store) {
            val index = store.indexOfFirst { it.id == id }
            if (index == -1) return null
            val now = timestampProvider()
            val updated = updater(store[index]).let { t ->
                val shouldSetResolvedAt =
                    (t.status == Status.RESOLVED || t.status == Status.CLOSED) &&
                    store[index].resolvedAt == null
                t.copy(
                    updatedAt = now,
                    resolvedAt = if (shouldSetResolvedAt) now else t.resolvedAt
                )
            }
            store[index] = updated
            updated
        }

    override fun delete(id: String): Boolean =
        synchronized(store) { store.removeIf { it.id == id } }

    // --- private helpers ---

    private fun snapshot(): List<Ticket> = synchronized(store) { store.toList() }

    private fun matchesCategory(ticket: Ticket, filter: TicketFilter): Boolean =
        filter.category == null || ticket.category == filter.category

    private fun matchesPriority(ticket: Ticket, filter: TicketFilter): Boolean =
        filter.priority == null || ticket.priority == filter.priority

    private fun matchesStatus(ticket: Ticket, filter: TicketFilter): Boolean =
        filter.status == null || ticket.status == filter.status

    private fun matchesAssignedTo(ticket: Ticket, filter: TicketFilter): Boolean =
        filter.assignedTo == null || ticket.assignedTo == filter.assignedTo

    private fun matchesCustomerId(ticket: Ticket, filter: TicketFilter): Boolean =
        filter.customerId == null || ticket.customerId == filter.customerId

    private fun matchesSearch(ticket: Ticket, filter: TicketFilter): Boolean {
        val term = filter.search ?: return true
        val lower = term.lowercase()
        return ticket.subject.lowercase().contains(lower) ||
               ticket.description.lowercase().contains(lower)
    }
}

/** Convenience factory used by [homework2.validation.TicketValidator.toTicket]. */
fun defaultMetadata(): Metadata = Metadata(
    source = Source.API,
    browser = null,
    deviceType = null
)

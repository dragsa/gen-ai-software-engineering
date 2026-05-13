package homework2.service

import homework2.models.Category
import homework2.models.CreateTicketRequest
import homework2.models.Priority
import homework2.models.Source
import homework2.models.Status
import homework2.models.TicketFilter
import homework2.models.UpdateMetadataRequest
import homework2.models.UpdateTicketRequest
import homework2.utils.parsers.ParsedRow
import homework2.validation.TicketValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TicketServiceImpl] covering bulkImport, listTickets filters,
 * updateTicket partial-update semantics, and the resolvedAt lifecycle.
 *
 * All tests use a real [InMemoryTicketRepository] with deterministic IDs so
 * no mocks are needed.
 */
class TicketServiceTest {

    private val validator = TicketValidator()
    private val classifier = TicketClassifier()

    private fun service(counter: Int = 0): Pair<TicketServiceImpl, InMemoryTicketRepository> {
        var seq = counter
        val repo = InMemoryTicketRepository(
            idGenerator       = { "id-${++seq}" },
            timestampProvider = { "2026-01-01T00:00:00Z" }
        )
        return TicketServiceImpl(repo, validator, classifier) to repo
    }

    private fun validRequest(
        customerId: String    = "cust-001",
        email: String         = "alice@example.com",
        name: String          = "Alice",
        subject: String       = "Cannot login to my account",
        description: String   = "I have been unable to login since yesterday morning",
        category: String?     = null,
        priority: String?     = null
    ) = CreateTicketRequest(
        customerId    = customerId,
        customerEmail = email,
        customerName  = name,
        subject       = subject,
        description   = description,
        category      = category,
        priority      = priority
    )

    // --- bulkImport ---

    @Test
    fun `bulkImport with all valid rows returns correct counts`() {
        val (svc, _) = service()
        val rows = listOf(
            ParsedRow.Success(1, validRequest(customerId = "c-1")),
            ParsedRow.Success(2, validRequest(customerId = "c-2")),
            ParsedRow.Success(3, validRequest(customerId = "c-3"))
        )

        val summary = svc.bulkImport(rows)

        assertEquals(3, summary.total)
        assertEquals(3, summary.successful)
        assertEquals(0, summary.failed)
        assertTrue(summary.failures.isEmpty())
    }

    @Test
    fun `bulkImport with one ParsedRow Failure records it in summary`() {
        val (svc, _) = service()
        val rows = listOf(
            ParsedRow.Success(1, validRequest(customerId = "c-1")),
            ParsedRow.Failure(2, "malformed row", "raw data here"),
            ParsedRow.Success(3, validRequest(customerId = "c-3"))
        )

        val summary = svc.bulkImport(rows)

        assertEquals(3, summary.total)
        assertEquals(2, summary.successful)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.failures.size)
        assertEquals(2, summary.failures[0].row)
        assertEquals("malformed row", summary.failures[0].reason)
    }

    @Test
    fun `bulkImport records validation failures for invalid Success rows`() {
        val (svc, _) = service()
        val invalidReq = validRequest(email = "not-an-email")  // will fail validation
        val rows = listOf(
            ParsedRow.Success(1, invalidReq)
        )

        val summary = svc.bulkImport(rows)

        assertEquals(1, summary.total)
        assertEquals(0, summary.successful)
        assertEquals(1, summary.failed)
        assertTrue(summary.failures[0].reason.contains("customer_email"))
    }

    @Test
    fun `bulkImport with all Failure rows returns all failed`() {
        val (svc, _) = service()
        val rows = listOf(
            ParsedRow.Failure(1, "error one", null),
            ParsedRow.Failure(2, "error two", "raw")
        )

        val summary = svc.bulkImport(rows)

        assertEquals(2, summary.total)
        assertEquals(0, summary.successful)
        assertEquals(2, summary.failed)
    }

    // --- listTickets filters ---

    @Test
    fun `listTickets with category filter returns only matching tickets`() {
        val (svc, _) = service()
        svc.createTicket(validRequest(category = "account_access"))
        svc.createTicket(validRequest(category = "billing_question"))

        val results = svc.listTickets(TicketFilter(category = Category.ACCOUNT_ACCESS))

        assertEquals(1, results.size)
        assertEquals(Category.ACCOUNT_ACCESS, results[0].category)
    }

    @Test
    fun `listTickets with priority filter returns only matching tickets`() {
        val (svc, _) = service()
        svc.createTicket(validRequest(priority = "high"))
        svc.createTicket(validRequest(priority = "low"))

        val results = svc.listTickets(TicketFilter(priority = Priority.HIGH))

        assertEquals(1, results.size)
        assertEquals(Priority.HIGH, results[0].priority)
    }

    @Test
    fun `listTickets with status filter returns only matching tickets`() {
        val (svc, _) = service()
        val ticket = svc.createTicket(validRequest())

        // Update one ticket to in_progress
        svc.updateTicket(ticket.id, UpdateTicketRequest(status = "in_progress"))

        val newTickets  = svc.listTickets(TicketFilter(status = Status.NEW))
        val inProgress  = svc.listTickets(TicketFilter(status = Status.IN_PROGRESS))

        assertEquals(0, newTickets.size)
        assertEquals(1, inProgress.size)
    }

    @Test
    fun `listTickets with customerId filter returns only matching tickets`() {
        val (svc, _) = service()
        svc.createTicket(validRequest(customerId = "cust-A"))
        svc.createTicket(validRequest(customerId = "cust-B"))

        val results = svc.listTickets(TicketFilter(customerId = "cust-A"))

        assertEquals(1, results.size)
        assertEquals("cust-A", results[0].customerId)
    }

    @Test
    fun `listTickets with search filter matches subject`() {
        val (svc, _) = service()
        svc.createTicket(validRequest(subject = "Login issue"))
        svc.createTicket(validRequest(
            subject     = "Payment problem",
            description = "My payment was charged but the invoice still shows unpaid"
        ))

        val results = svc.listTickets(TicketFilter(search = "login"))

        assertEquals(1, results.size)
        assertTrue(results[0].subject.lowercase().contains("login"))
    }

    @Test
    fun `listTickets with search filter matches description`() {
        val (svc, _) = service()
        svc.createTicket(validRequest(description = "I cannot access my account at all"))
        svc.createTicket(validRequest(description = "My invoice shows wrong amount"))

        val results = svc.listTickets(TicketFilter(search = "invoice"))

        assertEquals(1, results.size)
    }

    @Test
    fun `listTickets with assignedTo filter returns only matching tickets`() {
        val (svc, _) = service()
        val t1 = svc.createTicket(validRequest())
        svc.createTicket(validRequest())

        svc.updateTicket(t1.id, UpdateTicketRequest(assignedTo = "agent-007"))

        val results = svc.listTickets(TicketFilter(assignedTo = "agent-007"))
        assertEquals(1, results.size)
        assertEquals("agent-007", results[0].assignedTo)
    }

    // --- resolvedAt lifecycle ---

    @Test
    fun `updateTicket sets resolvedAt when status changes to RESOLVED`() {
        val (svc, _) = service()
        val ticket = svc.createTicket(validRequest())

        assertNull(ticket.resolvedAt)

        val updated = svc.updateTicket(ticket.id, UpdateTicketRequest(status = "resolved"))

        assertNotNull(updated)
        assertNotNull(updated.resolvedAt)
    }

    @Test
    fun `updateTicket sets resolvedAt when status changes to CLOSED`() {
        val (svc, _) = service()
        val ticket = svc.createTicket(validRequest())

        val updated = svc.updateTicket(ticket.id, UpdateTicketRequest(status = "closed"))

        assertNotNull(updated)
        assertNotNull(updated.resolvedAt)
    }

    @Test
    fun `updateTicket does not overwrite existing resolvedAt`() {
        val (svc, repo) = service()
        val ticket = svc.createTicket(validRequest())

        // First resolution sets resolvedAt
        svc.updateTicket(ticket.id, UpdateTicketRequest(status = "resolved"))
        val afterFirst = repo.findById(ticket.id)!!.resolvedAt

        // Subsequent update keeps the original resolvedAt
        svc.updateTicket(ticket.id, UpdateTicketRequest(status = "closed"))
        val afterSecond = repo.findById(ticket.id)!!.resolvedAt

        assertEquals(afterFirst, afterSecond)
    }

    // --- applyUpdate / mergeMetadata ---

    @Test
    fun `updateTicket with metadata merges only non-null fields`() {
        val (svc, _) = service()
        val req = validRequest()
        val ticket = svc.createTicket(req)

        val updated = svc.updateTicket(
            ticket.id,
            UpdateTicketRequest(
                metadata = UpdateMetadataRequest(source = "email")
            )
        )

        assertNotNull(updated)
        assertEquals(Source.EMAIL, updated.metadata.source)
        // browser and deviceType remain null (they were never set)
        assertNull(updated.metadata.browser)
        assertNull(updated.metadata.deviceType)
    }

    @Test
    fun `deleteTicket returns false for nonexistent id`() {
        val (svc, _) = service()
        val result = svc.deleteTicket("does-not-exist")
        assertEquals(false, result)
    }

    @Test
    fun `getTicket returns null for nonexistent id`() {
        val (svc, _) = service()
        assertNull(svc.getTicket("does-not-exist"))
    }

    @Test
    fun `updateTicket returns null for nonexistent id`() {
        val (svc, _) = service()
        val result = svc.updateTicket("does-not-exist", UpdateTicketRequest())
        assertNull(result)
    }
}

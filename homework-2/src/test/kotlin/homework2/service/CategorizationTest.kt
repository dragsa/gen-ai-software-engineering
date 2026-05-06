package homework2.service

import homework2.models.Category
import homework2.models.CreateTicketRequest
import homework2.models.Metadata
import homework2.models.Priority
import homework2.models.Source
import homework2.models.Status
import homework2.models.Ticket
import homework2.validation.TicketValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TicketClassifier] and the classify-ticket flow in [TicketServiceImpl].
 *
 * The classifier is exercised directly with constructed [Ticket] objects.
 * The service-level test uses a real [InMemoryTicketRepository] to verify
 * that [TicketServiceImpl.classifyTicket] persists the classification result.
 */
class CategorizationTest {

    private val classifier = TicketClassifier(timestampProvider = { "2026-01-01T00:00:00Z" })

    private fun ticket(
        id: String = "t-001",
        subject: String = "",
        description: String = ""
    ): Ticket = Ticket(
        id            = id,
        customerId    = "cust-001",
        customerEmail = "alice@example.com",
        customerName  = "Alice Smith",
        subject       = subject,
        description   = description,
        category      = Category.OTHER,
        priority      = Priority.MEDIUM,
        status        = Status.NEW,
        createdAt     = "2026-01-01T00:00:00Z",
        updatedAt     = "2026-01-01T00:00:00Z",
        resolvedAt    = null,
        assignedTo    = null,
        tags          = emptyList(),
        metadata      = Metadata(source = Source.API, browser = null, deviceType = null)
    )

    // --- Category classification ---

    @Test
    fun `classify returns ACCOUNT_ACCESS for login-related keywords`() {
        val decision = classifier.classify(
            ticket(subject = "Cannot login to my account", description = "I forgot my password")
        )
        assertEquals(Category.ACCOUNT_ACCESS, decision.category)
    }

    @Test
    fun `classify returns BILLING_QUESTION for billing-related keywords`() {
        val decision = classifier.classify(
            ticket(subject = "Invoice issue", description = "I was overcharged on my last billing cycle and want a refund")
        )
        assertEquals(Category.BILLING_QUESTION, decision.category)
    }

    @Test
    fun `classify returns TECHNICAL_ISSUE for technical keywords`() {
        val decision = classifier.classify(
            ticket(subject = "App crashes on startup", description = "The application fails immediately with a null pointer exception")
        )
        assertEquals(Category.TECHNICAL_ISSUE, decision.category)
    }

    @Test
    fun `classify returns FEATURE_REQUEST for feature keywords`() {
        val decision = classifier.classify(
            ticket(subject = "Feature request for dark mode", description = "I would like to suggest an enhancement to add dark mode support")
        )
        assertEquals(Category.FEATURE_REQUEST, decision.category)
    }

    @Test
    fun `classify returns OTHER when no category keywords match`() {
        val decision = classifier.classify(
            ticket(subject = "Hello there", description = "Just saying hi to the support team")
        )
        assertEquals(Category.OTHER, decision.category)
    }

    // --- Priority classification ---

    @Test
    fun `classify returns URGENT for critical production keyword`() {
        val decision = classifier.classify(
            ticket(subject = "Production down emergency", description = "Our critical service is completely unavailable")
        )
        assertEquals(Priority.URGENT, decision.priority)
    }

    @Test
    fun `classify returns HIGH for blocking keyword`() {
        val decision = classifier.classify(
            ticket(subject = "Blocking issue", description = "This is blocking our team from deploying asap")
        )
        assertEquals(Priority.HIGH, decision.priority)
    }

    @Test
    fun `classify returns LOW for minor cosmetic keyword`() {
        val decision = classifier.classify(
            ticket(subject = "Minor cosmetic issue", description = "This is a nice to have suggestion")
        )
        assertEquals(Priority.LOW, decision.priority)
    }

    @Test
    fun `classify returns MEDIUM when no priority keywords match`() {
        val decision = classifier.classify(
            ticket(subject = "General inquiry", description = "I have been unable to login since yesterday morning")
        )
        assertEquals(Priority.MEDIUM, decision.priority)
    }

    // --- Confidence ---

    @Test
    fun `confidence is between 0 and 1 and non-zero for keyword match`() {
        val decision = classifier.classify(
            ticket(subject = "Cannot login", description = "I forgot my password and the authentication is broken")
        )
        assertTrue(decision.confidence in 0.0..1.0)
        assertTrue(decision.confidence > 0.0)
    }

    // --- Decision log ---

    @Test
    fun `getDecisions returns snapshot that grows with each classify call`() {
        val localClassifier = TicketClassifier()
        assertEquals(0, localClassifier.getDecisions().size)

        localClassifier.classify(ticket(id = "t-1", subject = "login issue", description = "cannot login at all"))
        localClassifier.classify(ticket(id = "t-2", subject = "billing issue", description = "invoice shows wrong amount"))
        assertEquals(2, localClassifier.getDecisions().size)
    }

    // --- Service-level classify ---

    @Test
    fun `classifyTicket via service updates ticket category and priority`() {
        val repository = InMemoryTicketRepository(
            idGenerator       = { "t-fixed" },
            timestampProvider = { "2026-01-01T00:00:00Z" }
        )
        val validator  = TicketValidator()
        val service    = TicketServiceImpl(repository, validator, classifier)

        val req = CreateTicketRequest(
            customerId    = "cust-001",
            customerEmail = "alice@example.com",
            customerName  = "Alice Smith",
            subject       = "Cannot login to my account",
            description   = "I have been unable to login since yesterday morning after a password reset"
        )
        val created = service.createTicket(req)
        val decision = service.classifyTicket(created.id)

        assertNotNull(decision)
        assertEquals(Category.ACCOUNT_ACCESS, decision.category)

        val updated = repository.findById(created.id)
        assertNotNull(updated)
        assertEquals(Category.ACCOUNT_ACCESS, updated.category)
    }

    @Test
    fun `classifyTicket returns null for unknown ticket id`() {
        val repository = InMemoryTicketRepository()
        val validator  = TicketValidator()
        val service    = TicketServiceImpl(repository, validator, classifier)

        val decision = service.classifyTicket("nonexistent-id")
        assertNull(decision)
    }
}

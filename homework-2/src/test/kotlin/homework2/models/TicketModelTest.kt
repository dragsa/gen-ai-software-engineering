package homework2.models

import homework2.validation.TicketValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for domain model helpers and the [TicketValidator].
 *
 * These tests exercise enum companion factories and validation logic
 * without starting the HTTP server.
 */
class TicketModelTest {

    private val validator = TicketValidator()

    // --- Enum.fromValue ---

    @Test
    fun `Category fromValue returns matching entry`() {
        assertEquals(Category.ACCOUNT_ACCESS, Category.fromValue("account_access"))
        assertEquals(Category.BILLING_QUESTION, Category.fromValue("billing_question"))
        assertEquals(Category.OTHER, Category.fromValue("other"))
    }

    @Test
    fun `Category fromValue is case-insensitive`() {
        assertEquals(Category.TECHNICAL_ISSUE, Category.fromValue("TECHNICAL_ISSUE"))
        assertEquals(Category.FEATURE_REQUEST, Category.fromValue("Feature_Request"))
    }

    @Test
    fun `Category fromValue returns null for unknown value`() {
        assertNull(Category.fromValue("unknown_category"))
        assertNull(Category.fromValue(""))
    }

    @Test
    fun `Priority fromValue returns all entries correctly`() {
        assertEquals(Priority.URGENT, Priority.fromValue("urgent"))
        assertEquals(Priority.HIGH,   Priority.fromValue("high"))
        assertEquals(Priority.MEDIUM, Priority.fromValue("medium"))
        assertEquals(Priority.LOW,    Priority.fromValue("low"))
    }

    @Test
    fun `Status fromValue returns all entries correctly`() {
        assertEquals(Status.NEW,              Status.fromValue("new"))
        assertEquals(Status.IN_PROGRESS,      Status.fromValue("in_progress"))
        assertEquals(Status.WAITING_CUSTOMER, Status.fromValue("waiting_customer"))
        assertEquals(Status.RESOLVED,         Status.fromValue("resolved"))
        assertEquals(Status.CLOSED,           Status.fromValue("closed"))
    }

    @Test
    fun `Source fromValue returns all entries correctly`() {
        assertEquals(Source.WEB_FORM, Source.fromValue("web_form"))
        assertEquals(Source.EMAIL,    Source.fromValue("email"))
        assertEquals(Source.API,      Source.fromValue("api"))
        assertEquals(Source.CHAT,     Source.fromValue("chat"))
        assertEquals(Source.PHONE,    Source.fromValue("phone"))
    }

    @Test
    fun `DeviceType fromValue returns all entries correctly`() {
        assertEquals(DeviceType.DESKTOP, DeviceType.fromValue("desktop"))
        assertEquals(DeviceType.MOBILE,  DeviceType.fromValue("mobile"))
        assertEquals(DeviceType.TABLET,  DeviceType.fromValue("tablet"))
        assertNull(DeviceType.fromValue("smartwatch"))
    }

    // --- TicketValidator.validateCreate ---

    @Test
    fun `validateCreate returns empty list for fully valid request`() {
        val req = CreateTicketRequest(
            customerId    = "cust-001",
            customerEmail = "alice@example.com",
            customerName  = "Alice Smith",
            subject       = "Cannot login to my account",
            description   = "I have been unable to login since yesterday morning"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }

    @Test
    fun `validateCreate reports error for blank customer_id`() {
        val req = CreateTicketRequest(
            customerId    = "  ",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "customer_id" })
    }

    @Test
    fun `validateCreate reports error for invalid email`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "not-valid",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "customer_email" })
    }

    @Test
    fun `validateCreate reports error for description too short`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Short"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "description" })
    }

    @Test
    fun `validateCreate reports error for invalid category value`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            category      = "not_a_category"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "category" })
    }

    @Test
    fun `validateUpdate skips null fields and validates non-null ones`() {
        val req = UpdateTicketRequest(
            customerEmail = "bad-email",
            subject       = null  // null — should not be validated
        )
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "customer_email" })
        assertTrue(errors.none { it.field == "subject" })
    }

    @Test
    fun `toTicket applies server-side defaults for omitted optional fields`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass"
        )
        val ticket = validator.toTicket(req, id = "id-001", now = "2026-01-01T00:00:00Z")
        assertEquals(Category.OTHER,    ticket.category)
        assertEquals(Priority.MEDIUM,   ticket.priority)
        assertEquals(Status.NEW,        ticket.status)
        assertNotNull(ticket.metadata)
        assertEquals(Source.API,        ticket.metadata.source)
        assertNull(ticket.metadata.browser)
    }

    // --- Additional validateCreate branches ---

    @Test
    fun `validateCreate reports error for blank customer_name`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "",
            subject       = "Subject",
            description   = "Description that is long enough to pass"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "customer_name" })
    }

    @Test
    fun `validateCreate reports error for empty subject`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "",
            description   = "Description that is long enough to pass"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "subject" })
    }

    @Test
    fun `validateCreate reports error for subject exceeding 200 chars`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "A".repeat(201),
            description   = "Description that is long enough to pass"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "subject" })
    }

    @Test
    fun `validateCreate reports error for description exceeding 2000 chars`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "A".repeat(2001)
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "description" })
    }

    @Test
    fun `validateCreate reports error for invalid priority value`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            priority      = "not_a_priority"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "priority" })
    }

    @Test
    fun `validateCreate reports error for invalid status value`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            status        = "not_a_status"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "status" })
    }

    @Test
    fun `validateCreate with valid optional fields passes`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            category      = "billing_question",
            priority      = "high",
            status        = "in_progress"
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }

    @Test
    fun `validateCreate reports error for invalid metadata source`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            metadata      = CreateMetadataRequest(source = "fax_machine")
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "metadata.source" })
    }

    @Test
    fun `validateCreate reports error for invalid metadata device_type`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            metadata      = CreateMetadataRequest(source = "api", deviceType = "smartwatch")
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.any { it.field == "metadata.device_type" })
    }

    @Test
    fun `validateCreate with valid metadata passes`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            metadata      = CreateMetadataRequest(source = "web_form", browser = "Firefox", deviceType = "desktop")
        )
        val errors = validator.validateCreate(req)
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }

    // --- Additional validateUpdate branches ---

    @Test
    fun `validateUpdate reports error for blank customer_name`() {
        val req = UpdateTicketRequest(customerName = "")
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "customer_name" })
    }

    @Test
    fun `validateUpdate reports error for blank customer_id`() {
        val req = UpdateTicketRequest(customerId = "   ")
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "customer_id" })
    }

    @Test
    fun `validateUpdate reports error for invalid description`() {
        val req = UpdateTicketRequest(description = "Short")
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "description" })
    }

    @Test
    fun `validateUpdate reports error for invalid subject too long`() {
        val req = UpdateTicketRequest(subject = "A".repeat(201))
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "subject" })
    }

    @Test
    fun `validateUpdate reports error for invalid category`() {
        val req = UpdateTicketRequest(category = "nonsense")
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "category" })
    }

    @Test
    fun `validateUpdate reports error for invalid priority`() {
        val req = UpdateTicketRequest(priority = "super_high")
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "priority" })
    }

    @Test
    fun `validateUpdate reports error for invalid status`() {
        val req = UpdateTicketRequest(status = "limbo")
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "status" })
    }

    @Test
    fun `validateUpdate reports error for invalid metadata source`() {
        val req = UpdateTicketRequest(
            metadata = UpdateMetadataRequest(source = "pigeon")
        )
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "metadata.source" })
    }

    @Test
    fun `validateUpdate reports error for invalid metadata device_type`() {
        val req = UpdateTicketRequest(
            metadata = UpdateMetadataRequest(deviceType = "hoverboard")
        )
        val errors = validator.validateUpdate(req)
        assertTrue(errors.any { it.field == "metadata.device_type" })
    }

    @Test
    fun `validateUpdate returns empty list when all supplied fields are valid`() {
        val req = UpdateTicketRequest(
            category = "bug_report",
            priority = "urgent",
            status   = "resolved",
            metadata = UpdateMetadataRequest(source = "email", deviceType = "mobile")
        )
        val errors = validator.validateUpdate(req)
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }

    // --- toTicket with all optional fields ---

    @Test
    fun `toTicket maps all supplied optional fields correctly`() {
        val req = CreateTicketRequest(
            customerId    = "cust-1",
            customerEmail = "alice@example.com",
            customerName  = "Alice",
            subject       = "Subject",
            description   = "Description that is long enough to pass",
            category      = "bug_report",
            priority      = "urgent",
            status        = "in_progress",
            assignedTo    = "agent-99",
            tags          = listOf("urgent", "prod"),
            metadata      = CreateMetadataRequest(source = "chat", browser = "Safari", deviceType = "tablet")
        )
        val ticket = validator.toTicket(req, id = "id-002", now = "2026-06-01T00:00:00Z")

        assertEquals("id-002",              ticket.id)
        assertEquals(Category.BUG_REPORT,   ticket.category)
        assertEquals(Priority.URGENT,       ticket.priority)
        assertEquals(Status.IN_PROGRESS,    ticket.status)
        assertEquals("agent-99",            ticket.assignedTo)
        assertEquals(listOf("urgent", "prod"), ticket.tags)
        assertEquals(Source.CHAT,           ticket.metadata.source)
        assertEquals("Safari",              ticket.metadata.browser)
        assertEquals(DeviceType.TABLET,     ticket.metadata.deviceType)
    }
}

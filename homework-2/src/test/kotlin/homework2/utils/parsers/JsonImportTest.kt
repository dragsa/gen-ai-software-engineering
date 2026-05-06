package homework2.utils.parsers

import homework2.testsupport.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [JsonTicketParser].
 *
 * The parser is exercised directly using fixture files from [Fixtures].
 * No HTTP server is started.
 */
class JsonImportTest {

    @Test
    fun `parse JSON array returns Success row for each valid element`() {
        val content = Fixtures.text("json/valid_tickets.json")
        val rows = JsonTicketParser.parse(content)

        assertEquals(3, rows.size)
        rows.forEach { row -> assertIs<ParsedRow.Success>(row) }
    }

    @Test
    fun `parse JSON array maps fields correctly for first element`() {
        val content = Fixtures.text("json/valid_tickets.json")
        val first = JsonTicketParser.parse(content)[0] as ParsedRow.Success

        assertEquals("cust-001", first.request.customerId)
        assertEquals("alice@example.com", first.request.customerEmail)
        assertEquals("account_access", first.request.category)
        assertEquals("high", first.request.priority)
    }

    @Test
    fun `parse single JSON object returns one Success row`() {
        val content = Fixtures.text("json/single_ticket.json")
        val rows = JsonTicketParser.parse(content)

        assertEquals(1, rows.size)
        val row = rows[0]
        assertIs<ParsedRow.Success>(row)
        assertEquals("cust-001", row.request.customerId)
        assertEquals("feature_request", row.request.category)
    }

    @Test
    fun `parse JSON array with invalid element returns Failure for that element`() {
        val content = Fixtures.text("json/invalid_tickets.json")
        val rows = JsonTicketParser.parse(content)

        assertEquals(3, rows.size)
        assertIs<ParsedRow.Success>(rows[0])
        assertIs<ParsedRow.Failure>(rows[1])
        assertIs<ParsedRow.Success>(rows[2])

        val failure = rows[1] as ParsedRow.Failure
        assertEquals(2, failure.row)
    }

    @Test
    fun `parse completely invalid JSON returns single Failure`() {
        val content = "not valid json at all {{{"
        val rows = JsonTicketParser.parse(content)

        assertEquals(1, rows.size)
        assertIs<ParsedRow.Failure>(rows[0])
        assertTrue((rows[0] as ParsedRow.Failure).error.contains("Invalid JSON", ignoreCase = true))
    }

    @Test
    fun `parse empty JSON array returns empty list`() {
        val rows = JsonTicketParser.parse("[]")
        assertTrue(rows.isEmpty())
    }
}

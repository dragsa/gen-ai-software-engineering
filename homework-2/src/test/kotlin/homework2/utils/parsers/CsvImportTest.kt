package homework2.utils.parsers

import homework2.testsupport.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [CsvTicketParser].
 *
 * Tests use fixture files from [Fixtures] so content is maintained in one place.
 * No HTTP server is started — the parser is exercised directly.
 */
class CsvImportTest {

    @Test
    fun `parse valid CSV returns Success row for each data row`() {
        val content = Fixtures.text("csv/valid_tickets.csv")
        val rows = CsvTicketParser.parse(content)

        assertEquals(3, rows.size)
        rows.forEach { row -> assertIs<ParsedRow.Success>(row) }
    }

    @Test
    fun `parse valid CSV first row maps customer_id correctly`() {
        val content = Fixtures.text("csv/valid_tickets.csv")
        val first = CsvTicketParser.parse(content)[0]

        assertIs<ParsedRow.Success>(first)
        assertEquals("cust-001", first.request.customerId)
        assertEquals("alice@example.com", first.request.customerEmail)
    }

    @Test
    fun `parse valid CSV preserves optional fields`() {
        val content = Fixtures.text("csv/valid_tickets.csv")
        val rows = CsvTicketParser.parse(content)

        val first = rows[0] as ParsedRow.Success
        assertEquals("account_access", first.request.category)
        assertEquals("high", first.request.priority)
        assertEquals("agent-1", first.request.assignedTo)
    }

    @Test
    fun `parse header-only CSV returns empty list`() {
        val content = Fixtures.text("csv/header_only.csv")
        val rows = CsvTicketParser.parse(content)

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `parse CSV missing required column returns single Failure`() {
        val content = Fixtures.text("csv/missing_column.csv")
        val rows = CsvTicketParser.parse(content)

        assertEquals(1, rows.size)
        assertIs<ParsedRow.Failure>(rows[0])
        val failure = rows[0] as ParsedRow.Failure
        assertTrue(failure.error.contains("missing required columns", ignoreCase = true))
    }

    @Test
    fun `parse CSV row numbers are 1-based`() {
        val content = Fixtures.text("csv/valid_tickets.csv")
        val rows = CsvTicketParser.parse(content)

        assertEquals(1, (rows[0] as ParsedRow.Success).row)
        assertEquals(2, (rows[1] as ParsedRow.Success).row)
        assertEquals(3, (rows[2] as ParsedRow.Success).row)
    }
}

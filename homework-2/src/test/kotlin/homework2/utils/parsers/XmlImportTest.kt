package homework2.utils.parsers

import homework2.testsupport.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [XmlTicketParser].
 *
 * The parser is exercised directly using fixture files from [Fixtures].
 * No HTTP server is started.
 */
class XmlImportTest {

    @Test
    fun `parse tickets root element returns Success for each ticket`() {
        val content = Fixtures.text("xml/valid_tickets.xml")
        val rows = XmlTicketParser.parse(content)

        assertEquals(3, rows.size)
        rows.forEach { row -> assertIs<ParsedRow.Success>(row) }
    }

    @Test
    fun `parse tickets root maps first ticket fields correctly`() {
        val content = Fixtures.text("xml/valid_tickets.xml")
        val first = XmlTicketParser.parse(content)[0] as ParsedRow.Success

        assertEquals("cust-001", first.request.customerId)
        assertEquals("alice@example.com", first.request.customerEmail)
        assertEquals("account_access", first.request.category)
        assertEquals("high", first.request.priority)
    }

    @Test
    fun `parse tickets root extracts nested tags`() {
        val content = Fixtures.text("xml/valid_tickets.xml")
        val first = XmlTicketParser.parse(content)[0] as ParsedRow.Success

        assertEquals(listOf("auth", "login"), first.request.tags)
    }

    @Test
    fun `parse single ticket root element returns one Success row`() {
        val content = Fixtures.text("xml/single_ticket.xml")
        val rows = XmlTicketParser.parse(content)

        assertEquals(1, rows.size)
        val row = rows[0]
        assertIs<ParsedRow.Success>(row)
        assertEquals("feature_request", row.request.category)
        assertEquals("Alice Smith", row.request.customerName)
    }

    @Test
    fun `parse XML with missing required field returns Failure for that ticket`() {
        val content = Fixtures.text("xml/missing_field.xml")
        val rows = XmlTicketParser.parse(content)

        assertEquals(2, rows.size)
        assertIs<ParsedRow.Success>(rows[0])
        assertIs<ParsedRow.Failure>(rows[1])

        val failure = rows[1] as ParsedRow.Failure
        assertTrue(failure.error.contains("description", ignoreCase = true))
    }

    @Test
    fun `parse XML with wrong root element returns single Failure`() {
        val content = """<?xml version="1.0"?><root><item/></root>"""
        val rows = XmlTicketParser.parse(content)

        assertEquals(1, rows.size)
        assertIs<ParsedRow.Failure>(rows[0])
        assertTrue((rows[0] as ParsedRow.Failure).error.contains("Expected root element", ignoreCase = true))
    }
}

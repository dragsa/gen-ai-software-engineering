package homework2.performance

import homework2.models.CreateTicketRequest
import homework2.service.InMemoryTicketRepository
import homework2.service.TicketClassifier
import homework2.service.TicketServiceImpl
import homework2.utils.parsers.CsvTicketParser
import homework2.utils.parsers.JsonTicketParser
import homework2.validation.TicketValidator
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance benchmarks for the service layer.
 *
 * Tests exercise the service and parsers directly (no HTTP overhead) so that
 * timing measurements reflect algorithmic cost rather than network stack latency.
 * All budgets are deliberately generous to stay green on CI without JVM pre-warming.
 *
 * Tolerances:
 *   - 200-row import (parse + validate + persist): < 3 000 ms
 *   - 100 classify calls p95 latency:              < 50 ms
 *   - GET /tickets list with 1 000 tickets:        < 500 ms
 *   - Combined-filter with 1 000 tickets:          < 200 ms
 */
class PerformanceTest {

    private fun makeService(): TicketServiceImpl {
        val repo       = InMemoryTicketRepository()
        val validator  = TicketValidator()
        val classifier = TicketClassifier()
        return TicketServiceImpl(repo, validator, classifier)
    }

    private fun validRequest(i: Int, category: String = "billing_question", priority: String = "high") =
        CreateTicketRequest(
            customerId    = "cust-$i",
            customerEmail = "user$i@example.com",
            customerName  = "User $i",
            subject       = "Performance test ticket $i",
            description   = "This is a generated ticket for performance benchmarking purposes index $i",
            category      = category,
            priority      = priority
        )

    /** Builds an N-row CSV string with all required columns. */
    private fun buildCsv(rows: Int): String {
        val sb = StringBuilder()
        sb.appendLine("customer_id,customer_email,customer_name,subject,description,category,priority")
        repeat(rows) { i ->
            sb.appendLine(
                "cust-$i,user$i@example.com,User $i," +
                "Perf ticket $i," +
                "Description for performance benchmark ticket number $i in the test suite," +
                "billing_question,high"
            )
        }
        return sb.toString()
    }

    /** Builds an N-element JSON array string. */
    private fun buildJson(rows: Int): String {
        val elements = (0 until rows).joinToString(",\n") { i ->
            """{"customer_id":"cust-$i","customer_email":"user$i@example.com","customer_name":"User $i","subject":"Perf ticket $i","description":"Description for performance benchmark ticket number $i in the test suite","category":"billing_question","priority":"high"}"""
        }
        return "[$elements]"
    }

    // -------------------------------------------------------------------------
    // 1. CSV import throughput (200 rows)
    // -------------------------------------------------------------------------

    @Test
    fun `import 200-row CSV completes in under 3 seconds`() {
        val svc     = makeService()
        val content = buildCsv(200)

        val start = System.currentTimeMillis()
        val rows  = CsvTicketParser.parse(content)
        val summary = svc.bulkImport(rows)
        val elapsed = System.currentTimeMillis() - start

        assertTrue(summary.successful == 200, "Expected 200 successful imports, got ${summary.successful}")
        assertTrue(elapsed < 3_000, "CSV import took ${elapsed}ms — expected < 3 000ms")
    }

    // -------------------------------------------------------------------------
    // 2. JSON import throughput (200 elements)
    // -------------------------------------------------------------------------

    @Test
    fun `import 200-element JSON array completes in under 3 seconds`() {
        val svc     = makeService()
        val content = buildJson(200)

        val start   = System.currentTimeMillis()
        val rows    = JsonTicketParser.parse(content)
        val summary = svc.bulkImport(rows)
        val elapsed = System.currentTimeMillis() - start

        assertTrue(summary.successful == 200, "Expected 200 successful imports, got ${summary.successful}")
        assertTrue(elapsed < 3_000, "JSON import took ${elapsed}ms — expected < 3 000ms")
    }

    // -------------------------------------------------------------------------
    // 3. Classifier latency p95 (100 classifications)
    // -------------------------------------------------------------------------

    @Test
    fun `classifier p95 latency is under 50ms over 100 calls`() {
        val svc = makeService()

        // Seed one ticket to classify repeatedly
        val ticket = svc.createTicket(
            validRequest(0).copy(
                subject     = "Cannot login to my account after password reset",
                description = "I have been unable to login since yesterday morning after a forced password reset"
            )
        )

        val latencies = LongArray(100)
        repeat(100) { i ->
            val t0 = System.currentTimeMillis()
            svc.classifyTicket(ticket.id)
            latencies[i] = System.currentTimeMillis() - t0
        }

        latencies.sort()
        val p95 = latencies[94]   // 95th percentile (0-indexed: index 94 of 100)
        assertTrue(p95 < 50, "Classifier p95 latency ${p95}ms — expected < 50ms")
    }

    // -------------------------------------------------------------------------
    // 4. List 1 000 tickets without a filter
    // -------------------------------------------------------------------------

    @Test
    fun `listing 1000 tickets completes in under 500ms`() {
        val svc = makeService()

        // Seed with varied categories so the list is realistic
        val categories = listOf("billing_question", "technical_issue", "account_access", "other", "feature_request")
        repeat(1_000) { i ->
            svc.createTicket(validRequest(i, category = categories[i % categories.size]))
        }

        val start   = System.currentTimeMillis()
        val results = svc.listTickets(homework2.models.TicketFilter())
        val elapsed = System.currentTimeMillis() - start

        assertTrue(results.size == 1_000, "Expected 1 000 tickets, got ${results.size}")
        assertTrue(elapsed < 500, "List took ${elapsed}ms — expected < 500ms")
    }

    // -------------------------------------------------------------------------
    // 5. Combined-filter latency on 1 000 tickets
    // -------------------------------------------------------------------------

    @Test
    fun `combined category and priority filter on 1000 tickets completes in under 200ms`() {
        val svc = makeService()

        // Seed 1 000 tickets; every 5th is billing_question/high
        repeat(1_000) { i ->
            val cat = if (i % 5 == 0) "billing_question" else "technical_issue"
            val pri = if (i % 5 == 0) "high"             else "medium"
            svc.createTicket(validRequest(i, category = cat, priority = pri))
        }

        val filter = homework2.models.TicketFilter(
            category = homework2.models.Category.BILLING_QUESTION,
            priority = homework2.models.Priority.HIGH
        )

        val start   = System.currentTimeMillis()
        val results = svc.listTickets(filter)
        val elapsed = System.currentTimeMillis() - start

        assertTrue(results.size == 200, "Expected 200 matched tickets, got ${results.size}")
        assertTrue(elapsed < 200, "Combined filter took ${elapsed}ms — expected < 200ms")
    }
}

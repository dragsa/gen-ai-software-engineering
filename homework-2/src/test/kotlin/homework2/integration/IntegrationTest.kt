package homework2.integration

import homework2.testsupport.Fixtures
import homework2.testsupport.testApp
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * End-to-end integration tests exercising multi-step workflows across the full
 * HTTP stack.  Each test boots a fresh in-memory application via [testApp] and
 * drives the API entirely through HTTP calls — no direct service or repository
 * access.
 */
class IntegrationTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val boundary = "IntBoundary-ABCDE"

    // --- multipart helper (same raw-byte approach as ImportApiTest) ---

    private fun filePart(bytes: ByteArray, filename: String, partContentType: String): OutgoingContent {
        val header = (
            "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n" +
            "Content-Type: $partContentType\r\n" +
            "\r\n"
        ).toByteArray(Charsets.UTF_8)
        val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val body = header + bytes + footer
        val ct = ContentType.parse("multipart/form-data; boundary=$boundary")
        return object : OutgoingContent.ByteArrayContent() {
            override val contentType = ct
            override fun bytes() = body
        }
    }

    // -------------------------------------------------------------------------
    // 1. Full ticket lifecycle
    // -------------------------------------------------------------------------

    @Test
    fun `full lifecycle create update delete returns correct states at each step`() = testApplication {
        testApp()(this)

        // Create
        val createResponse = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(subject = "Integration lifecycle test"))
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val id = json.parseToJsonElement(createResponse.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.content

        // Read back
        val getResponse = client.get("/tickets/$id")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertEquals(
            "Integration lifecycle test",
            json.parseToJsonElement(getResponse.bodyAsText()).jsonObject["subject"]!!.jsonPrimitive.content
        )

        // Update status to in_progress
        val putResponse = client.put("/tickets/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"status": "in_progress", "assigned_to": "agent-integration"}""")
        }
        assertEquals(HttpStatusCode.OK, putResponse.status)
        val updated = json.parseToJsonElement(putResponse.bodyAsText()).jsonObject
        assertEquals("in_progress", updated["status"]!!.jsonPrimitive.content)
        assertEquals("agent-integration", updated["assigned_to"]!!.jsonPrimitive.content)

        // Update status to resolved — resolvedAt must be set
        val resolveResponse = client.put("/tickets/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"status": "resolved"}""")
        }
        assertEquals(HttpStatusCode.OK, resolveResponse.status)
        val resolved = json.parseToJsonElement(resolveResponse.bodyAsText()).jsonObject
        assertNotNull(resolved["resolved_at"])

        // Delete
        val deleteResponse = client.delete("/tickets/$id")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Confirm gone
        val afterDelete = client.get("/tickets/$id")
        assertEquals(HttpStatusCode.NotFound, afterDelete.status)
    }

    // -------------------------------------------------------------------------
    // 2. Bulk import → auto-classify → filter by resulting category
    // -------------------------------------------------------------------------

    @Test
    fun `bulk CSV import then auto-classify then filter returns classified ticket`() = testApplication {
        testApp()(this)

        // Import 3 tickets from CSV fixture
        val importResponse = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("csv/valid_tickets.csv"), "tickets.csv", "text/csv"))
        }
        assertEquals(HttpStatusCode.OK, importResponse.status)
        val summary = json.parseToJsonElement(importResponse.bodyAsText()).jsonObject
        assertEquals("3", summary["successful"]!!.jsonPrimitive.content)

        // Pick the first ticket from the list
        val listResponse = client.get("/tickets")
        val tickets = json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertEquals(3, tickets.size)
        val firstId = tickets[0].jsonObject["id"]!!.jsonPrimitive.content

        // Auto-classify it
        val classifyResponse = client.post("/tickets/$firstId/auto-classify")
        assertEquals(HttpStatusCode.OK, classifyResponse.status)
        val decision = json.parseToJsonElement(classifyResponse.bodyAsText()).jsonObject
        val category = decision["category"]!!.jsonPrimitive.content

        // Filter by the category that was just assigned — ticket must appear
        val filteredResponse = client.get("/tickets?category=$category")
        assertEquals(HttpStatusCode.OK, filteredResponse.status)
        val filtered = json.parseToJsonElement(filteredResponse.bodyAsText()).jsonArray
        val ids = filtered.map { it.jsonObject["id"]!!.jsonPrimitive.content }
        assert(firstId in ids) { "Expected ticket $firstId in filtered list for category=$category" }
    }

    // -------------------------------------------------------------------------
    // 3. Combined category + priority filter
    // -------------------------------------------------------------------------

    @Test
    fun `combined category and priority filter returns only matching tickets`() = testApplication {
        testApp()(this)

        // billing_question / high
        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(
                customerId  = "cust-A",
                email       = "a@example.com",
                category    = "billing_question",
                priority    = "high"
            ))
        }
        // billing_question / low
        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(
                customerId  = "cust-B",
                email       = "b@example.com",
                category    = "billing_question",
                priority    = "low"
            ))
        }
        // technical_issue / high
        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(
                customerId  = "cust-C",
                email       = "c@example.com",
                category    = "technical_issue",
                priority    = "high"
            ))
        }

        val response = client.get("/tickets?category=billing_question&priority=high")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        assertEquals("billing_question", body[0].jsonObject["category"]!!.jsonPrimitive.content)
        assertEquals("high", body[0].jsonObject["priority"]!!.jsonPrimitive.content)
    }

    // -------------------------------------------------------------------------
    // 4. Malformed import pathway — partial success reported in body
    // -------------------------------------------------------------------------

    @Test
    fun `import with one invalid element returns 200 with partial failure summary`() = testApplication {
        testApp()(this)

        val importResponse = client.post("/tickets/import") {
            setBody(filePart(
                Fixtures.bytes("json/invalid_tickets.json"),
                "mixed.json",
                "application/json"
            ))
        }
        assertEquals(HttpStatusCode.OK, importResponse.status)

        val body = json.parseToJsonElement(importResponse.bodyAsText()).jsonObject
        assertEquals("3", body["total"]!!.jsonPrimitive.content)
        assertEquals("2", body["successful"]!!.jsonPrimitive.content)
        assertEquals("1", body["failed"]!!.jsonPrimitive.content)

        val failures = body["failures"]!!.jsonArray
        assertEquals(1, failures.size)
        assertNotNull(failures[0].jsonObject["reason"])

        // Only 2 tickets should exist in the store
        val listResponse = client.get("/tickets")
        val listed = json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertEquals(2, listed.size)
    }

    // -------------------------------------------------------------------------
    // 5. Concurrent writes — no lost tickets
    // -------------------------------------------------------------------------

    @Test
    fun `20 concurrent POST tickets all succeed with no lost writes`() = testApplication {
        testApp()(this)

        val n = 20
        coroutineScope {
            (1..n).map { i ->
                async {
                    client.post("/tickets") {
                        contentType(ContentType.Application.Json)
                        setBody(Fixtures.validCreateRequest(
                            customerId = "cust-concurrent-$i",
                            email      = "user$i@example.com"
                        ))
                    }
                }
            }.awaitAll()
        }

        val listResponse = client.get("/tickets")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val tickets = json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertEquals(n, tickets.size, "Expected $n tickets but found ${tickets.size} — write was lost")
    }
}

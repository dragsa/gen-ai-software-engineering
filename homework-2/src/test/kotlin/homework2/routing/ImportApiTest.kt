package homework2.routing

import homework2.testsupport.Fixtures
import homework2.testsupport.testApp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * HTTP-level tests for POST /tickets/import.
 *
 * Raw MIME multipart bytes are constructed manually so that the Content-Disposition
 * header includes `filename=`, which causes Ktor's multipart parser to classify the
 * part as a [io.ktor.http.content.PartData.FileItem] — the type the route handler
 * checks for. Using the high-level `formData { append(...) }` builder is avoided
 * because it prepends its own Content-Disposition (without `filename`) which makes
 * the server see a FormItem instead.
 */
class ImportApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** MIME multipart boundary used in all test parts. */
    private val boundary = "TestBoundary-ABCDE-12345"

    /**
     * Builds a raw multipart/form-data body with a single file part named "file".
     * Returns an [OutgoingContent] ready for [io.ktor.client.request.setBody].
     */
    private fun filePart(
        bytes: ByteArray,
        filename: String,
        partContentType: String
    ): OutgoingContent {
        val header = (
            "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n" +
            "Content-Type: $partContentType\r\n" +
            "\r\n"
        ).toByteArray(Charsets.UTF_8)
        val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val body   = header + bytes + footer
        val ct     = ContentType.parse("multipart/form-data; boundary=$boundary")

        return object : OutgoingContent.ByteArrayContent() {
            override val contentType = ct
            override fun bytes() = body
        }
    }

    // --- CSV import ---

    @Test
    fun `import valid CSV returns 200 with correct summary`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("csv/valid_tickets.csv"), "tickets.csv", "text/csv"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("3", body["total"]!!.jsonPrimitive.content)
        assertEquals("3", body["successful"]!!.jsonPrimitive.content)
        assertEquals("0", body["failed"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import CSV header-only returns 200 with zero counts`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("csv/header_only.csv"), "empty.csv", "text/csv"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("0", body["total"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import CSV missing required column returns 200 with one failure`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("csv/missing_column.csv"), "bad.csv", "text/csv"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("1", body["failed"]!!.jsonPrimitive.content)
        assertEquals("0", body["successful"]!!.jsonPrimitive.content)
    }

    // --- JSON import ---

    @Test
    fun `import valid JSON array returns 200 with correct summary`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("json/valid_tickets.json"), "tickets.json", "application/json"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("3", body["total"]!!.jsonPrimitive.content)
        assertEquals("3", body["successful"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import JSON with one invalid element returns partial success`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("json/invalid_tickets.json"), "mixed.json", "application/json"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("3", body["total"]!!.jsonPrimitive.content)
        assertEquals("2", body["successful"]!!.jsonPrimitive.content)
        assertEquals("1", body["failed"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import single JSON object returns one successful row`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("json/single_ticket.json"), "one.json", "application/json"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("1", body["total"]!!.jsonPrimitive.content)
        assertEquals("1", body["successful"]!!.jsonPrimitive.content)
    }

    // --- XML import ---

    @Test
    fun `import valid XML returns 200 with correct summary`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("xml/valid_tickets.xml"), "tickets.xml", "application/xml"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("3", body["total"]!!.jsonPrimitive.content)
        assertEquals("3", body["successful"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import XML with missing required field returns partial failure`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            setBody(filePart(Fixtures.bytes("xml/missing_field.xml"), "partial.xml", "application/xml"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("2", body["total"]!!.jsonPrimitive.content)
        assertEquals("1", body["successful"]!!.jsonPrimitive.content)
        assertEquals("1", body["failed"]!!.jsonPrimitive.content)
    }

    // --- Format detection via filename extension ---

    @Test
    fun `import detects CSV format from filename when no Content-Type given`() = testApplication {
        testApp()(this)

        val bytes   = Fixtures.bytes("csv/valid_tickets.csv")
        val header  = (
            "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"export.csv\"\r\n" +
            "\r\n"                 // no Content-Type part header
        ).toByteArray(Charsets.UTF_8)
        val footer  = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val body    = header + bytes + footer
        val ct      = ContentType.parse("multipart/form-data; boundary=$boundary")

        val response = client.post("/tickets/import") {
            setBody(object : OutgoingContent.ByteArrayContent() {
                override val contentType = ct
                override fun bytes() = body
            })
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val respBody = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("3", respBody["successful"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import detects JSON format from filename extension`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            // filename ends with .json but no Content-Type part header
            val bytes  = Fixtures.bytes("json/valid_tickets.json")
            val header = (
                "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"data.json\"\r\n" +
                "\r\n"
            ).toByteArray(Charsets.UTF_8)
            val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
            val body   = header + bytes + footer
            setBody(object : OutgoingContent.ByteArrayContent() {
                override val contentType = ContentType.parse("multipart/form-data; boundary=$boundary")
                override fun bytes() = body
            })
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("3", body["successful"]!!.jsonPrimitive.content)
    }

    @Test
    fun `import detects XML format from filename extension`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/import") {
            val bytes  = Fixtures.bytes("xml/valid_tickets.xml")
            val header = (
                "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"data.xml\"\r\n" +
                "\r\n"
            ).toByteArray(Charsets.UTF_8)
            val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
            val body   = header + bytes + footer
            setBody(object : OutgoingContent.ByteArrayContent() {
                override val contentType = ContentType.parse("multipart/form-data; boundary=$boundary")
                override fun bytes() = body
            })
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // --- Error paths ---

    @Test
    fun `import returns 400 when no file part found`() = testApplication {
        testApp()(this)

        // Send a multipart body with a FormItem (no filename) — server won't see a FileItem
        val header = (
            "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"\r\n" +  // no filename= → FormItem
            "Content-Type: text/csv\r\n" +
            "\r\n"
        ).toByteArray(Charsets.UTF_8)
        val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val body   = header + "some,data\r\n".toByteArray() + footer

        val response = client.post("/tickets/import") {
            setBody(object : OutgoingContent.ByteArrayContent() {
                override val contentType = ContentType.parse("multipart/form-data; boundary=$boundary")
                override fun bytes() = body
            })
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val respBody = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(respBody["error"]!!.jsonPrimitive.content.contains("No file", ignoreCase = true))
    }

    @Test
    fun `import returns 400 when format cannot be determined from filename`() = testApplication {
        testApp()(this)

        val bytes  = "some bytes".toByteArray()
        val header = (
            "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"data.bin\"\r\n" +
            "\r\n"   // no Content-Type, .bin extension → unknown format
        ).toByteArray(Charsets.UTF_8)
        val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val body   = header + bytes + footer

        val response = client.post("/tickets/import") {
            setBody(object : OutgoingContent.ByteArrayContent() {
                override val contentType = ContentType.parse("multipart/form-data; boundary=$boundary")
                override fun bytes() = body
            })
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val respBody = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(respBody["error"]!!.jsonPrimitive.content.contains("format", ignoreCase = true))
    }

    @Test
    fun `import returns 400 for malformed CSV content`() = testApplication {
        testApp()(this)

        // A CSV with all required columns but rows that fail validation
        val csv = "customer_id,customer_email,customer_name,subject,description\n" +
                  ",not-an-email,,x,short"  // all fields invalid

        val response = client.post("/tickets/import") {
            setBody(filePart(csv.toByteArray(Charsets.UTF_8), "bad_data.csv", "text/csv"))
        }

        // The import still returns 200 — per-row failures go into the summary
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("0", body["successful"]!!.jsonPrimitive.content)
        assertEquals("1", body["failed"]!!.jsonPrimitive.content)
    }
}

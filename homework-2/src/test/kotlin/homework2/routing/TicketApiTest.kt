package homework2.routing

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
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * HTTP-level integration tests for all ticket endpoints.
 *
 * Each test boots a fresh in-memory application via [testApp] so tests are
 * fully isolated — no shared state between runs.
 */
class TicketApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- POST /tickets ---

    @Test
    fun `POST tickets creates ticket and returns 201`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["id"])
        assertEquals("cust-001", body["customer_id"]!!.jsonPrimitive.content)
        assertEquals("new", body["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST tickets validates blank customer_id and returns 400`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(customerId = "  "))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Validation failed", body["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST tickets validates invalid email and returns 400`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(email = "not-an-email"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val details = json.parseToJsonElement(response.bodyAsText())
            .jsonObject["details"]!!.jsonArray
        assertTrue(details.any { it.jsonObject["field"]!!.jsonPrimitive.content == "customer_email" })
    }

    @Test
    fun `POST tickets validates description too short and returns 400`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(description = "short"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val details = json.parseToJsonElement(response.bodyAsText())
            .jsonObject["details"]!!.jsonArray
        assertTrue(details.any { it.jsonObject["field"]!!.jsonPrimitive.content == "description" })
    }

    // --- GET /tickets ---

    @Test
    fun `GET tickets returns empty array when no tickets exist`() = testApplication {
        testApp()(this)

        val response = client.get("/tickets")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(body.isEmpty())
    }

    @Test
    fun `GET tickets returns created ticket in list`() = testApplication {
        testApp()(this)

        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }

        val response = client.get("/tickets")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        assertEquals("cust-001", body[0].jsonObject["customer_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET tickets filters by status`() = testApplication {
        testApp()(this)

        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }

        val response = client.get("/tickets?status=in_progress")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(body.isEmpty())
    }

    // --- GET /tickets/{id} ---

    @Test
    fun `GET tickets by id returns 404 for unknown ticket`() = testApplication {
        testApp()(this)

        val response = client.get("/tickets/nonexistent-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET tickets by id returns ticket after creation`() = testApplication {
        testApp()(this)

        val createBody = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(name = "Bob Test"))
        }.bodyAsText()

        val id = json.parseToJsonElement(createBody).jsonObject["id"]!!.jsonPrimitive.content

        val response = client.get("/tickets/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(id, body["id"]!!.jsonPrimitive.content)
        assertEquals("Bob Test", body["customer_name"]!!.jsonPrimitive.content)
    }

    // --- PUT /tickets/{id} ---

    @Test
    fun `PUT tickets updates subject and returns 200`() = testApplication {
        testApp()(this)

        val createBody = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }.bodyAsText()
        val id = json.parseToJsonElement(createBody).jsonObject["id"]!!.jsonPrimitive.content

        val updatePayload = """{"subject": "Updated subject here"}"""
        val response = client.put("/tickets/$id") {
            contentType(ContentType.Application.Json)
            setBody(updatePayload)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Updated subject here", body["subject"]!!.jsonPrimitive.content)
    }

    @Test
    fun `PUT tickets returns 404 for unknown ticket`() = testApplication {
        testApp()(this)

        val response = client.put("/tickets/ghost-id") {
            contentType(ContentType.Application.Json)
            setBody("""{"subject": "Irrelevant"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- DELETE /tickets/{id} ---

    @Test
    fun `DELETE tickets returns 204 then GET returns 404`() = testApplication {
        testApp()(this)

        val createBody = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }.bodyAsText()
        val id = json.parseToJsonElement(createBody).jsonObject["id"]!!.jsonPrimitive.content

        val deleteResponse = client.delete("/tickets/$id")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val getResponse = client.get("/tickets/$id")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    // --- POST /tickets/{id}/auto-classify ---

    @Test
    fun `POST auto-classify returns classification response`() = testApplication {
        testApp()(this)

        val createBody = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(
                Fixtures.validCreateRequest(
                    subject     = "Cannot login to my account",
                    description = "I have been unable to login since yesterday morning after a password reset"
                )
            )
        }.bodyAsText()
        val id = json.parseToJsonElement(createBody).jsonObject["id"]!!.jsonPrimitive.content

        val response = client.post("/tickets/$id/auto-classify")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(id, body["ticket_id"]!!.jsonPrimitive.content)
        assertNotNull(body["category"])
        assertNotNull(body["confidence"])
        assertNotNull(body["keywords_found"])
    }

    @Test
    fun `POST auto-classify returns 404 for unknown ticket`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets/no-such-ticket/auto-classify")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- GET /tickets filter validation ---

    @Test
    fun `GET tickets returns 400 for invalid category query param`() = testApplication {
        testApp()(this)

        val response = client.get("/tickets?category=not_a_category")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val details = body["details"]!!.jsonArray
        assertTrue(details.any { it.jsonObject["field"]!!.jsonPrimitive.content == "category" })
    }

    @Test
    fun `GET tickets returns 400 for invalid priority query param`() = testApplication {
        testApp()(this)

        val response = client.get("/tickets?priority=super_high")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val details = body["details"]!!.jsonArray
        assertTrue(details.any { it.jsonObject["field"]!!.jsonPrimitive.content == "priority" })
    }

    @Test
    fun `GET tickets returns 400 for invalid status query param`() = testApplication {
        testApp()(this)

        val response = client.get("/tickets?status=limbo")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET tickets filters by category query param`() = testApplication {
        testApp()(this)

        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(category = "billing_question"))
        }
        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(customerId = "cust-002", email = "bob@example.com"))
        }

        val response = client.get("/tickets?category=billing_question")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        assertEquals("billing_question", body[0].jsonObject["category"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET tickets with search query filters by subject content`() = testApplication {
        testApp()(this)

        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest(subject = "Login failure for user"))
        }
        client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(
                Fixtures.validCreateRequest(
                    customerId  = "cust-002",
                    email       = "bob@example.com",
                    subject     = "Payment declined",
                    description = "My payment was charged but the invoice still shows unpaid"
                )
            )
        }

        val response = client.get("/tickets?search=login")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
    }

    // --- PUT /tickets/{id} branch coverage ---

    @Test
    fun `PUT tickets returns 400 for invalid JSON body`() = testApplication {
        testApp()(this)

        val createBody = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }.bodyAsText()
        val id = json.parseToJsonElement(createBody).jsonObject["id"]!!.jsonPrimitive.content

        val response = client.put("/tickets/$id") {
            contentType(ContentType.Application.Json)
            setBody("not json at all")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT tickets validates supplied fields and returns 400 on violation`() = testApplication {
        testApp()(this)

        val createBody = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody(Fixtures.validCreateRequest())
        }.bodyAsText()
        val id = json.parseToJsonElement(createBody).jsonObject["id"]!!.jsonPrimitive.content

        val response = client.put("/tickets/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"customer_email": "not-an-email"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val details = json.parseToJsonElement(response.bodyAsText())
            .jsonObject["details"]!!.jsonArray
        assertTrue(details.any { it.jsonObject["field"]!!.jsonPrimitive.content == "customer_email" })
    }

    // --- POST /tickets malformed body ---

    @Test
    fun `POST tickets returns 400 for completely malformed body`() = testApplication {
        testApp()(this)

        val response = client.post("/tickets") {
            contentType(ContentType.Application.Json)
            setBody("this is not json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Validation failed", body["error"]!!.jsonPrimitive.content)
    }

    // --- DELETE /tickets/{id} 404 ---

    @Test
    fun `DELETE tickets returns 404 for unknown id`() = testApplication {
        testApp()(this)

        val response = client.delete("/tickets/ghost-ticket-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}

package homework1.routing

import homework1.entrypoint.module
import homework1.testsupport.TestFixtures
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TransactionsApiIntegrationTest {
    private val json = Json

    @Test
    fun `create transaction returns validator errors when JSON is valid but payload is semantically invalid`() =
        testApplication {
            application { module() }

            val response = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody(TestFixtures.payload("invalidTransaction"))
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val errorFields = extractErrorFields(response.bodyAsText())
            assertTrue("amount" in errorFields)
            assertTrue("currency" in errorFields)
            assertTrue("fromAccount" in errorFields)
        }

    @Test
    fun `create transaction enforces mandatory payload shape and ignores client status`() = testApplication {
        application { module() }

        val missingRequired = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"fromAccount":"ACC-A1B2C","toAccount":"ACC-D3E4F"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, missingRequired.status)
        val missingRequiredFields = extractErrorFields(missingRequired.bodyAsText())
        assertEquals(setOf("body"), missingRequiredFields)

        val clientStatus = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(TestFixtures.payload("createWithClientStatus"))
        }

        assertEquals(HttpStatusCode.Created, clientStatus.status)
        assertEquals(
            "COMPLETED",
            parseObject(clientStatus.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content
        )
    }

    @Test
    fun `api returns failed status for insufficient funds and excludes it from balance`() = testApplication {
        application { module() }

        val deposit = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(TestFixtures.payload("createDeposit"))
        }
        assertEquals(HttpStatusCode.Created, deposit.status)
        assertEquals("COMPLETED", parseObject(deposit.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content)

        val transfer = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(TestFixtures.payload("createTransfer"))
        }
        assertEquals(HttpStatusCode.Created, transfer.status)
        assertEquals("COMPLETED", parseObject(transfer.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content)

        val withdrawalFailed = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(TestFixtures.payload("createInsufficientWithdrawal"))
        }
        assertEquals(HttpStatusCode.Created, withdrawalFailed.status)
        val failedBody = parseObject(withdrawalFailed.bodyAsText()).jsonObject
        assertEquals("FAILED", failedBody.getValue("status").jsonPrimitive.content)
        val failedId = failedBody.getValue("id").jsonPrimitive.content

        val listResponse = client.get("/transactions")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val listed = parseObject(listResponse.bodyAsText()).jsonArray
        assertTrue(
            listed.any {
                val obj = it.jsonObject
                obj.getValue("id").jsonPrimitive.content == failedId &&
                    obj.getValue("status").jsonPrimitive.content == "FAILED"
            }
        )

        val balanceResponse = client.get("/accounts/ACC-A1B2C/balance")
        assertEquals(HttpStatusCode.OK, balanceResponse.status)
        val balancesObject = parseObject(balanceResponse.bodyAsText())
            .jsonObject.getValue("balances")
            .jsonObject
        assertEquals("49.75", balancesObject.getValue("USD").jsonPrimitive.content)
    }

    @Test
    fun `transactions filters return validation errors for invalid type dates and date range`() = testApplication {
        application { module() }

        val invalidType = client.get("/transactions?type=wrong")
        assertEquals(HttpStatusCode.BadRequest, invalidType.status)
        assertTrue("type" in extractErrorFields(invalidType.bodyAsText()))

        val invalidDates = client.get("/transactions?from=not-a-date&to=still-not-a-date")
        assertEquals(HttpStatusCode.BadRequest, invalidDates.status)
        val invalidDateFields = extractErrorFields(invalidDates.bodyAsText())
        assertTrue("from" in invalidDateFields)
        assertTrue("to" in invalidDateFields)

        val invalidRange = client.get("/transactions?from=2026-02-10&to=2026-01-10")
        assertEquals(HttpStatusCode.BadRequest, invalidRange.status)
        assertTrue("from" in extractErrorFields(invalidRange.bodyAsText()))
    }

    @Test
    fun `get transaction by id returns created item and not found for unknown id`() = testApplication {
        application { module() }

        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(TestFixtures.payload("createDeposit"))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val createdId = parseObject(created.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

        val fetched = client.get("/transactions/$createdId")
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertEquals(createdId, parseObject(fetched.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content)

        val missing = client.get("/transactions/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, missing.status)
    }

    @Test
    fun `balance endpoint validates account id format`() = testApplication {
        application { module() }

        val invalid = client.get("/accounts/ACC-12/balance")
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertTrue("accountId" in extractErrorFields(invalid.bodyAsText()))
    }

    @Test
    fun `summary endpoint returns account transactions sorted newest first and handles empty or invalid account`() =
        testApplication {
            application { module() }

            val deposit = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody(TestFixtures.payload("createDeposit"))
            }
            assertEquals(HttpStatusCode.Created, deposit.status)
            val depositId = parseObject(deposit.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

            val transfer = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody(TestFixtures.payload("createTransfer"))
            }
            assertEquals(HttpStatusCode.Created, transfer.status)
            val transferId = parseObject(transfer.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

            val failedWithdrawal = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody(TestFixtures.payload("createInsufficientWithdrawal"))
            }
            assertEquals(HttpStatusCode.Created, failedWithdrawal.status)
            val failedBody = parseObject(failedWithdrawal.bodyAsText()).jsonObject
            assertEquals("FAILED", failedBody.getValue("status").jsonPrimitive.content)
            val failedId = failedBody.getValue("id").jsonPrimitive.content

            val summary = client.get("/accounts/ACC-A1B2C/summary")
            assertEquals(HttpStatusCode.OK, summary.status)
            val summaryArray = parseObject(summary.bodyAsText()).jsonArray
            assertEquals(3, summaryArray.size)
            assertEquals(
                listOf(failedId, transferId, depositId),
                summaryArray.map { it.jsonObject.getValue("id").jsonPrimitive.content }
            )
            assertTrue(summaryArray.any { it.jsonObject.getValue("status").jsonPrimitive.content == "FAILED" })

            val emptySummary = client.get("/accounts/${TestFixtures.lookup("summaryEmptyAccountId")}/summary")
            assertEquals(HttpStatusCode.OK, emptySummary.status)
            assertEquals(0, parseObject(emptySummary.bodyAsText()).jsonArray.size)

            val invalidAccountSummary = client.get("/accounts/${TestFixtures.lookup("invalidAccountId")}/summary")
            assertEquals(HttpStatusCode.BadRequest, invalidAccountSummary.status)
            val invalidFields = extractErrorFields(invalidAccountSummary.bodyAsText())
            assertTrue("accountId" in invalidFields)
        }

    @Test
    fun `health route returns hello world`() = testApplication {
        application { module() }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello World", response.bodyAsText())
    }

    @Test
    fun `openapi specification endpoint is available`() = testApplication {
        application { module() }

        val response = client.get("/openapi.yaml")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("openapi:"))
        assertTrue(body.contains("/transactions:"))
        assertTrue(body.contains("/accounts/{accountId}/summary:"))
    }

    @Test
    fun `swagger ui endpoint is available`() = testApplication {
        application { module() }

        val response = client.get("/swagger")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("<html", ignoreCase = true))
    }

    private fun parseObject(text: String) = json.parseToJsonElement(text)

    private fun extractErrorFields(body: String): Set<String> =
        parseObject(body)
            .jsonObject
            .getValue("details")
            .jsonArray
            .map { it.jsonObject.getValue("field").jsonPrimitive.content }
            .toSet()
}

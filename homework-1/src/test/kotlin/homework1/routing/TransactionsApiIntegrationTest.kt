package homework1.routing

import homework1.entrypoint.module
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
            setBody(
                """{
                    "toAccount":"ACC-A1B2C",
                    "amount":10.0,
                    "currency":"USD",
                    "type":"deposit",
                    "status":"completed"
                }"""
            )
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
            setBody("""{"toAccount":"ACC-A1B2C","amount":200.0,"currency":"USD","type":"deposit"}""")
        }
        assertEquals(HttpStatusCode.Created, deposit.status)
        assertEquals("COMPLETED", parseObject(deposit.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content)

        val transfer = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                    "fromAccount":"ACC-A1B2C",
                    "toAccount":"ACC-D3E4F",
                    "amount":150.0,
                    "currency":"USD",
                    "type":"transfer"
                }"""
            )
        }
        assertEquals(HttpStatusCode.Created, transfer.status)
        assertEquals("COMPLETED", parseObject(transfer.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content)

        val withdrawalFailed = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"fromAccount":"ACC-A1B2C","amount":100.0,"currency":"USD","type":"withdrawal"}""")
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
        assertEquals("50.0", balancesObject.getValue("USD").jsonPrimitive.content)
    }

    @Test
    fun `summary endpoint returns account transactions sorted newest first and handles empty or invalid account`() =
        testApplication {
            application { module() }

            val deposit = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody("""{"toAccount":"ACC-A1B2C","amount":200.0,"currency":"USD","type":"deposit"}""")
            }
            assertEquals(HttpStatusCode.Created, deposit.status)
            val depositId = parseObject(deposit.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

            val transfer = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{
                        "fromAccount":"ACC-A1B2C",
                        "toAccount":"ACC-D3E4F",
                        "amount":150.0,
                        "currency":"USD",
                        "type":"transfer"
                    }"""
                )
            }
            assertEquals(HttpStatusCode.Created, transfer.status)
            val transferId = parseObject(transfer.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content

            val failedWithdrawal = client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody("""{"fromAccount":"ACC-A1B2C","amount":100.0,"currency":"USD","type":"withdrawal"}""")
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

            val emptySummary = client.get("/accounts/ACC-Z9Y8X/summary")
            assertEquals(HttpStatusCode.OK, emptySummary.status)
            assertEquals(0, parseObject(emptySummary.bodyAsText()).jsonArray.size)

            val invalidAccountSummary = client.get("/accounts/ACC-12/summary")
            assertEquals(HttpStatusCode.BadRequest, invalidAccountSummary.status)
            val invalidFields = extractErrorFields(invalidAccountSummary.bodyAsText())
            assertTrue("accountId" in invalidFields)
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

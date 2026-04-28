package homework1.models

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MoneyAndCurrencySerializationTest {
    private val json = Json

    @Test
    fun `currency decoding is case insensitive`() {
        val payload = """
            {
              "toAccount":"ACC-A1B2C",
              "amount":"10.00",
              "currency":"usd",
              "type":"deposit"
            }
        """.trimIndent()

        val request = json.decodeFromString<CreateTransactionRequest>(payload)

        assertEquals(CurrencyCode.USD, request.currency)
    }

    @Test
    fun `invalid currency code fails deserialization`() {
        val payload = """
            {
              "toAccount":"ACC-A1B2C",
              "amount":"10.00",
              "currency":"not-a-code",
              "type":"deposit"
            }
        """.trimIndent()

        assertFailsWith<SerializationException> {
            json.decodeFromString<CreateTransactionRequest>(payload)
        }
    }

    @Test
    fun `amount supports tolerant input from string and number`() {
        val stringPayload = """
            {
              "toAccount":"ACC-A1B2C",
              "amount":"10.50",
              "currency":"USD",
              "type":"deposit"
            }
        """.trimIndent()
        val numberPayload = """
            {
              "toAccount":"ACC-A1B2C",
              "amount":10.5,
              "currency":"USD",
              "type":"deposit"
            }
        """.trimIndent()

        val fromString = json.decodeFromString<CreateTransactionRequest>(stringPayload)
        val fromNumber = json.decodeFromString<CreateTransactionRequest>(numberPayload)

        assertEquals(BigDecimal("10.50"), fromString.amount)
        assertEquals(BigDecimal("10.5"), fromNumber.amount)
    }

    @Test
    fun `amount and balances serialize as decimal strings`() {
        val transaction = Transaction(
            id = "tx-1",
            fromAccount = null,
            toAccount = "ACC-A1B2C",
            amount = BigDecimal("10.50"),
            currency = CurrencyCode.USD,
            type = TransactionType.DEPOSIT,
            timestamp = "2026-01-01T00:00:00Z",
            status = TransactionStatus.COMPLETED
        )
        val balance = BalanceResponse(
            accountId = "ACC-A1B2C",
            balances = mapOf(CurrencyCode.USD to BigDecimal("19.65"))
        )

        val txJson = json.encodeToString(transaction)
        val balanceJson = json.encodeToString(balance)

        kotlin.test.assertTrue(txJson.contains("\"amount\":\"10.50\""))
        kotlin.test.assertTrue(txJson.contains("\"currency\":\"USD\""))
        kotlin.test.assertTrue(balanceJson.contains("\"USD\":\"19.65\""))
    }
}

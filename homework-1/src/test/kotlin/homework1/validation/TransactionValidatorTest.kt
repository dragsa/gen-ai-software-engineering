package homework1.validation

import homework1.models.CreateTransactionRequest
import homework1.models.CurrencyCode
import homework1.models.TransactionType
import homework1.testsupport.TestFixtures
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class TransactionValidatorTest {
    private val validator = TransactionValidator()
    private val json = Json

    @Test
    fun `amount must be positive`() {
        val request = CreateTransactionRequest(
            fromAccount = "ACC-A1B2C",
            toAccount = "ACC-D3E4F",
            amount = BigDecimal("-10.00"),
            currency = CurrencyCode.USD,
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)
        assertTrue(errors.any { it.field == "amount" })
    }

    @Test
    fun `amount must have at most two decimals`() {
        val request = CreateTransactionRequest(
            fromAccount = "ACC-A1B2C",
            toAccount = "ACC-D3E4F",
            amount = BigDecimal("10.005"),
            currency = CurrencyCode.USD,
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)
        assertTrue(errors.any { it.field == "amount" })
    }

    @Test
    fun `valid amount with two decimals passes validation`() {
        val request = CreateTransactionRequest(
            fromAccount = "ACC-A1B2C",
            toAccount = "ACC-D3E4F",
            amount = BigDecimal("10.50"),
            currency = CurrencyCode.USD,
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)
        assertFalse(errors.any { it.field == "amount" })
    }

    @Test
    fun `deposit requires toAccount only`() {
        val missingTo = CreateTransactionRequest(
            amount = BigDecimal("10.00"),
            currency = CurrencyCode.USD,
            type = "deposit"
        )
        val withTo = CreateTransactionRequest(
            toAccount = "ACC-A1B2C",
            amount = BigDecimal("10.00"),
            currency = CurrencyCode.USD,
            type = "deposit"
        )

        val missingToErrors = validator.validateCreateRequest(missingTo)
        val withToErrors = validator.validateCreateRequest(withTo)

        assertTrue(missingToErrors.any { it.field == "toAccount" })
        assertFalse(withToErrors.any { it.field == "toAccount" })
        assertFalse(withToErrors.any { it.field == "fromAccount" })
    }

    @Test
    fun `withdrawal requires fromAccount only`() {
        val missingFrom = CreateTransactionRequest(
            amount = BigDecimal("10.00"),
            currency = CurrencyCode.USD,
            type = "withdrawal"
        )
        val withFrom = CreateTransactionRequest(
            fromAccount = "ACC-A1B2C",
            amount = BigDecimal("10.00"),
            currency = CurrencyCode.USD,
            type = "withdrawal"
        )

        val missingFromErrors = validator.validateCreateRequest(missingFrom)
        val withFromErrors = validator.validateCreateRequest(withFrom)

        assertTrue(missingFromErrors.any { it.field == "fromAccount" })
        assertFalse(withFromErrors.any { it.field == "fromAccount" })
        assertFalse(withFromErrors.any { it.field == "toAccount" })
    }

    @Test
    fun `transfer requires both fromAccount and toAccount`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("10.00"),
            currency = CurrencyCode.USD,
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)

        assertTrue(errors.any { it.field == "fromAccount" })
        assertTrue(errors.any { it.field == "toAccount" })
    }

    @Test
    fun `create request validates account format for both fromAccount and toAccount`() {
        val request = CreateTransactionRequest(
            fromAccount = "BAD",
            toAccount = "ALSO_BAD",
            amount = BigDecimal("10.00"),
            currency = CurrencyCode.USD,
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)

        assertTrue(errors.any { it.field == "fromAccount" })
        assertTrue(errors.any { it.field == "toAccount" })
    }

    @Test
    fun `validate filters returns all field errors for invalid input`() {
        val result = validator.validateFilters(
            accountId = "ACC-12",
            typeRaw = "wrong",
            fromRaw = "2026-99-01",
            toRaw = "still-not-date"
        )

        val errorFields = result.errors.map { it.field }.toSet()
        assertTrue("accountId" in errorFields)
        assertTrue("type" in errorFields)
        assertTrue("from" in errorFields)
        assertTrue("to" in errorFields)
    }

    @Test
    fun `validate filters rejects date range where from is after to`() {
        val result = validator.validateFilters(
            accountId = null,
            typeRaw = null,
            fromRaw = "2026-02-02",
            toRaw = "2026-01-31"
        )

        assertTrue(result.errors.any { it.field == "from" })
    }

    @Test
    fun `validate filters returns parsed filter for valid values`() {
        val result = validator.validateFilters(
            accountId = "ACC-A1B2C",
            typeRaw = "deposit",
            fromRaw = "2026-01-01",
            toRaw = "2026-01-31"
        )

        assertTrue(result.errors.isEmpty())
        assertEquals("ACC-A1B2C", result.filter.accountId)
        assertEquals(TransactionType.DEPOSIT, result.filter.type)
        assertEquals(LocalDate.parse("2026-01-01"), result.filter.from)
        assertEquals(LocalDate.parse("2026-01-31"), result.filter.to)
    }

    @Test
    fun `toCreateCommand maps to typed domain command`() {
        val deposit = validator.toCreateCommand(
            CreateTransactionRequest(
                toAccount = "ACC-A1B2C",
                amount = BigDecimal("10.00"),
                currency = CurrencyCode.USD,
                type = "deposit"
            )
        )
        val withdrawal = validator.toCreateCommand(
            CreateTransactionRequest(
                fromAccount = "ACC-A1B2C",
                amount = BigDecimal("10.00"),
                currency = CurrencyCode.USD,
                type = "withdrawal"
            )
        )
        val transfer = validator.toCreateCommand(
            CreateTransactionRequest(
                fromAccount = "ACC-A1B2C",
                toAccount = "ACC-D3E4F",
                amount = BigDecimal("10.00"),
                currency = CurrencyCode.USD,
                type = "transfer"
            )
        )

        assertTrue(deposit is homework1.models.DepositCommand)
        assertTrue(withdrawal is homework1.models.WithdrawalCommand)
        assertTrue(transfer is homework1.models.TransferCommand)
    }

    @Test
    fun `fixture happy payloads stay valid and invalid payload stays invalid`() {
        val happyPayloads = listOf(
            "createDeposit",
            "createTransfer",
            "createWithdrawal",
            "createInsufficientWithdrawal"
        )

        happyPayloads.forEach { payloadName ->
            val request = json.decodeFromString<CreateTransactionRequest>(TestFixtures.payload(payloadName))
            val errors = validator.validateCreateRequest(request)
            assertTrue(errors.isEmpty(), "Expected fixture '$payloadName' to be valid, got: $errors")
        }

        val invalidRequest = json.decodeFromString<CreateTransactionRequest>(TestFixtures.payload("invalidTransaction"))
        val invalidErrors = validator.validateCreateRequest(invalidRequest)
        assertTrue(invalidErrors.isNotEmpty())
    }
}

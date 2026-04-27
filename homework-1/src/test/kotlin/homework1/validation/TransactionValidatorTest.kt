package homework1.validation

import homework1.models.CreateTransactionRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionValidatorTest {
    private val validator = TransactionValidator()

    @Test
    fun `amount must be positive and have at most two decimals`() {
        val request = CreateTransactionRequest(
            fromAccount = "ACC-A1B2C",
            toAccount = "ACC-D3E4F",
            amount = -10.005,
            currency = "USD",
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)
        assertTrue(errors.any { it.field == "amount" })
    }

    @Test
    fun `status in request is ignored by validator`() {
        val request = CreateTransactionRequest(
            toAccount = "ACC-A1B2C",
            amount = 10.0,
            currency = "USD",
            type = "deposit",
            status = "completed"
        )

        val errors = validator.validateCreateRequest(request)

        assertFalse(errors.any { it.field == "status" })
    }

    @Test
    fun `deposit requires toAccount only`() {
        val missingTo = CreateTransactionRequest(
            amount = 10.0,
            currency = "USD",
            type = "deposit"
        )
        val withTo = CreateTransactionRequest(
            toAccount = "ACC-A1B2C",
            amount = 10.0,
            currency = "USD",
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
            amount = 10.0,
            currency = "USD",
            type = "withdrawal"
        )
        val withFrom = CreateTransactionRequest(
            fromAccount = "ACC-A1B2C",
            amount = 10.0,
            currency = "USD",
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
            amount = 10.0,
            currency = "USD",
            type = "transfer"
        )

        val errors = validator.validateCreateRequest(request)

        assertTrue(errors.any { it.field == "fromAccount" })
        assertTrue(errors.any { it.field == "toAccount" })
    }

    @Test
    fun `toCreateCommand maps to typed domain command`() {
        val deposit = validator.toCreateCommand(
            CreateTransactionRequest(
                toAccount = "ACC-A1B2C",
                amount = 10.0,
                currency = "usd",
                type = "deposit"
            )
        )
        val withdrawal = validator.toCreateCommand(
            CreateTransactionRequest(
                fromAccount = "ACC-A1B2C",
                amount = 10.0,
                currency = "usd",
                type = "withdrawal"
            )
        )
        val transfer = validator.toCreateCommand(
            CreateTransactionRequest(
                fromAccount = "ACC-A1B2C",
                toAccount = "ACC-D3E4F",
                amount = 10.0,
                currency = "usd",
                type = "transfer"
            )
        )

        assertTrue(deposit is homework1.models.DepositCommand)
        assertTrue(withdrawal is homework1.models.WithdrawalCommand)
        assertTrue(transfer is homework1.models.TransferCommand)
    }
}

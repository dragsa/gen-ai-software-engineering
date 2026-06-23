package homework6.agent

import homework6.common.TransactionData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionValidatorTest {

    @Test
    fun validTransactionPasses() {
        assertNull(TransactionValidator.validate(txn(id = "TXN001").data))
    }

    @Test
    fun missingSourceAccountIsRejected() {
        val reason = TransactionValidator.validate(txn(sourceAccount = null).data)
        assertEquals("missing source_account", reason)
    }

    @Test
    fun missingAmountIsRejected() {
        val reason = TransactionValidator.validate(txn(amount = null).data)
        assertEquals("missing amount", reason)
    }

    @Test
    fun nonPositiveAmountIsRejected() {
        val reason = TransactionValidator.validate(txn(amount = "-100.00").data)
        assertEquals("amount must be positive", reason)
    }

    @Test
    fun currencyOutsideIso4217IsRejected() {
        val reason = TransactionValidator.validate(txn(currency = "XYZ").data)
        assertNotNull(reason)
        assertTrue(reason.contains("ISO 4217"))
    }

    @Test
    fun processMarksValidatedAndRoutesToFraudDetector() {
        val out = TransactionValidator.process(txn(id = "TXN001"))
        assertEquals("validated", out.data.status)
        assertEquals("transaction_validator", out.sourceAgent)
        assertEquals("fraud_detector", out.targetAgent)
        assertNull(out.data.reason)
    }

    @Test
    fun processMarksRejectedWithReason() {
        val out = TransactionValidator.process(txn(id = "TXN006", currency = "XYZ"))
        assertEquals("rejected", out.data.status)
        assertNotNull(out.data.reason)
    }

    @Test
    fun blankTransactionIdIsRejected() {
        assertEquals("missing transaction_id", TransactionValidator.validate(TransactionData(transactionId = "")))
    }
}

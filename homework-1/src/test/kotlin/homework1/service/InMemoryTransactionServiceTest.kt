package homework1.service

import homework1.models.CurrencyCode
import homework1.models.DepositCommand
import homework1.models.TransactionStatus
import homework1.models.TransferCommand
import homework1.models.WithdrawalCommand
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryTransactionServiceTest {
    @Test
    fun `service derives transaction status and excludes failed from balance`() {
        var idCounter = 0
        val service = InMemoryTransactionService(
            idGenerator = {
                idCounter += 1
                "tx-$idCounter"
            },
            timestampProvider = { "2026-01-01T00:00:00Z" }
        )

        val deposit = service.createTransaction(
            DepositCommand(
                toAccount = "ACC-A1B2C",
                amount = BigDecimal("100.00"),
                currency = CurrencyCode.USD
            )
        )
        assertEquals(TransactionStatus.COMPLETED, deposit.status)

        val withdrawalCompleted = service.createTransaction(
            WithdrawalCommand(
                fromAccount = "ACC-A1B2C",
                amount = BigDecimal("40.00"),
                currency = CurrencyCode.USD
            )
        )
        assertEquals(TransactionStatus.COMPLETED, withdrawalCompleted.status)

        val withdrawalFailed = service.createTransaction(
            WithdrawalCommand(
                fromAccount = "ACC-A1B2C",
                amount = BigDecimal("100.00"),
                currency = CurrencyCode.USD
            )
        )
        assertEquals(TransactionStatus.FAILED, withdrawalFailed.status)

        val balance = service.getAccountBalance("ACC-A1B2C")
        assertEquals(BigDecimal("60.00"), balance.balances[CurrencyCode.USD])

        val listed = service.listTransactions(homework1.models.TransactionFilter())
        assertEquals(3, listed.size)
        assertTrue(listed.any { it.id == withdrawalFailed.id && it.status == TransactionStatus.FAILED })

        val foundFailed = service.getTransactionById(withdrawalFailed.id)
        assertNotNull(foundFailed)
        assertEquals(TransactionStatus.FAILED, foundFailed.status)
        assertNull(service.getTransactionById("unknown"))
    }

    @Test
    fun `transfer requires sufficient source funds to complete`() {
        val service = InMemoryTransactionService(
            idGenerator = { java.util.UUID.randomUUID().toString() },
            timestampProvider = { "2026-01-01T00:00:00Z" }
        )

        service.createTransaction(
            DepositCommand(
                toAccount = "ACC-SRC01",
                amount = BigDecimal("50.00"),
                currency = CurrencyCode.USD
            )
        )

        val completedTransfer = service.createTransaction(
            TransferCommand(
                fromAccount = "ACC-SRC01",
                toAccount = "ACC-DST01",
                amount = BigDecimal("30.00"),
                currency = CurrencyCode.USD
            )
        )
        assertEquals(TransactionStatus.COMPLETED, completedTransfer.status)

        val failedTransfer = service.createTransaction(
            TransferCommand(
                fromAccount = "ACC-SRC01",
                toAccount = "ACC-DST01",
                amount = BigDecimal("100.00"),
                currency = CurrencyCode.USD
            )
        )
        assertEquals(TransactionStatus.FAILED, failedTransfer.status)

        val sourceBalance = service.getAccountBalance("ACC-SRC01")
        val destinationBalance = service.getAccountBalance("ACC-DST01")
        assertEquals(BigDecimal("20.00"), sourceBalance.balances[CurrencyCode.USD])
        assertEquals(BigDecimal("30.00"), destinationBalance.balances[CurrencyCode.USD])
    }

    @Test
    fun `summary includes all statuses for account sorted by newest first and empty when none`() {
        var idCounter = 0
        val timestamps = ArrayDeque(
            listOf(
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:01:00Z",
                "2026-01-01T00:02:00Z",
                "2026-01-01T00:03:00Z"
            )
        )
        val service = InMemoryTransactionService(
            idGenerator = {
                idCounter += 1
                "tx-$idCounter"
            },
            timestampProvider = { timestamps.removeFirst() }
        )

        val deposit = service.createTransaction(
            DepositCommand(
                toAccount = "ACC-A1B2C",
                amount = BigDecimal("100.00"),
                currency = CurrencyCode.USD
            )
        )
        val transferOut = service.createTransaction(
            TransferCommand(
                fromAccount = "ACC-A1B2C",
                toAccount = "ACC-D3E4F",
                amount = BigDecimal("60.00"),
                currency = CurrencyCode.USD
            )
        )
        val failedWithdrawal = service.createTransaction(
            WithdrawalCommand(
                fromAccount = "ACC-A1B2C",
                amount = BigDecimal("100.00"),
                currency = CurrencyCode.USD
            )
        )
        service.createTransaction(
            DepositCommand(
                toAccount = "ACC-Z9Y8X",
                amount = BigDecimal("1.00"),
                currency = CurrencyCode.USD
            )
        )

        val summary = service.getAccountSummary("ACC-A1B2C")
        assertEquals(3, summary.size)
        assertEquals(listOf(failedWithdrawal.id, transferOut.id, deposit.id), summary.map { it.id })
        assertTrue(summary.any { it.status == TransactionStatus.FAILED })

        val missingAccountSummary = service.getAccountSummary("ACC-NONE1")
        assertTrue(missingAccountSummary.isEmpty())
    }

    @Test
    fun `decimal arithmetic stays precise for recurring financial additions and subtractions`() {
        val service = InMemoryTransactionService(
            idGenerator = { java.util.UUID.randomUUID().toString() },
            timestampProvider = { "2026-01-01T00:00:00Z" }
        )

        service.createTransaction(
            DepositCommand(
                toAccount = "ACC-DEC01",
                amount = BigDecimal("0.10"),
                currency = CurrencyCode.USD
            )
        )
        service.createTransaction(
            DepositCommand(
                toAccount = "ACC-DEC01",
                amount = BigDecimal("0.20"),
                currency = CurrencyCode.USD
            )
        )
        service.createTransaction(
            WithdrawalCommand(
                fromAccount = "ACC-DEC01",
                amount = BigDecimal("0.30"),
                currency = CurrencyCode.USD
            )
        )

        val balance = service.getAccountBalance("ACC-DEC01")
        assertEquals(BigDecimal("0.00"), balance.balances[CurrencyCode.USD])
    }
}

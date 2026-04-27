package homework1.service

import homework1.models.DepositCommand
import homework1.models.TransactionStatus
import homework1.models.TransferCommand
import homework1.models.WithdrawalCommand
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
                amount = 100.0,
                currency = "USD"
            )
        )
        assertEquals(TransactionStatus.COMPLETED, deposit.status)

        val withdrawalCompleted = service.createTransaction(
            WithdrawalCommand(
                fromAccount = "ACC-A1B2C",
                amount = 40.0,
                currency = "USD"
            )
        )
        assertEquals(TransactionStatus.COMPLETED, withdrawalCompleted.status)

        val withdrawalFailed = service.createTransaction(
            WithdrawalCommand(
                fromAccount = "ACC-A1B2C",
                amount = 100.0,
                currency = "USD"
            )
        )
        assertEquals(TransactionStatus.FAILED, withdrawalFailed.status)

        val balance = service.getAccountBalance("ACC-A1B2C")
        assertEquals("60.0", balance.balances["USD"])

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
                amount = 50.0,
                currency = "USD"
            )
        )

        val completedTransfer = service.createTransaction(
            TransferCommand(
                fromAccount = "ACC-SRC01",
                toAccount = "ACC-DST01",
                amount = 30.0,
                currency = "USD"
            )
        )
        assertEquals(TransactionStatus.COMPLETED, completedTransfer.status)

        val failedTransfer = service.createTransaction(
            TransferCommand(
                fromAccount = "ACC-SRC01",
                toAccount = "ACC-DST01",
                amount = 100.0,
                currency = "USD"
            )
        )
        assertEquals(TransactionStatus.FAILED, failedTransfer.status)

        val sourceBalance = service.getAccountBalance("ACC-SRC01")
        val destinationBalance = service.getAccountBalance("ACC-DST01")
        assertEquals("20.0", sourceBalance.balances["USD"])
        assertEquals("30.0", destinationBalance.balances["USD"])
    }
}

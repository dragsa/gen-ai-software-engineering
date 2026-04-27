package homework1.service

import homework1.models.BalanceResponse
import homework1.models.CreateTransactionCommand
import homework1.models.Transaction
import homework1.models.TransactionFilter
import homework1.models.TransactionStatus
import homework1.utils.isoInstantToUtcDate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class InMemoryTransactionService(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val timestampProvider: () -> String = { Instant.now().toString() }
) : TransactionService {

    private val transactions = mutableListOf<Transaction>()

    override fun createTransaction(command: CreateTransactionCommand): Transaction {
        val transaction = Transaction(
            id = idGenerator(),
            fromAccount = command.fromAccount,
            toAccount = command.toAccount,
            amount = command.amount,
            currency = command.currency,
            type = command.type,
            timestamp = timestampProvider(),
            status = command.status
        )
        synchronized(transactions) {
            transactions.add(transaction)
        }
        return transaction
    }

    override fun listTransactions(filter: TransactionFilter): List<Transaction> =
        snapshot().filter { transaction ->
            val accountMatch =
                filter.accountId == null ||
                    transaction.fromAccount == filter.accountId ||
                    transaction.toAccount == filter.accountId
            val typeMatch = filter.type == null || transaction.type == filter.type
            val date = isoInstantToUtcDate(transaction.timestamp)
            val fromMatch = filter.from == null || date >= filter.from
            val toMatch = filter.to == null || date <= filter.to
            accountMatch && typeMatch && fromMatch && toMatch
        }

    override fun getTransactionById(id: String): Transaction? =
        snapshot().firstOrNull { it.id == id }

    override fun getAccountBalance(accountId: String): BalanceResponse {
        val balancesByCurrency = mutableMapOf<String, BigDecimal>()
        snapshot()
            .asSequence()
            .filter { it.status != TransactionStatus.FAILED }
            .forEach { transaction ->
                val amount = BigDecimal.valueOf(transaction.amount)
                if (transaction.toAccount == accountId) {
                    balancesByCurrency[transaction.currency] =
                        (balancesByCurrency[transaction.currency] ?: BigDecimal.ZERO).add(amount)
                }
                if (transaction.fromAccount == accountId) {
                    balancesByCurrency[transaction.currency] =
                        (balancesByCurrency[transaction.currency] ?: BigDecimal.ZERO).subtract(amount)
                }
            }

        return BalanceResponse(
            accountId = accountId,
            balances = balancesByCurrency.mapValues { (_, amount) -> amount.toPlainString() }
        )
    }

    private fun snapshot(): List<Transaction> =
        synchronized(transactions) { transactions.toList() }
}

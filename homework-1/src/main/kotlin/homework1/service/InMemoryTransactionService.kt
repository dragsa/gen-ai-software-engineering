package homework1.service

import homework1.models.BalanceResponse
import homework1.models.CreateTransactionCommand
import homework1.models.DepositCommand
import homework1.models.Transaction
import homework1.models.TransactionFilter
import homework1.models.TransactionStatus
import homework1.models.TransferCommand
import homework1.models.WithdrawalCommand
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
        synchronized(transactions) {
            val status = resolveStatus(command, transactions)
            val (fromAccount, toAccount, type) = when (command) {
                is DepositCommand -> Triple(null, command.toAccount, homework1.models.TransactionType.DEPOSIT)
                is WithdrawalCommand -> Triple(command.fromAccount, null, homework1.models.TransactionType.WITHDRAWAL)
                is TransferCommand -> Triple(command.fromAccount, command.toAccount, homework1.models.TransactionType.TRANSFER)
            }
            val transaction = Transaction(
                id = idGenerator(),
                fromAccount = fromAccount,
                toAccount = toAccount,
                amount = command.amount,
                currency = command.currency,
                type = type,
                timestamp = timestampProvider(),
                status = status
            )
            transactions.add(transaction)
            return transaction
        }
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

    override fun getAccountSummary(accountId: String): List<Transaction> =
        snapshot()
            .asSequence()
            .filter { it.fromAccount == accountId || it.toAccount == accountId }
            .sortedByDescending { Instant.parse(it.timestamp) }
            .toList()

    private fun snapshot(): List<Transaction> =
        synchronized(transactions) { transactions.toList() }

    private fun resolveStatus(command: CreateTransactionCommand, existing: List<Transaction>): TransactionStatus {
        if (command is DepositCommand) return TransactionStatus.COMPLETED

        val fromAccount = when (command) {
            is WithdrawalCommand -> command.fromAccount
            is TransferCommand -> command.fromAccount
            is DepositCommand -> return TransactionStatus.COMPLETED
        }
        val available = completedBalanceForCurrency(fromAccount, command.currency, existing)
        val requested = BigDecimal.valueOf(command.amount)
        return if (available.compareTo(requested) >= 0) {
            TransactionStatus.COMPLETED
        } else {
            TransactionStatus.FAILED
        }
    }

    private fun completedBalanceForCurrency(
        accountId: String,
        currency: String,
        existing: List<Transaction>
    ): BigDecimal =
        existing.asSequence()
            .filter { it.status == TransactionStatus.COMPLETED }
            .filter { it.currency == currency }
            .fold(BigDecimal.ZERO) { acc, transaction ->
                val amount = BigDecimal.valueOf(transaction.amount)
                var next = acc
                if (transaction.toAccount == accountId) {
                    next = next.add(amount)
                }
                if (transaction.fromAccount == accountId) {
                    next = next.subtract(amount)
                }
                next
            }
}

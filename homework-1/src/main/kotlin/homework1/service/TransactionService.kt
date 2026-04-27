package homework1.service

import homework1.models.BalanceResponse
import homework1.models.CreateTransactionCommand
import homework1.models.Transaction
import homework1.models.TransactionFilter

interface TransactionService {
    fun createTransaction(command: CreateTransactionCommand): Transaction
    fun listTransactions(filter: TransactionFilter): List<Transaction>
    fun getTransactionById(id: String): Transaction?
    fun getAccountBalance(accountId: String): BalanceResponse
    fun getAccountSummary(accountId: String): List<Transaction>
}

package homework1.models

import java.time.LocalDate

data class CreateTransactionCommand(
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val status: TransactionStatus
)

data class TransactionFilter(
    val accountId: String? = null,
    val type: TransactionType? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

package homework1.models

import java.time.LocalDate

sealed interface CreateTransactionCommand {
    val amount: Double
    val currency: String
}

data class DepositCommand(
    val toAccount: String,
    override val amount: Double,
    override val currency: String
) : CreateTransactionCommand

data class WithdrawalCommand(
    val fromAccount: String,
    override val amount: Double,
    override val currency: String
) : CreateTransactionCommand

data class TransferCommand(
    val fromAccount: String,
    val toAccount: String,
    override val amount: Double,
    override val currency: String
) : CreateTransactionCommand

data class TransactionFilter(
    val accountId: String? = null,
    val type: TransactionType? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

package homework1.models

import java.math.BigDecimal
import java.time.LocalDate

sealed interface CreateTransactionCommand {
    val amount: BigDecimal
    val currency: CurrencyCode
}

data class DepositCommand(
    val toAccount: String,
    override val amount: BigDecimal,
    override val currency: CurrencyCode
) : CreateTransactionCommand

data class WithdrawalCommand(
    val fromAccount: String,
    override val amount: BigDecimal,
    override val currency: CurrencyCode
) : CreateTransactionCommand

data class TransferCommand(
    val fromAccount: String,
    val toAccount: String,
    override val amount: BigDecimal,
    override val currency: CurrencyCode
) : CreateTransactionCommand

data class TransactionFilter(
    val accountId: String? = null,
    val type: TransactionType? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

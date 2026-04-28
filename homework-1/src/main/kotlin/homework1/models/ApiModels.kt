package homework1.models

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(
    val fromAccount: String? = null,
    val toAccount: String? = null,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val type: String
)

@Serializable
data class Transaction(
    val id: String,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val type: TransactionType,
    val timestamp: String,
    val status: TransactionStatus
)

@Serializable
data class BalanceResponse(
    val accountId: String,
    val balances: Map<CurrencyCode, @Serializable(with = BigDecimalAsStringSerializer::class) BigDecimal>
)

@Serializable
data class ErrorResponse(
    val error: String,
    val details: List<ValidationError> = emptyList()
)

@Serializable
data class ValidationError(
    val field: String,
    val message: String
)

@Serializable
enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}

@Serializable
enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}

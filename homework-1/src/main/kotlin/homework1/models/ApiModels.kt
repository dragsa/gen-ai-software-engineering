package homework1.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val type: String? = null,
    val status: String? = null
)

@Serializable
data class Transaction(
    val id: String,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val timestamp: String,
    val status: TransactionStatus
)

@Serializable
data class BalanceResponse(
    val accountId: String,
    val balances: Map<String, String>
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

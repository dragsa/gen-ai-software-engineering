package homework1

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID
import kotlinx.serialization.Serializable

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

private val transactions = mutableListOf<Transaction>()
private val validCurrencyCodes = Currency.getAvailableCurrencies().map { it.currencyCode }.toSet()
private val accountPattern = Regex("^ACC-[A-Za-z0-9]{5}$")

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello World")
        }

        route("/transactions") {
            post {
                val request = try {
                    call.receive<CreateTransactionRequest>()
                } catch (_: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            error = "Validation failed",
                            details = listOf(
                                ValidationError("body", "Request body must be valid JSON")
                            )
                        )
                    )
                    return@post
                }

                val errors = validateCreateTransaction(request)
                if (errors.isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Validation failed", errors)
                    )
                    return@post
                }

                val created = Transaction(
                    id = UUID.randomUUID().toString(),
                    fromAccount = request.fromAccount,
                    toAccount = request.toAccount,
                    amount = request.amount!!,
                    currency = request.currency!!.uppercase(),
                    type = parseTypeValue(request.type)!!,
                    timestamp = Instant.now().toString(),
                    status = parseStatusValue(request.status) ?: TransactionStatus.COMPLETED
                )
                transactions.add(created)
                call.respond(HttpStatusCode.Created, created)
            }

            get {
                val accountId = call.request.queryParameters["accountId"]
                val typeRaw = call.request.queryParameters["type"]
                val fromRaw = call.request.queryParameters["from"]
                val toRaw = call.request.queryParameters["to"]

                val errors = mutableListOf<ValidationError>()
                if (accountId != null && !isValidAccount(accountId)) {
                    errors.add(
                        ValidationError(
                            "accountId",
                            "Account must follow ACC-XXXXX format with alphanumeric suffix"
                        )
                    )
                }

                val type = parseType(typeRaw, "type", errors)
                val fromDate = parseDate(fromRaw, "from", errors)
                val toDate = parseDate(toRaw, "to", errors)

                if (fromDate != null && toDate != null && fromDate > toDate) {
                    errors.add(ValidationError("from", "from must be less than or equal to to"))
                }

                if (errors.isNotEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Validation failed", errors))
                    return@get
                }

                val filtered = transactions.filter { tx ->
                    val accountMatch = accountId == null || tx.fromAccount == accountId || tx.toAccount == accountId
                    val typeMatch = type == null || tx.type == type
                    val date = Instant.parse(tx.timestamp).atOffset(ZoneOffset.UTC).toLocalDate()
                    val fromMatch = fromDate == null || date >= fromDate
                    val toMatch = toDate == null || date <= toDate
                    accountMatch && typeMatch && fromMatch && toMatch
                }
                call.respond(HttpStatusCode.OK, filtered)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Transaction id is required"))
                    return@get
                }

                val transaction = transactions.firstOrNull { it.id == id }
                if (transaction == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transaction not found"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, transaction)
            }
        }

        get("/accounts/{accountId}/balance") {
            val accountId = call.parameters["accountId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "accountId is required"))
                return@get
            }

            if (!isValidAccount(accountId)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        "Validation failed",
                        listOf(
                            ValidationError(
                                "accountId",
                                "Account must follow ACC-XXXXX format with alphanumeric suffix"
                            )
                        )
                    )
                )
                return@get
            }

            val balancesByCurrency = mutableMapOf<String, BigDecimal>()
            transactions
                .asSequence()
                .filter { it.status != TransactionStatus.FAILED }
                .forEach { tx ->
                    val value = BigDecimal.valueOf(tx.amount)
                    if (tx.toAccount == accountId) {
                        balancesByCurrency[tx.currency] =
                            (balancesByCurrency[tx.currency] ?: BigDecimal.ZERO).add(value)
                    }
                    if (tx.fromAccount == accountId) {
                        balancesByCurrency[tx.currency] =
                            (balancesByCurrency[tx.currency] ?: BigDecimal.ZERO).subtract(value)
                    }
                }

            val result = BalanceResponse(
                accountId = accountId,
                balances = balancesByCurrency.mapValues { (_, amount) -> amount.toPlainString() }
            )
            call.respond(HttpStatusCode.OK, result)
        }
    }
}

private fun isValidAccount(account: String): Boolean = accountPattern.matches(account)

private fun parseType(
    raw: String?,
    field: String,
    errors: MutableList<ValidationError>
): TransactionType? {
    if (raw == null) return null
    return runCatching { TransactionType.valueOf(raw.uppercase()) }
        .getOrElse {
            errors.add(ValidationError(field, "type must be one of: deposit, withdrawal, transfer"))
            null
        }
}

private fun parseDate(
    raw: String?,
    field: String,
    errors: MutableList<ValidationError>
): LocalDate? {
    if (raw == null) return null
    return runCatching { LocalDate.parse(raw) }
        .getOrElse {
            errors.add(ValidationError(field, "$field must follow ISO date format YYYY-MM-DD"))
            null
        }
}

private fun validateCreateTransaction(request: CreateTransactionRequest): List<ValidationError> {
    val errors = mutableListOf<ValidationError>()

    if (request.amount == null || request.amount <= 0) {
        errors.add(ValidationError("amount", "Amount must be a positive number"))
    } else {
        val scale = BigDecimal.valueOf(request.amount).stripTrailingZeros().scale()
        if (scale > 2) {
            errors.add(ValidationError("amount", "Amount must have at most 2 decimal places"))
        }
    }

    if (request.currency == null || request.currency.uppercase() !in validCurrencyCodes) {
        errors.add(ValidationError("currency", "Invalid currency code"))
    }

    val type = parseTypeValue(request.type)
    if (request.type == null) {
        errors.add(ValidationError("type", "type is required"))
    } else if (type == null) {
        errors.add(ValidationError("type", "type must be one of: deposit, withdrawal, transfer"))
    }

    if (request.status != null && parseStatusValue(request.status) == null) {
        errors.add(ValidationError("status", "status must be one of: pending, completed, failed"))
    }

    val from = request.fromAccount
    val to = request.toAccount
    if (type != null) {
        when (type) {
            TransactionType.TRANSFER -> {
                if (from.isNullOrBlank()) {
                    errors.add(ValidationError("fromAccount", "fromAccount is required for transfer"))
                }
                if (to.isNullOrBlank()) {
                    errors.add(ValidationError("toAccount", "toAccount is required for transfer"))
                }
            }

            TransactionType.DEPOSIT -> {
                if (to.isNullOrBlank()) {
                    errors.add(ValidationError("toAccount", "toAccount is required for deposit"))
                }
            }

            TransactionType.WITHDRAWAL -> {
                if (from.isNullOrBlank()) {
                    errors.add(ValidationError("fromAccount", "fromAccount is required for withdrawal"))
                }
            }
        }
    }

    if (!from.isNullOrBlank() && !isValidAccount(from)) {
        errors.add(
            ValidationError(
                "fromAccount",
                "fromAccount must follow ACC-XXXXX format with alphanumeric suffix"
            )
        )
    }
    if (!to.isNullOrBlank() && !isValidAccount(to)) {
        errors.add(
            ValidationError(
                "toAccount",
                "toAccount must follow ACC-XXXXX format with alphanumeric suffix"
            )
        )
    }

    return errors
}

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

private fun parseTypeValue(raw: String?): TransactionType? {
    if (raw == null) return null
    return runCatching { TransactionType.valueOf(raw.uppercase()) }.getOrNull()
}

private fun parseStatusValue(raw: String?): TransactionStatus? {
    if (raw == null) return null
    return runCatching { TransactionStatus.valueOf(raw.uppercase()) }.getOrNull()
}

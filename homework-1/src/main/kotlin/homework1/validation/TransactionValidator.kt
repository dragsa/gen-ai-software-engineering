package homework1.validation

import homework1.models.CreateTransactionCommand
import homework1.models.CreateTransactionRequest
import homework1.models.TransactionFilter
import homework1.models.ValidationError
import homework1.utils.isValidAccount
import homework1.utils.parseIsoDate
import homework1.utils.parseTransactionStatus
import homework1.utils.parseTransactionType
import java.math.BigDecimal
import java.util.Currency

data class FilterValidationResult(
    val filter: TransactionFilter = TransactionFilter(),
    val errors: List<ValidationError> = emptyList()
)

class TransactionValidator {
    private val validCurrencyCodes = Currency.getAvailableCurrencies().map { it.currencyCode }.toSet()

    fun validateCreateRequest(request: CreateTransactionRequest): List<ValidationError> {
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

        val type = parseTransactionType(request.type)
        if (request.type == null) {
            errors.add(ValidationError("type", "type is required"))
        } else if (type == null) {
            errors.add(ValidationError("type", "type must be one of: deposit, withdrawal, transfer"))
        }

        if (request.status != null && parseTransactionStatus(request.status) == null) {
            errors.add(ValidationError("status", "status must be one of: pending, completed, failed"))
        }

        val from = request.fromAccount
        val to = request.toAccount
        if (type != null) {
            when (type) {
                homework1.models.TransactionType.TRANSFER -> {
                    if (from.isNullOrBlank()) {
                        errors.add(ValidationError("fromAccount", "fromAccount is required for transfer"))
                    }
                    if (to.isNullOrBlank()) {
                        errors.add(ValidationError("toAccount", "toAccount is required for transfer"))
                    }
                }

                homework1.models.TransactionType.DEPOSIT -> {
                    if (to.isNullOrBlank()) {
                        errors.add(ValidationError("toAccount", "toAccount is required for deposit"))
                    }
                }

                homework1.models.TransactionType.WITHDRAWAL -> {
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

    fun toCreateCommand(request: CreateTransactionRequest): CreateTransactionCommand =
        CreateTransactionCommand(
            fromAccount = request.fromAccount,
            toAccount = request.toAccount,
            amount = request.amount!!,
            currency = request.currency!!.uppercase(),
            type = parseTransactionType(request.type)!!,
            status = parseTransactionStatus(request.status) ?: homework1.models.TransactionStatus.COMPLETED
        )

    fun validateFilters(
        accountId: String?,
        typeRaw: String?,
        fromRaw: String?,
        toRaw: String?
    ): FilterValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (accountId != null && !isValidAccount(accountId)) {
            errors.add(
                ValidationError(
                    "accountId",
                    "Account must follow ACC-XXXXX format with alphanumeric suffix"
                )
            )
        }

        val type = parseTransactionType(typeRaw)
        if (typeRaw != null && type == null) {
            errors.add(ValidationError("type", "type must be one of: deposit, withdrawal, transfer"))
        }

        val from = parseIsoDate(fromRaw)
        if (fromRaw != null && from == null) {
            errors.add(ValidationError("from", "from must follow ISO date format YYYY-MM-DD"))
        }

        val to = parseIsoDate(toRaw)
        if (toRaw != null && to == null) {
            errors.add(ValidationError("to", "to must follow ISO date format YYYY-MM-DD"))
        }

        if (from != null && to != null && from > to) {
            errors.add(ValidationError("from", "from must be less than or equal to to"))
        }

        return FilterValidationResult(
            filter = TransactionFilter(accountId = accountId, type = type, from = from, to = to),
            errors = errors
        )
    }

    fun validateAccountId(accountId: String, field: String = "accountId"): List<ValidationError> {
        if (isValidAccount(accountId)) return emptyList()
        return listOf(
            ValidationError(
                field,
                "Account must follow ACC-XXXXX format with alphanumeric suffix"
            )
        )
    }
}

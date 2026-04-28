package homework1.validation

import homework1.models.CreateTransactionCommand
import homework1.models.CreateTransactionRequest
import homework1.models.DepositCommand
import homework1.models.TransactionFilter
import homework1.models.TransactionType
import homework1.models.TransferCommand
import homework1.models.ValidationError
import homework1.models.WithdrawalCommand
import homework1.utils.isValidAccount
import homework1.utils.parseIsoDate
import homework1.utils.parseTransactionType
import java.math.BigDecimal

data class FilterValidationResult(
    val filter: TransactionFilter = TransactionFilter(),
    val errors: List<ValidationError> = emptyList()
)

class TransactionValidator {
    fun validateCreateRequest(request: CreateTransactionRequest): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (request.amount <= BigDecimal.ZERO) {
            errors.add(ValidationError("amount", "Amount must be a positive number"))
        } else {
            val scale = request.amount.stripTrailingZeros().scale()
            if (scale > 2) {
                errors.add(ValidationError("amount", "Amount must have at most 2 decimal places"))
            }
        }

        val type = parseTransactionType(request.type)
        if (type == null) {
            errors.add(ValidationError("type", "type must be one of: deposit, withdrawal, transfer"))
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

    fun toCreateCommand(request: CreateTransactionRequest): CreateTransactionCommand =
        when (parseTransactionType(request.type)!!) {
            TransactionType.DEPOSIT -> DepositCommand(
                toAccount = request.toAccount!!,
                amount = request.amount,
                currency = request.currency
            )

            TransactionType.WITHDRAWAL -> WithdrawalCommand(
                fromAccount = request.fromAccount!!,
                amount = request.amount,
                currency = request.currency
            )

            TransactionType.TRANSFER -> TransferCommand(
                fromAccount = request.fromAccount!!,
                toAccount = request.toAccount!!,
                amount = request.amount,
                currency = request.currency
            )
        }

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

package homework1.routing

import homework1.models.CreateTransactionRequest
import homework1.models.ErrorResponse
import homework1.models.ValidationError
import homework1.service.TransactionService
import homework1.validation.TransactionValidator
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.registerTransactionRoutes(
    transactionService: TransactionService,
    transactionValidator: TransactionValidator
) {
    route("/transactions") {
        post {
            val request = try {
                call.receive<CreateTransactionRequest>()
            } catch (_: Exception) {
                call.respondValidation(
                    listOf(ValidationError("body", "Request body must be valid JSON"))
                )
                return@post
            }

            val errors = transactionValidator.validateCreateRequest(request)
            if (errors.isNotEmpty()) {
                call.respondValidation(errors)
                return@post
            }

            val created = transactionService.createTransaction(
                transactionValidator.toCreateCommand(request)
            )
            call.respond(HttpStatusCode.Created, created)
        }

        get {
            val accountId = call.request.queryParameters["accountId"]
            val type = call.request.queryParameters["type"]
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]

            val validationResult = transactionValidator.validateFilters(accountId, type, from, to)
            if (validationResult.errors.isNotEmpty()) {
                call.respondValidation(validationResult.errors)
                return@get
            }

            val filtered = transactionService.listTransactions(validationResult.filter)
            call.respond(HttpStatusCode.OK, filtered)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Transaction id is required"))
                return@get
            }

            val transaction = transactionService.getTransactionById(id)
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

        val accountErrors = transactionValidator.validateAccountId(accountId)
        if (accountErrors.isNotEmpty()) {
            call.respondValidation(accountErrors)
            return@get
        }

        val balance = transactionService.getAccountBalance(accountId)
        call.respond(HttpStatusCode.OK, balance)
    }
}

private suspend fun ApplicationCall.respondValidation(errors: List<ValidationError>) {
    respond(HttpStatusCode.BadRequest, ErrorResponse("Validation failed", errors))
}

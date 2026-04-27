package homework1.entrypoint

import homework1.routing.registerHealthRoutes
import homework1.routing.registerTransactionRoutes
import homework1.service.InMemoryTransactionService
import homework1.service.TransactionService
import homework1.validation.TransactionValidator
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun Application.module() {
    module(
        transactionService = InMemoryTransactionService(),
        transactionValidator = TransactionValidator()
    )
}

fun Application.module(
    transactionService: TransactionService,
    transactionValidator: TransactionValidator
) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        registerHealthRoutes()
        registerTransactionRoutes(transactionService, transactionValidator)
    }
}

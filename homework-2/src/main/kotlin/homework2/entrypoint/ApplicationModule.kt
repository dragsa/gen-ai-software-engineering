package homework2.entrypoint

import homework2.routing.registerDocumentationRoutes
import homework2.routing.registerTicketRoutes
import homework2.service.InMemoryTicketRepository
import homework2.service.TicketService
import homework2.service.TicketServiceImpl
import homework2.validation.TicketValidator
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

/**
 * Default wiring used at runtime.
 * Constructs production collaborators and delegates to the parameterised overload.
 */
fun Application.module() {
    val repository = InMemoryTicketRepository()
    val validator  = TicketValidator()
    val service    = TicketServiceImpl(repository, validator)
    module(service, validator)
}

/**
 * Parameterised overload used in tests.
 * Accepts collaborators so tests can substitute fakes without rewiring the application.
 */
fun Application.module(
    service: TicketService,
    validator: TicketValidator
) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        registerDocumentationRoutes()
        registerTicketRoutes(service, validator)
    }
}

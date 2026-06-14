package homework4.entrypoint

import homework4.routing.registerDocumentationRoutes
import homework4.routing.registerSnippetRoutes
import homework4.service.InMemorySnippetService
import homework4.service.SnippetService
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
    module(InMemorySnippetService())
}

/**
 * Parameterised overload used in tests.
 * Accepts collaborators so tests can substitute fakes without rewiring the application.
 */
fun Application.module(service: SnippetService) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        registerDocumentationRoutes()
        registerSnippetRoutes(service)
    }
}

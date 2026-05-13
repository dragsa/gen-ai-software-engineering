package homework2.testsupport

import homework2.entrypoint.module
import homework2.service.InMemoryTicketRepository
import homework2.service.TicketClassifier
import homework2.service.TicketServiceImpl
import homework2.validation.TicketValidator
import io.ktor.server.testing.ApplicationTestBuilder

/**
 * Creates a [testApplication] setup block with injected collaborators.
 *
 * Every API test calls this instead of duplicating application wiring.
 * Callers may inject custom fakes for the repository, validator, or classifier.
 * Defaults create fresh production-equivalent instances so most tests need no arguments.
 */
fun testApp(
    repository: InMemoryTicketRepository = InMemoryTicketRepository(),
    validator:  TicketValidator          = TicketValidator(),
    classifier: TicketClassifier         = TicketClassifier()
): ApplicationTestBuilder.() -> Unit = {
    application {
        module(
            service   = TicketServiceImpl(repository, validator, classifier),
            validator = validator
        )
    }
}

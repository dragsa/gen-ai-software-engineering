package homework4.routing

import homework4.models.CreateSnippetRequest
import homework4.models.ErrorResponse
import homework4.models.ValidationError
import homework4.models.toResponse
import homework4.service.SnippetService
import homework4.utils.TokenAuth
import homework4.validation.SnippetValidator
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

private const val API_TOKEN_HEADER = "X-Api-Token"

fun Route.registerSnippetRoutes(service: SnippetService) {
    // Create a snippet (requires authorization).
    post("/snippets") {
        if (!TokenAuth.isAuthorized(call.request.headers[API_TOKEN_HEADER])) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(listOf(ValidationError("authorization", "invalid or missing API token"))),
            )
            return@post
        }

        val request = call.receive<CreateSnippetRequest>()
        val errors = SnippetValidator.validate(request)
        if (errors.isNotEmpty()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(errors))
            return@post
        }

        val created = service.create(request.title, request.content)
        call.respond(HttpStatusCode.Created, created.toResponse())
    }

    // Retrieve a snippet by id.
    get("/snippets/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(listOf(ValidationError("id", "id must be an integer"))),
            )
            return@get
        }

        val snippet = service.get(id)
        if (snippet == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(listOf(ValidationError("id", "snippet not found"))),
            )
            return@get
        }

        call.respond(HttpStatusCode.OK, snippet.toResponse())
    }

    // Search snippets by title.
    get("/snippets") {
        val query = call.request.queryParameters["q"].orEmpty()
        val results = service.search(query).map { it.toResponse() }
        call.respond(HttpStatusCode.OK, results)
    }
}

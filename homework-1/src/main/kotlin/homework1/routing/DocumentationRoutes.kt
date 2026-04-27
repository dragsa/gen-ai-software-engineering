package homework1.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.plugins.swagger.swaggerUI

fun Route.registerDocumentationRoutes() {
    swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")

    get("/openapi.yaml") {
        val spec = object {}.javaClass.classLoader.getResourceAsStream("openapi.yaml")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: run {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "OpenAPI specification not found"))
                return@get
            }

        call.respondText(spec, ContentType.parse("application/yaml"))
    }
}

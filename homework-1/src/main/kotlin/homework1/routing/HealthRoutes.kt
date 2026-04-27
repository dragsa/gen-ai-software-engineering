package homework1.routing

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.registerHealthRoutes() {
    get("/") {
        call.respondText("Hello World")
    }
}

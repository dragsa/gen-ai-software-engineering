package homework2.routing

import homework2.models.Category
import homework2.models.ClassificationResponse
import homework2.models.ErrorResponse
import homework2.models.Priority
import homework2.models.Status
import homework2.models.CreateTicketRequest
import homework2.models.TicketFilter
import homework2.models.TicketResponse
import homework2.models.UpdateTicketRequest
import homework2.models.ValidationError
import homework2.service.TicketService
import homework2.utils.parsers.CsvTicketParser
import homework2.utils.parsers.JsonTicketParser
import homework2.utils.parsers.XmlTicketParser
import homework2.validation.TicketValidator
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.registerTicketRoutes(
    service: TicketService,
    validator: TicketValidator
) {
    route("/tickets") {

        // POST /tickets — create a single ticket
        post {
            val req = try {
                call.receive<CreateTicketRequest>()
            } catch (e: Exception) {
                call.respondValidation(
                    listOf(ValidationError("body", "Request body must be valid JSON: ${e.message}"))
                )
                return@post
            }

            val errors = validator.validateCreate(req)
            if (errors.isNotEmpty()) {
                call.respondValidation(errors)
                return@post
            }

            val ticket = service.createTicket(req)

            val autoClassify = call.request.queryParameters["auto_classify"]
                ?.toBooleanStrictOrNull() ?: false
            if (autoClassify) service.classifyTicket(ticket.id)

            call.respond(HttpStatusCode.Created, TicketResponse.from(ticket))
        }

        // GET /tickets — list tickets with optional filters
        get {
            val params = call.request.queryParameters
            val errors = mutableListOf<ValidationError>()

            val category = params["category"]?.let {
                Category.fromValue(it) ?: run {
                    errors += ValidationError("category", "category must be one of: ${Category.entries.joinToString { e -> e.value }}")
                    null
                }
            }
            val priority = params["priority"]?.let {
                Priority.fromValue(it) ?: run {
                    errors += ValidationError("priority", "priority must be one of: ${Priority.entries.joinToString { e -> e.value }}")
                    null
                }
            }
            val status = params["status"]?.let {
                Status.fromValue(it) ?: run {
                    errors += ValidationError("status", "status must be one of: ${Status.entries.joinToString { e -> e.value }}")
                    null
                }
            }

            if (errors.isNotEmpty()) {
                call.respondValidation(errors)
                return@get
            }

            val filter = TicketFilter(
                category   = category,
                priority   = priority,
                status     = status,
                assignedTo = params["assigned_to"],
                customerId = params["customer_id"],
                search     = params["search"]
            )

            val tickets = service.listTickets(filter).map { TicketResponse.from(it) }
            call.respond(HttpStatusCode.OK, tickets)
        }

        // POST /tickets/import — bulk import from CSV, JSON, or XML
        post("/import") {
            val multipart = call.receiveMultipart()

            var fileBytes: ByteArray? = null
            var fileName: String? = null
            var partContentType: String? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem && fileBytes == null) {
                    fileName = part.originalFileName
                    partContentType = part.contentType?.toString()
                    @Suppress("DEPRECATION")
                    fileBytes = part.streamProvider().use { it.readBytes() }
                }
                part.dispose()
            }

            val capturedBytes = fileBytes ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("No file part found in multipart request")
                )
                return@post
            }

            val format = detectFormat(partContentType, fileName) ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Cannot determine file format. Provide Content-Type or use a .csv/.json/.xml filename")
                )
                return@post
            }

            val content = capturedBytes.toString(Charsets.UTF_8)

            val parsedRows = try {
                when (format) {
                    Format.CSV  -> CsvTicketParser.parse(content)
                    Format.JSON -> JsonTicketParser.parse(content)
                    Format.XML  -> XmlTicketParser.parse(content)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Failed to parse file: ${e.message}")
                )
                return@post
            }

            val summary = service.bulkImport(parsedRows)
            call.respond(HttpStatusCode.OK, summary)
        }

        // GET /tickets/{id}
        get("/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ticket id is required"))
                return@get
            }

            val ticket = service.getTicket(id) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Ticket not found"))
                return@get
            }

            call.respond(HttpStatusCode.OK, TicketResponse.from(ticket))
        }

        // PUT /tickets/{id}
        put("/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ticket id is required"))
                return@put
            }

            val req = try {
                call.receive<UpdateTicketRequest>()
            } catch (e: Exception) {
                call.respondValidation(
                    listOf(ValidationError("body", "Request body must be valid JSON: ${e.message}"))
                )
                return@put
            }

            val errors = validator.validateUpdate(req)
            if (errors.isNotEmpty()) {
                call.respondValidation(errors)
                return@put
            }

            val ticket = service.updateTicket(id, req) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Ticket not found"))
                return@put
            }

            call.respond(HttpStatusCode.OK, TicketResponse.from(ticket))
        }

        // DELETE /tickets/{id}
        delete("/{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ticket id is required"))
                return@delete
            }

            val deleted = service.deleteTicket(id)
            if (!deleted) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Ticket not found"))
                return@delete
            }

            call.respond(HttpStatusCode.NoContent)
        }

        // POST /tickets/{id}/auto-classify
        post("/{id}/auto-classify") {
            val id = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Ticket id is required"))
                return@post
            }

            val decision = service.classifyTicket(id) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Ticket not found"))
                return@post
            }

            call.respond(HttpStatusCode.OK, ClassificationResponse.from(decision))
        }
    }
}

// --- private helpers ---

private enum class Format { CSV, JSON, XML }

private fun detectFormat(contentType: String?, fileName: String?): Format? {
    contentType?.let { ct ->
        return when {
            ct.contains("text/csv", ignoreCase = true)         -> Format.CSV
            ct.contains("application/csv", ignoreCase = true)  -> Format.CSV
            ct.contains("application/json", ignoreCase = true) -> Format.JSON
            ct.contains("application/xml", ignoreCase = true)  -> Format.XML
            ct.contains("text/xml", ignoreCase = true)         -> Format.XML
            else -> null
        }
    }
    return fileName?.let { name ->
        when {
            name.endsWith(".csv",  ignoreCase = true) -> Format.CSV
            name.endsWith(".json", ignoreCase = true) -> Format.JSON
            name.endsWith(".xml",  ignoreCase = true) -> Format.XML
            else -> null
        }
    }
}

private suspend fun ApplicationCall.respondValidation(errors: List<ValidationError>) =
    respond(HttpStatusCode.BadRequest, ErrorResponse("Validation failed", errors))

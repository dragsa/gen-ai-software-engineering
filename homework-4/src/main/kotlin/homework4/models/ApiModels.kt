package homework4.models

import kotlinx.serialization.Serializable

/** Request DTO for creating a snippet. Shaped to the HTTP contract. */
@Serializable
data class CreateSnippetRequest(
    val title: String,
    val content: String,
)

/** Response DTO returned to clients. */
@Serializable
data class SnippetResponse(
    val id: Int,
    val title: String,
    val content: String,
)

/** A single validation failure. */
@Serializable
data class ValidationError(
    val field: String,
    val message: String,
)

/** Single, consistent error response shape used across the subproject. */
@Serializable
data class ErrorResponse(
    val errors: List<ValidationError>,
)

fun Snippet.toResponse(): SnippetResponse = SnippetResponse(id = id, title = title, content = content)

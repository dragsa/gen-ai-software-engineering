package homework4.models

/**
 * Domain model representing a stored code/text snippet.
 * Internal business state — never exposed directly over HTTP.
 */
data class Snippet(
    val id: Int,
    val title: String,
    val content: String,
)

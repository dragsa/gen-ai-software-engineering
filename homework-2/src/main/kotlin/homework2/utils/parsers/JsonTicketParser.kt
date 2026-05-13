package homework2.utils.parsers

import homework2.models.CreateTicketRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

/**
 * Parses a JSON string into a list of [ParsedRow] results.
 *
 * Accepts either:
 *   - A JSON array:  [{...}, {...}]  — each element decoded individually.
 *   - A JSON object: {...}           — treated as a single-element import.
 *
 * Each element is decoded inside a try/catch so one malformed element does not
 * abort the rest of the import.
 *
 * Row numbers are 1-based index into the array (or 1 for a single object).
 */
object JsonTicketParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parse(content: String): List<ParsedRow> {
        val root = try {
            json.parseToJsonElement(content)
        } catch (e: SerializationException) {
            return listOf(ParsedRow.Failure(0, "Invalid JSON: ${e.message}", null))
        } catch (e: Exception) {
            return listOf(ParsedRow.Failure(0, "Failed to parse JSON: ${e.message}", null))
        }

        val elements = when (root) {
            is JsonArray  -> root.jsonArray.toList()
            is JsonObject -> listOf(root)
            else          -> return listOf(ParsedRow.Failure(0, "JSON must be an object or array of objects", null))
        }

        return elements.mapIndexed { index, element ->
            val row = index + 1
            try {
                val request = json.decodeFromJsonElement<CreateTicketRequest>(element)
                ParsedRow.Success(row, request)
            } catch (e: SerializationException) {
                ParsedRow.Failure(row, "Failed to decode element: ${e.message}", element.toString())
            } catch (e: Exception) {
                ParsedRow.Failure(row, "Unexpected error on element: ${e.message}", element.toString())
            }
        }
    }
}

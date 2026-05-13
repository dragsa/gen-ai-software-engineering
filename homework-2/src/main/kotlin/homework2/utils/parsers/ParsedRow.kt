package homework2.utils.parsers

import homework2.models.CreateTicketRequest

/**
 * Outcome of parsing a single row or element from an imported file.
 *
 * [row] is always 1-based. Parsers produce a [List<ParsedRow>] so the service
 * can process successes and failures uniformly without aborting the whole import
 * on a single bad record.
 */
sealed class ParsedRow {

    /** The record was parsed into a valid-structure request ready for business validation. */
    data class Success(
        val row: Int,
        val request: CreateTicketRequest
    ) : ParsedRow()

    /** The record could not be parsed at all (structural error, missing columns, etc.). */
    data class Failure(
        val row: Int,
        val error: String,
        val rawData: String? = null
    ) : ParsedRow()
}

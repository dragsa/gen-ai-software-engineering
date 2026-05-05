package homework2.utils.parsers

import homework2.models.CreateMetadataRequest
import homework2.models.CreateTicketRequest
import org.apache.commons.csv.CSVFormat
import java.io.StringReader

/**
 * Parses a CSV string into a list of [ParsedRow] results.
 *
 * Expected header columns (case-sensitive):
 *   customer_id, customer_email, customer_name, subject, description,
 *   category, priority, status, assigned_to, tags,
 *   metadata_source, metadata_browser, metadata_device_type
 *
 * Required columns: customer_id, customer_email, customer_name, subject, description.
 * All other columns are optional and default to null / empty when absent.
 *
 * tags column: comma-separated values within the cell (e.g. "billing,urgent").
 * metadata_* columns: mapped to [CreateMetadataRequest] only when metadata_source is present and non-blank.
 */
object CsvTicketParser {

    private val REQUIRED_COLUMNS = setOf(
        "customer_id", "customer_email", "customer_name", "subject", "description"
    )

    fun parse(content: String): List<ParsedRow> {
        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build()

        val records = try {
            format.parse(StringReader(content)).records
        } catch (e: Exception) {
            return listOf(ParsedRow.Failure(0, "Failed to parse CSV: ${e.message}", null))
        }

        if (records.isEmpty()) return emptyList()

        val headerNames = records.first().parser.headerNames.toSet()
        val missingRequired = REQUIRED_COLUMNS - headerNames
        if (missingRequired.isNotEmpty()) {
            return listOf(
                ParsedRow.Failure(
                    0,
                    "CSV is missing required columns: ${missingRequired.joinToString()}",
                    null
                )
            )
        }

        return records.mapIndexed { index, record ->
            val row = index + 1
            try {
                val tags = record.isMapped("tags")
                    .takeIf { it }
                    ?.let { record.get("tags") }
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

                val metadataSource = record.isMapped("metadata_source")
                    .takeIf { it }
                    ?.let { record.get("metadata_source") }
                    ?.takeIf { it.isNotBlank() }

                val metadata = metadataSource?.let {
                    CreateMetadataRequest(
                        source = it,
                        browser = record.isMapped("metadata_browser")
                            .takeIf { mapped -> mapped }
                            ?.let { record.get("metadata_browser") }
                            ?.takeIf { v -> v.isNotBlank() },
                        deviceType = record.isMapped("metadata_device_type")
                            .takeIf { mapped -> mapped }
                            ?.let { record.get("metadata_device_type") }
                            ?.takeIf { v -> v.isNotBlank() }
                    )
                }

                ParsedRow.Success(
                    row = row,
                    request = CreateTicketRequest(
                        customerId    = record.get("customer_id"),
                        customerEmail = record.get("customer_email"),
                        customerName  = record.get("customer_name"),
                        subject       = record.get("subject"),
                        description   = record.get("description"),
                        category      = record.isMapped("category")
                            .takeIf { it }?.let { record.get("category") }?.takeIf { it.isNotBlank() },
                        priority      = record.isMapped("priority")
                            .takeIf { it }?.let { record.get("priority") }?.takeIf { it.isNotBlank() },
                        status        = record.isMapped("status")
                            .takeIf { it }?.let { record.get("status") }?.takeIf { it.isNotBlank() },
                        assignedTo    = record.isMapped("assigned_to")
                            .takeIf { it }?.let { record.get("assigned_to") }?.takeIf { it.isNotBlank() },
                        tags          = tags,
                        metadata      = metadata
                    )
                )
            } catch (e: Exception) {
                ParsedRow.Failure(row, "Failed to parse row: ${e.message}", record.toString())
            }
        }
    }
}

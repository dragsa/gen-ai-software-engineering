package homework6.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Aggregated result of a pipeline run, produced by [ReportingAgent.summarize].
 * Per-currency totals are stored as strings to preserve BigDecimal precision in JSON.
 */
@Serializable
data class PipelineSummary(
    val total: Int,
    @SerialName("by_status") val byStatus: Map<String, Int>,
    @SerialName("settled_totals_by_currency") val settledTotalsByCurrency: Map<String, String>,
    @SerialName("ctr_over_10k") val ctrOver10k: List<String>,
    @SerialName("cross_border") val crossBorder: List<String>,
) {
    /** Human-readable rendering used for stdout and the `pipeline://summary` MCP resource. */
    fun toText(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Pipeline Summary ===")
        sb.appendLine("Total transactions: $total")
        sb.appendLine("By status:")
        byStatus.toSortedMap().forEach { (status, count) -> sb.appendLine("  - $status: $count") }
        sb.appendLine("Settled totals by currency:")
        if (settledTotalsByCurrency.isEmpty()) {
            sb.appendLine("  - (none)")
        } else {
            settledTotalsByCurrency.toSortedMap().forEach { (cur, sum) -> sb.appendLine("  - $cur: $sum") }
        }
        sb.appendLine("CTR (>= 10,000): ${if (ctrOver10k.isEmpty()) "(none)" else ctrOver10k.joinToString(", ")}")
        sb.appendLine("Cross-border: ${if (crossBorder.isEmpty()) "(none)" else crossBorder.joinToString(", ")}")
        return sb.toString().trimEnd()
    }
}

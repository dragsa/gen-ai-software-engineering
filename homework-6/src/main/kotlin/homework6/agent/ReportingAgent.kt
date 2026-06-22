package homework6.agent

import homework6.common.AgentMessage
import homework6.common.AuditLogger
import homework6.common.MessageIo
import homework6.common.SharedDirs

import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Runtime Agent 3. Aggregates the terminal messages in shared/results/ into a [PipelineSummary].
 * [summarize] is a pure function over the collected results (unit-tested); [main] reads the live
 * results directory and writes the summary files consumed by the MCP `pipeline://summary` resource.
 */
object ReportingAgent {

    private val CTR_THRESHOLD = BigDecimal("10000.00")
    private const val HOME_COUNTRY = "US"

    fun summarize(results: List<AgentMessage>): PipelineSummary {
        val byStatus = results
            .groupingBy { it.data.status ?: "unknown" }
            .eachCount()

        val settledTotals = LinkedHashMap<String, BigDecimal>()
        for (message in results) {
            val data = message.data
            if (data.status == "settled" && data.amount != null && data.currency != null) {
                settledTotals[data.currency] =
                    (settledTotals[data.currency] ?: BigDecimal.ZERO).add(data.amount)
            }
        }

        val ctrOver10k = results
            .filter { it.data.amount != null && it.data.amount >= CTR_THRESHOLD }
            .map { it.data.transactionId }
            .sorted()

        val crossBorder = results
            .filter { val c = it.data.metadata?.country; !c.isNullOrBlank() && c != HOME_COUNTRY }
            .map { it.data.transactionId }
            .sorted()

        return PipelineSummary(
            total = results.size,
            byStatus = byStatus,
            settledTotalsByCurrency = settledTotals.mapValues { it.value.toPlainString() },
            ctrOver10k = ctrOver10k,
            crossBorder = crossBorder,
        )
    }

    /** Writes pipeline-summary.json and pipeline-summary.txt into shared/results/. */
    fun writeSummary(dirs: SharedDirs, summary: PipelineSummary) {
        Files.createDirectories(dirs.results)
        Files.writeString(
            dirs.results.resolve("pipeline-summary.json"),
            MessageIo.json.encodeToString(PipelineSummary.serializer(), summary),
        )
        Files.writeString(dirs.results.resolve("pipeline-summary.txt"), summary.toText())
    }
}

/** Standalone entrypoint: summarizes the current shared/results/ directory. */
fun main() {
    val dirs = SharedDirs.under(Paths.get("."))
    val results = MessageIo.listJson(dirs.results).map { MessageIo.read(it) }
    val summary = ReportingAgent.summarize(results)
    ReportingAgent.writeSummary(dirs, summary)
    AuditLogger().log("reporting_agent", "ALL", "summarized")
    println(summary.toText())
}

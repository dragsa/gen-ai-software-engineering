package homework6.agent

import homework6.common.AgentMessage
import homework6.common.AgentRunner
import homework6.common.AuditLogger
import homework6.common.MessageIo
import homework6.common.Samples
import homework6.common.SharedDirs

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

/**
 * Orchestrator. Sequences the three runtime agents in-process (default demo) while moving messages
 * through the shared/ directories. Implements no business rules itself.
 *
 * [run] is exposed as a plain function (not only inside main) so the integration test can drive it
 * against a temp `shared/` directory.
 */
object Integrator {

    fun run(base: Path, samplePath: Path, audit: AuditLogger = AuditLogger()): PipelineSummary {
        val dirs = SharedDirs.under(base)
        dirs.clearAndCreate()

        // 1. Seed shared/input/ with one message per raw transaction.
        for (data in Samples.load(samplePath)) {
            val message = AgentMessage(
                messageId = UUID.randomUUID().toString(),
                timestamp = Instant.now().toString(),
                sourceAgent = "integrator",
                targetAgent = "transaction_validator",
                messageType = "transaction",
                data = data.copy(status = "received"),
            )
            MessageIo.write(dirs.input, message)
        }

        // 2. Validator stage: input -> processing -> output (validated) / results (rejected).
        AgentRunner.runStage(dirs.input, dirs, audit, "transaction_validator", TransactionValidator::process)

        // 3. Fraud stage: output -> processing -> results (settled / flagged_for_review).
        AgentRunner.runStage(dirs.output, dirs, audit, "fraud_detector", FraudDetector::process)

        // 4. Reporting stage: aggregate all terminal results into the run summary.
        val results = MessageIo.listJson(dirs.results).map { MessageIo.read(it) }
        val summary = ReportingAgent.summarize(results)
        ReportingAgent.writeSummary(dirs, summary)
        audit.log("reporting_agent", "ALL", "summarized")

        return summary
    }
}

fun main() {
    val summary = Integrator.run(Paths.get("."), Paths.get("sample-transactions.json"))
    println()
    println(summary.toText())
}

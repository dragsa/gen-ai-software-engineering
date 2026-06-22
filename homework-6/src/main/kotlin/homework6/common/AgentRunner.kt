package homework6.common

import java.nio.file.Files
import java.nio.file.Path

/**
 * Drives one agent stage over a file inbox using the claim-and-clear lifecycle:
 * for each message in [inbox], move it into `processing/`, transform it, write the result to the
 * next stage, then remove the claimed file from `processing/`.
 *
 * Routing: a `validated` result goes to `output/`; anything else (rejected, settled,
 * flagged_for_review) is terminal and goes to `results/`.
 */
object AgentRunner {
    fun runStage(
        inbox: Path,
        dirs: SharedDirs,
        audit: AuditLogger,
        agentName: String,
        transform: (AgentMessage) -> AgentMessage,
    ) {
        for (file in MessageIo.listJson(inbox)) {
            val claimed = MessageIo.claim(file, dirs.processing)
            val message = MessageIo.read(claimed)
            val result = transform(message)
            val targetDir = if (result.data.status == "validated") dirs.output else dirs.results
            MessageIo.write(targetDir, result)
            Files.deleteIfExists(claimed)
            audit.log(agentName, result.data.transactionId, result.data.status ?: "unknown")
        }
    }
}

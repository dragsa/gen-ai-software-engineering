package homework6.agent

import homework6.common.MessageIo
import homework6.common.SharedDirs
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegratorIntegrationTest {

    /** Self-contained fixture covering settle, flag (high-value + odd-hour cross-border), and both rejects. */
    private val fixture = """
        [
          {"transaction_id":"TXN001","timestamp":"2026-03-16T09:00:00Z","source_account":"ACC-1","destination_account":"ACC-2","amount":"1500.00","currency":"USD","transaction_type":"transfer","metadata":{"channel":"online","country":"US"}},
          {"transaction_id":"TXN002","timestamp":"2026-03-16T09:15:00Z","source_account":"ACC-1","destination_account":"ACC-3","amount":"25000.00","currency":"USD","transaction_type":"wire_transfer","metadata":{"channel":"branch","country":"US"}},
          {"transaction_id":"TXN004","timestamp":"2026-03-16T02:47:00Z","source_account":"ACC-1","destination_account":"ACC-5","amount":"500.00","currency":"EUR","transaction_type":"transfer","metadata":{"channel":"api","country":"DE"}},
          {"transaction_id":"TXN006","timestamp":"2026-03-16T10:05:00Z","source_account":"ACC-1","destination_account":"ACC-9","amount":"200.00","currency":"XYZ","transaction_type":"transfer","metadata":{"channel":"online","country":"US"}},
          {"transaction_id":"TXN007","timestamp":"2026-03-16T10:10:00Z","source_account":"ACC-1","destination_account":"ACC-8","amount":"-100.00","currency":"GBP","transaction_type":"refund","metadata":{"channel":"online","country":"GB"}}
        ]
    """.trimIndent()

    @Test
    fun fullPipelineProducesOneTerminalResultPerTransaction() {
        val base = createTempDirectory("hw6-int")
        val samplePath = base.resolve("sample-transactions.json")
        Files.writeString(samplePath, fixture)

        val summary = Integrator.run(base, samplePath)

        // Every input transaction has exactly one terminal record.
        val dirs = SharedDirs.under(base)
        val resultIds = MessageIo.listJson(dirs.results).map { MessageIo.read(it).data.transactionId }.sorted()
        assertEquals(listOf("TXN001", "TXN002", "TXN004", "TXN006", "TXN007"), resultIds)

        assertEquals(5, summary.total)
        assertEquals(1, summary.byStatus["settled"])             // TXN001
        assertEquals(2, summary.byStatus["flagged_for_review"])  // TXN002, TXN004
        assertEquals(2, summary.byStatus["rejected"])            // TXN006, TXN007
        assertEquals(listOf("TXN002"), summary.ctrOver10k)
        assertEquals(listOf("TXN004", "TXN007"), summary.crossBorder)
    }

    @Test
    fun processingIsEmptyAfterRun() {
        val base = createTempDirectory("hw6-int")
        val samplePath = base.resolve("sample-transactions.json")
        Files.writeString(samplePath, fixture)

        Integrator.run(base, samplePath)

        val processing = SharedDirs.under(base).processing
        val leftovers = MessageIo.listJson(processing)
        assertTrue(leftovers.isEmpty(), "processing/ must be empty after a run (claim-and-clear)")
    }

    @Test
    fun runPreservesGitkeepPlaceholders() {
        val base = createTempDirectory("hw6-int")
        val samplePath = base.resolve("sample-transactions.json")
        Files.writeString(samplePath, fixture)

        val dirs = SharedDirs.under(base)
        dirs.createAll()
        Files.writeString(dirs.input.resolve(".gitkeep"), "")

        Integrator.run(base, samplePath)

        assertTrue(dirs.input.resolve(".gitkeep").exists(), ".gitkeep must survive clearAndCreate")
    }
}

package homework6.agent

import homework6.common.SharedDirs
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportingAgentTest {

    private val results = listOf(
        txn(id = "TXN001", amount = "1500.00", currency = "USD", country = "US", status = "settled"),
        txn(id = "TXN002", amount = "25000.00", currency = "USD", country = "US", status = "flagged_for_review"),
        txn(id = "TXN003", amount = "9999.99", currency = "USD", country = "US", status = "settled"),
        txn(id = "TXN004", amount = "500.00", currency = "EUR", country = "DE", status = "flagged_for_review"),
        txn(id = "TXN006", amount = "200.00", currency = "XYZ", country = "US", status = "rejected"),
        txn(id = "TXN007", amount = "-100.00", currency = "GBP", country = "GB", status = "rejected"),
    )

    @Test
    fun summarizeCountsAndAggregatesCorrectly() {
        val s = ReportingAgent.summarize(results)
        assertEquals(6, s.total)
        assertEquals(mapOf("settled" to 2, "flagged_for_review" to 2, "rejected" to 2), s.byStatus)
        assertEquals("11499.99", s.settledTotalsByCurrency["USD"])  // 1500.00 + 9999.99, settled only
        assertEquals(listOf("TXN002"), s.ctrOver10k)                 // amount >= 10000
        assertEquals(listOf("TXN004", "TXN007"), s.crossBorder)      // country != US
    }

    @Test
    fun summaryTextRendersSections() {
        val text = ReportingAgent.summarize(results).toText()
        assertTrue(text.contains("Total transactions: 6"))
        assertTrue(text.contains("USD: 11499.99"))
        assertTrue(text.contains("CTR (>= 10,000): TXN002"))
        assertTrue(text.contains("Cross-border: TXN004, TXN007"))
    }

    @Test
    fun emptyResultsRenderNonePlaceholders() {
        val text = ReportingAgent.summarize(emptyList()).toText()
        assertTrue(text.contains("Total transactions: 0"))
        assertTrue(text.contains("(none)"))
    }

    @Test
    fun writeSummaryProducesJsonAndText() {
        val base = createTempDirectory("hw6-report")
        val dirs = SharedDirs.under(base)
        dirs.createAll()
        ReportingAgent.writeSummary(dirs, ReportingAgent.summarize(results))

        assertTrue(dirs.results.resolve("pipeline-summary.json").exists())
        val txt = dirs.results.resolve("pipeline-summary.txt")
        assertTrue(txt.exists())
        assertTrue(txt.readText().contains("Pipeline Summary"))
    }
}

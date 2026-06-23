package homework6.common

import java.math.BigDecimal
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageIoTest {

    private fun sampleMessage(id: String) = AgentMessage(
        messageId = "m-$id",
        timestamp = "2026-03-16T09:00:00Z",
        sourceAgent = "integrator",
        targetAgent = "transaction_validator",
        messageType = "transaction",
        data = TransactionData(
            transactionId = id,
            amount = BigDecimal("1500.00"),
            currency = "USD",
            status = "received",
        ),
    )

    @Test
    fun writeThenReadRoundTrips() {
        val dir = createTempDirectory("hw6-io")
        val written = MessageIo.write(dir, sampleMessage("TXN001"))
        assertEquals("TXN001.json", written.fileName.toString())

        val read = MessageIo.read(written)
        assertEquals("TXN001", read.data.transactionId)
        assertEquals(0, read.data.amount!!.compareTo(BigDecimal("1500.00")))
        assertEquals("USD", read.data.currency)
    }

    @Test
    fun claimMovesFileIntoProcessing() {
        val base = createTempDirectory("hw6-io")
        val dirs = SharedDirs.under(base)
        dirs.createAll()
        val src = MessageIo.write(dirs.input, sampleMessage("TXN002"))

        val claimed = MessageIo.claim(src, dirs.processing)

        assertFalse(src.exists(), "source file should be moved out of input")
        assertTrue(claimed.exists())
        assertEquals(dirs.processing, claimed.parent)
    }

    @Test
    fun listJsonExcludesSummaryAndNonJson() {
        val dir = createTempDirectory("hw6-io")
        MessageIo.write(dir, sampleMessage("TXN003"))
        Files.writeString(dir.resolve("pipeline-summary.json"), "{}")
        Files.writeString(dir.resolve(".gitkeep"), "")
        Files.writeString(dir.resolve("notes.txt"), "x")

        val names = MessageIo.listJson(dir).map { it.fileName.toString() }
        assertEquals(listOf("TXN003.json"), names)
    }
}

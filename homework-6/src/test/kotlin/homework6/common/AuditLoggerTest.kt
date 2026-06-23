package homework6.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditLoggerTest {

    @Test
    fun masksAccountKeepingPrefixAndLastDigit() {
        assertEquals("ACC-***1", AuditLogger.maskAccount("ACC-1001"))
    }

    @Test
    fun masksNullBlankAndDigitlessAccounts() {
        assertEquals("***", AuditLogger.maskAccount(null))
        assertEquals("***", AuditLogger.maskAccount("   "))
        assertEquals("***", AuditLogger.maskAccount("ABC"))
    }

    @Test
    fun keepsSingleDigitAccountAsIs() {
        assertEquals("ACC-7", AuditLogger.maskAccount("ACC-7"))
    }

    @Test
    fun logLineHasExpectedShape() {
        val lines = mutableListOf<String>()
        AuditLogger(sink = { lines.add(it) }).log("transaction_validator", "TXN001", "validated")
        assertEquals(1, lines.size)
        val parts = lines[0].split(" | ")
        assertEquals(4, parts.size)
        assertEquals("transaction_validator", parts[1])
        assertEquals("TXN001", parts[2])
        assertEquals("validated", parts[3])
        assertTrue(parts[0].isNotBlank())
    }
}

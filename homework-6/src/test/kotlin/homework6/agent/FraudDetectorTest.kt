package homework6.agent

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FraudDetectorTest {

    @Test
    fun highValueIsFlagged() {
        val out = FraudDetector.process(txn(id = "TXN002", amount = "25000.00"))
        assertEquals("flagged_for_review", out.data.status)
        assertContains(out.data.riskReasons, "high_value")
        assertTrue((out.data.riskScore ?: 0) >= 60)
    }

    @Test
    fun oddHourCrossBorderIsFlagged() {
        val out = FraudDetector.process(
            txn(id = "TXN004", amount = "500.00", currency = "EUR", country = "DE", timestamp = "2026-03-16T02:47:00Z"),
        )
        assertEquals("flagged_for_review", out.data.status)
        assertContains(out.data.riskReasons, "odd_hour")
        assertContains(out.data.riskReasons, "cross_border")
    }

    @Test
    fun oddHourAloneDoesNotFlag() {
        // Odd hour but domestic -> not flagged (rule requires odd_hour AND cross_border).
        val out = FraudDetector.process(txn(amount = "500.00", country = "US", timestamp = "2026-03-16T02:00:00Z"))
        assertEquals("settled", out.data.status)
    }

    @Test
    fun nearThresholdSettlesButIsNoted() {
        val out = FraudDetector.process(txn(id = "TXN003", amount = "9999.99"))
        assertEquals("settled", out.data.status)
        assertEquals(true, out.data.nearThreshold)
        assertContains(out.data.riskReasons, "near_threshold")
    }

    @Test
    fun ordinaryTransactionSettlesWithNoSignals() {
        val out = FraudDetector.process(txn(id = "TXN001", amount = "1500.00"))
        assertEquals("settled", out.data.status)
        assertEquals(0, out.data.riskScore)
        assertTrue(out.data.riskReasons.isEmpty())
        assertEquals(false, out.data.nearThreshold)
    }

    @Test
    fun isOddHourBoundaries() {
        assertTrue(FraudDetector.isOddHour("2026-03-16T00:00:00Z"))
        assertTrue(FraudDetector.isOddHour("2026-03-16T04:59:00Z"))
        assertFalse(FraudDetector.isOddHour("2026-03-16T05:00:00Z"))
        assertFalse(FraudDetector.isOddHour("2026-03-16T09:00:00Z"))
        assertFalse(FraudDetector.isOddHour(null))
        assertFalse(FraudDetector.isOddHour("not-a-timestamp"))
    }

    @Test
    fun isNearThresholdBoundaries() {
        assertTrue(FraudDetector.isNearThreshold(BigDecimal("9999.99")))
        assertTrue(FraudDetector.isNearThreshold(BigDecimal("9000.00")))
        assertFalse(FraudDetector.isNearThreshold(BigDecimal("8999.99")))
        assertFalse(FraudDetector.isNearThreshold(BigDecimal("10000.00")))
        assertFalse(FraudDetector.isNearThreshold(BigDecimal("25000.00")))
    }
}

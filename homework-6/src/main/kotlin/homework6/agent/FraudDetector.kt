package homework6.agent

import homework6.common.AgentMessage
import homework6.common.AgentRunner
import homework6.common.AuditLogger
import homework6.common.SharedDirs

import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset

/**
 * Runtime Agent 2. Scores a validated transaction 0-100 from weighted signals and decides
 * `flagged_for_review` vs `settled`. Pure [process] is callable from the integrator.
 */
object FraudDetector {

    private val HIGH_VALUE = BigDecimal("10000.00")
    private val NEAR_MIN = BigDecimal("0.01")
    private val NEAR_MAX = BigDecimal("1000.00")
    private const val HOME_COUNTRY = "US"

    fun process(message: AgentMessage): AgentMessage {
        val data = message.data
        val amount = data.amount ?: BigDecimal.ZERO

        val highValue = amount > HIGH_VALUE
        val oddHour = isOddHour(data.timestamp)
        val country = data.metadata?.country
        val crossBorder = !country.isNullOrBlank() && country != HOME_COUNTRY
        val nearThreshold = isNearThreshold(amount)

        val reasons = buildList {
            if (highValue) add("high_value")
            if (oddHour) add("odd_hour")
            if (crossBorder) add("cross_border")
            if (nearThreshold) add("near_threshold")
        }

        val flagged = highValue || (oddHour && crossBorder)
        val status = if (flagged) "flagged_for_review" else "settled"

        return message.copy(
            sourceAgent = "fraud_detector",
            targetAgent = "results",
            data = data.copy(
                status = status,
                riskScore = riskScore(highValue, oddHour, crossBorder, nearThreshold),
                riskReasons = reasons,
                nearThreshold = nearThreshold,
            ),
        )
    }

    /** Amount is within $0.01-$1,000.00 below the $10,000 reporting threshold (structuring signal). */
    fun isNearThreshold(amount: BigDecimal): Boolean {
        val diff = HIGH_VALUE.subtract(amount)
        return diff >= NEAR_MIN && diff <= NEAR_MAX
    }

    /** True when the UTC hour of the timestamp is in [0, 5). */
    fun isOddHour(timestamp: String?): Boolean {
        if (timestamp.isNullOrBlank()) return false
        return try {
            val hour = Instant.parse(timestamp).atZone(ZoneOffset.UTC).hour
            hour in 0 until 5
        } catch (_: Exception) {
            false
        }
    }

    private fun riskScore(highValue: Boolean, oddHour: Boolean, crossBorder: Boolean, near: Boolean): Int {
        var score = 0
        if (highValue) score += 60
        if (crossBorder) score += 25
        if (oddHour) score += 15
        if (near) score += 20
        return minOf(score, 100)
    }
}

/** Standalone entrypoint: drains shared/output/ (validated messages) into shared/results/. */
fun main() {
    val dirs = SharedDirs.under(Paths.get("."))
    dirs.createAll()
    AgentRunner.runStage(dirs.output, dirs, AuditLogger(), "fraud_detector", FraudDetector::process)
    println("FraudDetector: drained shared/output/")
}

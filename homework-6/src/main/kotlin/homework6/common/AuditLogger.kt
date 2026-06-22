package homework6.common

import java.time.Instant

/**
 * Emits one audit line per agent decision: `timestamp(ISO-8601) | agent | transaction_id | outcome`.
 * Account numbers are masked and customer names are never logged.
 */
class AuditLogger(private val sink: (String) -> Unit = ::println) {

    fun log(agent: String, transactionId: String, outcome: String) {
        sink("${Instant.now()} | $agent | $transactionId | $outcome")
    }

    companion object {
        /** Masks an account number keeping its non-digit prefix and only the last digit, e.g. `ACC-1001` -> `ACC-***1`. */
        fun maskAccount(account: String?): String {
            if (account.isNullOrBlank()) return "***"
            val firstDigit = account.indexOfFirst { it.isDigit() }
            if (firstDigit < 0) return "***"
            val prefix = account.substring(0, firstDigit)
            val digits = account.substring(firstDigit)
            if (digits.length <= 1) return "$prefix$digits"
            return prefix + "*".repeat(digits.length - 1) + digits.last()
        }
    }
}

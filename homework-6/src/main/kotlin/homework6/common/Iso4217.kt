package homework6.common

/** Minimal ISO 4217 currency allow-list used by [TransactionValidator]. */
object Iso4217 {
    val allowed: Set<String> = setOf(
        "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "SEK", "NOK", "NZD", "CNY",
    )

    fun isValid(code: String?): Boolean = code != null && code in allowed
}

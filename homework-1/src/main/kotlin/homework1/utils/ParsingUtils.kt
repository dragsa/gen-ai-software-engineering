package homework1.utils

import homework1.models.TransactionStatus
import homework1.models.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun parseTransactionType(raw: String?): TransactionType? {
    if (raw == null) return null
    return runCatching { TransactionType.valueOf(raw.uppercase()) }.getOrNull()
}

fun parseTransactionStatus(raw: String?): TransactionStatus? {
    if (raw == null) return null
    return runCatching { TransactionStatus.valueOf(raw.uppercase()) }.getOrNull()
}

fun parseIsoDate(raw: String?): LocalDate? {
    if (raw == null) return null
    return runCatching { LocalDate.parse(raw) }.getOrNull()
}

fun isoInstantToUtcDate(timestamp: String): LocalDate =
    Instant.parse(timestamp).atOffset(ZoneOffset.UTC).toLocalDate()

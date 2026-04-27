package homework1.utils

import homework1.models.TransactionStatus
import homework1.models.TransactionType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParsingUtilsTest {
    @Test
    fun `parse transaction type handles valid invalid and null values`() {
        assertEquals(TransactionType.DEPOSIT, parseTransactionType("deposit"))
        assertNull(parseTransactionType("unknown"))
        assertNull(parseTransactionType(null))
    }

    @Test
    fun `parse transaction status handles valid invalid and null values`() {
        assertEquals(TransactionStatus.COMPLETED, parseTransactionStatus("completed"))
        assertNull(parseTransactionStatus("wrong"))
        assertNull(parseTransactionStatus(null))
    }

    @Test
    fun `parse iso date handles valid invalid and null values`() {
        assertEquals(LocalDate.parse("2026-01-01"), parseIsoDate("2026-01-01"))
        assertNull(parseIsoDate("2026-13-40"))
        assertNull(parseIsoDate(null))
    }
}

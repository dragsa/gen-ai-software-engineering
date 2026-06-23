package homework6.common

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class BigDecimalSerializerTest {

    private val json = Json

    @Test
    fun encodesAsPlainJsonString() {
        val encoded = json.encodeToString(BigDecimalSerializer, BigDecimal("9999.99"))
        assertEquals("\"9999.99\"", encoded)
    }

    @Test
    fun decodesFromJsonStringPreservingScale() {
        val value = json.decodeFromString(BigDecimalSerializer, "\"1500.00\"")
        assertEquals(0, value.compareTo(BigDecimal("1500.00")))
        assertEquals("1500.00", value.toPlainString())
    }

    @Test
    fun roundTripIsStable() {
        val original = BigDecimal("25000.00")
        val restored = json.decodeFromString(
            BigDecimalSerializer,
            json.encodeToString(BigDecimalSerializer, original),
        )
        assertEquals(original.toPlainString(), restored.toPlainString())
    }
}

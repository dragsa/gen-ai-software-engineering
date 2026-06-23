package homework6.common

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Iso4217Test {

    @Test
    fun acceptsKnownCurrencies() {
        assertTrue(Iso4217.isValid("USD"))
        assertTrue(Iso4217.isValid("EUR"))
        assertTrue(Iso4217.isValid("GBP"))
    }

    @Test
    fun rejectsUnknownCurrency() {
        assertFalse(Iso4217.isValid("XYZ"))
    }

    @Test
    fun rejectsNullAndLowercase() {
        assertFalse(Iso4217.isValid(null))
        assertFalse(Iso4217.isValid("usd"))
    }
}

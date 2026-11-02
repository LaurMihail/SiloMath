package com.silomath.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.model.CanonicalDecimal
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfficialDecimalStorageAdapterTest {

    @Test
    fun encode_usesScaleSixHalfUpPlainText() {
        assertEquals(
            "123.456790",
            OfficialDecimalStorageAdapter.encode(BigDecimal("123.4567895"))
        )
        assertEquals(
            "0.000001",
            OfficialDecimalStorageAdapter.encode(BigDecimal("0.0000005"))
        )
        assertEquals(
            "1000000000000.000000",
            OfficialDecimalStorageAdapter.encode(BigDecimal("1E+12"))
        )
    }

    @Test
    fun decode_returnsCanonicalScaleSixDecimal() {
        val decoded = OfficialDecimalStorageAdapter.decode("42.5")
        assertEquals("42.500000", decoded.toPlainString())
    }

    @Test
    fun roomConvertersDelegateToCanonicalTextAdapter() {
        val converters = SiloMathConverters()
        val canonical = CanonicalDecimal.from("7.125")

        assertEquals("7.125000", converters.fromCanonicalDecimal(canonical))
        assertEquals(
            "7.125000",
            converters.toCanonicalDecimal("7.125")?.toPlainString()
        )
        assertEquals(
            "7.125000",
            converters.fromBigDecimal(BigDecimal("7.125"))
        )
    }
}

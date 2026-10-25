package com.silomath.core.database

import com.silomath.core.model.CanonicalDecimal
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Canonical Room/SQLite adapter for official V6 decimal values.
 *
 * Frozen physical contract: official decimals persist as TEXT at scale 6,
 * using ROUND_HALF_UP. SQLite REAL/Double is not an authoritative V6 storage
 * representation.
 */
object OfficialDecimalStorageAdapter {
    private const val SCALE = 6

    fun encode(value: BigDecimal): String =
        value.setScale(SCALE, RoundingMode.HALF_UP).toPlainString()

    fun encode(value: CanonicalDecimal): String = value.toPlainString()

    fun decode(value: String): CanonicalDecimal = CanonicalDecimal.from(value)

    fun decodeBigDecimal(value: String): BigDecimal =
        BigDecimal(value.trim()).setScale(SCALE, RoundingMode.HALF_UP)
}

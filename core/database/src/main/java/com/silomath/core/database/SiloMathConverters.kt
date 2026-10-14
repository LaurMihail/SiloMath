package com.silomath.core.database

import androidx.room.TypeConverter
import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.SyncOperationState
import java.math.BigDecimal

class SiloMathConverters {

    @TypeConverter
    fun fromSyncOperationState(state: SyncOperationState?): String? {
        return state?.name
    }

    @TypeConverter
    fun toSyncOperationState(state: String?): SyncOperationState? {
        return state?.let { SyncOperationState.valueOf(it) }
    }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.setScale(6, java.math.RoundingMode.HALF_UP)?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it).setScale(6, java.math.RoundingMode.HALF_UP) }
    }

    @TypeConverter
    fun fromCanonicalDecimal(decimal: CanonicalDecimal?): String? {
        return decimal?.toPlainString()
    }

    @TypeConverter
    fun toCanonicalDecimal(value: String?): CanonicalDecimal? {
        return value?.let { CanonicalDecimal.from(it) }
    }
}

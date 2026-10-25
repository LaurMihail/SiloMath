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
        return value?.let(OfficialDecimalStorageAdapter::encode)
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let(OfficialDecimalStorageAdapter::decodeBigDecimal)
    }

    @TypeConverter
    fun fromCanonicalDecimal(decimal: CanonicalDecimal?): String? {
        return decimal?.let(OfficialDecimalStorageAdapter::encode)
    }

    @TypeConverter
    fun toCanonicalDecimal(value: String?): CanonicalDecimal? {
        return value?.let(OfficialDecimalStorageAdapter::decode)
    }
}

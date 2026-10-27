package com.silomath.backend.calculation

import com.silomath.calculation.CalculationError
import com.silomath.calculation.CalculationFailure
import com.silomath.calculation.CanonicalCalculationInput
import com.silomath.calculation.ProductionCalculationRegistry
import com.silomath.calculation.VersionedCalculationResult
import java.math.BigDecimal

/**
 * Doc13 verification over server-supplied FROZEN authoritative inputs. Transport/auth/snapshot
 * loaders must supply that context; this service does not treat client input as authoritative.
 * No endpoint or workflow state is created here.
 */
class AuthoritativeCalculationVerifier {
    fun verify(authoritativeFrozenInput: CanonicalCalculationInput,clientFingerprint: String,
        clientProductVolumeM3: BigDecimal): VersionedCalculationResult {
        val calculated=ProductionCalculationRegistry.calculate(authoritativeFrozenInput)
        if(clientFingerprint != calculated.normalizedInputFingerprint) throw CalculationFailure(CalculationError.GEOM_STALE_RESULT)
        if(clientProductVolumeM3.signum() < 0) throw CalculationFailure(CalculationError.GEOM_INVALID_PARAMETER)
        val officialVolume=calculated.numerical.productVolumeM3.toPersistenceDecimal()
        if((officialVolume-clientProductVolumeM3).abs() > VOLUME_VERIFICATION_TOLERANCE)
            throw CalculationFailure(CalculationError.GEOM_CALCULATION_MISMATCH)
        return calculated
    }
    companion object {
        private val VOLUME_VERIFICATION_TOLERANCE=BigDecimal("0.001")
    }
}

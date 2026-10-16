package com.silomath.core.domain.model

import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.GeometryFamilyId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.OrganizationId
import java.time.Instant
import java.util.Locale

enum class GeometryFamilyCode(
    val sequenceCode: String
) {
    CYL_TRUNC_CONE_V1("F01"),
    CYL_FLAT_V1("F02"),
    SQUARE_FLAT_V1("F03"),
    RECT_FLAT_V1("F04"),
    SQUARE_PYRAMID_HOPPER_V1("F05"),
    RECT_PYRAMID_HOPPER_V1("F06"),
    REG_HEX_FLAT_V1("F07"),
    STAR_VERIFIED_AREA_V1("F08"),
    POCKET_VERIFIED_AREA_V1("F09"),
    PERIMETER_VERIFIED_AREA_V1("F10");

    val requiresVerifiedArea: Boolean
        get() = this == STAR_VERIFIED_AREA_V1 ||
            this == POCKET_VERIFIED_AREA_V1 ||
            this == PERIMETER_VERIFIED_AREA_V1
}

data class GeometryFamily(
    val geometryFamilyId: GeometryFamilyId,
    val familyCode: GeometryFamilyCode,
    val displayName: String,
    val formulaVersion: String,
    val parameterContract: String
) {
    init {
        require(displayName.isNotBlank()) {
            "GeometryFamily displayName must not be blank"
        }
        require(displayName == displayName.trim()) {
            "GeometryFamily displayName must be trimmed"
        }
        require(formulaVersion.isNotBlank()) {
            "GeometryFamily formulaVersion must not be blank"
        }
        require(formulaVersion == formulaVersion.trim()) {
            "GeometryFamily formulaVersion must be trimmed"
        }
        require(parameterContract.isNotBlank()) {
            "GeometryFamily parameterContract must not be blank"
        }
        require(parameterContract == parameterContract.trim()) {
            "GeometryFamily parameterContract must be trimmed"
        }
    }
}

@JvmInline
value class GeometryOrigin(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "GeometryOrigin must not be blank"
        }
        require(value == value.trim()) {
            "GeometryOrigin must be trimmed"
        }
    }
}

@JvmInline
value class AreaProvenance(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "AreaProvenance must not be blank"
        }
        require(value == value.trim()) {
            "AreaProvenance must be trimmed"
        }
    }
}

sealed interface GeometryDimensions {
    val familyCode: GeometryFamilyCode
}

data class CylTruncConeDimensions(
    val diameterM: CanonicalDecimal,
    val cylindricalBodyHeightM: CanonicalDecimal,
    val hopperHeightM: CanonicalDecimal,
    val outletDiameterM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.CYL_TRUNC_CONE_V1
}

data class CylFlatDimensions(
    val diameterM: CanonicalDecimal,
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.CYL_FLAT_V1
}

data class SquareFlatDimensions(
    val sideLengthM: CanonicalDecimal,
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.SQUARE_FLAT_V1
}

data class RectFlatDimensions(
    val lengthM: CanonicalDecimal,
    val widthM: CanonicalDecimal,
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.RECT_FLAT_V1
}

data class SquarePyramidHopperDimensions(
    val sideLengthM: CanonicalDecimal,
    val bodyHeightM: CanonicalDecimal,
    val hopperHeightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.SQUARE_PYRAMID_HOPPER_V1
}

data class RectPyramidHopperDimensions(
    val lengthM: CanonicalDecimal,
    val widthM: CanonicalDecimal,
    val bodyHeightM: CanonicalDecimal,
    val hopperHeightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.RECT_PYRAMID_HOPPER_V1
}

data class RegularHexFlatDimensions(
    val sideLengthM: CanonicalDecimal,
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.REG_HEX_FLAT_V1
}

data class StarVerifiedAreaDimensions(
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.STAR_VERIFIED_AREA_V1
}

data class PocketVerifiedAreaDimensions(
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.POCKET_VERIFIED_AREA_V1
}

data class PerimeterVerifiedAreaDimensions(
    val heightM: CanonicalDecimal
) : GeometryDimensions {
    override val familyCode =
        GeometryFamilyCode.PERIMETER_VERIFIED_AREA_V1
}

data class GeometryProfile(
    val geometryProfileId: GeometryProfileId,
    val organizationId: OrganizationId,
    val name: String,
    val code: String,
    val geometryFamilyId: GeometryFamilyId,
    val lifecycleStatus: MasterStatus,
    val currentVersionId: GeometryProfileVersionId,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(name.isNotBlank()) {
            "GeometryProfile name must not be blank"
        }
        require(name == name.trim()) {
            "GeometryProfile name must be trimmed"
        }
        require(name.length <= 150) {
            "GeometryProfile name must be <= 150 characters"
        }

        require(code.isNotBlank()) {
            "GeometryProfile code must not be blank"
        }
        require(code == code.trim().uppercase(Locale.ROOT)) {
            "GeometryProfile code must be trimmed uppercase"
        }
        require(code.length <= 50) {
            "GeometryProfile code must be <= 50 characters"
        }

        require(createdBy.isNotBlank()) {
            "GeometryProfile createdBy must not be blank"
        }
        require(updatedBy.isNotBlank()) {
            "GeometryProfile updatedBy must not be blank"
        }
        require(entityVersion >= 1L) {
            "GeometryProfile entityVersion must be >= 1"
        }
    }
}

data class GeometryProfileVersion(
    val geometryProfileVersionId: GeometryProfileVersionId,
    val geometryProfileId: GeometryProfileId,
    val versionNumber: Int,
    val geometryFamilyId: GeometryFamilyId,
    val dimensions: GeometryDimensions,
    val verifiedAreaM2: CanonicalDecimal?,
    val areaProvenance: AreaProvenance?,
    val measurementReferenceHeightM: CanonicalDecimal,
    val maximumFillHeightM: CanonicalDecimal,
    val geometryOrigin: GeometryOrigin,
    val maxCapacityM3: CanonicalDecimal,
    val createdAt: Instant,
    val createdBy: String,
    val schemaVersion: String
) {
    init {
        require(versionNumber >= 1) {
            "GeometryProfileVersion versionNumber must be >= 1"
        }
        require(createdBy.isNotBlank()) {
            "GeometryProfileVersion createdBy must not be blank"
        }
        require(schemaVersion.isNotBlank()) {
            "GeometryProfileVersion schemaVersion must not be blank"
        }

        require(measurementReferenceHeightM.value.signum() > 0) {
            "GeometryProfileVersion measurementReferenceHeightM must be > 0"
        }
        require(maximumFillHeightM.value.signum() > 0) {
            "GeometryProfileVersion maximumFillHeightM must be > 0"
        }
        require(maxCapacityM3.value.signum() > 0) {
            "GeometryProfileVersion maxCapacityM3 must be > 0"
        }

        if (dimensions.familyCode.requiresVerifiedArea) {
            require(
                verifiedAreaM2 != null &&
                    verifiedAreaM2.value.signum() > 0
            ) {
                "F08-F10 GeometryProfileVersion requires positive verifiedAreaM2"
            }

            require(areaProvenance != null) {
                "F08-F10 GeometryProfileVersion requires areaProvenance"
            }
        } else {
            require(verifiedAreaM2 == null) {
                "F01-F07 GeometryProfileVersion must not define verifiedAreaM2"
            }

            require(areaProvenance == null) {
                "F01-F07 GeometryProfileVersion must not define areaProvenance"
            }
        }
    }
}

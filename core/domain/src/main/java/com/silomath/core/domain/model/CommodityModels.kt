package com.silomath.core.domain.model

import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import java.time.Instant
import java.util.Locale

enum class CommodityLifecycleStatus {
    ACTIVE,
    INACTIVE
}

enum class CommodityCategory {
    CEREAL,
    PSEUDOCEREAL,
    OILSEED,
    PULSE
}

data class CommodityReferenceValue(
    val value: CanonicalDecimal,
    val unit: String
) {
    init {
        require(unit.isNotBlank()) { "Commodity reference unit must not be blank" }
        require(unit == unit.trim()) { "Commodity reference unit must be trimmed" }
    }
}

data class Commodity(
    val commodityId: CommodityId,
    val code: String,
    val currentVersionId: CommodityVersionId,
    val lifecycleStatus: CommodityLifecycleStatus,
    val createdAt: Instant,
    val governanceActor: String
) {
    init {
        require(code.isNotBlank()) { "Commodity code must not be blank" }
        require(code == code.trim().uppercase(Locale.ROOT)) {
            "Commodity code must be trimmed uppercase"
        }
        require(code.matches(Regex("[A-Z0-9_]+"))) {
            "Commodity code must contain only A-Z, 0-9 and underscore"
        }
        require(governanceActor.isNotBlank()) {
            "Commodity governanceActor must not be blank"
        }
    }

    fun isSelectableForNewOperation(versionId: CommodityVersionId): Boolean =
        lifecycleStatus == CommodityLifecycleStatus.ACTIVE && currentVersionId == versionId
}

data class CommodityVersion(
    val commodityVersionId: CommodityVersionId,
    val commodityId: CommodityId,
    val versionNumber: Long,
    val displayName: String,
    val category: CommodityCategory,
    val referenceTestWeight: CommodityReferenceValue?,
    val moistureReference: CommodityReferenceValue?,
    val impuritiesReference: CommodityReferenceValue?,
    val definitionMetadata: Map<String, String>,
    val createdAt: Instant,
    val createdBy: String
) {
    init {
        require(versionNumber > 0L) { "CommodityVersion versionNumber must be positive" }
        require(displayName.isNotBlank()) { "CommodityVersion displayName must not be blank" }
        require(displayName == displayName.trim()) { "CommodityVersion displayName must be trimmed" }
        require(createdBy.isNotBlank()) { "CommodityVersion createdBy must not be blank" }
        require(definitionMetadata.keys.all { it.isNotBlank() && it == it.trim() }) {
            "CommodityVersion definitionMetadata keys must be non-blank and trimmed"
        }
    }
}

data class CommodityCatalogDefinition(
    val code: String,
    val displayName: String,
    val category: CommodityCategory
) {
    init {
        require(code == code.trim().uppercase(Locale.ROOT)) {
            "Commodity catalog code must be trimmed uppercase"
        }
        require(displayName.isNotBlank() && displayName == displayName.trim()) {
            "Commodity catalog displayName must be non-blank and trimmed"
        }
    }
}

object V6CommodityCatalog {
    val definitions: List<CommodityCatalogDefinition> = listOf(
        CommodityCatalogDefinition("COMMON_WHEAT", "Common Wheat", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("DURUM_WHEAT", "Durum Wheat", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("SPELT", "Spelt", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("CORN_MAIZE", "Corn/Maize", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("BARLEY", "Barley", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("MALTING_BARLEY", "Malting Barley", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("OATS", "Oats", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("RYE", "Rye", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("TRITICALE", "Triticale", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("SORGHUM", "Sorghum", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("MILLET", "Millet", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("RICE", "Rice", CommodityCategory.CEREAL),
        CommodityCatalogDefinition("BUCKWHEAT", "Buckwheat", CommodityCategory.PSEUDOCEREAL),
        CommodityCatalogDefinition("SUNFLOWER_SEED", "Sunflower Seed", CommodityCategory.OILSEED),
        CommodityCatalogDefinition("RAPESEED_CANOLA", "Rapeseed/Canola", CommodityCategory.OILSEED),
        CommodityCatalogDefinition("SOYBEAN", "Soybean", CommodityCategory.OILSEED),
        CommodityCatalogDefinition("MUSTARD_SEED", "Mustard Seed", CommodityCategory.OILSEED),
        CommodityCatalogDefinition("LINSEED_FLAXSEED", "Linseed/Flaxseed", CommodityCategory.OILSEED),
        CommodityCatalogDefinition("FIELD_PEAS", "Field Peas", CommodityCategory.PULSE),
        CommodityCatalogDefinition("FIELD_FABA_BEANS", "Field/Faba Beans", CommodityCategory.PULSE),
        CommodityCatalogDefinition("LUPINS", "Lupins", CommodityCategory.PULSE),
        CommodityCatalogDefinition("DRY_BEANS", "Dry Beans", CommodityCategory.PULSE),
        CommodityCatalogDefinition("CHICKPEAS", "Chickpeas", CommodityCategory.PULSE),
        CommodityCatalogDefinition("LENTILS", "Lentils", CommodityCategory.PULSE)
    )

    private val prohibitedGenericCodes = setOf("OTHER", "UNKNOWN", "EMPTY")

    init {
        require(definitions.size == 24) { "V6 Commodity catalog must contain exactly 24 identities" }
        require(definitions.map { it.code }.distinct().size == definitions.size) {
            "V6 Commodity catalog codes must be unique"
        }
        require(definitions.map { it.displayName }.distinct().size == definitions.size) {
            "V6 Commodity catalog display names must be unique"
        }
        require(definitions.none { it.code in prohibitedGenericCodes }) {
            "V6 Commodity catalog must not contain generic Other/Unknown/Empty identities"
        }
    }

    fun byCode(code: String): CommodityCatalogDefinition? =
        definitions.firstOrNull { it.code == code.trim().uppercase(Locale.ROOT) }
}

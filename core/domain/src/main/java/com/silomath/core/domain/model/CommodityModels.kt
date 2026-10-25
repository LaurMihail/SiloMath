package com.silomath.core.domain.model

import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import java.math.BigDecimal
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

    fun isSelectableForNewOperation(version: CommodityVersion): Boolean =
        version.commodityId == commodityId &&
            lifecycleStatus == CommodityLifecycleStatus.ACTIVE &&
            currentVersionId == version.commodityVersionId
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

    fun requireGovernedDefinitionMetadata(schema: CommodityDefinitionMetadataSchema) {
        schema.requireValid(definitionMetadata)
    }
}

data class CommodityCatalogEntry(
    val commodity: Commodity,
    val currentVersion: CommodityVersion
) {
    init {
        require(currentVersion.commodityId == commodity.commodityId) {
            "CommodityCatalogEntry version must belong to Commodity"
        }
        require(currentVersion.commodityVersionId == commodity.currentVersionId) {
            "CommodityCatalogEntry must expose the governed current CommodityVersion"
        }
    }
}

enum class CommodityMetadataValueType {
    BOOLEAN,
    INTEGER,
    DECIMAL,
    TEXT,
    ENUM
}

data class CommodityMetadataKeyDefinition(
    val key: String,
    val semanticLabel: String,
    val valueType: CommodityMetadataValueType,
    val unit: String? = null,
    val enumDomain: Set<String> = emptySet(),
    val nullable: Boolean,
    val versionApplicability: String,
    val historicalReferenceMaterial: Boolean
) {
    init {
        require(key.isNotBlank() && key == key.trim()) {
            "Commodity metadata key must be non-blank and trimmed"
        }
        require(semanticLabel.isNotBlank() && semanticLabel == semanticLabel.trim()) {
            "Commodity metadata semanticLabel must be non-blank and trimmed"
        }
        require(versionApplicability.isNotBlank() && versionApplicability == versionApplicability.trim()) {
            "Commodity metadata versionApplicability must be non-blank and trimmed"
        }
        require(unit == null || (unit.isNotBlank() && unit == unit.trim())) {
            "Commodity metadata unit must be null or non-blank and trimmed"
        }
        require(enumDomain.all { it.isNotBlank() && it == it.trim() }) {
            "Commodity metadata enum values must be non-blank and trimmed"
        }
        require(valueType == CommodityMetadataValueType.ENUM || enumDomain.isEmpty()) {
            "enumDomain is permitted only for ENUM metadata"
        }
        require(valueType != CommodityMetadataValueType.ENUM || enumDomain.isNotEmpty()) {
            "ENUM metadata requires a non-empty enumDomain"
        }
    }
}

/**
 * Platform-controlled schema for CommodityVersion.definitionMetadata.
 *
 * V6 does not define a tenant/customer extension point. The concrete registry is
 * supplied by platform governance; therefore this domain type validates only keys
 * admitted by that registry and their governed scalar types without inventing
 * additional product metadata keys.
 */
class CommodityDefinitionMetadataSchema(
    definitions: Collection<CommodityMetadataKeyDefinition>
) {
    private val definitionsByKey: Map<String, CommodityMetadataKeyDefinition>

    init {
        require(definitions.map { it.key }.distinct().size == definitions.size) {
            "Commodity metadata schema keys must be unique"
        }
        definitionsByKey = definitions.associateBy { it.key }
    }

    fun requireValid(values: Map<String, String>) {
        val unknownKeys = values.keys - definitionsByKey.keys
        require(unknownKeys.isEmpty()) {
            "Unknown Commodity definitionMetadata keys: ${unknownKeys.sorted().joinToString()}"
        }

        definitionsByKey.values
            .filterNot { it.nullable }
            .forEach { definition ->
                require(values.containsKey(definition.key)) {
                    "Missing required Commodity definitionMetadata key: ${definition.key}"
                }
            }

        values.forEach { (key, value) ->
            val definition = requireNotNull(definitionsByKey[key])
            require(value == value.trim()) {
                "Commodity definitionMetadata value for $key must be trimmed"
            }
            require(matchesType(value, definition)) {
                "Commodity definitionMetadata value for $key does not match ${definition.valueType}"
            }
        }
    }

    private fun matchesType(
        value: String,
        definition: CommodityMetadataKeyDefinition
    ): Boolean = when (definition.valueType) {
        CommodityMetadataValueType.BOOLEAN -> value == "true" || value == "false"
        CommodityMetadataValueType.INTEGER -> value.toLongOrNull() != null
        CommodityMetadataValueType.DECIMAL -> runCatching { BigDecimal(value) }.isSuccess
        CommodityMetadataValueType.TEXT -> value.isNotEmpty()
        CommodityMetadataValueType.ENUM -> value in definition.enumDomain
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

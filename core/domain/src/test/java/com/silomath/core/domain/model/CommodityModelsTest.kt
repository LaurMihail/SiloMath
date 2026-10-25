package com.silomath.core.domain.model

import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CommodityModelsTest {

    private val commodityId = CommodityId("11111111-1111-4111-8111-111111111111")
    private val otherCommodityId = CommodityId("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
    private val versionId = CommodityVersionId("22222222-2222-4222-8222-222222222222")
    private val otherVersionId = CommodityVersionId("33333333-3333-4333-8333-333333333333")
    private val instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun v6CatalogContainsExactlyTheFrozen24Identities() {
        val expectedCodes = listOf(
            "COMMON_WHEAT",
            "DURUM_WHEAT",
            "SPELT",
            "CORN_MAIZE",
            "BARLEY",
            "MALTING_BARLEY",
            "OATS",
            "RYE",
            "TRITICALE",
            "SORGHUM",
            "MILLET",
            "RICE",
            "BUCKWHEAT",
            "SUNFLOWER_SEED",
            "RAPESEED_CANOLA",
            "SOYBEAN",
            "MUSTARD_SEED",
            "LINSEED_FLAXSEED",
            "FIELD_PEAS",
            "FIELD_FABA_BEANS",
            "LUPINS",
            "DRY_BEANS",
            "CHICKPEAS",
            "LENTILS"
        )
        val expectedNames = listOf(
            "Common Wheat",
            "Durum Wheat",
            "Spelt",
            "Corn/Maize",
            "Barley",
            "Malting Barley",
            "Oats",
            "Rye",
            "Triticale",
            "Sorghum",
            "Millet",
            "Rice",
            "Buckwheat",
            "Sunflower Seed",
            "Rapeseed/Canola",
            "Soybean",
            "Mustard Seed",
            "Linseed/Flaxseed",
            "Field Peas",
            "Field/Faba Beans",
            "Lupins",
            "Dry Beans",
            "Chickpeas",
            "Lentils"
        )

        assertEquals(expectedCodes, V6CommodityCatalog.definitions.map { it.code })
        assertEquals(expectedNames, V6CommodityCatalog.definitions.map { it.displayName })
        assertEquals(24, V6CommodityCatalog.definitions.map { it.code }.distinct().size)
        assertEquals(24, V6CommodityCatalog.definitions.map { it.displayName }.distinct().size)
        assertNull(V6CommodityCatalog.byCode("OTHER"))
        assertNull(V6CommodityCatalog.byCode("UNKNOWN"))
        assertNull(V6CommodityCatalog.byCode("EMPTY"))
    }

    @Test
    fun catalogUsesExactNormativeCategoriesAndLookupIsCanonicalized() {
        assertNotNull(V6CommodityCatalog.byCode(" common_wheat "))
        assertEquals(
            CommodityCategory.PSEUDOCEREAL,
            V6CommodityCatalog.byCode("BUCKWHEAT")!!.category
        )
        assertEquals(
            CommodityCategory.OILSEED,
            V6CommodityCatalog.byCode("SUNFLOWER_SEED")!!.category
        )
        assertEquals(
            CommodityCategory.PULSE,
            V6CommodityCatalog.byCode("LENTILS")!!.category
        )
    }

    @Test
    fun commodityRequiresCanonicalCodeAndGovernanceProvenance() {
        assertThrows(IllegalArgumentException::class.java) {
            commodity(code = " common_wheat ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            commodity(governanceActor = "   ")
        }
    }

    @Test
    fun onlyActiveCurrentVersionOfSameCommodityIsSelectableForNewOperation() {
        val active = commodity()
        val inactive = commodity(lifecycleStatus = CommodityLifecycleStatus.INACTIVE)
        val currentVersion = commodityVersion()
        val wrongParent = commodityVersion(commodityId = otherCommodityId)

        assertTrue(active.isSelectableForNewOperation(versionId))
        assertFalse(active.isSelectableForNewOperation(otherVersionId))
        assertFalse(inactive.isSelectableForNewOperation(versionId))
        assertTrue(active.isSelectableForNewOperation(currentVersion))
        assertFalse(active.isSelectableForNewOperation(wrongParent))
        assertFalse(inactive.isSelectableForNewOperation(currentVersion))
    }

    @Test
    fun catalogEntryMustExposeTheGovernedCurrentVersion() {
        CommodityCatalogEntry(
            commodity = commodity(),
            currentVersion = commodityVersion()
        )

        assertThrows(IllegalArgumentException::class.java) {
            CommodityCatalogEntry(
                commodity = commodity(),
                currentVersion = commodityVersion(commodityVersionId = otherVersionId)
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            CommodityCatalogEntry(
                commodity = commodity(),
                currentVersion = commodityVersion(commodityId = otherCommodityId)
            )
        }
    }

    @Test
    fun commodityVersionRequiresPositiveVersionAndFrozenDisplayNameShape() {
        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(versionNumber = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(displayName = " Common Wheat ")
        }
    }

    @Test
    fun definitionMetadataRejectsUnknownKeysAndWrongGovernedTypes() {
        val schema = CommodityDefinitionMetadataSchema(
            definitions = listOf(
                CommodityMetadataKeyDefinition(
                    key = "reference_grade",
                    semanticLabel = "Reference grade",
                    valueType = CommodityMetadataValueType.ENUM,
                    enumDomain = setOf("A", "B"),
                    nullable = true,
                    versionApplicability = "platform-governed",
                    historicalReferenceMaterial = true
                ),
                CommodityMetadataKeyDefinition(
                    key = "reference_index",
                    semanticLabel = "Reference index",
                    valueType = CommodityMetadataValueType.DECIMAL,
                    nullable = false,
                    versionApplicability = "platform-governed",
                    historicalReferenceMaterial = true
                )
            )
        )

        commodityVersion(
            definitionMetadata = mapOf(
                "reference_grade" to "A",
                "reference_index" to "12.5"
            )
        ).requireGovernedDefinitionMetadata(schema)

        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(
                definitionMetadata = mapOf(
                    "reference_index" to "12.5",
                    "tenant_custom" to "forbidden"
                )
            ).requireGovernedDefinitionMetadata(schema)
        }

        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(
                definitionMetadata = mapOf("reference_index" to "not-a-decimal")
            ).requireGovernedDefinitionMetadata(schema)
        }

        assertThrows(IllegalArgumentException::class.java) {
            commodityVersion(
                definitionMetadata = mapOf("reference_grade" to "A")
            ).requireGovernedDefinitionMetadata(schema)
        }
    }

    private fun commodity(
        code: String = "COMMON_WHEAT",
        lifecycleStatus: CommodityLifecycleStatus = CommodityLifecycleStatus.ACTIVE,
        governanceActor: String = "platform-governance"
    ) = Commodity(
        commodityId = commodityId,
        code = code,
        currentVersionId = versionId,
        lifecycleStatus = lifecycleStatus,
        createdAt = instant,
        governanceActor = governanceActor
    )

    private fun commodityVersion(
        commodityId: CommodityId = this.commodityId,
        commodityVersionId: CommodityVersionId = versionId,
        versionNumber: Long = 1L,
        displayName: String = "Common Wheat",
        definitionMetadata: Map<String, String> = emptyMap()
    ) = CommodityVersion(
        commodityVersionId = commodityVersionId,
        commodityId = commodityId,
        versionNumber = versionNumber,
        displayName = displayName,
        category = CommodityCategory.CEREAL,
        referenceTestWeight = null,
        moistureReference = null,
        impuritiesReference = null,
        definitionMetadata = definitionMetadata,
        createdAt = instant,
        createdBy = "platform-governance"
    )
}

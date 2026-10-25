package com.silomath.core.database

import com.silomath.core.data.repository.OfflineGeometryRepository
import com.silomath.core.data.repository.OfflineInventorySnapshotRepository
import com.silomath.core.database.dao.GeometryDao
import com.silomath.core.database.dao.InventoryDao
import com.silomath.core.database.entity.GeometryVersionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Retained legacy persistence compliance checks that are still authoritative.
 *
 * Stocktaking validation moved to canonical UUID-backed core:domain tests under
 * StocktakingModelsTest. Legacy Long-backed StocktakingRecordEntity behavior is
 * intentionally not treated as the V6 domain contract.
 */
class Phase4ComplianceTest {

    private lateinit var geometryDao: GeometryDao
    private lateinit var geometryRepository: OfflineGeometryRepository

    @Before
    fun setup() {
        geometryDao = mockk(relaxed = true)
        geometryRepository = OfflineGeometryRepository(geometryDao)
    }

    @Test
    fun `createSnapshot enforces immutability`() = runTest {
        val inventoryDao = mockk<InventoryDao>(relaxed = true)
        val repository = OfflineInventorySnapshotRepository(inventoryDao)

        coEvery { inventoryDao.getSnapshotsByCampaignSync(1) } returns listOf(mockk())

        try {
            repository.createSnapshot(1, "{}", emptyList())
            fail("Expected IllegalStateException for existing snapshot")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("already exists"))
        }
    }

    @Test
    fun `GeometryVersion append-only logic enforced`() = runTest {
        coEvery { geometryDao.getMaxVersionNumber(1) } returns 2

        val newVersion =
            GeometryVersionEntity(
                id = 0,
                geometryProfileId = 1,
                versionNumber = 2,
                fixedParameters = "{}",
                requiredDimensionsSchema = "{}",
                measurementSchema = "{}",
                supportedUnits = "{}",
                calculationMethod = "test",
                validationConstraints = "{}"
            )

        try {
            geometryRepository.createNewVersion(newVersion)
            fail("Expected IllegalArgumentException for non-incremental version")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        val validVersion = newVersion.copy(versionNumber = 3)
        geometryRepository.createNewVersion(validVersion)
        coVerify { geometryDao.insertVersion(validVersion) }
    }
}

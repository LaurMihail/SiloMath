package com.silomath.core.database

import com.silomath.core.data.repository.OfflineGeometryRepository
import com.silomath.core.data.repository.OfflineInventorySnapshotRepository
import com.silomath.core.data.repository.OfflineStocktakingRepository
import com.silomath.core.database.dao.GeometryDao
import com.silomath.core.database.dao.InventoryDao
import com.silomath.core.database.dao.StocktakingDao
import com.silomath.core.database.entity.GeometryVersionEntity
import com.silomath.core.database.entity.StocktakingRecordEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class Phase4ComplianceTest {

    private lateinit var stocktakingDao: StocktakingDao
    private lateinit var geometryDao: GeometryDao
    private lateinit var stocktakingRepository: OfflineStocktakingRepository
    private lateinit var geometryRepository: OfflineGeometryRepository

    @Before
    fun setup() {
        stocktakingDao = mockk(relaxed = true)
        geometryDao = mockk(relaxed = true)
        stocktakingRepository = OfflineStocktakingRepository(stocktakingDao)
        geometryRepository = OfflineGeometryRepository(geometryDao)
    }

    @Test
    fun `StocktakingRecord validation - Invalid ownership throws exception`() = runTest {
        val record = StocktakingRecordEntity(
            id = 1,
            campaignId = 1,
            storageSpaceId = 1,
            commodityId = 1,
            density = 1.0,
            ownership = "Invalid",
            clientId = null,
            status = "To Do"
        )
        try {
            stocktakingRepository.updateRecord(record)
            fail("Expected IllegalArgumentException for invalid ownership")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun `StocktakingRecord validation - Third-Party without client throws exception`() = runTest {
        val record = StocktakingRecordEntity(
            id = 1,
            campaignId = 1,
            storageSpaceId = 1,
            commodityId = 1,
            density = 1.0,
            ownership = "Third-Party",
            clientId = null,
            status = "To Do"
        )
        try {
            stocktakingRepository.updateRecord(record)
            fail("Expected IllegalArgumentException for Third-Party without client")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun `StocktakingRecord validation - Invalid status throws exception`() = runTest {
        val record = StocktakingRecordEntity(
            id = 1,
            campaignId = 1,
            storageSpaceId = 1,
            commodityId = 1,
            density = 1.0,
            ownership = "Owned",
            clientId = null,
            status = "Invalid Status"
        )
        try {
            stocktakingRepository.updateRecord(record)
            fail("Expected IllegalArgumentException for invalid status")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun `addMeasurement enforces maximum 5 rule`() = runTest {
        coEvery { stocktakingDao.getMeasurementCount(1) } returns 4
        stocktakingRepository.addMeasurement(1, 10.5)
        coVerify { stocktakingDao.addMeasurementTransactional(1, 10.5) }

        // Test rejection of 6th measurement (simulated by DAO throwing)
        coEvery { stocktakingDao.addMeasurementTransactional(1, any()) } throws IllegalArgumentException("Maximum 5 measurements per record exceeded")
        try {
            stocktakingRepository.addMeasurement(1, 10.5)
            fail("Expected IllegalArgumentException for 6th measurement")
        } catch (e: IllegalArgumentException) {
            assertEquals("Maximum 5 measurements per record exceeded", e.message)
        }
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
    fun `GeometryVersion - Append-only logic enforced`() = runTest {
        coEvery { geometryDao.getMaxVersionNumber(1) } returns 2
        
        val newVersion = GeometryVersionEntity(
            id = 0,
            geometryProfileId = 1,
            versionNumber = 2, // Same as max, should fail
            dimensionsJson = "{}"
        )
        
        try {
            geometryRepository.createNewVersion(newVersion)
            fail("Expected IllegalArgumentException for non-incremental version")
        } catch (e: IllegalArgumentException) {
            // Success
        }
        
        val validVersion = newVersion.copy(versionNumber = 3)
        geometryRepository.createNewVersion(validVersion)
        coVerify { geometryDao.insertVersion(validVersion) }
    }
}

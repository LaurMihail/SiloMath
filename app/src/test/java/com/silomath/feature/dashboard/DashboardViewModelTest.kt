package com.silomath.feature.dashboard

import com.silomath.core.database.entity.CampaignEntity
import com.silomath.core.database.entity.StocktakingRecordEntity
import com.silomath.core.domain.repository.CampaignRepository
import com.silomath.core.domain.repository.StocktakingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val campaignRepository: CampaignRepository = mockk()
    private val stocktakingRepository: StocktakingRepository = mockk()
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { campaignRepository.getActiveCampaigns() } returns flowOf(emptyList())
        coEvery { stocktakingRepository.getRecords(any()) } returns flowOf(emptyList())
        viewModel = DashboardViewModel(campaignRepository, stocktakingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct default counts`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(0, state.activeCampaigns)
        assertEquals(0, state.binStatusCount["Validated"])
    }

    @Test
    fun `no repository call when campaign filter is absent`() = runTest {
        // Upon creation, it shouldn't call getRecords because filter is null
        coVerify(exactly = 0) { stocktakingRepository.getRecords(any()) }
    }

    @Test
    fun `active campaign count is repository-derived`() = runTest {
        val campaigns = listOf(
            CampaignEntity(id = 1, organizationId = 1, name = "C1", status = "Active"),
            CampaignEntity(id = 2, organizationId = 1, name = "C2", status = "Active")
        )
        coEvery { campaignRepository.getActiveCampaigns() } returns flowOf(campaigns)
        
        val vm = DashboardViewModel(campaignRepository, stocktakingRepository)
        
        assertEquals(2, vm.uiState.value.activeCampaigns)
    }

    @Test
    fun `bin status counts use only approved statuses from repository records`() = runTest {
        val records = listOf(
            StocktakingRecordEntity(id = 1, campaignId = 1, storageSpaceId = 1, status = "Validated", ownership = "Owned", clientId = null, commodityId = null, density = null),
            StocktakingRecordEntity(id = 2, campaignId = 1, storageSpaceId = 2, status = "To Do", ownership = "Owned", clientId = null, commodityId = null, density = null),
            StocktakingRecordEntity(id = 3, campaignId = 1, storageSpaceId = 3, status = "Issues", ownership = "Owned", clientId = null, commodityId = null, density = null),
            StocktakingRecordEntity(id = 4, campaignId = 1, storageSpaceId = 4, status = "To Do", ownership = "Owned", clientId = null, commodityId = null, density = null)
        )
        coEvery { stocktakingRepository.getRecords(1L) } returns flowOf(records)
        
        viewModel.onFilterChanged(campId = 1L)
        
        val statusCount = viewModel.uiState.value.binStatusCount
        assertEquals(1, statusCount["Validated"])
        assertEquals(2, statusCount["To Do"])
        assertEquals(1, statusCount["Issues"])
    }
}

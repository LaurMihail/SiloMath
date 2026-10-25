package com.silomath.feature.dashboard

import com.silomath.core.domain.repository.CampaignRepository
import com.silomath.core.domain.repository.StocktakingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val campaignRepository: CampaignRepository = mockk()
    private val stocktakingRepository: StocktakingRepository = mockk(relaxed = true)
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { campaignRepository.observeActiveCampaignCount() } returns flowOf(0)
        viewModel = DashboardViewModel(campaignRepository, stocktakingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has zero active campaigns`() = runTest {
        assertEquals(0, viewModel.uiState.value.activeCampaigns)
    }

    @Test
    fun `active campaign count is repository derived`() = runTest {
        every { campaignRepository.observeActiveCampaignCount() } returns flowOf(2)

        val vm = DashboardViewModel(campaignRepository, stocktakingRepository)

        assertEquals(2, vm.uiState.value.activeCampaigns)
    }

    @Test
    fun `filter changes update selected identifiers`() = runTest {
        viewModel.onFilterChanged(
            orgId = 11L,
            campId = 22L,
            locId = 33L,
            commId = 44L,
            ownership = "Owned",
            clientId = 55L
        )

        val state = viewModel.uiState.value
        assertEquals(11L, state.selectedOrganizationId)
        assertEquals(22L, state.selectedCampaignId)
        assertEquals(33L, state.selectedLocationId)
        assertEquals(44L, state.selectedCommodityId)
        assertEquals("Owned", state.selectedOwnership)
        assertEquals(55L, state.selectedClientId)
    }
}

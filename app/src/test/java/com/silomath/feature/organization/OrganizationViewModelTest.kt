package com.silomath.feature.organization

import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.OrganizationRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationViewModelTest {

    private val organizationRepository: OrganizationRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: OrganizationViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { organizationRepository.getOrganizations() } returns flowOf(emptyList())
        viewModel = OrganizationViewModel(organizationRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectOrganization success updates state for navigation`() = runTest {
        coEvery { authRepository.setActiveOrganization(any()) } returns Result.success(Unit)
        
        viewModel.selectOrganization(42L)
        
        assertEquals(42L, viewModel.uiState.value.selectedOrganizationId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `selectOrganization failure does not set selected ID and exposes error`() = runTest {
        coEvery { authRepository.setActiveOrganization(any()) } returns Result.failure(Exception("Failed"))
        
        viewModel.selectOrganization(42L)
        
        assertNull(viewModel.uiState.value.selectedOrganizationId)
        assertEquals("Failed", viewModel.uiState.value.error)
    }
}

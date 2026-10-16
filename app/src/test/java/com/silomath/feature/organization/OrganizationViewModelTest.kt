package com.silomath.feature.organization

import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.OrganizationRepository
import com.silomath.core.model.UserId
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationViewModelTest {

    private val organizationRepository: OrganizationRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: OrganizationViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val userId =
        "11111111-1111-1111-1111-111111111111"

    private val organizationId =
        "22222222-2222-2222-2222-222222222222"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { authRepository.sessionState } returns
            flowOf(
                SessionState(
                    userId = userId,
                    token = "token"
                )
            )

        every {
            organizationRepository.observeAuthorizedOrganizations(
                UserId(userId)
            )
        } returns flowOf(emptyList())

        viewModel =
            OrganizationViewModel(
                organizationRepository,
                authRepository
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectOrganization success updates state for navigation`() =
        runTest {
            coEvery {
                authRepository.setActiveOrganization(organizationId)
            } returns Result.success(Unit)

            viewModel.selectOrganization(organizationId)

            assertEquals(
                organizationId,
                viewModel.uiState.value.selectedOrganizationId
            )
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `selectOrganization failure does not set selected ID and exposes error`() =
        runTest {
            coEvery {
                authRepository.setActiveOrganization(organizationId)
            } returns Result.failure(Exception("Failed"))

            viewModel.selectOrganization(organizationId)

            assertNull(
                viewModel.uiState.value.selectedOrganizationId
            )
            assertEquals(
                "Failed",
                viewModel.uiState.value.error
            )
        }
}
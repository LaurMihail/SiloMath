package com.silomath.feature.masterdata.organization

import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationViewModelTest {

    private val authRepository:
        AuthRepository = mockk()

    private val sessionState =
        MutableStateFlow(
            SessionState(
                userId = "user-1",
                token = "access",
                authorizedOrganizations =
                    listOf(
                        AuthorizedOrganizationContext(
                            membershipId =
                                "membership-1",
                            organizationId =
                                "org-1",
                            organizationName =
                                "Alpha Grain",
                            organizationCode =
                                "ALPHA",
                            role = "ADMIN"
                        ),
                        AuthorizedOrganizationContext(
                            membershipId =
                                "membership-2",
                            organizationId =
                                "org-2",
                            organizationName =
                                "Beta Storage",
                            organizationCode =
                                "BETA",
                            role = "STANDARD"
                        )
                    )
            )
        )

    private val dispatcher =
        UnconfinedTestDispatcher()

    private lateinit var viewModel:
        OrganizationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery {
            authRepository.sessionState
        } returns sessionState
        viewModel =
            OrganizationViewModel(
                authRepository
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authorized organizations come from server session context`() =
        runTest {
            assertEquals(
                2,
                viewModel
                    .uiState
                    .value
                    .organizations
                    .size
            )
        }

    @Test
    fun `search filters by organization code case insensitively`() =
        runTest {
            viewModel
                .onSearchQueryChanged(
                    "beta"
                )

            val organizations =
                viewModel
                    .uiState
                    .value
                    .organizations

            assertEquals(1, organizations.size)
            assertEquals(
                "org-2",
                organizations
                    .single()
                    .organizationId
            )
        }

    @Test
    fun `unauthorized organization cannot be selected`() =
        runTest {
            viewModel.selectOrganization(
                "org-unauthorized"
            )

            assertEquals(
                "Organization is not authorized for current user",
                viewModel.uiState.value.error
            )
            assertNull(
                viewModel
                    .uiState
                    .value
                    .selectedOrganizationId
            )

            coVerify(exactly = 0) {
                authRepository
                    .setActiveOrganization(any())
            }
        }

    @Test
    fun `authorized organization selection persists active context`() =
        runTest {
            coEvery {
                authRepository
                    .setActiveOrganization(
                        "org-1"
                    )
            } returns Result.success(Unit)

            viewModel.selectOrganization(
                "org-1"
            )

            assertEquals(
                "org-1",
                viewModel
                    .uiState
                    .value
                    .selectedOrganizationId
            )
            assertTrue(
                viewModel
                    .uiState
                    .value
                    .error == null
            )
        }
}
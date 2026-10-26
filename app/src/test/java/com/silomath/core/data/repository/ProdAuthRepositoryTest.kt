package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.dto.AcceptedResponseDto
import com.silomath.core.data.network.dto.AuthorizedMembershipSummaryDto
import com.silomath.core.data.network.dto.RefreshSessionRequestDto
import com.silomath.core.data.network.dto.ResetPasswordRequestDto
import com.silomath.core.data.network.dto.SessionResponseDto
import com.silomath.core.data.network.dto.SessionUserSummaryDto
import com.silomath.core.data.network.dto.SignInRequestDto
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProdAuthRepositoryTest {

    private lateinit var sessionStore:
        SessionStore

    private lateinit var authApi:
        AuthApi

    private lateinit var repository:
        ProdAuthRepository

    @Before
    fun setup() {
        sessionStore =
            mockk(relaxed = true)
        authApi = mockk()
        repository =
            ProdAuthRepository(
                sessionStore,
                authApi
            )
    }

    @Test
    fun `signIn stores canonical server authorized organizations`() =
        runTest {
            coEvery {
                authApi.signIn(
                    SignInRequestDto(
                        email =
                            "user@example.com",
                        password =
                            "password"
                    )
                )
            } returns sessionResponse()

            val result =
                repository.signIn(
                    "user@example.com",
                    "password"
                )

            assertTrue(result.isSuccess)

            coVerify(exactly = 1) {
                sessionStore.updateSession(
                    SessionState(
                        userId = "user-123",
                        token =
                            "access-token",
                        refreshToken =
                            "refresh-token",
                        activeOrganizationId =
                            null,
                        authorizedOrganizations =
                            listOf(
                                authorizedContext(
                                    organizationId =
                                        "org-42"
                                )
                            ),
                        isOfflineCapable =
                            false
                    )
                )
            }
        }

    @Test
    fun `restoreRemoteSession preserves active organization only when still authorized`() =
        runTest {
            val existing =
                SessionState(
                    userId = "user-123",
                    token =
                        "expired-access",
                    refreshToken =
                        "stored-refresh",
                    activeOrganizationId =
                        "org-42",
                    authorizedOrganizations =
                        listOf(
                            authorizedContext(
                                organizationId =
                                    "org-42"
                            )
                        ),
                    isOfflineCapable =
                        true
                )

            every {
                sessionStore.sessionState
            } returns flowOf(existing)

            coEvery {
                authApi.refreshSession(
                    RefreshSessionRequestDto(
                        refreshToken =
                            "stored-refresh"
                    )
                )
            } returns sessionResponse()

            val result =
                repository
                    .restoreRemoteSession()

            assertTrue(result.isSuccess)

            coVerify(exactly = 1) {
                sessionStore.updateSession(
                    existing.copy(
                        userId =
                            "user-123",
                        token =
                            "access-token",
                        refreshToken =
                            "refresh-token",
                        activeOrganizationId =
                            "org-42",
                        authorizedOrganizations =
                            listOf(
                                authorizedContext(
                                    organizationId =
                                        "org-42"
                                )
                            )
                    )
                )
            }
        }

    @Test
    fun `restoreRemoteSession clears active organization removed from server authorization`() =
        runTest {
            val existing =
                SessionState(
                    userId = "user-123",
                    token =
                        "expired-access",
                    refreshToken =
                        "stored-refresh",
                    activeOrganizationId =
                        "org-old",
                    authorizedOrganizations =
                        listOf(
                            authorizedContext(
                                organizationId =
                                    "org-old"
                            )
                        )
                )

            every {
                sessionStore.sessionState
            } returns flowOf(existing)

            coEvery {
                authApi.refreshSession(any())
            } returns sessionResponse()

            val result =
                repository
                    .restoreRemoteSession()

            assertTrue(result.isSuccess)

            coVerify(exactly = 1) {
                sessionStore.updateSession(
                    existing.copy(
                        userId =
                            "user-123",
                        token =
                            "access-token",
                        refreshToken =
                            "refresh-token",
                        activeOrganizationId =
                            null,
                        authorizedOrganizations =
                            listOf(
                                authorizedContext(
                                    organizationId =
                                        "org-42"
                                )
                            )
                    )
                )
            }
        }

    @Test
    fun `setActiveOrganization accepts only server authorized organization`() =
        runTest {
            val existing =
                SessionState(
                    userId = "user-123",
                    token = "access",
                    authorizedOrganizations =
                        listOf(
                            authorizedContext(
                                organizationId =
                                    "org-42"
                            )
                        )
                )

            every {
                sessionStore.sessionState
            } returns flowOf(existing)

            val result =
                repository
                    .setActiveOrganization(
                        "org-42"
                    )

            assertTrue(result.isSuccess)

            coVerify(exactly = 1) {
                sessionStore.updateSession(
                    existing.copy(
                        activeOrganizationId =
                            "org-42"
                    )
                )
            }
        }

    @Test
    fun `setActiveOrganization rejects cross organization selection`() =
        runTest {
            every {
                sessionStore.sessionState
            } returns flowOf(
                SessionState(
                    userId = "user-123",
                    token = "access",
                    authorizedOrganizations =
                        listOf(
                            authorizedContext(
                                organizationId =
                                    "org-42"
                            )
                        )
                )
            )

            val result =
                repository
                    .setActiveOrganization(
                        "org-unauthorized"
                    )

            assertTrue(result.isFailure)
            assertEquals(
                "Organization is not authorized for current user",
                result
                    .exceptionOrNull()
                    ?.message
            )

            coVerify(exactly = 0) {
                sessionStore.updateSession(
                    any()
                )
            }
        }

    @Test
    fun `restoreRemoteSession fails closed without protected refresh credential`() =
        runTest {
            every {
                sessionStore.sessionState
            } returns flowOf(
                SessionState(
                    userId = "user-123",
                    token =
                        "expired-access"
                )
            )

            val result =
                repository
                    .restoreRemoteSession()

            assertTrue(result.isFailure)
            assertEquals(
                "No protected refresh credential available",
                result
                    .exceptionOrNull()
                    ?.message
            )

            coVerify(exactly = 0) {
                authApi.refreshSession(any())
            }
        }

    @Test
    fun `updatePassword sends recovery context and clears local auth after server success`() =
        runTest {
            coEvery {
                authApi.resetPassword(
                    ResetPasswordRequestDto(
                        recoveryToken =
                            "recovery-token",
                        newPassword =
                            "new-password"
                    )
                )
            } returns
                AcceptedResponseDto(
                    accepted = true
                )

            val result =
                repository.updatePassword(
                    recoveryContext =
                        "recovery-token",
                    newPassword =
                        "new-password"
                )

            assertTrue(result.isSuccess)

            coVerify(exactly = 1) {
                sessionStore.clearSession()
            }
        }

    @Test
    fun `updatePassword keeps local auth until server reset succeeds`() =
        runTest {
            coEvery {
                authApi.resetPassword(any())
            } throws
                IllegalStateException(
                    "Reset rejected"
                )

            val result =
                repository.updatePassword(
                    recoveryContext =
                        "recovery-token",
                    newPassword =
                        "new-password"
                )

            assertTrue(result.isFailure)

            coVerify(exactly = 0) {
                sessionStore.clearSession()
            }
        }

    @Test
    fun `signOut clears local session after backend sign out`() =
        runTest {
            every {
                sessionStore.sessionState
            } returns flowOf(
                SessionState(
                    token =
                        "access-token"
                )
            )

            coEvery {
                authApi.signOut(
                    "Bearer access-token"
                )
            } returns Unit

            val result =
                repository.signOut()

            assertTrue(result.isSuccess)

            coVerify(exactly = 1) {
                authApi.signOut(
                    "Bearer access-token"
                )
            }

            coVerify(exactly = 1) {
                sessionStore.clearSession()
            }
        }

    private fun sessionResponse():
        SessionResponseDto =
        SessionResponseDto(
            user =
                SessionUserSummaryDto(
                    userId = "user-123",
                    fullName =
                        "Test User",
                    email =
                        "user@example.com"
                ),
            accessToken =
                "access-token",
            refreshToken =
                "refresh-token",
            expiresAtEpochSeconds =
                123456789L,
            memberships =
                listOf(
                    AuthorizedMembershipSummaryDto(
                        membershipId =
                            "membership-org-42",
                        organizationId =
                            "org-42",
                        organizationName =
                            "Organization org-42",
                        organizationCode =
                            "CODE",
                        role = "ADMIN"
                    )
                )
        )

    private fun authorizedContext(
        organizationId: String
    ) =
        AuthorizedOrganizationContext(
            membershipId =
                "membership-$organizationId",
            organizationId =
                organizationId,
            organizationName =
                "Organization $organizationId",
            organizationCode =
                "CODE",
            role = "ADMIN"
        )
}
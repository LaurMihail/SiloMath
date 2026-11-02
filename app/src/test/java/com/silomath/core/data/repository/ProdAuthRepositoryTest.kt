package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.auth.SupabaseAuthGateway
import com.silomath.core.data.auth.SupabaseAuthSession
import com.silomath.core.data.network.api.CurrentUserApi
import com.silomath.core.data.network.dto.AuthorizedMembershipSummaryDto
import com.silomath.core.data.network.dto.CurrentUserResponseDto
import com.silomath.core.data.network.dto.SessionUserSummaryDto
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class ProdAuthRepositoryTest {

    private val sessionFlow =
        MutableStateFlow(SessionState())
    private lateinit var sessionStore: SessionStore
    private lateinit var supabaseAuth: SupabaseAuthGateway
    private lateinit var currentUserApi: CurrentUserApi
    private lateinit var repository: ProdAuthRepository

    @Before
    fun setup() {
        sessionFlow.value = SessionState()
        sessionStore = mockk(relaxed = true)
        supabaseAuth = mockk(relaxed = true)
        currentUserApi = mockk()
        every { sessionStore.sessionState } returns sessionFlow
        repository =
            ProdAuthRepository(
                sessionStore = sessionStore,
                supabaseAuth = supabaseAuth,
                currentUserApi = currentUserApi
            )
    }

    @Test
    fun signInUsesSupabaseAndPersistsCanonicalMemberships() =
        runTest {
            val stored = slot<SessionState>()
            coEvery {
                supabaseAuth.signIn(
                    "admin@example.com",
                    "correct-password"
                )
            } returns authSession()
            coEvery {
                currentUserApi.getCurrentUser(
                    "Bearer access-token"
                )
            } returns currentUserResponse()
            coEvery {
                sessionStore.updateSession(
                    capture(stored)
                )
            } returns Unit

            val result =
                repository.signIn(
                    "admin@example.com",
                    "correct-password"
                )

            assertTrue(result.isSuccess)
            assertEquals("user-123", stored.captured.userId)
            assertEquals(
                "access-token",
                stored.captured.token
            )
            assertEquals(
                "refresh-token",
                stored.captured.refreshToken
            )
            assertNull(
                stored.captured.activeOrganizationId
            )
            assertFalse(stored.captured.isOfflineCapable)
            assertEquals(
                listOf(
                    authorizedContext("org-42")
                ),
                stored.captured.authorizedOrganizations
            )
            assertFalse(
                stored.captured.token == "debug_token"
            )
        }

    @Test
    fun signInFailureDoesNotPersistSession() =
        runTest {
            coEvery {
                supabaseAuth.signIn(any(), any())
            } throws IllegalStateException(
                "Invalid login credentials"
            )

            val result =
                repository.signIn(
                    "admin@example.com",
                    "wrong-password"
                )

            assertTrue(result.isFailure)
            coVerify(exactly = 0) {
                sessionStore.updateSession(any())
            }
            coVerify {
                supabaseAuth.clearInMemorySession()
            }
        }

    @Test
    fun signInRejectsCanonicalIdentityMismatch() =
        runTest {
            coEvery {
                supabaseAuth.signIn(any(), any())
            } returns authSession()
            coEvery {
                currentUserApi.getCurrentUser(any())
            } returns currentUserResponse(
                userId = "different-user"
            )

            val result =
                repository.signIn(
                    "admin@example.com",
                    "correct-password"
                )

            assertTrue(result.isFailure)
            coVerify(exactly = 0) {
                sessionStore.updateSession(any())
            }
        }

    @Test
    fun restoreRefreshesTokensAndPreservesAuthorizedActiveOrganization() =
        runTest {
            sessionFlow.value =
                existingSession(
                    activeOrganizationId = "org-42"
                )
            val stored = slot<SessionState>()
            coEvery {
                supabaseAuth.refreshSession(
                    "old-refresh"
                )
            } returns authSession(
                accessToken = "new-access",
                refreshToken = "new-refresh"
            )
            coEvery {
                currentUserApi.getCurrentUser(
                    "Bearer new-access"
                )
            } returns currentUserResponse()
            coEvery {
                sessionStore.updateSession(
                    capture(stored)
                )
            } returns Unit

            val result =
                repository.restoreRemoteSession()

            assertTrue(result.isSuccess)
            assertEquals(
                "org-42",
                stored.captured.activeOrganizationId
            )
            assertEquals(
                "new-access",
                stored.captured.token
            )
            assertEquals(
                "new-refresh",
                stored.captured.refreshToken
            )
            assertTrue(stored.captured.isOfflineCapable)
        }

    @Test
    fun restoreClearsRemovedActiveOrganization() =
        runTest {
            sessionFlow.value =
                existingSession(
                    activeOrganizationId = "org-removed"
                )
            val stored = slot<SessionState>()
            coEvery {
                supabaseAuth.refreshSession(any())
            } returns authSession()
            coEvery {
                currentUserApi.getCurrentUser(any())
            } returns currentUserResponse()
            coEvery {
                sessionStore.updateSession(
                    capture(stored)
                )
            } returns Unit

            val result =
                repository.restoreRemoteSession()

            assertTrue(result.isSuccess)
            assertNull(
                stored.captured.activeOrganizationId
            )
        }

    @Test
    fun restoreFailsClosedWithoutProtectedRefreshCredential() =
        runTest {
            sessionFlow.value =
                SessionState(
                    userId = "user-123",
                    token = "expired-access",
                    refreshToken = null
                )

            val result =
                repository.restoreRemoteSession()

            assertTrue(result.isFailure)
            coVerify(exactly = 0) {
                supabaseAuth.refreshSession(any())
            }
        }

    @Test
    fun signOutAlwaysClearsLocalCredentialsWhenRemoteFails() =
        runTest {
            sessionFlow.value =
                existingSession(
                    activeOrganizationId = "org-42"
                ).copy(
                    token = "access-token",
                    refreshToken = "refresh-token"
                )
            coEvery {
                supabaseAuth.signOut(
                    "access-token",
                    "refresh-token"
                )
            } throws IllegalStateException(
                "Remote unavailable"
            )

            val result = repository.signOut()

            assertTrue(result.isFailure)
            coVerify { sessionStore.clearSession() }
        }

    @Test
    fun passwordResetUsesSupabaseAuth() =
        runTest {
            coEvery {
                supabaseAuth.requestPasswordReset(
                    "admin@example.com"
                )
            } returns Unit

            val result =
                repository.resetPassword(
                    "admin@example.com"
                )

            assertTrue(result.isSuccess)
            coVerify {
                supabaseAuth.requestPasswordReset(
                    "admin@example.com"
                )
            }
        }

    @Test
    fun passwordUpdateUsesRecoveryTokenAndClearsLocalAuth() =
        runTest {
            coEvery {
                supabaseAuth.updatePassword(
                    "recovery-access-token",
                    "new-password"
                )
            } returns Unit

            val result =
                repository.updatePassword(
                    "recovery-access-token",
                    "new-password"
                )

            assertTrue(result.isSuccess)
            coVerify { sessionStore.clearSession() }
            coVerify {
                supabaseAuth.clearInMemorySession()
            }
        }

    @Test
    fun setActiveOrganizationAcceptsOnlyCanonicalMembership() =
        runTest {
            sessionFlow.value =
                existingSession(
                    activeOrganizationId = null
                )
            val stored = slot<SessionState>()
            coEvery {
                sessionStore.updateSession(
                    capture(stored)
                )
            } returns Unit

            val accepted =
                repository.setActiveOrganization(
                    "org-42"
                )
            val rejected =
                repository.setActiveOrganization(
                    "other-org"
                )

            assertTrue(accepted.isSuccess)
            assertTrue(rejected.isFailure)
            assertEquals(
                "org-42",
                stored.captured.activeOrganizationId
            )
        }

    private fun authSession(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token"
    ) = SupabaseAuthSession(
        userId = "user-123",
        accessToken = accessToken,
        refreshToken = refreshToken
    )

    private fun currentUserResponse(
        userId: String = "user-123"
    ) = CurrentUserResponseDto(
        user =
            SessionUserSummaryDto(
                userId = userId,
                fullName = "Test Admin",
                email = "admin@example.com"
            ),
        memberships =
            listOf(
                AuthorizedMembershipSummaryDto(
                    membershipId =
                        " membership-org-42 ",
                    organizationId = " org-42 ",
                    organizationName =
                        " Test Organization ",
                    organizationCode = " test ",
                    role = "ADMIN"
                )
            )
    )

    private fun existingSession(
        activeOrganizationId: String?
    ) = SessionState(
        userId = "user-123",
        token = "old-access",
        refreshToken = "old-refresh",
        activeOrganizationId =
            activeOrganizationId,
        authorizedOrganizations =
            listOf(
                authorizedContext("org-42"),
                authorizedContext("org-removed")
            ),
        isOfflineCapable = true
    )

    private fun authorizedContext(
        organizationId: String
    ) = AuthorizedOrganizationContext(
        membershipId =
            "membership-" + organizationId,
        organizationId = organizationId,
        organizationName =
            if (organizationId == "org-42") {
                "Test Organization"
            } else {
                "Removed Organization"
            },
        organizationCode =
            if (organizationId == "org-42") {
                "TEST"
            } else {
                "REMOVED"
            },
        role = "ADMIN"
    )
}

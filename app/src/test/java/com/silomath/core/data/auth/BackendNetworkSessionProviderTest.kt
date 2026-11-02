package com.silomath.core.data.auth

import com.silomath.core.data.network.api.CurrentUserApi
import com.silomath.core.data.network.dto.AuthorizedMembershipSummaryDto
import com.silomath.core.data.network.dto.CurrentUserResponseDto
import com.silomath.core.data.network.dto.SessionUserSummaryDto
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class BackendNetworkSessionProviderTest {

    private val sessionFlow =
        MutableStateFlow(SessionState())
    private lateinit var sessionStore: SessionStore
    private lateinit var credentialStore:
        SecureCredentialStore
    private lateinit var supabaseAuth:
        SupabaseAuthGateway
    private lateinit var currentUserApi:
        CurrentUserApi
    private lateinit var provider:
        BackendNetworkSessionProvider

    @Before
    fun setup() {
        sessionStore = mockk(relaxed = true)
        credentialStore = mockk()
        supabaseAuth = mockk()
        currentUserApi = mockk()
        every { sessionStore.sessionState } returns sessionFlow

        provider =
            BackendNetworkSessionProvider(
                sessionStore = sessionStore,
                secureCredentialStore =
                    credentialStore,
                supabaseAuth = supabaseAuth,
                currentUserApi = currentUserApi
            )
    }

    @Test
    fun refreshPersistsRotatedTokensAndCanonicalMemberships() =
        runTest {
            sessionFlow.value =
                SessionState(
                    userId = "user-123",
                    token = "expired-access",
                    refreshToken = "old-refresh",
                    activeOrganizationId = "org-42",
                    authorizedOrganizations =
                        listOf(authorizedOrganization()),
                    isOfflineCapable = true
                )
            every {
                credentialStore.getAccessToken()
            } returns "expired-access"
            every {
                credentialStore.getRefreshToken()
            } returns "old-refresh"
            coEvery {
                supabaseAuth.refreshSession(
                    "old-refresh"
                )
            } returns
                SupabaseAuthSession(
                    userId = "user-123",
                    accessToken = "new-access",
                    refreshToken = "new-refresh"
                )
            coEvery {
                currentUserApi.getCurrentUser(
                    "Bearer new-access"
                )
            } returns currentUser()
            val stored = slot<SessionState>()
            coEvery {
                sessionStore.updateSession(
                    capture(stored)
                )
            } returns Unit

            val result =
                provider.refreshAccessToken(
                    "expired-access"
                )

            assertEquals("new-access", result)
            assertEquals(
                "new-refresh",
                stored.captured.refreshToken
            )
            assertEquals(
                "org-42",
                stored.captured.activeOrganizationId
            )
            assertEquals(
                listOf(authorizedOrganization()),
                stored.captured.authorizedOrganizations
            )
        }

    @Test
    fun refreshClearsNoLongerAuthorizedActiveOrganization() =
        runTest {
            sessionFlow.value =
                SessionState(
                    userId = "user-123",
                    activeOrganizationId =
                        "removed-org",
                    isOfflineCapable = true
                )
            every {
                credentialStore.getAccessToken()
            } returns "expired-access"
            every {
                credentialStore.getRefreshToken()
            } returns "old-refresh"
            coEvery {
                supabaseAuth.refreshSession(any())
            } returns
                SupabaseAuthSession(
                    userId = "user-123",
                    accessToken = "new-access",
                    refreshToken = "new-refresh"
                )
            coEvery {
                currentUserApi.getCurrentUser(any())
            } returns currentUser()
            val stored = slot<SessionState>()
            coEvery {
                sessionStore.updateSession(
                    capture(stored)
                )
            } returns Unit

            provider.refreshAccessToken(
                "expired-access"
            )

            assertNull(
                stored.captured.activeOrganizationId
            )
        }

    @Test
    fun refreshFailsClosedWhenSupabaseRejectsRefreshToken() =
        runTest {
            every {
                credentialStore.getAccessToken()
            } returns "expired-access"
            every {
                credentialStore.getRefreshToken()
            } returns "invalid-refresh"
            coEvery {
                supabaseAuth.refreshSession(any())
            } throws IllegalStateException(
                "Invalid refresh token"
            )

            val result =
                provider.refreshAccessToken(
                    "expired-access"
                )

            assertNull(result)
        }

    private fun currentUser() =
        CurrentUserResponseDto(
            user =
                SessionUserSummaryDto(
                    userId = "user-123",
                    fullName = "Test Admin",
                    email = "admin@example.com"
                ),
            memberships =
                listOf(
                    AuthorizedMembershipSummaryDto(
                        membershipId =
                            "membership-org-42",
                        organizationId = "org-42",
                        organizationName =
                            "Test Organization",
                        organizationCode = "TEST",
                        role = "ADMIN"
                    )
                )
        )

    private fun authorizedOrganization() =
        AuthorizedOrganizationContext(
            membershipId =
                "membership-org-42",
            organizationId = "org-42",
            organizationName =
                "Test Organization",
            organizationCode = "TEST",
            role = "ADMIN"
        )
}

package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.dto.AcceptedResponseDto
import com.silomath.core.data.network.dto.RefreshSessionRequestDto
import com.silomath.core.data.network.dto.ResetPasswordRequestDto
import com.silomath.core.data.network.dto.SessionResponseDto
import com.silomath.core.data.network.dto.SessionUserSummaryDto
import com.silomath.core.data.network.dto.SignInRequestDto
import com.silomath.core.domain.model.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProdAuthRepositoryTest {

    private lateinit var sessionStore: SessionStore
    private lateinit var authApi: AuthApi
    private lateinit var repository: ProdAuthRepository

    @Before
    fun setup() {
        sessionStore = mockk(relaxed = true)
        authApi = mockk()
        repository = ProdAuthRepository(sessionStore, authApi)
    }

    @Test
    fun `signIn stores canonical backend session`() = runTest {
        val response = sessionResponse()

        coEvery {
            authApi.signIn(
                SignInRequestDto(
                    email = "user@example.com",
                    password = "password"
                )
            )
        } returns response

        val result = repository.signIn("user@example.com", "password")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            sessionStore.updateSession(
                SessionState(
                    userId = "user-123",
                    token = "access-token",
                    refreshToken = "refresh-token",
                    activeOrganizationId = null,
                    isOfflineCapable = false
                )
            )
        }
    }

    @Test
    fun `restoreRemoteSession rotates protected credentials and preserves local context`() = runTest {
        val existingSession =
            SessionState(
                userId = "user-123",
                token = "expired-access-token",
                refreshToken = "stored-refresh-token",
                activeOrganizationId = "org-42",
                isOfflineCapable = true
            )
        every { sessionStore.sessionState } returns flowOf(existingSession)
        coEvery {
            authApi.refreshSession(
                RefreshSessionRequestDto(
                    refreshToken = "stored-refresh-token"
                )
            )
        } returns sessionResponse()

        val result = repository.restoreRemoteSession()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            sessionStore.updateSession(
                existingSession.copy(
                    userId = "user-123",
                    token = "access-token",
                    refreshToken = "refresh-token"
                )
            )
        }
    }

    @Test
    fun `restoreRemoteSession fails closed without protected refresh credential`() = runTest {
        every { sessionStore.sessionState } returns
            flowOf(
                SessionState(
                    userId = "user-123",
                    token = "expired-access-token"
                )
            )

        val result = repository.restoreRemoteSession()

        assertTrue(result.isFailure)
        assertEquals(
            "No protected refresh credential available",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) {
            authApi.refreshSession(any())
        }
    }

    @Test
    fun `updatePassword sends recovery context and clears local auth after server success`() = runTest {
        coEvery {
            authApi.resetPassword(
                ResetPasswordRequestDto(
                    recoveryToken = "recovery-token",
                    newPassword = "new-password"
                )
            )
        } returns AcceptedResponseDto(accepted = true)

        val result =
            repository.updatePassword(
                recoveryContext = "recovery-token",
                newPassword = "new-password"
            )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            authApi.resetPassword(
                ResetPasswordRequestDto(
                    recoveryToken = "recovery-token",
                    newPassword = "new-password"
                )
            )
        }
        coVerify(exactly = 1) {
            sessionStore.clearSession()
        }
    }

    @Test
    fun `updatePassword keeps local auth until server reset succeeds`() = runTest {
        coEvery {
            authApi.resetPassword(any())
        } throws IllegalStateException("Reset rejected")

        val result =
            repository.updatePassword(
                recoveryContext = "recovery-token",
                newPassword = "new-password"
            )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) {
            sessionStore.clearSession()
        }
    }

    @Test
    fun `setActiveOrganization preserves session and updates string organization id`() = runTest {
        val existingSession =
            SessionState(
                userId = "user-123",
                token = "access-token",
                refreshToken = "refresh-token",
                isOfflineCapable = true
            )
        every { sessionStore.sessionState } returns flowOf(existingSession)

        val result =
            repository.setActiveOrganization("org-42")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            sessionStore.updateSession(
                existingSession.copy(
                    activeOrganizationId = "org-42"
                )
            )
        }
    }

    @Test
    fun `setActiveOrganization returns persistence failure`() = runTest {
        every {
            sessionStore.sessionState
        } returns flowOf(SessionState())

        coEvery {
            sessionStore.updateSession(any())
        } throws IllegalStateException("Persistence failed")

        val result =
            repository.setActiveOrganization("org-42")

        assertTrue(result.isFailure)
        assertEquals(
            "Persistence failed",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `signOut clears local session after backend sign out`() = runTest {
        every {
            sessionStore.sessionState
        } returns flowOf(
            SessionState(token = "access-token")
        )

        coEvery {
            authApi.signOut("Bearer access-token")
        } returns Unit

        val result = repository.signOut()

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) {
            authApi.signOut("Bearer access-token")
        }

        coVerify(exactly = 1) {
            sessionStore.clearSession()
        }
    }

    private fun sessionResponse(): SessionResponseDto =
        SessionResponseDto(
            user =
                SessionUserSummaryDto(
                    userId = "user-123",
                    fullName = "Test User",
                    email = "user@example.com"
                ),
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtEpochSeconds = 123456789L,
            memberships = emptyList()
        )
}
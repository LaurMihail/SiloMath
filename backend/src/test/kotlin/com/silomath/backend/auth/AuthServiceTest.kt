package com.silomath.backend.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthServiceTest {

    @Test
    fun `disabled canonical user is rejected and issued session is globally revoked`() = runSuspend {
        val provider = FakeProvider()
        val directory = FakeDirectory(
            lifecycle = CanonicalUserLifecycle.DISABLED
        )
        val service = DefaultAuthService(provider, directory)

        val result = service.signIn("user@example.com", "secret")

        assertEquals(AuthOperationResult.UserDisabled, result)
        assertEquals(listOf("access-token"), provider.globalRevocations)
    }

    @Test
    fun `active canonical user receives session with authorized memberships`() = runSuspend {
        val provider = FakeProvider()
        val directory = FakeDirectory(
            lifecycle = CanonicalUserLifecycle.ACTIVE
        )
        val service = DefaultAuthService(provider, directory)

        val result = service.signIn("user@example.com", "secret")
        val session = assertIs<AuthOperationResult.Success<SessionResponse>>(result).value

        assertEquals("user-1", session.user.userId)
        assertEquals("access-token", session.accessToken)
        assertEquals("refresh-token", session.refreshToken)
        assertEquals(1, session.memberships.size)
        assertEquals("org-1", session.memberships.single().organizationId)
    }

    @Test
    fun `forgot password remains non enumerating for provider client errors`() = runSuspend {
        val provider = FakeProvider(
            recoveryResult = ProviderActionResult.InvalidContext
        )
        val service = DefaultAuthService(
            provider,
            FakeDirectory(CanonicalUserLifecycle.ACTIVE)
        )

        val result = service.forgotPassword("unknown@example.com")

        assertIs<AuthOperationResult.Success<AcceptedResponse>>(result)
    }

    private class FakeProvider(
        private val recoveryResult: ProviderActionResult = ProviderActionResult.Success
    ) : AuthenticationProvider {
        val globalRevocations = mutableListOf<String>()

        override suspend fun signIn(email: String, password: String) =
            ProviderSessionResult.Success(
                ProviderSession(
                    userId = "user-1",
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    expiresAtEpochSeconds = 1234L
                )
            )

        override suspend fun refresh(refreshToken: String) =
            ProviderSessionResult.SessionExpired

        override suspend fun requestPasswordRecovery(email: String) =
            recoveryResult

        override suspend fun resetPasswordAndRevokeAll(
            recoveryToken: String,
            newPassword: String
        ) = ProviderActionResult.Success

        override suspend fun signOutLocal(accessToken: String) =
            ProviderActionResult.Success

        override suspend fun signOutGlobal(accessToken: String): ProviderActionResult {
            globalRevocations += accessToken
            return ProviderActionResult.Success
        }
    }

    private class FakeDirectory(
        private val lifecycle: CanonicalUserLifecycle
    ) : CanonicalUserDirectory {
        override fun findSessionIdentity(userId: String) =
            CanonicalSessionIdentity(
                user = SessionUserSummary(
                    userId = userId,
                    fullName = "Test User",
                    email = "user@example.com"
                ),
                lifecycle = lifecycle,
                memberships = listOf(
                    AuthorizedMembershipSummary(
                        membershipId = "membership-1",
                        organizationId = "org-1",
                        organizationName = "Organization",
                        organizationCode = "ORG",
                        role = "STANDARD"
                    )
                )
            )
    }
}

private fun runSuspend(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking { block() }
}

package com.silomath.backend.auth

import kotlin.test.Test
import kotlin.test.assertIs

class PasswordResetContractTest {

    @Test
    fun `reset password success is exposed only after provider confirms revocation`() =
        kotlinx.coroutines.runBlocking {
            val service = DefaultAuthService(
                provider = object : AuthenticationProvider {
                    override suspend fun signIn(email: String, password: String) =
                        ProviderSessionResult.InvalidCredentials

                    override suspend fun refresh(refreshToken: String) =
                        ProviderSessionResult.SessionExpired

                    override suspend fun requestPasswordRecovery(email: String) =
                        ProviderActionResult.Success

                    override suspend fun resetPasswordAndRevokeAll(
                        recoveryToken: String,
                        newPassword: String
                    ) = ProviderActionResult.Success

                    override suspend fun signOutLocal(accessToken: String) =
                        ProviderActionResult.Success

                    override suspend fun signOutGlobal(accessToken: String) =
                        ProviderActionResult.Success
                },
                userDirectory = object : CanonicalUserDirectory {
                    override fun findSessionIdentity(userId: String) = null
                }
            )

            assertIs<AuthOperationResult.Success<AcceptedResponse>>(
                service.resetPassword("recovery-token", "new-password")
            )
        }
}

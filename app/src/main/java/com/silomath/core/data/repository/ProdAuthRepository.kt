package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.dto.ForgotPasswordRequestDto
import com.silomath.core.data.network.dto.RefreshSessionRequestDto
import com.silomath.core.data.network.dto.ResetPasswordRequestDto
import com.silomath.core.data.network.dto.SessionResponseDto
import com.silomath.core.data.network.dto.SignInRequestDto
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProdAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val authApi: AuthApi
) : AuthRepository {

    override val sessionState: Flow<SessionState> =
        sessionStore.sessionState

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<Unit> = runCatching {
        val response =
            authApi.signIn(
                SignInRequestDto(
                    email = email,
                    password = password
                )
            )

        sessionStore.updateSession(
            SessionState(
                userId = response.user.userId,
                token = response.accessToken,
                refreshToken = response.refreshToken,
                activeOrganizationId = null,
                authorizedOrganizations =
                    response.toAuthorizedOrganizations(),
                isOfflineCapable = false
            )
        )
    }

    override suspend fun signOut(): Result<Unit> {
        val accessToken =
            sessionStore.sessionState.first().token

        val serverResult =
            runCatching {
                if (!accessToken.isNullOrBlank()) {
                    authApi.signOut(
                        "Bearer $accessToken"
                    )
                }
            }

        sessionStore.clearSession()
        return serverResult
    }

    override suspend fun resetPassword(
        email: String
    ): Result<Unit> =
        runCatching {
            authApi.forgotPassword(
                ForgotPasswordRequestDto(
                    email = email
                )
            )
        }.map { Unit }

    override suspend fun updatePassword(
        recoveryContext: String,
        newPassword: String
    ): Result<Unit> {
        if (recoveryContext.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Recovery context is required"
                )
            )
        }

        if (newPassword.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "New password is required"
                )
            )
        }

        return runCatching {
            authApi.resetPassword(
                ResetPasswordRequestDto(
                    recoveryToken = recoveryContext,
                    newPassword = newPassword
                )
            )

            sessionStore.clearSession()
        }
    }

    override suspend fun setActiveOrganization(
        organizationId: String
    ): Result<Unit> {
        val normalizedOrganizationId =
            organizationId.trim()

        if (normalizedOrganizationId.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Organization id is required"
                )
            )
        }

        return runCatching {
            val currentState =
                sessionStore.sessionState.first()

            val isAuthorized =
                currentState.authorizedOrganizations.any {
                    it.organizationId ==
                        normalizedOrganizationId
                }

            check(isAuthorized) {
                "Organization is not authorized for current user"
            }

            sessionStore.updateSession(
                currentState.copy(
                    activeOrganizationId =
                        normalizedOrganizationId
                )
            )
        }
    }

    override suspend fun restoreRemoteSession():
        Result<Unit> {
        val currentState =
            sessionStore.sessionState.first()

        val refreshToken =
            currentState.refreshToken
                ?.takeIf { it.isNotBlank() }
                ?: return Result.failure(
                    IllegalStateException(
                        "No protected refresh credential available"
                    )
                )

        return runCatching {
            val response =
                authApi.refreshSession(
                    RefreshSessionRequestDto(
                        refreshToken = refreshToken
                    )
                )

            val authorizedOrganizations =
                response.toAuthorizedOrganizations()

            val restoredActiveOrganizationId =
                currentState.activeOrganizationId
                    ?.takeIf { activeId ->
                        authorizedOrganizations.any {
                            it.organizationId == activeId
                        }
                    }

            sessionStore.updateSession(
                currentState.copy(
                    userId = response.user.userId,
                    token = response.accessToken,
                    refreshToken =
                        response.refreshToken,
                    activeOrganizationId =
                        restoredActiveOrganizationId,
                    authorizedOrganizations =
                        authorizedOrganizations
                )
            )
        }
    }

    private fun SessionResponseDto
        .toAuthorizedOrganizations():
        List<AuthorizedOrganizationContext> =
        memberships
            .map {
                AuthorizedOrganizationContext(
                    membershipId =
                        it.membershipId.trim(),
                    organizationId =
                        it.organizationId.trim(),
                    organizationName =
                        it.organizationName.trim(),
                    organizationCode =
                        it.organizationCode
                            .trim()
                            .uppercase(),
                    role = it.role.trim()
                )
            }
            .filter {
                it.membershipId.isNotBlank() &&
                    it.organizationId.isNotBlank() &&
                    it.organizationName.isNotBlank() &&
                    it.organizationCode.isNotBlank()
            }
            .distinctBy { it.organizationId }
            .sortedWith(
                compareBy<
                    AuthorizedOrganizationContext
                > {
                    it.organizationName.lowercase()
                }.thenBy {
                    it.organizationCode
                }
            )
}
package com.silomath.feature.authentication

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val sessionStateFlow =
        MutableStateFlow(SessionState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery {
            authRepository.sessionState
        } returns sessionStateFlow

        coEvery {
            authRepository.restoreRemoteSession()
        } returns Result.failure(
            IllegalStateException("No stored session")
        )

        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startup restoration failure completes bootstrap without authenticated session`() =
        runTest {
            assertTrue(
                viewModel.uiState.value.isSessionRestoreComplete
            )
            assertFalse(
                viewModel.uiState.value.isRestoredSessionReady
            )
        }

    @Test
    fun `startup restoration success marks restored session ready`() =
        runTest {
            coEvery {
                authRepository.restoreRemoteSession()
            } answers {
                sessionStateFlow.value =
                    SessionState(
                        userId = "user-123",
                        token = "rotated-access-token",
                        refreshToken = "rotated-refresh-token"
                    )
                Result.success(Unit)
            }

            viewModel = AuthViewModel(authRepository)

            assertTrue(
                viewModel.uiState.value.isSessionRestoreComplete
            )
            assertTrue(
                viewModel.uiState.value.isRestoredSessionReady
            )
        }

    @Test
    fun `signIn with valid credentials sets success state`() =
        runTest {
            coEvery {
                authRepository.signIn(any(), any())
            } returns Result.success(Unit)

            viewModel.onEmailChanged("admin@silo.com")
            viewModel.onPasswordChanged("password")
            viewModel.signIn()

            assertTrue(viewModel.uiState.value.isSuccess)
            assertTrue(
                viewModel.uiState.value.isRestoredSessionReady
            )
        }

    @Test
    fun `signIn with empty fields sets error state`() =
        runTest {
            viewModel.onEmailChanged("")
            viewModel.onPasswordChanged("")
            viewModel.signIn()

            assertEquals(
                "Please fill in all fields",
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `continueOffline with valid session succeeds`() =
        runTest {
            sessionStateFlow.value =
                SessionState(
                    userId = "1",
                    token = "abc",
                    isOfflineCapable = true
                )

            viewModel.continueOffline()

            assertTrue(viewModel.uiState.value.isSuccess)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `continueOffline with invalid session fails`() =
        runTest {
            sessionStateFlow.value =
                SessionState(
                    userId = null,
                    isOfflineCapable = false
                )

            viewModel.continueOffline()

            assertFalse(viewModel.uiState.value.isSuccess)
            assertEquals(
                "No valid offline session available",
                viewModel.uiState.value.error
            )
        }

    @Test
    fun `password reset forwards recovery context and reports success`() =
        runTest {
            coEvery {
                authRepository.updatePassword(
                    "recovery-token",
                    "new-password"
                )
            } returns Result.success(Unit)

            viewModel.updatePassword(
                recoveryContext = "recovery-token",
                newPassword = "new-password"
            )

            assertTrue(
                viewModel.passwordResetUiState.value.isSuccess
            )
            assertFalse(
                viewModel.passwordResetUiState.value.isSubmitting
            )
            assertNull(
                viewModel.passwordResetUiState.value.error
            )

            coVerify(exactly = 1) {
                authRepository.updatePassword(
                    "recovery-token",
                    "new-password"
                )
            }
        }

    @Test
    fun `password reset failure exposes generic recovery error`() =
        runTest {
            coEvery {
                authRepository.updatePassword(any(), any())
            } returns Result.failure(
                IllegalStateException("provider detail")
            )

            viewModel.updatePassword(
                recoveryContext = "recovery-token",
                newPassword = "new-password"
            )

            assertFalse(
                viewModel.passwordResetUiState.value.isSuccess
            )
            assertEquals(
                "The recovery context is invalid or expired, " +
                    "or the password was rejected.",
                viewModel.passwordResetUiState.value.error
            )
        }
}
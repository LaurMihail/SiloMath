package com.silomath.feature.authentication

import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val sessionStateFlow = MutableStateFlow(SessionState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.sessionState } returns sessionStateFlow
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn with valid credentials sets success state`() = runTest {
        coEvery { authRepository.signIn(any(), any(), any()) } returns Result.success(Unit)

        viewModel.onEmailChanged("admin@silo.com")
        viewModel.onPasswordChanged("password")
        viewModel.onRememberMeChanged(true)
        viewModel.signIn()

        assertEquals(true, viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `signIn with empty fields sets error state`() = runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")
        viewModel.signIn()

        assertEquals("Please fill in all fields", viewModel.uiState.value.error)
    }

    @Test
    fun `continueOffline with valid session succeeds`() = runTest {
        sessionStateFlow.value = SessionState(userId = "1", token = "abc", isOfflineCapable = true)
        
        viewModel.continueOffline()
        
        assertEquals(true, viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `continueOffline with invalid session fails`() = runTest {
        sessionStateFlow.value = SessionState(userId = null, isOfflineCapable = false)
        
        viewModel.continueOffline()
        
        assertEquals(false, viewModel.uiState.value.isSuccess)
        assertEquals("No valid offline session available", viewModel.uiState.value.error)
    }
}

package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.domain.model.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProdAuthRepositoryTest {

    private lateinit var sessionStore: SessionStore
    private lateinit var repository: ProdAuthRepository

    @Before
    fun setup() {
        sessionStore = mockk(relaxed = true)
        repository = ProdAuthRepository(sessionStore)
    }

    @Test
    fun `setActiveOrganization preserves existing session and updates ID`() = runTest {
        val existingSession = SessionState(
            userId = "user123",
            token = "token123",
            isOfflineCapable = true,
            isRememberMeEnabled = true
        )
        coEvery { sessionStore.sessionState } returns flowOf(existingSession)
        
        val result = repository.setActiveOrganization(42L)
        
        assertTrue(result.isSuccess)
        coVerify { 
            sessionStore.updateSession(
                existingSession.copy(activeOrganizationId = 42L)
            )
        }
    }

    @Test
    fun `signIn handles rememberMe semantics in test implementation`() = runTest {
        // We use MockAuthRepository here since ProdAuthRepository.signIn always fails
        val mockRepo = MockAuthRepository(sessionStore)
        
        // case 1: rememberMe = true
        mockRepo.signIn("admin@silo.com", "password", true)
        coVerify { 
            sessionStore.updateSession(match { it.isRememberMeEnabled }) 
        }

        // case 2: rememberMe = false
        mockRepo.signIn("admin@silo.com", "password", false)
        coVerify { 
            sessionStore.updateSession(match { !it.isRememberMeEnabled }) 
        }
    }

    @Test
    fun `setActiveOrganization returns failure if session store fails`() = runTest {
        coEvery { sessionStore.sessionState } returns flowOf(SessionState())
        coEvery { sessionStore.updateSession(any()) } throws Exception("Persistence failed")
        
        val result = repository.setActiveOrganization(42L)
        
        assertTrue(result.isFailure)
        assertEquals("Persistence failed", result.exceptionOrNull()?.message)
    }
}

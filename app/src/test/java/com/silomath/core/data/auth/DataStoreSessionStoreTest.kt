package com.silomath.core.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.silomath.core.domain.model.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSessionStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val USER_ID = stringPreferencesKey("user_id")
    private val TOKEN = stringPreferencesKey("token")

    @Test
    fun `rememberMe true persists credentials to disk`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test1.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { dataFile })
        val store = DataStoreSessionStore(dataStore)
        
        val state = SessionState(
            userId = "user1",
            token = "token1",
            isRememberMeEnabled = true
        )
        
        store.updateSession(state)
        
        // Verify via Flow (In-memory/Combined)
        assertEquals("user1", store.sessionState.first().userId)
        
        // Verify raw DataStore (On disk)
        val prefs = dataStore.data.first()
        assertEquals("user1", prefs[USER_ID])
        assertEquals("token1", prefs[TOKEN])
    }

    @Test
    fun `rememberMe false keeps credentials in memory but clears from disk`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test2.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { dataFile })
        val store = DataStoreSessionStore(dataStore)
        
        val state = SessionState(
            userId = "user1",
            token = "token1",
            isRememberMeEnabled = false
        )
        
        store.updateSession(state)
        
        // Verify in-memory (Flow should have credentials)
        val current = store.sessionState.first()
        assertEquals("user1", current.userId)
        assertTrue(current.isLoggedIn)
        
        // Verify raw DataStore (On disk should NOT have credentials)
        val prefs = dataStore.data.first()
        assertNull(prefs[USER_ID])
        assertNull(prefs[TOKEN])
    }

    @Test
    fun `isOfflineCapable is preserved independently`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test3.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = this, produceFile = { dataFile })
        val store = DataStoreSessionStore(dataStore)
        
        val state = SessionState(
            userId = "user1",
            token = "token1",
            isOfflineCapable = true,
            isRememberMeEnabled = false
        )
        
        store.updateSession(state)
        
        // Check disk for isOfflineCapable
        val isOfflineCapableKey = booleanPreferencesKey("is_offline_capable")
        val prefs = dataStore.data.first()
        assertTrue(prefs[isOfflineCapableKey] == true)
    }
}

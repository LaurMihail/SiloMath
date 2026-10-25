package com.silomath.core.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.silomath.core.domain.model.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSessionStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val userIdKey = stringPreferencesKey("user_id")
    private val activeOrganizationIdKey = stringPreferencesKey("active_organization_id")
    private val isOfflineCapableKey = booleanPreferencesKey("is_offline_capable")

    @Test
    fun `updateSession stores credentials in secure store and metadata in DataStore`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test1.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = this,
                produceFile = { dataFile }
            )
        val secureStore = InMemorySecureCredentialStore()
        val store = DataStoreSessionStore(dataStore, secureStore)

        val state =
            SessionState(
                userId = "user1",
                token = "access-token",
                refreshToken = "refresh-token",
                activeOrganizationId = "org-1",
                isOfflineCapable = true
            )

        store.updateSession(state)

        val current = store.sessionState.first()
        assertEquals("user1", current.userId)
        assertEquals("access-token", current.token)
        assertEquals("refresh-token", current.refreshToken)
        assertEquals("org-1", current.activeOrganizationId)
        assertTrue(current.isOfflineCapable)

        assertEquals("access-token", secureStore.getAccessToken())
        assertEquals("refresh-token", secureStore.getRefreshToken())

        val prefs = dataStore.data.first()
        assertEquals("user1", prefs[userIdKey])
        assertEquals("org-1", prefs[activeOrganizationIdKey])
        assertTrue(prefs[isOfflineCapableKey] == true)
    }

    @Test
    fun `sessionState reads credentials from secure store`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test2.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = this,
                produceFile = { dataFile }
            )
        val secureStore = InMemorySecureCredentialStore()
        val store = DataStoreSessionStore(dataStore, secureStore)

        secureStore.saveCredentials(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        val current = store.sessionState.first()

        assertEquals("access-token", current.token)
        assertEquals("refresh-token", current.refreshToken)
    }

    @Test
    fun `isOfflineCapable is persisted independently of credentials`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test3.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = this,
                produceFile = { dataFile }
            )
        val secureStore = InMemorySecureCredentialStore()
        val store = DataStoreSessionStore(dataStore, secureStore)

        store.updateSession(
            SessionState(
                userId = "user1",
                isOfflineCapable = true
            )
        )

        val current = store.sessionState.first()
        assertTrue(current.isOfflineCapable)
        assertNull(current.token)
        assertNull(current.refreshToken)

        val prefs = dataStore.data.first()
        assertTrue(prefs[isOfflineCapableKey] == true)
    }

    @Test
    fun `clearSession clears DataStore metadata and secure credentials`() = runTest {
        val dataFile = File(tmpFolder.newFolder(), "test4.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = this,
                produceFile = { dataFile }
            )
        val secureStore = InMemorySecureCredentialStore()
        val store = DataStoreSessionStore(dataStore, secureStore)

        store.updateSession(
            SessionState(
                userId = "user1",
                token = "access-token",
                refreshToken = "refresh-token",
                activeOrganizationId = "org-1",
                isOfflineCapable = true
            )
        )

        store.clearSession()

        val current = store.sessionState.first()
        assertNull(current.userId)
        assertNull(current.token)
        assertNull(current.refreshToken)
        assertNull(current.activeOrganizationId)
        assertFalse(current.isOfflineCapable)

        assertNull(secureStore.getAccessToken())
        assertNull(secureStore.getRefreshToken())

        val prefs = dataStore.data.first()
        assertNull(prefs[userIdKey])
        assertNull(prefs[activeOrganizationIdKey])
        assertNull(prefs[isOfflineCapableKey])
    }
}

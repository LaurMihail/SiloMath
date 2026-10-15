package com.silomath.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.silomath.core.domain.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureCredentialStore: SecureCredentialStore
) : SessionStore {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val ACTIVE_ORGANIZATION_ID = stringPreferencesKey("active_organization_id")
        val IS_OFFLINE_CAPABLE = booleanPreferencesKey("is_offline_capable")
    }

    override val sessionState: Flow<SessionState> = dataStore.data.map { preferences ->
        SessionState(
            userId = preferences[PreferencesKeys.USER_ID],
            token = secureCredentialStore.getAccessToken(),
            refreshToken = secureCredentialStore.getRefreshToken(),
            activeOrganizationId = preferences[PreferencesKeys.ACTIVE_ORGANIZATION_ID],
            isOfflineCapable = preferences[PreferencesKeys.IS_OFFLINE_CAPABLE] ?: false
        )
    }

    override suspend fun updateSession(sessionState: SessionState) {
        secureCredentialStore.saveCredentials(sessionState.token, sessionState.refreshToken)
        dataStore.edit { preferences ->
            sessionState.userId?.let { preferences[PreferencesKeys.USER_ID] = it }
            sessionState.activeOrganizationId?.let {
                preferences[PreferencesKeys.ACTIVE_ORGANIZATION_ID] = it
            }
            preferences[PreferencesKeys.IS_OFFLINE_CAPABLE] = sessionState.isOfflineCapable
        }
    }

    override suspend fun clearSession() {
        secureCredentialStore.clearCredentials()
        dataStore.edit { it.clear() }
    }
}

package com.silomath.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.silomath.core.domain.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionStore {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val TOKEN = stringPreferencesKey("token")
        val ACTIVE_ORGANIZATION_ID = longPreferencesKey("active_organization_id")
        val IS_OFFLINE_CAPABLE = booleanPreferencesKey("is_offline_capable")
        val IS_REMEMBER_ME_ENABLED = booleanPreferencesKey("is_remember_me_enabled")
    }

    private val transientSession = MutableStateFlow<SessionState?>(null)

    override val sessionState: Flow<SessionState> = dataStore.data.map { preferences ->
        SessionState(
            userId = preferences[PreferencesKeys.USER_ID],
            token = preferences[PreferencesKeys.TOKEN],
            activeOrganizationId = preferences[PreferencesKeys.ACTIVE_ORGANIZATION_ID],
            isOfflineCapable = preferences[PreferencesKeys.IS_OFFLINE_CAPABLE] ?: false,
            isRememberMeEnabled = preferences[PreferencesKeys.IS_REMEMBER_ME_ENABLED] ?: false
        )
    }.combine(transientSession) { persisted, transient ->
        transient ?: persisted
    }

    override suspend fun updateSession(sessionState: SessionState) {
        if (sessionState.isRememberMeEnabled) {
            transientSession.value = null
            dataStore.edit { preferences ->
                sessionState.userId?.let { preferences[PreferencesKeys.USER_ID] = it }
                sessionState.token?.let { preferences[PreferencesKeys.TOKEN] = it }
                sessionState.activeOrganizationId?.let { preferences[PreferencesKeys.ACTIVE_ORGANIZATION_ID] = it }
                preferences[PreferencesKeys.IS_OFFLINE_CAPABLE] = sessionState.isOfflineCapable
                preferences[PreferencesKeys.IS_REMEMBER_ME_ENABLED] = true
            }
        } else {
            transientSession.value = sessionState
            dataStore.edit { preferences ->
                preferences.remove(PreferencesKeys.USER_ID)
                preferences.remove(PreferencesKeys.TOKEN)
                sessionState.activeOrganizationId?.let { preferences[PreferencesKeys.ACTIVE_ORGANIZATION_ID] = it }
                preferences[PreferencesKeys.IS_OFFLINE_CAPABLE] = sessionState.isOfflineCapable
                preferences[PreferencesKeys.IS_REMEMBER_ME_ENABLED] = false
            }
        }
    }

    override suspend fun clearSession() {
        transientSession.value = null
        dataStore.edit { it.clear() }
    }
}

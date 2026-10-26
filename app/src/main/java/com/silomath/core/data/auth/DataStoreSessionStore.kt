package com.silomath.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureCredentialStore: SecureCredentialStore
) : SessionStore {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val ACTIVE_ORGANIZATION_ID =
            stringPreferencesKey("active_organization_id")
        val AUTHORIZED_ORGANIZATIONS =
            stringPreferencesKey("authorized_organizations_json")
        val IS_OFFLINE_CAPABLE =
            booleanPreferencesKey("is_offline_capable")
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val sessionState: Flow<SessionState> =
        dataStore.data.map { preferences ->
            SessionState(
                userId = preferences[PreferencesKeys.USER_ID],
                token = secureCredentialStore.getAccessToken(),
                refreshToken =
                    secureCredentialStore.getRefreshToken(),
                activeOrganizationId =
                    preferences[
                        PreferencesKeys.ACTIVE_ORGANIZATION_ID
                    ],
                authorizedOrganizations =
                    decodeAuthorizedOrganizations(
                        preferences[
                            PreferencesKeys.AUTHORIZED_ORGANIZATIONS
                        ]
                    ),
                isOfflineCapable =
                    preferences[
                        PreferencesKeys.IS_OFFLINE_CAPABLE
                    ] ?: false
            )
        }

    override suspend fun updateSession(
        sessionState: SessionState
    ) {
        secureCredentialStore.saveCredentials(
            sessionState.token,
            sessionState.refreshToken
        )

        dataStore.edit { preferences ->
            val userId = sessionState.userId
            if (userId == null) {
                preferences.remove(PreferencesKeys.USER_ID)
            } else {
                preferences[PreferencesKeys.USER_ID] =
                    userId
            }

            val activeOrganizationId =
                sessionState.activeOrganizationId

            if (activeOrganizationId == null) {
                preferences.remove(
                    PreferencesKeys.ACTIVE_ORGANIZATION_ID
                )
            } else {
                preferences[
                    PreferencesKeys.ACTIVE_ORGANIZATION_ID
                ] = activeOrganizationId
            }

            preferences[
                PreferencesKeys.AUTHORIZED_ORGANIZATIONS
            ] =
                json.encodeToString(
                    sessionState.authorizedOrganizations.map {
                        AuthorizedOrganizationPayload(
                            membershipId = it.membershipId,
                            organizationId = it.organizationId,
                            organizationName =
                                it.organizationName,
                            organizationCode =
                                it.organizationCode,
                            role = it.role
                        )
                    }
                )

            preferences[
                PreferencesKeys.IS_OFFLINE_CAPABLE
            ] = sessionState.isOfflineCapable
        }
    }

    override suspend fun clearSession() {
        secureCredentialStore.clearCredentials()
        dataStore.edit { it.clear() }
    }

    private fun decodeAuthorizedOrganizations(
        raw: String?
    ): List<AuthorizedOrganizationContext> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            json
                .decodeFromString<
                    List<AuthorizedOrganizationPayload>
                >(raw)
                .map {
                    AuthorizedOrganizationContext(
                        membershipId = it.membershipId,
                        organizationId = it.organizationId,
                        organizationName =
                            it.organizationName,
                        organizationCode =
                            it.organizationCode,
                        role = it.role
                    )
                }
        }.getOrDefault(emptyList())
    }
}

@Serializable
private data class AuthorizedOrganizationPayload(
    val membershipId: String,
    val organizationId: String,
    val organizationName: String,
    val organizationCode: String,
    val role: String
)
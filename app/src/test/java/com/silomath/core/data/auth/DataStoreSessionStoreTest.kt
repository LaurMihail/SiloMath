package com.silomath.core.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import java.io.File
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

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSessionStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val userIdKey =
        stringPreferencesKey("user_id")

    private val activeOrganizationIdKey =
        stringPreferencesKey(
            "active_organization_id"
        )

    private val authorizedOrganizationsKey =
        stringPreferencesKey(
            "authorized_organizations_json"
        )

    private val isOfflineCapableKey =
        booleanPreferencesKey(
            "is_offline_capable"
        )

    @Test
    fun `updateSession stores credentials and authorized organization context`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory
                    .create(
                        scope = this,
                        produceFile = {
                            File(
                                tmpFolder
                                    .newFolder(),
                                "test1.preferences_pb"
                            )
                        }
                    )

            val secureStore =
                InMemorySecureCredentialStore()

            val store =
                DataStoreSessionStore(
                    dataStore,
                    secureStore
                )

            val state =
                SessionState(
                    userId = "user1",
                    token =
                        "access-token",
                    refreshToken =
                        "refresh-token",
                    activeOrganizationId =
                        "org-1",
                    authorizedOrganizations =
                        listOf(
                            organizationContext(
                                organizationId =
                                    "org-1"
                            )
                        ),
                    isOfflineCapable = true
                )

            store.updateSession(state)

            val current =
                store.sessionState.first()

            assertEquals(
                "user1",
                current.userId
            )
            assertEquals(
                "access-token",
                current.token
            )
            assertEquals(
                "refresh-token",
                current.refreshToken
            )
            assertEquals(
                "org-1",
                current.activeOrganizationId
            )
            assertEquals(
                listOf(
                    organizationContext(
                        organizationId =
                            "org-1"
                    )
                ),
                current
                    .authorizedOrganizations
            )
            assertTrue(
                current.isOfflineCapable
            )

            val prefs =
                dataStore.data.first()

            assertEquals(
                "user1",
                prefs[userIdKey]
            )
            assertEquals(
                "org-1",
                prefs[
                    activeOrganizationIdKey
                ]
            )
            assertTrue(
                !prefs[
                    authorizedOrganizationsKey
                ].isNullOrBlank()
            )
            assertTrue(
                prefs[
                    isOfflineCapableKey
                ] == true
            )
        }

    @Test
    fun `updateSession removes stale active organization when unresolved`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory
                    .create(
                        scope = this,
                        produceFile = {
                            File(
                                tmpFolder
                                    .newFolder(),
                                "test2.preferences_pb"
                            )
                        }
                    )

            val secureStore =
                InMemorySecureCredentialStore()

            val store =
                DataStoreSessionStore(
                    dataStore,
                    secureStore
                )

            store.updateSession(
                SessionState(
                    userId = "user1",
                    activeOrganizationId =
                        "org-stale",
                    authorizedOrganizations =
                        listOf(
                            organizationContext(
                                organizationId =
                                    "org-stale"
                            )
                        )
                )
            )

            store.updateSession(
                SessionState(
                    userId = "user1",
                    activeOrganizationId =
                        null,
                    authorizedOrganizations =
                        emptyList()
                )
            )

            val current =
                store.sessionState.first()

            assertNull(
                current.activeOrganizationId
            )
            assertTrue(
                current
                    .authorizedOrganizations
                    .isEmpty()
            )
        }

    @Test
    fun `sessionState reads credentials from secure store`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory
                    .create(
                        scope = this,
                        produceFile = {
                            File(
                                tmpFolder
                                    .newFolder(),
                                "test3.preferences_pb"
                            )
                        }
                    )

            val secureStore =
                InMemorySecureCredentialStore()

            val store =
                DataStoreSessionStore(
                    dataStore,
                    secureStore
                )

            secureStore.saveCredentials(
                accessToken =
                    "access-token",
                refreshToken =
                    "refresh-token"
            )

            val current =
                store.sessionState.first()

            assertEquals(
                "access-token",
                current.token
            )
            assertEquals(
                "refresh-token",
                current.refreshToken
            )
        }

    @Test
    fun `clearSession clears authorization context and secure credentials`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory
                    .create(
                        scope = this,
                        produceFile = {
                            File(
                                tmpFolder
                                    .newFolder(),
                                "test4.preferences_pb"
                            )
                        }
                    )

            val secureStore =
                InMemorySecureCredentialStore()

            val store =
                DataStoreSessionStore(
                    dataStore,
                    secureStore
                )

            store.updateSession(
                SessionState(
                    userId = "user1",
                    token =
                        "access-token",
                    refreshToken =
                        "refresh-token",
                    activeOrganizationId =
                        "org-1",
                    authorizedOrganizations =
                        listOf(
                            organizationContext(
                                organizationId =
                                    "org-1"
                            )
                        ),
                    isOfflineCapable = true
                )
            )

            store.clearSession()

            val current =
                store.sessionState.first()

            assertNull(current.userId)
            assertNull(current.token)
            assertNull(
                current.refreshToken
            )
            assertNull(
                current.activeOrganizationId
            )
            assertTrue(
                current
                    .authorizedOrganizations
                    .isEmpty()
            )
            assertFalse(
                current.isOfflineCapable
            )

            assertNull(
                secureStore.getAccessToken()
            )
            assertNull(
                secureStore.getRefreshToken()
            )

            val prefs =
                dataStore.data.first()

            assertNull(prefs[userIdKey])
            assertNull(
                prefs[
                    activeOrganizationIdKey
                ]
            )
            assertNull(
                prefs[
                    authorizedOrganizationsKey
                ]
            )
            assertNull(
                prefs[
                    isOfflineCapableKey
                ]
            )
        }

    private fun organizationContext(
        organizationId: String
    ) =
        AuthorizedOrganizationContext(
            membershipId =
                "membership-$organizationId",
            organizationId =
                organizationId,
            organizationName =
                "Organization $organizationId",
            organizationCode =
                "CODE",
            role = "ADMIN"
        )
}
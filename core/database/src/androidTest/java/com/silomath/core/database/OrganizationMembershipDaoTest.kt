package com.silomath.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.entity.OrganizationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationMembershipDaoTest {

    private lateinit var database: SiloMathDatabase

    private val organizationId =
        "11111111-1111-4111-8111-111111111111"

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                SiloMathDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activeCanonicalMemberships_includeOnlyAuthoritativeActiveRows() =
        runTest {
            val legacyOrganizationId =
                insertOrganization()

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "active-admin",
                membershipId =
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                role =
                    "ADMIN",
                lifecycleStatus =
                    "ACTIVE",
                entityVersion =
                    1L
            )

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "active-standard",
                membershipId =
                    "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                role =
                    "STANDARD",
                lifecycleStatus =
                    "ACTIVE",
                entityVersion =
                    2L
            )

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "disabled-admin",
                membershipId =
                    "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
                role =
                    "ADMIN",
                lifecycleStatus =
                    "DISABLED",
                entityVersion =
                    3L
            )

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "missing-membership-id",
                membershipId =
                    null,
                role =
                    "ADMIN",
                lifecycleStatus =
                    "ACTIVE",
                entityVersion =
                    1L
            )

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "missing-version",
                membershipId =
                    "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
                role =
                    "ADMIN",
                lifecycleStatus =
                    "ACTIVE",
                entityVersion =
                    null
            )

            val result =
                database
                    .organizationMembershipDao()
                    .observeActiveCanonicalMemberships(
                        organizationId
                    )
                    .first()

            assertEquals(
                listOf(
                    "active-admin",
                    "active-standard"
                ),
                result.map { it.userId }
            )
        }

    @Test
    fun authoritativeCount_includesDisabledCanonicalMembership() =
        runTest {
            val legacyOrganizationId =
                insertOrganization()

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "disabled-admin",
                membershipId =
                    "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee",
                role =
                    "ADMIN",
                lifecycleStatus =
                    "DISABLED",
                entityVersion =
                    4L
            )

            val count =
                database
                    .organizationMembershipDao()
                    .countAuthoritativeCanonicalMemberships(
                        organizationId
                    )

            assertEquals(
                1,
                count
            )
        }

    @Test
    fun canonicalMembership_preservesDisabledHistoricalIdentity() =
        runTest {
            val legacyOrganizationId =
                insertOrganization()

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "disabled-standard",
                membershipId =
                    "ffffffff-ffff-4fff-8fff-ffffffffffff",
                role =
                    "STANDARD",
                lifecycleStatus =
                    "DISABLED",
                entityVersion =
                    7L
            )

            val membership =
                database
                    .organizationMembershipDao()
                    .getCanonicalMembership(
                        organizationId =
                            organizationId,
                        userId =
                            "disabled-standard"
                    )

            assertNotNull(
                membership
            )

            assertEquals(
                "DISABLED",
                membership?.lifecycleStatus
            )

            assertEquals(
                "STANDARD",
                membership?.role
            )
        }

    @Test
    fun incompleteMembership_isNotCanonical() =
        runTest {
            val legacyOrganizationId =
                insertOrganization()

            insertMembership(
                legacyOrganizationId =
                    legacyOrganizationId,
                userId =
                    "incomplete-user",
                membershipId =
                    null,
                role =
                    "ADMIN",
                lifecycleStatus =
                    "ACTIVE",
                entityVersion =
                    1L
            )

            val membership =
                database
                    .organizationMembershipDao()
                    .getCanonicalMembership(
                        organizationId =
                            organizationId,
                        userId =
                            "incomplete-user"
                    )

            assertNull(
                membership
            )
        }

    private suspend fun insertOrganization(): Long {
        return database
            .organizationDao()
            .insertOrganization(
                OrganizationEntity(
                    organizationId =
                        organizationId,
                    name =
                        "Test Organization",
                    code =
                        "TEST",
                    lifecycleStatus =
                        "ACTIVE",
                    entityVersion =
                        1L
                )
            )
    }

    private fun insertMembership(
        legacyOrganizationId: Long,
        userId: String,
        membershipId: String?,
        role: String?,
        lifecycleStatus: String?,
        entityVersion: Long?
    ) {
        database.openHelper
            .writableDatabase
            .execSQL(
                """
                INSERT INTO organization_memberships (
                    legacy_organization_id,
                    organization_id,
                    user_id,
                    legacy_role,
                    role,
                    membership_id,
                    lifecycle_status,
                    created_at,
                    created_by,
                    updated_at,
                    updated_by,
                    entity_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    legacyOrganizationId,
                    organizationId,
                    userId,
                    "Standard User",
                    role,
                    membershipId,
                    lifecycleStatus,
                    1_700_000_000_000L,
                    "system",
                    1_700_000_000_000L,
                    "system",
                    entityVersion
                )
            )
    }
}
package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.common.UuidUtils
import com.silomath.core.database.di.MIGRATION_13_14
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationMembershipMigrationTest {

    private val testDb = "migration_13_14_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration13To14_migratesOrganizationMembershipsCorrectly() {
        val validUuid = UuidUtils.generateUuid()
        var db = helper.createDatabase(testDb, 13)
        // Insert organizations: id 1 has valid organization_id (UUID), id 2 has null organization_id
        db.execSQL("INSERT INTO organizations (id, name, organization_id, lifecycle_status) VALUES (1, 'Org 1', '$validUuid', 'ACTIVE')")
        db.execSQL("INSERT INTO organizations (id, name, organization_id, lifecycle_status) VALUES (2, 'Org 2', NULL, 'ACTIVE')")

        // Insert organization members:
        // Member 1: org 1, Admin -> role ADMIN
        db.execSQL("INSERT INTO organization_members (organization_id, user_id, role) VALUES (1, 'usr-1', 'Admin')")
        // Member 2: org 1, Standard User -> role STANDARD
        db.execSQL("INSERT INTO organization_members (organization_id, user_id, role) VALUES (1, 'usr-2', 'Standard User')")
        // Member 3: org 2 (null organization_id), Standard -> role STANDARD
        db.execSQL("INSERT INTO organization_members (organization_id, user_id, role) VALUES (2, 'usr-3', 'Standard')")
        // Member 4: org 1, unknown role -> role NULL
        db.execSQL("INSERT INTO organization_members (organization_id, user_id, role) VALUES (1, 'usr-4', 'SuperUser')")
        db.close()

        // Run migration 13 -> 14 and validate schema version 14
        db = helper.runMigrationsAndValidate(testDb, 14, true, MIGRATION_13_14)

        // Verify total memberships count = 4
        val countCursor = db.query("SELECT COUNT(*) FROM organization_memberships")
        assertTrue(countCursor.moveToFirst())
        assertEquals(4, countCursor.getInt(0))
        countCursor.close()

        // Verify legacy organization_members table still exists and has original rows
        val legacyCount = db.query("SELECT COUNT(*) FROM organization_members")
        assertTrue(legacyCount.moveToFirst())
        assertEquals(4, legacyCount.getInt(0))
        legacyCount.close()

        // Verify organization_memberships FK delete actions are RESTRICT/NO ACTION
        val pragmaCursor = db.query("PRAGMA foreign_key_list(organization_memberships)")
        var fkCount = 0
        while (pragmaCursor.moveToNext()) {
            val onDelete = pragmaCursor.getString(6)
            assertTrue(onDelete.equals("RESTRICT", ignoreCase = true) || onDelete.equals("NO ACTION", ignoreCase = true))
            fkCount++
        }
        pragmaCursor.close()
        assertEquals(2, fkCount)

        // Verify Member 1 (Admin -> ADMIN, resolved UUID)
        val m1 = db.query("SELECT legacy_organization_id, organization_id, user_id, legacy_role, role, membership_id, lifecycle_status, created_at, created_by, updated_at, updated_by, entity_version FROM organization_memberships WHERE legacy_organization_id = 1 AND user_id = 'usr-1'")
        assertTrue(m1.moveToFirst())
        assertEquals(1L, m1.getLong(0))
        assertEquals(validUuid, m1.getString(1))
        assertEquals("usr-1", m1.getString(2))
        assertEquals("Admin", m1.getString(3))
        assertEquals("ADMIN", m1.getString(4))
        assertNull(m1.getString(5)) // membership_id NULL
        assertNull(m1.getString(6)) // lifecycle_status NULL
        assertTrue(m1.isNull(7)) // created_at NULL
        assertTrue(m1.isNull(8)) // created_by NULL
        assertTrue(m1.isNull(9)) // updated_at NULL
        assertTrue(m1.isNull(10)) // updated_by NULL
        assertTrue(m1.isNull(11)) // entity_version NULL
        m1.close()

        // Verify Member 3 (Unresolved organization UUID -> organization_id NULL)
        val m3 = db.query("SELECT legacy_organization_id, organization_id, user_id, legacy_role, role FROM organization_memberships WHERE legacy_organization_id = 2 AND user_id = 'usr-3'")
        assertTrue(m3.moveToFirst())
        assertEquals(2L, m3.getLong(0))
        assertNull(m3.getString(1)) // organization_id is NULL because parent organization_id was NULL
        assertEquals("usr-3", m3.getString(2))
        assertEquals("Standard", m3.getString(3))
        assertEquals("STANDARD", m3.getString(4))
        m3.close()

        // Verify Member 4 (Unknown role 'SuperUser' -> role NULL)
        val m4 = db.query("SELECT legacy_role, role FROM organization_memberships WHERE legacy_organization_id = 1 AND user_id = 'usr-4'")
        assertTrue(m4.moveToFirst())
        assertEquals("SuperUser", m4.getString(0))
        assertNull(m4.getString(1)) // unknown role maps to NULL
        m4.close()

        db.close()
    }
}

package com.silomath.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.common.UuidUtils
import com.silomath.core.database.di.MIGRATION_12_13
import com.silomath.core.database.di.createOrganizationTriggers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationMigrationTest {

    private val testDb = "migration_12_13_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration12To13_preservesOrganizationsAndMapsValidUuid() {
        val validUuid = UuidUtils.generateUuid()
        var db = helper.createDatabase(testDb, 12)
        // Row 1: Valid remoteId and remoteRevision
        db.execSQL("INSERT INTO organizations (id, name, remoteId, lifecycleStatus, remoteRevision) VALUES (1, 'Org Alpha', '$validUuid', 'ACTIVE', 5)")
        // Row 2: Null remoteId and null remoteRevision
        db.execSQL("INSERT INTO organizations (id, name, remoteId, lifecycleStatus, remoteRevision) VALUES (2, 'Org Beta', NULL, 'INACTIVE', NULL)")
        // Add a child location referencing legacy id = 1
        db.execSQL("INSERT INTO locations (id, organization_id, code, name, lifecycleStatus) VALUES (10, 1, 'LOC-1', 'Location 1', 'ACTIVE')")
        db.close()

        // Run MIGRATION_12_13 and validate schema version 13
        db = helper.runMigrationsAndValidate(testDb, 13, true, MIGRATION_12_13)

        // Verify exactly two organization rows exist
        val countCursor = db.query("SELECT COUNT(*) FROM organizations")
        assertTrue(countCursor.moveToFirst())
        assertEquals(2, countCursor.getInt(0))
        countCursor.close()

        // Verify Row 1 migration
        val cursor1 = db.query("SELECT id, organization_id, name, lifecycle_status, entity_version FROM organizations WHERE id = 1")
        assertTrue(cursor1.moveToFirst())
        assertEquals(1L, cursor1.getLong(0))
        assertEquals(validUuid, cursor1.getString(1))
        assertEquals("Org Alpha", cursor1.getString(2))
        assertEquals("ACTIVE", cursor1.getString(3))
        assertEquals(5L, cursor1.getLong(4))
        cursor1.close()

        // Verify Row 2 migration (null remoteId -> null organization_id, null remoteRevision -> null entity_version)
        val cursor2 = db.query("SELECT id, organization_id, name, lifecycle_status, entity_version FROM organizations WHERE id = 2")
        assertTrue(cursor2.moveToFirst())
        assertEquals(2L, cursor2.getLong(0))
        assertNull(cursor2.getString(1))
        assertEquals("Org Beta", cursor2.getString(2))
        assertEquals("INACTIVE", cursor2.getString(3))
        assertTrue(cursor2.isNull(4))
        cursor2.close()

        // Verify child location referencing legacy id = 1 remains valid
        val locCursor = db.query("SELECT name FROM locations WHERE organization_id = 1")
        assertTrue(locCursor.moveToFirst())
        assertEquals("Location 1", locCursor.getString(0))
        locCursor.close()

        db.close()
    }

    @Test
    fun migration12To13_invalidNonUuidRemoteIdBecomesNull() {
        val nonUuid36 = "org-1000000000000000000000000000000001" // 36 chars but non-hex
        var db = helper.createDatabase(testDb, 12)
        db.execSQL("INSERT INTO organizations (id, name, remoteId, lifecycleStatus, remoteRevision) VALUES (1, 'Org NonUuid', '$nonUuid36', 'ACTIVE', 1)")
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 13, true, MIGRATION_12_13)

        val cursor = db.query("SELECT organization_id FROM organizations WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertNull(cursor.getString(0)) // Invalid 36-char non-UUID mapped to NULL
        cursor.close()

        db.close()
    }

    @Test
    fun migratedDb13_enforcesPhysicalConstraintsOnInsertAndUpdate() {
        val validUuid1 = UuidUtils.generateUuid()
        val validUuid2 = UuidUtils.generateUuid()

        var db = helper.createDatabase(testDb, 12)
        db.execSQL("INSERT INTO organizations (id, name, remoteId, lifecycleStatus, remoteRevision) VALUES (1, 'Org 1', '$validUuid1', 'ACTIVE', 1)")
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 13, true, MIGRATION_12_13)

        // Invalid UUID format on INSERT and UPDATE
        try {
            db.execSQL("INSERT INTO organizations (name, organization_id) VALUES ('Bad Uuid', 'not-a-uuid')")
            fail("Expected SQLiteConstraintException for invalid UUID on INSERT")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET organization_id = 'bad-uuid-update' WHERE id = 1")
            fail("Expected SQLiteConstraintException for invalid UUID on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Untrimmed name on INSERT and UPDATE
        try {
            db.execSQL("INSERT INTO organizations (name) VALUES ('  Untrimmed Name  ')")
            fail("Expected SQLiteConstraintException for untrimmed name on INSERT")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET name = '  Untrimmed  ' WHERE id = 1")
            fail("Expected SQLiteConstraintException for untrimmed name on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Blank name on INSERT
        try {
            db.execSQL("INSERT INTO organizations (name) VALUES ('')")
            fail("Expected SQLiteConstraintException for blank name on INSERT")
        } catch (_: SQLiteConstraintException) {}

        // Name length > 150 on INSERT
        val longName = "N".repeat(151)
        try {
            db.execSQL("INSERT INTO organizations (name) VALUES ('$longName')")
            fail("Expected SQLiteConstraintException for name length > 150 on INSERT")
        } catch (_: SQLiteConstraintException) {}

        // Lowercase code rejected
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org Code', 'org-c')")
            fail("Expected SQLiteConstraintException for lowercase code")
        } catch (_: SQLiteConstraintException) {}

        // Code with leading/trailing whitespace rejected
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org Code', ' ORG-C ')")
            fail("Expected SQLiteConstraintException for whitespace code")
        } catch (_: SQLiteConstraintException) {}

        // Code length > 50 rejected
        val longCode = "C".repeat(51)
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org Code', '$longCode')")
            fail("Expected SQLiteConstraintException for code length > 50")
        } catch (_: SQLiteConstraintException) {}

        // Oversize address > 250
        val longAddress = "A".repeat(251)
        try {
            db.execSQL("INSERT INTO organizations (name, address) VALUES ('Org Addr', '$longAddress')")
            fail("Expected SQLiteConstraintException for oversize address")
        } catch (_: SQLiteConstraintException) {}

        // Oversize city > 100
        val longCity = "C".repeat(101)
        try {
            db.execSQL("INSERT INTO organizations (name, city) VALUES ('Org City', '$longCity')")
            fail("Expected SQLiteConstraintException for oversize city")
        } catch (_: SQLiteConstraintException) {}

        // Duplicate organization_id
        db.execSQL("INSERT INTO organizations (name, organization_id) VALUES ('Org A', '$validUuid2')")
        try {
            db.execSQL("UPDATE organizations SET organization_id = '$validUuid2' WHERE id = 1")
            fail("Expected SQLiteConstraintException for duplicate organization_id on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Duplicate code
        db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org C', 'ORG-C')")
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org D', 'ORG-C')")
            fail("Expected SQLiteConstraintException for duplicate code")
        } catch (_: SQLiteConstraintException) {}

        // Invalid lifecycle_status on INSERT and UPDATE
        try {
            db.execSQL("INSERT INTO organizations (name, lifecycle_status) VALUES ('Bad Status', 'DELETED')")
            fail("Expected SQLiteConstraintException for invalid lifecycle_status on INSERT")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET lifecycle_status = 'LOCKED' WHERE id = 1")
            fail("Expected SQLiteConstraintException for invalid lifecycle_status on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Invalid entity_version < 1 on INSERT and UPDATE
        try {
            db.execSQL("INSERT INTO organizations (name, entity_version) VALUES ('Bad Version', 0)")
            fail("Expected SQLiteConstraintException for entity_version < 1 on INSERT")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET entity_version = 0 WHERE id = 1")
            fail("Expected SQLiteConstraintException for entity_version < 1 on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        db.close()
    }

    @Test
    fun freshDb13_enforcesPhysicalConstraintsParityWithoutTestCreatedTriggers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("fresh_db13_parity_test.db")

        // Build database using production callback mechanism
        val roomDb = Room.databaseBuilder(
            context,
            SiloMathDatabase::class.java,
            "fresh_db13_parity_test.db"
        )
        .addMigrations(MIGRATION_12_13)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createOrganizationTriggers(db)
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                createOrganizationTriggers(db)
            }
        })
        .allowMainThreadQueries()
        .build()

        val db = roomDb.openHelper.writableDatabase

        // Invalid UUID format on INSERT and UPDATE
        try {
            db.execSQL("INSERT INTO organizations (name, organization_id) VALUES ('Bad Uuid', 'not-a-uuid')")
            fail("Expected SQLiteConstraintException on fresh DB13 for invalid UUID on INSERT")
        } catch (_: SQLiteConstraintException) {}

        db.execSQL("INSERT INTO organizations (name, id) VALUES ('Org 1', 100)")
        try {
            db.execSQL("UPDATE organizations SET organization_id = 'not-a-uuid' WHERE id = 100")
            fail("Expected SQLiteConstraintException on fresh DB13 for invalid UUID on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Untrimmed / blank / >150 name
        try {
            db.execSQL("INSERT INTO organizations (name) VALUES ('  Name  ')")
            fail("Expected SQLiteConstraintException on fresh DB13 for untrimmed name")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("INSERT INTO organizations (name) VALUES ('')")
            fail("Expected SQLiteConstraintException on fresh DB13 for blank name on INSERT")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET name = '  Untrimmed  ' WHERE id = 100")
            fail("Expected SQLiteConstraintException on fresh DB13 for untrimmed name on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        val longName = "N".repeat(151)
        try {
            db.execSQL("INSERT INTO organizations (name) VALUES ('$longName')")
            fail("Expected SQLiteConstraintException on fresh DB13 for name > 150")
        } catch (_: SQLiteConstraintException) {}

        // Lowercase / whitespace / >50 code
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org', 'abc')")
            fail("Expected SQLiteConstraintException on fresh DB13 for lowercase code")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org', ' ABC ')")
            fail("Expected SQLiteConstraintException on fresh DB13 for whitespace code")
        } catch (_: SQLiteConstraintException) {}

        val longCode = "C".repeat(51)
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org', '$longCode')")
            fail("Expected SQLiteConstraintException on fresh DB13 for code > 50")
        } catch (_: SQLiteConstraintException) {}

        // Address > 250
        val longAddress = "A".repeat(251)
        try {
            db.execSQL("INSERT INTO organizations (name, address) VALUES ('Org', '$longAddress')")
            fail("Expected SQLiteConstraintException on fresh DB13 for address > 250")
        } catch (_: SQLiteConstraintException) {}

        // City > 100
        val longCity = "C".repeat(101)
        try {
            db.execSQL("INSERT INTO organizations (name, city) VALUES ('Org', '$longCity')")
            fail("Expected SQLiteConstraintException on fresh DB13 for city > 100")
        } catch (_: SQLiteConstraintException) {}

        // Invalid lifecycle
        try {
            db.execSQL("INSERT INTO organizations (name, lifecycle_status) VALUES ('Org', 'PENDING')")
            fail("Expected SQLiteConstraintException on fresh DB13 for invalid lifecycle")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET lifecycle_status = 'PENDING' WHERE id = 100")
            fail("Expected SQLiteConstraintException on fresh DB13 for invalid lifecycle on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Entity version < 1
        try {
            db.execSQL("INSERT INTO organizations (name, entity_version) VALUES ('Org', 0)")
            fail("Expected SQLiteConstraintException on fresh DB13 for entity_version < 1")
        } catch (_: SQLiteConstraintException) {}

        try {
            db.execSQL("UPDATE organizations SET entity_version = 0 WHERE id = 100")
            fail("Expected SQLiteConstraintException on fresh DB13 for entity_version < 1 on UPDATE")
        } catch (_: SQLiteConstraintException) {}

        // Duplicate organization_id
        val validUuid = UuidUtils.generateUuid()
        db.execSQL("INSERT INTO organizations (name, organization_id) VALUES ('Org A', '$validUuid')")
        try {
            db.execSQL("INSERT INTO organizations (name, organization_id) VALUES ('Org B', '$validUuid')")
            fail("Expected SQLiteConstraintException on fresh DB13 for duplicate organization_id")
        } catch (_: SQLiteConstraintException) {}

        // Duplicate code
        db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org W', 'UNIQ')")
        try {
            db.execSQL("INSERT INTO organizations (name, code) VALUES ('Org Z', 'UNIQ')")
            fail("Expected SQLiteConstraintException on fresh DB13 for duplicate code")
        } catch (_: SQLiteConstraintException) {}

        roomDb.close()
    }
}

package com.silomath.core.database.migration

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.silomath.core.database.offline.CanonicalOfflineGuards

/** Additive only. Existing legacy identities, numeric values and outboxes are not migrated implicitly. */
class CanonicalOfflineExpand25To26 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        CanonicalOfflineGuards.install(db)
    }
}

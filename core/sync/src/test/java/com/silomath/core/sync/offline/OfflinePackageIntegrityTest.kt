package com.silomath.core.sync.offline

import com.silomath.core.data.network.dto.OfflineManifestDto
import com.silomath.core.database.entity.OfflinePackageDownloadEntity
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class OfflinePackageIntegrityTest {
    private val json=Json { encodeDefaults=true }
    private fun hash(bytes: ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private data class Fixture(val record: OfflinePackageDownloadEntity,val bytes: ByteArray)
    private fun fixture(edit: (JsonObject)->JsonObject={it},countsWrong: Boolean=false,pinnedWrong: Boolean=false): Fixture {
        val snapshot=buildJsonObject {
            put("snapshot_schema_version","snapshot-v1");put("calculation_engine_version","engine-v1")
            put("calculation_schema_versions",buildJsonArray { add("LEVEL_BASED_V1") })
        }
        val tables=buildJsonObject { OfflinePackageIntegrityVerifier.requiredTables.forEach { name ->
            put(name,if(name=="campaign_snapshots") buildJsonArray { add(snapshot) } else JsonArray(emptyList()))
        }}
        val root=edit(buildJsonObject {
            put("formatVersion",2);put("manifestId","manifest");put("organizationId","org");put("campaignId","campaign")
            put("deviceInstallationId","device");put("authorizedUserId","user");put("packageVersion","version")
            put("generatedAt","2026-10-27T00:00:00Z");put("tables",tables)
        })
        val bytes=root.toString().toByteArray()
        val inventory=buildJsonObject { tables.forEach { (name,rows) -> put(name,buildJsonObject {
            put("count",if(countsWrong) 9 else rows.jsonArray.size);put("sha256",hash(rows.toString().toByteArray()))
        }) }}
        val metadata=buildJsonObject {
            put("formatVersion",2);put("authorizedUserId","user");put("byteSize",bytes.size)
            put("snapshot_schema_version",if(pinnedWrong) "other" else "snapshot-v1");put("calculation_engine_version","engine-v1")
            put("calculation_schema_versions",buildJsonArray { add("LEVEL_BASED_V1") })
        }
        val manifest=OfflineManifestDto("manifest","org","campaign","device","version",inventory,hash(bytes),"SHA-256",metadata,"2026-10-27T00:00:00Z")
        val record=OfflinePackageDownloadEntity("manifest","campaign","org","device","user","version","VERIFYING",json.encodeToString(manifest),"staged",bytes.size.toLong(),null,null,null,1)
        return Fixture(record,bytes)
    }
    private fun staging(bytes: ByteArray)=object : PackageStagingStore {
        override suspend fun write(manifestId: String,source: java.io.InputStream,expectedBytes: Long): StagedPackage=error("Not used")
        override fun open(privateRef: String)=ByteArrayInputStream(bytes)
    }
    private suspend fun verifies(f: Fixture)=OfflinePackageIntegrityVerifier(staging(f.bytes)).verify(f.record)

    @Test fun completeInventoryPassesWithoutActivating()=runBlocking {
        val f=fixture();val result=verifies(f)
        assertEquals("user",result.authorizedUserId)
        assertEquals(OfflinePackageIntegrityVerifier.requiredTables,result.tables.keys)
        assertNull(f.record.activatedAt)
    }
    @Test fun corruptionFailsWholeArtifactHash()=runBlocking<Unit> {
        val f=fixture()
        try { OfflinePackageIntegrityVerifier(staging(f.bytes+byteArrayOf(0))).verify(f.record);fail() } catch(_: IllegalArgumentException) {}
    }
    @Test fun wrongUserFailsEvenWithMatchingArtifactHash()=runBlocking<Unit> {
        val f=fixture(edit={JsonObject(it+ ("authorizedUserId" to JsonPrimitive("other-user")))})
        try { verifies(f);fail() } catch(_: IllegalArgumentException) {}
    }
    @Test fun collectionCountMismatchFails()=runBlocking<Unit> {
        try { verifies(fixture(countsWrong=true));fail() } catch(_: IllegalArgumentException) {}
    }
    @Test fun missingCollectionFails()=runBlocking<Unit> {
        val f=fixture(edit={ root -> JsonObject(root+("tables" to JsonObject(root.getValue("tables").jsonObject-"evidence"))) })
        try { verifies(f);fail() } catch(_: IllegalArgumentException) {}
    }
    @Test fun mismatchPinnedMetadataFails()=runBlocking<Unit> {
        try { verifies(fixture(pinnedWrong=true));fail() } catch(_: IllegalArgumentException) {}
    }
    @Test fun verifyingCannotBeSkipped()=runBlocking<Unit> {
        val f=fixture()
        try { OfflinePackageIntegrityVerifier(staging(f.bytes)).verify(f.record.copy(state="DOWNLOADING"));fail() } catch(_: IllegalArgumentException) {}
    }
}

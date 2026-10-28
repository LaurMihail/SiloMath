package com.silomath.core.sync.offline

import com.silomath.core.data.network.dto.OfflineManifestDto
import com.silomath.core.database.entity.OfflinePackageDownloadEntity
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.*

/** Crosses the real PostgreSQL/backend/client serialization boundary with test-only source data. */
class CanonicalPackageContractTest {
    @Test fun backendPackagePreparesWithoutLegacyConversion()=runBlocking {
        val path=System.getenv("SILOMATH_OFFLINE_TEST_FIXTURE_OUT")
        assumeTrue("Requires backend-generated disposable test fixture",path!=null)
        val wrapper=Json.parseToJsonElement(Files.readString(Path.of(path!!))).jsonObject
        val manifest=Json.decodeFromJsonElement<OfflineManifestDto>(wrapper.getValue("manifest"))
        val bytes=wrapper.getValue("package").jsonPrimitive.content.toByteArray(Charsets.UTF_8)
        val user=manifest.verificationMetadata!!.getValue("authorizedUserId").jsonPrimitive.content
        val record=OfflinePackageDownloadEntity(manifest.manifestId,manifest.campaignId,manifest.organizationId,
            manifest.deviceInstallationId,user,manifest.packageVersion,"VERIFYING",Json.encodeToString(manifest),"fixture",
            bytes.size.toLong(),null,null,null,1)
        val staging=object : PackageStagingStore {
            override suspend fun write(manifestId: String,source: java.io.InputStream,expectedBytes: Long): StagedPackage=error("Not used")
            override fun open(privateRef: String)=ByteArrayInputStream(bytes)
        }
        val verified=OfflinePackageIntegrityVerifier(staging).verify(record)
        val prepared=OfflineImportPlanner.prepare(verified)
        assertEquals("45.123456",prepared.plan.rows.getValue("locations").single()["latitude_deg"])
        assertEquals("26.123456",prepared.plan.rows.getValue("locations").single()["longitude_deg"])
        assertEquals(user,prepared.plan.rows.getValue("device_installations").single()["user_id"])
        assertEquals("2.000000",prepared.plan.rows.getValue("geometry_profile_versions").single()["diameter_m"])
        assertNull(record.activatedAt)
    }
}

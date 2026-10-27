package com.silomath.backend.offline

import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import org.junit.Before
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.*

/** Executes against an explicitly supplied disposable PostgreSQL schema, never a production URL. */
class JdbcOfflinePackageServiceTest {
    @get:Rule val temporary = TemporaryFolder()
    private val url = System.getenv("SILOMATH_OFFLINE_TEST_DATABASE_URL") ?: ""
    @Before fun requireDisposableDatabase() {
        assumeTrue("Requires explicit disposable local PostgreSQL", url.startsWith("jdbc:postgresql://127.0.0.1:"))
    }
    private val user = id()
    private val org = id()
    private val campaign = id()
    private val device = id()
    private val directory get() = temporary.root.toPath().resolve("packages")
    private fun service(path: java.nio.file.Path=directory)=JdbcOfflinePackageService(url,"silomath_test",null,path)
    private fun connection()=DriverManager.getConnection(url,"silomath_test",null)

    @Test fun `complete scoped package matches durable manifest and exact coordinates`() = runBlocking {
        fixture()
        val result=service().current(user,org,campaign,device,null) as OfflineManifestResult.Found
        val manifest=result.manifest
        val downloaded=service().download(user,org,campaign,device,manifest.packageVersion,manifest.manifestId) as OfflinePackageResult.Found
        assertEquals(manifest.integrityHash,OfflinePackageEncoder.sha256(downloaded.artifact.bytes))
        val body=Json.parseToJsonElement(downloaded.artifact.bytes.toString(Charsets.UTF_8)).jsonObject
        val tables=body.getValue("tables").jsonObject
        assertEquals(OfflinePackageEncoder.requiredTables,tables.keys)
        assertEquals("45.123456",tables.getValue("locations").jsonArray.single().jsonObject.getValue("latitude_deg").jsonPrimitive.content)
        assertEquals(org,tables.getValue("organizations").jsonArray.single().jsonObject.getValue("organization_id").jsonPrimitive.content)
        assertEquals("LEVEL_BASED_V1",manifest.verificationMetadata!!.getValue("calculation_schema_versions").jsonArray.single().jsonPrimitive.content)
        assertFalse(body.containsKey("offlineReady"))
        assertEquals("10.000000",tables.getValue("geometry_profile_versions").jsonArray.single().jsonObject.getValue("height_m").jsonPrimitive.content)
        assertEquals(manifest.contentManifest.getValue("campaign_snapshot_scope_items").jsonObject.getValue("sha256").jsonPrimitive.content,
            OfflinePackageEncoder.sha256(tables.getValue("campaign_snapshot_scope_items").toString().toByteArray(Charsets.UTF_8)))
        System.getenv("SILOMATH_OFFLINE_TEST_FIXTURE_OUT")?.let { path ->
            val json=Json { encodeDefaults=true }
            java.nio.file.Files.writeString(java.nio.file.Path.of(path),buildJsonObject {
                put("manifest",json.encodeToJsonElement(manifest))
                put("package",downloaded.artifact.bytes.toString(Charsets.UTF_8))
            }.toString())
        }
        assertEquals(OfflinePackageResult.NotFound,service().download(user,org,campaign,device,"wrong-version",manifest.manifestId))
    }

    @Test fun `revoked installation and campaign access deny package download`() = runBlocking {
        fixture()
        val m=(service().current(user,org,campaign,device,null) as OfflineManifestResult.Found).manifest
        connection().use { c -> c.prepareStatement("update silomath.device_installations set revoked_at=now() where device_installation_id=?::uuid").use {
            it.setString(1,device);it.executeUpdate()
        }}
        assertEquals(OfflinePackageResult.Forbidden,service().download(user,org,campaign,device,m.packageVersion,m.manifestId))
        assertEquals(OfflineManifestResult.Forbidden,service().current(user,org,campaign,device,null))
    }

    @Test fun `other tenant and other user cannot obtain manifest`() = runBlocking {
        fixture()
        assertEquals(OfflineManifestResult.Forbidden,service().current(user,id(),campaign,device,null))
        assertEquals(OfflineManifestResult.Forbidden,service().current(id(),org,campaign,device,null))
    }

    @Test fun `failed package persistence preserves prior current manifest`() = runBlocking {
        fixture()
        val m=(service().current(user,org,campaign,device,null) as OfflineManifestResult.Found).manifest
        val blocked=temporary.newFile("not-a-directory").toPath()
        assertFails { service(blocked).current(user,org,campaign,device,null) }
        val current=JdbcOfflineManifestService(url,"silomath_test",null).current(user,org,campaign,device,m.packageVersion) as OfflineManifestResult.Found
        assertEquals(m.manifestId,current.manifest.manifestId)
        assertTrue(Files.exists(directory.resolve(m.manifestId+".package")))
    }

    @Test fun `incomplete snapshot cannot publish manifest`() = runBlocking {
        fixture()
        connection().use { c -> c.prepareStatement("update silomath.campaign_snapshots set calculation_schema_versions=null where campaign_id=?::uuid").use {
            it.setString(1,campaign);it.executeUpdate()
        }}
        assertFails { service().current(user,org,campaign,device,null) }
        val count=connection().use { c -> c.prepareStatement("select count(*) from silomath.offline_working_set_manifests where campaign_id=?::uuid").use {
            it.setString(1,campaign);it.executeQuery().use { rows -> rows.next();rows.getInt(1) }
        }}
        assertEquals(0,count)
    }

    private fun fixture() = connection().use { c ->
        val location=id();val unit=id();val space=id();val family=id();val profile=id();val version=id()
        val snapshot=id();val commodity=id();val commodityVersion=id();val membership=id()
        insert(c,"users",mapOf("user_id" to user,"lifecycle_status" to "ACTIVE"))
        insert(c,"organizations",mapOf("organization_id" to org,"lifecycle_status" to "ACTIVE"))
        insert(c,"organization_memberships",mapOf("membership_id" to membership,"organization_id" to org,"user_id" to user,"role" to "ADMIN","lifecycle_status" to "ACTIVE"))
        insert(c,"inventory_campaigns",mapOf("campaign_id" to campaign,"organization_id" to org,"code" to "INV-2026-0001","campaign_year" to 2026,"sequence" to 1,"inventory_type" to "CONTROL","status" to "ACTIVE"))
        insert(c,"campaign_snapshots",mapOf("campaign_snapshot_id" to snapshot,"campaign_id" to campaign,"organization_id" to org,"inventory_type" to "CONTROL","calculation_engine_version" to "engine-v1","snapshot_schema_version" to "snapshot-v1","calculation_schema_versions" to "[\"LEVEL_BASED_V1\"]","organization_name" to "Test Organization","organization_code" to "TEST"))
        insert(c,"campaign_scopes",mapOf("campaign_id" to campaign,"storage_space_id" to space))
        insert(c,"campaign_snapshot_scope_items",mapOf("campaign_snapshot_id" to snapshot,"storage_space_id" to space,"location_id" to location,"storage_unit_id" to unit,"geometry_family_id" to family,"geometry_profile_id" to profile,"geometry_profile_version_id" to version,
            "location_code" to "LOC","location_name" to "Location","storage_unit_code" to "UNIT","storage_unit_name" to "Unit","storage_space_code" to "SPACE",
            "geometry_family_code" to "CYL_FLAT_V1","geometry_family_name" to "Cylinder","geometry_profile_code" to "PROFILE","geometry_profile_name" to "Profile",
            "geometry_formula_version" to "formula-v1","geometry_dimensions" to "{\"diameter_m\":2.000000,\"height_m\":10.000000}",
            "measurement_reference_height_m" to 10,"maximum_fill_height_m" to 10,"max_capacity_m3" to BigDecimal("31.415927")))
        insert(c,"locations",mapOf("location_id" to location,"organization_id" to org,"latitude_deg" to BigDecimal("45.123456"),"longitude_deg" to BigDecimal("26.123456")))
        insert(c,"storage_units",mapOf("storage_unit_id" to unit,"organization_id" to org,"location_id" to location))
        insert(c,"storage_spaces",mapOf("storage_space_id" to space,"organization_id" to org,"storage_unit_id" to unit,"geometry_profile_id" to profile))
        insert(c,"geometry_families",mapOf("geometry_family_id" to family,"family_code" to "CYL_FLAT_V1","formula_version" to "formula-v1"))
        insert(c,"geometry_profiles",mapOf("geometry_profile_id" to profile,"organization_id" to org,"geometry_family_id" to family,"current_version_id" to version))
        insert(c,"geometry_profile_versions",mapOf("geometry_profile_version_id" to version,"geometry_profile_id" to profile,"geometry_family_id" to family,"diameter_m" to 2,"height_m" to 10,"measurement_reference_height_m" to 10,"maximum_fill_height_m" to 10,"max_capacity_m3" to BigDecimal("31.415927"),"formula_version" to "formula-v1"))
        insert(c,"commodities",mapOf("commodity_id" to commodity,"current_version_id" to commodityVersion))
        insert(c,"commodity_versions",mapOf("commodity_version_id" to commodityVersion,"commodity_id" to commodity))
        insert(c,"campaign_snapshot_team_items",mapOf("campaign_snapshot_id" to snapshot,"user_id" to user,"organization_membership_id" to membership,"role_at_assignment" to "ADMIN","user_full_name" to "Test User","user_email" to "user@example.test"))
        insert(c,"campaign_snapshot_commodity_items",mapOf("campaign_snapshot_id" to snapshot,"commodity_id" to commodity,"commodity_version_id" to commodityVersion))
        c.prepareStatement("update silomath.inventory_campaigns set campaign_snapshot_id=?::uuid where campaign_id=?::uuid").use {
            it.setString(1,snapshot);it.setString(2,campaign);it.executeUpdate()
        }
    }

    /** Test-only defaults for required columns; production never fabricates records. */
    private fun insert(c: Connection,table: String,values: Map<String,Any>) {
        val columns=mutableListOf<Pair<String,String>>()
        c.prepareStatement("select column_name,udt_name,is_nullable from information_schema.columns where table_schema='silomath' and table_name=? order by ordinal_position").use {
            it.setString(1,table);it.executeQuery().use { rows -> while(rows.next()) {
                val name=rows.getString(1)
                if(rows.getString(3)=="NO" || name in values) columns+=name to rows.getString(2)
            }}
        }
        val sql="insert into silomath.$table ("+columns.joinToString(","){it.first}+") values ("+columns.joinToString(","){ "?::"+it.second }+")"
        c.prepareStatement(sql).use { statement ->
            columns.forEachIndexed { i,(name,type) ->
                val value=values[name] ?: when(type) {
                    "uuid" -> if(name.endsWith("_by")) user else id()
                    "int4","int8","numeric" -> 1
                    "timestamptz","timestamp" -> "2026-10-27T00:00:00Z"
                    "date" -> "2026-10-27"
                    "bool" -> false
                    "jsonb" -> "{}"
                    else -> if(name=="lifecycle_status") "ACTIVE" else "test"
                }
                statement.setString(i+1,value.toString())
            }
            statement.executeUpdate()
        }
    }
    private fun id()=UUID.randomUUID().toString()
}

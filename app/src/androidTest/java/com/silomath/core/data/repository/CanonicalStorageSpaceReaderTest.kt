package com.silomath.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.*
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.model.*
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Real Room/DAO/repository; explicitly controlled fixture, no Production data or geometry implementation. */
@RunWith(AndroidJUnit4::class)
class CanonicalStorageSpaceReaderTest {
    private lateinit var db:SiloMathDatabase
    private lateinit var reader:CanonicalStorageSpaceReader
    private lateinit var session:MutableStateFlow<SessionState>
    private fun id(n:Int)="10000000-0000-0000-0000-"+n.toString().padStart(12,'0')
    private val org=OrganizationId(id(1));private val loc=LocationId(id(2));private val unit=StorageUnitId(id(3))
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),SiloMathDatabase::class.java).build()
        session=MutableStateFlow(SessionState(userId=id(9),token="test-only",activeOrganizationId=org.value,
            authorizedOrganizations=listOf(AuthorizedOrganizationContext(id(8),org.value,"Fixture","FIX","STANDARD"))))
        val auth=object:AuthRepository {
            override val sessionState:Flow<SessionState> get()=session
            override suspend fun signIn(email:String,password:String)=error("Unused")
            override suspend fun signOut()=error("Unused")
            override suspend fun resetPassword(email:String)=error("Unused")
            override suspend fun updatePassword(recoveryContext:String,newPassword:String)=error("Unused")
            override suspend fun setActiveOrganization(organizationId:String)=error("Unused")
            override suspend fun restoreRemoteSession()=error("Unused")
        }
        reader=CanonicalStorageSpaceReader(db,auth)
        db.withTransaction {
            val sql=db.openHelper.writableDatabase
            for(offset in listOf(0,100)) {
                val o=id(1+offset);val l=id(2+offset);val u=id(3+offset);val family=id(4+offset);val profile=id(5+offset);val version=id(6+offset);val actor=id(9)
                sql.execSQL("INSERT INTO v6_organizations(organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) VALUES('$o','Fixture','$o','ACTIVE',10,'$actor',20,'$actor',1)")
                sql.execSQL("INSERT INTO v6_locations(location_id,organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) VALUES('$l','$o','Fixture','L','ACTIVE',10,'$actor',20,'$actor',1)")
                sql.execSQL("INSERT INTO v6_storage_units(storage_unit_id,organization_id,location_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) VALUES('$u','$o','$l','Unit','U','ACTIVE',10,'$actor',20,'$actor',1)")
                sql.execSQL("INSERT INTO v6_geometry_families(geometry_family_id,family_code,name,formula_version,parameter_contract) VALUES('$family','F02','Fixture','1','MD004_TEST_ONLY')")
                sql.execSQL("INSERT INTO v6_geometry_profiles(geometry_profile_id,organization_id,name,code,geometry_family_id,lifecycle_status,current_version_id,created_at,created_by,updated_at,updated_by,entity_version) VALUES('$profile','$o','Fixture','G','$family','ACTIVE','$version',10,'$actor',20,'$actor',1)")
                sql.execSQL("INSERT INTO v6_geometry_profile_versions(geometry_profile_version_id,geometry_profile_id,version_number,geometry_family_id,measurement_reference_height_m,maximum_fill_height_m,geometry_origin,max_capacity_m3,formula_version,schema_version,created_at,created_by) VALUES('$version','$profile',1,'$family','1','1','MD004_TEST_ONLY','1','1','MD004_TEST_ONLY',10,'$actor')")
            }
        }
    }
    @After fun close(){db.close()}
    private fun child(n:Int,code:String="S",offset:Int=0,status:String="ACTIVE") {
        db.openHelper.writableDatabase.execSQL("INSERT INTO v6_storage_spaces(storage_space_id,organization_id,storage_unit_id,code,description,geometry_profile_id,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) VALUES('${id(n)}','${id(1+offset)}','${id(3+offset)}','$code','Original','${id(5+offset)}','$status',10,'${id(9)}',20,'${id(9)}',7)")
    }
    @Test fun canonicalUuidParentReadsCanonicalChildrenWithoutNumericMirror()=runBlocking {
        child(22,"B",status="INACTIVE");child(21,"A");child(23,"X",100)
        val rows=reader.observe(org,loc,unit).first().getOrThrow()
        assertEquals(listOf("A","B"),rows.map{it.code});assertEquals(listOf(id(21),id(22)),rows.map{it.storageSpaceId.value})
        assertTrue(rows.all{it.storageUnitId==unit});assertEquals(GeometryProfileId(id(5)),rows.first().geometryProfileId)
        assertEquals(MasterStatus.INACTIVE,rows.last().lifecycleStatus);assertEquals(Instant.ofEpochMilli(10),rows.first().createdAt)
        assertEquals(7L,rows.first().entityVersion);assertEquals("Original",rows.first().description)
        assertNull(db.storageDao().getStorageUnitByStorageUnitId(unit.value))
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertEquals(0,it.count)}
    }
    @Test fun validEmptyIsDistinctFromMissingWrongParentAndWrongTenant()=runBlocking {
        assertEquals(emptyList<StorageSpace>(),reader.observe(org,loc,unit).first().getOrThrow())
        assertTrue(reader.observe(org,LocationId(id(102)),unit).first().isFailure)
        assertTrue(reader.observe(org,loc,StorageUnitId(id(103))).first().isFailure)
        assertTrue(reader.observe(org,loc,StorageUnitId(id(999))).first().isFailure)
        assertTrue(reader.observe(OrganizationId(id(101)),LocationId(id(102)),StorageUnitId(id(103))).first().isFailure)
    }
    @Test fun legacyOnlyChildIsNotPromotedAndRemainsIntact()=runBlocking {
        db.organizationDao().insertOrganization(OrganizationEntity(id=1,name="Retained"))
        db.locationDao().insertLocation(LocationEntity(id=1,organizationId=1,name="Retained",code="LEG"))
        db.storageDao().insertStorageUnit(StorageUnitEntity(id=1,locationId=1,code="LEG",storageUnitId=unit.value))
        db.geometryDao().insertFamily(GeometryFamilyEntity(id=1,name="Retained"))
        db.geometryDao().insertProfile(GeometryProfileEntity(id=1,name="Retained",geometryFamilyId=1,profileCode="LEG"))
        db.storageDao().insertStorageSpace(StorageSpaceEntity(id=1,storageUnitId=1,code="LEG",geometryProfileId=1,storageSpaceId=id(31),canonicalStorageUnitId=unit.value,
            canonicalGeometryProfileId=id(5),canonicalOrganizationId=org.value,createdAt=10,createdBy=id(9),updatedAt=20,updatedBy=id(9),entityVersion=1))
        assertTrue(reader.observe(org,loc,unit).first().getOrThrow().isEmpty())
        db.openHelper.readableDatabase.query("SELECT code FROM storage_spaces WHERE id=1").use{assertTrue(it.moveToFirst());assertEquals("LEG",it.getString(0))}
        assertTrue(db.syncOperationDao().getPendingOperationsList().isEmpty())
    }
    @Test fun liveReaderObservesCanonicalInsertionAndRevocation()=runBlocking {
        val observed=mutableListOf<Result<List<StorageSpace>>>()
        val job=launch(Dispatchers.Default){reader.observe(org,loc,unit).collect{ synchronized(observed){observed+=it} }}
        suspend fun await(predicate:()->Boolean){withTimeout(5000){while(!predicate())delay(10)}}
        try {
            await{synchronized(observed){observed.any{it.isSuccess && it.getOrThrow().isEmpty()}}}
            db.withTransaction { child(21) }
            await{synchronized(observed){observed.any{it.isSuccess && it.getOrThrow().size==1}}}
            session.value=session.value.copy(authorizedOrganizations=emptyList())
            await{synchronized(observed){observed.lastOrNull()?.isFailure==true}}
        } finally{job.cancelAndJoin()}
    }
}

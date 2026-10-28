package com.silomath.core.domain.model

import com.silomath.core.model.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class StorageSpaceContractTest {
    private fun id(n:Int)="30000000-0000-0000-0000-"+n.toString().padStart(12,'0')
    private fun row(code:String="S1",description:String?=null,status:MasterStatus=MasterStatus.ACTIVE,version:Long=1)=StorageSpace(
        StorageSpaceId(id(1)),StorageUnitId(id(2)),code,description,GeometryProfileId(id(3)),status,
        Instant.EPOCH,id(4),Instant.EPOCH,id(4),version)
    @Test fun canonicalCreateAcceptsApprovedFields(){assertEquals("S1",row().code)}
    @Test fun lifecycleIsExactlyActiveOrInactive(){assertEquals(setOf(MasterStatus.ACTIVE,MasterStatus.INACTIVE),MasterStatus.entries.toSet())}
    @Test fun codeMustBeNonBlankTrimmedAndUppercase(){
        listOf("", " ", " s1 ","s1").forEach{value->assertThrows(IllegalArgumentException::class.java){row(code=value)}}
    }
    @Test fun descriptionAndGeometryReferenceArePreserved(){val value=row(description="Context");assertEquals("Context",value.description);assertEquals(id(3),value.geometryProfileId.value)}
    @Test fun entityVersionMustBePositive(){assertThrows(IllegalArgumentException::class.java){row(version=0)}}
}

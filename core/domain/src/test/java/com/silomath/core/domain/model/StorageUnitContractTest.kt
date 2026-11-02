package com.silomath.core.domain.model

import com.silomath.core.model.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class StorageUnitContractTest {
    private val id="10000000-0000-0000-0000-000000000001"
    private val parent="20000000-0000-0000-0000-000000000001"
    private fun unit()=StorageUnit(StorageUnitId(id),LocationId(parent),"Unit","U1",null,MasterStatus.ACTIVE,Instant.EPOCH,id,Instant.EPOCH,id,1)
    private fun rejected(block:()->Unit){try{block();fail("Expected invalid canonical value")}catch(_:IllegalArgumentException){}}
    @Test fun uuidIsStableAndIndependentOfBusinessCode(){val u=unit();assertEquals(id,u.copy(code="OTHER").storageUnitId.value);assertEquals(parent,u.locationId.value)}
    @Test fun legacyNumericAndMalformedIdsAreRejected(){listOf("1","remote-1","1-1-1-1-1","").forEach{rejected{StorageUnitId(it)}}}
    @Test fun nameAndCodeRequireCanonicalBounds(){listOf(""," "," x","x ","x".repeat(151)).forEach{rejected{unit().copy(name=it)}};listOf("","a"," A ","X".repeat(51)).forEach{rejected{unit().copy(code=it)}}}
    @Test fun optionalDescriptionAndBothLifecycleValuesRoundTrip(){assertNull(unit().description);assertEquals("",unit().copy(description="").description);assertEquals(MasterStatus.INACTIVE,unit().copy(lifecycleStatus=MasterStatus.INACTIVE).lifecycleStatus);rejected{unit().copy(description="x".repeat(501))}}
    @Test fun versionStartsAtOneAndMetadataIsNotSynthesized(){rejected{unit().copy(entityVersion=0)};assertEquals(Instant.EPOCH,unit().createdAt);assertEquals(id,unit().updatedBy)}
}

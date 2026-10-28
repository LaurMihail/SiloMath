package com.silomath.backend.storagespace

import kotlin.test.*
import kotlinx.serialization.json.*

class StorageSpaceContractsTest {
    private fun create()=buildJsonObject{put("code"," s1 ");put("geometryProfileId","50000000-0000-0000-0000-000000000001")}
    @Test fun createNormalizesCodeAndDefaultsLifecycle(){
        val fields=StorageSpaceFields.parse(create(),true)
        assertEquals("S1",fields["code"]!!.jsonPrimitive.content);assertEquals("ACTIVE",fields["lifecycleStatus"]!!.jsonPrimitive.content)
    }
    @Test fun patchPreservesAbsentAndNullableDescription(){
        assertFalse(StorageSpaceFields.parse(buildJsonObject{put("code","S")},false).containsKey("description"))
        assertEquals(JsonNull,StorageSpaceFields.parse(buildJsonObject{put("description",JsonNull)},false)["description"])
    }
    @Test fun immutableLegacyServerAndParentFieldsAreRejected(){
        for(key in listOf("id","storageSpaceId","storageUnitId","organizationId","locationId","remoteId","entityVersion","createdAt"))
            assertFailsWith<StorageSpaceFailure>{StorageSpaceFields.parse(JsonObject(create()+mapOf(key to JsonPrimitive("override"))),true)}
    }
    @Test fun invalidCodeGeometryAndLifecycleAreRejected(){
        for(value in listOf(""," "))assertFailsWith<StorageSpaceFailure>{StorageSpaceFields.parse(JsonObject(create()+("code" to JsonPrimitive(value))),true)}
        assertFailsWith<StorageSpaceFailure>{StorageSpaceFields.parse(JsonObject(create()+("geometryProfileId" to JsonPrimitive("legacy-1"))),true)}
        assertFailsWith<StorageSpaceFailure>{StorageSpaceFields.parse(JsonObject(create()+("lifecycleStatus" to JsonPrimitive("DRAFT"))),true)}
    }
    @Test fun canonicalIdentityNeverAcceptsNumericLegacy(){assertFailsWith<StorageSpaceFailure>{spaceUuid("1","storageSpaceId")}}
}

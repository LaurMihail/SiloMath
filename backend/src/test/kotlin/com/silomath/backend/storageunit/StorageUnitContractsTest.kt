package com.silomath.backend.storageunit

import kotlin.test.*
import kotlinx.serialization.json.*

class StorageUnitContractsTest {
    private fun create()=buildJsonObject{put("name"," Unit ");put("code"," u1 ")}
    @Test fun `create normalizes name code and defaults only approved lifecycle`() {
        val fields=StorageUnitFields.parse(create(),true)
        assertEquals("Unit",fields["name"]!!.jsonPrimitive.content);assertEquals("U1",fields["code"]!!.jsonPrimitive.content)
        assertEquals("ACTIVE",fields["lifecycleStatus"]!!.jsonPrimitive.content)
    }
    @Test fun `patch preserves absent versus nullable description`() {
        assertFalse(StorageUnitFields.parse(buildJsonObject{put("name","New")},false).containsKey("description"))
        assertEquals(JsonNull,StorageUnitFields.parse(buildJsonObject{put("description",JsonNull)},false)["description"])
        for(key in listOf("name","code","lifecycleStatus"))assertFailsWith<StorageUnitFailure>{StorageUnitFields.parse(buildJsonObject{put(key,JsonNull)},false)}
    }
    @Test fun `immutable legacy and server controlled fields cannot be overridden`() {
        for(key in listOf("id","storageUnitId","locationId","organizationId","remoteId","type","createdAt","updatedBy","entityVersion"))
            assertFailsWith<StorageUnitFailure>{StorageUnitFields.parse(JsonObject(create()+mapOf(key to JsonPrimitive("override"))),true)}
    }
    @Test fun `limits and enum reject invalid values`() {
        for((key,value) in listOf("name" to " ","name" to "x".repeat(151),"code" to "x".repeat(51),"description" to "x".repeat(501),"lifecycleStatus" to "DRAFT"))
            assertFailsWith<StorageUnitFailure>{StorageUnitFields.parse(JsonObject(create()+mapOf(key to JsonPrimitive(value))),true)}
        assertFailsWith<StorageUnitFailure>{StorageUnitFields.parse(buildJsonObject{put("name",1);put("code","U")},true)}
    }
    @Test fun `canonical UUID never comes from numeric legacy identity`() {
        val id="10000000-0000-0000-0000-000000000001";assertEquals(id,canonicalUuid(id,"id"))
        listOf("1","1-1-1-1-1","remote-id").forEach{assertFailsWith<StorageUnitFailure>{canonicalUuid(it,"id")}}
    }
}

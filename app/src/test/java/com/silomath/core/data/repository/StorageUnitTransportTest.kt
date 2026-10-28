package com.silomath.core.data.repository

import com.silomath.core.data.network.api.*
import com.silomath.core.data.network.auth.*
import com.silomath.core.data.network.dto.*
import com.silomath.core.data.network.source.*
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Real loopback HTTP + Retrofit serialization, never live backend or Supabase. */
class StorageUnitTransportTest {
    private lateinit var server:HttpServer
    private lateinit var api:StorageUnitApi
    private lateinit var remote:RestStorageUnitRemoteSource
    private val requests=java.util.concurrent.CopyOnWriteArrayList<Triple<String,Map<String,List<String>>,String>>()
    private val replies=java.util.concurrent.ConcurrentLinkedQueue<Pair<Int,String>>()
    private val org="10000000-0000-0000-0000-000000000001"
    private val location="10000000-0000-0000-0000-000000000002"
    private val entity="10000000-0000-0000-0000-000000000003"
    private var refreshes=0
    private val json=Json{ignoreUnknownKeys=true;encodeDefaults=true;explicitNulls=true}
    private fun id()=UUID.randomUUID().toString()
    private fun record()=StorageUnitResponseDto(entity,org,location,"Unit","U",null,"INACTIVE","2026-01-01T00:00:00Z",org,"2026-02-01T00:00:00Z",location,9)
    private fun batch()=StorageUnitSyncBatchDto(org,"test-only",operations=listOf(StorageUnitSyncOperationDto(id(),id(),org,
        entityType="STORAGE_UNIT",entityId=entity,operationType="CREATE",eventAtUtc="2000-01-01T00:00:00Z",payload=buildJsonObject{put("locationId",location);put("name","Unit");put("code","U")})))
    @Before fun setup(){
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){exchange->
            requests+=Triple(exchange.requestMethod+" "+exchange.requestURI.toString(),exchange.requestHeaders.toMap(),exchange.requestBody.bufferedReader().readText())
            val reply=replies.poll() ?: (500 to "{}")
            val bytes=reply.second.toByteArray();exchange.responseHeaders.add("Content-Type","application/json")
            if(reply.first==429)exchange.responseHeaders.add("Retry-After","120")
            exchange.sendResponseHeaders(reply.first,bytes.size.toLong());exchange.responseBody.use{it.write(bytes)}
        };server.start()
        val retrofit=Retrofit.Builder().baseUrl("http://127.0.0.1:${server.address.port}/")
            .client(OkHttpClient()).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
        api=retrofit.create(StorageUnitApi::class.java)
        val session=object:NetworkSessionProvider {
            override fun currentAccessToken()="test-only"
            override suspend fun refreshAccessToken(failedAccessToken:String?):String {refreshes++;return "refreshed-test-only"}
        }
        remote=RestStorageUnitRemoteSource(api,retrofit.create(LocationApi::class.java),retrofit.create(OrganizationApi::class.java),AuthenticatedApiCallExecutor(session))
    }
    @After fun close(){server.stop(0)}
    private fun header(n:Int,name:String)=requests[n].second.entries.single{it.key.equals(name,true)}.value.single()
    @Test fun canonicalCrudPathsHeadersAndLosslessResponseMapping()=runBlocking {
        val encoded=json.encodeToString(StorageUnitResponseDto.serializer(),record())
        repeat(3){replies.add(200 to encoded)}
        val key=id();val op=id()
        api.create(org,location,key,StorageUnitCreateRequestDto("Unit","U",idempotencyKey=key,clientOperationId=op))
        api.patch(org,entity,key,9,buildJsonObject{put("description",JsonNull);put("clientOperationId",op)})
        val mapped=remote.get(org,entity)
        assertEquals("POST /api/v1/organizations/$org/locations/$location/storage-units",requests[0].first)
        assertEquals("PATCH /api/v1/organizations/$org/storage-units/$entity",requests[1].first)
        assertEquals("GET /api/v1/organizations/$org/storage-units/$entity",requests[2].first)
        assertEquals(key,header(0,"Idempotency-Key"));assertEquals("9",header(1,"If-Match-Entity-Version"))
        val create=json.parseToJsonElement(requests[0].third).jsonObject
        assertFalse(create.containsKey("id"));assertFalse(create.containsKey("locationId"));assertFalse(create.containsKey("remoteId"));assertFalse(create.containsKey("createdAt"))
        val patch=json.parseToJsonElement(requests[1].third).jsonObject;assertEquals(JsonNull,patch["description"]);assertFalse(patch.containsKey("name"))
        assertEquals(record().transfer(),mapped)
    }
    @Test fun paginatedReadUsesCanonicalParentAndRejectsNonAdvancingCursor()=runBlocking {
        replies.add(200 to json.encodeToString(StorageUnitPageDto.serializer(),StorageUnitPageDto(listOf(record()),"opaque",true)))
        replies.add(200 to json.encodeToString(StorageUnitPageDto.serializer(),StorageUnitPageDto(emptyList(),null,false)))
        assertEquals(listOf(record().transfer()),remote.list(org,location))
        assertTrue(requests[1].first.endsWith("?cursor=opaque"))
        repeat(2){replies.add(200 to json.encodeToString(StorageUnitPageDto.serializer(),StorageUnitPageDto(emptyList(),"same",true)))}
        assertTrue(runCatching{remote.list(org,location)}.isFailure)
    }
    @Test fun syncKeepsStableIdentityDeviceAndRetryKeyAcrossOneAuthRefresh()=runBlocking {
        val b=batch();replies.add(401 to "{}");replies.add(200 to json.encodeToString(StorageUnitSyncBatchResultDto.serializer(),StorageUnitSyncBatchResultDto(
            listOf(StorageUnitSyncResultDto(b.operations.single().syncOperationId,"ACKNOWLEDGED",1)),"2026-01-01T00:00:00Z","test")))
        assertEquals("ACKNOWLEDGED",remote.push(b).results.single().outcome);assertEquals(1,refreshes)
        assertEquals(requests[0].third,requests[1].third);assertEquals(org,header(0,"X-Device-Installation-Id"))
        val op=json.parseToJsonElement(requests[0].third).jsonObject["operations"]!!.jsonArray.single().jsonObject
        assertEquals(entity,op["entityId"]!!.jsonPrimitive.content);assertEquals(JsonNull,op["baseEntityVersion"])
    }
    @Test fun failuresExposeMachineCodeAndRetryabilityWithoutServerMessage()=runBlocking {
        replies.add(503 to """{"code":"SERVICE_UNAVAILABLE","retryable":true,"message":"sensitive-detail"}""")
        val retry=runCatching{remote.push(batch())}.exceptionOrNull() as StorageUnitTransportFailure
        assertTrue(retry.retryable);assertEquals("SERVICE_UNAVAILABLE",retry.message)
        replies.add(422 to """{"code":"VALIDATION_FAILED","retryable":false,"message":"sensitive-detail"}""")
        assertFalse((runCatching{remote.push(batch())}.exceptionOrNull() as StorageUnitTransportFailure).retryable)
        replies.add(429 to "{}")
        val limited=runCatching{remote.push(batch())}.exceptionOrNull() as StorageUnitTransportFailure
        assertTrue(limited.retryable);assertEquals("120",limited.retryAfter)
        replies.add(403 to "{}")
        val denied=runCatching{remote.get(org,entity)}.exceptionOrNull() as StorageUnitTransportFailure
        assertFalse(denied.retryable);assertEquals("AUTH_FORBIDDEN",denied.code)
    }
}

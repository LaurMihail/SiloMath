package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.*
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

interface StorageSpaceApi {
    @GET("api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces")
    suspend fun list(@Path("organizationId") org:String,@Path("storageUnitId") unit:String,@Query("cursor") cursor:String?=null,
        @Query("search") search:String?=null,@Query("status") status:String?=null):StorageSpacePageDto
    @GET("api/v1/organizations/{organizationId}/storage-spaces/{storageSpaceId}")
    suspend fun get(@Path("organizationId") org:String,@Path("storageSpaceId") id:String):StorageSpaceResponseDto
    @POST("api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces")
    suspend fun create(@Path("organizationId") org:String,@Path("storageUnitId") unit:String,@Header("Idempotency-Key") key:String,
        @Body request:StorageSpaceCreateRequestDto):StorageSpaceResponseDto
    @POST("api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces:batch-create")
    suspend fun batchCreate(@Path("organizationId") org:String,@Path("storageUnitId") unit:String,@Header("Idempotency-Key") key:String,
        @Body request:StorageSpaceBatchCreateRequestDto):StorageSpaceBatchResponseDto
    @PATCH("api/v1/organizations/{organizationId}/storage-spaces/{storageSpaceId}")
    suspend fun patch(@Path("organizationId") org:String,@Path("storageSpaceId") id:String,@Header("Idempotency-Key") key:String,
        @Header("If-Match-Entity-Version") base:Long,@Body request:JsonObject):StorageSpaceResponseDto
    @POST("api/v1/sync/batch")
    suspend fun push(@Header("X-Device-Installation-Id") device:String,@Body request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto
}

package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.*
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

interface StorageUnitApi {
    @GET("organizations/{organizationId}/locations/{locationId}/storage-units")
    suspend fun list(@Path("organizationId") org:String,@Path("locationId") location:String,@Query("cursor") cursor:String?=null,
        @Query("search") search:String?=null,@Query("status") status:String?=null):StorageUnitPageDto
    @GET("organizations/{organizationId}/storage-units/{storageUnitId}")
    suspend fun get(@Path("organizationId") org:String,@Path("storageUnitId") id:String):StorageUnitResponseDto
    @POST("organizations/{organizationId}/locations/{locationId}/storage-units")
    suspend fun create(@Path("organizationId") org:String,@Path("locationId") location:String,@Header("Idempotency-Key") key:String,
        @Body request:StorageUnitCreateRequestDto):StorageUnitResponseDto
    @PATCH("organizations/{organizationId}/storage-units/{storageUnitId}")
    suspend fun patch(@Path("organizationId") org:String,@Path("storageUnitId") id:String,@Header("Idempotency-Key") key:String,
        @Header("If-Match-Entity-Version") base:Long,@Body request:JsonObject):StorageUnitResponseDto
    @POST("sync/batch")
    suspend fun push(@Header("X-Device-Installation-Id") device:String,@Body request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto
}

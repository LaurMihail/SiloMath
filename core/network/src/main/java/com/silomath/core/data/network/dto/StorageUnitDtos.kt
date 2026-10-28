package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class StorageUnitResponseDto(val id:String,val organizationId:String,val locationId:String,val name:String,val code:String,
    val description:String?=null,val lifecycleStatus:String,val createdAt:String,val createdBy:String,val updatedAt:String,val updatedBy:String,val entityVersion:Long)
@Serializable data class StorageUnitPageDto(val items:List<StorageUnitResponseDto>,val nextCursor:String?=null,val hasMore:Boolean)
@Serializable data class StorageUnitCreateRequestDto(val name:String,val code:String,val description:String?=null,val lifecycleStatus:String="ACTIVE",
    val idempotencyKey:String,val clientOperationId:String,val occurredAtUtc:String?=null)
@Serializable data class StorageUnitSyncBatchDto(val deviceInstallationId:String,val clientVersion:String,val checkpointContext:String?=null,val operations:List<StorageUnitSyncOperationDto>)
@Serializable data class StorageUnitSyncOperationDto(val syncOperationId:String,val idempotencyKey:String,val organizationId:String,val campaignId:String?=null,
    val entityType:String,val entityId:String,val operationType:String,val baseEntityVersion:Long?=null,val eventAtUtc:String,
    val dependencyIds:List<String> = emptyList(),val payload:JsonObject)
@Serializable data class StorageUnitSyncResultDto(val syncOperationId:String,val outcome:String,val entityVersion:Long?=null,
    val error:JsonObject?=null,val conflict:JsonObject?=null)
@Serializable data class StorageUnitSyncBatchResultDto(val results:List<StorageUnitSyncResultDto>,val serverTimeUtc:String,val correlationId:String)

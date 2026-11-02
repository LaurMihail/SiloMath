package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class StorageSpaceResponseDto(val id:String,val organizationId:String,val locationId:String,val storageUnitId:String,
    val code:String,val description:String?=null,val geometryProfileId:String,val lifecycleStatus:String,
    val createdAt:String,val createdBy:String,val updatedAt:String,val updatedBy:String,val entityVersion:Long)
@Serializable data class StorageSpacePageDto(val items:List<StorageSpaceResponseDto>,val nextCursor:String?=null,val hasMore:Boolean)
@Serializable data class StorageSpaceBatchResponseDto(val items:List<StorageSpaceResponseDto>)
@Serializable data class StorageSpaceCreateRequestDto(val code:String,val description:String?=null,val geometryProfileId:String,
    val lifecycleStatus:String="ACTIVE",val idempotencyKey:String,val clientOperationId:String,val occurredAtUtc:String?=null)
@Serializable data class StorageSpaceBatchItemDto(val code:String,val description:String?=null,val geometryProfileId:String,val lifecycleStatus:String="ACTIVE")
@Serializable data class StorageSpaceBatchCreateRequestDto(val items:List<StorageSpaceBatchItemDto>,val idempotencyKey:String,
    val clientOperationId:String,val occurredAtUtc:String?=null)

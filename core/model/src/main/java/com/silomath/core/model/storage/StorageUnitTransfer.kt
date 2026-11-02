package com.silomath.core.model.storage

/** Complete canonical transport facts, never a legacy row or synthesized provenance. */
data class StorageUnitTransfer(val id:String,val organizationId:String,val locationId:String,val name:String,val code:String,
    val description:String?,val lifecycleStatus:String,val createdAt:String,val createdBy:String,val updatedAt:String,
    val updatedBy:String,val entityVersion:Long)

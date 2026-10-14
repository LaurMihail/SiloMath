package com.silomath.core.model.organization

data class OrganizationTransferRecord(
    val id: String,
    val name: String,
    val code: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val lifecycleStatus: String,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long
)

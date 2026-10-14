package com.silomath.core.model.organization

data class UserMembershipTransferRecord(
    val membershipId: String,
    val organizationId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val userLifecycleStatus: String,
    val role: String,
    val membershipLifecycleStatus: String,
    val disabledAt: String?,
    val disabledBy: String?,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long
)
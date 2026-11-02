package com.silomath.core.domain.model

import com.silomath.core.model.OrganizationId
import com.silomath.core.model.OrganizationMembershipId
import com.silomath.core.model.UserId
import java.time.Instant

enum class UserStatus {
    ACTIVE,
    DISABLED
}

enum class MasterStatus {
    ACTIVE,
    INACTIVE
}

enum class MembershipStatus {
    ACTIVE,
    DISABLED
}

enum class OrganizationRole {
    ADMIN,
    STANDARD
}

data class User(
    val userId: UserId,
    val fullName: String,
    val email: String,
    val lifecycleStatus: UserStatus,
    val createdAt: Instant,
    val createdBy: String?,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(entityVersion >= 1L) { "User entityVersion must be >= 1" }
    }
}

data class OrganizationReadModel(
    val organizationId: OrganizationId,
    val name: String,
    val code: String?
)

data class Organization(
    val organizationId: OrganizationId,
    val name: String,
    val code: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val lifecycleStatus: MasterStatus,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(entityVersion >= 1L) { "Organization entityVersion must be >= 1" }
    }
}

data class OrganizationMembership(
    val membershipId: OrganizationMembershipId,
    val organizationId: OrganizationId,
    val userId: UserId,
    val role: OrganizationRole,
    val lifecycleStatus: MembershipStatus,
    val disabledAt: Instant?,
    val disabledBy: String?,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
) {
    init {
        require(entityVersion >= 1L) { "OrganizationMembership entityVersion must be >= 1" }
    }
}
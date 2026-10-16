package com.silomath.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInstallation(
    val deviceInstallationId: DeviceInstallationId,
    val userId: UserId? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAuthorizedAt: Long? = null,
    val revokedAt: Long? = null
)

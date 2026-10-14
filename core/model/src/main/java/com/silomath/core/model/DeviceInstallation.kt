package com.silomath.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInstallation(
    val deviceInstallationId: String,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAuthorizedAt: Long? = null,
    val revokedAt: Long? = null
)

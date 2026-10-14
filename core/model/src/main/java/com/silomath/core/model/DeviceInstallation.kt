package com.silomath.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInstallation(
    val installationUuid: String,
    val createdAt: Long = System.currentTimeMillis()
)

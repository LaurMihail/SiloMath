package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OfflineManifestDto(
    val manifestId: String,
    val organizationId: String,
    val campaignId: String,
    val deviceInstallationId: String,
    val packageVersion: String,
    val contentManifest: JsonObject,
    val integrityHash: String,
    val integrityAlgorithm: String,
    val verificationMetadata: JsonObject?,
    val generatedAt: String
)

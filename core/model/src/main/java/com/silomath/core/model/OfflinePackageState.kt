package com.silomath.core.model

/** Frozen local technical lifecycle (OS-D25); never a Campaign status. */
enum class OfflinePackageState {
    NOT_DOWNLOADED, DOWNLOAD_PENDING, DOWNLOADING, VERIFYING, READY, DOWNLOAD_ERROR
}

data class OfflinePackageIdentity(
    val manifestId: OfflineWorkingSetManifestId,
    val campaignId: CampaignId,
    val organizationId: OrganizationId,
    val deviceInstallationId: DeviceInstallationId,
    val authorizedUserId: UserId,
    val packageVersion: String
) {
    init { require(packageVersion.isNotBlank()) }
}

/** Readiness still requires current authorization, integrity and compatibility. */
data class OfflinePackageProgress(
    val identity: OfflinePackageIdentity,
    val state: OfflinePackageState,
    val downloadedBytes: Long = 0,
    val verifiedAt: Long? = null,
    val activatedAt: Long? = null,
    val errorCode: String? = null
) {
    init {
        require(downloadedBytes >= 0)
        require(activatedAt == null || verifiedAt != null)
        require(state != OfflinePackageState.READY || verifiedAt != null && activatedAt != null)
        require(state != OfflinePackageState.DOWNLOAD_ERROR || !errorCode.isNullOrBlank())
    }

    fun startDownload(): OfflinePackageProgress {
        require(state == OfflinePackageState.DOWNLOAD_PENDING)
        return copy(state = OfflinePackageState.DOWNLOADING, downloadedBytes = 0, errorCode = null)
    }

    fun beginVerification(bytes: Long): OfflinePackageProgress {
        require(state == OfflinePackageState.DOWNLOADING && bytes > 0)
        return copy(state = OfflinePackageState.VERIFYING, downloadedBytes = bytes)
    }

    fun failed(code: String): OfflinePackageProgress {
        require(state != OfflinePackageState.READY) { "Refresh failures belong to staging, never the active package" }
        return copy(state = OfflinePackageState.DOWNLOAD_ERROR, errorCode = code)
    }

    fun retry(): OfflinePackageProgress {
        require(state == OfflinePackageState.DOWNLOAD_ERROR)
        return copy(state = OfflinePackageState.DOWNLOAD_PENDING, downloadedBytes = 0,
            verifiedAt = null, activatedAt = null, errorCode = null)
    }
}

package com.silomath.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Per-version staging/recovery metadata; business data remains in canonical Room tables. */
@Entity(tableName = "offline_package_downloads", indices = [
    Index(value = ["campaign_id", "device_installation_id", "package_version"], unique = true),
    Index(value = ["authorized_user_id", "state"])
])
data class OfflinePackageDownloadEntity(
    @PrimaryKey @ColumnInfo(name = "manifest_id") val manifestId: String,
    @ColumnInfo(name = "campaign_id") val campaignId: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "device_installation_id") val deviceInstallationId: String,
    @ColumnInfo(name = "authorized_user_id") val authorizedUserId: String,
    @ColumnInfo(name = "package_version") val packageVersion: String,
    val state: String,
    @ColumnInfo(name = "manifest_json") val manifestJson: String,
    @ColumnInfo(name = "staging_private_ref") val stagingPrivateRef: String?,
    @ColumnInfo(name = "downloaded_bytes") val downloadedBytes: Long,
    @ColumnInfo(name = "verified_at") val verifiedAt: Long?,
    @ColumnInfo(name = "activated_at") val activatedAt: Long?,
    @ColumnInfo(name = "error_code") val errorCode: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

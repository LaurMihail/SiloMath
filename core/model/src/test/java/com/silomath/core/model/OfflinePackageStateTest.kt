package com.silomath.core.model

import org.junit.Assert.*
import org.junit.Test

class OfflinePackageStateTest {
    private val uuid="00000000-0000-0000-0000-000000000001"
    private val identity=OfflinePackageIdentity(OfflineWorkingSetManifestId(uuid),CampaignId(uuid),
        OrganizationId(uuid),DeviceInstallationId(uuid),UserId(uuid),"version-1")

    @Test fun lifecycleUsesExactlyFrozenTechnicalStates() {
        assertEquals(setOf("NOT_DOWNLOADED","DOWNLOAD_PENDING","DOWNLOADING","VERIFYING","READY","DOWNLOAD_ERROR"),
            OfflinePackageState.entries.map { it.name }.toSet())
    }
    @Test fun downloadMustFinishBeforeVerification() {
        val pending=OfflinePackageProgress(identity,OfflinePackageState.DOWNLOAD_PENDING)
        val verifying=pending.startDownload().beginVerification(1024)
        assertEquals(OfflinePackageState.VERIFYING,verifying.state)
        assertEquals(1024L,verifying.downloadedBytes)
        assertNull(verifying.activatedAt)
    }
    @Test fun readyCannotBeConstructedWithoutVerificationAndActivation() {
        assertThrows(IllegalArgumentException::class.java) { OfflinePackageProgress(identity,OfflinePackageState.READY) }
        assertThrows(IllegalArgumentException::class.java) { OfflinePackageProgress(identity,OfflinePackageState.READY,verifiedAt=1) }
    }
    @Test fun failurePreservesIdentityAndRetryDoesNotInventVersion() {
        val downloading=OfflinePackageProgress(identity,OfflinePackageState.DOWNLOAD_PENDING).startDownload()
        val retry=downloading.failed("NETWORK_UNAVAILABLE").retry()
        assertEquals(identity,retry.identity)
        assertEquals(OfflinePackageState.DOWNLOAD_PENDING,retry.state)
        assertNull(retry.errorCode)
    }
    @Test fun failedRefreshCannotDemoteActivePackage() {
        val ready=OfflinePackageProgress(identity,OfflinePackageState.READY,verifiedAt=1,activatedAt=2)
        assertThrows(IllegalArgumentException::class.java) { ready.failed("DOWNLOAD_INTERRUPTED") }
    }
    @Test fun verificationCannotSkipDownload() {
        val pending=OfflinePackageProgress(identity,OfflinePackageState.DOWNLOAD_PENDING)
        assertThrows(IllegalArgumentException::class.java) { pending.beginVerification(1024) }
    }
}

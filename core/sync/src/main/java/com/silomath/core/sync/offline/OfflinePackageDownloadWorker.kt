package com.silomath.core.sync.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.silomath.core.data.network.source.OfflinePackageRemoteSource
import com.silomath.core.data.network.source.OfflinePackageHttpException
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class OfflinePackageDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val auth: AuthRepository,
    private val database: SiloMathDatabase,
    private val remote: OfflinePackageRemoteSource,
    private val staging: PackageStagingStore
) : CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result=withContext(Dispatchers.IO) {
        val user=inputData.getString("userId") ?: return@withContext Result.failure()
        val org=inputData.getString("organizationId") ?: return@withContext Result.failure()
        val campaign=inputData.getString("campaignId") ?: return@withContext Result.failure()
        val device=inputData.getString("deviceInstallationId") ?: return@withContext Result.failure()
        if(listOf(user,org,campaign,device).any { runCatching { UUID.fromString(it).toString()!=it }.getOrDefault(true) }) return@withContext Result.failure()
        var refreshed=false
        while(true) {
            val session=auth.sessionState.first()
            if(session.userId!=user || !session.isLoggedIn || session.authorizedOrganizations.none { it.organizationId==org }) return@withContext Result.failure()
            try {
                val record=OfflinePackageDownloadRunner(remote,database.offlinePackageDownloadDao(),staging)
                    .run(session.token!!,user,org,campaign,device)
                try {
                    val verified=OfflinePackageIntegrityVerifier(staging).verify(record)
                    OfflineImportPlanner.prepare(verified)
                    database.offlinePackageDownloadDao().update(record.copy(verifiedAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis()))
                } catch(cancelled: CancellationException) {
                    throw cancelled
                } catch(_: Exception) {
                    database.offlinePackageDownloadDao().update(record.copy(state="DOWNLOAD_ERROR",errorCode="PACKAGE_INTEGRITY_OR_STAGING_FAILED",updatedAt=System.currentTimeMillis()))
                    return@withContext Result.failure()
                }
                return@withContext Result.success()
            } catch(cancelled: CancellationException) {
                throw cancelled
            } catch(failure: Exception) {
                val status=when(failure) {
                    is OfflinePackageHttpException -> failure.status
                    else -> null
                }
                if(status==401 && !refreshed) {
                    refreshed=true
                    if(auth.restoreRemoteSession().isSuccess) continue
                }
                if(status==429 || status!=null && status in 500..599 || status==null && failure is IOException) {
                    val attempt=inputData.getInt("attempt",0)
                    val retryAfter=(failure as? OfflinePackageHttpException)?.retryAfter
                    OfflinePackageDownloadScheduler.retry(context,user,org,campaign,device,attempt+1,
                        PackageDownloadRetryPolicy.delayMillis(attempt,retryAfter))
                    // The attempt ends; durable DOWNLOAD_ERROR and the chained delayed work retain the outcome.
                    return@withContext Result.success()
                }
                return@withContext Result.failure()
            }
        }
        @Suppress("UNREACHABLE_CODE") Result.failure()
    }
}

object OfflinePackageDownloadScheduler {
    suspend fun enqueue(context: Context,userId: String,organizationId: String,campaignId: String,deviceInstallationId: String)=withContext(Dispatchers.IO) {
        schedule(context,userId,organizationId,campaignId,deviceInstallationId,0,0,ExistingWorkPolicy.KEEP)
    }
    internal fun retry(context: Context,userId: String,organizationId: String,campaignId: String,deviceInstallationId: String,attempt: Int,delay: Long) {
        schedule(context,userId,organizationId,campaignId,deviceInstallationId,attempt,delay,ExistingWorkPolicy.APPEND_OR_REPLACE)
    }
    private fun schedule(context: Context,userId: String,organizationId: String,campaignId: String,deviceInstallationId: String,attempt: Int,delay: Long,policy: ExistingWorkPolicy) {
        val request=OneTimeWorkRequestBuilder<OfflinePackageDownloadWorker>()
            .setInitialDelay(delay,TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf("userId" to userId,"organizationId" to organizationId,
                "campaignId" to campaignId,"deviceInstallationId" to deviceInstallationId,"attempt" to attempt))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("offline-package:$userId:$campaignId:$deviceInstallationId",policy,request).result.get()
    }
}

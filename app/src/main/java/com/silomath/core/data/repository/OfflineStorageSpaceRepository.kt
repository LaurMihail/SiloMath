package com.silomath.core.data.repository

import androidx.room.withTransaction
import com.silomath.core.data.network.dto.*
import com.silomath.core.data.network.source.*
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.*
import com.silomath.core.database.entity.canonical.V6StorageSpacesRow
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import com.silomath.core.model.SyncOperationState
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** Canonical UUID Storage Space owner. Legacy numeric tables are retained but never operational authority. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineStorageSpaceRepository @Inject constructor(
    private val database:SiloMathDatabase,
    private val auth:AuthRepository,
    private val remote:StorageSpaceRemoteSource
):StorageSpaceRepository {
    private val dao get()=database.canonicalStorageSpaceReadDao()
    private val pushes=Mutex()
    private fun scope(session:SessionState,write:Boolean=false):String {
        check(session.isLoggedIn)
        val org=checkNotNull(session.activeOrganizationId);OrganizationId(org)
        check(session.authorizedOrganizations.any{it.organizationId==org && (it.role=="ADMIN" || (!write && it.role=="STANDARD"))})
        return org
    }
    private suspend fun unchanged(session:SessionState){check(auth.sessionState.first()==session){"Authorization changed"}}
    private suspend fun <T> guarded(block:suspend()->T):Result<T> = try{Result.success(block())}
        catch(e:CancellationException){throw e}catch(e:Exception){Result.failure(e)}

    override fun observeStorageSpaces(storageUnitId:StorageUnitId):Flow<List<StorageSpace>> =
        auth.sessionState.flatMapLatest { session ->
            val org=runCatching{scope(session)}.getOrNull()
            if(org==null)flowOf(emptyList()) else dao.observeByUnit(org,storageUnitId.value).map{rows->rows.map{it.domain(org,storageUnitId.value)}}
        }

    override suspend fun getStorageSpace(storageSpaceId:StorageSpaceId):Result<StorageSpace> = guarded {
        val session=auth.sessionState.first();val org=scope(session)
        checkNotNull(dao.get(org,storageSpaceId.value)).domain(org)
    }

    override suspend fun hasHistoricalReferences(storageSpaceId:StorageSpaceId):Result<Boolean> = guarded {
        val session=auth.sessionState.first();val org=scope(session)
        database.withTransaction {
            unchanged(session);checkNotNull(dao.get(org,storageSpaceId.value));dao.historicallyReferenced(storageSpaceId.value)
        }
    }

    override suspend fun refreshStorageSpaces(storageUnitId:StorageUnitId):Result<Unit> = guarded {
        val session=auth.sessionState.first();val org=scope(session)
        val parent=checkNotNull(dao.parent(org,storageUnitId.value))
        val rows=remote.list(org,storageUnitId.value).map{it.row(org,parent.locationId,storageUnitId.value)}
        check(rows.map{it.storageSpaceId}.distinct().size==rows.size)
        unchanged(session)
        database.withTransaction {
            unchanged(session);checkNotNull(dao.parent(org,storageUnitId.value))
            rows.forEach { row ->
                val old=dao.get(org,row.storageSpaceId);check(old==null || old.storageUnitId==storageUnitId.value)
                val pending=dao.unresolved(row.storageSpaceId)
                if(pending.isEmpty() && (old==null || old.entityVersion<row.entityVersion)) {
                    if(old==null)dao.insert(row) else dao.update(row)
                } else if(old!=null && row.entityVersion>old.entityVersion && pending.isNotEmpty() && pending.none{it.state==SyncOperationState.CONFLICT}) {
                    val operation=pending.first()
                    database.syncDependencyConflictDao().insertConflict(SyncConflictRoomEntity(UUID.randomUUID().toString(),org,operation.syncOperationId,
                        "STORAGE_SPACE",row.storageSpaceId,operation.payloadJson,Json.encodeToString(StorageSpaceResponseDto.serializer(),row.dto(parent.locationId)),
                        operation.baseEntityVersion,row.entityVersion,System.currentTimeMillis(),"OPEN"))
                    database.syncOperationDao().updateOperation(operation.copy(state=SyncOperationState.CONFLICT,lastError="REMOTE_VERSION_CONFLICT",updatedAt=System.currentTimeMillis()))
                }
            }
        }
    }

    override suspend fun saveStorageSpace(storageSpace:StorageSpace):Result<Unit> = guarded {
        val session=auth.sessionState.first();val org=scope(session,true);val actor=uuid(checkNotNull(session.userId))
        database.withTransaction {
            unchanged(session)
            val parent=checkNotNull(dao.parent(org,storageSpace.storageUnitId.value))
            val geometry=checkNotNull(dao.geometry(org,storageSpace.geometryProfileId.value))
            val old=dao.get(org,storageSpace.storageSpaceId.value)
            check(old==null || old.storageUnitId==storageSpace.storageUnitId.value){"Storage Space parent is immutable"}
            check((old?.entityVersion ?: 1L)==storageSpace.entityVersion){"Storage Space version conflict"}
            check(old==null || old.createdAt==storageSpace.createdAt.toEpochMilli() && old.createdBy==storageSpace.createdBy){"Creation provenance is immutable"}
            check(!dao.duplicate(storageSpace.storageUnitId.value,storageSpace.code,storageSpace.storageSpaceId.value)){"Storage Space Code already exists in Storage Unit"}
            val historical=old!=null && dao.historicallyReferenced(old.storageSpaceId)
            check(!historical || old.code==storageSpace.code){"Historically referenced Code is stable"}
            check(!historical || old.geometryProfileId==storageSpace.geometryProfileId.value){"Historically referenced Geometry Profile is stable"}
            check(old!=null && old.geometryProfileId==geometry.geometryProfileId || geometry.lifecycleStatus=="ACTIVE"){"New Geometry Profile selection must be Active"}
            val now=System.currentTimeMillis()
            val row=V6StorageSpacesRow(storageSpace.storageSpaceId.value,org,parent.storageUnitId,storageSpace.code,storageSpace.description,
                geometry.geometryProfileId,storageSpace.lifecycleStatus.name,old?.createdAt ?: now,old?.createdBy ?: actor,now,actor,storageSpace.entityVersion)
            row.domain(org,parent.storageUnitId)
            val operationId=UUID.randomUUID().toString()
            val payload=buildJsonObject {
                put("code",row.code);put("description",row.description?.let(::JsonPrimitive) ?: JsonNull)
                put("geometryProfileId",row.geometryProfileId);put("lifecycleStatus",row.lifecycleStatus)
                if(old==null)put("storageUnitId",row.storageUnitId)
            }
            val op=SyncOperationEntity(operationId,UUID.randomUUID().toString(),org,entityType="STORAGE_SPACE",entityId=row.storageSpaceId,
                operationType=if(old==null)"CREATE" else "PATCH",baseEntityVersion=old?.entityVersion,eventAt=now,payloadJson=payload.toString(),
                createdAt=maxOf(now,dao.lastQueuedAt(row.storageSpaceId)+1),retryMetadata=buildJsonObject{put("capturedByUserId",actor)}.toString())
            if(old==null)dao.insert(row) else check(dao.update(row)==1)
            database.syncOperationDao().insertOperation(op)
            (dao.unresolved(row.storageUnitId)+dao.unresolved(row.geometryProfileId))
                .filter{it.operationType=="CREATE"}.distinctBy{it.syncOperationId}.forEach { prerequisite ->
                    database.syncDependencyConflictDao().insertDependency(SyncOperationDependencyRoomEntity(UUID.randomUUID().toString(),
                        prerequisite.syncOperationId,operationId,"REQUIRED_BEFORE",now))
                }
        }
    }

    override suspend fun synchronizeStorageSpaces(deviceInstallationId:DeviceInstallationId,clientVersion:String):Result<Unit> = guarded {
        check(clientVersion.isNotBlank())
        pushes.withLock {
            val session=auth.sessionState.first();val org=scope(session,true);val blocked=mutableSetOf<String>()
            for(op in dao.pending(org)) {
                if(op.entityId in blocked)continue
                if(op.state !in setOf(SyncOperationState.PENDING,SyncOperationState.RETRYABLE_ERROR,SyncOperationState.IN_FLIGHT)){blocked+=op.entityId;continue}
                val next=op.retryMetadata?.let{Json.parseToJsonElement(it).jsonObject["nextAttemptAt"]?.jsonPrimitive?.longOrNull} ?: 0L
                if(op.state==SyncOperationState.RETRYABLE_ERROR && next>System.currentTimeMillis()){blocked+=op.entityId;continue}
                val dependencies=database.syncDependencyConflictDao().getDependencies(op.syncOperationId)
                if(dependencies.any{database.syncOperationDao().getOperationById(it.parentOperationId)?.state!=SyncOperationState.ACKNOWLEDGED}){blocked+=op.entityId;continue}
                unchanged(session)
                check(op.retryMetadata?.let{Json.parseToJsonElement(it).jsonObject["capturedByUserId"]?.jsonPrimitive?.content}==session.userId)
                val request=StorageUnitSyncBatchDto(deviceInstallationId.value,clientVersion,operations=listOf(StorageUnitSyncOperationDto(
                    op.syncOperationId,op.idempotencyKey,org,op.campaignId,op.entityType,op.entityId,op.operationType,op.baseEntityVersion,
                    Instant.ofEpochMilli(op.eventAt).toString(),dependencies.map{it.parentOperationId},Json.parseToJsonElement(op.payloadJson).jsonObject)))
                database.withTransaction {
                    unchanged(session);val durable=checkNotNull(database.syncOperationDao().getOperationById(op.syncOperationId))
                    val recovered=if(durable.state in setOf(SyncOperationState.IN_FLIGHT,SyncOperationState.RETRYABLE_ERROR))durable.copy(state=SyncOperationState.PENDING) else durable
                    database.syncOperationDao().updateOperation(recovered.copy(state=SyncOperationState.IN_FLIGHT,attemptCount=recovered.attemptCount+1,updatedAt=System.currentTimeMillis()))
                }
                val result=try{remote.push(request).also{check(it.results.size==1)}.results.single().also{check(it.syncOperationId==op.syncOperationId)}}
                catch(e:CancellationException){throw e}catch(e:Exception){
                    val retry=e !is StorageSpaceTransportFailure || e.retryable
                    database.withTransaction {val durable=checkNotNull(database.syncOperationDao().getOperationById(op.syncOperationId))
                        database.syncOperationDao().updateOperation(durable.copy(state=if(retry)SyncOperationState.RETRYABLE_ERROR else SyncOperationState.PERMANENT_ERROR,
                            retryMetadata=if(retry)retryMetadata(durable,(e as? StorageSpaceTransportFailure)?.retryAfter) else durable.retryMetadata,
                            lastError=(e as? StorageSpaceTransportFailure)?.code ?: "SERVICE_UNAVAILABLE",updatedAt=System.currentTimeMillis()))}
                    throw e
                }
                val server=if(result.outcome=="ACKNOWLEDGED")remote.get(org,op.entityId) else null
                unchanged(session)
                database.withTransaction {
                    unchanged(session);val current=checkNotNull(database.syncOperationDao().getOperationById(op.syncOperationId))
                    check(current.idempotencyKey==op.idempotencyKey && current.state==SyncOperationState.IN_FLIGHT)
                    when(result.outcome) {
                        "ACKNOWLEDGED" -> {
                            val old=checkNotNull(dao.get(org,op.entityId));val authoritative=checkNotNull(server).row(org,dao.parent(org,old.storageUnitId)!!.locationId,old.storageUnitId)
                            check(authoritative.entityVersion>=checkNotNull(result.entityVersion))
                            database.syncOperationDao().updateOperation(current.copy(state=SyncOperationState.ACKNOWLEDGED,lastError=null,updatedAt=System.currentTimeMillis(),
                                retryMetadata=buildJsonObject{put("capturedByUserId",session.userId);put("authoritativeEntityVersion",result.entityVersion)}.toString()))
                            if(dao.unresolved(op.entityId).isEmpty())dao.update(authoritative)
                        }
                        "CONFLICT" -> {
                            database.syncDependencyConflictDao().insertConflict(SyncConflictRoomEntity(UUID.randomUUID().toString(),org,op.syncOperationId,
                                "STORAGE_SPACE",op.entityId,op.payloadJson,result.conflict?.toString() ?: "{}",op.baseEntityVersion,result.entityVersion,System.currentTimeMillis(),"OPEN"))
                            database.syncOperationDao().updateOperation(current.copy(state=SyncOperationState.CONFLICT,lastError="SYNC_CONFLICT",updatedAt=System.currentTimeMillis()));blocked+=op.entityId
                        }
                        "RETRYABLE_ERROR","PERMANENT_ERROR" -> {
                            val state=if(result.outcome=="RETRYABLE_ERROR")SyncOperationState.RETRYABLE_ERROR else SyncOperationState.PERMANENT_ERROR
                            database.syncOperationDao().updateOperation(current.copy(state=state,lastError=result.error?.get("code")?.jsonPrimitive?.content ?: "VALIDATION_FAILED",
                                retryMetadata=if(state==SyncOperationState.RETRYABLE_ERROR)retryMetadata(current,null) else current.retryMetadata,updatedAt=System.currentTimeMillis()));blocked+=op.entityId
                        }
                        else -> error("Unsupported sync outcome")
                    }
                }
            }
        }
    }

    private fun retryMetadata(operation:SyncOperationEntity,retryAfter:String?):String {
        val now=System.currentTimeMillis();val delay=com.silomath.core.sync.offline.PackageDownloadRetryPolicy.delayMillis((operation.attemptCount-1).coerceAtLeast(0),retryAfter,now)
        val metadata=operation.retryMetadata?.let{Json.parseToJsonElement(it).jsonObject} ?: JsonObject(emptyMap())
        return JsonObject(metadata+("nextAttemptAt" to JsonPrimitive(if(delay>Long.MAX_VALUE-now)Long.MAX_VALUE else now+delay))).toString()
    }
    private fun V6StorageSpacesRow.domain(org:String,unit:String=storageUnitId):StorageSpace {
        check(organizationId==org && storageUnitId==unit);uuid(createdBy);uuid(updatedBy)
        return StorageSpace(StorageSpaceId(storageSpaceId),StorageUnitId(storageUnitId),code,description,GeometryProfileId(geometryProfileId),
            MasterStatus.valueOf(lifecycleStatus),Instant.ofEpochMilli(createdAt),createdBy,Instant.ofEpochMilli(updatedAt),updatedBy,entityVersion)
    }
    private fun StorageSpaceResponseDto.row(org:String,location:String,unit:String)=V6StorageSpacesRow(uuid(id),uuid(organizationId),uuid(storageUnitId),code,
        description,uuid(geometryProfileId),lifecycleStatus,Instant.parse(createdAt).toEpochMilli(),uuid(createdBy),Instant.parse(updatedAt).toEpochMilli(),uuid(updatedBy),entityVersion)
        .also{check(it.organizationId==org && this.locationId==location && it.storageUnitId==unit);it.domain(org,unit)}
    private fun V6StorageSpacesRow.dto(location:String)=StorageSpaceResponseDto(storageSpaceId,organizationId,location,storageUnitId,code,description,geometryProfileId,
        lifecycleStatus,Instant.ofEpochMilli(createdAt).toString(),createdBy,Instant.ofEpochMilli(updatedAt).toString(),updatedBy,entityVersion)
    private fun uuid(value:String)=value.also{check(UUID.fromString(value).toString()==value)}
}

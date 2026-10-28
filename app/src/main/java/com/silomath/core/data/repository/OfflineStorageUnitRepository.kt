package com.silomath.core.data.repository

import androidx.room.withTransaction
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.*
import com.silomath.core.database.entity.canonical.*
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import com.silomath.core.model.SyncOperationState
import com.silomath.core.model.storage.StorageUnitTransfer
import com.silomath.core.data.network.dto.*
import com.silomath.core.data.network.source.StorageUnitRemoteSource
import com.silomath.core.data.network.source.StorageUnitTransportFailure
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** IADR-0002 canonical UUID store. No numeric/remoteId projection is operational authority. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineStorageUnitRepository @Inject constructor(private val database:SiloMathDatabase,
    private val auth:AuthRepository,private val remote:StorageUnitRemoteSource):StorageUnitRepository {
    private val dao get()=database.storageUnitCanonicalDao()
    private val pushes=Mutex()
    private fun scope(s:SessionState,write:Boolean=false):String {
        check(s.isLoggedIn);val org=checkNotNull(s.activeOrganizationId);OrganizationId(org)
        check(s.authorizedOrganizations.any{it.organizationId==org && (it.role=="ADMIN" || (!write && it.role=="STANDARD"))})
        return org
    }
    private suspend fun unchanged(s:SessionState){check(auth.sessionState.first()==s){"Authorization changed"}}
    private suspend fun <T> guarded(block:suspend()->T):Result<T> = try{Result.success(block())}
        catch(e:CancellationException){throw e}catch(e:Exception){Result.failure(e)}
    override fun observeStorageUnits(locationId:LocationId):Flow<List<StorageUnit>> = auth.sessionState.flatMapLatest { session ->
        val org=runCatching{scope(session)}.getOrNull()
        if(org==null)flowOf(emptyList()) else
            dao.observe(org,locationId.value).map{rows -> rows.map{it.domain(org,locationId.value)}}
    }

    override suspend fun refreshStorageUnits(locationId:LocationId):Result<Unit> = guarded {
        val session=auth.sessionState.first();val org=scope(session)
        // Only complete authenticated V6 responses establish parent provenance; never legacy inference.
        val organization=remote.organization(org);val parent=remote.parent(org,locationId.value)
        check(organization.id==org && parent.id==locationId.value && parent.organizationId==org)
        MasterStatus.valueOf(organization.lifecycleStatus)
        Location(LocationId(parent.id),OrganizationId(parent.organizationId),parent.name,parent.code,parent.address,parent.city,parent.country,parent.notes,
            MasterStatus.valueOf(parent.lifecycleStatus),Instant.parse(parent.createdAt),uuid(parent.createdBy),Instant.parse(parent.updatedAt),uuid(parent.updatedBy),
            version(parent.entityVersion),parent.latitude,parent.longitude)
        val rows=remote.list(org,locationId.value).map{it.row(org,locationId.value)}
        check(rows.map{it.storageUnitId}.distinct().size==rows.size)
        unchanged(session)
        database.withTransaction {
            unchanged(session)
            if(dao.organization(org)==null)dao.insertOrganization(V6OrganizationsRow(org,organization.name,organization.code,
                organization.address,organization.city,organization.country,organization.lifecycleStatus,
                timestamp(organization.createdAt),uuid(organization.createdBy),timestamp(organization.updatedAt),uuid(organization.updatedBy),version(organization.entityVersion)))
            if(dao.parent(org,locationId.value)==null)dao.insertParent(V6LocationsRow(locationId.value,org,parent.name,parent.code,
                parent.address,parent.city,parent.country,parent.notes,parent.latitude?.toPlainString(),parent.longitude?.toPlainString(),parent.lifecycleStatus,
                timestamp(parent.createdAt),uuid(parent.createdBy),timestamp(parent.updatedAt),uuid(parent.updatedBy),version(parent.entityVersion)))
            rows.forEach { row ->
                val old=dao.get(row.storageUnitId)
                check(old==null || old.organizationId==org && old.locationId==locationId.value)
                val pending=dao.unresolved(row.storageUnitId)
                if(pending.isEmpty() && (old==null || old.entityVersion<row.entityVersion)) {
                    if(old==null)dao.insert(row) else dao.update(row)
                } else if(old!=null && row.entityVersion>old.entityVersion && pending.isNotEmpty() && pending.none{it.state==SyncOperationState.CONFLICT}) {
                    val operation=pending.first()
                    val authoritative=Json.encodeToString(StorageUnitResponseDto.serializer(),StorageUnitResponseDto(row.storageUnitId,row.organizationId,
                        row.locationId,row.name,row.code,row.description,row.lifecycleStatus,Instant.ofEpochMilli(row.createdAt).toString(),row.createdBy,
                        Instant.ofEpochMilli(row.updatedAt).toString(),row.updatedBy,row.entityVersion))
                    database.syncDependencyConflictDao().insertConflict(SyncConflictRoomEntity(UUID.randomUUID().toString(),org,operation.syncOperationId,
                        "STORAGE_UNIT",row.storageUnitId,operation.payloadJson,authoritative,operation.baseEntityVersion,row.entityVersion,System.currentTimeMillis(),"OPEN"))
                    database.syncOperationDao().updateOperation(operation.copy(state=SyncOperationState.CONFLICT,lastError="SYNC_CONFLICT",updatedAt=System.currentTimeMillis()))
                }
            }
        }
    }
    override suspend fun saveStorageUnit(storageUnit:StorageUnit):Result<Unit> = guarded {
        val session=auth.sessionState.first();val org=scope(session,true);val actor=uuid(checkNotNull(session.userId))
        database.withTransaction {
            unchanged(session)
            if(dao.parent(org,storageUnit.locationId.value)==null) {
                // Explicit reconciliation of a captured, still-pending canonical Location CREATE.
                // Incomplete legacy rows/guessed metadata never qualify; Organization provenance must already be verified.
                checkNotNull(dao.organization(org)){"Verified Organization unavailable"}
                val parentOps=dao.unresolved(storageUnit.locationId.value)
                check(parentOps.any{it.entityType=="LOCATION" && it.operationType=="CREATE" && it.organizationId==org && it.baseEntityVersion==null})
                val parent=checkNotNull(database.locationDao().getLocationByLocationId(storageUnit.locationId.value))
                check(parent.canonicalOrganizationId==org && parent.locationId==storageUnit.locationId.value && parent.createdBy==actor)
                val fact=Location(LocationId(checkNotNull(parent.locationId)),OrganizationId(org),parent.name,parent.code,parent.address,parent.city,parent.country,parent.notes,
                    MasterStatus.valueOf(parent.lifecycleStatus),Instant.ofEpochMilli(checkNotNull(parent.createdAt)),uuid(checkNotNull(parent.createdBy)),
                    Instant.ofEpochMilli(checkNotNull(parent.updatedAt)),uuid(checkNotNull(parent.updatedBy)),checkNotNull(parent.entityVersion),parent.latitudeDeg,parent.longitudeDeg)
                dao.insertParent(V6LocationsRow(fact.locationId.value,org,fact.name,fact.code,fact.address,fact.city,fact.country,fact.notes,
                    fact.latitude?.toPlainString(),fact.longitude?.toPlainString(),fact.lifecycleStatus.name,fact.createdAt.toEpochMilli(),fact.createdBy,
                    fact.updatedAt.toEpochMilli(),fact.updatedBy,fact.entityVersion))
            }
            val old=dao.get(storageUnit.storageUnitId.value)
            check(old==null || old.organizationId==org && old.locationId==storageUnit.locationId.value){"Storage Unit parent is immutable"}
            check((old?.entityVersion ?: 1L)==storageUnit.entityVersion){"Storage Unit version conflict"}
            check(old==null || old.createdAt==storageUnit.createdAt.toEpochMilli() && old.createdBy==storageUnit.createdBy){"Creation provenance is immutable"}
            check(!dao.duplicate(storageUnit.locationId.value,storageUnit.code,storageUnit.storageUnitId.value)){"Storage Unit Code already exists in Location"}
            check(old==null || old.code==storageUnit.code || !dao.historicallyReferenced(old.storageUnitId)){"Historically referenced Code is stable"}
            val now=System.currentTimeMillis()
            val row=V6StorageUnitsRow(storageUnit.storageUnitId.value,org,storageUnit.locationId.value,storageUnit.name,storageUnit.code,
                storageUnit.description,storageUnit.lifecycleStatus.name,old?.createdAt ?: now,old?.createdBy ?: actor,now,actor,storageUnit.entityVersion)
            row.domain(org,storageUnit.locationId.value)
            val id=UUID.randomUUID().toString()
            val payload=buildJsonObject {
                put("name",row.name);put("code",row.code);put("description",row.description?.let(::JsonPrimitive) ?: JsonNull);put("lifecycleStatus",row.lifecycleStatus)
                if(old==null)put("locationId",row.locationId) // CREATE sync intent's canonical parent, never a PATCH reassignment.
            }
            val op=SyncOperationEntity(syncOperationId=id,idempotencyKey=UUID.randomUUID().toString(),organizationId=org,
                entityType="STORAGE_UNIT",entityId=row.storageUnitId,operationType=if(old==null)"CREATE" else "PATCH",
                baseEntityVersion=old?.entityVersion,eventAt=now,payloadJson=payload.toString(),
                createdAt=maxOf(now,dao.lastQueuedAt(row.storageUnitId)+1),
                retryMetadata=buildJsonObject{put("capturedByUserId",actor)}.toString())
            if(old==null)dao.insert(row) else check(dao.update(row)==1)
            database.syncOperationDao().insertOperation(op)
            // A locally created parent must be acknowledged before the child may be dispatched.
            dao.unresolved(row.locationId).filter{it.entityType=="LOCATION" && it.operationType=="CREATE"}.forEach {parent ->
                database.syncDependencyConflictDao().insertDependency(SyncOperationDependencyRoomEntity(UUID.randomUUID().toString(),
                    parent.syncOperationId,id,"REQUIRED_BEFORE",now))
            }
        }
    }
    override suspend fun synchronizeStorageUnits(deviceInstallationId:DeviceInstallationId,clientVersion:String):Result<Unit> = guarded {
        check(clientVersion.isNotBlank())
        pushes.withLock {
            val session=auth.sessionState.first();val org=scope(session,true);val blocked=mutableSetOf<String>()
            for(op in dao.pending(org)) {
                if(op.entityId in blocked)continue
                if(op.state !in setOf(SyncOperationState.PENDING,SyncOperationState.RETRYABLE_ERROR,SyncOperationState.IN_FLIGHT)){blocked+=op.entityId;continue}
                val nextAttempt=op.retryMetadata?.let{Json.parseToJsonElement(it).jsonObject["nextAttemptAt"]?.jsonPrimitive?.longOrNull} ?: 0L
                if(op.state==SyncOperationState.RETRYABLE_ERROR && nextAttempt>System.currentTimeMillis()){blocked+=op.entityId;continue}
                val dependencies=database.syncDependencyConflictDao().getDependencies(op.syncOperationId)
                if(dependencies.any{database.syncOperationDao().getOperationById(it.parentOperationId)?.state!=SyncOperationState.ACKNOWLEDGED}){blocked+=op.entityId;continue}
                unchanged(session)
                check(op.retryMetadata?.let{Json.parseToJsonElement(it).jsonObject["capturedByUserId"]?.jsonPrimitive?.content}==session.userId) {
                    "Queued work requires its capturing actor; it is retained for controlled recovery"
                }
                val request=StorageUnitSyncBatchDto(deviceInstallationId.value,clientVersion,operations=listOf(StorageUnitSyncOperationDto(
                    op.syncOperationId,op.idempotencyKey,org,op.campaignId,op.entityType,op.entityId,op.operationType,op.baseEntityVersion,
                    Instant.ofEpochMilli(op.eventAt).toString(),dependencies.map{it.parentOperationId},Json.parseToJsonElement(op.payloadJson).jsonObject)))
                database.withTransaction {
                    unchanged(session)
                    // This singleton owner holds its run lock: retained IN_FLIGHT means interrupted prior run, never success.
                    val durable=checkNotNull(database.syncOperationDao().getOperationById(op.syncOperationId))
                    if(durable.state in setOf(SyncOperationState.IN_FLIGHT,SyncOperationState.RETRYABLE_ERROR))
                        database.syncOperationDao().updateOperation(durable.copy(state=SyncOperationState.PENDING,updatedAt=System.currentTimeMillis()))
                    database.syncOperationDao().updateOperation(durable.copy(state=SyncOperationState.IN_FLIGHT,
                        attemptCount=durable.attemptCount+1,updatedAt=System.currentTimeMillis()))
                }
                val (result,server)=try {
                    val response=remote.push(request).also{check(it.results.size==1)}.results.single().also{check(it.syncOperationId==op.syncOperationId)}
                    response to if(response.outcome=="ACKNOWLEDGED")remote.get(org,op.entityId) else null
                } catch(e:CancellationException){throw e} catch(e:Exception) {
                    val retry=e !is StorageUnitTransportFailure || e.retryable
                    val code=if(e is StorageUnitTransportFailure)e.code else "SERVICE_UNAVAILABLE"
                    database.withTransaction {
                        val durable=checkNotNull(database.syncOperationDao().getOperationById(op.syncOperationId))
                        database.syncOperationDao().updateOperation(durable.copy(
                            state=if(retry)SyncOperationState.RETRYABLE_ERROR else SyncOperationState.PERMANENT_ERROR,
                            retryMetadata=if(retry)retryMetadata(durable,(e as? StorageUnitTransportFailure)?.retryAfter) else durable.retryMetadata,
                            lastError=code,updatedAt=System.currentTimeMillis()))
                    }
                    throw e
                }
                unchanged(session)
                database.withTransaction {
                    unchanged(session)
                    val current=checkNotNull(database.syncOperationDao().getOperationById(op.syncOperationId))
                    check(current.idempotencyKey==op.idempotencyKey && current.state==SyncOperationState.IN_FLIGHT)
                    when(result.outcome) {
                        "ACKNOWLEDGED" -> {
                            val old=checkNotNull(dao.get(op.entityId));val row=checkNotNull(server).row(org,old.locationId)
                            check(row.storageUnitId==op.entityId && row.entityVersion>=checkNotNull(result.entityVersion))
                            database.syncOperationDao().updateOperation(current.copy(state=SyncOperationState.ACKNOWLEDGED,
                                retryMetadata=buildJsonObject{put("capturedByUserId",session.userId);put("authoritativeEntityVersion",result.entityVersion)}.toString(),lastError=null,updatedAt=System.currentTimeMillis()))
                            if(dao.unresolved(op.entityId).isEmpty())dao.update(row) // Later local intents are never silently overwritten/rebased.
                        }
                        "CONFLICT" -> {
                            database.syncDependencyConflictDao().insertConflict(SyncConflictRoomEntity(UUID.randomUUID().toString(),org,op.syncOperationId,
                                "STORAGE_UNIT",op.entityId,op.payloadJson,result.conflict?.toString() ?: "{}",op.baseEntityVersion,result.entityVersion,
                                System.currentTimeMillis(),"OPEN"))
                            database.syncOperationDao().updateOperation(current.copy(state=SyncOperationState.CONFLICT,lastError="SYNC_CONFLICT",updatedAt=System.currentTimeMillis()));blocked+=op.entityId
                        }
                        "RETRYABLE_ERROR","PERMANENT_ERROR" -> {
                            val state=if(result.outcome=="RETRYABLE_ERROR")SyncOperationState.RETRYABLE_ERROR else SyncOperationState.PERMANENT_ERROR
                            database.syncOperationDao().updateOperation(current.copy(state=state,updatedAt=System.currentTimeMillis(),retryMetadata=if(state==SyncOperationState.RETRYABLE_ERROR)retryMetadata(current,null) else current.retryMetadata,lastError=result.error?.get("code")?.jsonPrimitive?.content ?: "VALIDATION_FAILED"));blocked+=op.entityId
                        }
                        else -> error("Unsupported sync outcome")
                    }
                }
            }
        }
    }
    private fun retryMetadata(operation:SyncOperationEntity,retryAfter:String?):String {
        val now=System.currentTimeMillis()
        val delay=com.silomath.core.sync.offline.PackageDownloadRetryPolicy.delayMillis((operation.attemptCount-1).coerceAtLeast(0),retryAfter,now)
        val due=if(delay>Long.MAX_VALUE-now)Long.MAX_VALUE else now+delay
        val metadata=operation.retryMetadata?.let{Json.parseToJsonElement(it).jsonObject} ?: JsonObject(emptyMap())
        return JsonObject(metadata+mapOf("nextAttemptAt" to JsonPrimitive(due))).toString()
    }
    private fun V6StorageUnitsRow.domain(org:String,location:String):StorageUnit {
        check(organizationId==org && locationId==location)
        uuid(createdBy);uuid(updatedBy)
        return StorageUnit(StorageUnitId(storageUnitId),LocationId(locationId),name,code,description,MasterStatus.valueOf(lifecycleStatus),
            Instant.ofEpochMilli(createdAt),createdBy,Instant.ofEpochMilli(updatedAt),updatedBy,entityVersion)
    }
    private fun StorageUnitTransfer.row(org:String,location:String)=V6StorageUnitsRow(uuid(id),uuid(organizationId),uuid(locationId),name,code,description,
        lifecycleStatus,timestamp(createdAt),uuid(createdBy),timestamp(updatedAt),uuid(updatedBy),version(entityVersion)).also{it.domain(org,location)}
    private fun uuid(value:String)=value.also{check(UUID.fromString(value).toString()==value)}
    private fun timestamp(value:String)=Instant.parse(value).toEpochMilli()
    private fun version(value:Long)=value.also{check(it>=1)}
}

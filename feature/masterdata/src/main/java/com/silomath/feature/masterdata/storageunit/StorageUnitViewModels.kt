package com.silomath.feature.masterdata.storageunit

import androidx.lifecycle.*
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal fun SessionState.canRead(org:OrganizationId)=isLoggedIn && activeOrganizationId==org.value &&
    authorizedOrganizations.any{it.organizationId==org.value && it.role in setOf("ADMIN","STANDARD")}
internal fun SessionState.canWrite(org:OrganizationId)=canRead(org) && authorizedOrganizations.any{it.organizationId==org.value && it.role=="ADMIN"}

data class StorageUnitDetailsState(val location:Location?=null,val unit:StorageUnit?=null,val spaces:List<StorageSpace> = emptyList(),
    val loading:Boolean=true,val admin:Boolean=false,val error:String?=null,val spacesError:String?=null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StorageUnitDetailsViewModel @Inject constructor(private val auth:AuthRepository,private val locations:LocationRepository,
    private val units:StorageUnitRepository,private val spaces:StorageSpaceReader):ViewModel() {
    private val target=MutableStateFlow<Triple<OrganizationId,LocationId,StorageUnitId>?>(null)
    fun bind(org:OrganizationId,location:LocationId,unit:StorageUnitId){target.value=Triple(org,location,unit)}
    val uiState=combine(auth.sessionState,target){s,t->s to t}.flatMapLatest {(session,ids)->
        if(ids==null)flowOf(StorageUnitDetailsState())
        else if(!session.canRead(ids.first))flowOf(StorageUnitDetailsState(loading=false,error="Storage Unit is not available."))
        else combine(locations.observeLocations(ids.first),units.observeStorageUnits(ids.second),spaces.observe(ids.first,ids.second,ids.third)){parents,children,result->
            val parent=parents.firstOrNull{it.organizationId==ids.first && it.locationId==ids.second}
            val child=children.firstOrNull{it.locationId==ids.second && it.storageUnitId==ids.third}
            if(parent==null || child==null)StorageUnitDetailsState(loading=false,error="Storage Unit is not available locally.")
            else StorageUnitDetailsState(parent,child,result.getOrNull().orEmpty(),loading=false,admin=session.canWrite(ids.first),
                spacesError=if(result.isFailure)"Storage Spaces could not be loaded." else null)
        }.onStart{emit(StorageUnitDetailsState())}.catch{emit(StorageUnitDetailsState(loading=false,error="Storage Unit could not be loaded."))}
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),StorageUnitDetailsState())
}

data class StorageUnitDraft(val name:String="",val code:String="",val description:String="",val status:MasterStatus=MasterStatus.ACTIVE) {
    fun model(id:StorageUnitId,parent:LocationId,actor:String,original:StorageUnit?,now:Instant)=StorageUnit(
        original?.storageUnitId ?: id,parent,name.trim(),code.trim().uppercase(Locale.ROOT),
        if(description==original?.description.orEmpty())original?.description else description.ifEmpty{null},status,
        original?.createdAt ?: now,original?.createdBy ?: actor,now,actor,original?.entityVersion ?: 1L)
    companion object {fun from(row:StorageUnit)=StorageUnitDraft(row.name,row.code,row.description.orEmpty(),row.lifecycleStatus)}
}
data class StorageUnitEditorState(val location:Location?=null,val draft:StorageUnitDraft=StorageUnitDraft(),val loading:Boolean=true,
    val allowed:Boolean=false,val saving:Boolean=false,val codeLocked:Boolean=false,val error:String?=null)

@HiltViewModel
class StorageUnitEditorViewModel @Inject constructor(private val auth:AuthRepository,private val locations:LocationRepository,
    private val units:StorageUnitRepository,private val saved:SavedStateHandle):ViewModel() {
    private val state=MutableStateFlow(StorageUnitEditorState());val uiState=state.asStateFlow()
    private val events=Channel<Unit>(Channel.BUFFERED);val savedEvents=events.receiveAsFlow()
    private var target:Triple<OrganizationId,LocationId,StorageUnitId?>?=null
    private var original:StorageUnit?=null;private var loaded=false;private var binding:Job?=null;private var draftKey="";private var loadedActor:String?=null
    fun bind(org:OrganizationId,location:LocationId,id:StorageUnitId?) {
        if(target==Triple(org,location,id))return
        target=Triple(org,location,id);loaded=false;original=null;binding?.cancel();state.value=StorageUnitEditorState()
        binding=viewModelScope.launch {auth.sessionState.collectLatest {session->
            if(loaded && loadedActor!=session.userId){loaded=false;original=null;state.value=StorageUnitEditorState()}
            if(!session.canWrite(org)){loaded=false;original=null;state.value=StorageUnitEditorState(loading=false,error="Storage Unit editing requires an authorized Admin.");return@collectLatest}
            try {
                combine(locations.observeLocations(org),units.observeStorageUnits(location)){parents,children->
                    parents.firstOrNull{it.organizationId==org && it.locationId==location} to id?.let{key->children.firstOrNull{it.storageUnitId==key && it.locationId==location}}
                }.collect {(parent,row)->
                    if(parent==null || id!=null && row==null){state.value=StorageUnitEditorState(loading=false,error="Storage Unit parent/context is not available locally.");return@collect}
                    if(!loaded) {
                        draftKey="unit.${session.userId}.${org.value}.${location.value}.${id?.value ?: "new"}."
                        original=row
                        val fallback=row?.let(StorageUnitDraft::from) ?: StorageUnitDraft()
                        val values=saved.get<ArrayList<String>>(draftKey+"draft")
                        val draft=if(values?.size==4)StorageUnitDraft(values[0],values[1],values[2],MasterStatus.entries.find{it.name==values[3]} ?: fallback.status) else fallback
                        val locked=id?.let{units.hasHistoricalReferences(location,it).getOrThrow()} ?: false
                        if(auth.sessionState.first()!=session)return@collect
                        state.value=StorageUnitEditorState(parent,draft,loading=false,allowed=true,codeLocked=locked);loadedActor=session.userId;loaded=true
                    } else if(row!=original && !state.value.saving)state.value=state.value.copy(allowed=false,error="Storage Unit changed. Close and reopen before saving.")
                }
            } catch(cancel:CancellationException){throw cancel}
            catch(_:Exception){state.value=state.value.copy(loading=false,allowed=false,error="Storage Unit editing context could not be verified.")}
        }}
    }
    fun update(draft:StorageUnitDraft){
        if(!state.value.allowed || state.value.saving || state.value.codeLocked && draft.code!=state.value.draft.code)return
        state.value=state.value.copy(draft=draft,error=null)
        saved[draftKey+"draft"]=arrayListOf(draft.name,draft.code,draft.description,draft.status.name)
    }
    fun save()=viewModelScope.launch {
        val ids=target ?: return@launch;val current=state.value
        if(!current.allowed || current.loading || current.saving)return@launch
        val session=auth.sessionState.first()
        if(!session.canWrite(ids.first)){state.value=current.copy(allowed=false,error="Storage Unit editing requires an authorized Admin.");return@launch}
        state.value=current.copy(saving=true,error=null)
        try {
            check(locations.observeLocations(ids.first).first().any{it.organizationId==ids.first && it.locationId==ids.second})
            val latest=ids.third?.let{id->units.observeStorageUnits(ids.second).first().firstOrNull{it.storageUnitId==id && it.locationId==ids.second}}
            check(latest==original)
            val locked=ids.third?.let{units.hasHistoricalReferences(ids.second,it).getOrThrow()} ?: false
            check(!locked || current.draft.code.trim().uppercase(Locale.ROOT)==original?.code)
            val id=ids.third ?: StorageUnitId(saved.get<String>(draftKey+"newId") ?: UUID.randomUUID().toString().also{saved[draftKey+"newId"]=it})
            val row=current.draft.model(id,ids.second,checkNotNull(session.userId),original,Instant.now()) // Domain validates; server timestamps stay authoritative.
            check(auth.sessionState.first()==session && state.value.allowed)
            units.saveStorageUnit(row).getOrThrow()
            saved.remove<ArrayList<String>>(draftKey+"draft");state.value=state.value.copy(saving=false,allowed=false)
            events.send(Unit) // Never emit success before the MD-003 local business+outbox transaction commits.
        } catch(cancel:CancellationException){throw cancel}
        catch(_:Exception){state.value=state.value.copy(saving=false,error="Storage Unit could not be saved. Check Name, Code and current data, then retry.")}
    }
}

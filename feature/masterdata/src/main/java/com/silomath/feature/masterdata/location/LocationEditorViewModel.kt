package com.silomath.feature.masterdata.location

import androidx.lifecycle.*
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class LocationEditorState(val draft:LocationDraft=LocationDraft(),val loading:Boolean=true,val saving:Boolean=false,
    val allowed:Boolean=false,val codeLocked:Boolean=false,val errors:Map<String,String> = emptyMap(),val error:String?=null)

@HiltViewModel
class LocationEditorViewModel @Inject constructor(private val auth:AuthRepository,private val locations:LocationRepository,
    private val saved:SavedStateHandle):ViewModel() {
    private val state=MutableStateFlow(LocationEditorState())
    val uiState=state.asStateFlow()
    private val events=Channel<Unit>(Channel.BUFFERED)
    val savedEvents=events.receiveAsFlow()
    private var target:Pair<OrganizationId,LocationId?>?=null
    private var original:Location?=null
    private var loaded=false
    private var binding:Job?=null
    fun bind(org:OrganizationId,id:LocationId?) {
        if(target==org to id)return
        target=org to id;loaded=false;original=null
        binding?.cancel();state.value=LocationEditorState()
        binding=viewModelScope.launch {
            auth.sessionState.collectLatest {session ->
                if(!authorized(session,org)) {loaded=false;original=null;state.value=LocationEditorState(loading=false,error="Location editing requires an authorized Admin.");return@collectLatest}
                try {
                    locations.observeLocations(org).collect {rows ->
                        val row=if(id==null)null else rows.firstOrNull{it.locationId==id && it.organizationId==org}
                        if(id!=null && row==null) {state.value=LocationEditorState(loading=false,error="Location is not available locally.");return@collect}
                        if(!loaded) {
                            val lock=if(id==null)false else locations.hasHistoricalReferences(org,id).getOrThrow()
                            original=row
                            val draft=restoreDraft(row?.let(LocationDraft::from) ?: LocationDraft())
                            state.value=LocationEditorState(draft,loading=false,allowed=true,codeLocked=lock)
                            loaded=true
                        } else if(row!=original) {
                            state.value=state.value.copy(allowed=false,error="Location changed. Close and reopen the editor before saving.")
                        }
                    }
                } catch(cancel:CancellationException){throw cancel}
                catch(_:Exception){state.value=state.value.copy(loading=false,allowed=false,error="Location editing context could not be verified.")}
            }
        }
    }
    fun update(draft:LocationDraft) {
        if(!state.value.allowed || state.value.saving)return
        state.value=state.value.copy(draft=draft,errors=emptyMap(),error=null)
        saved["editor.countryIsNull"]=draft.country==null
        saved["editor.draft"]=arrayListOf(draft.name,draft.code,draft.address,draft.city,draft.country ?: "",draft.notes,
            draft.status.name,draft.latitude,draft.longitude)
    }
    private fun restoreDraft(fallback:LocationDraft):LocationDraft {
        val values=saved.get<ArrayList<String>>("editor.draft") ?: return fallback
        if(values.size!=9)return fallback
        return LocationDraft(values[0],values[1],values[2],values[3],if(saved.get<Boolean>("editor.countryIsNull")==true)null else values[4],values[5],
            MasterStatus.entries.find{it.name==values[6]} ?: fallback.status,values[7],values[8])
    }
    fun save()=viewModelScope.launch {
        val ids=target ?: return@launch
        val current=state.value
        if(!current.allowed || current.saving || current.loading)return@launch
        val session=auth.sessionState.first()
        if(!authorized(session,ids.first)){state.value=current.copy(allowed=false,error="Location editing requires an authorized Admin.");return@launch}
        state.value=current.copy(saving=true,error=null)
        try {
            val rows=locations.observeLocations(ids.first).first().filter{it.organizationId==ids.first}
            val latest=ids.second?.let{id -> rows.firstOrNull{it.locationId==id}}
            require(latest==original){"Stale Location"}
            val locked=ids.second?.let{locations.hasHistoricalReferences(ids.first,it).getOrThrow()} ?: false
            val errors=current.draft.errors(original,rows,locked)
            if(errors.isNotEmpty()){state.value=current.copy(errors=errors,codeLocked=locked);return@launch}
            if(auth.sessionState.first()!=session || !state.value.allowed){state.value=current.copy(allowed=false,error="Authorization changed. Reopen the editor.");return@launch}
            val draft=current.draft
            val now=Instant.now() // Local mutation event time; server remains authoritative on acknowledgement.
            val actor=checkNotNull(session.userId)
            val row=Location(original?.locationId ?: LocationId(saved.get<String>("editor.newId") ?: UUID.randomUUID().toString().also{saved["editor.newId"]=it}),ids.first,
                draft.name.trim(),draft.code.trim().uppercase(Locale.ROOT),draft.address.ifEmpty{null},draft.city.ifEmpty{null},
                draft.country,draft.notes.ifEmpty{null},draft.status,original?.createdAt ?: now,original?.createdBy ?: actor,
                now,actor,original?.entityVersion ?: 1L,draft.latitude.trim().toBigDecimalOrNull(),draft.longitude.trim().toBigDecimalOrNull())
            locations.saveLocation(row).getOrThrow()
            saved.remove<ArrayList<String>>("editor.draft")
            state.value=state.value.copy(saving=false,allowed=false)
            events.send(Unit) // Only after durable local business row + outbox transaction success.
        } catch(cancel:CancellationException){throw cancel}
        catch(_:Exception){state.value=state.value.copy(saving=false,error="Location could not be saved. Review the current data and retry.")}
    }
    private fun authorized(session:SessionState,org:OrganizationId)=session.isLoggedIn && session.activeOrganizationId==org.value &&
        session.authorizedOrganizations.any{it.organizationId==org.value && it.role=="ADMIN"}
}

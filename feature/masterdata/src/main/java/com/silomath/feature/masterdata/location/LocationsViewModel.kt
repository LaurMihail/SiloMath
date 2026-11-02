package com.silomath.feature.masterdata.location

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.LocationRepository
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.navigation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationsViewModel @Inject constructor(private val auth: AuthRepository,
    private val repository: LocationRepository, private val saved: SavedStateHandle):ViewModel() {
    private val query=saved.getStateFlow("location.query", "")
    private val lifecycle=saved.getStateFlow("location.lifecycle", LifecycleFilter.Active.name)
    private val mode=saved.getStateFlow("location.view",LocationView.List.name)
    private val selected=saved.getStateFlow<String?>("location.selected",null)
    private val refresh=MutableStateFlow(0)
    private val navigationError=MutableStateFlow<String?>(null)
    private val requests=Channel<OrganizationDestination>(Channel.BUFFERED)
    val navigationRequests=requests.receiveAsFlow()

    private val source=combine(auth.sessionState,refresh) { session,_ -> session }.flatMapLatest { session ->
        val org=authorizedOrganization(session)
        if (org == null) flowOf(LocationUiState(loading=false,error="Select an authorized organization to view Locations."))
        else repository.observeLocations(org).map { rows ->
            val authorized=rows.filter {it.organizationId==org}
            LocationUiState(org,authorized,authorized.size,isAdmin=session.authorizedOrganizations.any {
                it.organizationId==org.value && it.role=="ADMIN"
            },loading=false)
        }.onStart {emit(LocationUiState(organizationId=org))}
            .catch {emit(LocationUiState(organizationId=org,loading=false,error="Locations could not be loaded."))}
    }
    private data class Presentation(val query:String,val lifecycle:LifecycleFilter,
        val view:LocationView,val selected:LocationId?)
    private val presentation=combine(query,lifecycle,mode,selected) { q,life,view,id ->
        Presentation(q,enumValues<LifecycleFilter>().find {it.name==life} ?: LifecycleFilter.Active,
            enumValues<LocationView>().find {it.name==view} ?: LocationView.List,
            id?.let {runCatching{LocationId(it)}.getOrNull()})
    }
    val uiState=combine(source,presentation,navigationError) { data,p,error ->
        val filtered=data.organizationId?.let {filteredLocations(data.locations,it,p.query,p.lifecycle)} ?: emptyList()
        data.copy(locations=filtered,query=p.query,lifecycle=p.lifecycle,view=p.view,
            selectedId=p.selected?.takeIf {id -> filtered.any {it.locationId==id}},navigationError=error)
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),LocationUiState())

    fun search(value:String) {saved["location.query"]=value;navigationError.value=null}
    fun filter(value:LifecycleFilter) {saved["location.lifecycle"]=value.name;navigationError.value=null}
    fun view(value:LocationView) {saved["location.view"]=value.name;navigationError.value=null}
    fun retry() {refresh.value++}
    fun navigationUnavailable(editing:Boolean) {
        navigationError.value=if(editing) "Location editing is currently unavailable." else "Location could not be opened."
    }
    fun open(id:LocationId)=viewModelScope.launch {
        val session=auth.sessionState.first();val org=authorizedOrganization(session) ?: return@launch
        if (uiState.value.organizationId!=org || uiState.value.locations.none {it.locationId==id}) return@launch
        saved["location.selected"]=id.value
        requests.send(LocationUnits(org,id))
    }
    fun edit(id:LocationId?=null)=viewModelScope.launch {
        val session=auth.sessionState.first();val org=authorizedOrganization(session) ?: return@launch
        if(session.authorizedOrganizations.none {it.organizationId==org.value && it.role=="ADMIN"}) return@launch
        if(uiState.value.organizationId!=org || (id!=null && uiState.value.locations.none{it.locationId==id})) return@launch
        requests.send(LocationEditor(org,id))
    }
    private fun authorizedOrganization(session:SessionState):OrganizationId? {
        if(!session.isLoggedIn) return null
        val id=session.activeOrganizationId ?: return null
        if(session.authorizedOrganizations.none{it.organizationId==id && it.role in setOf("ADMIN","STANDARD")}) return null
        return runCatching{OrganizationId(id)}.getOrNull()
    }
}

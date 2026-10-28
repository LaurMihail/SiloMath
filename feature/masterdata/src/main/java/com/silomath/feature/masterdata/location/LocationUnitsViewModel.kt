package com.silomath.feature.masterdata.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.domain.model.Location
import com.silomath.core.domain.model.StorageUnit
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class LocationUnitsState(val location:Location?=null,val units:List<StorageUnit> = emptyList(),
    val loading:Boolean=true,val error:String?=null,val admin:Boolean=false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationUnitsViewModel @Inject constructor(auth:AuthRepository,
    locations:LocationRepository,units:StorageUnitRepository):ViewModel() {
    private val target=MutableStateFlow<Pair<OrganizationId,LocationId>?>(null)
    fun bind(organizationId:OrganizationId,locationId:LocationId){target.value=organizationId to locationId}
    val uiState=combine(auth.sessionState,target){session,ids -> session to ids}.flatMapLatest {(session,ids) ->
        if(ids==null)flowOf(LocationUnitsState())
        else if(!session.isLoggedIn || session.activeOrganizationId!=ids.first.value ||
            session.authorizedOrganizations.none {it.organizationId==ids.first.value && it.role in setOf("ADMIN","STANDARD")})
            flowOf(LocationUnitsState(loading=false,error="Location is not available."))
        else locations.observeLocations(ids.first).flatMapLatest {rows ->
            val location=rows.firstOrNull{it.organizationId==ids.first && it.locationId==ids.second}
            if(location==null)flowOf(LocationUnitsState(loading=false,error="Location is not available locally."))
            else units.observeStorageUnits(ids.second).map {values -> LocationUnitsState(location,
                values.filter{it.locationId==ids.second}.sortedBy{it.name.lowercase(java.util.Locale.ROOT)},
                loading=false,admin=session.authorizedOrganizations.any{it.organizationId==ids.first.value && it.role=="ADMIN"}) }
        }.onStart{emit(LocationUnitsState())}.catch{emit(LocationUnitsState(loading=false,error="Storage Units could not be loaded."))}
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),LocationUnitsState())
}

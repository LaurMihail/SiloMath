package com.silomath.feature.masterdata.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.silomath.core.designsystem.*
import com.silomath.core.designsystem.components.*
import com.silomath.core.domain.model.Location
import com.silomath.core.model.*
import com.silomath.core.navigation.*
import com.silomath.feature.masterdata.location.map.*

@Composable
fun LocationsRoute(organizationId:OrganizationId,
    onNavigate:suspend (OrganizationDestination)->Boolean,
    viewModel:LocationsViewModel=hiltViewModel(),mapRuntime:FacilityMapRuntime=hiltViewModel<MapRuntimeViewModel>().runtime) {
    val data by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val navigate by rememberUpdatedState(onNavigate)
    LaunchedEffect(viewModel,lifecycle) {
        viewModel.navigationRequests.flowWithLifecycle(lifecycle).collect { target ->
            if(!navigate(target)) viewModel.navigationUnavailable(target is LocationEditor)
        }
    }
    val scoped=if(data.organizationId==organizationId) data else LocationUiState()
    LocationsScreen(scoped,viewModel::search,viewModel::filter,viewModel::view,
        {viewModel.open(it)}, {viewModel.edit(null)}, {viewModel.edit(it)}, viewModel::retry) { rows,selected,select ->
        NativeFacilityMap(rows,selected,select,mapRuntime)
    }
}

/** Map renderer is a presentation slot: it receives exactly the filtered positioned subset. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationsScreen(state:LocationUiState,onSearch:(String)->Unit,
    onFilter:(LifecycleFilter)->Unit,onView:(LocationView)->Unit,
    onOpen:(LocationId)->Unit,onAdd:()->Unit,onEdit:(LocationId)->Unit,onRetry:()->Unit,
    map:@Composable (List<Location>,LocationId?,(LocationId)->Unit)->Unit) {
    SiloMathAdaptiveContent(Modifier.fillMaxSize().imePadding()) { window ->
      BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollPage=maxHeight<500.dp || LocalDensity.current.fontScale>=1.7f
        val scrolling=if(scrollPage) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(Modifier.fillMaxSize().then(scrolling),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            SiloMathSearchField(state.query,onSearch,Modifier.fillMaxWidth())
            Text("Lifecycle Filter",style=MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                LifecycleFilter.entries.forEach {value -> SiloMathFilterChip(value.name,state.lifecycle==value,{onFilter(value)})}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                LocationView.entries.forEach {value -> SiloMathFilterChip(value.name,state.view==value,{onView(value)})}
            }
            state.navigationError?.let{SiloMathErrorState(it)}
            if(state.isAdmin) SiloMathPrimaryButton("Add Location",onAdd)
            when {
                state.loading -> CircularProgressIndicator()
                state.error!=null -> SiloMathErrorState(state.error,retry=onRetry)
                state.unfilteredCount==0 -> SiloMathEmptyState("No Locations available.")
                state.locations.isEmpty() -> SiloMathEmptyState("No Locations match these filters.")
                else -> {
                    val wide=window!=SiloMathWindowClass.Compact
                    val body=if(scrollPage) Modifier.height(400.dp) else Modifier.weight(1f)
                    if(wide && state.view==LocationView.Map) Row(body,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        LocationRows(state,onOpen,onEdit,Modifier.weight(1f))
                        Box(Modifier.weight(1f)) {LocationMapContent(state,onView,onOpen,map)}
                    } else if(state.view==LocationView.List) LocationRows(state,onOpen,onEdit,body)
                    else Box(body) {LocationMapContent(state,onView,onOpen,map)}
                }
            }
        }
      }
    }
}

@Composable
private fun LocationRows(state:LocationUiState,onOpen:(LocationId)->Unit,
    onEdit:(LocationId)->Unit,modifier:Modifier) {
    LazyColumn(modifier.fillMaxSize().testTag("location-results"),verticalArrangement=Arrangement.spacedBy(8.dp),
        contentPadding=PaddingValues(bottom=16.dp)) {
        items(state.locations,key={it.locationId.value}) {row ->
            SiloMathCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    SiloMathListRow(row.name,row.code,{onOpen(row.locationId)},Modifier.weight(1f))
                    if(state.isAdmin) IconButton({onEdit(row.locationId)},Modifier.siloMathTouchTarget()) {
                        Icon(Icons.Default.Edit,"Edit "+row.name)
                    }
                }
                SiloMathStatusChip(if(row.lifecycleStatus.name=="ACTIVE") "Active" else "Inactive",SiloMathTone.Neutral)
            }
        }
    }
}

@Composable
private fun LocationMapContent(state:LocationUiState,onView:(LocationView)->Unit,onOpen:(LocationId)->Unit,
    map:@Composable (List<Location>,LocationId?,(LocationId)->Unit)->Unit) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(state.positionedLocations.isEmpty()) SiloMathEmptyState("No Locations with stored map positions match these filters.")
        else Box(Modifier.weight(1f)) {map(state.positionedLocations,state.selectedId,onOpen)}
        if(state.positionedLocations.size<state.locations.size) Text("Locations without map positions remain available in List.",
            style=MaterialTheme.typography.bodySmall,color=SiloMathColors.textSecondary)
        SiloMathSecondaryButton("Show list",{onView(LocationView.List)})
    }
}

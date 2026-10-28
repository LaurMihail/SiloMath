package com.silomath.feature.masterdata.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silomath.core.designsystem.*
import com.silomath.core.designsystem.components.*
import com.silomath.core.domain.model.StorageUnit
import com.silomath.core.model.*
import com.silomath.core.navigation.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LocationUnitsRoute(organizationId:OrganizationId,locationId:LocationId,
    navigate:suspend(OrganizationDestination)->Boolean,viewModel:LocationUnitsViewModel=hiltViewModel()) {
    LaunchedEffect(organizationId,locationId){viewModel.bind(organizationId,locationId)}
    val state by viewModel.uiState.collectAsStateWithLifecycle();val scope=rememberCoroutineScope()
    var query by rememberSaveable{mutableStateOf("")};var lifecycle by rememberSaveable{mutableStateOf(LifecycleFilter.Active)}
    var message by rememberSaveable{mutableStateOf<String?>(null)}
    fun open(target:OrganizationDestination){scope.launch{if(!navigate(target))message="Storage Unit context is not available."}}
    LocationUnitsContent(state,query,{query=it},lifecycle,{lifecycle=it},
        {open(StorageUnitDetails(organizationId,locationId,it.storageUnitId))},
        {open(StorageUnitEditor(organizationId,locationId))},{open(StorageUnitEditor(organizationId,locationId,it.storageUnitId))},message)
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationUnitsContent(state:LocationUnitsState,query:String,onQuery:(String)->Unit,lifecycle:LifecycleFilter,onLifecycle:(LifecycleFilter)->Unit,
    onOpen:(StorageUnit)->Unit,onAdd:()->Unit,onEdit:(StorageUnit)->Unit,message:String?=null) {
    SiloMathAdaptiveContent(Modifier.fillMaxSize().imePadding()) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.semantics{contentDescription="Loading Storage Units"})
            state.error!=null -> SiloMathErrorState(state.error)
            else -> state.location?.let{location->
                val search=query.trim().lowercase(Locale.ROOT)
                val all=state.units.filter{it.locationId==location.locationId}
                val rows=all.filter{(lifecycle==LifecycleFilter.All || it.lifecycleStatus.name==lifecycle.name.uppercase(Locale.ROOT)) &&
                    (search.isEmpty() || it.name.lowercase(Locale.ROOT).contains(search) || it.code.lowercase(Locale.ROOT).contains(search))}
                LazyColumn(Modifier.fillMaxSize().testTag("storage-unit-list"),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=16.dp)) {
                    item{Text(location.name,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics{heading()})}
                    item{Text(location.code,style=MaterialTheme.typography.bodyMedium,color=SiloMathColors.textSecondary)}
                    item{Text("Local data",style=MaterialTheme.typography.bodySmall,color=SiloMathColors.textSecondary)}
                    item{SiloMathSearchField(query,onQuery,Modifier.fillMaxWidth(),"Search Storage Units")}
                    item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        LifecycleFilter.entries.forEach{filter->SiloMathFilterChip(filter.name,lifecycle==filter,{onLifecycle(filter)})}
                    }}
                    if(state.admin)item{SiloMathPrimaryButton("Add Storage Unit",onAdd)}
                    message?.let{item{SiloMathErrorState(it)}}
                    if(rows.isEmpty())item{SiloMathEmptyState(if(all.isEmpty())"No Storage Units in this Location." else "No Storage Units match these filters.")}
                    items(rows,key={it.storageUnitId.value}){unit->
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            SiloMathListRow(unit.name,"${unit.code} · ${if(unit.lifecycleStatus.name=="ACTIVE")"Active" else "Inactive"}",{onOpen(unit)})
                            if(state.admin)SiloMathSecondaryButton("Edit Storage Unit ${unit.code}",{onEdit(unit)})
                        }
                    }
                }
            }
        }
    }
}

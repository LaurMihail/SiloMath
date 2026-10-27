package com.silomath.feature.masterdata.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silomath.core.designsystem.*
import com.silomath.core.designsystem.components.*
import com.silomath.core.model.*
import com.silomath.core.navigation.*
import kotlinx.coroutines.launch
import java.util.Locale

/** Existing canonical LOC-02 destination; no separate Location Details screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationUnitsRoute(organizationId:OrganizationId,locationId:LocationId,
    navigate:suspend (OrganizationDestination)->Boolean,viewModel:LocationUnitsViewModel=hiltViewModel()) {
    LaunchedEffect(organizationId,locationId){viewModel.bind(organizationId,locationId)}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    var query by rememberSaveable{mutableStateOf("")}
    var lifecycle by rememberSaveable{mutableStateOf(LifecycleFilter.Active)}
    var message by rememberSaveable{mutableStateOf<String?>(null)}
    SiloMathAdaptiveContent(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            when {
                state.loading -> CircularProgressIndicator()
                state.error!=null -> SiloMathErrorState(state.error!!)
                else -> state.location?.let {location ->
                    Text(location.name,style=MaterialTheme.typography.headlineSmall)
                    Text(location.code,style=MaterialTheme.typography.bodyMedium,color=SiloMathColors.textSecondary)
                    SiloMathSearchField(query,{query=it},Modifier.fillMaxWidth(),"Search Storage Units")
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        LifecycleFilter.entries.forEach{value->SiloMathFilterChip(value.name,lifecycle==value,{lifecycle=value})}
                    }
                    // STU-02 and P1-SH-02 remain owned by the audited Storage Unit UI task.
                    if(state.admin)SiloMathSecondaryButton("Add Storage Unit",{message="Storage Unit editing is currently unavailable."})
                    message?.let{SiloMathErrorState(it)}
                    val search=query.trim().lowercase(Locale.ROOT)
                    val rows=state.units.filter{unit ->
                        (lifecycle==LifecycleFilter.All || unit.lifecycleStatus.name==lifecycle.name.uppercase(Locale.ROOT)) &&
                            (search.isEmpty() || unit.name.lowercase(Locale.ROOT).contains(search) || unit.code.lowercase(Locale.ROOT).contains(search))
                    }
                    if(rows.isEmpty())SiloMathEmptyState("No Storage Units available locally for these filters.")
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        items(rows,key={it.storageUnitId.value}){unit ->
                            SiloMathListRow(unit.name,unit.code,{scope.launch {
                                if(!navigate(StorageUnitDetails(organizationId,locationId,unit.storageUnitId)))
                                    message="Storage Unit details are currently unavailable."
                            }})
                        }
                    }
                }
            }
        }
    }
}

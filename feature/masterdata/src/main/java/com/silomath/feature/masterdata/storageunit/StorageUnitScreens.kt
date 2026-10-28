package com.silomath.feature.masterdata.storageunit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.silomath.core.designsystem.*
import com.silomath.core.designsystem.components.*
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.model.StorageSpace
import com.silomath.core.model.*
import com.silomath.core.navigation.*
import kotlinx.coroutines.launch

@Composable
fun StorageUnitDetailsRoute(org:OrganizationId,location:LocationId,id:StorageUnitId,navigate:suspend(OrganizationDestination)->Boolean,
    viewModel:StorageUnitDetailsViewModel=hiltViewModel()) {
    LaunchedEffect(org,location,id){viewModel.bind(org,location,id)}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope();var navigationError by remember{mutableStateOf<String?>(null)}
    StorageUnitDetailsContent(state,onEdit={scope.launch{if(!navigate(StorageUnitEditor(org,location,id)))navigationError="Storage Unit editing is not available."}},
        onSpace={space->scope.launch{if(!navigate(StorageSpaceDetails(org,location,id,space)))navigationError="Storage Space details are not available."}},navigationError=navigationError)
}

@Composable
fun StorageUnitDetailsContent(state:StorageUnitDetailsState,onEdit:()->Unit,onSpace:(StorageSpaceId)->Unit,navigationError:String?=null) {
    SiloMathAdaptiveContent(Modifier.fillMaxSize().imePadding()) {window->
        when {
            state.loading -> CircularProgressIndicator(Modifier.semantics{contentDescription="Loading Storage Unit"})
            state.error!=null -> SiloMathErrorState(state.error)
            state.unit!=null && state.location!=null -> {
                val u=state.unit;val location=state.location
                val details:LazyListScope.()->Unit={
                    item{Text(u.name,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics{heading()})}
                    item{Text(u.code,style=MaterialTheme.typography.titleMedium)}
                    item{Text("Location: ${location.name} (${location.code})",style=MaterialTheme.typography.bodyMedium)}
                    u.description?.let{item{Text(it,style=MaterialTheme.typography.bodyMedium)}}
                    item{SiloMathStatusChip(if(u.lifecycleStatus==MasterStatus.ACTIVE)"Active" else "Inactive",SiloMathTone.Neutral)}
                    item{Text("Local data",style=MaterialTheme.typography.bodySmall,color=SiloMathColors.textSecondary)}
                    if(state.admin)item{SiloMathSecondaryButton("Edit Storage Unit",onEdit)}
                    navigationError?.let{item{SiloMathErrorState(it)}}
                }
                val children:LazyListScope.()->Unit={
                    item{Text("Storage Spaces",style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics{heading()})}
                    // P1-SH-03 is owned by MD-006; no enabled action can pretend that editor exists.
                    if(state.admin)item{SiloMathSecondaryButton("Add Storage Space",{},enabled=false)}
                    when {
                        state.spacesError!=null -> item{SiloMathErrorState(state.spacesError)}
                        state.spaces.isEmpty() -> item{SiloMathEmptyState("No Storage Spaces in this Storage Unit.")}
                        else -> items(state.spaces,key={it.storageSpaceId.value}){space->
                            if(space.storageUnitId==u.storageUnitId)SpaceRow(space){onSpace(space.storageSpaceId)}
                        }
                    }
                }
                if(window==SiloMathWindowClass.Expanded)Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                    LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag("unit-details-list"),verticalArrangement=Arrangement.spacedBy(16.dp),content=details)
                    LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag("unit-spaces-list"),verticalArrangement=Arrangement.spacedBy(16.dp),content=children)
                } else LazyColumn(Modifier.fillMaxSize().testTag("unit-details-list"),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=16.dp)) {details();children()}
            }
        }
    }
}
@Composable private fun SpaceRow(space:StorageSpace,onClick:()->Unit)=SiloMathListRow(space.code,
    listOfNotNull(space.description,if(space.lifecycleStatus==MasterStatus.ACTIVE)"Active" else "Inactive").joinToString(" · "),onClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageUnitEditorRoute(org:OrganizationId,location:LocationId,id:StorageUnitId?,onDismiss:()->Unit,viewModel:StorageUnitEditorViewModel=hiltViewModel()) {
    LaunchedEffect(org,location,id){viewModel.bind(org,location,id)}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle;val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(viewModel,lifecycle){viewModel.savedEvents.flowWithLifecycle(lifecycle).collect{dismiss()}}
    ModalBottomSheet(onDismissRequest={if(!state.saving)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=SiloMathColors.surface) {
        StorageUnitEditorContent(state,id!=null,viewModel::update,{viewModel.save()},onDismiss)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageUnitEditorContent(state:StorageUnitEditorState,editing:Boolean,onChange:(StorageUnitDraft)->Unit,onSave:()->Unit,onCancel:()->Unit) {
    val draft=state.draft;val enabled=state.allowed && !state.loading && !state.saving
    SiloMathAdaptiveContent(Modifier.fillMaxWidth().imePadding()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(if(editing)"Edit Storage Unit" else "Add Storage Unit",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics{heading()})
            if(state.loading)CircularProgressIndicator(Modifier.semantics{contentDescription="Loading Storage Unit editor"})
            state.error?.let{SiloMathErrorState(it)}
            if(state.allowed) {
                state.location?.let{Text("Location: ${it.name} (${it.code})",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.semantics{stateDescription="Read only"})}
                SiloMathTextField(draft.name,{if(enabled)onChange(draft.copy(name=it))},"Storage Unit Name",Modifier.fillMaxWidth(),readOnly=!enabled)
                SiloMathTextField(draft.code,{if(enabled && !state.codeLocked)onChange(draft.copy(code=it))},"Storage Unit Code",Modifier.fillMaxWidth(),readOnly=!enabled || state.codeLocked)
                if(state.codeLocked)Text("Storage Unit Code is protected by historical references.",style=MaterialTheme.typography.bodySmall)
                SiloMathTextField(draft.description,{if(enabled)onChange(draft.copy(description=it))},"Description",Modifier.fillMaxWidth(),readOnly=!enabled,singleLine=false)
                Text("Status",style=MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    MasterStatus.entries.forEach{status->FilterChip(selected=draft.status==status,onClick={onChange(draft.copy(status=status))},enabled=enabled,
                        label={Text(if(status==MasterStatus.ACTIVE)"Active" else "Inactive")},modifier=Modifier.siloMathTouchTarget())}
                }
                SiloMathPrimaryButton(if(state.saving)"Saving…" else "Save",onSave,enabled=enabled)
            }
            SiloMathSecondaryButton("Cancel",onCancel,enabled=!state.saving)
        }
    }
}

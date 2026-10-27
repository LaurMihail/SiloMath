package com.silomath.feature.masterdata.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.silomath.core.designsystem.*
import com.silomath.core.designsystem.components.*
import com.silomath.core.domain.model.*
import com.silomath.core.model.*
import com.silomath.feature.masterdata.location.map.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditorRoute(org:OrganizationId,id:LocationId?,onDismiss:()->Unit,
    viewModel:LocationEditorViewModel=hiltViewModel(),runtime:FacilityMapRuntime=hiltViewModel<MapRuntimeViewModel>().runtime) {
    LaunchedEffect(org,id){viewModel.bind(org,id)}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(viewModel,lifecycle){viewModel.savedEvents.flowWithLifecycle(lifecycle).collect{dismiss()}}
    ModalBottomSheet(onDismissRequest={if(!state.saving)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=SiloMathColors.surface) {
        LocationEditorContent(state,id!=null,viewModel::update,{viewModel.save()},onDismiss) {lat,lon,pick ->
            NativeFacilityMap(emptyList(),null,{},runtime,
                if(lat!=null && lon!=null)lat to lon else null,pick)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationEditorContent(state:LocationEditorState,editing:Boolean,onChange:(LocationDraft)->Unit,onSave:()->Unit,
    onCancel:()->Unit,map:@Composable (java.math.BigDecimal?,java.math.BigDecimal?,(java.math.BigDecimal,java.math.BigDecimal)->Unit)->Unit) {
    var choosingCountry by remember{mutableStateOf(false)}
    val draft=state.draft
    val enabled=state.allowed && !state.saving && !state.loading
    SiloMathAdaptiveContent(Modifier.fillMaxWidth().imePadding()) {
      Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(if(editing)"Edit Location" else "Add Location",style=MaterialTheme.typography.headlineSmall)
        if(state.loading)CircularProgressIndicator()
        state.error?.let{SiloMathErrorState(it)}
        if(state.allowed) {
            EditorField("Location Name",draft.name,{onChange(draft.copy(name=it))},enabled,state.errors["name"])
            EditorField("Location Code",draft.code,{onChange(draft.copy(code=it))},enabled && !state.codeLocked,state.errors["code"])
            if(state.codeLocked)Text("Location Code is protected by historical references.",style=MaterialTheme.typography.bodySmall)
            EditorField("Address",draft.address,{onChange(draft.copy(address=it))},enabled,state.errors["address"])
            EditorField("City",draft.city,{onChange(draft.copy(city=it))},enabled,state.errors["city"])
            Text("Country",style=MaterialTheme.typography.labelMedium)
            SiloMathSecondaryButton(CountryCatalog.label(draft.country) ?: if(draft.country==null)"Select Country" else "Historical Country: ${draft.country}",
                {choosingCountry=true},enabled=enabled)
            if(draft.country!=null && CountryCatalog.label(draft.country)==null)Text(
                "Historical Country value is preserved unchanged. A separate audit is required before migration.",style=MaterialTheme.typography.bodySmall)
            if(draft.country!=null)SiloMathSecondaryButton("Clear Country",{onChange(draft.copy(country=null))},enabled=enabled)
            state.errors["country"]?.let{Text(it,color=SiloMathColors.error)}
            EditorField("Notes",draft.notes,{onChange(draft.copy(notes=it))},enabled,state.errors["notes"],multiline=true)
            Text("Status",style=MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                MasterStatus.entries.forEach {status -> FilterChip(selected=draft.status==status,
                    onClick={onChange(draft.copy(status=status))},enabled=enabled,label={Text(if(status==MasterStatus.ACTIVE)"Active" else "Inactive")},
                    modifier=Modifier.siloMathTouchTarget())}
            }
            Text("Map Position",style=MaterialTheme.typography.titleMedium)
            EditorField("Latitude",draft.latitude,{onChange(draft.copy(latitude=it))},enabled,state.errors["latitude"],decimal=true)
            EditorField("Longitude",draft.longitude,{onChange(draft.copy(longitude=it))},enabled,state.errors["longitude"],decimal=true)
            if(draft.latitude.isNotEmpty() || draft.longitude.isNotEmpty())SiloMathSecondaryButton("Clear Map Position",
                {onChange(draft.copy(latitude="",longitude=""))},enabled=enabled)
            if(enabled)Box(Modifier.fillMaxWidth().height(280.dp)) {
                val lat=draft.latitude.toBigDecimalOrNull()?.takeIf{it>=(-90).toBigDecimal() && it<=90.toBigDecimal()}
                val lon=draft.longitude.toBigDecimalOrNull()?.takeIf{it>=(-180).toBigDecimal() && it<=180.toBigDecimal()}
                map(lat,lon) {a,b->onChange(draft.copy(latitude=a.toPlainString(),longitude=b.toPlainString()))}
            }
            Text("Enter coordinates or select a position on the map. No device location is used.",style=MaterialTheme.typography.bodySmall)
            SiloMathPrimaryButton(if(state.saving)"Saving…" else "Save",onSave,enabled=enabled)
        }
        SiloMathSecondaryButton("Cancel",onCancel,enabled=!state.saving)
      }
    }
    if(choosingCountry)CountryPicker(onSelect={choosingCountry=false;onChange(draft.copy(country=it))},onDismiss={choosingCountry=false})
}

@Composable
private fun EditorField(label:String,value:String,onChange:(String)->Unit,enabled:Boolean,error:String?,
    multiline:Boolean=false,decimal:Boolean=false) {
    OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),enabled=enabled,label={Text(label)},singleLine=!multiline,
        minLines=if(multiline)3 else 1,isError=error!=null,supportingText=error?.let{{Text(it)}},
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal)KeyboardType.Decimal else KeyboardType.Text))
}

@Composable
private fun CountryPicker(onSelect:(String)->Unit,onDismiss:()->Unit) {
    var query by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Country")},confirmButton={TextButton(onDismiss){Text("Cancel")}},text={
        Column(Modifier.fillMaxWidth()) {
            // This searches options; it never becomes a persisted Country value.
            SiloMathSearchField(query,{query=it},Modifier.fillMaxWidth(),"Search Countries")
            val rows=CountryCatalog.options.filter{it.label.contains(query.trim(),true) || it.code.equals(query.trim(),true)}
            LazyColumn(Modifier.heightIn(max=360.dp)) {items(rows,key={it.code}) {country ->
                SiloMathListRow(country.label,country.code,{onSelect(country.code)})
            }}
        }
    })
}

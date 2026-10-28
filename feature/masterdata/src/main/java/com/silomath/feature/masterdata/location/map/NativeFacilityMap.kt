package com.silomath.feature.masterdata.location.map

import java.math.BigDecimal
import android.content.ComponentCallbacks
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.silomath.core.designsystem.SiloMathColors
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.silomath.core.designsystem.components.*
import com.silomath.core.domain.model.Location
import com.silomath.core.model.LocationId
import org.maplibre.android.maps.*
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.geojson.*
import kotlinx.serialization.json.*

/** No location component, sensors, user position, remote styles/glyphs or geocoding. */
@Composable
fun NativeFacilityMap(rows:List<Location>,selectedId:LocationId?,onSelect:(LocationId)->Unit,runtime:FacilityMapRuntime,
    draftPosition:Pair<BigDecimal,BigDecimal>?=null,onPositionSelected:((BigDecimal,BigDecimal)->Unit)?=null) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val currentRows by rememberUpdatedState(rows)
    val select by rememberUpdatedState(onSelect)
    val pickPosition by rememberUpdatedState(onPositionSelected)
    var map by remember {mutableStateOf<MapLibreMap?>(null)}
    var ready by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    var fullyRendered by remember {mutableStateOf(false)}
    var fitted by remember {mutableStateOf<List<String>>(emptyList())}
    val view=remember(context,runtime) {
      try {
        runtime.initialize()
        MapView(context).also {v ->
            v.onCreate(null)
            v.addOnDidFailLoadingMapListener {failed=true}
            v.addOnDidFinishRenderingMapListener {full -> if(full)fullyRendered=true}
            v.getMapAsync {native ->
                map=native
                native.uiSettings.isCompassEnabled=false
                native.setStyle(Style.Builder().fromJson(styleJson(runtime.config))) {style ->
                    style.addSource(GeoJsonSource("locations",FeatureCollection.fromFeatures(emptyArray<Feature>())))
                    style.addLayer(CircleLayer("locations","locations").withProperties(
                        circleRadius(8f),circleColor(SiloMathColors.primary.toArgb()),circleStrokeColor(SiloMathColors.surface.toArgb()),circleStrokeWidth(2f)))
                    style.addLayer(CircleLayer("selected-location","locations").withFilter(eq(get("selected"),literal(true)))
                        .withProperties(circleRadius(12f),circleColor(SiloMathColors.primary.toArgb()),circleStrokeColor(SiloMathColors.surface.toArgb()),circleStrokeWidth(4f)))
                    ready=true
                }
                native.addOnMapClickListener {point ->
                    val pick=pickPosition
                    if(pick!=null) {
                        pick(BigDecimal.valueOf(point.latitude),BigDecimal.valueOf(point.longitude));return@addOnMapClickListener true
                    }
                    val screen=native.projection.toScreenLocation(point)
                    val radius=24f*context.resources.displayMetrics.density
                    val hitBox=android.graphics.RectF(screen.x-radius,screen.y-radius,screen.x+radius,screen.y+radius)
                    val feature=native.queryRenderedFeatures(hitBox,"locations").firstOrNull()
                    val id=feature?.getStringProperty("locationId")
                    val row=currentRows.firstOrNull{it.locationId.value==id}
                    if(row!=null){select(row.locationId);true}else false
                }
            }
        }
      } catch (_:RuntimeException) {null} catch (_:LinkageError) {null}
    }
    if(view==null) {SiloMathOfflineBanner("Map unavailable. Use List to access Locations.");return}
    DisposableEffect(view,owner,runtime) {
        var started=false;var resumed=false
        val observer=LifecycleEventObserver {_,event -> when(event) {
            Lifecycle.Event.ON_START -> if(!started){view.onStart();started=true}
            Lifecycle.Event.ON_RESUME -> if(!resumed){view.onResume();resumed=true}
            Lifecycle.Event.ON_PAUSE -> if(resumed){view.onPause();resumed=false}
            Lifecycle.Event.ON_STOP -> if(started){view.onStop();started=false}
            else -> Unit
        }}
        val memory=object:ComponentCallbacks {
            override fun onConfigurationChanged(newConfig:Configuration) {}
            override fun onLowMemory(){view.onLowMemory()}
        }
        val removeFailure=runtime.listen{view.post{failed=true}}
        context.registerComponentCallbacks(memory);owner.lifecycle.addObserver(observer)
        onDispose {
            removeFailure();context.unregisterComponentCallbacks(memory);owner.lifecycle.removeObserver(observer)
            if(resumed)view.onPause();if(started)view.onStop();view.onDestroy()
        }
    }
    LaunchedEffect(rows,selectedId,ready,draftPosition) {
        val native=map ?: return@LaunchedEffect
        if(!ready)return@LaunchedEffect
        val points=rows.mapNotNull {row ->
            val lat=row.latitude ?: return@mapNotNull null;val lon=row.longitude ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(lon.toDouble(),lat.toDouble())).also {
                it.addStringProperty("locationId",row.locationId.value)
                it.addBooleanProperty("selected",row.locationId==selectedId)
            }
        }
        val editing=draftPosition?.let {(lat,lon)->Feature.fromGeometry(Point.fromLngLat(lon.toDouble(),lat.toDouble())).also {
            it.addBooleanProperty("selected",true)
        }}
        native.getStyle {it.getSourceAs<GeoJsonSource>("locations")?.setGeoJson(FeatureCollection.fromFeatures(points+listOfNotNull(editing)))}
        val fingerprint=rows.map {"${it.locationId.value}:${it.latitude}:${it.longitude}"}+listOfNotNull(draftPosition?.toString())
        if(fitted!=fingerprint && (rows.isNotEmpty() || draftPosition!=null)) {
            val coordinates=rows.mapNotNull {row -> row.latitude?.let {lat ->row.longitude?.let {lon->LatLng(lat.toDouble(),lon.toDouble())}}}+listOfNotNull(draftPosition?.let{LatLng(it.first.toDouble(),it.second.toDouble())})
            if(coordinates.size==1)native.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates.first(),12.0))
            else if(coordinates.isNotEmpty())native.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(coordinates).build(),48))
            fitted=fingerprint
        }
    }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(!runtime.config.enabled || failed || !fullyRendered) SiloMathOfflineBanner("Basemap unavailable or incomplete. List remains available.")
        AndroidView(factory={view},modifier=Modifier.fillMaxWidth().weight(1f).semantics {contentDescription=if(ready)"Location map" else "Location map loading"})
        if(runtime.config.enabled)Text(runtime.config.attributionText)
        // Equivalent labeled selected marker actions, even where native accessibility is limited.
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(rows,key={it.locationId.value}) {row ->
                SiloMathLocationMarker(row.name,row.locationId==selectedId,{onSelect(row.locationId)})
            }
        }
    }
}

private fun styleJson(config:MapProviderConfig):String = buildJsonObject {
    put("version",8)
    put("sources",buildJsonObject {if(config.enabled) put("basemap",buildJsonObject {
        put("type","raster");put("tiles",JsonArray(listOf(JsonPrimitive(config.tileTemplate))))
        put("tileSize",256)
        put("attribution","<a href=\"${config.attributionUrl}\">${config.attributionText}</a>")
    })})
    put("layers",buildJsonArray {
        add(buildJsonObject {put("id","background");put("type","background");put("paint",buildJsonObject{put("background-color","#%06X".format(java.util.Locale.ROOT,SiloMathColors.surfaceSubtle.toArgb() and 0xFFFFFF))})})
        if(config.enabled)add(buildJsonObject {put("id","basemap");put("type","raster");put("source","basemap")
            put("paint",buildJsonObject {put("raster-saturation",-1);put("raster-opacity",0.8)})})
    })
}.toString()

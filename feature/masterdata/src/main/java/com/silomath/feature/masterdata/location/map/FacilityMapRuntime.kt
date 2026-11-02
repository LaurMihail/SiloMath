package com.silomath.feature.masterdata.location.map

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.maplibre.android.MapLibre
import org.maplibre.android.log.Logger
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton

class MapPrivacyInterceptor(private val config:MapProviderConfig,
    private val onFailure:()->Unit={}):Interceptor {
    override fun intercept(chain:Interceptor.Chain):Response {
        val original=chain.request()
        if(!config.permits(original.url)) {onFailure();throw IOException("Map request is unavailable")}
        val clean=Request.Builder().url(original.url).get().header("User-Agent","SiloMath (Android facility map)")
            .header("Accept","image/png,image/jpeg,image/webp")
        listOf("If-None-Match","If-Modified-Since","Range").forEach {name -> original.header(name)?.let{clean.header(name,it)}}
        return try {chain.proceed(clean.build()).also{if(!it.isSuccessful && it.code!=304)onFailure()}}
            catch(e:IOException){onFailure();throw e}
    }
}

@Singleton
class FacilityMapRuntime @Inject constructor(@ApplicationContext private val context:Context,val config:MapProviderConfig) {
    private val failures=CopyOnWriteArraySet<()->Unit>()
    private var initialized=false
    private val client by lazy {
        OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .dispatcher(Dispatcher().apply{maxRequests=8;maxRequestsPerHost=4})
            .cache(Cache(File(context.cacheDir,"facility-map-tiles"),40L*1024*1024))
            .addInterceptor(MapPrivacyInterceptor(config){failures.forEach{it()}}).build()
    }
    @Synchronized fun initialize() {
        if(initialized)return
        MapLibre.getInstance(context)
        Logger.setVerbosity(Logger.NONE)
        HttpRequestUtil.setLogEnabled(false)
        HttpRequestUtil.setPrintRequestUrlOnFailure(false)
        HttpRequestUtil.setOkHttpClient(client)
        initialized=true
    }
    fun listen(listener:()->Unit):()->Unit {failures+=listener;return{failures-=listener}}
}

@HiltViewModel
class MapRuntimeViewModel @Inject constructor(val runtime:FacilityMapRuntime):ViewModel()

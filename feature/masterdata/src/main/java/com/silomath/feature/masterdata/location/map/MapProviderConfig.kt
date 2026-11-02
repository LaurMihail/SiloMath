package com.silomath.feature.masterdata.location.map

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Controlled deployment config, never tenant/session configuration. */
data class MapProviderConfig(val tileTemplate:String="",val attributionText:String="",val attributionUrl:String="") {
    val sampleUrl=tileTemplate.replace("{z}","0").replace("{x}","0").replace("{y}","0").toHttpUrlOrNull()
    private val marked=tileTemplate.replace("{z}","__SM_Z__").replace("{x}","__SM_X__").replace("{y}","__SM_Y__").toHttpUrlOrNull()
    private val markers=Regex("__SM_[ZXY]__")
    private val pathPattern=marked?.encodedPath?.split(markers)?.joinToString("[0-9]+") {Regex.escape(it)}?.toRegex()
    val enabled:Boolean get()=sampleUrl?.isHttps==true && sampleUrl.username.isEmpty() && sampleUrl.password.isEmpty() &&
        listOf("__SM_Z__","__SM_X__","__SM_Y__").all{marked?.encodedPath?.contains(it)==true} && attributionText.isNotBlank() &&
        attributionUrl.toHttpUrlOrNull()?.isHttps==true
    fun permits(url:okhttp3.HttpUrl):Boolean {
        val sample=sampleUrl ?: return false
        if(!enabled || !url.isHttps || url.host!=sample.host || url.port!=sample.port ||
            url.username.isNotEmpty() || url.password.isNotEmpty() || url.encodedQuery!=sample.encodedQuery) return false
        // Match a safe URL expanded only with numeric tile addresses, never business payloads.
        return pathPattern?.matches(url.encodedPath)==true
    }
}

package com.silomath.feature.masterdata.location

import com.silomath.core.domain.model.*
import java.math.BigDecimal
import java.util.Locale

data class LocationDraft(val name:String="",val code:String="",val address:String="",val city:String="",
    val country:String?=null,val notes:String="",val status:MasterStatus=MasterStatus.ACTIVE,
    val latitude:String="",val longitude:String="") {
    fun errors(original:Location?,rows:List<Location>,codeLocked:Boolean):Map<String,String> = buildMap {
        if(name.trim().isEmpty() || name.trim().length>150)put("name","Enter a Location Name of 1–150 characters.")
        val normalized=code.trim().uppercase(Locale.ROOT)
        if(normalized.isEmpty() || normalized.length>50)put("code","Enter a Location Code of 1–50 characters.")
        else if(rows.any{it.locationId!=original?.locationId && it.code.equals(normalized,true)})put("code","Location Code already exists in this Organization.")
        if(codeLocked && normalized!=original?.code)put("code","Historically referenced Location Code cannot be changed.")
        if(address.length>250)put("address","Maximum 250 characters.")
        if(city.length>100)put("city","Maximum 100 characters.")
        if(notes.length>500)put("notes","Maximum 500 characters.")
        if(country!=original?.country && !CountryCatalog.isAllowedNewValue(country))put("country","Select a Country from the ISO 3166-1 catalogue.")
        val lat=latitude.trim();val lon=longitude.trim()
        if(lat.isNotEmpty() || lon.isNotEmpty()) {
            if(lat.toBigDecimalOrNull()?.let{it>=BigDecimal("-90") && it<=BigDecimal("90")}!=true)put("latitude","Enter latitude between -90 and 90; both coordinates are required.")
            if(lon.toBigDecimalOrNull()?.let{it>=BigDecimal("-180") && it<=BigDecimal("180")}!=true)put("longitude","Enter longitude between -180 and 180; both coordinates are required.")
        }
    }
    companion object {
        fun from(row:Location)=LocationDraft(row.name,row.code,row.address.orEmpty(),row.city.orEmpty(),row.country,
            row.notes.orEmpty(),row.lifecycleStatus,row.latitude?.toPlainString().orEmpty(),row.longitude?.toPlainString().orEmpty())
    }
}

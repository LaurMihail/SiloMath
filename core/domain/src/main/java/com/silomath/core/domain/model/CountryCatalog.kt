package com.silomath.core.domain.model

/** FD-74. Packaged complete catalogue; membership never depends on device locale/network. */
data class CountryOption(val code:String,val label:String)
object CountryCatalog {
    val options:List<CountryOption> by lazy {
        val stream=checkNotNull(CountryCatalog::class.java.getResourceAsStream("/country/iso3166-1.tsv"))
        val rows=stream.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.filter{it.isNotBlank()}.map {line ->
            val fields=line.split('\t');require(fields.size==2 && fields[0].matches(Regex("[A-Z]{2}")) && fields[1].isNotBlank())
            CountryOption(fields[0],fields[1])
        }.toList() }
        require(rows.isNotEmpty() && rows.map{it.code}.distinct().size==rows.size)
        rows.sortedBy{it.label}
    }
    fun contains(code:String)=options.any{it.code==code}
    fun label(code:String?)=options.firstOrNull{it.code==code}?.label
    fun isAllowedNewValue(value:String?)=value==null || contains(value)
}

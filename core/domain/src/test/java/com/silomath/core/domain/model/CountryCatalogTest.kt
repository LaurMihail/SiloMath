package com.silomath.core.domain.model

import org.junit.Assert.*
import org.junit.Test

class CountryCatalogTest {
    @Test fun packagedCatalogueHasCompleteAssignedCodesWithoutReservedOrUserAssignedValues() {
        val codes=CountryCatalog.options.map{it.code}.toSet()
        assertEquals(249,codes.size) // Observed source snapshot evidence, not a product catalogueVersion.
        assertEquals(java.util.Locale.getISOCountries().toSet(),codes)
        assertEquals("Romania",CountryCatalog.label("RO"));assertEquals("Germany",CountryCatalog.label("DE"))
        assertEquals("United States",CountryCatalog.label("US"))
        listOf("ZZ","XK","UK","ro","Romania","USA","", " RO ").forEach{assertFalse(it,CountryCatalog.contains(it))}
        assertTrue(CountryCatalog.isAllowedNewValue(null))
        assertTrue(CountryCatalog.options.all{it.label.isNotBlank() && it.code.matches(Regex("[A-Z]{2}"))})
    }
}

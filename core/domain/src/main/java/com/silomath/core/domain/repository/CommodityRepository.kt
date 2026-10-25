package com.silomath.core.domain.repository

import com.silomath.core.domain.model.Commodity
import com.silomath.core.domain.model.CommodityCatalogEntry
import com.silomath.core.domain.model.CommodityVersion
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import kotlinx.coroutines.flow.Flow

/**
 * Read-only organization-user access to the platform-governed Commodity catalog.
 *
 * V6 deliberately exposes no customer mutation operations here. Commodity creation,
 * lifecycle changes and version publication belong to platform governance, not to an
 * organization Admin or Standard user.
 */
interface CommodityRepository {
    fun observeCatalog(): Flow<List<CommodityCatalogEntry>>

    suspend fun getCommodity(
        commodityId: CommodityId
    ): Commodity?

    suspend fun getCommodityVersion(
        commodityVersionId: CommodityVersionId
    ): CommodityVersion?
}

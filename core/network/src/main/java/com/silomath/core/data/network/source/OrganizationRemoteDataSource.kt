package com.silomath.core.data.network.source

import com.silomath.core.model.organization.OrganizationTransferRecord

interface OrganizationRemoteDataSource {

    suspend fun getOrganizations():
        Result<List<OrganizationTransferRecord>>
}

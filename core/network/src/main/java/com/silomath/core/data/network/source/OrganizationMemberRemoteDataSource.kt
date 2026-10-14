package com.silomath.core.data.network.source

import com.silomath.core.data.network.dto.OrganizationMemberRemote

interface OrganizationMemberRemoteDataSource {
    suspend fun getOrganizationMembers(
        organizationRemoteId: String
    ): List<OrganizationMemberRemote>
}

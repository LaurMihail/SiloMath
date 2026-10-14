package com.silomath.core.data.network.source

import com.silomath.core.model.organization.UserMembershipTransferRecord

interface OrganizationMembershipRemoteDataSource {

    suspend fun getOrganizationMemberships(
        organizationId: String
    ): Result<List<UserMembershipTransferRecord>>
}
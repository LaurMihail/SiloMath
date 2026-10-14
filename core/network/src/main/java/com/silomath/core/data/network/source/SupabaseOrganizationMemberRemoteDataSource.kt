package com.silomath.core.data.network.source

import com.silomath.core.data.network.dto.OrganizationMemberRemote
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class SupabaseOrganizationMemberRemoteDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) : OrganizationMemberRemoteDataSource {

    override suspend fun getOrganizationMembers(
        organizationRemoteId: String
    ): List<OrganizationMemberRemote> {
        return supabaseClient.postgrest.from("organization_members")
            .select {
                filter {
                    eq("organization_id", organizationRemoteId)
                }
            }
            .decodeList<OrganizationMemberRemote>()
    }
}

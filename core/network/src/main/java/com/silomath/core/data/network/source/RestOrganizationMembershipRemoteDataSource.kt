package com.silomath.core.data.network.source

import com.silomath.core.data.network.api.OrganizationMembershipApi
import com.silomath.core.data.network.auth.AuthenticatedApiCallExecutor
import com.silomath.core.data.network.dto.UserMembershipResponseDto
import com.silomath.core.model.organization.UserMembershipTransferRecord
import javax.inject.Inject

class RestOrganizationMembershipRemoteDataSource @Inject constructor(
    private val api: OrganizationMembershipApi,
    private val authenticatedApiCallExecutor:
        AuthenticatedApiCallExecutor
) : OrganizationMembershipRemoteDataSource {

    override suspend fun getOrganizationMemberships(
        organizationId: String
    ): Result<List<UserMembershipTransferRecord>> {
        return try {
            val result =
                mutableListOf<UserMembershipTransferRecord>()

            var cursor: String? = null
            var hasMore: Boolean

            do {
                val page =
                    authenticatedApiCallExecutor.execute {
                        api.getOrganizationUsers(
                            organizationId =
                                organizationId,
                            cursor =
                                cursor
                        )
                    }

                result +=
                    page.items.map {
                        it.toTransferRecord()
                    }

                hasMore =
                    page.hasMore

                cursor =
                    if (hasMore) {
                        page.nextCursor
                            ?: throw IllegalStateException(
                                "User membership response has " +
                                    "hasMore=true but nextCursor is null."
                            )
                    } else {
                        null
                    }
            } while (hasMore)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun UserMembershipResponseDto.toTransferRecord():
        UserMembershipTransferRecord {
        return UserMembershipTransferRecord(
            membershipId =
                membershipId,
            organizationId =
                organizationId,
            userId =
                userId,
            fullName =
                fullName,
            email =
                email,
            userLifecycleStatus =
                userLifecycleStatus,
            role =
                role,
            membershipLifecycleStatus =
                membershipLifecycleStatus,
            disabledAt =
                disabledAt,
            disabledBy =
                disabledBy,
            createdAt =
                createdAt,
            createdBy =
                createdBy,
            updatedAt =
                updatedAt,
            updatedBy =
                updatedBy,
            entityVersion =
                entityVersion
        )
    }
}
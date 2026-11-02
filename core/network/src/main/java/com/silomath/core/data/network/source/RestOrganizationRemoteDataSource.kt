package com.silomath.core.data.network.source

import com.silomath.core.data.network.api.OrganizationApi
import com.silomath.core.data.network.auth.AuthenticatedApiCallExecutor
import com.silomath.core.data.network.dto.OrganizationResponseDto
import com.silomath.core.model.organization.OrganizationTransferRecord
import javax.inject.Inject

class RestOrganizationRemoteDataSource @Inject constructor(
    private val organizationApi: OrganizationApi,
    private val authenticatedApiCallExecutor:
        AuthenticatedApiCallExecutor
) : OrganizationRemoteDataSource {

    override suspend fun getOrganizations():
        Result<List<OrganizationTransferRecord>> {
        return try {
            val organizationIds =
                linkedSetOf<String>()

            var cursor: String? = null
            var hasMore: Boolean

            do {
                val page =
                    authenticatedApiCallExecutor.execute {
                        organizationApi.getOrganizations(
                            cursor = cursor
                        )
                    }

                for (item in page.items) {
                    organizationIds += item.id
                }

                hasMore =
                    page.hasMore

                cursor =
                    nextCursorOrThrow(
                        hasMore =
                            page.hasMore,
                        nextCursor =
                            page.nextCursor
                    )
            } while (hasMore)

            val result =
                mutableListOf<OrganizationTransferRecord>()

            for (organizationId in organizationIds) {
                val response =
                    authenticatedApiCallExecutor.execute {
                        organizationApi.getOrganization(
                            organizationId =
                                organizationId
                        )
                    }

                if (
                    !response.id.equals(
                        organizationId,
                        ignoreCase = true
                    )
                ) {
                    throw IllegalStateException(
                        "Organization response identity mismatch: " +
                            "${response.id} != $organizationId"
                    )
                }

                result +=
                    response.toTransferRecord()
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun nextCursorOrThrow(
        hasMore: Boolean,
        nextCursor: String?
    ): String? {
        if (!hasMore) {
            return null
        }

        return nextCursor
            ?: throw IllegalStateException(
                "Organization response has hasMore=true " +
                    "but nextCursor is null."
            )
    }

    private fun OrganizationResponseDto.toTransferRecord():
        OrganizationTransferRecord {
        return OrganizationTransferRecord(
            id =
                id,
            name =
                name,
            code =
                code,
            address =
                address,
            city =
                city,
            country =
                country,
            lifecycleStatus =
                lifecycleStatus,
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

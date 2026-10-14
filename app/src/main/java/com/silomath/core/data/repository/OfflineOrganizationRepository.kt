package com.silomath.core.data.repository

import androidx.room.withTransaction
import com.silomath.core.data.network.source.OrganizationMembershipRemoteDataSource
import com.silomath.core.data.network.source.OrganizationRemoteDataSource
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.dao.OrganizationDao
import com.silomath.core.database.dao.OrganizationMemberDao
import com.silomath.core.database.dao.OrganizationMembershipDao
import com.silomath.core.database.entity.OrganizationEntity
import com.silomath.core.database.entity.OrganizationMemberEntity
import com.silomath.core.database.entity.OrganizationMembershipEntity
import com.silomath.core.domain.repository.OrganizationRepository
import com.silomath.core.model.organization.OrganizationTransferRecord
import com.silomath.core.model.organization.UserMembershipTransferRecord
import java.time.OffsetDateTime
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Transitional M3 Organization repository.
 *
 * Room remains the Android client read source.
 *
 * Organization and OrganizationMembership remote reads use the V6 REST
 * boundary in :core:network.
 *
 * OrganizationMembership remains in Expand/Migrate:
 * - authoritative canonical memberships are preferred;
 * - organization_members remains only as rollout compatibility fallback;
 * - incomplete historical migration rows are never authoritative;
 * - once authoritative canonical membership data exists for an Organization,
 *   legacy fallback can no longer grant access, even if all canonical
 *   memberships are DISABLED.
 *
 * Final CONTRACT remains blocked until all local Long-key dependencies have
 * migrated to canonical UUID identity.
 */
class OfflineOrganizationRepository @Inject constructor(
    private val organizationDao: OrganizationDao,
    private val organizationMemberDao: OrganizationMemberDao,
    private val siloMathDatabase: SiloMathDatabase,
    private val organizationRemoteDataSource:
        OrganizationRemoteDataSource,
    private val organizationMembershipRemoteDataSource:
        OrganizationMembershipRemoteDataSource
) : OrganizationRepository {

    private val organizationMembershipDao:
        OrganizationMembershipDao
        get() = siloMathDatabase.organizationMembershipDao()

    private suspend fun resolveOrganization(
        organizationId: String
    ): OrganizationEntity? {
        val canonicalUuid =
            canonicalUuidOrNull(
                organizationId
            )
                ?: return null

        return organizationDao
            .getOrganizationByOrganizationId(
                canonicalUuid
            )
    }

    override fun getOrganizations():
        Flow<List<OrganizationEntity>> {
        return organizationDao
            .getAllOrganizations()
    }

    override fun getMembersByOrganization(
        organizationId: String
    ): Flow<List<OrganizationMemberEntity>> {
        val canonicalOrganizationId =
            canonicalUuidOrNull(
                organizationId
            )
                ?: return flowOf(
                    emptyList()
                )

        val canonicalActiveMemberships =
            organizationMembershipDao
                .observeActiveCanonicalMemberships(
                    canonicalOrganizationId
                )

        val canonicalAuthoritativeCount =
            organizationMembershipDao
                .observeAuthoritativeCanonicalMembershipCount(
                    canonicalOrganizationId
                )

        return combine(
            canonicalActiveMemberships,
            canonicalAuthoritativeCount,
            organizationDao.getAllOrganizations()
        ) {
                activeMemberships,
                authoritativeCount,
                organizations ->

            if (authoritativeCount > 0) {
                activeMemberships.map {
                    it.toLegacyUiProjection()
                }
            } else {
                val organization =
                    organizations.firstOrNull {
                        it.organizationId
                            ?.equals(
                                canonicalOrganizationId,
                                ignoreCase = true
                            ) == true
                    }

                if (organization == null) {
                    emptyList()
                } else {
                    organizationMemberDao
                        .getMembersByOrganizationSync(
                            organization.id
                        )
                }
            }
        }
    }

    override suspend fun saveOrganization(
        organization: OrganizationEntity
    ) {
        organizationDao
            .insertOrganization(
                organization
            )
    }

    override suspend fun refreshOrganizations():
        Result<Unit> {
        return try {
            val remoteOrganizations =
                organizationRemoteDataSource
                    .getOrganizations()
                    .getOrThrow()

            siloMathDatabase.withTransaction {
                for (remote in remoteOrganizations) {
                    reconcileOrganization(
                        remote
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshOrganizationMembers(
        organizationId: String
    ): Result<Unit> {
        return try {
            val organization =
                resolveOrganization(
                    organizationId
                )
                    ?: return Result.failure(
                        IllegalStateException(
                            "Local organization not found for id: " +
                                organizationId
                        )
                    )

            val canonicalOrganizationId =
                organization.organizationId
                    ?.let {
                        canonicalUuidOrThrow(
                            it,
                            "Organization.organizationId"
                        )
                    }
                    ?: return Result.failure(
                        IllegalStateException(
                            "Organization remoteId is missing for id: " +
                                organizationId
                        )
                    )

            val remoteMemberships =
                organizationMembershipRemoteDataSource
                    .getOrganizationMemberships(
                        canonicalOrganizationId
                    )
                    .getOrThrow()

            val seenMembershipIds =
                mutableSetOf<String>()

            val seenUserIds =
                mutableSetOf<String>()

            val canonicalEntities =
                remoteMemberships.map { remote ->
                    remote.toCanonicalMembershipEntity(
                        expectedOrganizationId =
                            canonicalOrganizationId,
                        legacyOrganizationId =
                            organization.id,
                        seenMembershipIds =
                            seenMembershipIds,
                        seenUserIds =
                            seenUserIds
                    )
                }

            siloMathDatabase.withTransaction {
                organizationMembershipDao
                    .upsertCanonicalMemberships(
                        canonicalEntities
                    )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrganizationRemoteId(
        organizationId: String
    ): Result<String> {
        val organization =
            resolveOrganization(
                organizationId
            )
                ?: return Result.failure(
                    IllegalStateException(
                        "Local organization not found for id: " +
                            organizationId
                    )
                )

        val organizationRemoteId =
            organization.organizationId

        if (organizationRemoteId.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Organization remoteId is missing for id: " +
                        organizationId
                )
            )
        }

        return Result.success(
            canonicalUuidOrThrow(
                organizationRemoteId,
                "Organization.organizationId"
            )
        )
    }

    private suspend fun reconcileOrganization(
        remote: OrganizationTransferRecord
    ) {
        val canonicalOrganizationId =
            canonicalUuidOrThrow(
                remote.id,
                "Organization.id"
            )

        val canonicalCode =
            remote.code
                .trim()
                .uppercase(Locale.ROOT)

        require(canonicalCode.isNotBlank()) {
            "Organization code received from remote source is blank"
        }

        val lifecycleStatus =
            normalizeOrganizationLifecycleStatus(
                remote.lifecycleStatus
            )

        val createdAt =
            parseServerInstant(
                remote.createdAt,
                fieldName =
                    "Organization.createdAt"
            )

        val updatedAt =
            parseServerInstant(
                remote.updatedAt,
                fieldName =
                    "Organization.updatedAt"
            )

        require(remote.entityVersion >= 1L) {
            "Organization entityVersion must be >= 1"
        }

        val existing =
            organizationDao
                .getOrganizationByOrganizationId(
                    canonicalOrganizationId
                )

        if (existing != null) {
            organizationDao.updateOrganization(
                existing.copy(
                    organizationId =
                        canonicalOrganizationId,
                    name =
                        remote.name,
                    code =
                        canonicalCode,
                    address =
                        remote.address,
                    city =
                        remote.city,
                    country =
                        remote.country,
                    lifecycleStatus =
                        lifecycleStatus,
                    createdAt =
                        createdAt,
                    createdBy =
                        remote.createdBy,
                    updatedAt =
                        updatedAt,
                    updatedBy =
                        remote.updatedBy,
                    entityVersion =
                        remote.entityVersion
                )
            )
        } else {
            organizationDao.insertOrganization(
                OrganizationEntity(
                    organizationId =
                        canonicalOrganizationId,
                    name =
                        remote.name,
                    code =
                        canonicalCode,
                    address =
                        remote.address,
                    city =
                        remote.city,
                    country =
                        remote.country,
                    lifecycleStatus =
                        lifecycleStatus,
                    createdAt =
                        createdAt,
                    createdBy =
                        remote.createdBy,
                    updatedAt =
                        updatedAt,
                    updatedBy =
                        remote.updatedBy,
                    entityVersion =
                        remote.entityVersion
                )
            )
        }
    }

    private fun UserMembershipTransferRecord
        .toCanonicalMembershipEntity(
            expectedOrganizationId: String,
            legacyOrganizationId: Long,
            seenMembershipIds: MutableSet<String>,
            seenUserIds: MutableSet<String>
        ): OrganizationMembershipEntity {

        val canonicalMembershipId =
            canonicalUuidOrThrow(
                membershipId,
                "UserMembership.membershipId"
            )

        val canonicalRemoteOrganizationId =
            canonicalUuidOrThrow(
                organizationId,
                "UserMembership.organizationId"
            )

        require(
            canonicalRemoteOrganizationId ==
                expectedOrganizationId
        ) {
            "UserMembership organizationId mismatch: " +
                "$canonicalRemoteOrganizationId != " +
                expectedOrganizationId
        }

        val canonicalUserId =
            canonicalUuidOrThrow(
                userId,
                "UserMembership.userId"
            )

        require(
            seenMembershipIds.add(
                canonicalMembershipId
            )
        ) {
            "Duplicate membershipId returned by server: " +
                canonicalMembershipId
        }

        require(
            seenUserIds.add(
                canonicalUserId
            )
        ) {
            "Duplicate User membership returned for Organization: " +
                canonicalUserId
        }

        val canonicalRole =
            normalizeMembershipRole(
                role
            )

        val canonicalMembershipLifecycle =
            normalizeMembershipLifecycleStatus(
                membershipLifecycleStatus
            )

        normalizeUserLifecycleStatus(
            userLifecycleStatus
        )

        require(fullName.trim().isNotEmpty()) {
            "UserMembership fullName must not be blank"
        }

        require(email.trim().isNotEmpty()) {
            "UserMembership email must not be blank"
        }

        require(entityVersion >= 1L) {
            "UserMembership entityVersion must be >= 1"
        }

        val canonicalCreatedBy =
            canonicalUuidOrThrow(
                createdBy,
                "UserMembership.createdBy"
            )

        val canonicalUpdatedBy =
            canonicalUuidOrThrow(
                updatedBy,
                "UserMembership.updatedBy"
            )

        val parsedCreatedAt =
            parseServerInstant(
                createdAt,
                "UserMembership.createdAt"
            )

        val parsedUpdatedAt =
            parseServerInstant(
                updatedAt,
                "UserMembership.updatedAt"
            )

        val canonicalDisabledBy =
            disabledBy?.let {
                canonicalUuidOrThrow(
                    it,
                    "UserMembership.disabledBy"
                )
            }

        val parsedDisabledAt =
            disabledAt?.let {
                parseServerInstant(
                    it,
                    "UserMembership.disabledAt"
                )
            }

        if (
            canonicalMembershipLifecycle ==
            "DISABLED"
        ) {
            require(parsedDisabledAt != null) {
                "DISABLED OrganizationMembership requires disabledAt"
            }

            require(canonicalDisabledBy != null) {
                "DISABLED OrganizationMembership requires disabledBy"
            }
        }

        /*
         * fullName / email / userLifecycleStatus are part of the approved
         * CR-API-ORG-MEM-001 transport contract.
         *
         * They are deliberately not written into UserEntity here because
         * UserEntity requires its own canonical created/updated metadata and
         * entityVersion, which UserMembershipResponse does not carry.
         *
         * Fabricating User metadata from Membership metadata is prohibited.
         */
        return OrganizationMembershipEntity(
            legacyOrganizationId =
                legacyOrganizationId,
            organizationId =
                canonicalRemoteOrganizationId,
            userId =
                canonicalUserId,
            legacyRole =
                canonicalRole.toLegacyRole(),
            role =
                canonicalRole,
            membershipId =
                canonicalMembershipId,
            lifecycleStatus =
                canonicalMembershipLifecycle,
            disabledAt =
                parsedDisabledAt,
            disabledBy =
                canonicalDisabledBy,
            createdAt =
                parsedCreatedAt,
            createdBy =
                canonicalCreatedBy,
            updatedAt =
                parsedUpdatedAt,
            updatedBy =
                canonicalUpdatedBy,
            entityVersion =
                entityVersion
        )
    }

    private fun OrganizationMembershipEntity
        .toLegacyUiProjection():
        OrganizationMemberEntity {

        val canonicalRole =
            role
                ?: throw IllegalStateException(
                    "Canonical OrganizationMembership role is missing"
                )

        return OrganizationMemberEntity(
            organizationId =
                legacyOrganizationId,
            userId =
                userId,
            role =
                canonicalRole.toLegacyRole()
        )
    }

    private fun String.toLegacyRole():
        String {
        return when (this) {
            "ADMIN" ->
                "Admin"

            "STANDARD" ->
                "Standard User"

            else ->
                throw IllegalStateException(
                    "Unsupported canonical OrganizationMembership " +
                        "role: $this"
                )
        }
    }

    private fun normalizeOrganizationLifecycleStatus(
        value: String
    ): String {
        return when (
            value
                .trim()
                .uppercase(Locale.ROOT)
        ) {
            "ACTIVE" ->
                "ACTIVE"

            "INACTIVE" ->
                "INACTIVE"

            else ->
                throw IllegalStateException(
                    "Unsupported Organization lifecycle status " +
                        "received from remote source: $value"
                )
        }
    }

    private fun normalizeMembershipRole(
        value: String
    ): String {
        return when (
            value
                .trim()
                .uppercase(Locale.ROOT)
        ) {
            "ADMIN" ->
                "ADMIN"

            "STANDARD" ->
                "STANDARD"

            else ->
                throw IllegalStateException(
                    "Unsupported OrganizationMembership role: $value"
                )
        }
    }

    private fun normalizeMembershipLifecycleStatus(
        value: String
    ): String {
        return when (
            value
                .trim()
                .uppercase(Locale.ROOT)
        ) {
            "ACTIVE" ->
                "ACTIVE"

            "DISABLED" ->
                "DISABLED"

            else ->
                throw IllegalStateException(
                    "Unsupported OrganizationMembership lifecycle: " +
                        value
                )
        }
    }

    private fun normalizeUserLifecycleStatus(
        value: String
    ): String {
        return when (
            value
                .trim()
                .uppercase(Locale.ROOT)
        ) {
            "ACTIVE" ->
                "ACTIVE"

            "DISABLED" ->
                "DISABLED"

            else ->
                throw IllegalStateException(
                    "Unsupported User lifecycle status: $value"
                )
        }
    }

    private fun canonicalUuidOrNull(
        value: String
    ): String? {
        val canonical =
            value
                .trim()
                .lowercase(Locale.ROOT)

        if (
            !com.silomath.core.common.UuidUtils
                .isValidUuid(canonical)
        ) {
            return null
        }

        return canonical
    }

    private fun canonicalUuidOrThrow(
        value: String,
        fieldName: String
    ): String {
        return canonicalUuidOrNull(
            value
        )
            ?: throw IllegalStateException(
                "Invalid UUID received for $fieldName: $value"
            )
    }

    private fun parseServerInstant(
        value: String,
        fieldName: String
    ): Long {
        return try {
            OffsetDateTime
                .parse(value)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            throw IllegalStateException(
                "Invalid UTC instant received for $fieldName: $value",
                e
            )
        }
    }
}
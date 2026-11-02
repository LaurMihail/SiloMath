package com.silomath.core.domain.model

import com.silomath.core.model.OrganizationId
import com.silomath.core.model.OrganizationMembershipId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GovernanceModelsTest {

    private val userId =
        UserId("11111111-1111-1111-1111-111111111111")

    private val organizationId =
        OrganizationId("22222222-2222-2222-2222-222222222222")

    private val membershipId =
        OrganizationMembershipId(
            "33333333-3333-3333-3333-333333333333"
        )

    private val instant =
        Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun entityVersionStartsAtOneOrHigher() {
        val organization = Organization(
            organizationId = organizationId,
            name = "Organization",
            code = "ORG",
            address = null,
            city = null,
            country = null,
            lifecycleStatus = MasterStatus.ACTIVE,
            createdAt = instant,
            createdBy = userId.value,
            updatedAt = instant,
            updatedBy = userId.value,
            entityVersion = 1L
        )

        assertEquals(1L, organization.entityVersion)
    }

    @Test
    fun userRejectsZeroEntityVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            User(
                userId = userId,
                fullName = "Test User",
                email = "user@example.com",
                lifecycleStatus = UserStatus.ACTIVE,
                createdAt = instant,
                createdBy = null,
                updatedAt = instant,
                updatedBy = userId.value,
                entityVersion = 0L
            )
        }
    }

    @Test
    fun organizationRejectsZeroEntityVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            Organization(
                organizationId = organizationId,
                name = "Organization",
                code = "ORG",
                address = null,
                city = null,
                country = null,
                lifecycleStatus = MasterStatus.ACTIVE,
                createdAt = instant,
                createdBy = userId.value,
                updatedAt = instant,
                updatedBy = userId.value,
                entityVersion = 0L
            )
        }
    }

    @Test
    fun membershipRejectsZeroEntityVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            OrganizationMembership(
                membershipId = membershipId,
                organizationId = organizationId,
                userId = userId,
                role = OrganizationRole.STANDARD,
                lifecycleStatus = MembershipStatus.ACTIVE,
                disabledAt = null,
                disabledBy = null,
                createdAt = instant,
                createdBy = userId.value,
                updatedAt = instant,
                updatedBy = userId.value,
                entityVersion = 0L
            )
        }
    }

    @Test
    fun activeMembershipAllowsConditionalDisableMetadataToBeAbsent() {
        val membership = OrganizationMembership(
            membershipId = membershipId,
            organizationId = organizationId,
            userId = userId,
            role = OrganizationRole.ADMIN,
            lifecycleStatus = MembershipStatus.ACTIVE,
            disabledAt = null,
            disabledBy = null,
            createdAt = instant,
            createdBy = userId.value,
            updatedAt = instant,
            updatedBy = userId.value,
            entityVersion = 1L
        )

        assertEquals(MembershipStatus.ACTIVE, membership.lifecycleStatus)
        assertNull(membership.disabledAt)
        assertNull(membership.disabledBy)
    }
}
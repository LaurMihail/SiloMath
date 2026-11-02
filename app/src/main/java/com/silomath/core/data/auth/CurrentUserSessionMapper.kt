package com.silomath.core.data.auth

import com.silomath.core.data.network.dto.CurrentUserResponseDto
import com.silomath.core.domain.model.AuthorizedOrganizationContext

fun CurrentUserResponseDto.toAuthorizedOrganizations():
    List<AuthorizedOrganizationContext> =
    memberships
        .map {
            AuthorizedOrganizationContext(
                membershipId = it.membershipId.trim(),
                organizationId = it.organizationId.trim(),
                organizationName = it.organizationName.trim(),
                organizationCode =
                    it.organizationCode.trim().uppercase(),
                role = it.role.trim()
            )
        }
        .filter {
            it.membershipId.isNotBlank() &&
                it.organizationId.isNotBlank() &&
                it.organizationName.isNotBlank() &&
                it.organizationCode.isNotBlank()
        }
        .distinctBy { it.organizationId }
        .sortedWith(
            compareBy<AuthorizedOrganizationContext> {
                it.organizationName.lowercase()
            }.thenBy {
                it.organizationCode
            }
        )

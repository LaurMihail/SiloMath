package com.silomath.backend.location

import com.silomath.backend.security.AuthorizedOrganizationMembership
import com.silomath.backend.security.OrganizationAuthorizationResolver
import com.silomath.backend.security.OrganizationAuthorizationResult
import com.silomath.backend.security.OrganizationRole
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

internal class JdbcOrganizationAuthorizationResolver(
    private val databaseUrl: String,
    private val databaseUser: String?,
    private val databasePassword: String?
) : OrganizationAuthorizationResolver {

    override suspend fun authorize(
        userId: String,
        organizationId: String
    ): OrganizationAuthorizationResult =
        try {
            openConnection().use { connection ->
                connection.prepareStatement(
                    """
                    select
                        m.membership_id,
                        m.organization_id,
                        m.role
                    from silomath.organization_memberships m
                    join silomath.organizations o
                      on o.organization_id =
                         m.organization_id
                    where m.user_id = ?::uuid
                      and m.organization_id = ?::uuid
                      and m.lifecycle_status = 'ACTIVE'
                      and o.lifecycle_status = 'ACTIVE'
                    limit 1
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(
                        1,
                        userId
                    )
                    statement.setString(
                        2,
                        organizationId
                    )

                    statement.executeQuery().use {
                        rows ->
                        if (!rows.next()) {
                            OrganizationAuthorizationResult
                                .Forbidden
                        } else {
                            val role =
                                when (
                                    rows.getString("role")
                                ) {
                                    "ADMIN" ->
                                        OrganizationRole.ADMIN

                                    "STANDARD" ->
                                        OrganizationRole.STANDARD

                                    else ->
                                        null
                                }

                            if (role == null) {
                                OrganizationAuthorizationResult
                                    .Forbidden
                            } else {
                                OrganizationAuthorizationResult
                                    .Authorized(
                                        AuthorizedOrganizationMembership(
                                            membershipId =
                                                rows.getString(
                                                    "membership_id"
                                                ),
                                            organizationId =
                                                rows.getString(
                                                    "organization_id"
                                                ),
                                            role = role
                                        )
                                    )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            OrganizationAuthorizationResult.Forbidden
        }

    private fun openConnection(): Connection {
        if (databaseUser.isNullOrBlank()) {
            return DriverManager.getConnection(
                databaseUrl
            )
        }

        val properties =
            Properties().apply {
                setProperty(
                    "user",
                    databaseUser
                )

                if (
                    !databasePassword.isNullOrBlank()
                ) {
                    setProperty(
                        "password",
                        databasePassword
                    )
                }
            }

        return DriverManager.getConnection(
            databaseUrl,
            properties
        )
    }
}

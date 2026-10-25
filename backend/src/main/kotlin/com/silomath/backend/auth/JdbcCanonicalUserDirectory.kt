package com.silomath.backend.auth

import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

internal class JdbcCanonicalUserDirectory(
    private val databaseUrl: String,
    private val databaseUser: String?,
    private val databasePassword: String?
) : CanonicalUserDirectory {

    override fun findSessionIdentity(
        userId: String
    ): CanonicalSessionIdentity? =
        openConnection().use { connection ->
            val user = connection.findUser(userId) ?: return null
            user.copy(
                memberships = connection.findAuthorizedMemberships(userId)
            )
        }

    private fun openConnection(): Connection {
        if (databaseUser.isNullOrBlank()) {
            return DriverManager.getConnection(databaseUrl)
        }

        val properties = Properties().apply {
            setProperty("user", databaseUser)
            if (!databasePassword.isNullOrBlank()) {
                setProperty("password", databasePassword)
            }
        }
        return DriverManager.getConnection(databaseUrl, properties)
    }

    private fun Connection.findUser(
        userId: String
    ): CanonicalSessionIdentity? {
        prepareStatement(
            """
            select
                user_id,
                full_name,
                email,
                lifecycle_status
            from silomath.users
            where user_id = ?::uuid
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return null

                val lifecycle = when (rows.getString("lifecycle_status")) {
                    "ACTIVE" -> CanonicalUserLifecycle.ACTIVE
                    "DISABLED" -> CanonicalUserLifecycle.DISABLED
                    else -> return null
                }

                return CanonicalSessionIdentity(
                    user = SessionUserSummary(
                        userId = rows.getString("user_id"),
                        fullName = rows.getString("full_name"),
                        email = rows.getString("email")
                    ),
                    lifecycle = lifecycle,
                    memberships = emptyList()
                )
            }
        }
    }

    private fun Connection.findAuthorizedMemberships(
        userId: String
    ): List<AuthorizedMembershipSummary> {
        prepareStatement(
            """
            select
                m.membership_id,
                m.organization_id,
                o.name as organization_name,
                o.code as organization_code,
                m.role
            from silomath.organization_memberships m
            join silomath.organizations o
              on o.organization_id = m.organization_id
            where m.user_id = ?::uuid
              and m.lifecycle_status = 'ACTIVE'
              and o.lifecycle_status = 'ACTIVE'
            order by o.name, o.organization_id
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { rows ->
                val result = mutableListOf<AuthorizedMembershipSummary>()
                while (rows.next()) {
                    result += AuthorizedMembershipSummary(
                        membershipId = rows.getString("membership_id"),
                        organizationId = rows.getString("organization_id"),
                        organizationName = rows.getString("organization_name"),
                        organizationCode = rows.getString("organization_code"),
                        role = rows.getString("role")
                    )
                }
                return result
            }
        }
    }
}

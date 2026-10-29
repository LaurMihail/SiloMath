package com.silomath.backend.health

import com.silomath.backend.config.BackendConfig
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

fun interface BackendReadinessProbe {
    fun isReady(): Boolean
}

class JdbcBackendReadinessProbe(
    private val config: BackendConfig
) : BackendReadinessProbe {

    override fun isReady(): Boolean {
        if (!config.authRuntimeConfigured) {
            return false
        }

        return try {
            openConnection().use { connection ->
                connection.prepareStatement("select 1").use { statement ->
                    statement.executeQuery().use { result ->
                        result.next() && result.getInt(1) == 1
                    }
                }
            }
        } catch (error: Exception) {
            System.err.println(
                "SiloMath backend readiness database check failed: ${diagnostic(error)}"
            )
            false
        }
    }

    private fun diagnostic(error: Exception): String =
        when (error) {
            is SQLException ->
                "type=${error.javaClass.name} sqlState=${error.sqlState ?: "unknown"} vendorCode=${error.errorCode}"
            else ->
                "type=${error.javaClass.name}"
        }

    private fun openConnection(): Connection {
        val databaseUrl = requireNotNull(config.databaseUrl)

        if (config.databaseUser.isNullOrBlank()) {
            return DriverManager.getConnection(databaseUrl)
        }

        val properties =
            Properties().apply {
                setProperty("user", config.databaseUser)
                if (!config.databasePassword.isNullOrBlank()) {
                    setProperty("password", config.databasePassword)
                }
            }

        return DriverManager.getConnection(databaseUrl, properties)
    }
}

package com.silomath.backend.health

import com.silomath.backend.config.BackendConfig
import java.sql.Connection
import java.sql.DriverManager
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
        } catch (_: Exception) {
            false
        }
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

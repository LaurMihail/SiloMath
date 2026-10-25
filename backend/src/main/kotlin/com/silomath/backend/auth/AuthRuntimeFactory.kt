package com.silomath.backend.auth

import com.silomath.backend.config.BackendConfig

internal object AuthRuntimeFactory {
    fun create(config: BackendConfig): AuthService {
        if (!config.authRuntimeConfigured) {
            return UnavailableAuthService()
        }

        val databaseUrl = requireNotNull(config.databaseUrl)
        val supabaseUrl = requireNotNull(config.supabaseUrl)
        val supabaseSecretKey = requireNotNull(config.supabaseSecretKey)

        return DefaultAuthService(
            provider = SupabaseAuthenticationProvider(
                baseUrl = supabaseUrl,
                secretKey = supabaseSecretKey
            ),
            userDirectory = JdbcCanonicalUserDirectory(
                databaseUrl = databaseUrl,
                databaseUser = config.databaseUser,
                databasePassword = config.databasePassword
            )
        )
    }
}

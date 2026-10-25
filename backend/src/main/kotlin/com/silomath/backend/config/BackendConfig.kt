package com.silomath.backend.config

data class BackendConfig(
    val host: String,
    val port: Int,
    val runtimeEnvironment: String,
    val databaseUrl: String? = null,
    val databaseUser: String? = null,
    val databasePassword: String? = null,
    val supabaseUrl: String? = null,
    val supabaseSecretKey: String? = null
) {
    val authRuntimeConfigured: Boolean
        get() =
            !databaseUrl.isNullOrBlank() &&
                !supabaseUrl.isNullOrBlank() &&
                !supabaseSecretKey.isNullOrBlank()

    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_RUNTIME_ENVIRONMENT = "development"

        private const val HOST_ENV = "SILOMATH_BACKEND_HOST"
        private const val PORT_ENV = "SILOMATH_BACKEND_PORT"
        private const val RUNTIME_ENV_ENV = "SILOMATH_BACKEND_ENVIRONMENT"
        private const val DATABASE_URL_ENV = "SILOMATH_DATABASE_URL"
        private const val DATABASE_USER_ENV = "SILOMATH_DATABASE_USER"
        private const val DATABASE_PASSWORD_ENV = "SILOMATH_DATABASE_PASSWORD"
        private const val SUPABASE_URL_ENV = "SILOMATH_SUPABASE_URL"
        private const val SUPABASE_SECRET_KEY_ENV = "SILOMATH_SUPABASE_SECRET_KEY"

        private val ALLOWED_RUNTIME_ENVIRONMENTS =
            setOf(
                "development",
                "staging",
                "production"
            )

        fun fromEnvironment(
            values: Map<String, String> = System.getenv()
        ): BackendConfig {
            val host =
                values[HOST_ENV]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_HOST

            val port =
                values[PORT_ENV]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toIntOrNull()
                    ?: DEFAULT_PORT

            require(port in 1..65535) {
                "$PORT_ENV must be an integer between 1 and 65535."
            }

            val runtimeEnvironment =
                values[RUNTIME_ENV_ENV]
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_RUNTIME_ENVIRONMENT

            require(
                runtimeEnvironment in ALLOWED_RUNTIME_ENVIRONMENTS
            ) {
                "$RUNTIME_ENV_ENV must be one of: " +
                    ALLOWED_RUNTIME_ENVIRONMENTS.joinToString(", ")
            }

            val config = BackendConfig(
                host = host,
                port = port,
                runtimeEnvironment = runtimeEnvironment,
                databaseUrl = values.nonBlank(DATABASE_URL_ENV),
                databaseUser = values.nonBlank(DATABASE_USER_ENV),
                databasePassword = values.nonBlank(DATABASE_PASSWORD_ENV),
                supabaseUrl = values.nonBlank(SUPABASE_URL_ENV)?.trimEnd('/'),
                supabaseSecretKey = values.nonBlank(SUPABASE_SECRET_KEY_ENV)
            )

            if (runtimeEnvironment == "production") {
                require(config.authRuntimeConfigured) {
                    "Production requires database and Supabase Auth server secrets."
                }
            }

            return config
        }

        private fun Map<String, String>.nonBlank(key: String): String? =
            this[key]?.trim()?.takeIf { it.isNotEmpty() }
    }
}

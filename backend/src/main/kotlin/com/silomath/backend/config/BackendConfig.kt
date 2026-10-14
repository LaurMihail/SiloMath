package com.silomath.backend.config

data class BackendConfig(
    val host: String,
    val port: Int,
    val runtimeEnvironment: String
) {
    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_RUNTIME_ENVIRONMENT = "development"

        private const val HOST_ENV = "SILOMATH_BACKEND_HOST"
        private const val PORT_ENV = "SILOMATH_BACKEND_PORT"
        private const val RUNTIME_ENV_ENV = "SILOMATH_BACKEND_ENVIRONMENT"

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

            return BackendConfig(
                host = host,
                port = port,
                runtimeEnvironment = runtimeEnvironment
            )
        }
    }
}

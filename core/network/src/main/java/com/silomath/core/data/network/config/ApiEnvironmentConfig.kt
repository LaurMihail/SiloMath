package com.silomath.core.data.network.config

data class ApiEnvironmentConfig(
    val baseUrl: String
) {

    init {
        require(baseUrl.isNotBlank()) {
            "Backend API base URL must not be blank"
        }

        require(
            baseUrl.startsWith("http://") ||
                baseUrl.startsWith("https://")
        ) {
            "Backend API base URL must use HTTP or HTTPS"
        }

        require(baseUrl.endsWith("/")) {
            "Backend API base URL must end with '/'"
        }
    }
}

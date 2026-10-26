package com.silomath.backend.location

import com.silomath.backend.auth.JdbcCanonicalUserDirectory
import com.silomath.backend.auth.SupabaseServerAccessTokenVerifier
import com.silomath.backend.config.BackendConfig
import com.silomath.backend.security.AccessTokenVerifier
import com.silomath.backend.security.OrganizationAuthorizationResolver
import com.silomath.backend.security.RejectingAccessTokenVerifier

data class LocationRuntime(
    val accessTokenVerifier: AccessTokenVerifier,
    val organizationAuthorizationResolver:
        OrganizationAuthorizationResolver,
    val readService: LocationReadService?,
    val mutationService: LocationMutationService?
)

object LocationRuntimeFactory {

    fun create(
        config: BackendConfig
    ): LocationRuntime {
        if (!config.authRuntimeConfigured) {
            return LocationRuntime(
                accessTokenVerifier =
                    RejectingAccessTokenVerifier,
                organizationAuthorizationResolver =
                    OrganizationAuthorizationResolver {
                            _,
                            _ ->
                        com.silomath.backend.security
                            .OrganizationAuthorizationResult
                            .Forbidden
                    },
                readService = null,
                mutationService = null
            )
        }

        val databaseUrl =
            requireNotNull(
                config.databaseUrl
            )

        val userDirectory =
            JdbcCanonicalUserDirectory(
                databaseUrl =
                    databaseUrl,
                databaseUser =
                    config.databaseUser,
                databasePassword =
                    config.databasePassword
            )

        return LocationRuntime(
            accessTokenVerifier =
                SupabaseServerAccessTokenVerifier(
                    baseUrl =
                        requireNotNull(
                            config.supabaseUrl
                        ),
                    secretKey =
                        requireNotNull(
                            config.supabaseSecretKey
                        ),
                    userDirectory =
                        userDirectory
                ),
            organizationAuthorizationResolver =
                JdbcOrganizationAuthorizationResolver(
                    databaseUrl =
                        databaseUrl,
                    databaseUser =
                        config.databaseUser,
                    databasePassword =
                        config.databasePassword
                ),
            readService =
                JdbcLocationReadService(
                    databaseUrl =
                        databaseUrl,
                    databaseUser =
                        config.databaseUser,
                    databasePassword =
                        config.databasePassword
                ),
            mutationService =
                JdbcLocationMutationService(
                    databaseUrl =
                        databaseUrl,
                    databaseUser =
                        config.databaseUser,
                    databasePassword =
                        config.databasePassword
                )
        )
    }
}

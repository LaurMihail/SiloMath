package com.silomath.backend

import com.silomath.backend.api.configureApiSerialization
import com.silomath.backend.api.configureCorrelationIds
import com.silomath.backend.auth.AuthRuntimeFactory
import com.silomath.backend.config.BackendConfig
import com.silomath.backend.health.BackendReadinessProbe
import com.silomath.backend.health.JdbcBackendReadinessProbe
import com.silomath.backend.location.LocationRuntimeFactory
import com.silomath.backend.routes.configureAuthRouting
import com.silomath.backend.routes.configureFoundationRouting
import com.silomath.backend.routes.configureOfflineManifestRouting
import com.silomath.backend.offline.JdbcOfflineManifestService
import com.silomath.backend.offline.JdbcOfflinePackageService
import com.silomath.backend.routes.configureOfflinePackageRouting
import java.nio.file.Path
import com.silomath.backend.routes.configureStorageUnitRouting
import com.silomath.backend.routes.configureStorageSpaceRouting
import com.silomath.backend.routes.configureLocationRouting
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = BackendConfig.fromEnvironment()

    embeddedServer(
        factory = Netty,
        host = config.host,
        port = config.port
    ) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: BackendConfig = BackendConfig.fromEnvironment(),
    readinessProbe: BackendReadinessProbe = JdbcBackendReadinessProbe(config)
) {
    @Suppress("UNUSED_VARIABLE")
    val runtimeEnvironment = config.runtimeEnvironment

    configureCorrelationIds()
    configureApiSerialization()
    configureFoundationRouting(readinessProbe)
    configureAuthRouting(
        AuthRuntimeFactory.create(config)
    )
    val locationRuntime = LocationRuntimeFactory.create(config)
    configureLocationRouting(locationRuntime)
    val storageUnitService=config.databaseUrl?.takeIf{config.authRuntimeConfigured}?.let {
        com.silomath.backend.storageunit.JdbcStorageUnitService(it,config.databaseUser,config.databasePassword)
    }
    val storageSpaceService=config.databaseUrl?.takeIf{config.authRuntimeConfigured}?.let {
        com.silomath.backend.storagespace.JdbcStorageSpaceService(it,config.databaseUser,config.databasePassword)
    }
    configureStorageUnitRouting(locationRuntime.accessTokenVerifier,locationRuntime.organizationAuthorizationResolver,storageUnitService,storageSpaceService)
    configureStorageSpaceRouting(locationRuntime.accessTokenVerifier,locationRuntime.organizationAuthorizationResolver,storageSpaceService)
    val packageService = config.offlinePackageDirectory?.let { directory ->
        config.databaseUrl?.let { url ->
            JdbcOfflinePackageService(url, config.databaseUser, config.databasePassword, Path.of(directory))
        }
    }
    configureOfflineManifestRouting(
        locationRuntime.accessTokenVerifier,
        locationRuntime.organizationAuthorizationResolver,
        packageService ?: config.databaseUrl?.let {
            JdbcOfflineManifestService(it, config.databaseUser, config.databasePassword)
        }
    )
    configureOfflinePackageRouting(locationRuntime.accessTokenVerifier, locationRuntime.organizationAuthorizationResolver, packageService)
}

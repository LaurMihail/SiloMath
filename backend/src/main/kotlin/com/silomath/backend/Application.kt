package com.silomath.backend

import com.silomath.backend.api.configureApiSerialization
import com.silomath.backend.api.configureCorrelationIds
import com.silomath.backend.auth.AuthRuntimeFactory
import com.silomath.backend.config.BackendConfig
import com.silomath.backend.location.LocationRuntimeFactory
import com.silomath.backend.routes.configureAuthRouting
import com.silomath.backend.routes.configureFoundationRouting
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
    config: BackendConfig = BackendConfig.fromEnvironment()
) {
    @Suppress("UNUSED_VARIABLE")
    val runtimeEnvironment = config.runtimeEnvironment

    configureCorrelationIds()
    configureApiSerialization()
    configureFoundationRouting()
    configureAuthRouting(
        AuthRuntimeFactory.create(config)
    )
    configureLocationRouting(
        LocationRuntimeFactory.create(config)
    )
}

package com.silomath.backend.api

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.util.AttributeKey
import java.util.UUID

const val CORRELATION_ID_HEADER = "X-Correlation-Id"

private val CorrelationIdKey =
    AttributeKey<String>("SiloMathCorrelationId")

private val CorrelationIdPlugin = createApplicationPlugin(
    name = "SiloMathCorrelationId"
) {
    onCall { call ->
        val correlationId = call.request.headers[CORRELATION_ID_HEADER]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString()

        call.attributes.put(CorrelationIdKey, correlationId)
        call.response.headers.append(
            CORRELATION_ID_HEADER,
            correlationId
        )
    }
}

/**
 * Frozen V6 request correlation boundary.
 *
 * A caller-provided trace identifier may be propagated, otherwise the server
 * creates one. Correlation is diagnostic only and never participates in
 * authentication or authorization.
 */
fun Application.configureCorrelationIds() {
    install(CorrelationIdPlugin)
}

fun ApplicationCall.correlationId(): String =
    attributes[CorrelationIdKey]

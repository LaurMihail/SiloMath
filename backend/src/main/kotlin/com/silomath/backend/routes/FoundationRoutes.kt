package com.silomath.backend.routes

import com.silomath.backend.health.BackendReadinessProbe
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

object FoundationRoutes {
    const val API_BASE_PATH = "/api/v1"
    const val HEALTH_BASE_PATH = "/health"
}

fun Application.configureFoundationRouting(
    readinessProbe: BackendReadinessProbe
) {
    routing {
        route(FoundationRoutes.HEALTH_BASE_PATH) {
            get("/live") {
                call.respondText(
                    text = "OK",
                    contentType = ContentType.Text.Plain,
                    status = HttpStatusCode.OK
                )
            }

            get("/ready") {
                val ready = readinessProbe.isReady()
                call.respondText(
                    text = if (ready) "READY" else "NOT_READY",
                    contentType = ContentType.Text.Plain,
                    status =
                        if (ready) {
                            HttpStatusCode.OK
                        } else {
                            HttpStatusCode.ServiceUnavailable
                        }
                )
            }
        }

        /*
         * Canonical SiloMath V6 business API mount point.
         *
         * IMP-REP-002 intentionally exposes no business endpoint here.
         * Business routes are introduced only by their dependency-aware
         * API/domain backlog tasks.
         */
        route(FoundationRoutes.API_BASE_PATH) {
        }
    }
}

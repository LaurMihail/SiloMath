package com.silomath.backend.routes

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

fun Application.configureFoundationRouting() {
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
                call.respondText(
                    text = "READY",
                    contentType = ContentType.Text.Plain,
                    status = HttpStatusCode.OK
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
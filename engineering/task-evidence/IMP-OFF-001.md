# IMP-OFF-001 — Offline Manifest Endpoint

Status: IMPLEMENTED / LOCAL VERIFIED

Execution reordered by explicit user authorization to unblock IMP-AUTH-010. AUTH-010 remains BLOCKED_TECHNICAL until verified authorized packages can be atomically activated locally. GATE-M5 is a scheduling predecessor, not a requirement to execute calculation in the manifest endpoint.

Authority: 01 Product Baseline offline working set; 02 D29/FD-45; 09 section 4.17; 10 offline_working_set_manifests; 11 sections 10/16; 12 sections 13–16; 17 section 5.

GET /api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-manifest validates the installation identity, enforces bearer/organization access and reads the current campaign/installation/version manifest. JDBC rechecks active user/membership/organization and campaign access (Admin or active Campaign Team) in a repeatable-read snapshot. It returns stored counts/hashes/compatibility metadata without asserting local READY. Missing current manifests return 404; dependency errors return retryable 503. Package creation/transfer belongs to OFF-002.

Validation: `.\gradlew.bat -p backend test --tests com.silomath.backend.offline.OfflineManifestRoutesTest --tests com.silomath.backend.security.AuthMiddlewareTest` — PASS, 8 manifest route tests and 6 auth middleware tests. Production JDBC SQL reviewed against canonical schema; live PostgreSQL integration not executed.

`git diff --check` — PASS. No environment changes or push.
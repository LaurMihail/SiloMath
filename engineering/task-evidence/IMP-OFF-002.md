# IMP-OFF-002 — Campaign-scoped Offline Package Endpoint

Status: IMPLEMENTED / LOCAL VERIFIED

Authority: 01 offline working set; 02 D29/FD-45; 09 section 4.17; 10 canonical physical schema; 11 section 16; 12 sections 13–16 and OS-D25–29; 15 section 26; 17 session/offline authorization.

Manifest requests without an explicit version materialize a fresh immutable package from a repeatable-read PostgreSQL snapshot. The closed query inventory scopes campaign, snapshot children, hierarchy, pinned geometry/Commodity versions, current authorization, Stocktaking, Issues and Evidence metadata. Decimal JSON is preserved without floating-point conversion. Coverage checks reject missing scope/hierarchy/version context. Evidence binary references remain separate and local-private paths are excluded. Counts, per-collection hashes and pinned compatibility metadata accompany the whole-artifact SHA-256.

The authenticated installation owner is checked (including known revocation). A durable private file is written before a transaction switches the current server manifest. Exact version/manifest download revalidates access, verifies stored bytes and emits binary content plus integrity headers. It never asserts Android READY. Failed materialization leaves the previous manifest untouched. The private artifact directory is controlled deployment configuration `SILOMATH_OFFLINE_PACKAGE_DIRECTORY`; no external environment was configured.

Validation:
- Created a new isolated PostgreSQL 18 cluster under Windows TEMP on localhost:55439. Loaded the canonical 20261015000000 expansion and 20261018 additive migrations; no existing database/Supabase was accessed. Stopped this cluster after testing; no files were deleted.
- With `SILOMATH_OFFLINE_TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:55439/postgres`, ran `.\gradlew.bat -p backend test --tests 'com.silomath.backend.offline.*' --tests com.silomath.backend.config.AuthRuntimeConfigTest`: PASS, 19 tests, zero failures/skips. Five real-JDBC tests cover materialization/download, coordinate precision, tenant/user denial, device revocation, failed-publication preservation and incomplete compatibility refusal. Four package-route tests cover exact bytes/headers, request binding, denial and retryable dependency failure.
- JDBC integration tests require the explicitly supplied disposable localhost URL; absent that environment variable they are skipped, never pointed at configured application databases.
- `git diff --check`: PASS.

Local package acquisition/verification/activation and AUTH-010 remain pending; no offline eligibility is enabled by this task.
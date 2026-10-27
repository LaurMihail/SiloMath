# IMP-CALC-022 - shared executable engine / backend verification

Classification: numerical client/server component parity IMPLEMENTED / LOCAL VERIFIED. CampaignSnapshot-to-authoritative-input loading and real connected workflow integration remain pending CAM-008; GATE-M5 is not Done.

Authority: Doc13 sections23/25, CE-D32/42, CALC-AC13/19; approved CR-CALC-VERSION-CONTRACT-001 exact tuple. Engine/fingerprint/golden predecessors are already locally verified. No schema, math or baseline change in this subtask.

## Changed files and behavior

- backend/settings.gradle.kts includes the existing pure Kotlin calculation-engine project, not a second numerical implementation. A local catalog override preserves backend's existing kotlinx-serialization-json1.9.0; Android build keeps its existing catalog version.
- backend/build.gradle.kts consumes that project and supplies the repository path to tests.
- calculation-engine/build.gradle.kts resolves its test repository path from its own project directory, so the same project works under either build root. No tests are skipped/disabled.
- AuthoritativeCalculationVerifier recomputes from explicitly server-supplied frozen canonical input; same fingerprint is required, negative volume is invalid, absolute official volume delta<=0.001m3 is accepted and larger delta returns GEOM_CALCULATION_MISMATCH. Stale/unknown-version inputs cannot use tolerance or latest-version fallback.
- CalculationParityTest crosses the actual backend verifier and shared client numerical module for all ten normative family vectors, the independent canonical hash reference, density/quantity parity, exact inclusive tolerance boundaries and negative/stale/unsupported rejection.

No HTTP endpoint, auth bypass, client-authoritative snapshot loader, Supabase call or workflow-state transition is created. Future endpoint/sync callers must load authoritative frozen context themselves; this service does not claim to authorize arbitrary client-supplied context.

## Commands/results

- `.\gradlew.bat -p backend :test --tests com.silomath.backend.calculation.CalculationParityTest`: PASS,4 tests,0 failures/errors/skips. All ten family assertions executed.
- `.\gradlew.bat -p backend :dependencyInsight --configuration runtimeClasspath --dependency kotlinx-serialization-json`: selected1.9.0 (existing backend version); no transitive upgrade to the Android catalog version.
- `.\gradlew.bat :calculation-engine:test :calculation-engine:build :app:compileProductionDebugKotlin`: PASS; numerical module recompiled for the client build;58-test result unchanged/up-to-date and Production compilation PASS. This invocation does not claim a new58-test execution where Gradle reused test outputs.
- `engineering/validate-module-dependencies.ps1`: PASS; original19-module graph retained, calculation-engine pure Kotlin/JVM.
- `git diff --check`: PASS.

No push, Supabase mutation, historical metadata reassignment, package activation or milestone completion. Pre-existing IDE changes excluded.

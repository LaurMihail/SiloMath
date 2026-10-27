# IMP-UI-008 — Navigation3 and Android platform navigation

Status: IMPLEMENTED / GREEN for navigation infrastructure and migration of currently existing Production composition. Own audited predecessor GATE-M1 is accepted DONE (PR7 evidence; merge5d2f75f checked ancestor). UI-007 is GREEN at4afe034f877819edcb0762d868d44033d4a80de9; MD-002 remains unimplemented until this commit.

## Authority and implementation

Doc01/02 and Doc06 navigation identity/authorization/Back, Doc07 section9, Doc08 core:navigation/app ownership, Doc18 TA-D117/118/120 and platform requirements. Existing approved ADR-010 defines Navigation3; no new product behavior/version contract or baseline amendment.

- core:navigation owns @Serializable NavKey hierarchy, canonical five shell identities/order, stable canonical UUID arguments, master-data and full Campaign→Location→Unit→Space hierarchy context, parent-chain construction, saveable per-tab back stacks, entry SavedState and ViewModel-store decorators. No serialized mutable domain objects, passwords, tokens, recovery secrets or calculation-version metadata.
- app remains composition root/entry renderer and maps trusted auth/organization state into navigation scope. Removed all Production NavHost/rememberNavController/composable string-route authority. Legacy presentation-only bottom-bar API is not used as a routing authority. No feature-to-feature/core-to-feature coupling.
- Contextual-reference Intent transport is typed JSON keys, strictly decoded (unknown fields/keys, invalid UUIDs and oversized requests rejected). Authentication/org/role presentation checks precede domain authorization; authorization is rechecked after async work and again before rendering restored content. Cancellation propagates; dependency failures deny access without becoming authorization grants.
- Production currently authorizes shell targets only after rereading actual AuthRepository scope. Unimplemented entity/campaign/state renderers remain denied by default. Feature owners must enable their authoritative domain resolver together with their renderer; no synthetic authorization, metadata-only compatibility or new blank detail route is exposed. This infrastructure task does not complete those feature tasks.
- Single Activity handles new contextual-reference intents; no public URI scheme/product URL contract invented. Recovery remains its existing separate auth flow; only boolean presentation flags/keys are saveable, not recovery credential values. Reset Back uses the canonical SignIn parent. Existing AuthViewModel and auth repository logic remain unchanged.
- Actual NavDisplay uses standard platform/Jetpack predictive-back support. Stack pop occurs on completed Back only. Detail state/Unit identity/search/scroll survives Back; per-tab stacks retain inactive hierarchy. Saved-state restoration revalidates access before protected content.
- Explicit edge-to-edge helper follows rendered canonical light-surface appearance instead of device wallpaper/night-mode authority. Deliberate system/cutout/gesture/IME bounds, scrollable existing auth forms, lifecycle-aware StateFlow collection in current destinations and explicit AppHeader inset ownership avoid hidden actions/double status insets.

## Dependencies

New Navigation3 runtime/UI pinned1.0.1, a stable official release compatible with existing Compose generation. The newer1.2.0 POM would require a newer runtime; it was not used to force a Compose/Kotlin upgrade. Lifecycle runtime-compose/viewmodel-navigation3 reuse the existing2.11.0 catalog pin; Kotlin/AGP/SDK/Compose BOM/legacy catalog pins unchanged. Any transitively retained Navigation2 classes through existing Hilt are not a Production route owner.

Primary API provenance: https://developer.android.com/guide/navigation/navigation-3/save-state ; https://developer.android.com/guide/navigation/navigation-3/basics ; https://developer.android.com/jetpack/androidx/releases/navigation3 ; https://developer.android.com/guide/navigation/navigation-event/dispatcher . Exact selected artifacts/POMs were read from Google's Maven repository. No external service mutation.

## Executed verification

Final command after last source edit:
`.\gradlew.bat :core:navigation:testDebugUnitTest :app:testProductionDebugUnitTest --tests com.silomath.app.navigation.NavigationScopeAdapterTest --tests com.silomath.app.navigation.ProductionDestinationAuthorizerTest --tests com.silomath.feature.authentication.AuthViewModelTest :app:compileProductionDebugKotlin :core:navigation:connectedDebugAndroidTest :core:designsystem:connectedDebugAndroidTest`

PASS. XML reports verified:12 navigation contract tests +3 scope-adapter tests +3 real Production-authorizer tests +8 existing AuthViewModel regressions +4 native platform tests +6 design-system regressions =36 PASS,0 failures/errors/skips. Production compilation PASS. Per-suite timestamps/report paths retained in IMP-UI-008-validation.json.

Native API37 tests use a test-APK-only ComponentActivity with the real NavDisplay/back stack: hardware/system Back restores Unit search/scroll/stable IDs; Activity recreation roundtrips serialized stack and entry state; real NavigationEventDispatcher receives predictive start/progress/cancel/commit and changes stack only on commit; actual system-bar icon appearance matches light surfaces. This is Activity recreation plus serialization coverage, not a claim of manual OS process-kill testing or completed Stocktaking workflows.

Initial new platform test compilation used the wrong positional constructor argument order for NavigationEvent. One targeted correction to named arguments passed; no speculative loop or frozen-rule change. Warnings are preexisting/deprecations; no warning suppression or unrelated refactor.

`engineering/validate-module-dependencies.ps1`: PASS19/19,65 project dependencies,0violations. Source search confirms no Production NavHost/rememberNavController/composable route declarations in app/core:navigation. `git diff --check`: PASS; cached check repeated at commit gate. No operational DB/schema/backend/Supabase modification, reset/checkout/delete, push or IDE staging.

## Remaining scope is owned elsewhere

Full43-component feature implementation and visual-floor comparison are not certified by navigation infrastructure. UI-009 owns Stitch comparison per explicit user clarification; MD-002 owns LOC-01 data/provider/forms/renderers. Existing unimplemented top-level feature boundaries remain unchanged. No GATE-M3/M5/M6, AUTH-010 or snapshotSchemaVersion resolution is inferred. Proceed automatically to MD-002 after this GREEN atomic local commit.

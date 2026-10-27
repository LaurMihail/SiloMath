# IMP-UI-007 — Canonical SiloMath Design System

Status: IMPLEMENTED / GREEN, bounded to canonical design-system ownership. GATE-M1 was audited DONE with PR7 evidence; merge5d2f75f was checked as an ancestor of the inspected HEAD. This is not satisfied through recent PREIMPLEMENTED components.

Authority: Doc01/02, Doc08 ownership, Doc21 sections3–14, Doc18 UI/accessibility requirements. No frozen document, business semantics or lifecycle changed.

## Implemented

- Exact18 Doc21 colors; a single explicit canonical Material3 palette. No wallpaper/dynamic-color or template-purple/dark-theme authority. Existing EnterpriseNavy/Steel public aliases resolve into approved tokens.
- Packaged unmodified Inter4.1 Regular/SemiBold/Bold with original OFL1.1 license, release provenance and SHA256 asset manifest. All12 frozen typography roles, line heights/weights and tabular-number style; offline resource loading.
- Spacing4/8/16/24/32/40dp;4/8/12dp shapes;1/2dp borders and restrained elevation primitives. Current-window classes under600,600–839,840+ with16/24/32dp margins; reusable safe-content/IME modifiers and48dp touch-target primitive. Optional motion respects platform animator enablement; no custom animation bypasses platform scaling.
- Reusable primary/secondary buttons, text/search fields with associated errors/read-only semantics, cards/lazy-list-compatible rows, text-bearing status/sync chips, selected filters, offline/error/empty presentation. Canonical generic adaptive bottom-bar/rail presentation and top app bar; typed destination ownership/Production shell migration belongs to UI-008.
- Provider-neutral map host, visible degradation/attribution/List path and labeled selected marker primitive. No authorization/filtering/geocoding/GPS/business persistence in design system; actual facility data/provider integration belongs to MD-002.
- Test-only LOC-01 component reference uses controlled fixture rows and exact labels; no fake Production records or premature LOC-01 acceptance.

## Executed validation

Final command after the last Production source change:
`.\gradlew.bat :core:designsystem:testDebugUnitTest :app:compileProductionDebugKotlin :core:designsystem:connectedDebugAndroidTest`
PASS.3 unit tests executed: text/semantic surface contrast, focus/action contrast and resize breakpoint/margin behavior.6 instrumented Compose tests PASS on API37 Pixel8 emulator,0 failures/errors/skips: map fallback+selected marker/List action, disabled/read-only/field error semantics,2x font size48dp controls, compact-to-rail resize selection/callback retention, explicit night configuration with unchanged palette/font and reference control/screenshot export. Production compilation PASS; final invocation reused its unchanged compilation output.

Earlier compilation failure was a missing @Composable annotation for WindowInsets.safeDrawing access; one targeted correction passed. Initial instrumentation failed before test bodies because Compose transitively resolved Espresso3.5.0 and API37 removed InputManager.getInstance. Declaring the already-pinned catalog espresso-core3.7.0 corrected it; no library version/catalog upgrade. A screenshot export initially used app-private storage removed during runner cleanup; the test now uses Gradle's additionalTestOutputDir, with the reference retained as deliberate QA evidence rather than committing build directories.

`engineering/validate-module-dependencies.ps1`: PASS,19/19 modules,0 violations. Corrected graph validation:321 nodes,607 original edges preserved +2 approved architectural edges =609,acyclic; original XLSX checksum unchanged. `git diff --check`: PASS before staging; cached check repeated at commit gate.

## Visual QA and gate ownership

Inspected the actual final1080x2400 component screenshot: canonical slate/white/cobalt hierarchy, packaged typography, primarySoft selected treatment, readable titles/rows and visible primary action; no clipping in captured compact reference. It is a design-component reference, not completed Production LOC-01 navigation/authorization acceptance.

Product Owner clarification: UI-007 validates against Doc21; lack of local Stitch artifact does not block this task. UI-009 owns comparison to Stitch project944090356426037338/screen380c1446ed4948f4b93e5e2447c6023f. engineering/ui-reference-registry.json records the supplied URL and comparison NOT PERFORMED. Doc06/21 override conflicts; no pixel-lock or baseline change is inferred. UI-009 remains NOT STARTED.

## Next boundary

Commit this GREEN task and proceed to UI-008, then MD-002 after both UI tasks are GREEN/committed. Approved dependency correction is separate from execution-order exceptions. AUTH/READY/M3 cycle and snapshotSchemaVersion UNRESOLVED remain untouched. Existing five IDE changes excluded. No push or Supabase mutation.

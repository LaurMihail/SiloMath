# IMP-MD-002 — pre-implementation architectural dependency audit

Date: 2026-10-27
Inspected HEAD: cb9bb9ebca2751317bd52a6729ede5109654ba8e
Status: BLOCKED_TECHNICAL / architectural prerequisite missing from audited execution graph.
Implementation status: NOT STARTED. No implementation source or baseline changed.

## Authority and task boundary

01_PRODUCT_BASELINE and 02_DECISION_REGISTER; Doc06 LOC-01 and exhaustive LOC01-01..06 controls, LOC-02 destination identity and P1-SH-01 entry; Doc07 Navigation3 architecture; Doc08 module ownership for core:navigation, core:designsystem and feature:masterdata; Doc21 sections8.14/9 and canonical semantic/brand tokens; Doc18 TA-D222 and related UI/adaptive/accessibility acceptance.

MD-001 is accepted DONE at checkpoint91227af. The audited workbook lists only IMP-MD-001 as the direct predecessor of MD-002. This syntactic eligibility does not supply the missing frozen UI infrastructure.

## Concrete blocker

The complete task requires a reachable Locations destination, persistent canonical shell navigation, stable-ID LOC-02/Add/Edit navigation, restoration/back behavior and Doc21 visual quality. The current composition root app/src/main/java/com/silomath/app/MainActivity.kt uses androidx.navigation.compose.NavHost/rememberNavController and string routes. Its locations destination is an empty Box. core/navigation contains build.gradle.kts and build outputs, but no src implementation or type-safe Navigation3 contracts; its dependency is the transitional navigation-compose library.

The approved owner/task for Navigation3 keys, stable-ID arguments, root navigation/restoration/predictive-back/platform behavior is IMP-UI-008. Silently implementing that infrastructure while closing MD-002 would execute another task outside the approved dependency order. Adding a new string-route screen instead would extend an architecture explicitly superseded by the frozen V6 contract. A detached Composable/test harness alone would not fulfill complete Production LOC-01 integration.

Related UI foundation deficiency: core/designsystem/src/main/java/com/silomath/core/designsystem/Theme.kt still defaults dynamicColor=true and uses template Purple/Pink tokens. Doc21 prohibits dynamic replacement of canonical semantic/brand colors and specifies the canonical design system. The complete foundation task is IMP-UI-007. MD-002 must not silently absorb that task or claim Doc21 visual acceptance against the transitional theme.

This is a prerequisite/ordering gap in the execution plan, not a contradiction between official documents and not a request to change frozen behavior. The previous reconciliation identified MD-002 as first executable based on the audited direct edge alone; that assessment was incomplete at the architecture boundary.

## Required execution decision

Approve recording IMP-UI-007 / IMP-UI-008 as the relevant missing UI architecture prerequisites of MD-002, preserving the original audited graph and its audit trail. Execute their own audited predecessors and close those tasks separately before completing LOC-01. This note does not add dependency edges, preimplement either task, authorize an exception, or mark any predecessor satisfied.

The AUTH-010/READY/M3 cycle and snapshotSchemaVersion remain untouched. No map provider selection, mock Production dataset or legacy navigation workaround was introduced.

## Verification in this task

Read git status --short, AGENTS.md, the documents and source files above, and the accepted reconciliation/backlog task contracts. Confirmed missing core:navigation source and existing transitional shell/theme by direct inspection. No Kotlin or resource changes, therefore no Gradle build, unit tests or instrumentation run; no new PASS claim for LOC-01.

Final git diff --check and git status --short are executed after writing this note. No green implementation commit is created for a blocked task. Existing five IDE working-tree changes are excluded and left unchanged. No push or Supabase mutation.

## Subsequent approved dependency correction

The Product Owner approved UI-007 and UI-008 as missing architectural prerequisites before MD-002. Classification: DEPENDENCY_CORRECTION, not an execution-order exception. See engineering/dependency-corrections/DC-2026-10-27-MD002-UI-foundation.json and the versioned reconciliation overlay. The ordering approval requested above is now RESOLVED; MD-002 remains pending until both UI tasks are GREEN and committed. No frozen behavior changed.

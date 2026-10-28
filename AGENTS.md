# SiloMath Agent Contract

## Authority
SiloMath V6 APPROVED / FROZEN is the current product authority.

Documents 01–21 under `baseline/` are the Master Baseline:

1. `01_PRODUCT_BASELINE` is the overall product authority.
2. `02_DECISION_REGISTER` is the canonical decision authority.
3. The relevant specialist document governs its domain when interpreted with Documents 01 and 02.
4. An APPROVED Change Request modifies only its explicit approved delta after controlled application/refreeze.
5. Implementation ADRs may select open technical details but cannot change frozen product semantics.

If baseline documents conflict, report the contradiction. Do not resolve it through implementation assumption, existing code, historical evidence, README content, task evidence or generated proposals.

An explicit request alone does not silently change frozen behavior. An intentional frozen change requires the Baseline Change / Change Request process, Product Owner approval and controlled application/refreeze. Do not reopen or reinterpret frozen decisions outside that process.

## Technology
SiloMath uses:

- Kotlin
- Jetpack Compose
- Clean Architecture
- MVVM + UDF
- StateFlow
- Room / SQLite with approved SQLCipher production protection
- Hilt
- Jetpack Navigation 3 with type-safe serializable destination keys
- Coroutines / Flow
- WorkManager
- Retrofit + OkHttp
- Kotlin / Ktor backend
- PostgreSQL
- DataStore only for non-operational preferences/session state

Room is the local operational and read authority for supported local-first/offline-first work. The backend is the authoritative enterprise/server source of truth, and server-owned operations remain server-authoritative. Local-first/offline-first behavior must preserve the package, authorization, transaction, synchronization, conflict and history invariants in Document 12.

## Execution Rules

- Perform only the explicitly requested task.
- Work on one subtask at a time.
- Stop after completing the requested subtask.
- Do not automatically continue to another phase or task.
- Inspect only files directly relevant to the current task.
- Do not recursively scan the entire project unless explicitly required.
- Do not reread all project documentation unless explicitly requested.
- Keep code changes minimal and localized.
- Do not refactor unrelated code.
- Do not invent product, UX, navigation, architecture, business-rule, schema, or calculation decisions.
- If required information is missing, report:

  `BLOCKED — NEEDS APPROVAL`

- Do not silently choose an undefined product behavior.

## Safety Rules

- Never use broad destructive shell commands.
- Never recursively delete folders unless explicitly approved.
- Never use wildcard deletion for project source files.
- Before moving or deleting files, identify the exact source and destination.
- Move files first, verify the destination, and only then remove the confirmed obsolete source.
- Never delete tests during package restructuring.
- Do not modify generated files unless required by the build system.

## Production Data Rules

- No fake production authentication.
- No hard-coded production credentials.
- No mock tokens.
- No fabricated API responses.
- No invented database IDs.
- No hard-coded domain/business data in production code.
- Mocks and fakes belong only in test/debug source sets unless explicitly approved.

## Dependency Rules

Do not upgrade or change:

- Gradle
- Android Gradle Plugin
- Kotlin
- SDK versions
- dependencies

unless explicitly requested.

## Error Handling

If a command, build, test, or code change fails:

1. Identify the exact cause.
2. Make at most one targeted correction attempt.
3. If the same problem remains, STOP.
4. Report the exact error and affected file.

Do not perform speculative multi-file repair loops.

## Validation Rules

Run only validation requested by the current task.

Do not repeatedly run Gradle Sync, build, tests, or lint without a concrete reason.

Validation results are valid only if they were executed after the final code modification.

Never claim a test, build, lint, or device check was executed unless it actually ran.

Never claim functionality is complete merely because the project compiles.

Use these statuses accurately:

- IMPLEMENTED
- PARTIAL
- NOT IMPLEMENTED
- NOT TESTED
- BLOCKED

## UI Rules

SiloMath screens and sheets already marked as frozen must not be redesigned or reinterpreted.

Do not create temporary user-visible placeholder screens for future modules.

Do not change approved labels, navigation structure, statuses, filters, business terminology, or screen hierarchy without explicit approval.

## Reporting

Keep final reports concise.

Report only:

1. files changed;
2. what was implemented;
3. validation commands actually executed;
4. results;
5. remaining issues.

Then STOP and wait for approval.

## Atomic Task Enforcement

When a prompt is marked TASK or specifies "only":

- The task scope is a hard boundary.
- Do not inspect, edit, test, refactor, or repair unrelated features.
- If an unrelated test/build failure is discovered, STOP and report it. Do not fix it.
- Never expand a targeted test into a full test suite unless explicitly requested.
- Never run Gradle Sync, assemble, lint, or instrumented tests unless explicitly requested.
- Execute at most the exact validation command requested by the prompt.
- If that command succeeds, STOP immediately.
- Do not perform additional validation "for confidence".

## File Inspection Budget

When a task explicitly lists allowed files:

- The listed files are a hard read boundary.
- Do not use recursive Get-ChildItem, grep, Select-String, repository-wide search, directory-wide file reads, or equivalent broad searches outside that list.
- Do not read every file in a package or directory to discover a contract.
- If one additional file is required, open that exact file only when its identity is already known.
- If the required file cannot be identified without a broad search, STOP and report:
  `BLOCKED — ADDITIONAL FILE DISCOVERY REQUIRED`
- Do not perform broad file discovery "for confidence" or to prove that a method does not exist.
## Reporting Accuracy

Final reports must describe ONLY the current task.

- Do not repeat changes from previous tasks.
- Do not report cumulative project state.
- Report the exact commands actually executed.
- Never reconstruct or combine several commands into one claimed command.
- Never claim lint, build, tests, device execution, or verification from an earlier task as a current-task result.
- Use the exact task identifier supplied by the prompt.

## Domain Independence

Do not infer relationships between independent business concepts.

For SiloMath specifically:
- Remember Me does NOT imply Offline Capability.
- Offline Capability does NOT imply Remember Me.
- Authentication persistence and offline eligibility must remain independent unless an approved specification explicitly links them.
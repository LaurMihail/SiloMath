# IMP-MD-004 — Storage Unit UI

Historical audit status: BLOCKED_TECHNICAL — missing architectural read prerequisite. Date: 2026-10-28.
Checkpoint: MD-003 DONE at 6db82d7f419af6d2908ed579ab99768d8daec85d. No MD-004 implementation or commit.

## Authority and audit

V6 APPROVED/FROZEN Docs01/02; Doc03 BR-AUTHZ-001/BR-MD-001; Doc04 Master Data authorization; Doc06 LOC-02, STU-02, P1-SH-02 exhaustive controls; Doc21 visual/adaptive/accessibility authority; Docs07/08 repository/module boundaries; Doc17 tenant protection; canonical MD-003 repository and current StorageSpaceRepository implementation. Frozen authority supersedes the old v1.0 label in AGENTS.md. Baseline and dependency graph are unchanged.

LOC-02 already has authorized Location/Unit binding and local search/lifecycle presentation, but its Add/Edit and STU-02 navigation remain fail-closed pending this task. MD-003 provides canonical Unit UUID/parent/metadata/local mutation+outbox behavior. These implemented parts must be reused, not reimplemented.

Doc06 STU02-05 requires the child StorageSpace read model, always visible, parent fixed; STU-02 displays actual authorized cached children and distinguishes content/empty/error. STU02-07 and STS-02/P1-SH-03 remain later Storage Space feature boundaries; no fabricated action or empty state is authorized.

## Exact blocker

app/src/main/java/com/silomath/core/data/repository/OfflineStorageSpaceRepository.kt observeStorageSpaces calls StorageDao.getStorageUnitByStorageUnitId, then getStorageSpacesByUnit(storageUnit.id). That lookup requires a numeric legacy parent and returns emptyList when it is absent. MD-003 deliberately writes/reads v6_storage_units and never fabricates a legacy numeric mirror. Therefore the current reader cannot certify completeness or tenant/parent scoping for the canonical hierarchy. Empty results from that missing legacy parent must not be shown as verified absence of children.

The feature may not call DAOs directly (Doc08), copy incomplete legacy projections, fabricate Production data, or repair Storage Space Repository/API under MD-004 (explicit task scope exclusion). A baseline error/empty visual state is not authorization to substitute a permanently unimplemented read source and claim this mandatory contract DONE.

## Dependency correction needed

The audited graph currently has MD-004 <- MD-001 + MD-003, MD-005 <- MD-001 + MD-004, MD-006 <- MD-001 + MD-005. Full completion of STU-02 additionally needs a canonical authorized local Storage Space read projection. Simply adding the whole MD-005 as a predecessor would create a cycle.

Recommended decision: explicitly approve a dependency correction that extracts only that minimal read prerequisite from MD-005 before MD-004. It must read existing canonical UUID rows through a domain repository/read contract, validate Organization -> Location -> Unit -> Space lineage and react to authorization changes, without Storage Space mutation/API/UI implementation. Keep the remaining MD-005/006 scope and audited edges/history intact. This proposal is not executed or treated as an execution-order exception.

## Verification and scope

Read-only audit of baseline, existing contracts/code and dependency TSV; git status --short before/after audit. No build/test command run: no implementation changed. No existing tests weakened. Only this blocker evidence file is added. Existing IDE changes are untouched. No changes to MD-003, baseline, graph, Supabase or other repositories; no commit/push. Do not mark MD-004 GREEN/DONE until the prerequisite decision and all requested implementation/test/build gates are satisfied.

## Approved correction / current status

2026-10-28: Product Owner explicitly approved the extracted read-only prerequisite. It is now GREEN in the separate atomic commit containing IMP-MD-004-read-prerequisite.md; see reconciliation/2026-10-28-MD004-storage-space-read-prerequisite/README.md. Original blocker and proposal above are retained as audit history. Current MD-004 status: IN PROGRESS, UI not yet accepted. MD-005 status is unchanged and is not declared Done or partially complete. Resume MD-004 after the prerequisite commit.

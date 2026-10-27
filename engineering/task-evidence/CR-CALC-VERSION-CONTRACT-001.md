# CR-CALC-VERSION-CONTRACT-001 implementation verification

Approval: explicit Product Owner message in this conversation, exact tuple stored in approved CR JSON and Doc13 section22.1. Separate attachment unavailable; no attachment provenance is fabricated.

Implemented: exact typed Production registry resolving executable F01-F10 handlers; canonical SHA-256 fingerprint metadata; stale detection; immutable frozen input capture and historical exact-tuple reproduction, unknown versions rejected without fallback. Mathematical kernel unchanged.

Executed `.\gradlew.bat :calculation-engine:test :calculation-engine:build :app:compileProductionDebugKotlin`: PASS, 58 tests, zero failures/errors/skips; Production compilation PASS. Final baseline/RVM validators PASS as listed below. The final repeated Gradle invocation was up-to-date; the 58 tests were actually executed after the last Kotlin code change in the preceding identical command.

RVM: FD-73 unit contract coverage verified against actual source hash; broader CALC requirements remain MAPPED/Not Run at integrated acceptance level even though their unit components pass. No CAM-008, CALC end-to-end or GATE-M5 completion is inferred.

No push, Supabase mutation, historical relabeling, mathematical change or READY activation. Synced sources and IDE files remain untouched.

## Final baseline verification

- `engineering/build-requirement-registry.ps1` and `engineering/build-rvm.ps1`: regenerated from the updated 21-document source and controlled mappings. 2187 source rows / 1840 unique IDs; RVM21 MAPPED /1 VERIFIED /1818 UNMAPPED. Only FD-73 is marked unit-contract verified by this change; milestones are not inferred from this count.
- Bundled Python `engineering/validate-calculation-version-contract.py --write-evidence`: PASS, 163 actual checks, zero failures. Checks exact typed tuple, unchanged formula codes, 21 source documents/revision/hash consistency, material cross-references and Doc13 correction. Mathematical sections3-19 and26-28 match the pre-change approved baseline byte-for-byte after canonical newline handling; their source commit/hash are recorded in the new manifest.
- `engineering/validate-baseline-change-process.ps1`: PASS (2 approved CR records).
- `engineering/validate-baseline-registry.ps1`: PASS,21/21 matching document hashes.
- `engineering/validate-requirement-registry.ps1`: PASS,20/21 documents with source IDs; only Doc21 lacks a source namespace. FD-73 now occurs in authoritative Doc01 and is included explicitly; range extended to FD-73, no source checks removed.
- `engineering/validate-rvm.ps1`, `engineering/validate-adr-process.ps1`, `engineering/validate-module-dependencies.ps1`: PASS.
- `git diff --check`: PASS.

Targeted governance corrections during validation: read the pre-existing UTF-8 BOM in rvm-mappings.json; preserve LF for baseline files as required by .gitattributes; handle Doc07's existing plain-text revision marker; update the validator's exact coverage expectation for the approved FD-73 reference in Doc01. An optional attempt to reuse Windows PowerShell5 formatting was blocked by the host script execution policy. No policy bypass occurred; the existing generator scripts ran successfully under the current PowerShell and regenerated JSON/CSV. Historical dated manifests/evidence were preserved.

## Canonical fingerprint implementation

For approved schema1/normalization1, UTF-8 JSON keys sort lexicographically recursively, nulls are explicit, normalized decimals are locale-independent unscaled decimal text with redundant trailing zeros removed, and version numbers remain JSON integers (engineVersion stays string2.0). Material contains frozen context/profile/family/formula identities and tuple, datum/origin/reference/max height, all fixed geometry parameters, required area provenance/constant-section flag, normalized ordered readings with sequence, entered density/unit and normalized density. SHA-256 yields lower-case hex. The independent Python-generated fixed reference material/hash is committed as a test oracle; Kotlin must match it byte-for-byte. Commodity/ownership/client identity are absent because they do not alter numerical semantics.

## Integration boundaries

The initial exact tuple is backed by the real Doc13 numerical handler for every family. Stored inputs/tuple are captured defensively, history resolves its stored handler, and unknown versions reject GEOM_VERSION_UNSUPPORTED without a latest-handler fallback. This does not claim support for any unapproved/unimplemented pre-2.0 historical engine.

CAM-008, snapshot-to-pinned-input binding, complete end-to-end CALC acceptance, client/server parity integration and GATE-M5 remain pending. The approval and this commit do not complete them. Continue audited CALC work next, then the required campaign integration before offline activation. The earlier metadata gap/aggregation contradiction is resolved by this APPROVED Baseline Change, not by an IADR.

## Subsequent numerical branch verification

CALC-022 shared-engine backend verification is implemented and locally verified in IMP-CALC-022-shared-engine-parity.md. CALC-021 additional boundaries/explicit Empty numerical branch are verified in IMP-CALC-021-boundary-properties.md:63 client-module tests and4 backend parity tests PASS after the latest shared source change. Existing controlled mapping source hashes now identify that exact tested source. Earlier58-test baseline-change results remain the historical initial verification record. CampaignSnapshot integration and GATE-M5 remain pending.

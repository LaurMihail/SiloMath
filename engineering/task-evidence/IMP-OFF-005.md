# IMP-OFF-005 — Package Integrity Verification

Status: IMPLEMENTED / LOCAL VERIFIED

Authority: 01 offline integrity; 02 D29; 09 manifest; 11 section 16; 12 section 15, OS-D28 and SYNC-T13/T14; 17 authenticated offline context.

Added authenticated staging decryption followed by exact byte length and whole-artifact SHA-256 verification, manifest/journal/envelope user+tenant+campaign+device+version identity binding, exact required collection inventory, per-collection count/hash checks, positive entity/immutable-version checks and matching pinned schema/engine metadata. Unsupported transport formats fail closed. This verifier proves integrity/completeness of the declared version; runtime engine compatibility and business-graph import remain separate guards.

Worker records verifiedAt only after successful verification and keeps VERIFYING until import/activation. Failure records PACKAGE_INTEGRITY_FAILED; the current manifest/package and local business data are untouched. No server READY/Campaign status is trusted.

Validation: `.\gradlew.bat :core:sync:testDebugUnitTest --tests com.silomath.core.sync.offline.OfflinePackageIntegrityTest --tests com.silomath.core.sync.offline.OfflinePackageDownloadTest :app:compileDevelopmentDebugKotlin` — PASS, 14 tests, zero failures; app compilation PASS. Integrity tests cover valid complete inventory, corruption/length, user mismatch, missing collection, count mismatch, pinned metadata mismatch and forbidden lifecycle skipping. Encryption tamper remains covered by OFF-004 regression.

`git diff --check`: PASS. No package activation/session eligibility change or external mutation.
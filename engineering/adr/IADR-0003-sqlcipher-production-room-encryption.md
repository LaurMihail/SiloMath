# IADR-0003 - SQLCipher Room Encryption and Forward-safe Protection Conversion

## Metadata
- ID: IADR-0003
- Status: ACCEPTED
- Owner: SiloMath
- Decision date: 2026-10-27
- Supersedes: None
- Superseded by: None

## Context
The operational Room database is ordinary SQLite, although package staging is encrypted. The user explicitly approved modern SQLCipher and the key, conversion, recovery, backup and test constraints before Production READY.

## Frozen baseline references
01_PRODUCT_BASELINE; 02_DECISION_REGISTER FD-69; 07_ARCHITECTURE; 10_DATABASE_SCHEMA section 18; 12_OFFLINE_SYNC_SPECIFICATION; 17_SECURITY_AUDIT_PRIVACY sections 7/7.1/15, SEC-AC07/SEC-T17/SEC-T33; 18_TEST_STRATEGY_ACCEPTANCE; IADR-0002.

## Decision
Pin maintained net.zetetic:sqlcipher-android 4.19.1. Retain Room/AGP/Kotlin/SDK versions. Use a preserving SQLCipher Room-2 open-helper adapter in application DI for every flavor; no environment/debug plaintext switch exists. Controlled migration tests can explicitly construct plain fixtures. Defer native loading, key I/O and physical conversion until Room background opening, not DI construction on the main thread. Replace destructive corruption defaults with sanitized recovery failures that retain files.

## Alternatives considered
- Legacy android-database-sqlcipher: rejected, not the approved maintained provider.
- Platform encryption alone: rejected, does not implement explicit Keystore-backed application/database protection.
- Field encryption or another operational database: rejected, would disrupt canonical queries/constraints or Room authority.
- Flavor-based plaintext switch: rejected, risks a Production bypass.

## Key management
A SecureRandom 256-bit secret is generated per installation/database, never from credentials. Wrap it with a distinct non-exportable Android Keystore AES-GCM key and authenticated package/database identity. AtomicFile stores only the authenticated envelope under noBackupFilesDir. Existing protected data with missing/corrupt key material must fail closed, never regenerate a key over data or create an empty replacement. Transient key arrays remain local to opening and are cleared on close; they are never logged, backed up, transmitted or included in diagnostics.

## Plaintext to encrypted conversion
Serialize preparation with a private cross-process lock. Checkpoint the source WAL and use an exclusive transaction before normal Room writers open. Export schema/data into a same-directory encrypted candidate using bound key parameters, retaining user_version and sqlite_sequence explicitly. Compare schema, row counts, row-content digests including legacy REAL bits and exact decimal TEXT, integrity checks and FK-check results before promotion. This is physical conversion, not business migration or reinterpretation.

Fsync the candidate and an authenticated verification proof. After closing conversion handles, promote by same-directory atomic rename and directory fsync. Retain every legacy/canonical table and all unresolved outboxes. Remove only obsolete empty sidecars and technical proof artifacts after safe closure. No destructive fallback or business-table contraction is allowed.

## Rollback and recovery
Before verification/promotion, keep the original authoritative and semantically unchanged. Incomplete candidates never become current. Resume only after key/proof/integrity/source-equivalence verification. Atomic promotion leaves either the original or the complete encrypted file; an authenticated proof permits completion after a crash. A protected initialized marker prevents recreating an established missing database. Missing keys, corrupt artifacts or failed verification preserve available files and require controlled recovery/re-authentication/re-download, without plaintext fallback or silent loss of unsynced data.

## Backup exclusions
Exclude database files/sidecars, files, shared preferences/auth credentials, protected stores, conversion artifacts and wrapped secrets from cloud backup/device transfer. Use noBackupFilesDir for key envelopes, locks, state and staging as additional protection. Test legacy backup XML and Android 12+ extraction resources.

## Test matrix
- Fresh encrypted database and real Keystore envelope reopen.
- Populated plaintext conversion with legacy, v6_*, sync_outbox and sync_operations; exact retained values/IDs/sequence.
- Correct key reopens; missing/wrong/corrupt keys fail without file replacement or deletion.
- Standard SQLite cannot read the resulting file; the plaintext SQLite header is absent.
- Interrupted conversion before/after promotion recovers without data loss.
- Executable authentic Room migration tests and explicitly approved reconstructed behavior tests pass. Authentic schemas 9-14 are unavailable: their unchanged historical MigrationTestHelper tests remain BLOCKED, not fully PASS. Reconstructed sources are non-authoritative and never exported as historical JSON. The user accepted this documented validation debt as non-blocking.
- Backup resource exclusions; Production/Development compilation; native page-size verification on the available 16KB emulator.

## Consequences
The native dependency and Keystore recovery boundary add storage/startup complexity. Conversion requires temporary space and exclusive access, but changes no logical data. Operational READY remains dependent on security, integrity, compatibility, current authorization and atomic activation.

## Baseline impact check
Changes frozen V6 baseline: NO

## Notes
Accepted by explicit user approval. Primary references: https://github.com/sqlcipher/sqlcipher-android and https://www.zetetic.net/sqlcipher/sqlcipher-api/ . No Supabase/external environment change is authorized or needed.

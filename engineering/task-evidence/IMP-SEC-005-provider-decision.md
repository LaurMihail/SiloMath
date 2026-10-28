# IMP-SEC-005 — Production Room encryption decision required before READY

Status: BLOCKED_TECHNICAL / security architecture decision required.
This is a proposal for IADR-0003, not an accepted decision or an implemented security control.

## Verified current state

- IADR-0002 is ACCEPTED and implemented through the schema-26 EXPAND checkpoint (62c7aa0) and verified canonical staging (adffbeb).
- DatabaseModule.provideDatabase still builds silomath_db with the default SQLite open helper. No encrypted SQLite factory/driver, database passphrase provider or plaintext-to-encrypted conversion exists in core:database/app. The version catalog and accepted IADRs contain no database-encryption provider.
- Package staging already uses AES-GCM/Android Keystore in noBackupFilesDir. That protects the transport artifact, not the operational Room database.
- Existing backup XML excludes credential preferences, not the full protected database/offline/key hierarchy.
- Backlog DEPENDENCY_AUDITED identifies IMP-SEC-005 (Production Room Encryption, Not Started, depends on IMP-DB-010) and IMP-SEC-007 (Backup Exclusions). They are real prerequisites for Production operation, even though placed later in the execution list.

## Frozen requirements

17_SECURITY_AUDIT_PRIVACY sections 7/7.1/15, SEC-AC07 and SEC-T17: Production Room/offline business data must be encrypted with a random installation database secret protected through Android Keystore. Secrets/keys/protected stores are excluded from backup; key failure must not cause a plaintext bypass or deletion of recoverable unsynced work. IADR-0002 explicitly preserves these requirements before operational READY. Documents 10/12 require forward-safe preservation, exact data and atomic activation. User stop condition: an architectural decision absent from the approved baseline must be reported.

## Concrete proposed decision

1. Use the maintained Community `net.zetetic:sqlcipher-android` provider through Room 2 SupportOpenHelperFactory. Retain the existing Room/AGP/Kotlin/SDK versions; pin and verify a compatible provider artifact before merge rather than performing unrelated upgrades.
2. Generate a random 256-bit per-installation database secret, wrapped with a distinct Android Keystore AES-GCM key. Store only its protected envelope in a no-backup private location. Do not derive it from password, token or package contents; never transmit or log it.
3. Convert an existing plaintext database through a private encrypted temporary copy while normal database writers are stopped. Verify schema/user_version, all retained canonical and legacy tables, stable identities, row counts, FK/integrity checks, exact decimal values and unresolved outboxes before an atomic database-file transition. Preserve a protected recovery path. Conversion changes protection, not logical data; no legacy-table CONTRACT or fabricated backfill is permitted.
4. Fail closed when key material/conversion/protection verification fails. Preserve recoverable data; never silently open a plaintext database in Production or clear pending work.
5. Exclude database files, sidecars, wrapped keys, protected package/Evidence stores and conversion/recovery artifacts from platform backup/transfer. Do not leave a permanent plaintext backup after a successful protected transition.
6. Only after protection, integrity, compatibility, current authorization and atomic activation pass may Production READY/Continue Offline become available.

This proposal would add a native security dependency and an installation-wide data-protection conversion. It is separate from the IADR-0002 schema migration approval. No dependency, installed application database, key store or Production environment has been changed for this proposal.

## Required acceptance verification

- Encrypted database cannot be read as ordinary SQLite; wrong/missing keys fail closed.
- Populated plaintext→encrypted conversion retains every table, UUID, legacy value and unsynced operation without reinterpretation.
- Interrupted conversion/restart recovers safely; no destructive fallback or silently lost work.
- Android API26+ and supported native ABI/page-size coverage, including the available 16KB-page emulator.
- Existing Room schema migrations and canonical package validation remain green.
- Backup/restore exclusions and Keystore loss behavior satisfy Document 17.
- OFF-007/008 atomic activation and AUTH-010 are verified after the prerequisite is satisfied.

## Primary implementation documentation consulted

- https://github.com/sqlcipher/sqlcipher-android — maintained Android provider and Room 2 integration through SupportOpenHelperFactory.
- https://www.zetetic.net/sqlcipher/sqlcipher-api/ — sqlcipher_export copies schema/data/triggers to an attached encrypted database; user_version must be preserved explicitly. rekey does not encrypt an ordinary plaintext SQLite database.

No push, external deployment, plaintext database replacement or deletion has occurred in this session. The disposable PostgreSQL test cluster was stopped after successful tests.
## Resolution - 2026-10-27
The user explicitly approved modern SQLCipher and the complete security/conversion/test constraints. IADR-0003 is ACCEPTED; implementation is resumed. The original report remains historical evidence.

# IMP-OFF-004 — Package Download Worker

Status: IMPLEMENTED / LOCAL VERIFIED

Authority: 01 local-first; 02 D29/FD-45; 08 core:sync ownership; 11 section 16/auth/error contract; 12 OS-D02/13–17/25–28 and sections 13–16; 17 sections 5/7/15.

Added explicit bearer-authenticated manifest/binary transport, WorkManager unique download scheduling, current-user/authorized-organization checks, one secure refresh attempt for 401, bounded jittered backoff and Retry-After precedence. Delayed retries are durably enqueued before the current attempt ends. Interrupted work resumes with its stored manifest/version; superseded 404 versions require a fresh manifest. Tokens are never WorkManager inputs or journal values.

Download writes a per-version AES-256-GCM staging file under noBackupFilesDir, using an Android Keystore-protected key. Identity/length checks and durable progress lead only to VERIFYING. Key loss/decryption errors never fall back to plaintext. Failed/cancelled attempts leave recoverable technical state and never replace an active package. App composition now uses HiltWorkerFactory. Existing catalog artifacts were scoped into core:sync without version upgrades; core:domain dependency is allowed by Doc08.

Validation: `.\gradlew.bat :core:sync:testDebugUnitTest --tests com.silomath.core.sync.offline.OfflinePackageDownloadTest :app:compileDevelopmentDebugKotlin` — final PASS, seven unit tests, zero failures/skips, app Hilt/Kotlin compilation PASS. Tests cover encrypted round trip/tamper, truncated transfer, user binding, cancellation, same-version retry and Retry-After/backoff. Initial classpath error was fixed by declaring the existing Room dependency in core:sync; initial JUnit return-type error was fixed to Unit. Final rerun follows the durability fix to await WorkManager enqueue persistence.

`git diff --check`: PASS. No eligibility/READY activation, UI changes, version upgrades, external deployment or push. Complete integrity/import/activation remain OFF-005..008.
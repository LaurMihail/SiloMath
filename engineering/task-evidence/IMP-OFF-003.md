# IMP-OFF-003 — Local Package State Model

Status: IMPLEMENTED / LOCAL VERIFIED

Authority: 01 offline working set; 02 D29; 09 manifest identity; 10 Room persistence; 12 section 14/OS-D25–28; 17 offline session authority.

Added the exact six frozen technical package states, canonical per-user/campaign/device/version identity, guarded download/verification/failure/retry progress and explicit verification/activation metadata requirements for READY. No session eligibility is inferred from these states. Failed refresh cannot demote a READY version. Added per-version staging/recovery metadata and DAO in Room; additive schema 24→25 retains existing data and the current-manifest selector. Operational business tables and outbox are unchanged.

Validation:
- `.\gradlew.bat :core:model:testDebugUnitTest --tests com.silomath.core.model.OfflinePackageStateTest :core:database:compileDebugAndroidTestKotlin`: model tests PASS; initial new instrumentation test did not compile because RoomDatabase is not Closeable. Corrected only the test to close explicitly in finally.
- `.\gradlew.bat :core:model:testDebugUnitTest --tests com.silomath.core.model.OfflinePackageStateTest :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.OfflinePackageMigration24To25Test'`: compilation PASS, six model tests PASS; device execution failed because the previously connected emulator disappeared.
- Started the existing Pixel_8 AVD hidden, waited for boot, then ran `.\gradlew.bat :core:database:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.silomath.core.database.OfflinePackageMigration24To25Test'`: PASS on Pixel_8 Android 17/API37, including retained business-data and new staging-table assertions.
- `git diff --check`: PASS.

Schema 25.json is the required generated Room migration contract, not a temporary build output. No Production/external environment changes or push.
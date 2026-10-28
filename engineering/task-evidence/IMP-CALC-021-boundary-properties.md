# IMP-CALC-021 - independent boundary/property verification

Numerical component IMPLEMENTED / LOCAL VERIFIED. Doc13 CE-B19/CE-D10 explicit Mark Empty numerical zeros are a separate result type and API without readings, density, invented geometry/version provenance or Validated status. The authorized Stocktaking workflow/version/activation checks remain outside this pure numerical helper; no protected action is granted.

Added deterministic seeded all-family height/capacity/monotonicity checks, exact repeated-reading properties for every allowed count1..5, invalid-parameter cases across all ten families and UTF-8/escaped canonical-fingerprint roundtrip checks. Existing golden, exact-precision, runtime-registry, fingerprint/stale and history tests remain green. No frozen mathematics or baseline document changed in this subtask.

Final after source changes:
- `.\gradlew.bat :calculation-engine:test :calculation-engine:build :app:compileProductionDebugKotlin`: PASS,63 executed tests,0 failures/errors/skips; module build and Production compilation PASS. Final repeated build may reuse compilation outputs; the test task actually executed after the last test-source edit.
- `.\gradlew.bat -p backend :test --tests com.silomath.backend.calculation.CalculationParityTest`: PASS again,4 executed tests,0 failures/errors/skips against the updated common module.
- `engineering/validate-calculation-version-contract.py`: PASS,163 checks, no baseline drift.
- `engineering/validate-rvm.ps1`: PASS after refreshing the calculation source hash in existing controlled mappings; integration-level Not Run rows remain pending rather than being silently upgraded to full workflow PASS.
- `git diff --check`: PASS.

No push, Supabase mutation, production snapshot substitution or GATE-M5 closure. The next required integration boundary is still CAM-008.

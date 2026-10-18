# IMP-GOV-006 repository patch

Generated against the supplied `SiloMath-main.zip` and the 2026-10-17 APPROVED/FROZEN refreeze.

## Result

- Canonical documents: 21/21 Markdown files, exact manifest hashes.
- Requirement registry: 2174 source rows / 1838 unique IDs.
- RVM: 1838 rows; 15 mapped; 0 verified; 1823 unmapped.
- Existing `engineering/rvm-mappings.json`: preserved unchanged (15 rows).
- Existing `build-rvm.ps1` and `validate-rvm.ps1`: preserved unchanged.
- CI status: PENDING; do not mark IMP-GOV-006 Done until Governance Validators passes on the repository.

## Apply

Copy `baseline/`, `governance_evidence/`, and the included `engineering/` files to repository root, preserving paths. Do not delete historical `.docx` files; they simply cease to be the registered current authority because `engineering/baseline-registry.json` points to the refrozen Markdown set.

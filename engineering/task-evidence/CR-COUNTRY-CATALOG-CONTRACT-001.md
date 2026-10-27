# CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74 — approved refreeze

Date: 2026-10-27. Authority: explicit Product Owner approval in this conversation. Status: APPROVED / FROZEN. Existing namespaces extended with FD-74, BR-MD-014 and TA-D233; no new ID scheme.

## Scope

Complete ISO 3166-1 catalogue; persisted/API uppercase alpha-2; names are presentation labels; no free-text new values; fail-closed membership validation; complete options locally/cached. Existing optional/null semantics unchanged. Historical non-conforming values preserved unchanged pending separate audit before migration. This refreeze implements documentation and governance only: it does not certify runtime Country validation or authorize migration.

Material contract/cross-reference changes: documents 01, 02, 03, 06, 09, 10, 11, 12, 16, 18, 19, 20. Other nine documents change only the shared revision marker. All 21 hashes/byte counts/line counts refreshed. Both Organization and Location UI/data contracts aligned. Previous manifest and validation evidence retained byte-identical. CALC mathematical hash remains 1dd2c2d4671d208487541a1c07d8cfae72cd0c81c21d0bf21ce779428925f53c. snapshotSchemaVersion remains unresolved and is not equated with inputSchemaVersion.

The requirement extractor previously capped decisions at FD-73. Its regex and declared range were extended narrowly to FD-74; the validation range was updated accordingly. RVM mappings were not fabricated: new runtime requirements remain unverified/unmapped. The CALC validator now follows the current registered revision and writes its own contract-specific evidence without overwriting Country evidence; mathematical checks are unchanged.

## Executed verification

- validate-country-contract.py --write-evidence: PASS, 186 checks, zero failures.
- validate-calculation-version-contract.py --write-evidence: PASS, 163 checks, zero failures.
- build-requirement-registry.ps1: PASS; 2202 source rows / 1843 unique IDs.
- build-rvm.ps1 and validate-rvm.ps1: PASS; 21 mapped / 1 verified / 1821 unmapped.
- validate-requirement-registry.ps1: PASS; 21 documents, 20 with source namespaces; Doc21 intentionally has no source-ID namespace.
- validate-baseline-change-process.ps1: PASS; three approved CRs.
- validate-baseline-registry.ps1: PASS; 21/21 documents and registered hashes.
- validate-adr-process.ps1: PASS; three implementation ADRs, no new IADR.
- git diff --check: PASS. Existing line-ending conversion warnings are non-blocking.
- git diff --exit-code for the previous frozen manifest and refreeze report: PASS (unchanged).

No application code or database change belongs to this commit, so no Android build/runtime test is claimed for this documentation-only refreeze. In-progress MD-002 source edits and existing IDE configuration edits are excluded. No push or Supabase mutation.

## Exact commit file scope

- `baseline/01_PRODUCT_BASELINE.md`
- `baseline/02_DECISION_REGISTER.md`
- `baseline/03_BUSINESS_RULES.md`
- `baseline/04_USER_ROLES_PERMISSIONS.md`
- `baseline/05_WORKFLOWS_STATE_MACHINES.md`
- `baseline/06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION.md`
- `baseline/07_ARCHITECTURE.md`
- `baseline/08_MODULE_ARCHITECTURE.md`
- `baseline/09_DATA_MODEL.md`
- `baseline/10_DATABASE_SCHEMA.md`
- `baseline/11_API_SPECIFICATION.md`
- `baseline/12_OFFLINE_SYNC_SPECIFICATION.md`
- `baseline/13_CALCULATION_ENGINE_SPECIFICATION.md`
- `baseline/14_COMMODITY_SPECIFICATION.md`
- `baseline/15_EVIDENCE_STORAGE_SPECIFICATION.md`
- `baseline/16_REPORTING_SPECIFICATION.md`
- `baseline/17_SECURITY_AUDIT_PRIVACY.md`
- `baseline/18_TEST_STRATEGY_ACCEPTANCE.md`
- `baseline/19_RETENTION_POLICY.md`
- `baseline/20_PRODUCTION_OPERATIONS.md`
- `baseline/21_UI_DESIGN_SYSTEM_FINAL.md`
- `engineering/baseline-change/CR-COUNTRY-CATALOG-CONTRACT-001.md`
- `engineering/baseline-change/CR-COUNTRY-CATALOG-CONTRACT-001.json`
- `engineering/baseline-clarifications/COUNTRY-CATALOG-PROPOSAL.md`
- `engineering/baseline-change-register.json`
- `engineering/baseline-registry.json`
- `engineering/build-requirement-registry.ps1`
- `engineering/validate-requirement-registry.ps1`
- `engineering/validate-calculation-version-contract.py`
- `engineering/validate-country-contract.py`
- `engineering/requirement-id-registry.json`
- `engineering/requirement-id-registry.csv`
- `engineering/rvm.json`
- `engineering/rvm.csv`
- `governance_evidence/SiloMath_V6_FROZEN_MANIFEST_2026-10-27_COUNTRY.json`
- `governance_evidence/SiloMath_V6_Refreeze_Validation_2026-10-27_COUNTRY.json`
- `governance_evidence/SiloMath_V6_Calculation_Validation_2026-10-27_COUNTRY.json`
- `engineering/task-evidence/CR-COUNTRY-CATALOG-CONTRACT-001.md`

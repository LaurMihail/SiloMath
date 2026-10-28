# SiloMath

SiloMath is a native Android bulk-inventory and physical-stocktaking system implemented against the **SiloMath V6 APPROVED / FROZEN** product contract.

## Product authority

The 21 Markdown documents in `baseline/` are the Master Baseline:

1. `01_PRODUCT_BASELINE.md` — overall product authority;
2. `02_DECISION_REGISTER.md` — canonical approved decisions;
3. Documents 03–21 — specialist authority within their assigned domains, interpreted with Documents 01 and 02.

Approved frozen changes are governed through `engineering/baseline-change/` and `engineering/baseline-change-register.json`. Repository implementation decisions that do not change frozen semantics are recorded under `engineering/adr/` and `engineering/adr-register.json`. Historical manifests and refreeze evidence are retained under `governance_evidence/`.

This README is an orientation document and does not override the Master Baseline.

## Repository structure

- `app/` — Android application composition and integration.
- `core/` — shared model, domain, persistence, network, sync, navigation, design-system and testing modules.
- `feature/` — feature modules.
- `calculation-engine/` — isolated deterministic calculation boundary.
- `backend/` — Kotlin/Ktor backend.
- `supabase/migrations/` — versioned PostgreSQL schema migrations.
- `engineering/` — governance registers, RVM, validators and task evidence.

## Build and validation entry points

Android and JVM tasks use the Gradle wrapper at repository root. Backend tasks use `gradlew -p backend`.

Governance entry points include:

- `engineering/validate-baseline-registry.ps1`
- `engineering/validate-baseline-change-process.ps1`
- `engineering/validate-adr-process.ps1`
- `engineering/validate-requirement-registry.ps1`
- `engineering/validate-rvm.ps1`
- `engineering/validate-module-dependencies.ps1`
- `engineering/validate-postgres-v6-contract.ps1`

Run only the gates required by the active task and record only results actually executed. Do not mutate live Production or Supabase environments through documentation or validation workflows.

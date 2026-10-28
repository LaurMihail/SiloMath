# IMP-OFF-006 — Staged Import: required Room migration decision

Classification: BLOCKED_TECHNICAL / architecture decision required.
AUTH-010 remains BLOCKED_TECHNICAL. OFF-001..005 are locally committed and verified. No offline eligibility is enabled.

## Verified implementation gap

The current Room schema is not capable of representing all authoritative rows of the frozen campaign package:

- GeometryFamilyEntity has legacy integer identity/name fields; GeometryVersionEntity stores fixed_parameters/required_dimensions_schema as generic JSON and has no canonical immutable GeometryProfileVersion structure.
- StocktakingRecordEntity has legacy integer references and status/ownership projection, without canonical stocktaking_record_id, organization scope or current_revision_id.
- MeasurementEntity stores Double value and a root-record reference, without canonical measurement/revision identity, sequence, raw unit, normalized ullage, datum metadata or pinned geometry version.
- CalculationResultEntity stores Double volume/mass and generic metadata, without the explicit canonical revision/version/fingerprint/decimal structure.
- Existing DAO/repository contracts still use these legacy identities. SyncOutboxEntity can retain unresolved local legacy work.

Adding package rows through those types would either discard required frozen facts, route exact decimals through floating point, invent historical context, or make a side artifact the operational source of truth. None is acceptable.

## Authority

- 01_PRODUCT_BASELINE: local-first, deterministic/pinned context, durable unsynced work.
- 02_DECISION_REGISTER: D29/D30 and FD-69.
- 07_ARCHITECTURE: Room source of truth; AR-NFR-02/04/09/10.
- 09_DATA_MODEL: canonical GeometryProfileVersion, StocktakingRecord/Revision, Measurement and CalculationResult aggregates.
- 10_DATABASE_SCHEMA: sections 3/6/7/18, especially explicit geometry parameters, decimal TEXT rather than REAL, revision identities, forward-only tested migration, no historical reinterpretation or conversion through floating point.
- 12_OFFLINE_SYNC_SPECIFICATION: sections 13–16 and OS-D21–29.
- 17_SECURITY_AUDIT_PRIVACY: production Room/offline encryption remains a mandatory prerequisite before operational activation.
- engineering/adr/README.md: cross-cutting migration choices not determined by the baseline require IADR; only ACCEPTED IADRs are authoritative. The registry contains only IADR-0001 (Ktor), no accepted Room operational migration decision.
- Existing server precedent 20261015002000_v6_stage_legacy_snapshot_provenance.sql explicitly withholds canonical migration where exact historical versions/provenance are unavailable; versions are never synthesized.

## Concrete proposed decision for review (not accepted)

Recommended: a forward-only canonical Room migration in the same database, with explicit UUID/revision/decimal tables matching Document 10. Preserve incompatible legacy tables and pending legacy outbox data without reinterpreting them; keep them outside package readiness until authoritative reconciliation is possible. Update affected DAO/repository adapters through an explicit compatibility boundary. Populate canonical business rows only from verified authorized source data; never recover missing decimal/version facts by guessing or Double conversion. Record the exact table/adapter transition in IADR-0002 before implementation. No destructive fallback, silent legacy merge or new product state is authorized.

Alternative: expand the existing operational tables in place with canonical columns, preserving legacy columns and adapters until contraction. This avoids parallel tables but requires explicit nullability/FK/unique-key transitions and guards preventing legacy writers from invalidating canonical revision/version invariants.

The unresolved choice is the cross-cutting legacy/canonical authority and compatibility boundary, not the frozen V6 field definitions. Baseline conformity remains mandatory for either approach. This is an implementation architecture decision, not a request to change frozen product behavior.

## Required verification after a decision

- Representative populated schema 25 migration: stable identities, row counts, orphan/tenant/uniqueness checks and retained unsynced work.
- Exact decimal and pinned version/fingerprint preservation; ambiguous legacy facts remain unconverted.
- Negative immutable revision/version tests and canonical DAO/repository tests.
- Verified staged graph import does not change current data before activation; failed import/refresh preserves prior package and dirty work.
- Production encryption/backup controls before enabling READY/Continue Offline.

## Current checkpoint

977fa02 — OFF-005 integrity verification.
This document is a reviewable blocker report, not an accepted IADR or an implemented migration. No OFF-006 code/schema change has been made.
## Resolution � 2026-10-27

The user accepted IADR-0002 and EXPAND -> MIGRATE/RECONCILE -> CONTRACT. The architecture blocker above is resolved. This report preserves the prior findings; implementation is resumed with canonical schema EXPAND in the same Room database, no legacy contraction and no READY before the full frozen activation gates.

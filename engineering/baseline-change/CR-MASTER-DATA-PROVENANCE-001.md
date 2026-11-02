# Change Request: CR-MASTER-DATA-PROVENANCE-001

## Governance metadata

- CR ID: CR-MASTER-DATA-PROVENANCE-001
- Title: Platform governance provenance actor for global master-data publication
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES
- Owner: Laur
- Requested by: Laur
- Created date: 2026-10-31

## Approval

- Decision: APPROVED
- Decision date: 2026-10-31
- Approved / rejected by: Laur
- Superseded by: None

Only an APPROVED Change Request authorizes the explicitly approved frozen
baseline delta.

## Affected frozen baseline

### Documents

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER
- 09_DATA_MODEL
- 10_DATABASE_SCHEMA
- 14_COMMODITY_SPECIFICATION
- 17_SECURITY_AUDIT_PRIVACY
- 20_PRODUCTION_OPERATIONS

### Requirement / Decision IDs

- Commodity governance and creation provenance in 14_COMMODITY_SPECIFICATION §4.
- Commodity / CommodityVersion persistence contract in 09_DATA_MODEL and
  10_DATABASE_SCHEMA.
- Platform-governed reference-data authority remains unchanged.
- A new Decision Register entry is required for this approved clarification.

Where the affected baseline text has no formal Requirement / Decision ID,
identify the exact document and section instead.

## Current frozen contract

Commodity and CommodityVersion are global platform-governed reference data.

The frozen Commodity contract requires creation/governance provenance.
The physical V6 database schema requires:
- silomath.commodities.created_by UUID NOT NULL;
- silomath.commodity_versions.created_by UUID NOT NULL.

The current frozen baseline does not define the stable platform-governance UUID
to use when the authoritative creator is the platform rather than an
authenticated organization user.

The current schema does not require those two created_by values to reference a
silomath.users row.

## Requested delta

Freeze the following platform-governance provenance identity:

PLATFORM_GOVERNANCE_ACTOR_ID =
`00000000-0000-0000-0000-000000000001`

Contract:
- it is a stable technical provenance identity for platform-governed
  master-data publication;
- it is not an authenticated application user;
- it is not an organization member;
- it does not imply an entry in silomath.users;
- it may populate created_by (and equivalent platform-governance provenance
  fields where the frozen contract explicitly permits platform-governed
  creation) for global platform master data;
- it must not be used to impersonate user-authoritative actions;
- audit events continue to use their own actor/system_actor contract.

The initial canonical Commodity and CommodityVersion seed is explicitly within
this approved use.

## Reason

The frozen product requires platform-governed Commodity master data and
traceable creation provenance, while the database requires a non-null UUID.
Without a frozen platform provenance identity, the seed would have to invent a
UUID, attribute publication to an unrelated application user, or weaken the
non-null provenance contract.

A stable reserved provenance identity closes that gap without fabricating a
user or tenant relationship.

## Alternatives considered

1. Create a synthetic silomath.users row.
   - Rejected because platform governance is not an authenticated person and
     the current created_by columns do not require a user FK.

2. Use an arbitrary existing user UUID.
   - Rejected because it would create false provenance.

3. Make created_by nullable.
   - Rejected because it weakens the frozen traceability requirement.

4. Generate a random UUID per seed/deployment.
   - Rejected because provenance identity must be stable and auditable.

5. Reuse audit_events.system_actor.
   - Rejected because system_actor is a separate textual audit contract and is
     not the UUID provenance identity required by these master-data tables.

## Impact analysis

### Business rules / lifecycle / workflows

- No Commodity lifecycle or selection-rule change.
- Platform governance authority remains unchanged.

### UI / navigation / design

- No UI or navigation change.

### Architecture / module boundaries

- No module-boundary change.
- The reserved actor is provenance data, not an authentication principal.

### Data model / database

- `00000000-0000-0000-0000-000000000001` is reserved as
  PLATFORM_GOVERNANCE_ACTOR_ID.
- Canonical platform-governed Commodity and CommodityVersion seed rows may use
  this UUID in created_by.
- No silomath.users row is required solely for this identity under the current
  schema.
- No authorization rule may infer user privileges from this UUID.

### API / backend contract

- No new end-user API authority is introduced.
- Server/master-data publication code may emit the reserved provenance actor
  only for explicitly platform-governed data.

### Offline / synchronization

- Offline packages may carry this provenance value when they include governed
  master-data creation metadata.
- It is informational provenance, not an offline authentication identity.

### Calculation engine

- No calculation-engine change.

### Evidence / reporting

- Governance/audit reporting may distinguish platform-published master data by
  this stable provenance value.
- Existing audit_events actor semantics remain separate.

### Security / privacy

- The reserved UUID is not a credential, session identity or authorization
  principal.
- It must never grant access, role membership or tenant ownership.
- No user record is fabricated for the reserved UUID.

### Retention / production operations

- Production seed and future governed master-data publication must use the same
  frozen provenance UUID where this contract applies.
- Operational documentation must identify the value as reserved platform
  provenance.

## RVM impact

Add or update RVM coverage to verify:
- canonical platform-governed Commodity and CommodityVersion rows use the
  frozen provenance actor;
- the reserved UUID does not require or fabricate a silomath.users row;
- authorization/RLS logic does not treat the provenance UUID as a user or
  tenant principal;
- audit_events.system_actor remains a separate contract.

## Test impact

Required:
- seed assertion for created_by on canonical Commodity rows;
- seed assertion for created_by on canonical CommodityVersion rows;
- schema/regression test proving no FK to silomath.users is required for these
  columns under the approved physical contract;
- security regression ensuring the reserved UUID has no user/organization
  authorization effect.

## Migration and compatibility impact

The canonical initial Commodity/CommodityVersion seed may populate created_by
with PLATFORM_GOVERNANCE_ACTOR_ID.

No synthetic application user is created.

No existing user, membership or tenant row is modified.

If a future schema adds a user FK to these provenance columns, that change
requires a separate approved baseline/schema decision; this CR does not
authorize one.

## Release-gate impact

Before Production:
- baseline documents and Decision Register updated;
- canonical seed uses the approved provenance UUID;
- seed provenance assertions pass;
- security/RLS regression remains GREEN;
- CI and migration gates remain GREEN.

## Approved delta

APPROVED on 2026-10-31:

`PLATFORM_GOVERNANCE_ACTOR_ID` is frozen as:

`00000000-0000-0000-0000-000000000001`

It is the stable non-user, non-tenant provenance identity for explicitly
platform-governed master-data publication where the V6 persistence contract
requires a UUID creator/provenance value and does not require an authenticated
user FK.

The canonical initial Commodity and CommodityVersion seed is an approved use.

This CR does not create an authenticated user, organization member, role,
authorization bypass, audit-event actor replacement or new customer-editable
master-data capability.

## Implementation verification

Complete when the approved change is applied.

- Baseline documentation updated: PENDING
- Requirement registry updated: PENDING
- RVM updated: PENDING
- Affected tests updated: PENDING
- Applicable validators PASS: PENDING
- CI PASS: PENDING
- Implementation commit / PR: PENDING
- Release evidence: PENDING

## Authority guard

This Change Request MUST NOT be treated as authority while its status is
PROPOSED or REJECTED.

Only the explicitly APPROVED delta may alter the frozen V6 contract.

All unaffected requirements in Documents 01–21 remain authoritative.

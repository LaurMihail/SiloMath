# Change Request: CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001

## Governance metadata

- CR ID: CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001
- Title: Define canonical physical mapping for CampaignSnapshot commodity compatibility
- Status: APPROVED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES
- Owner: SiloMath
- Requested by: Product / Implementation
- Created date: 2026-10-20

## Approval

- Decision: APPROVED
- Decision date: 2026-10-20
- Approved / rejected by: Product Owner
- Superseded by:

Only an APPROVED Change Request authorizes the explicitly approved frozen
baseline delta.

## Affected frozen baseline

### Documents

- 01_PRODUCT_BASELINE
- 02_DECISION_REGISTER
- 09_DATA_MODEL
- 10_DATABASE_SCHEMA
- 11_API_SPECIFICATION
- 12_OFFLINE_SYNC_SPECIFICATION
- 14_COMMODITY_SPECIFICATION
- 18_TEST_STRATEGY_ACCEPTANCE

### Requirement / Decision IDs

- 09_DATA_MODEL  -  CampaignSnapshot / commodityCompatibilitySnapshot
- 09_DATA_MODEL  -  DM-D18
- 10_DATABASE_SCHEMA  -  DB-D43
- 14_COMMODITY_SPECIFICATION  -  Campaign and snapshot behavior
- 14_COMMODITY_SPECIFICATION  -  CO-D14
- 14_COMMODITY_SPECIFICATION  -  CO-D28

## Current frozen contract

The frozen logical data model defines
CampaignSnapshot.commodityCompatibilitySnapshot as structured frozen
Commodity compatibility context containing relevant Commodity identity and
version data.

CampaignSnapshot is an aggregate whose N-valued frozen collections are
represented as explicit child collections/records rather than one opaque
JSON business object.

The frozen physical database contract defines CampaignSnapshot persistence
as a header plus explicit child snapshot tables for N-valued collections.

However, the canonical physical table inventory explicitly names only:

- campaign_snapshot_scope_items
- campaign_snapshot_team_items

No canonical PostgreSQL/Room physical mapping is defined for the
N-valued commodityCompatibilitySnapshot collection.

Implementation therefore cannot select a physical representation without
introducing structure not explicitly defined by the frozen physical
database contract.

## Requested delta

Define the canonical physical representation of
CampaignSnapshot.commodityCompatibilitySnapshot as an explicit immutable
N-valued child collection.

Add the following canonical physical entity:

campaign_snapshot_commodity_items

Minimum canonical fields:

- snapshot_commodity_item_id UUID primary key
- campaign_snapshot_id UUID parent reference
- commodity_id UUID stable Commodity identity reference
- commodity_version_id UUID immutable CommodityVersion reference

The table represents launch-time Commodity compatibility context only.

The collection is immutable after Campaign Save & Launch.

The representation must not be an opaque JSON business object.

The approved physical mapping does not change Commodity selection,
Commodity governance, Stocktaking, calculation, Campaign lifecycle,
authorization, offline authority or Final Snapshot semantics.

Detailed FK, uniqueness, CHECK and index enforcement remains governed by the
existing database-hardening tasks and frozen database rules.

## Reason

The logical and physical frozen contracts currently require an N-valued
Commodity compatibility collection but do not provide a canonical physical
mapping for that collection.

The omission blocks full closure of IMP-DB-001 because an implementation
choice would otherwise have to invent either:

- a new physical child table;
- JSON storage;
- embedding into campaign_snapshot_scope_items; or
- another representation not explicitly authorized by the frozen
  physical database contract.

The requested change resolves the documentation gap without changing
approved product behavior.

## Alternatives considered

### Opaque JSON on campaign_snapshots

Rejected.

This would conflict with the frozen requirement that N-valued
CampaignSnapshot business collections use explicit child structures rather
than an opaque JSON business object.

### Add Commodity fields to campaign_snapshot_scope_items

Rejected.

Commodity compatibility is not defined as one Commodity per Storage Space
at Campaign launch. Embedding it into scope rows would change the semantic
cardinality and introduce an unsupported relationship.

### Reuse current Commodity / CommodityVersion tables without snapshot child rows

Rejected.

Current governed reference data alone does not persist the frozen
Campaign launch compatibility set and therefore cannot prove which
Commodity versions were part of the launch-time compatibility context.

### Leave the mapping implementation-defined

Rejected.

Document 10 is the physical database authority. Selecting a new canonical
business persistence structure without controlled baseline approval would
silently resolve a frozen-document contradiction by implementation
preference.

## Impact analysis

### Business rules / lifecycle / workflows

No change.

Campaign lifecycle remains ACTIVE / COMPLETED.

Save & Launch remains atomic.

CampaignSnapshot remains immutable launch-time context.

### UI / navigation / design

No change.

No new screen, action, status or user-visible capability.

### Architecture / module boundaries

No change to the frozen module topology or dependency direction.

Persistence implementations must map the approved child collection through
existing architecture boundaries.

### Data model / database

Adds one explicit physical child table for the existing
commodityCompatibilitySnapshot logical collection:

campaign_snapshot_commodity_items

The child table belongs to CampaignSnapshot and is immutable after launch.

Detailed PK/FK/UQ/CHECK/index hardening follows the existing database task
separation and frozen database rules.

### API / backend contract

No new product API capability.

CampaignSnapshot API representations continue to expose the existing frozen
Commodity compatibility context.

Backend persistence mapping is updated to the approved physical structure.

### Offline / synchronization

No semantic change.

The Campaign-scoped offline package continues to contain required authorized
Commodity / CommodityVersion compatibility data.

Room may require an equivalent explicit local child representation where the
frozen package persistence contract requires persisted snapshot data.

No new sync state, conflict strategy or authority is introduced.

### Calculation engine

No change.

Commodity identity/reference values do not become V6 numerical calculation
inputs.

### Evidence / reporting

No change to Evidence.

Historical reporting continues to use frozen/pinned Commodity identity and
version context and must not reinterpret history from current master data.

### Security / privacy

No change to authorization or privacy boundaries.

The new child data remains subject to the parent Campaign/Organization
authorization context.

### Retention / production operations

CampaignSnapshot retention remains unchanged.

The new child collection is retained with its immutable CampaignSnapshot
parent under the existing retention rules.

Production migration must follow the frozen Expand -> Migrate -> Contract
policy where applicable.

## RVM impact

Update or add verification coverage proving that:

- CampaignSnapshot Commodity compatibility is represented physically;
- all persisted compatibility items belong to the correct CampaignSnapshot;
- Commodity and CommodityVersion identity are preserved;
- the snapshot collection cannot be silently rewritten by later Commodity
  catalog changes;
- offline package/history remains able to resolve the approved frozen
  compatibility set.

Existing CampaignSnapshot and Commodity historical-integrity RVM entries
must be updated to reference the canonical child mapping.

## Test impact

Add or update tests covering:

- migration creates campaign_snapshot_commodity_items;
- canonical column types exist;
- CampaignSnapshot persistence can store zero-or-more / as-required
  compatibility entries according to the frozen semantic contract;
- duplicate logical compatibility entries are rejected when the applicable
  uniqueness hardening task is implemented;
- later Commodity version publication/deactivation does not rewrite
  existing CampaignSnapshot commodity rows;
- migration assertions verify the canonical physical structure;
- offline/package mapping preserves the collection where applicable.

No existing test may remain authoritative if it assumes that CampaignSnapshot
Commodity compatibility has no physical representation.

## Migration and compatibility impact

Introduce the physical child table through a forward-only additive
migration.

No existing CampaignSnapshot row may be assigned fabricated Commodity
compatibility data where authoritative launch-time provenance is unavailable.

Existing historical rows without authoritative compatibility provenance
must remain distinguishable rather than being populated by assumption.

FK, uniqueness, CHECK, index and immutability hardening continue under their
existing implementation tasks where already separated.

No destructive rewrite of historical snapshot meaning is permitted.

## Release-gate impact

The following must pass before the approved change is considered applied:

- baseline-change process validator;
- baseline registry / traceability validators affected by refreeze;
- PostgreSQL migration validation;
- backend tests;
- Room migration regression where local schema changes are introduced;
- Android build/static quality where affected;
- full applicable CI required by the repository.

IMP-DB-001 must not be marked fully compliant until the approved physical
mapping is implemented and closure-audited.

## Approved delta

APPROVED:

CampaignSnapshot.commodityCompatibilitySnapshot is persisted as an explicit
immutable N-valued child collection.

Canonical PostgreSQL physical entity:

campaign_snapshot_commodity_items

Minimum canonical fields:

- snapshot_commodity_item_id UUID primary key
- campaign_snapshot_id UUID parent reference
- commodity_id UUID stable Commodity identity reference
- commodity_version_id UUID immutable CommodityVersion reference

The collection represents launch-time Commodity compatibility context only.

The representation must not be an opaque JSON business object.

The approved delta does not change Commodity governance, Commodity selection,
Stocktaking semantics, Campaign lifecycle, calculation behavior,
authorization, offline authority, reporting semantics or Final Snapshot
semantics.

Detailed FK, uniqueness, CHECK, index and immutability enforcement remain
governed by the existing database-hardening tasks and frozen database rules.

No additional product behavior is approved by this Change Request.

## Implementation verification

Complete when the approved change is applied.

- Baseline documentation updated:
- Requirement registry updated:
- RVM updated:
- Affected tests updated:
- Applicable validators PASS:
- CI PASS:
- Implementation commit / PR:
- Release evidence:

## Authority guard

This Change Request MUST NOT be treated as authority while its status is
PROPOSED or REJECTED.

Only the explicitly APPROVED delta may alter the frozen V6 contract.

All unaffected requirements in Documents 01-21 remain authoritative.

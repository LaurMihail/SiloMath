# SiloMath V6 — 09 DATA_MODEL

**Document ID:** 09_DATA_MODEL  
**Version:** V6 Final Documentation — Data Model 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-17 — Android/UX implementation-closure refreeze  
**Product:** SiloMath  
**Normative role:** Canonical logical/conceptual data model for V6. It defines entities, semantic attributes, relationships, cardinalities, ownership, mutability, versioning, historical integrity and cross-aggregate invariants. Physical SQL/Room types, exact table/column names, indexes and foreign-key actions are delegated to Document 10.

## 1. Purpose, scope and authority

This document is derived from the frozen V6 contract in Documents 01–08. It does not reintroduce deprecated concepts or invent product behavior. Where V6 freezes a semantic fact but not a physical representation, this document specifies the semantic requirement and deliberately leaves the storage representation to Document 10. A field marked structured/typed may become normalized tables, embedded values or serialized metadata only if Document 10 preserves the invariants defined here.

- Technical identities use stable UUID-style IDs; business codes remain separate.
- Tenant-specific business data is explicitly Organization-scoped or has an unambiguous parent chain to Organization.
- Every entity designated **Mutable synchronized** carries createdAt, createdBy, updatedAt, updatedBy and entityVersion. Technical/local-only records use the explicit metadata listed in their entity specification.
- Immutable snapshots/version records carry creation metadata and schema/version identifiers; they do not acquire semantic updatedAt behavior.
- No destructive cascade or hard delete may invalidate operational or historical inventory references.
- Date-time persistence/API semantics are UTC instants; Start Date and Planned End Date are LocalDate calendar values.
- Canonical calculation units are m, m², m³, kg/m³ and kg; official persisted decimal results use scale 6 and ROUND_HALF_UP at the persistence/presentation boundary, with no intermediate rounding.


## 2. Canonical entity inventory

| Domain family | Entity | Lifecycle / mutability | Purpose |
| --- | --- | --- | --- |
| Identity & tenancy | User | Mutable synchronized | Platform identity; authentication subject and audit actor. |
| Identity & tenancy | Organization | Mutable synchronized | Tenant/business boundary. |
| Identity & tenancy | OrganizationMembership | Mutable synchronized | User access to an Organization and role; ACTIVE/DISABLED membership lifecycle. |
| Master data | Location | Mutable synchronized | Organization-scoped physical location. |
| Master data | StorageUnit | Mutable synchronized | Container/structure inside a Location. |
| Master data | StorageSpace | Mutable synchronized | Stocktaking unit inside a Storage Unit. |
| Master data | Client | Mutable synchronized | Organization-scoped Third-Party owner/client master. |
| Geometry | GeometryFamily | Platform-governed reference | Mathematical geometry model F01-F10. |
| Geometry | GeometryProfile | Mutable logical master | Customer-defined geometry profile identity. |
| Geometry | GeometryProfileVersion | Immutable version | Concrete frozen geometry dimensions/provenance/version. |
| Commodity | Commodity | Platform-governed reference | Canonical commodity identity. |
| Commodity | CommodityVersion | Immutable version | Versioned commodity definition/reference data. |
| Campaign | InventoryCampaign | Mutable until completed | Campaign header and lifecycle. |
| Campaign | CampaignScope | Immutable after launch | Exact Storage Space scope of launched campaign. |
| Campaign | CampaignTeamAssignment | Mutable synchronized history-preserving assignment | Authorized operational team assignment; ACTIVE/REMOVED; Admin may change team while Campaign is ACTIVE. |
| Campaign | CampaignSnapshot | Immutable snapshot | Launch-time reproducibility/authorization context. |
| Campaign | OfflineWorkingSetManifest | Immutable package manifest/version | Campaign×DeviceInstallation versioned offline package manifest and integrity metadata. |
| Stocktaking | StocktakingRecord | Mutable synchronized aggregate root | One conceptual record per Campaign×StorageSpace; points to current StocktakingRevision. |
| Stocktaking | Measurement | Revision-owned child | 1–5 non-empty ullage readings belonging to a StocktakingRevision. |
| Stocktaking | CalculationResult | Revision calculation record; frozen at validation | Deterministic derived volume/quantity result and metadata for a StocktakingRevision. |
| Stocktaking | Issue | Mutable lifecycle + preserved history | Contextual Open/Resolved operational issue. |
| Stocktaking | IssueResolution | Append-only event/history | Explicit audited issue resolution. |
| Stocktaking | Evidence | Metadata mutable through transfer; historical content immutable | PHOTO/NOTE evidence metadata and binary reference. |
| Close | FinalInventorySnapshot | Immutable snapshot | Exactly one successful final snapshot per completed Campaign. |
| Close | FinalInventorySnapshotEntry | Immutable snapshot entry | Per scoped Storage Space frozen final facts. |
| Platform | SyncOperation | Durable operational record | Transactional outbox mutation. |
| Platform | SyncConflict | Durable operational record | Explicit local/server conflict context. |
| Platform | AuditEvent | Append-only | Authoritative audit history. |
| Platform | Notification | Mutable read state | In-app notification; not source of truth. |
| Platform | UserPreference | Mutable user-scoped preference | User-level approved preferences; one logical set per User. |
| Platform | OrganizationConfiguration | Mutable synchronized where configured | Admin-controlled approved organization-level settings only. |
| Identity & tenancy | DeviceInstallation | Mutable technical identity | Privacy-safe app installation identity for campaign offline packages; no hardware identifier. |
| Stocktaking | StocktakingRevision | Working/validated revision | Revisioned stocktaking state; validated revisions immutable; Reset Validation creates a new working revision. |
| Stocktaking | IssueStatusEvent | Append-only | OPEN/RESOLVED status transition history, including reopen before Campaign completion. |
| Platform | SyncOperationDependency | Immutable operational edge | Explicit parent→dependent SyncOperation ordering relationship. |


**Canonical logical inventory:** 35 authoritative logical entities/records. Derived report datasets are read models and are not additional authoritative business entities.

## 3. Shared identity, metadata and value-object contract

| Concept | Canonical semantic contract |
| --- | --- |
| Stable technical ID | UUID-style immutable identity; never derived from business Code. |
| Business Code | Separate human/business identifier. Master Codes normalize trim+uppercase and become stable after historical references. Campaign Code is server-generated. |
| Organization scope | Direct organizationId where appropriate or provable immutable parent relationship; server/local queries must never cross tenant boundaries. |
| Mutable synchronized metadata | createdAt, createdBy, updatedAt, updatedBy, entityVersion are mandatory for entities designated Mutable synchronized; exceptions are stated explicitly per entity. |
| Immutable metadata | createdAt, createdBy plus schema/version identifiers; no semantic updatedAt. |
| Lifecycle status | User ACTIVE/DISABLED; Master ACTIVE/INACTIVE; Campaign ACTIVE/COMPLETED; Issue OPEN/RESOLVED. |
| Operational classification | Stocktaking TO_DO/ISSUES/VALIDATED; ALL is UI filter only. |
| Ownership | OWNED or THIRD_PARTY. |
| Sync state | PENDING, IN_FLIGHT, ACKNOWLEDGED, RETRYABLE_ERROR, CONFLICT, PERMANENT_ERROR. |
| Evidence sync | LOCAL_PENDING, UPLOADING, SYNCED, SYNC_ERROR. |
| Time | UTC instant for timestamps; LocalDate for campaign Start/Planned End. |
| Decimal precision | Deterministic decimal; no intermediate rounding; persisted official numeric values scale 6 where applicable. |


## 4. Entity specifications

### 4.1 User

**Domain family:** Identity & tenancy  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** Platform identity; authentication subject and audit actor.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| userId | UUID | Yes | No | Stable technical identity. | Never reused; preserved when Disabled. |
| fullName | Text | Yes | Yes | Human-readable full name. | Required; UI contract. |
| email | Email/Text | Yes | Controlled | Authentication identifier. | Unique; no separate username. |
| lifecycleStatus | UserStatus | Yes | Yes | ACTIVE or DISABLED. | Disabled blocks new auth/operations/assignments; history retained. |
| createdAt | UTC instant | Yes | No | Creation timestamp. | Canonical metadata. |
| createdBy | UUID/User ref | Contextual | No | Provisioning/administrative actor. | System actor permitted where provisioned. |
| updatedAt | UTC instant | Yes | Yes | Last mutation timestamp. | Mutable metadata. |
| updatedBy | UUID/User ref | Yes | Yes | Last mutation actor. |  |
| entityVersion | Integer/opaque version | Yes | Server-controlled | Optimistic concurrency version. |  |


### 4.2 Organization

**Domain family:** Identity & tenancy  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** Tenant/business boundary.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| organizationId | UUID | Yes | No | Stable tenant identity. |  |
| name | Text <=150 | Yes | Yes | Organization Name. | Trim; capitalization preserved. |
| code | Text <=50 | Yes | Controlled | Business identifier. | trim+uppercase; case-insensitive unique; stable after historical references. |
| address | Text <=250 | No | Yes | Postal/street address. |  |
| city | Text <=100 | No | Yes | City. |  |
| country | Country code/value | No | Yes | Country selector value. |  |
| lifecycleStatus | MasterStatus | Yes | Yes | ACTIVE/INACTIVE. | Default ACTIVE; historical refs preserved. |
| createdAt | UTC instant | Yes | No | Creation. | Provisioning context for new tenant. |
| createdBy | Actor ref | Yes | No | Provisioning actor. |  |
| updatedAt | UTC instant | Yes | Yes | Last update. |  |
| updatedBy | Actor ref | Yes | Yes | Last update actor. |  |
| entityVersion | Version | Yes | Server-controlled | Optimistic concurrency. |  |


### 4.3 OrganizationMembership

**Domain family:** Identity & tenancy  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** User access to an Organization and role.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| membershipId | UUID | Yes | No | Stable membership identity. | One logical membership per Organization×User; reactivation does not create a duplicate. |  |
| organizationId | Organization ref | Yes | No | Tenant access scope. | Together with userId is logically unique. |  |
| userId | User ref | Yes | No | Member. | Together with organizationId is logically unique. |  |
| role | Role | Yes | Yes | ADMIN or STANDARD. | Exactly two roles; role change is audited and increments entityVersion. Historical snapshots/audit are not rewritten. |
| lifecycleStatus | MembershipStatus | Yes | Yes | ACTIVE or DISABLED. | Disabled membership grants no access; reactivation uses the same membership identity. |
| disabledAt/disabledBy | UTC instant + actor ref | Conditional | Controlled | Membership disable metadata. | Preserved for history. |
| createdAt | UTC instant | Yes | No | Grant time. |  |
| createdBy | Actor ref | Yes | No | Grant actor. |  |
| updatedAt | UTC instant | Yes | Yes | Last change. |  |
| updatedBy | Actor ref | Yes | Yes | Last change actor. |  |
| entityVersion | Version | Yes | Server-controlled | Concurrency. |  |


### 4.4 Location

**Domain family:** Master data  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** Organization-scoped physical location.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| locationId | UUID | Yes | No | Stable identity. |  |
| organizationId | Organization ref | Yes | No | Tenant owner. | Context-derived in UI. |
| name | Text <=150 | Yes | Yes | Location Name. | Trim; whitespace-only invalid. |
| code | Text <=50 | Yes | Controlled | Business code. | trim+uppercase; unique in Organization; stable after historical refs. |
| address | Text <=250 | No | Yes | Address. |  |
| city | Text <=100 | No | Yes | City. |  |
| country | Country value | No | Yes | Country. |  |
| notes | Text <=500 | No | Yes | Master-data notes. |  |
| latitude | Decimal degrees | No | Yes | Authoritative facility latitude for map presentation. | Must be paired with longitude; -90..90; no user/device tracking semantics. |
| longitude | Decimal degrees | No | Yes | Authoritative facility longitude for map presentation. | Must be paired with latitude; -180..180; no user/device tracking semantics. |
| lifecycleStatus | MasterStatus | Yes | Yes | ACTIVE/INACTIVE. | Default ACTIVE. |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical metadata | Yes | Controlled | Mutable synchronized metadata. | Per IC-13. |


### 4.5 StorageUnit

**Domain family:** Master data  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** Container/structure inside a Location.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| storageUnitId | UUID | Yes | No | Stable identity. |  |
| locationId | Location ref | Yes | No | Parent Location. | Organization derived through parent. |
| name | Text <=150 | Yes | Yes | Storage Unit Name. |  |
| code | Text <=50 | Yes | Controlled | Business code. | trim+uppercase; unique in Location; stable after historical refs. |
| description | Text <=500 | No | Yes | Description. |  |
| lifecycleStatus | MasterStatus | Yes | Yes | ACTIVE/INACTIVE. | Default ACTIVE. |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical metadata | Yes | Controlled | Mutable synchronized metadata. |  |


### 4.6 StorageSpace

**Domain family:** Master data  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** Stocktaking unit inside a Storage Unit.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| storageSpaceId | UUID | Yes | No | Stable identity. |  |
| storageUnitId | StorageUnit ref | Yes | No | Parent Storage Unit. | Organization derived through hierarchy. |
| code | Text | Yes | Controlled | Business code. | Unique in Storage Unit; trim+uppercase; supports range generation; stable after history. |
| description | Text | No | Yes | Description. |  |
| geometryProfileId | GeometryProfile ref | Yes | Yes until historical constraints | Selected logical Geometry Profile. | Only Active selectable for new config. |
| maxCapacityM3 | Decimal | Derived display | No | Capacity exposed through the current/frozen GeometryProfileVersion. | Not an independently editable StorageSpace business value. |
| lifecycleStatus | MasterStatus | Yes | Yes | ACTIVE/INACTIVE. | Default ACTIVE. |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical metadata | Yes | Controlled | Mutable synchronized metadata. |  |


### 4.7 Client

**Domain family:** Master data  
**Lifecycle/mutability:** Mutable synchronized  
**Purpose:** Organization-scoped Third-Party owner/client master.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| clientId | UUID | Yes | No | Stable identity. |  |
| organizationId | Organization ref | Yes | No | Tenant owner. |  |
| name | Text <=150 | Yes | Yes | Client Name. |  |
| code | Text <=50 | Yes | Controlled | Business code. | trim+uppercase; unique in Organization; stable after historical refs. |
| registrationTaxId | Text <=50 | No | Yes | Registration/Tax ID. |  |
| notes | Text <=500 | No | Yes | Notes. |  |
| lifecycleStatus | MasterStatus | Yes | Yes | ACTIVE/INACTIVE. | Default ACTIVE; no CRM fields in V6. |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical metadata | Yes | Controlled | Mutable synchronized metadata. |  |


### 4.8 GeometryFamily

**Domain family:** Geometry  
**Lifecycle/mutability:** Platform-governed reference  
**Purpose:** Mathematical geometry model F01-F10.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| geometryFamilyId | Stable platform ID/code | Yes | Platform | Family identity. | Exactly ten V6 families F01-F10. |
| familyCode | Enum/code | Yes | Platform | CYL_TRUNC_CONE_V1 etc. | System-controlled. |
| displayName | Text | Yes | Platform | UI name. |  |
| formulaVersion | Version | Yes | Platform | Formula/strategy identity. | Historical calculation must preserve version. |
| parameterContract | Schema/definition | Yes | Platform | Required dimensional parameters. | Not customer-editable. |
| lifecycle/support state | Platform compatibility | Yes | Platform | Whether supported by current engine. | Must preserve historical compatibility. |


### 4.9 GeometryProfile

**Domain family:** Geometry  
**Lifecycle/mutability:** Mutable logical master  
**Purpose:** Customer-defined geometry profile identity.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| geometryProfileId | UUID | Yes | No | Logical profile identity. |  |
| organizationId | Organization ref | Yes | No | Tenant owner. | Customer Admin creates profiles. |
| name | Text <=150 | Yes | Yes | Geometry Profile Name. | Trim; whitespace-only invalid. |
| code | Text <=50 | Yes | Controlled | Geometry Profile business code. | trim+uppercase; case-insensitive unique in Organization; stable after historical references. |  |
| geometryFamilyId | GeometryFamily ref | Yes | Controlled | Mathematical family. | Changing family or any calculation-affecting parameter creates a new immutable GeometryProfileVersion. |  |
| lifecycleStatus | MasterStatus | Yes | Yes | ACTIVE/INACTIVE. | Inactive not selectable for new config. |
| currentVersionId | GeometryProfileVersion ref | Yes | Controlled | Current immutable version for new operations. | Version change does not rewrite old version. |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical metadata | Yes | Controlled | Mutable logical-master metadata. |  |


### 4.10 GeometryProfileVersion

**Domain family:** Geometry  
**Lifecycle/mutability:** Immutable version  
**Purpose:** Concrete frozen geometry dimensions/provenance/version.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| geometryProfileVersionId | UUID | Yes | No | Immutable version identity. |  |
| geometryProfileId | GeometryProfile ref | Yes | No | Logical parent. |  |
| versionNumber | Integer/version | Yes | No | Monotonic profile version. |  |
| geometryFamilyId | GeometryFamily ref | Yes | No | Family used by version. |  |
| dimensions | Typed decimal parameter set | Yes | No | Concrete family parameters in canonical metres. | Exact parameter names/formulas in Doc13. |
| verifiedAreaM2 | Decimal | Conditional | No | Constant verified area for F08-F10. | Required only by applicable families. |
| areaProvenance | Structured provenance | Conditional | No | Source/verification metadata for F08-F10. | Required and validated for F08–F10; absent for formula families that do not use verified area. |
| measurementReferenceHeightM | Decimal | Yes | No | Datum/reference height used by Calculation Engine v2. | Required for every profile version used for level-based stocktaking. |
| maximumFillHeightM | Decimal | Yes | No | Maximum valid fill height. |  |
| geometryOrigin | Datum/origin definition | Yes | No | Reference origin. |  |
| maxCapacityM3 | Decimal | Yes | No | Deterministically calculated maximum capacity. | Scale/rounding per numeric contract. |
| createdAt/createdBy | UTC instant + actor | Yes | No | Version creation metadata. | No semantic updatedAt. |
| schemaVersion | Version | Yes | No | Parameter schema identity. |  |


### 4.11 Commodity

**Domain family:** Commodity  
**Lifecycle/mutability:** Platform-governed reference  
**Purpose:** Canonical commodity identity.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| commodityId | Stable platform UUID/code | Yes | Platform | Canonical commodity identity. | 24 governed commodities. |
| code | Stable platform code | Yes | Platform | Machine/business identity. | No organization Add/Edit. |
| displayName | Text | Yes | Platform | Canonical name. |  |
| currentVersionId | CommodityVersion ref | Yes | Platform | Current definition version. | Historical versions retained. |
| lifecycle/support state | Platform governed | Yes | Platform | Compatibility/availability. | No organization-level custom Commodity. |


### 4.12 CommodityVersion

**Domain family:** Commodity  
**Lifecycle/mutability:** Immutable version  
**Purpose:** Versioned commodity definition/reference data.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| commodityVersionId | UUID/version ID | Yes | No | Immutable definition version. |  |
| commodityId | Commodity ref | Yes | No | Parent. |  |
| versionNumber | Version | Yes | No | Definition version. |  |
| displayName | Text | Yes | No | Versioned label/reference data. |  |
| referenceTestWeight | Decimal/unit value | Optional | No | Reference information only. | Never automatic fallback for actual density. |
| definitionMetadata | Structured version metadata | As governed | No | Platform-governed reference attributes. | Moisture/impurities do not auto-correct quantity. |
| createdAt/createdBy | Creation metadata | Yes | No | Version creation. |  |


### 4.13 InventoryCampaign

**Domain family:** Campaign  
**Lifecycle/mutability:** Mutable until completed  
**Purpose:** Campaign header and lifecycle.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| campaignId | UUID | Yes | No | Stable technical identity. |  |
| organizationId | Organization ref | Yes | No | Tenant owner. |  |
| campaignCode | Text | Yes after Save & Launch | No | Official INV-YYYY-NNNN. | Server-generated atomically; unique Organization+calendar year. |
| name | Text | Yes | Controlled | Campaign Name. |  |
| inventoryType | InventoryType | Yes | No after launch | ANNUAL/MONTHLY/CONTROL. |  |
| startDate | LocalDate | Yes | No after launch | Calendar date. | No timezone conversion. |
| plannedEndDate | LocalDate | Yes | No after launch | Planned calendar end. | Must be >= startDate; no auto-close. |
| status | CampaignStatus | Yes | Server-controlled | ACTIVE/COMPLETED. | Review is not status. |
| launchedAt | UTC instant | Yes | No | Save & Launch instant. | Distinct from createdAt. |
| launchedBy | User ref | Yes | No | Launching Admin. | Need not be team member. |
| completedAt | UTC instant | Conditional | No | Successful Close instant. |  |
| completedBy | User ref | Conditional | No | Closing Admin. |  |
| campaignSnapshotId | CampaignSnapshot ref | Yes after launch | No | Frozen launch snapshot. |  |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical metadata | Yes | Controlled | Campaign metadata/concurrency. |  |


### 4.14 CampaignScope

**Domain family:** Campaign  
**Lifecycle/mutability:** Immutable after launch  
**Purpose:** Exact Storage Space scope of launched campaign.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| campaignScopeId | UUID | Yes | No | Scope row identity. |  |
| campaignId | Campaign ref | Yes | No | Campaign. |  |
| storageSpaceId | StorageSpace ref | Yes | No | Scoped Storage Space. | Exact scope is frozen after launch. |
| snapshotHierarchyRef | Snapshot/context ref | Yes | No | Link to launch-time hierarchy context. | Must not rely only on future mutable master. |
| createdAt/createdBy | Creation metadata | Yes | No | Scope freeze metadata. |  |


### 4.15 CampaignTeamAssignment

**Domain family:** Campaign  
**Lifecycle/mutability:** Mutable synchronized, history-preserving assignment  
**Purpose:** Authorized operational Campaign Team membership. Launch requires at least one ACTIVE assignment; Admin may add/remove members while Campaign remains ACTIVE.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| assignmentId | UUID | Yes | No | Stable assignment identity. | Historical identity retained after removal. |
| campaignId | Campaign ref | Yes | No | Campaign. | Campaign must belong to same Organization as membership. |
| userId | User ref | Yes | No | Assigned user. | At most one ACTIVE assignment per Campaign×User. |
| organizationMembershipId | OrganizationMembership ref | Yes | No | Membership authorizing assignment. | Must be ACTIVE when assignment becomes ACTIVE; historical ref retained later. |
| assignmentStatus | TeamAssignmentStatus | Yes | Controlled | ACTIVE or REMOVED. | Campaign launch requires >=1 ACTIVE assignment. |
| assignedAt | UTC instant | Yes | No | Activation time. | |
| assignedBy | User ref | Yes | No | Admin assigning member. | |
| removedAt | UTC instant | Conditional | Controlled | Removal time. | Required when REMOVED. |
| removedBy | User ref | Conditional | Controlled | Admin removing member. | Required when REMOVED. |
| createdAt/createdBy/updatedAt/updatedBy/entityVersion | Canonical mutable metadata | Yes | Controlled | Synchronization/audit metadata. | Team changes are audited. |

**Authority effect:** removal revokes authority for new Stocktaking mutations immediately on the server. Already captured data and audit remain. Cached/offline authorization is invalidated at the next authorized refresh/sync; unauthorized subsequent mutations are rejected rather than silently accepted.

### 4.16 CampaignSnapshot

**Domain family:** Campaign  
**Lifecycle/mutability:** Immutable snapshot  
**Purpose:** Launch-time reproducibility/authorization context.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| campaignSnapshotId | UUID | Yes | No | Immutable snapshot identity. | Exactly one launch snapshot context per launched campaign. |
| campaignId | Campaign ref | Yes | No | Campaign. |  |
| organizationSnapshot | Structured frozen context | Yes | No | Organization identity/context required for reproduction. |  |
| campaignConfiguration | Structured frozen context | Yes | No | Code/name/type/dates and relevant launch configuration. |  |
| scopeSnapshot | Structured refs/data | Yes | No | Exact hierarchy/scope. |  |
| geometrySnapshot | Structured refs/data | Yes | No | Family/profile/version/dimensions/calculation context. |  |
| commodityCompatibilitySnapshot | Structured refs/data | As required | No | Relevant Commodity identity/version data. |  |
| teamSnapshot | Structured refs/data | Yes | No | Campaign Team at launch. |  |
| calculationSchemaVersions | Version set | Yes | No | Engine/formula/schema/aggregation compatibility identifiers. |  |
| createdAt/createdBy | Creation metadata | Yes | No | Launch snapshot creation. |  |
| snapshotSchemaVersion | Version | Yes | No | Snapshot schema. |  |


### 4.17 OfflineWorkingSetManifest

**Domain family:** Campaign  
**Lifecycle/mutability:** Immutable package manifest/version  
**Purpose:** Integrity and completeness metadata for campaign offline package.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| manifestId | UUID | Yes | No | Manifest identity. | Unique logical package version by Campaign×DeviceInstallation×packageVersion. |  |
| campaignId | Campaign ref | Yes | No | Campaign package. |  |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| deviceInstallationId | DeviceInstallation ref | Yes | No | Privacy-safe target installation. | No IMEI/hardware/advertising identifier. |
| packageVersion | Version | Yes | No | Package generation/version. | Monotonic/opaque version within Campaign×device; only one manifest is current. |  |
| contentManifest | Structured counts/hashes/versions | Yes | No | Required hierarchy, geometry, commodity, team, stocktaking/evidence/sync compatibility content. |  |
| integrityHash/signature metadata | Hash/verification metadata | Yes | No | Integrity verification. | OFFLINE_READY only after complete verification. |
| generatedAt | UTC instant | Yes | No | Server/package generation. |  |
| downloadedAt | UTC instant | Local | Yes | Device acquisition time. |  |
| verifiedAt | UTC instant | Conditional | Yes | Integrity verification time. |  |
| isCurrent | Boolean | Yes | Controlled | Current manifest selector for Campaign×device. | At most one current. |
| offlineReady | Derived Boolean | Derived | No | Package readiness. | True only for current complete verified authorized package; not CampaignStatus. |


### 4.18 StocktakingRecord

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Mutable synchronized aggregate root  
**Purpose:** Stable conceptual Stocktaking identity for one Campaign×StorageSpace, independent of revision history.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| stocktakingRecordId | UUID | Yes | No | Stable aggregate identity. | At most one conceptual record per Campaign×StorageSpace. |
| organizationId | Organization ref | Yes | No | Tenant. | Must equal Campaign Organization. |
| campaignId | Campaign ref | Yes | No | Campaign. | Storage Space must be in frozen CampaignScope. |
| storageSpaceId | StorageSpace ref | Yes | No | Scoped Storage Space. | Unique with campaignId. |
| currentRevisionId | StocktakingRevision ref | Conditional until first revision | Controlled | Current WORKING or VALIDATED revision. | Reset Validation repoints to newly created WORKING revision; old validated revision remains immutable. |
| classification | StocktakingClassification | Derived | No | TO_DO / ISSUES / VALIDATED. | Derived from current revision and Issue condition; ALL is UI filter only. |
| createdAt | UTC instant | Yes | No | Root creation. | |
| createdBy | User ref | Yes | No | Creator. | |
| updatedAt | UTC instant | Yes | Controlled | Last root mutation. | |
| updatedBy | User ref | Yes | Controlled | Last root mutation actor. | |
| entityVersion | Version | Yes | Server-controlled | Optimistic concurrency. | Root remains stable across revisions. |

### 4.19 Measurement

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Revision-owned child; immutable when owning revision is VALIDATED  
**Purpose:** One ullage reading belonging to one StocktakingRevision.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| measurementId | UUID | Yes | No | Reading identity. | |
| stocktakingRevisionId | StocktakingRevision ref | Yes | No | Owning revision. | Measurement never migrates between revisions. |
| sequenceNumber | Integer 1..5 | Yes | Controlled while WORKING | Reading order. | Unique within revision; max 5. |
| rawValue | Decimal | Yes | Controlled while WORKING | Operator ullage reading. | |
| rawUnit | Unit | Yes | Controlled while WORKING | Entered unit. | Must normalize deterministically to metres. |
| normalizedUllageM | Decimal | Yes | Derived | Canonical ullage. | Same datum/profile version/type across all measurements in revision. |
| datumMetadata | MeasurementDatum | Yes | Controlled while WORKING | Reference datum identity/values. | Must match frozen GeometryProfileVersion reference datum. |
| geometryProfileVersionId | GeometryProfileVersion ref | Yes | No | Exact geometry version. | Same across revision measurement set. |
| capturedAt | UTC instant | Yes | No | Capture time. | |
| capturedBy | User ref | Yes | No | Capturing team member. | |
| entityVersion | Version | Yes while WORKING | Server-controlled | Concurrency. | Competing measurement edits are not auto-merged. |

### 4.20 CalculationResult

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Replaceable current result while revision is WORKING; immutable when revision is VALIDATED  
**Purpose:** Deterministic Calculation Engine v2 result for one StocktakingRevision.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| calculationResultId | UUID | Yes | No | Result identity. | |
| stocktakingRevisionId | StocktakingRevision ref | Yes | No | Owning revision. | At most one current result at a time for a WORKING revision. |
| surfaceModelVersion | Enum/version | Yes | No | LEVEL_BASED_V1. | |
| aggregationMethod | Enum | Yes | No | MEAN_ULLAGE; combined rule identifier MEAN_ULLAGE_V1 with aggregationRuleVersion=1. | |
| aggregationRuleVersion | Integer/version | Yes | No | 1. | |
| effectiveUllageM | Decimal | Required non-empty | No | Mean normalized ullage. | Null for Mark Empty. |
| effectiveFillHeightM | Decimal | Required non-empty | No | H_ref - u_eff. | 0 <= h <= maximumFillHeightM; no clamp. |
| productVolumeM3 | Decimal scale6 persisted | Yes | No | Product volume. | Zero for Mark Empty. |
| actualBulkDensityKgM3 | Decimal scale6 | Required non-empty | No | Actual density used. | >0; null for Mark Empty. |
| physicalQuantityKg | Decimal scale6 | Yes | No | ProductVolumeM3 × ActualBulkDensityKgM3. | Zero for Mark Empty. |
| geometryFamilyId | GeometryFamily ref | Required non-empty | No | Geometry family. | Null only for explicit Mark Empty result path if engine geometry is bypassed. |
| geometryProfileVersionId | GeometryProfileVersion ref | Required non-empty | No | Exact geometry version. | Null only for explicit Mark Empty result path if calculation is bypassed. |
| engineVersion | Version | Yes | No | Calculation Engine implementation version. | |
| formulaVersion | Version | Required non-empty | No | F01–F10 formula/version identity. | |
| inputFingerprint | Hash | Required non-empty | No | Fingerprint of calculation inputs. | Stale fingerprint blocks Validation. |
| roundingPolicy | Enum | Yes | No | ROUND_HALF_UP. | Applied only at persistence/presentation boundary; no intermediate rounding. |
| calculatedAt | UTC instant | Yes | No | Calculation time. | |
| calculatedBy/source | User/system source | Yes | No | Origin. | |

When WORKING inputs change, the current result becomes stale and a new current result is produced. The result attached to a VALIDATED revision is frozen and retained historically.

### 4.21 Issue

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Mutable lifecycle + preserved history  
**Purpose:** Contextual Open/Resolved operational issue.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| issueId | UUID | Yes | No | Issue identity. |  |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| campaignId | Campaign ref | Yes | No | Campaign. |  |
| stocktakingRecordId | StocktakingRecord ref | Yes | No | Context. |  |
| issueCategory | IssueCategory | Yes | No | One of six frozen categories. |  |
| description | Text | Yes | Controlled | Issue description. | Mandatory all categories. |
| status | IssueStatus | Yes | Controlled | OPEN/RESOLVED current projection. | RESOLVED may be reopened to OPEN before Campaign completion; every transition creates IssueStatusEvent. |  |
| reportedAt/reportedBy | UTC instant + User ref | Yes | No | Creation audit. | Reporter must be authorized Campaign Team member. |
| resolvedAt/resolvedBy | UTC instant + User ref | Conditional | Controlled | Latest resolution summary. | Detailed resolution and status history retained; cleared/currently non-effective when reopened without deleting history. |
| entityVersion | Version | Yes | Server-controlled | Concurrency. |  |


### 4.22 IssueResolution

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Append-only event/history  
**Purpose:** Explicit audited issue resolution.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| issueResolutionId | UUID | Yes | No | Resolution event identity. |  |
| issueId | Issue ref | Yes | No | Resolved issue. |  |
| resolutionReason | Text | Yes | No | Explicit reason. | Required; includes Admin follow-up acceptance reason when applicable. |
| resolutionContext | IssueResolutionContext | Yes | No | NORMAL or CLOSE_FOLLOW_UP_ACCEPTANCE. | Context does not create a third Issue status. |
| resolvedAt | UTC instant | Yes | No | Resolution instant. |  |
| resolvedBy | User ref | Yes | No | Authorized actor. |  |
| auditEventId | AuditEvent ref | Yes | No | Linked authoritative audit event. |  |


### 4.23 Evidence

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Metadata mutable through transfer; historical content immutable  
**Purpose:** PHOTO/NOTE evidence metadata and binary reference.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| evidenceId | UUID | Yes | No | Evidence metadata identity. |  |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| campaignId | Campaign ref | Yes | No | Campaign context. |  |
| stocktakingRecordId | StocktakingRecord ref | Conditional | No | Principal Stocktaking owner context. | Exactly one principal owner: StocktakingRecord or Issue. |  |
| issueId | Issue ref | Conditional | No | Principal Issue owner context. | Exactly one principal owner: Issue or StocktakingRecord. |
| stocktakingRevisionId | StocktakingRevision ref | Conditional | Controlled before validation | Revision binding for Stocktaking-owned evidence when part of a validated revision. | Prevents Reset Validation from moving evidence retrospectively. |
| evidenceType | EvidenceType | Yes | No | PHOTO/NOTE. |  |
| noteText | Text | Required for NOTE | Controlled before validation | NOTE content. | NOTE has no binary object and does not consume photo limit. |  |
| localPrivateRef | Opaque local reference | PHOTO, device-side until cleanup | Controlled | App-private binary reference. | No public filesystem assumption. |
| remoteObjectRef | Opaque object key/ref | PHOTO after upload | Controlled | Remote object storage reference. |  |
| sha256 | Hash | PHOTO | No after content fixed | Binary integrity hash. |  |
| mimeType | Text | PHOTO | No | Normalized media type. | image/jpeg. |
| byteSize | Integer | PHOTO | No | Processed size. | Target <=1.5MB; hard max 3MB. |
| pixelWidth/pixelHeight | Integer | PHOTO | No | Normalized dimensions. | Long edge <=2048. |
| syncState | EvidenceSyncState | Yes | Controlled | LOCAL_PENDING/UPLOADING/SYNCED/SYNC_ERROR. | Applies to persistence/transfer lifecycle; NOTE may synchronize without binary upload. |  |
| removedAt/removedBy | UTC instant + actor | Conditional | Controlled before validation | Pre-validation logical removal metadata. | Removal is audited; validated evidence cannot be removed. |
| createdAt/createdBy | Creation metadata | Yes | No | Evidence creation. | Validated historical evidence becomes read-only. |


### 4.24 FinalInventorySnapshot

**Domain family:** Close  
**Lifecycle/mutability:** Immutable snapshot  
**Purpose:** Exactly one successful final snapshot per completed Campaign.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| finalSnapshotId | UUID | Yes | No | Immutable snapshot identity. | Exactly one per successfully closed Campaign. |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| campaignId | Campaign ref | Yes | No | Completed Campaign. | Unique. |
| campaignSnapshotId | CampaignSnapshot ref | Yes | No | Launch context. |  |
| closedAt | UTC instant | Yes | No | Authoritative Close instant. |  |
| closedBy | User ref | Yes | No | Closing Admin. |  |
| snapshotSchemaVersion | Version | Yes | No | Final snapshot schema. |  |
| calculationCompatibilityVersions | Version set | Yes | No | Required reproduction metadata. |  |
| createdAt/createdBy | Creation metadata | Yes | No | Immutable creation. | No updatedAt semantics. |


### 4.25 FinalInventorySnapshotEntry

**Domain family:** Close  
**Lifecycle/mutability:** Immutable snapshot entry  
**Purpose:** Per scoped Storage Space frozen final facts.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| snapshotEntryId | UUID | Yes | No | Entry identity. |  |
| finalSnapshotId | FinalInventorySnapshot ref | Yes | No | Parent. |  |
| storageSpaceId | Stable source ref | Yes | No | Source identity. | Historical display must not depend on mutable current master. |
| hierarchySnapshot | Structured frozen values/refs | Yes | No | Organization/Location/Unit/Space context. |  |
| geometrySnapshot | Structured frozen values/refs | Yes | No | Family/profile/version/dimensions/capacity. |  |
| commoditySnapshot | Structured frozen values/refs | Conditional | No | Commodity identity/version. | Non-empty only. |
| ownership/clientSnapshot | Structured frozen values/refs | Conditional | No | Ownership and Client when applicable. |  |
| isEmpty | Boolean | Yes | No | Final empty flag. |  |
| measurementsSnapshot | Structured list | Conditional | No | Raw/normalized readings and datum. |  |
| operationalValues | Structured decimals | Yes | No | Density/volume/quantity and related operational facts. |  |
| calculationMetadata | Structured versions/fingerprint | Yes | No | Reproduction metadata. |  |
| validationMetadata | Actor/time/revision | Yes | No | Validation evidence. |  |
| issueReferences/resolutionSummary | Frozen list | Zero or more | No | Issue IDs, categories, final status and resolution metadata relevant to the final record. | Empty list when no Issues exist. |
| evidenceReferences | Frozen list | Zero or more | No | Evidence IDs, type, hash/object reference and frozen display metadata needed by dossier/audit. | Empty list when no Evidence exists; binaries are not duplicated. |


### 4.26 SyncOperation

**Domain family:** Platform  
**Lifecycle/mutability:** Durable operational record  
**Purpose:** Transactional outbox mutation.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| syncOperationId | UUID | Yes | No | Durable outbox operation identity. | Also idempotency identity/key. |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| entityType/entityId | Type + UUID | Yes | No | Mutation target. |  |
| operationType | Machine enum | Yes | No | Mutation semantic. | Exact endpoint mapping in Doc12. |
| payload/reference | Structured mutation data/ref | Yes | No | Durable mutation. |  |
| baseEntityVersion | Version | Required for mutation of a versioned mutable entity; otherwise null | No | Optimistic concurrency base. | Exact applicability is determined by whether the target entity carries entityVersion. |
| state | SyncState | Yes | Controlled | PENDING/IN_FLIGHT/ACKNOWLEDGED/RETRYABLE_ERROR/CONFLICT/PERMANENT_ERROR. |  |
| idempotencyKey | UUID/opaque key | Yes | No | Retry identity. | Same key reused on retry. |

| attemptCount/nextAttemptAt | Integer/UTC instant | Yes | Controlled | Retry scheduling. | Exponential backoff. |
| lastErrorCode/correlationId | Text | Conditional | Controlled | Safe diagnostics. |  |
| createdAt/updatedAt | UTC instant | Yes | Controlled | Operational metadata. |  |


### 4.27 SyncConflict

**Domain family:** Platform  
**Lifecycle/mutability:** Durable operational record  
**Purpose:** Explicit local/server conflict context.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| syncConflictId | UUID | Yes | No | Conflict identity. |  |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| syncOperationId | SyncOperation ref | Yes | No | Originating mutation. |  |
| entityType/entityId | Type + UUID | Yes | No | Target. |  |
| localAttempt | Structured value/ref | Yes | No | Preserved local attempted value. |  |
| serverAuthoritative | Structured value/ref | Yes | No | Server value/version. |  |
| baseEntityVersion/serverEntityVersion | Versions | Yes | No | Concurrency context. |  |
| detectedAt | UTC instant | Yes | No | Conflict time. |  |
| status | SyncConflictStatus | Yes | Controlled | OPEN or RESOLVED. | Conflict record/history is retained. |
| resolutionType | ConflictResolutionType | Conditional | Controlled | RELOAD_AND_REENTER / DISCARD_LOCAL_ATTEMPT / NO_ACTION_REQUIRED. | FORCE_OVERWRITE and AUTO_MERGE do not exist. |
| resolvedAt/resolvedBy | UTC instant + actor | Conditional | Controlled | Explicit resolution handling metadata. | Re-entry/reload semantics per Doc12. |


### 4.28 AuditEvent

**Domain family:** Platform  
**Lifecycle/mutability:** Append-only  
**Purpose:** Authoritative audit history.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| eventId | UUID | Yes | No | Append-only event identity. |  |
| organizationId | Organization ref | Yes | No | Tenant. |  |
| actorUserId | User ref | Conditional | No | Human actor. | Nullable only for SYSTEM actions; source=SYSTEM and systemActor metadata then required. |
| action | Stable action code | Yes | No | Audited action. |  |
| entityType | Stable type code | Yes | No | Affected entity type. |  |
| entityId | UUID | Yes | No | Affected identity. |  |
| timestampUtc | UTC instant | Yes | No | Authoritative audit instant. |  |
| campaignId | Campaign ref | No | No | Campaign context. |  |
| changeMetadata | Structured before/after/change set | Conditional | No | Relevant structured change metadata. | Safe/minimized. |
| correlationId | Text/UUID | Yes | No | Cross-system correlation. |  |
| source | Stable source code | Yes | No | Originating client/system/source. | SYSTEM required when actorUserId is null. |
| systemActor | Stable system component identity | Conditional | No | System actor metadata. | Required when actorUserId is null. |  |


### 4.29 Notification

**Domain family:** Platform  
**Lifecycle/mutability:** Mutable read state  
**Purpose:** In-app notification; not source of truth.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| notificationId | UUID | Yes | No | Notification identity. |  |
| organizationId | Organization ref | Yes | No | Tenant/context. |  |
| userId | User ref | Yes | No | Recipient. |  |
| type | NotificationType | Yes | No | SYNC_ERROR/SYNC_CONFLICT/CAMPAIGN_ASSIGNMENT/ISSUE_REQUIRES_ATTENTION/CAMPAIGN_COMPLETED. |  |
| title | Text | Yes | No | Display title. |  |
| message | Text | Yes | No | Display message. |  |
| contextualReference | Typed entity/deep-link ref | No | No | Optional navigation target. | Must re-authorize target. |
| createdAt | UTC instant | Yes | No | Timestamp. |  |
| readAt | UTC instant | No | Yes | Read state timestamp. | Null = Unread. |


### 4.30 UserPreference

**Domain family:** Platform  
**Lifecycle/mutability:** Mutable user-scoped preference  
**Purpose:** User-level approved preferences.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| userId | User ref | Yes | No | Preference owner. |  |
| theme | ThemePreference | Yes | Yes | SYSTEM/LIGHT/DARK. |  |
| notificationPreferences | Structured allowed preferences | Conditional | Yes | Only actual configurable notification types. | No invented settings. |
| updatedAt | UTC instant | Yes | Yes | Preference update. |  |


### 4.31 OrganizationConfiguration

**Domain family:** Platform  
**Lifecycle/mutability:** Mutable synchronized where configured  
**Purpose:** Admin-controlled approved organization-level settings only.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| organizationId | Organization ref | Yes | No | Configuration owner. |  |
| configurationKey | Approved registry key | Yes per configured row | Platform-defined/Admin value | Key from platform-controlled registry. | Unique per Organization×key; arbitrary keys prohibited. |
| valueType | ConfigValueType | Yes | Platform-defined | BOOLEAN / INTEGER / DECIMAL / TEXT / ENUM. | Must match registry definition. |
| value | Typed scalar | Yes when row exists | Admin | Current configured value. | Validated by key type/range/allowed-values contract; arbitrary JSON prohibited. |
| updatedAt/updatedBy/entityVersion | Canonical mutable metadata | Conditional | Controlled | Synchronization/audit metadata. | Must not expose system-controlled rules. |



### 4.32 DeviceInstallation

**Domain family:** Identity & tenancy  
**Lifecycle/mutability:** Mutable technical identity  
**Purpose:** Privacy-safe application-installation identity used to bind versioned Campaign offline packages without hardware identifiers.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| deviceInstallationId | UUID | Yes | No | Random app-installation identity. | Generated for the installation; never IMEI, serial number, advertising ID or hardware fingerprint. |
| userId | User ref | Conditional | Controlled | Currently associated authorized user. | Association follows authorized session context; historical manifests keep the installation ID. |
| createdAt | UTC instant | Yes | No | Installation identity creation. | |
| lastAuthorizedAt | UTC instant | Conditional | Controlled | Last successful authorization association. | Not a substitute for server authorization. |
| revokedAt | UTC instant | Conditional | Controlled | Installation authorization revocation marker. | Revocation invalidates future authorized package use without deleting unsynced local operations. |

### 4.33 StocktakingRevision

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Working revision; immutable once Validated  
**Purpose:** Exact revision of Stocktaking business inputs and validation state. Reset Validation creates a new working revision rather than rewriting a validated revision.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| stocktakingRevisionId | UUID | Yes | No | Revision identity. | |
| stocktakingRecordId | StocktakingRecord ref | Yes | No | Stable aggregate root. | |
| revisionNumber | Positive integer | Yes | No | Monotonic revision number within record. | Unique per StocktakingRecord. |
| revisionState | StocktakingRevisionState | Yes | Controlled | WORKING or VALIDATED. | VALIDATED revision is immutable. |
| basedOnRevisionId | StocktakingRevision ref | Conditional | No | Source revision for Reset Validation. | New Reset revision references the prior validated revision. |
| isEmpty | Boolean | Yes | Controlled while WORKING | Mark Empty semantic. | If true: 0 measurements, volume=0, quantity=0; Commodity/density/Ownership/Client not required. |
| commodityVersionId | CommodityVersion ref | Required non-empty | Controlled while WORKING | Commodity frozen for this revision. | |
| actualBulkDensityKgM3 | Decimal | Required non-empty | Controlled while WORKING | Actual density. | >0; canonical kg/m³. |
| ownership | Ownership | Required non-empty | Controlled while WORKING | OWNED/THIRD_PARTY. | |
| clientId | Client ref | THIRD_PARTY only | Controlled while WORKING | Third-Party Client. | Null for OWNED. |
| notes | Text | No | Controlled while WORKING | Revision-specific Stocktaking Notes. | Historical validated notes preserved. |
| calculationResultId | CalculationResult ref | Conditional | Controlled while WORKING | Current calculation for revision. | Must be fresh before validation for non-empty. |
| validatedAt/validatedBy | UTC instant + User ref | VALIDATED | Set once | Validation metadata. | |
| createdAt/createdBy | UTC instant + actor | Yes | No | Revision creation metadata. | |
| entityVersion | Version | Yes while WORKING | Server-controlled | Concurrency. | No mutation after VALIDATED. |

### 4.34 IssueStatusEvent

**Domain family:** Stocktaking  
**Lifecycle/mutability:** Append-only  
**Purpose:** Complete OPEN/RESOLVED Issue transition history.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| issueStatusEventId | UUID | Yes | No | Event identity. | |
| issueId | Issue ref | Yes | No | Issue. | |
| fromStatus | IssueStatus | Conditional | No | Previous status. | Null only for initial OPEN creation event. |
| toStatus | IssueStatus | Yes | No | OPEN or RESOLVED. | RESOLVED→OPEN permitted only before Campaign COMPLETED. |
| reason | Text | Required for resolution/reopen | No | Transition reason. | Close follow-up acceptance requires explicit reason. |
| changedAt | UTC instant | Yes | No | Transition instant. | |
| changedBy | User ref | Yes | No | Authorized actor. | |
| auditEventId | AuditEvent ref | Yes | No | Linked audit event. | |

### 4.35 SyncOperationDependency

**Domain family:** Platform  
**Lifecycle/mutability:** Immutable operational edge  
**Purpose:** Explicit ordering/dependency relation between durable SyncOperations.

| Attribute / value | Semantic type | Required | Mutability | Meaning | Invariant / rule |
| --- | --- | --- | --- | --- | --- |
| dependencyId | UUID | Yes | No | Edge identity. | |
| parentOperationId | SyncOperation ref | Yes | No | Operation that must reach required terminal/ack state first. | |
| dependentOperationId | SyncOperation ref | Yes | No | Operation blocked by parent. | Parent and dependent cannot be identical. |
| dependencyType | SyncDependencyType | Yes | No | REQUIRED_BEFORE. | V6 uses explicit prerequisite ordering; no implicit force merge. |
| createdAt | UTC instant | Yes | No | Edge creation. | |

## 5. Relationship and cardinality model

| Parent/source | Child/target | Cardinality | Invariant |
| --- | --- | --- | --- |
| Organization | Location | 1 : N | Location belongs to exactly one Organization. |
| Organization | Client | 1 : N | Client is Organization-scoped. |
| Organization | OrganizationMembership | 1 : N | Membership grants a User access/role in an Organization. |
| User | OrganizationMembership | 1 : N | A User may have memberships in multiple authorized Organizations. |
| Location | StorageUnit | 1 : N | Storage Unit belongs to exactly one Location. |
| StorageUnit | StorageSpace | 1 : N | Storage Space belongs to exactly one Storage Unit. |
| GeometryFamily | GeometryProfile | 1 : N | Profile selects one mathematical family. |
| GeometryProfile | GeometryProfileVersion | 1 : N | Profile changes create immutable versions; old versions retained. |
| Commodity | CommodityVersion | 1 : N | Commodity definitions are versioned and historical versions retained. |
| Organization | InventoryCampaign | 1 : N | Campaign belongs to exactly one Organization. |
| InventoryCampaign | CampaignScope | 1 : N | Launched campaign has frozen scope rows. |
| CampaignScope | StorageSpace | N : 1 | Each scope row identifies one Storage Space; duplicate Campaign×Space prohibited. |
| InventoryCampaign | CampaignTeamAssignment | 1 : N history | Launch requires >=1 ACTIVE assignment; Admin may add/remove while ACTIVE; historical REMOVED assignments retained. |
| CampaignTeamAssignment | User | N : 1 | Assigned User must have authorized Organization membership. |
| InventoryCampaign | CampaignSnapshot | 1 : 1 after launch | Launch creates the frozen campaign snapshot/context. |
| InventoryCampaign | OfflineWorkingSetManifest | 1 : N | Versioned per DeviceInstallation; one current manifest per Campaign×device. |
| InventoryCampaign | StocktakingRecord | 1 : N | Exactly zero-or-one conceptual StocktakingRecord per scoped StorageSpace until work begins; max one per Campaign×StorageSpace. |
| StorageSpace | StocktakingRecord | 1 : N across campaigns | A space may be counted in many campaigns; max one per campaign. |
| StocktakingRecord | StocktakingRevision | 1 : N | Root keeps revision history; exactly one currentRevisionId. |
| StocktakingRevision | Measurement | 1 : 0..5 | Non-empty validated revision requires 1..5; Mark Empty validated revision requires 0. |
| StocktakingRevision | CalculationResult | 1 : 0..N working replacements; 1 frozen at validation | Input changes invalidate/replace working result; validated result remains immutable. |
| StocktakingRecord | Issue | 1 : N | Issues contextual to Stocktaking. |
| Issue | IssueResolution | 1 : N history | Every resolution has reason/actor/time and is preserved. |
| Issue | IssueStatusEvent | 1 : N | Append-only OPEN/RESOLVED transition history including reopen. |
| StocktakingRecord | Evidence | 1 : N | Principal Stocktaking evidence owner; PHOTO max 5 per StocktakingRevision; NOTE does not consume photo limit. |
| Issue | Evidence | 1 : N | Principal Issue evidence owner; PHOTO max 5 per Issue; NOTE does not consume photo limit. |
| InventoryCampaign | FinalInventorySnapshot | 1 : 0..1 | Exactly one after successful Close; none before. |
| FinalInventorySnapshot | FinalInventorySnapshotEntry | 1 : N | One final entry for every scoped Storage Space. |
| CampaignScope | FinalInventorySnapshotEntry | 1 : 1 at close | Final snapshot coverage equals frozen campaign scope. |
| SyncOperation | SyncConflict | 1 : 0..N | Conflict records preserve attempted/server values for an operation/entity. |
| SyncOperation | SyncOperationDependency | 1 : N edges | Explicit parent→dependent operation ordering; retries reuse same operation/idempotency key. |
| Organization/User/Entity | AuditEvent | 1 : N | Append-only audit events reference actor/context/entity. |
| User | Notification | 1 : N | Recipient notifications; read state mutable. |
| User | UserPreference | 1 : 0..1 logical preference set | Defaults apply when absent. |
| DeviceInstallation | OfflineWorkingSetManifest | 1 : N | Privacy-safe installation receives versioned Campaign packages. |
| Organization | OrganizationConfiguration | 1 : 0..N | At most one configured value per approved registry key; absence means not configured/default behavior. |


## 6. Aggregate boundaries

| Aggregate | Root | Included/associated records | Transactional / consistency boundary |
| --- | --- | --- | --- |
| Organization access | Organization | OrganizationMembership; organization-scoped masters | Tenant isolation and authorization. |
| Master hierarchy | Location / StorageUnit / StorageSpace | Hierarchy references; GeometryProfile association | No destructive cascade into history. |
| Geometry profile | GeometryProfile | GeometryProfileVersion | New calculation-affecting definition creates new immutable version. |
| Commodity | Commodity | CommodityVersion | Platform-governed immutable versions. |
| Campaign launch | InventoryCampaign | CampaignScope, CampaignTeamAssignment, CampaignSnapshot | Save & Launch server transaction establishes code, Active campaign, frozen scope/team/snapshot. |
| Stocktaking | StocktakingRecord | Measurements, CalculationResults, contextual Evidence, Issues | Local durable mutation + SyncOperation transaction where offline-capable. |
| Issue | Issue | IssueResolution, issue Evidence | Resolution explicit/audited; validation never auto-resolves. |
| Close | FinalInventorySnapshot | FinalInventorySnapshotEntry | Server atomic: revalidate guards + snapshot + Campaign Completed. |
| Sync outbox | SyncOperation | SyncConflict references | Durable retry/idempotency/version context. |
| Audit | AuditEvent | None; append-only stream | Independent immutable evidence of changes/actions. |


## 7. Organization and tenant isolation

- Every tenant-specific authoritative record must be scoped directly by organizationId or through a parent chain that cannot ambiguously cross Organizations.
- A stable child ID never authorizes access by itself; membership/role/team/state checks remain required.
- Client, Location, Geometry Profile, Campaign and tenant operational data cannot be referenced across Organizations.
- Platform-governed Geometry Families and Commodities are shared definitions, but tenant operations reference stable governed versions.
- New Organization creation is permitted only through authorized platform provisioning/onboarding context; normal Organization Admin membership does not grant arbitrary tenant creation.
- OrganizationMembership is unique per Organization×User and has ACTIVE/DISABLED lifecycle independent of User lifecycle.
- The server must reject any role/lifecycle change that would leave an Organization with no ACTIVE Admin.
- Campaign Team membership is separate from Organization Role. Team changes are Admin-only while Campaign ACTIVE and are auditable/history-preserving.


## 8. Master-data lifecycle and referential integrity

- Organization, Location, Storage Unit, Storage Space, Geometry Profile and Client use ACTIVE/INACTIVE rather than hard-delete semantics when historically referenced.
- Inactive records remain visible through historical snapshots/references and cannot be selected for new configuration where the frozen rules prohibit selection.
- Inactivation cannot invalidate an already-launched Active Campaign; that campaign continues using its frozen snapshot/version context.
- Master Codes are editable at creation and become stable after historical references exist. UUID identity never changes with Code.
- Commodity definitions and Geometry Families are platform-governed; organization users do not mutate them.


## 9. Versioning model

| Versioned concern | Version rule | Historical rule |
| --- | --- | --- |
| Geometry Profile | Logical GeometryProfile + immutable GeometryProfileVersion. | Campaign/Calculation/Snapshot retain exact version. |
| Commodity | Logical Commodity + immutable CommodityVersion. | Historical inventory retains exact version; current definition cannot rewrite history. |
| Stocktaking validation | Working revision + validation revision/history. | Admin Reset Validation creates new working revision; prior validated revision preserved. |
| Calculation | Immutable CalculationResult per input revision + engine/formula/aggregation versions + fingerprint. | Historical result never recalculated. |
| Campaign | CampaignSnapshot freezes launch-time operational context. | Scope/team/configuration used by campaign remain reproducible. |
| Final inventory | FinalInventorySnapshot + entries + schema/calculation compatibility versions. | Immutable official history. |
| Synchronization | entityVersion/baseEntityVersion. | Stale writes surface conflict; no silent last-write-wins. |


## 10. Campaign launch data invariants

- Campaign Code is absent/unofficial before authoritative Save & Launch and is assigned by the server as INV-YYYY-NNNN, atomically unique within Organization and calendar year.
- Inventory Type is exactly ANNUAL, MONTHLY or CONTROL.
- Start Date is mandatory; Planned End Date is mandatory and >= Start Date.
- Campaign Team contains at least one member; launching Admin is not automatically required to be in the team.
- Scope is an exact set of Storage Spaces and becomes immutable after launch.
- CampaignSnapshot freezes campaign configuration, Organization, exact scope/hierarchy context, geometry family/profile/version/calculation parameters, relevant Commodity version compatibility, team and schema/calculation versions.
- Save & Launch does not imply OFFLINE_READY.


## 11. Stocktaking data invariants

- Conceptual uniqueness is Campaign×StorageSpace -> at most one StocktakingRecord.
- Stocktaking mutation requires authorized Campaign Team membership for either Admin or Standard.
- Non-empty VALIDATED StocktakingRevision requires 1–5 Measurements owned by that revision, one common datum/profile version/type, CommodityVersion, Actual Bulk Density >0 and Ownership.
- THIRD_PARTY requires Client; OWNED does not require Client.
- Mark Empty sets isEmpty=true and final Validated semantics with Product Volume=0 and Physical Quantity=0; measurements, Commodity, density, Ownership and Client are not required.
- Geometry is read-only during Stocktaking; discrepancies are Issues, not edits to frozen geometry.
- TO_DO/ISSUES/VALIDATED are operational classifications; ALL is not persisted as a fourth state.
- Validate and Mark Empty do not auto-resolve an Open Issue.
- Validated StocktakingRevision is immutable. Admin Reset Validation is explicit/audited and creates a new WORKING StocktakingRevision based on the prior validated revision; the prior revision remains immutable history.
- Completed Campaign stocktaking cannot be modified.


## 12. Calculation data invariants

- Pipeline: Measurements -> Normalization -> MEAN_ULLAGE_V1 -> EffectiveFillHeight -> Geometry Engine -> Product Volume -> Quantity Engine -> Physical Quantity.
- Surface model is LEVEL_BASED_V1; aggregation metadata is MEAN_ULLAGE with rule version 1 (combined rule identifier MEAN_ULLAGE_V1).
- Effective fill height is H_ref - effective ullage and must remain within 0..maximumFillHeightM; invalid values block calculation/validation and are never silently clamped.
- Ten frozen geometry families F01-F10 are supported. F08-F10 require verified constant area plus provenance.
- PhysicalQuantityKg = ProductVolumeM3 × ActualBulkDensityKgM3 for non-empty records.
- Canonical units: m, m², m³, kg/m³, kg. UI kg/hl converts deterministically using 1 kg/hl = 10 kg/m³.
- At least 15 significant digits are preserved during intermediate deterministic processing; no intermediate rounding. Persistence uses scale 6, ROUND_HALF_UP.
- CalculationResult preserves all version/fingerprint metadata necessary to reject stale calculations and reproduce history.


## 13. Issue and resolution data invariants

- IssueCategory is exactly: Measurement Problem; Access / Safety; Geometry Discrepancy; Commodity Uncertainty; Storage Space Condition; Other.
- Description is mandatory for every Issue category.
- Issue current status is exactly OPEN or RESOLVED. RESOLVED may transition back to OPEN before Campaign COMPLETED; the transition is audited and appended to IssueStatusEvent history.
- Resolution is explicit, reasoned and audited; IssueResolution history is retained.
- Admin Close follow-up acceptance is represented by an explicit OPEN -> RESOLVED action with reason/audit, not by a third Issue status.


## 14. Evidence data invariants

- Evidence types are PHOTO and NOTE.
- Evidence has exactly one principal owner: StocktakingRecord or Issue. Maximum five PHOTO per StocktakingRevision and maximum five PHOTO per Issue; NOTE does not consume the photo limit.
- Photos normalize to JPEG, EXIF orientation is corrected, long edge <=2048 px, target <=1.5 MB, hard max 3 MB, SHA-256 required.
- Binary data is app-private locally and object-storage-backed remotely; Room/PostgreSQL retain metadata/references/hash, not large image BLOBs.
- Offline save must durably persist private file + metadata/outbox before UI success.
- Evidence sync states are LOCAL_PENDING -> UPLOADING -> SYNCED, with SYNC_ERROR for failed transfer/retry.
- Validated historical evidence is read-only; unnecessary EXIF is stripped and GPS/device identifiers are not automatically collected.


## 15. Close and Final Snapshot invariants

- Close is Admin-only and online.
- Server revalidates all Close Guards atomically: Campaign Active, every scoped Storage Space final/Validated (including Mark Empty), no To Do work, mandatory data valid, critical operations synchronized and no Open Issues unless explicitly resolved for follow-up with reason/audit.
- Successful Close creates exactly one FinalInventorySnapshot, exactly one entry per scoped Storage Space, and transitions Campaign to COMPLETED in one logical transaction.
- Failure leaves Campaign ACTIVE and creates no partial final snapshot.
- Final entries freeze hierarchy, geometry/version, Commodity, Ownership/Client where applicable, empty state, measurements, density/operational values, volume, quantity, calculation metadata, validation actor/time, issue/evidence references and close context.
- Completed historical reports read this snapshot and never recalculate from current master data.


## 16. Offline working-set data model

- OfflineWorkingSetManifest is Campaign-scoped; arbitrary whole-Organization replication is not required.
- Required content includes campaign header/status/snapshot/scope, hierarchy, pinned geometry/profile/schema/Commodity/calculation compatibility data, team authorization, existing stocktaking, Ownership/Client data, required evidence, sync metadata and integrity manifest.
- OFFLINE_READY is derived only after complete authorized package download and integrity verification; it is not CampaignStatus.
- Package/manifests must support invalidation/refresh when authorization or compatible package version changes without erasing unsynchronized local work.


## 17. Sync and conflict data invariants

- Every supported offline-capable business mutation and its SyncOperation are committed atomically in one local transaction before UI success.
- SyncOperation state machine is PENDING -> IN_FLIGHT -> ACKNOWLEDGED with RETRYABLE_ERROR, CONFLICT and PERMANENT_ERROR branches.
- Retries reuse the same idempotencyKey; base/entity version is preserved.
- Dependent operations preserve explicit parent→dependent ordering through SyncOperationDependency; retry reuses the same SyncOperation and idempotencyKey.
- Critical inventory conflicts are not silently merged or last-write-wins. SyncConflict is OPEN/RESOLVED, preserves local attempted value and server authoritative value/version, and resolves only as RELOAD_AND_REENTER, DISCARD_LOCAL_ATTEMPT or NO_ACTION_REQUIRED.
- Validated/Completed/snapshot data cannot be overwritten by stale local state. There is no generic Force Overwrite.
- Sign Out clears/revokes session credentials but does not delete unsynchronized business operations.


## 18. Audit data model

- AuditEvent minimum fields are eventId, organizationId, actorUserId, action, entityType, entityId, timestampUtc, optional campaignId, relevant structured before/after/change metadata, correlationId and source.
- AuditEvent is append-only; users cannot edit or delete audit history.
- Audit references remain interpretable even if a User becomes Disabled or master data later becomes Inactive.
- Notifications are not AuditEvents and cannot substitute for audit history.


## 19. Notification data model

- Minimum notification types: SYNC_ERROR, SYNC_CONFLICT, CAMPAIGN_ASSIGNMENT, ISSUE_REQUIRES_ATTENTION, CAMPAIGN_COMPLETED.
- Notification stores read/unread state, timestamp, title, message and optional contextual reference/deep link.
- Deep-link navigation re-checks current authorization and entity state.
- Notification is not a source of truth; authoritative entity/workflow state controls behavior.
- Server-side retention is 12 months.


## 20. Preferences and Organization Configuration

- UserPreference includes Theme = SYSTEM/LIGHT/DARK and only notification preferences that correspond to actually configurable notification types.
- OrganizationConfiguration uses a platform-controlled approved-key registry with one typed scalar value per Organization×key. Supported types are BOOLEAN, INTEGER, DECIMAL, TEXT and ENUM; arbitrary JSON is prohibited. Empty/not-configured is valid.
- System-controlled rules are never modeled as user-editable OrganizationConfiguration: formulas, max measurements, rounding, sync interval, server URL, conflict strategy, retention, geometry rules, commodity governance, workflow statuses, authentication/platform limits.


## 21. Deletion, retention and historical-reference rules

| Data class | Minimum/behavior | Deletion rule |
| --- | --- | --- |
| FinalInventorySnapshot + entries | At least 10 years | Immutable; no hard delete inside retention; preserve official history. |
| CampaignSnapshot | At least 10 years | Immutable; preserve launch context. |
| Critical AuditEvent | At least 10 years | Append-only. |
| Official Evidence tied to validated/final history | At least 10 years | Binary/metadata cleanup only under approved retention and integrity rules. |
| Issue + IssueResolution history | At least 10 years | Preserve resolution history. |
| Notification | 12 months server-side | May expire after retention; not source of truth. |
| Acknowledged diagnostic/sync operations | May be cleaned after approved period (90 days online for synced diagnostics) | Unresolved failures/conflicts are not auto-deleted. |
| Master data | Lifecycle Active/Inactive | No hard delete when historically referenced. |


## 22. Derived/read models

The following are derived/read models, not additional authoritative entities. Their storage/materialization is an implementation choice for Documents 10–12 provided the authoritative sources remain unchanged.

| Read model | Authoritative source |
| --- | --- |
| Dashboard summaries | Authorized current local/server domain data. |
| Stocktaking list/classification projections | StocktakingRecord + Issue state + campaign scope; ALL remains UI-only. |
| Sync Status | SyncOperation/SyncConflict/evidence transfer state. |
| Nine report datasets | Active operational data where report contract permits; Completed official history from FinalInventorySnapshot. |
| Inventory History comparison | 2..N FinalInventorySnapshots with COMPARABLE/SCOPE_ADDED/SCOPE_REMOVED/CONTEXT_CHANGED. |
| Digital Inventory Dossier | Final snapshot + approved evidence/audit/context references. |
| Audit Trail view | AuditEvent. |


## 23. Canonical uniqueness constraints

| Entity/relationship | Logical uniqueness |
| --- | --- |
| Organization | organizationId; Code case-insensitive after normalization according to provisioning scope. |
| Location | (organizationId, normalized code). |
| StorageUnit | (locationId, normalized code). |
| StorageSpace | (storageUnitId, normalized code). |
| Client | (organizationId, normalized code). |
| User | email unique as authentication identifier. |
| OrganizationMembership | One logical membership per Organization×User; lifecycle toggles ACTIVE/DISABLED on the same identity. |
| InventoryCampaign | campaignId; Campaign Code unique within Organization + calendar year. |
| CampaignScope | campaignId + storageSpaceId. |
| CampaignTeamAssignment | At most one ACTIVE assignment per campaignId + userId; removed assignment history retained. |
| StocktakingRecord | campaignId + storageSpaceId. |
| FinalInventorySnapshot | campaignId exactly one after successful close. |
| FinalInventorySnapshotEntry | finalSnapshotId + scoped storageSpace identity. |
| StocktakingRevision | stocktakingRecordId + revisionNumber. |
| Measurement | stocktakingRevisionId + sequenceNumber. |
| OfflineWorkingSetManifest | campaignId + deviceInstallationId + packageVersion; one current per Campaign×device. |
| OrganizationConfiguration | organizationId + configurationKey. |


## 24. Required reference behavior

| Reference | On parent lifecycle/change |
| --- | --- |
| Historical snapshot -> User | Retain actor identity/reference even if User Disabled. |
| Historical snapshot -> Master data | Retain frozen display/context; do not depend solely on mutable current row. |
| Active Campaign -> scope/profile/commodity versions | Use frozen launch context; later master inactivation/change must not invalidate campaign. |
| Stocktaking -> Client | Stable Client ID; historical Third-Party meaning preserved after Client inactivation. |
| CalculationResult -> Geometry/Profile version | Immutable exact version; never repoint to current. |
| FinalSnapshotEntry -> CalculationResult/context | Freeze sufficient values/versions; history remains reproducible even if operational rows later age out. |


## 25. Data ownership and write authority

| Data | Primary write authority |
| --- | --- |
| User/Organization/Membership | Authorized server/platform administration; client UI constrained by permissions/provisioning. |
| Master data | Authorized Admin operations; server validates tenant/role/lifecycle. |
| GeometryFamily/Commodity definitions | Platform governance only. |
| GeometryProfile | Organization Admin; immutable versions created for calculation-affecting change. |
| Campaign create/scope/team/launch | Admin; official launch/code server authoritative. |
| Stocktaking/Measurement/Evidence/Report Issue | Authorized Campaign Team; offline-capable local-first where allowed. |
| Reset Validation | Admin only; audited. |
| Issue resolution | Authorized campaign user; Close follow-up acceptance Admin-only context. |
| Close/Final Snapshot | Server-authoritative Admin-only online transaction. |
| AuditEvent | System/server append-only. |
| SyncOperation/Conflict | Client/server sync infrastructure; user cannot arbitrarily edit payload/history. |


## 26. Cross-document handoff to physical schema

- Document 10 must map every authoritative entity/value defined here to Room/PostgreSQL persistence or explicitly document why a value is embedded/derived.
- Document 10 must choose exact SQL/Room names, physical types, PK/FK/indexes, nullability and deletion actions without weakening these semantic invariants.
- Document 11 must expose API DTOs/endpoints without leaking persistence representation as the domain contract.
- Document 12 must refine SyncOperation/SyncConflict/OfflineWorkingSetManifest lifecycle and cleanup.
- Document 13 must define exact GeometryProfileVersion parameter schemas and all F01-F10 formulas.
- Documents 14–20 may refine governed values, evidence, reporting, security, tests, retention and operations but may not contradict this model without approved change.


## 27. Frozen Data-Model Decision Register — DM-D01–DM-D64

All decisions below are **APPROVED / FROZEN V6** and are normative for this document and Documents 10–20.

| ID | Frozen decision |
| --- | --- |
| DM-D01 | OrganizationMembership has stable membershipId; one logical membership per Organization×User; reactivation reuses identity. |
| DM-D02 | OrganizationMembership lifecycle is ACTIVE/DISABLED; Disabled grants no access and history remains. |
| DM-D03 | Role ADMIN↔STANDARD may change by authorized Admin; audited; entityVersion increments; historical snapshots/audit unchanged. |
| DM-D04 | Server blocks disabling/demoting the last ACTIVE Admin of an Organization. |
| DM-D05 | Campaign Team is Admin-editable while Campaign is ACTIVE; Campaign Scope remains immutable. |
| DM-D06 | CampaignSnapshot preserves launch team; later team changes never rewrite launch snapshot. |
| DM-D07 | CampaignTeamAssignment lifecycle ACTIVE/REMOVED with assigned/removed actor+time; one ACTIVE assignment per Campaign×User. |
| DM-D08 | Removing team member immediately revokes new mutation authority server-side; captured data/audit remain; offline authorization invalidates on refresh/sync and unauthorized uploads are rejected. |
| DM-D09 | GeometryProfile Code required max50, trim+uppercase, case-insensitive unique within Organization. |
| DM-D10 | GeometryProfile Name required max150. |
| DM-D11 | Any calculation-affecting geometry change creates a new immutable GeometryProfileVersion. |
| DM-D12 | GeometryProfile is ACTIVE/INACTIVE; GeometryProfileVersion has no independent Active/Inactive lifecycle. |
| DM-D13 | StorageSpace references logical GeometryProfile; operational/campaign context pins exact GeometryProfileVersion. |
| DM-D14 | Max Capacity belongs to GeometryProfileVersion as calculated value; StorageSpace displays it and never edits it. |
| DM-D15 | Commodity is platform logical identity; Stocktaking non-empty pins CommodityVersion. |
| DM-D16 | New CommodityVersion becomes current for new operations; history retains old version. |
| DM-D17 | Exactly one immutable CampaignSnapshot per launched Campaign. |
| DM-D18 | CampaignSnapshot is an aggregate with explicit child collections/records where N; not one opaque JSON business object. |
| DM-D19 | OfflineWorkingSetManifest is versioned N per Campaign×DeviceInstallation. |
| DM-D20 | DeviceInstallation is a technical UUID identity; no IMEI/hardware/advertising ID. |
| DM-D21 | Manifest unique by Campaign×DeviceInstallation×packageVersion; one current manifest per Campaign×device. |
| DM-D22 | OFFLINE_READY is derived from current complete verified authorized manifest; never CampaignStatus. |
| DM-D23 | StocktakingRecord is stable root; immutable/working history is represented by StocktakingRevision. |
| DM-D24 | StocktakingRecord points to currentRevisionId; validated revisions are immutable. |
| DM-D25 | Reset Validation creates a new working StocktakingRevision based on prior validated revision; prior remains immutable. |
| DM-D26 | Measurement belongs to StocktakingRevision; max five per revision. |
| DM-D27 | Working revision has at most one current CalculationResult at a time; input change invalidates/replaces it; validated result freezes. |
| DM-D28 | No separate Validation entity; validation metadata lives on StocktakingRevision and AuditEvent records action. |
| DM-D29 | Mark Empty is a VALIDATED StocktakingRevision with isEmpty=true, 0 measurements, 0 volume/quantity, no Commodity/density/Ownership/Client requirement. |
| DM-D30 | Stocktaking Notes belong to StocktakingRevision. |
| DM-D31 | Issue belongs to StocktakingRecord/campaign-space context, not one revision, and survives Reset Validation. |
| DM-D32 | Issue may have N IssueResolution events. |
| DM-D33 | Issue RESOLVED→OPEN reopen is allowed before Campaign completion and audited; impossible after COMPLETED. |
| DM-D34 | IssueStatusEvent is append-only transition history; Issue.status is current projection. |
| DM-D35 | Evidence has exactly one principal owner: StocktakingRecord or Issue. |
| DM-D36 | PHOTO requires binary metadata; NOTE requires noteText and has no binary object. |
| DM-D37 | Stocktaking-owned Evidence binds to StocktakingRevision when included in a validated revision. |
| DM-D38 | Working Evidence may be removed by authorized actor before Validation; removal audited; validated Evidence immutable. |
| DM-D39 | Maximum five PHOTO per StocktakingRevision and five PHOTO per Issue; NOTE does not consume photo limit. |
| DM-D40 | SyncConflict lifecycle OPEN/RESOLVED; conflict evidence retained. |
| DM-D41 | Conflict resolution types exactly RELOAD_AND_REENTER, DISCARD_LOCAL_ATTEMPT, NO_ACTION_REQUIRED; no FORCE_OVERWRITE/AUTO_MERGE. |
| DM-D42 | One logical mutation creates one SyncOperation; retry updates same operation and reuses idempotencyKey. |
| DM-D43 | SyncOperationDependency explicitly models parent→dependent ordering. |
| DM-D44 | Binary Evidence transfer may use technical operation, but Evidence/business operations preserve dependency ordering. |
| DM-D45 | OrganizationConfiguration is typed key/value using platform-controlled approved-key registry; 0..1 value per Organization×key. |
| DM-D46 | Configuration value types: BOOLEAN, INTEGER, DECIMAL, TEXT, ENUM; arbitrary JSON prohibited. |
| DM-D47 | Configuration changes audited/versioned; campaign snapshot copies a value only when it affects reproducibility. |
| DM-D48 | One logical UserPreference set per User; user-scoped, not Organization-scoped. |
| DM-D49 | Notification preferences are per configurable NotificationType only; mandatory critical notifications cannot be disabled if classified mandatory. |
| DM-D50 | Audit actorUserId nullable only for SYSTEM actions; source=SYSTEM and systemActor required. |
| DM-D51 | Audited business mutations preserve relevant structured changed fields/before-after, excluding secrets/binary evidence. |
| DM-D52 | Exactly one FinalInventorySnapshotEntry per scoped StorageSpace; unique FinalSnapshot×StorageSpace. |
| DM-D53 | FinalSnapshotEntry freezes stable IDs/version IDs and human-readable values needed without current-master joins. |
| DM-D54 | FinalSnapshotEntry freezes measurements, calculation inputs/results/version metadata, not merely operational references. |
| DM-D55 | Final snapshot preserves Issue/Evidence references and required frozen metadata; evidence binaries remain in object storage. |
| DM-D56 | Third-Party snapshot freezes clientId, Client Code and Name. |
| DM-D57 | Historical user representation keeps stable userId and frozen display name/email where report/audit readability requires it. |
| DM-D58 | No generic deletedAt soft-delete for business/master/history; approved lifecycle states are used; hard delete only technical/ephemeral per retention. |
| DM-D59 | Dashboard/list/report/history/sync projections are read models, not authoritative business entities. |
| DM-D60 | 09 freezes semantic entities/value objects/cardinalities/invariants; 10 may choose physical mapping only and cannot change semantics. |
| DM-D61 | Location latitude/longitude are an optional authoritative facility coordinate pair: both null or both present; latitude -90..90 and longitude -180..180. |
| DM-D62 | Location coordinates are Organization-scoped mutable synchronized master data; Admin mutation increments entityVersion and is audited. |
| DM-D63 | Location facility coordinates are not device/user location, do not authorize GPS/background-location collection, and do not create a tracking entity. |
| DM-D64 | Location List/Map presentation is a read-model concern; no separate Map entity or map-tile business entity is introduced. |

## 28. Acceptance criteria

- **DM-01** All 35 canonical V6 authoritative logical entities/records are represented.
- **DM-02** Every tenant-specific business entity is directly or unambiguously Organization-scoped.
- **DM-03** Stable UUID technical identity is separate from business Code.
- **DM-04** Entities designated Mutable synchronized carry the complete canonical mutation/version metadata; entity-specific exceptions are explicit.
- **DM-05** Immutable versions/snapshots use creation/schema/version metadata and are not semantically updated.
- **DM-06** Master Active/Inactive and User Active/Disabled lifecycle preserve historical references.
- **DM-07** GeometryProfile and Commodity use immutable versions for historically relevant definitions.
- **DM-08** Campaign Code, type, dates, team minimum, frozen scope and CampaignSnapshot invariants are represented.
- **DM-09** Campaign×StorageSpace has at most one conceptual StocktakingRecord.
- **DM-10** Non-empty Stocktaking enforces 1–5 measurements, density, commodity, ownership and conditional Client.
- **DM-11** Mark Empty semantics are represented without inventing an Empty status.
- **DM-12** Reset Validation preserves prior validated history and creates a new working revision.
- **DM-13** Issue uses exactly OPEN/RESOLVED and the six frozen categories; resolution is explicit/audited.
- **DM-14** Evidence metadata/storage/sync/limits/privacy semantics are represented.
- **DM-15** CalculationResult carries deterministic units, versions, fingerprint and scale/rounding semantics.
- **DM-16** Successful Close yields exactly one immutable FinalInventorySnapshot with complete per-space entries.
- **DM-17** Completed history is reproducible without current master-data recalculation.
- **DM-18** OfflineWorkingSetManifest models complete campaign-scoped integrity verification; OFFLINE_READY is not CampaignStatus.
- **DM-19** SyncOperation models atomic outbox/idempotency/version/order/retry semantics.
- **DM-20** SyncConflict preserves local attempt and server authoritative value; no force overwrite/auto merge.
- **DM-21** AuditEvent contains the complete frozen minimum event contract and is append-only.
- **DM-22** Notification types/read state/context are represented and remain distinct from Audit.
- **DM-23** UserPreference/OrganizationConfiguration do not expose system-controlled rules.
- **DM-24** Logical uniqueness and cardinality constraints cover all frozen identity relationships.
- **DM-25** No destructive cascade/hard-delete rule can invalidate snapshots/history.
- **DM-26** Physical schema details may vary only where they do not alter the semantic contract frozen here.
- **DM-27** DM-D01–DM-D64 are represented without unresolved data-model decisions.
- **DM-28** Campaign Team post-launch lifecycle/history and last-Admin protection are represented.
- **DM-29** StocktakingRevision, IssueStatusEvent, DeviceInstallation and SyncOperationDependency are explicit authoritative records.
- **DM-30** No later document is required to decide a business cardinality, lifecycle, ownership rule or revision invariant that belongs to the V6 data model.
- **DM-31** Location coordinate pair semantics/ranges/authority are closed and no implementation-time product decision is required for the approved map capability.
- **DM-32** User and OrganizationMembership are distinct authoritative entities with distinct canonical metadata/version provenance; transport projections must not synthesize one entity's metadata from the other.


## 29. Freeze statement

This document is the canonical APPROVED / FROZEN V6 logical data model after the 2026-10-17 implementation-closure refreeze. All semantic data-model decisions required to proceed to the physical schema are closed by Documents 01–08 and DM-D01–DM-D64. Document 10 may select physical persistence structures, SQL/Room names, concrete scalar storage types, indexes and FK mechanics only where those choices preserve every entity meaning, value contract, cardinality, uniqueness rule, lifecycle, ownership rule, revision boundary, tenant boundary, snapshot rule and historical-integrity invariant defined here. It may not invent or change business semantics. Adding/removing an authoritative entity or changing these semantics requires an approved V6 change.

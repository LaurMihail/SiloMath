# SiloMath V6 — Architecture

Document ID: 07_ARCHITECTURE

Version: V6 Final Documentation — Architecture 1.0

Status: APPROVED / FROZEN V6

Baseline revision: 2026-10-20 — CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze

Product: SiloMath

Normative role: Canonical system/software architecture contract for the SiloMath V6 Android client and its interaction boundaries with backend services, local persistence, synchronization, calculation, evidence storage, reporting, security, audit and operations. Detailed Gradle/module decomposition is specified in 08_MODULE_ARCHITECTURE; exact logical data structures, database schema and HTTP payloads are specified in documents 09–11.

## 1. Purpose and authority

This document defines how the frozen V6 product contract is implemented architecturally. It translates the approved product baseline, decisions, business rules, authorization model, workflows and UI/navigation contract into enforceable software boundaries. It does not reopen product decisions and does not introduce alternative workflows.

If a lower-level implementation choice conflicts with the frozen V6 product contract, the product contract wins. The V6 baseline is implementation-ready: any non-semantic technical choice not explicitly fixed by Documents 01–21 is an implementation decision that may be selected without Product/Baseline approval when it preserves all frozen behavior and boundaries. Architecturally material choices are recorded through IADR. Only an intentional change to frozen product behavior/structure requires Baseline Change; missing product semantics must never be invented in code.

| Input document | Architectural use |
| --- | --- |
| 01_PRODUCT_BASELINE | Product scope, canonical terminology, technology baseline, offline-first and UI guardrails. |
| 02_DECISION_REGISTER | D01–D36 and frozen decisions governing campaign, stocktaking, close, reports, offline/sync, calculation, retention and operations. |
| 03_BUSINESS_RULES | Normative domain invariants enforced across UI, domain and server boundaries. |
| 04_USER_ROLES_PERMISSIONS | Authorization dimensions and operation-level permission contract. |
| 05_WORKFLOWS_STATE_MACHINES | Canonical state transitions and workflow guards. |
| 06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION | 43-component UI contract, field/control contract, navigation and adaptive behavior. |
| FC-01–FC-11 / IC-01–IC-17 | Approved field and implementation-critical clarifications incorporated into V6 architecture. |

## 2. Scope and architectural boundaries

### 2.1 In scope

- Native Android client in Kotlin and Jetpack Compose.
- Authentication/session restoration and organization context.
- Master data: Organization, Location, Storage Unit, Storage Space, Geometry Profile and Client; platform-governed Geometry Families and Commodities.
- Inventory Campaign creation/launch, frozen scope/team and campaign snapshot.
- Offline-first Stocktaking, measurements, calculation, ownership/client, evidence, validation, Mark Empty and issues.
- Campaign Review, online atomic Close and immutable Final Inventory Snapshot.
- Nine canonical report types, Inventory History, Digital Inventory Dossier and Audit Trail.
- Notifications, Settings, Sync Status, Support and user administration.
- Local persistence, durable outbox, synchronization, conflict handling and campaign offline working set.
- Security, auditability, observability, migrations, release/operations and architectural quality gates.

### 2.2 Explicitly out of scope

- ERP/book-stock reconciliation and AJG justification workflows.
- Web, desktop, iOS or multiplatform client implementations.
- Customer-editable calculation rules, Geometry Families or Commodity definitions.
- A generic force-overwrite conflict mechanism.
- A Draft/Upcoming/In Progress campaign lifecycle.
- Any extra screen, sheet, role, campaign status or stocktaking status not in the frozen V6 contract.

## 3. Architecture drivers and non-functional requirements

| ID | Driver / requirement | Architectural consequence |
| --- | --- | --- |
| AR-NFR-01 | Assigned Stocktaking must remain usable without network. | Room is the client read source; complete campaign working set is downloaded before OFFLINE_READY. |
| AR-NFR-02 | No acknowledged local field work may be lost after process death. | Business mutation and SyncOperation are committed in one Room transaction before UI success. |
| AR-NFR-03 | Synchronization must be retry-safe. | Client operation UUID/idempotency key, durable queue and server idempotency. |
| AR-NFR-04 | Inventory calculations must be reproducible. | Pure calculation engine, frozen geometry/profile versions, versioned algorithms and persisted calculation metadata. |
| AR-NFR-05 | Historical inventory is immutable. | No destructive cascade; immutable snapshots; historical references never recomputed from current master data. |
| AR-NFR-06 | Authorization is enterprise-grade. | Organization membership, role, Campaign Team, entity state and connectivity guards; server re-authorizes protected remote operations. |
| AR-NFR-07 | Android UI must adapt. | Compact/Medium/Expanded window-aware Compose layouts support orientation, resize, multi-window and supported posture changes; no fixed-device assumptions. |
| AR-NFR-08 | Security and privacy by design. | Secure session storage, TLS, least privilege, sanitized diagnostics, EXIF stripping and no plaintext secrets. |
| AR-NFR-09 | Production schema evolution must preserve data. | Forward-safe migrations; no destructive production migration; migration tests. |
| AR-NFR-10 | Operational integrity takes precedence over availability. | Critical invalid/ambiguous actions block rather than silently guess, clamp or overwrite. |
| AR-NFR-11 | Operational performance must scale to large facilities. | Indexed Room queries, lazy lists, cursor-paged server collections and bounded sync batches. |
| AR-NFR-12 | Traceability is mandatory. | Requirement/decision → business rule → UI → domain → persistence/API → test. |

## 4. System context

```text
[ User / Android Device ]
          |
          v
[ SiloMath Android Client ]
  |  Compose + ViewModels
  |  Domain / Use Cases
  |  Repositories
  |---- Room / SQLite  <--- client-side source of truth
  |---- App-private evidence files
  |---- DataStore / secure session facilities
  |---- WorkManager durable sync
          |
          | HTTPS / REST
          v
[ Backend API / Identity / Authorization ]
  |---- PostgreSQL system of record
  |---- Object storage for evidence binaries
  |---- Reporting / Audit / Close services
```

The backend is the enterprise authoritative system of record. On Android, Room is the canonical source from which operational UI reads are produced. Network responses update local state; screens do not render directly from transient HTTP responses. Server authority is mandatory for authentication, privileged mutations, conflict decisions, Campaign Code generation and Campaign Close.

## 5. Architectural style and dependency direction

SiloMath uses layered Clean Architecture with MVVM/UDF at the presentation boundary and offline-first repositories at the data boundary.

```text
Compose UI
   ↓ UserAction                 ↑ immutable UiState / StateFlow
Screen ViewModel
   ↓
Use Cases / Domain Services
   ↓
Repository Interfaces
   ↓
Repository Implementations
   ├── Local Data Sources (Room / files / DataStore)
   └── Remote Data Sources (Retrofit / evidence transfer)
              ↓
          Backend API
```

| Principle | Mandatory rule |
| --- | --- |
| Local-first reads | Operational UI observes local repository state; it does not depend on a live network response. |
| UDF | Compose emits actions; ViewModel owns screen state coordination; state is immutable. |
| Dependency inversion | Domain and presentation depend on abstractions, not Room/Retrofit implementations. |
| No business logic in Compose | Validation, calculations, state transitions and permissions are not implemented in composables. |
| Pure domain calculations | Calculation engine has no Android, Compose, Room, Retrofit, Context or lifecycle dependency. |
| Stable boundaries | Network DTOs, Room entities and domain models are separated when concerns differ. |
| No cross-feature internals | Features communicate through stable core/domain contracts and navigation IDs, not by reaching into another feature implementation. |

## 6. Technology baseline

| Concern | V6 technology / rule |
| --- | --- |
| Platform | Android native; minimum API 26. |
| Language | Kotlin. |
| UI | Jetpack Compose + Material 3. |
| Presentation | MVVM + Unidirectional Data Flow; StateFlow. |
| Dependency injection | Hilt. |
| Local database | Room / SQLite. |
| Network | Retrofit + OkHttp over HTTPS. |
| Concurrency | Kotlin Coroutines + Flow. |
| Background work | WorkManager. |
| Preferences | Jetpack DataStore for small non-secret preferences. |
| Navigation | Jetpack Navigation 3; single-activity Compose application; type-safe serializable destination keys. |
| Backend expectation | Versioned REST services backed by PostgreSQL. |
| Evidence remote storage | Object storage referenced by metadata/object key. |
| Serialization | One project-wide typed serializer selected and used consistently. |
| Build configuration | Gradle Kotlin DSL/version catalog; environment-specific non-secret configuration. |

Library versions are implementation details managed centrally. Dependency/version/provider choices that preserve this architecture do not require Product/Baseline approval. An architectural-role change is recorded through IADR when it does not change frozen behavior; a role change that alters frozen behavior requires Baseline Change.

## 7. Application composition and module boundary

The frozen target decomposition is intentionally coarse enough to avoid micro-modularization. Document 08 defines exact Gradle dependencies and package ownership.

| Category | Target modules |
| --- | --- |
| Composition | :app |
| Core | :core:domain, :core:model, :core:common, :core:database, :core:network, :core:sync, :core:designsystem, :core:navigation, :core:testing |
| Features | :feature:auth, :feature:dashboard, :feature:masterdata, :feature:campaigns, :feature:stocktaking, :feature:close, :feature:reports, :feature:administration |
| Critical pure domain | :calculation-engine |

- Feature modules may depend on approved Core modules and domain abstractions.
- Core modules must not depend on Feature modules.
- The app module composes navigation and dependency injection and contains minimal business logic.
- No module is created merely because there is a screen, entity or table.
- Migration to this structure is incremental and must preserve working behavior.

## 8. Presentation architecture

Compose collects ViewModel state lifecycle-aware. Inactive destinations must not keep unnecessary UI-bound upstream collection active. Durable business results and critical success/failure states are represented in UiState/domain state; ephemeral navigation or presentation commands cannot become an alternative source of business truth.


### 8.1 Screen contract

- Every major screen has a screen-level ViewModel unless it is a purely stateless composition owned by a parent contract.
- Reusable visual components do not own independent business ViewModels.
- ViewModel exposes immutable UiState through StateFlow and accepts explicit user actions/events.
- One-off navigation/UI effects are separated from durable business state.
- A successful business operation is represented by persisted/domain state, not only by a transient snackbar/event.

### 8.2 UI field/control contract

The exhaustive field/control definitions live in Document 06. Architecture enforces them through typed UI models, local input parsing, domain validation and permission/state gating. Required/read-only/conditional fields are not inferred ad hoc in Compose.

### 8.3 Form behavior

- Numeric fields use appropriate keyboards and visible units.
- Editable and read-only values are visually distinguishable.
- Local syntactic errors are shown inline; domain/business validation remains authoritative in the domain/server layers.
- Primary actions must not bypass validation or disappear behind system insets.
- Critical statuses always include text; color is supplementary.

## 9. Navigation architecture

Navigation uses a single-activity Jetpack Navigation 3 stack. Destination keys are type-safe and serializable; arguments pass stable IDs rather than serialized mutable business objects. The operational shell has exactly five top-level destinations: Dashboard, Inventory Campaigns, Stocktaking, Locations and More. Authentication is outside the operational shell. System back uses supported Jetpack/system APIs and predictive back where available. Back-stack/state restoration must survive configuration change and process recreation as required by the workflow. Custom back interception is limited to explicit frozen workflow guards.

- Routes carry stable IDs only; large domain objects are never serialized into routes.
- Destinations reload data through repositories using IDs and current authorized organization/campaign context.
- Deep links must pass authentication and authorization checks before destination content is exposed.
- All Compose destinations are edge-to-edge safe. Layouts handle system bars, display cutouts, gesture insets and IME insets; required actions, validation messages and critical values may not be obscured.
- STK-05 → STK-03 back navigation restores Storage Unit context and relevant list/scroll position.
- Campaign Scope Selector and Team Assignment remain contextual components of CAM-03, not independent destinations.
- There is no separate route for Issues, Measurements, Calculation, Validation, Offline Working Set, Evidence Gallery, About or Sync Conflict Details.

## 10. Domain architecture

The domain layer owns business meaning, invariants, state transitions and deterministic calculations independent of Android framework concerns.

| Domain family | Canonical entities / concepts |
| --- | --- |
| Identity | User, Organization, OrganizationMembership, Role. |
| Master data | Location, StorageUnit, StorageSpace, Client. |
| Geometry | GeometryFamily, GeometryProfile, GeometryProfileVersion. |
| Commodity | Commodity, CommodityVersion. |
| Campaign | InventoryCampaign, CampaignScope, CampaignTeamAssignment, CampaignSnapshot, OfflineWorkingSetManifest. |
| Stocktaking | StocktakingRecord, Measurement, CalculationResult, Issue, IssueResolution, Evidence. |
| Close | FinalInventorySnapshot, FinalInventorySnapshotEntry. |
| Platform | SyncOperation, SyncConflict, AuditEvent, Notification, UserPreference, OrganizationConfiguration. |
| Read models | Report datasets, Inventory History/comparability projections, dashboard/status projections. |

### 10.1 Value objects and canonical types

- Stable UUID identity types at domain boundaries where useful.
- Decimal/typed values for Distance, Area, Volume, Mass, Density and Percentage.
- Canonical enums are closed to the V6 values defined in Documents 01–05.
- Calendar dates (Start Date / Planned End Date) are date-only values; instants/audit timestamps are UTC.
- Codes are business identifiers, not relational identities. UUID remains the technical identity.

### 10.2 Use-case boundary

| Use case family | Responsibility |
| --- | --- |
| Master data | Create/edit/activate/inactivate governed customer master data with authorization and referential protection. |
| Campaign | Create Campaign, resolve active Organization, configure scope/team, request server Campaign Code, Save & Launch and freeze snapshot. |
| Offline package | Prepare/verify assigned Campaign working set and transition to OFFLINE_READY only after integrity checks. |
| Stocktaking | Record measurements/data, Mark Empty, attach evidence, calculate, Validate, Reset Validation (Admin/audited) and Report/Resolve Issue. |
| Close | Evaluate Close Guards; request server-authoritative atomic Close. |
| Reporting | Build/read authoritative report datasets and snapshot-based exports without recalculating history. |
| Administration | User lifecycle, notifications, settings/configuration and audit access under permissions. |

## 11. Data architecture and source-of-truth rules

### 11.1 Repository boundary

Repositories are the only public application-data boundary for ViewModels/use cases. A network-backed repository coordinates local and remote data sources; upper layers do not decide whether to read Room or call Retrofit.

### 11.2 Client source of truth

- Room is the canonical client-side read source for operational entities.
- Remote refresh writes validated responses to Room transactionally, then UI reacts to Flow emissions.
- App-private files hold local evidence binaries; Room holds metadata, hash, ownership/context and sync state.
- DataStore holds small user/application preferences; it is not used as a relational store or credential vault.
- Secure session/token material uses Android security facilities/Keystore-backed protection, not plain DataStore values.

### 11.3 Identity, tenancy and metadata

- All tenant business data is explicitly organization-scoped.
- Technical IDs are UUIDs.
- Mutable synchronized entities carry createdAt, createdBy, updatedAt, updatedBy and entityVersion as applicable.
- Immutable snapshots carry creation actor/time and schema/version identifiers rather than mutable update semantics.
- Codes are normalized trim + uppercase; code uniqueness is case-insensitive after normalization; names preserve user capitalization and may duplicate when code differs.

### 11.4 Referential integrity

No destructive cascade may remove operational or historical facts. Referenced Organization/Location/Storage Unit/Storage Space/Geometry Profile/Commodity/Client/User identities remain resolvable for history. Inactive master data is unavailable for new configuration but remains readable wherever historical/current frozen context references it.

## 12. Offline-first architecture

### 12.1 Campaign-scoped working set

- Offline data is campaign-scoped; the client does not replicate the entire Organization by default.
- OFFLINE_READY is granted only after the complete assigned Campaign package and manifest pass integrity/authorization checks.
- Package contains the hierarchy, frozen geometry/profile versions, required Commodity/version data, team/authorization context and other data needed for assigned Stocktaking.
- A downloaded working set remains usable without requiring server pagination for the next operational item.

### 12.2 Critical local write protocol

```text
User action
  ↓
Domain validation / authorization precheck
  ↓
ROOM TRANSACTION
  ├── mutate business entity / append local evidence metadata
  └── INSERT SyncOperation with stable idempotency key
  ↓ commit
UiState reflects success / pending sync
  ↓
WorkManager → API → acknowledgement / conflict / classified failure
```

If the transaction does not commit, the UI must not report success. A network failure after commit does not discard the local mutation.

### 12.3 Sync states

| State | Meaning |
| --- | --- |
| PENDING | Durably queued, not currently being transmitted. |
| IN_FLIGHT | Transmission in progress. |
| ACKNOWLEDGED | Server accepted the operation. |
| RETRYABLE_ERROR | Transient failure; same idempotency key is reused under backoff. |
| CONFLICT | Optimistic-concurrency/business conflict requiring controlled handling. |
| PERMANENT_ERROR | Non-retryable validation/authorization failure requiring remediation. |

### 12.4 Ordering and retries

- FIFO is preserved for dependent operations on the same entity/stream.
- Independent streams may synchronize concurrently when safe.
- WorkManager uses durable unique work, network constraints and exponential backoff.
- Critical mutations are not coalesced in ways that erase audit meaning.
- Retry uses the same operation/idempotency identity; a retry must not create a duplicate server business action.

## 13. Conflict architecture

Generic last-write-wins and Force Overwrite are prohibited for audit-critical data.

| Entity/state | Conflict rule |
| --- | --- |
| Mutable master data | Server authoritative on rejected stale mutation; preserve local attempted change/context and refresh authoritative state. |
| Campaign scope/team after launch | Immutable; conflicting mutation rejected. |
| Measurement before validation | Optimistic concurrency via entityVersion; conflict is surfaced, not auto-merged. |
| Validated Stocktaking | No ordinary edit. Admin Reset Validation is explicit/audited and creates a new working revision while preserving prior validation history. |
| Completed Campaign / Final Snapshot | Immutable; client overwrite prohibited. |
| Geometry/Commodity versions referenced historically | Immutable/versioned; new definitions create new versions rather than rewriting history. |
| Audit events | Append-only; never merged destructively. |

Conflict resolution retains enough local attempted value and server-authoritative value to explain the conflict. The user reloads authoritative state and re-enters a change if it remains valid. Resolution is audited.

## 14. Calculation architecture

The calculation engine is a pure Kotlin domain component and the most strongly isolated computational subsystem.

### 14.1 Canonical pipeline

```text
Measurements → Normalization → Aggregation → EffectiveFillHeight → GeometryEngine → Volume → QuantityEngine → PhysicalQuantity
```

- Surface model: LEVEL_BASED_V1.
- Aggregation: MEAN_ULLAGE_V1; 1–5 readings; same datum/profile version/type and normalizable units.
- h = H_ref − u_eff; no silent clamping.
- V = V_family(h).
- PhysicalQuantityKg = ProductVolumeM3 × ActualBulkDensityKgM3.
- Mark Empty bypasses measurement/density requirements and produces explicit zero volume/quantity with EMPTY snapshot semantics.

### 14.2 Geometry families

| Code | Family |
| --- | --- |
| F01 | CYL_TRUNC_CONE_V1 |
| F02 | CYL_FLAT_V1 |
| F03 | SQUARE_FLAT_V1 |
| F04 | RECT_FLAT_V1 |
| F05 | SQUARE_PYRAMID_HOPPER_V1 |
| F06 | RECT_PYRAMID_HOPPER_V1 |
| F07 | REG_HEX_FLAT_V1 |
| F08 | STAR_VERIFIED_AREA_V1 |
| F09 | POCKET_VERIFIED_AREA_V1 |
| F10 | PERIMETER_VERIFIED_AREA_V1 |

### 14.3 Numerical contract

- Canonical units: m, m², m³, kg/m³ and kg.
- 1 kg/hl = 10 kg/m³. UI may display kg/hl where operationally appropriate and kg/t for physical quantity presentation.
- Intermediate arithmetic uses deterministic decimal precision of at least 15 significant digits where specified; no intermediate rounding.
- Persistence/presentation boundary uses scale 6 and ROUND_HALF_UP for governed persisted quantities.
- Geometry parameters are displayed with two decimals in Storage Space details.
- F08–F10 require verified area and provenance. Missing/invalid required formula data blocks calculation; no invention.

### 14.4 Reproducibility metadata

- Geometry family/profile/version and frozen dimensions.
- Raw and normalized measurements; aggregation method/version.
- Measurement datum/reference metadata.
- Actual Bulk Density input and normalized density.
- Calculation/engine/formula version and rounding policy.
- Result volume/quantity and validation actor/time where applicable.
- A deterministic fingerprint sufficient to detect stale calculation inputs.

## 15. Campaign architecture

### 15.1 Create & Launch

- CAM-03 is Admin-only.
- Campaign Code is server-generated, read-only and formatted INV-YYYY-NNNN; uniqueness is Organization + calendar year and allocation is atomic.
- Inventory Type is exactly Annual, Monthly or Control.
- Start Date and Planned End Date are mandatory; Planned End Date ≥ Start Date.
- Campaign Team contains at least one member; launching Admin need not belong to the team.
- Scope and Team are configured inside CAM-03 and scope becomes immutable after launch.
- Save & Launch produces an Active Campaign; there is no Draft state.

### 15.2 Campaign snapshot

Launch creates the stable operational context required for reproducibility. The snapshot preserves campaign identity/configuration, Organization, exact scope/hierarchy context, geometry family/profile/version and calculation parameters, relevant Commodity identity/version data, team, Inventory Type, dates and required schema/calculation version identifiers.

## 16. Stocktaking architecture

### 16.1 Record identity and classifications

- At most one StocktakingRecord exists for each Campaign × Storage Space.
- Operational classifications are To Do, Issues and Validated; All is a UI filter, not a stored lifecycle state.
- Issue lifecycle is independent: an Open Issue can coexist with a Validated record because Validate/Mark Empty never auto-resolve issues.

### 16.2 Non-empty path

- 1–5 valid measurements.
- Commodity required.
- Actual Bulk Density required and > 0.
- Ownership required: Owned or Third-Party.
- Client required only for Third-Party.
- Geometry is read-only during Stocktaking. Geometry anomaly is reported through Report Issue.
- Calculation must be current/valid before Validate.

### 16.3 Mark Empty

Mark Empty is a workflow, not a fourth stocktaking status. It creates a Validated result with isEmpty=true, Product Volume=0 and Physical Quantity=0. Measurements, Commodity, density, Ownership and Client are not required. Actor/time and retained notes/photos remain auditable.

### 16.4 Reset Validation

A Validated record is read-only in the ordinary workflow. Before Campaign Close, Admin may execute explicit audited Reset Validation. The prior validation revision/history is preserved and a new working revision is created. Standard users cannot reset validation. Completed Campaign data cannot be reset or edited.

## 17. Issue architecture

| Concern | V6 rule |
| --- | --- |
| Status | Exactly Open / Resolved. |
| Categories | Measurement Problem; Access / Safety; Geometry Discrepancy; Commodity Uncertainty; Storage Space Condition; Other. |
| Description | Mandatory for every Issue. |
| Reporting | Campaign Team member in authorized active context; offline creation allowed. |
| Resolution | Explicit and audited; Validate/Mark Empty do not auto-resolve. |
| Close follow-up | Admin may accept unresolved matter only by explicitly transitioning Open → Resolved with reason/audit as part of approved follow-up handling. |
| History | Issue and resolution history retained; no destructive rewrite. |

## 18. Evidence and binary-storage architecture

| Concern | Contract |
| --- | --- |
| Evidence types | PHOTO and NOTE. |
| Limits | Maximum 5 photos per Stocktaking Record and maximum 5 photos per Issue. |
| Photo normalization | JPEG, EXIF orientation corrected, long edge 2048 px, target ≤ 1.5 MB, hard maximum 3 MB. |
| Integrity | SHA-256 hash. |
| Privacy | Strip unnecessary EXIF; do not automatically retain GPS/device identifiers. |
| Local storage | App-private binary storage; Room stores metadata/local reference. |
| Remote storage | Object storage; database stores object reference/metadata, not large binary JSON payload. |
| Offline save | Binary durable locally → metadata/outbox in transaction → UI success. |
| Evidence sync states | LOCAL_PENDING → UPLOADING → SYNCED; failures use SYNC_ERROR. |
| Validated evidence | Read-only/immutable through normal workflow. |

## 19. Inventory Close and snapshot architecture

Close is a privileged online server transaction. Client prechecks improve UX but are not authoritative.

### 19.1 Close Guards

- Admin authorization and online connectivity.
- Campaign is Active.
- All scoped Storage Spaces are final/Validated, including Mark Empty results.
- No To Do work remains.
- Mandatory record data is valid.
- Required critical operations are synchronized.
- Open Issues are resolved, including explicit audited follow-up acceptance where used.
- Server revalidates all guards atomically immediately before Close.

### 19.2 Atomic close result

- Success creates exactly one immutable Final Inventory Snapshot.
- Success transitions Campaign Active → Completed.
- Close actor/time is persisted.
- Any failure leaves Campaign Active and creates no partial final snapshot.
- Final Snapshot contains per-space hierarchy/frozen geometry, Commodity, Ownership/Client, empty state, measurements, operational values, volume/quantity, calculation metadata, validation actor/time, Issue/evidence references and close metadata.
- Completed historical results are never recalculated from current master data.

## 20. Reporting architecture

Reporting uses a Single Authoritative Report Dataset so UI and exports do not implement divergent business logic.

| Report | Architecture source |
| --- | --- |
| Physical Inventory | Authoritative report dataset; Final Snapshot for completed official output. |
| Inventory by Location | Same authoritative dataset, grouped/projected by Location. |
| Inventory by Commodity | Same authoritative dataset, grouped/projected by Commodity. |
| Inventory by Ownership | Same authoritative dataset, grouped/projected by Ownership/Client context. |
| Storage Space Worksheet | Per-space snapshot/operational facts as permitted by report state. |
| Capacity Report | Geometry/profile capacity facts with governed units. |
| Inventory History | 2..N Final Snapshots with comparability classification. |
| Digital Inventory Dossier | Snapshot/report facts plus approved evidence/document references. |
| Audit Trail | Server-authoritative append-only audit facts; Admin-only UI. |

- Completed official reports read Final Inventory Snapshot; they never recalculate from current master data.
- Active-campaign monitoring must be visibly non-final.
- Standard export formats are PDF + CSV where the report contract supports tabular export.
- Digital Inventory Dossier uses PDF dossier plus approved attachments/evidence references/content as specified by its report contract.
- Inventory History classifications: COMPARABLE, SCOPE_ADDED, SCOPE_REMOVED, CONTEXT_CHANGED; no forecasting.

## 21. Authentication, session and authorization architecture

### 21.1 Authentication/session

- No public Sign Up, Remember Me, Google SSO or Microsoft SSO in V6.
- Email/password Sign In, Forgot Password and Reset Password are the UI contract.
- Password policy is server-controlled; Android displays/enforces server-defined requirements without inventing a second policy.
- Session/token lifetime is server-controlled; secure automatic restoration/refresh is used where supported.
- No new offline authentication authority is created. Offline continuation is only within previously authorized cached context according to offline policy.
- Sign Out removes/revokes local session credentials as supported but does not destroy unsynchronized business data.

### 21.2 Authorization dimensions

- Authenticated Active user.
- Valid Organization membership and authorized organization/data scope.
- Role: exactly Admin or Standard.
- Campaign access and Campaign Team membership for Stocktaking mutations.
- Entity/workflow state (Active/Completed, Open/Resolved, Validated etc.).
- Connectivity/sync guard where operation requires online authority.
- Server-side authorization for every protected remote mutation.

### 21.3 Organization provisioning

An Admin may edit Organizations for which Admin membership is authorized. Creation of a new tenant Organization is available only in an authorized platform provisioning/onboarding context; ordinary Organization Admin status does not grant arbitrary tenant creation. Initial Admin membership is established by the provisioning process.

## 22. Security and privacy architecture

| Area | Mandatory control |
| --- | --- |
| Transport | HTTPS/TLS only in Production; cleartext disabled. |
| Secrets | No plaintext passwords/tokens/secrets in source, BuildConfig committed values, logs or analytics. |
| Credential protection | Android Keystore-backed mechanisms for token/key protection. |
| Local data | Android app sandbox baseline; stronger DB encryption only through approved threat-model/ADR. |
| Authorization | Client gating is UX only; server is authoritative. |
| Evidence privacy | Strip unnecessary EXIF; no automatic GPS/device identifiers. |
| Logging | Sanitized structured logs; no raw authentication tokens or sensitive full business payloads. |
| Backups | Android backup/export configuration must not unintentionally expose offline campaign data. |
| Tenant isolation | Every tenant business query/mutation is organization-scoped and authorized. |
| Integrity | When integrity and availability conflict for close/snapshot/calculation, integrity wins. |

## 23. Audit architecture

Audit is a cross-cutting append-only domain capability. The Audit Trail UI is only a reader of audit facts; it is not the audit mechanism.

| Audit field | Contract |
| --- | --- |
| eventId | Stable UUID. |
| organizationId | Tenant scope. |
| actorUserId | Actor identity; preserved even if user later Disabled. |
| action | Stable machine-readable action. |
| entityType / entityId | Affected business identity. |
| timestampUtc | Authoritative UTC instant. |
| campaignId | Optional campaign context. |
| change metadata | Structured before/after or equivalent changed-field metadata where relevant and safe. |
| correlationId | Trace across client/API/server where applicable. |
| source | Origin such as Android/server/system process. |

- Audit events cannot be edited/deleted by normal users.
- Master-data mutation, Campaign launch, Validate/Mark Empty/Reset Validation, Issue resolution, Close and conflict resolution are auditable.
- Local pending audit facts, if shown, must be distinguishable from server-confirmed audit facts.

## 24. Notification architecture

| Type | Purpose |
| --- | --- |
| SYNC_ERROR | Actionable synchronization failure. |
| SYNC_CONFLICT | Conflict requiring controlled attention. |
| CAMPAIGN_ASSIGNMENT | User assigned to Campaign/team context. |
| ISSUE_REQUIRES_ATTENTION | Issue requires user/admin attention. |
| CAMPAIGN_COMPLETED | Campaign successfully closed/completed. |

- Notification state is Unread/Read with timestamp, title/message and optional contextual deep link.
- Notification is not a source of truth; destination entity/workflow state is authoritative.
- Deep links re-check authentication and authorization.
- Notification retention follows the approved 12-month policy.

## 25. Settings/configuration architecture

| Class | Ownership / storage / editability |
| --- | --- |
| User Preference | User-controlled; small preferences in DataStore where appropriate. Theme supports System / Light / Dark. |
| Organization Configuration | Admin-controlled only when an approved organization-level configuration exists in the domain contract. Empty/not-configured state is valid. |
| System-Controlled Rule | Not editable in Settings: calculation method, max measurements, rounding, sync strategy, server URL, conflict strategy, retention, Geometry Family rules, Commodity governance. |

Production server URL/environment cannot be changed by an end user. Settings must not expose Force Overwrite, disable-sync, custom sync interval or arbitrary server endpoints.

## 26. Time, units and formatting architecture

| Concern | Rule |
| --- | --- |
| Instants | Persist/transmit in UTC; API uses ISO-8601. |
| UI timestamps | Display in device/user local timezone while retaining authoritative UTC instant. |
| Calendar dates | Start Date and Planned End Date are date-only; no timezone conversion. |
| Length/area/volume | m / m² / m³. |
| Density | Canonical kg/m³; operational kg/hl display/input where specified, converted deterministically. |
| Mass | Canonical persistence kg; UI may display kg or t deterministically. |
| Geometry display | Two decimals in Storage Space details. |
| Persistence precision | NUMERIC(20,6), scale 6, ROUND_HALF_UP at governed boundary; no intermediate rounding. |

## 27. API boundary architecture

Exact endpoints and payloads belong to Document 11. Architecture mandates the following properties.

- Versioned REST over HTTPS.
- Stable UUID identifiers and explicit organization scope.
- Server-generated Campaign Code.
- Idempotency key on retryable mutations.
- entityVersion/optimistic concurrency on mutable synchronized entities.
- UTC ISO-8601 timestamps.
- Cursor pagination for large server collections.
- Campaign-scoped offline package/manifest endpoint or equivalent contract.
- Controlled evidence binary upload separate from ordinary JSON sync payloads.
- Atomic server Close endpoint.

### 27.1 Error envelope

```text
{
  "code": "STABLE_MACHINE_CODE",
  "message": "User-safe/server message",
  "fieldErrors": [...],
  "correlationId": "...",
  "retryable": false
}
```

HTTP semantics use standard 400/401/403/404/409/422/5xx classes as appropriate. Android maps machine-readable codes to typed outcomes; business logic must never parse free-text messages.

## 28. Error handling architecture

| Class | Behavior |
| --- | --- |
| Input/validation | Inline/local feedback; domain operation not executed until valid. |
| Offline/transient | Persist eligible local operation; show Pending/Offline; retry through durable pipeline. |
| Authentication | Attempt supported secure refresh; otherwise require Sign In without deleting durable unsynced business data. |
| Authorization | Explain insufficient permission; do not retry blindly. |
| Conflict | Persist CONFLICT and route to controlled resolution/reload. |
| Permanent server validation | Persist actionable failure; refresh authoritative state where appropriate. |
| Unexpected | Stable user-safe message plus correlation/diagnostic ID; sanitized telemetry. |
| Calculation integrity | Block on undefined/invalid formula data, mixed datum, invalid fill, stale fingerprint or unsupported version; never clamp silently. |

## 29. Concurrency and consistency

- Coroutines use structured concurrency; GlobalScope is prohibited.
- One-shot asynchronous work uses suspend; observable state uses Flow/StateFlow.
- Room transactions define atomic local mutation boundaries.
- Server transactions define authoritative Campaign Code allocation and Close atomicity.
- Optimistic concurrency uses entityVersion; stale critical writes do not silently overwrite.
- Per-entity sync ordering is preserved.
- UI recomposition does not trigger expensive calculations or network mutations.

## 30. Performance and scalability

- Release builds use R8 full-mode shrinking/optimization with verified rules for serialization/reflection.
- Baseline Profiles and Startup Profiles cover representative critical user journeys where supported.
- Macrobenchmark verifies representative startup and interaction/jank paths and is used to detect regressions.
- Production code performs no blocking disk/network I/O on the main thread.

- No network or blocking database work on the UI thread.
- Large lists use LazyColumn/LazyGrid and stable item keys.
- Room indexes support organization/campaign/location/unit/space and operational status filters.
- Server collections use cursor pagination where volume requires it; offline Campaign working set remains complete for assigned scope.
- Sync batches are bounded; evidence binaries use dedicated transfer.
- Calculation work is outside Compose recomposition and may use appropriate dispatcher while preserving deterministic results.
- Process death is normal: any action shown as saved must already be durable.

## 31. Adaptive UI architecture

| Window context | Architecture behavior |
| --- | --- |
| Compact | Persistent bottom navigation, single-pane primary flow. |
| Medium window | Adaptive layout may use additional width/list-detail/supporting-pane patterns only when it preserves frozen navigation semantics. |
| Expanded | List-detail/supporting panes allowed where useful; same 43 major UI contracts, no desktop redesign. |
| Landscape / multi-window | Reflow, do not merely stretch phone layout; preserve context and actions. |

The five bottom-nav information-architecture destinations remain canonical even if a larger-width visual navigation component is used. UI adaptation must not create new product destinations or permissions.

## 32. Database and migration architecture

Exact tables/columns/keys/indexes are Document 10. Architecture requires relational integrity, stable UUIDs, organization scoping, historical protection and forward-safe evolution.

- Foreign keys and business uniqueness constraints are enforced.
- No destructive cascade into operational/historical records.
- Production destructive migration is prohibited.
- Every supported production schema path has migration tests.
- Calculation-affecting configuration changes create new immutable versions instead of rewriting referenced versions.
- Inactive master data remains historically resolvable.
- Technical cleanup is allowed only under the Retention Policy and never removes unresolved sync/conflict facts.

## 33. Retention and lifecycle architecture

| Data class | Retention/lifecycle |
| --- | --- |
| Final snapshots, audit, evidence, issues, campaign snapshots | Minimum 10 years. |
| Synced technical diagnostics | 90 days, subject to policy/operational need. |
| Unresolved sync failures/conflicts | No automatic deletion while unresolved. |
| Notifications | 12 months. |
| Master data | Active/Inactive; historical references preserved; no hard delete that breaks snapshots. |
| Completed Campaign | Immutable through normal APIs. |

## 34. Observability and diagnostics

- Structured logs with severity and correlation IDs.
- Sync telemetry: queue depth, oldest pending age, retry/failure/conflict counts and run/operation correlation.
- Calculation diagnostics include engine/version identifiers without logging sensitive full payloads in production.
- Performance traces around campaign package preparation/load, DB migration, sync batches, evidence transfer and report generation where appropriate.
- Support diagnostics may include safe IDs, versions, counts and timestamps, but exclude tokens/secrets.
- Monitoring must distinguish integrity-blocking failures from transient availability failures.

## 35. Build, environments and release architecture

| Environment | Rule |
| --- | --- |
| Development | Developer diagnostics and non-production services. |
| Staging | Production-like QA/UAT environment. |
| Production | Protected production configuration and services. |

- Environment endpoints are build/environment configuration, not user Settings.
- Production build cannot be switched by the user to Development/Staging.
- Secrets live in protected CI/runtime mechanisms.
- Releases are staged and gated by compile/static analysis, unit tests, calculation golden tests, Room migration tests, lint and critical UI/integration tests.
- Release gates include R8 full-mode verification, representative Baseline/Startup Profile generation/verification, and Macrobenchmark evidence for critical user journeys.
- PostgreSQL backup at least daily with minimum 30-day retention and PITR where supported.
- Quarterly restore tests; objectives RPO ≤ 1 hour and RTO ≤ 4 hours.
- Risky schema changes use Expand → Migrate → Contract / forward-safe deployment patterns.

## 36. Testing architecture and seams

| Layer | Required test seam |
| --- | --- |
| Calculation engine | Pure JVM unit/golden tests for all 10 families, conversions, precision and invalid boundaries. |
| Domain | Rules for ownership/client, 1–5 measurements, Mark Empty, Reset Validation, issues, launch/close guards and immutable states. |
| Repository | Room + fake remote integration: local-first reads, transactional outbox, retry/idempotency and conflict classification. |
| Room | Migration, FK, uniqueness, indexes and transaction behavior. |
| ViewModel | UiState/action sequences, permission gating, offline/error transitions. |
| Compose UI | Critical forms, 43-component navigation contract, Navigation 3/back-stack/predictive-back behavior, edge-to-edge/inset/IME safety, adaptive layouts, accessibility semantics, and Location List/Map equivalence. |
| End-to-end | Sign In → Organization → Campaign → Location → Unit → Space → Calculate → Validate/Issue → Review → Close → Snapshot → Reports. |
| Security | Tenant isolation, role/team authorization, secret leakage, cleartext networking and session scenarios. |
| Operations | Backup/restore evidence, staged release and monitoring/alert behavior. |

## 37. Architecture risks and controls

| Risk | Control |
| --- | --- |
| Duplicate offline submissions | Stable operation UUID/idempotency key + server idempotency. |
| Multi-device overwrite | entityVersion + explicit CONFLICT; no generic last-write-wins. |
| Geometry regression | Pure engine + golden vectors + version metadata. |
| Historical corruption | Versioned definitions + Campaign Snapshot + immutable Final Snapshot + no destructive cascade. |
| Data lost before sync | Atomic Room business write + outbox before success. |
| Oversized ViewModels | Use cases/domain services/repositories and feature boundaries. |
| UI connectivity coupling | Room-backed Flow/StateFlow. |
| Client authorization bypass | Server-side authorization. |
| Tenant data leak | Organization-scoped queries/contracts and membership checks. |
| Evidence privacy leak | App-private storage, controlled upload, EXIF stripping, sanitized logs. |
| Migration corruption | Forward-safe migration + fixtures/tests + backup/restore. |
| Agent/developer invention | Frozen docs, explicit contracts and traceability. Non-semantic choices are implementation decisions/IADR and do not need Product approval; semantic changes require Baseline Change. |

## 38. Architecture decision set

| ADR | Accepted architectural decision |
| --- | --- |
| ADR-001 | Native Android implementation in Kotlin. |
| ADR-002 | Jetpack Compose for UI. |
| ADR-003 | Layered Clean Architecture + MVVM/UDF. |
| ADR-004 | Room is Android client source of truth. |
| ADR-005 | Critical offline writes use atomic local transaction + persistent outbox. |
| ADR-006 | WorkManager is persistent synchronization executor. |
| ADR-007 | Calculation engine is pure Kotlin and versioned/strategy-based. |
| ADR-008 | Geometry/Profile and governed calculation inputs are versioned; launched Campaign preserves immutable references/snapshot. |
| ADR-009 | Campaign Close creates exactly one immutable Final Inventory Snapshot. |
| ADR-010 | Single-activity Jetpack Navigation 3 with type-safe destination keys and five canonical operational top-level destinations. |
| ADR-011 | Reconciliation module excluded from V6. |
| ADR-012 | Server authorization is authoritative; client permission gating is not a security boundary. |
| ADR-013 | No generic last-write-wins/Force Overwrite for critical synchronized data. |
| ADR-014 | Evidence binaries use app-private local storage + controlled object storage, with metadata in relational persistence. |
| ADR-015 | Organization creation requires authorized provisioning context; ordinary Admin role alone cannot create arbitrary tenants. |
| ADR-016 | UTC instants + local UI timezone; campaign dates remain date-only. |
| ADR-017 | Production environment endpoints are build-controlled, not user-configurable. |
| ADR-018 | Predictive Back and supported system back APIs are part of the navigation contract. |
| ADR-019 | Production Compose UI is edge-to-edge safe and handles system/gesture/cutout/IME insets. |
| ADR-020 | Compose state observation is lifecycle-aware; durable business outcomes remain state-driven. |
| ADR-021 | Approved SiloMath Stitch references are the minimum visual-quality floor; Doc21 remains visual authority. |
| ADR-022 | Location facility coordinates support LOC-01 List/Map without device-location permission or user tracking. |
| ADR-023 | R8 full-mode, Baseline/Startup Profiles and Macrobenchmark are required release-performance controls. |

## 39. Implementation sequence

- Foundation: build conventions, dependency graph, app shell, Navigation 3, predictive back, edge-to-edge/inset handling, Hilt and shared model/domain foundations.
- Persistence: Room schema, DAOs, migrations, mapping and transactional outbox primitives.
- Network/security: Retrofit/OkHttp, session handling, authorization context, typed errors and environment configuration.
- Offline/sync: repositories, campaign working set, WorkManager, idempotency, versioning and conflicts.
- Master data and governed geometry/commodity read models.
- Pure Calculation Engine with complete golden tests.
- Campaign Create & Launch with server Campaign Code and frozen snapshot.
- Stocktaking end-to-end including evidence, issues, Validate, Mark Empty and Reset Validation.
- Review/Close with atomic server snapshot.
- Reports/history/dossier/audit and exports.
- Administration, notifications, settings, support.
- Adaptive UI, performance, security/privacy review, migration/restore tests and production readiness.

This sequence is architectural guidance, not permission to bypass the frozen screen/business-rule contract or to reimplement already compliant working functionality unnecessarily.

## 40. Architecture acceptance criteria

| ID | Acceptance criterion |
| --- | --- |
| AR-AC-01 | No ViewModel accesses a Room DAO or Retrofit service directly. |
| AR-AC-02 | No Compose component contains authoritative business calculation or lifecycle transition logic. |
| AR-AC-03 | Operational reads are available from Room without network after the required working set is prepared. |
| AR-AC-04 | Every critical offline Stocktaking mutation commits business state and SyncOperation atomically before UI success. |
| AR-AC-05 | Retried mutations are idempotent and do not duplicate server business actions. |
| AR-AC-06 | All critical synchronized mutable entities use version-aware conflict handling; no silent last-write-wins. |
| AR-AC-07 | Calculation Engine executes as pure JVM code and passes golden tests for all ten geometry families. |
| AR-AC-08 | Campaign launch freezes exact scope and required versioned calculation context; scope cannot change afterward. |
| AR-AC-09 | Campaign Code is server-generated atomically and never edited by the Android client. |
| AR-AC-10 | Non-empty validation cannot succeed without valid required measurement/calculation, Commodity/density and Ownership/Client rules. |
| AR-AC-11 | Mark Empty validates explicitly with zero volume/quantity and does not require non-empty fields. |
| AR-AC-12 | Validated records are read-only in normal flow; Admin Reset Validation is explicit and audited; Completed data remains immutable. |
| AR-AC-13 | Validate/Mark Empty never auto-resolve an Open Issue. |
| AR-AC-14 | Close is Admin-only, online, server-revalidated and atomic; failure creates no partial Final Snapshot. |
| AR-AC-15 | Exactly one Final Inventory Snapshot exists for a successfully closed Campaign. |
| AR-AC-16 | Completed official reports are generated from immutable Final Snapshot data, not current master data. |
| AR-AC-17 | All protected server operations revalidate identity, organization membership, role/team and state guards. |
| AR-AC-18 | Evidence respects limits, normalization, hash, private local storage, object storage and privacy rules. |
| AR-AC-19 | Production secrets are not stored in plaintext/source/logs and cleartext networking is disabled. |
| AR-AC-20 | Production database migrations preserve business data and are covered by migration tests. |
| AR-AC-21 | All 43 UI components in Document 06 can be implemented without violating architecture boundaries. |
| AR-AC-22 | Traceability can connect frozen requirement/decision to business rule, UI/domain behavior, persistence/API and test coverage. |
| AR-AC-23 | Navigation 3 type-safe destination/back-stack behavior, predictive back, deep-link guards and process restoration pass regression. |
| AR-AC-24 | Edge-to-edge/system-bar/cutout/gesture/IME safety passes critical-screen regression. |
| AR-AC-25 | Compose ViewModel state collection is lifecycle-aware and critical operation outcomes survive required lifecycle recreation. |
| AR-AC-26 | LOC-01 List/Map use the same authorized query/filter result; absence of basemap cannot block authoritative Location navigation. |
| AR-AC-27 | Release build verifies R8 full mode and representative Baseline/Startup Profile + Macrobenchmark evidence. |

## 41. Cross-document handoff

| Next document | What this architecture requires it to define |
| --- | --- |
| 08_MODULE_ARCHITECTURE | Exact Gradle module ownership, allowed dependency graph, package/API boundaries and incremental migration plan. |
| 09_DATA_MODEL | Exact domain entities, attributes, relationships, cardinalities, value objects and invariants. |
| 10_DATABASE_SCHEMA | Exact PostgreSQL/Room tables, columns, PK/FK, constraints, indexes, deletion/lifecycle and migration contract. |
| 11_API_SPECIFICATION | Exact endpoints, DTOs, errors, idempotency/versioning, pagination, evidence transfer and Close/package contracts. |
| 12_OFFLINE_SYNC_SPECIFICATION | Detailed queue/outbox, package manifest, triggers, retries, ordering, conflict and cleanup behavior. |
| 13_CALCULATION_ENGINE_SPECIFICATION | Exact formulas, parameters, validations, precision and golden vectors. |
| 17_SECURITY_AUDIT_PRIVACY | Threat controls, audit event taxonomy, privacy and security verification. |
| 18_TEST_STRATEGY_ACCEPTANCE | Traceable automated/manual acceptance matrix for all frozen contracts. |
| 19_RETENTION_POLICY | Entity-level retention/cleanup implementation. |
| 20_PRODUCTION_OPERATIONS | Environments, deployment, backup/restore, monitoring, incident and continuity procedures. |

## 42. Final architecture invariants

- Product behavior is defined by frozen V6 documents, not by framework convenience.
- Room is the Android operational read source; backend is enterprise authority.
- Every critical local success is durable before it is shown.
- Every privileged remote mutation is server-authorized.
- Every historical/calculation-critical fact is versioned or snapshotted sufficiently for reproduction.
- Every conflict that can affect inventory integrity is explicit; no silent overwrite.
- Every completed inventory is immutable and reportable from its Final Snapshot.
- Every tenant business operation is organization-scoped.
- Every major UI component uses the same domain/data architecture; no screen-specific bypass.
- Undefined future product behavior is not invented in code. Ordinary non-semantic implementation choices inside Documents 01–21 proceed without Product/Baseline approval and are recorded through IADR only when architecturally material.

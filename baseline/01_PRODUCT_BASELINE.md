# SiloMath V6 — Product Baseline

**Document ID:** 01_PRODUCT_BASELINE  
**Version:** V6 Final Documentation — Baseline 1.1  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-29 - CR-ARCH-SUPABASE-BACKEND-001 approved Supabase-only canonical core backend platform
**Product:** SiloMath  
**Platform:** Native Android  
**UI language:** English  
**Minimum Android version:** API 26  
**Normative role:** Primary product-level contract for SiloMath V6. Detailed domain specifications may refine implementation detail but must not contradict this baseline.

---

## 1. Purpose

This document defines the canonical product baseline for SiloMath V6. It establishes the product scope, actors, core concepts, lifecycle, functional phases, system-wide invariants, user-interface inventory, offline-first operating model, calculation principles, reporting model, security boundaries, data-history requirements, and production expectations.

SiloMath is a bulk inventory physical stocktaking platform. It supports organizations that manage physical storage infrastructure, execute inventory campaigns, measure product in storage spaces, calculate physical quantity from approved geometry and density inputs, validate results, close campaigns, preserve immutable historical snapshots, and produce official reports.

This baseline is normative. Product behavior must conform to the V6 contract defined by the final documentation set.

## 2. Product Objectives

SiloMath V6 shall:

1. Provide a controlled workflow from organization and master data through physical stocktaking, validation, inventory close, immutable history, and reporting.
2. Support reliable operation in environments with intermittent or unavailable connectivity through a campaign-scoped offline working set and durable synchronization.
3. Produce deterministic and reproducible physical inventory calculations using versioned geometry and calculation rules.
4. Preserve historical integrity by snapshotting the context required to reproduce and audit completed inventory campaigns.
5. Enforce organization, role, campaign-team, and data-scope authorization consistently in the Android client and on the server.
6. Separate business rules from Compose UI and preserve Clean Architecture boundaries.
7. Support operational use across compact, medium, and expanded Android application windows, including phone/tablet portrait/landscape, resize, multi-window, and supported posture changes.
8. Provide a polished enterprise UI whose production visual quality is no lower than the approved SiloMath Stitch reference while preserving the frozen business contract.
9. Provide an Organization-scoped Location map/list experience using stored facility coordinates without requiring device-location permission or user tracking.

## 3. Platform and Technology Baseline

The client application is a native Android application implemented in Kotlin and Jetpack Compose.

The architectural baseline is:

**Compose UI → ViewModel / UI State → Use Cases → Domain → Repository Contracts → Repository Implementations → Local / Remote Data Sources**

Core technologies are:

- Kotlin;
- Jetpack Compose;
- MVVM with unidirectional data flow;
- StateFlow;
- Hilt dependency injection;
- Room / SQLite;
- Retrofit and OkHttp for REST communication;
- Kotlin Coroutines and Flow;
- WorkManager for durable background synchronization;
- Jetpack Navigation 3 with type-safe, serializable destination keys and a single-activity Compose application;
- DataStore for appropriate local preferences;
- Supabase as the sole canonical core backend runtime platform;
- PostgreSQL as the authoritative enterprise datastore;
- Supabase Auth for canonical identity/authentication;
- Supabase PostgreSQL functions/RPC and Supabase Edge Functions for server-authoritative business operations;
- Supabase Storage for remote Evidence object storage;
- Supabase Realtime only as a non-authoritative refresh/notification accelerator where useful.

The application is offline-first/local-first for supported campaign operations. Compose must not contain business calculations or direct database access. ViewModels coordinate UI state and use cases. Repositories abstract local and remote data sources.

Direct Android access to Supabase services is permitted only where the API/security contract explicitly allows it and RLS/authorization enforce the required Organization, role and campaign scope. Critical mutations remain server-authoritative, atomic and idempotent. A separate application backend or external compute service is not part of the target core SiloMath runtime architecture.

## 4. Roles and Authorization Model

SiloMath V6 has exactly two organization roles:

- **Admin**
- **Standard**

User lifecycle states are:

- **Active**
- **Disabled**

A Disabled user cannot initiate new authentication or new operations and cannot receive new assignments. Historical identity and references are preserved. Reactivation restores the same identity.

### 4.1 Admin

Admin may, subject to organization scope and other workflow guards:

- manage organization master data;
- create and launch inventory campaigns;
- define campaign scope;
- assign campaign teams;
- manage users and organization configuration;
- review campaigns;
- close inventory campaigns;
- access the organization Audit Trail;
- access reports for authorized organization data.

### 4.2 Standard

Standard may:

- read authorized master data;
- participate in assigned inventory campaigns;
- execute permitted stocktaking operations when assigned to the Campaign Team;
- report issues;
- access authorized reports and completed-campaign information within permitted scope.

Standard cannot mutate master data and cannot close inventory campaigns.

### 4.3 Campaign Team

Organization role and Campaign Team membership are separate authorization dimensions.

Stocktaking mutations require Campaign Team membership for both Admin and Standard. An Admin may launch, monitor, review, administer, or close a campaign without being a Campaign Team member, but may not create or modify stocktaking measurements or records solely because of the Admin role.

## 5. Canonical Product Hierarchy

The physical master-data hierarchy is:

**Organization → Location → Storage Unit → Storage Space**

An Organization is the tenant and operational root.

A Location contains Storage Units. A Location may also carry an optional authoritative facility coordinate pair used only for facility map presentation. Coordinates are Organization-scoped master data, are Admin-managed, and never represent user/device tracking.

A Storage Unit contains Storage Spaces.

A Storage Space is the physical unit against which stocktaking is performed.

Master data uses the lifecycle:

- **Active**
- **Inactive**

Historically referenced master data must not be hard-deleted in a way that invalidates campaign snapshots, final inventory snapshots, reports, audit history, or other historical references.

## 6. Geometry Model

SiloMath distinguishes:

- **Geometry Family** — platform-governed mathematical model;
- **Geometry Profile** — concrete geometry configuration;
- **Geometry Profile Version** — immutable/versioned dimensional definition used for reproducible calculations.

Customers cannot create, edit, or delete Geometry Families or their formula definitions. Admin users may create and maintain Geometry Profiles within approved Geometry Families.

The platform supports exactly ten V6 geometry families:

| Code | Canonical identifier |
|---|---|
| F01 | `CYL_TRUNC_CONE_V1` |
| F02 | `CYL_FLAT_V1` |
| F03 | `SQUARE_FLAT_V1` |
| F04 | `RECT_FLAT_V1` |
| F05 | `SQUARE_PYRAMID_HOPPER_V1` |
| F06 | `RECT_PYRAMID_HOPPER_V1` |
| F07 | `REG_HEX_FLAT_V1` |
| F08 | `STAR_VERIFIED_AREA_V1` |
| F09 | `POCKET_VERIFIED_AREA_V1` |
| F10 | `PERIMETER_VERIFIED_AREA_V1` |

F08–F10 use a verified constant cross-sectional area with provenance. They are not applicable where the cross-section varies with height.

Maximum Capacity is automatically calculated from approved geometry parameters.

## 7. Commodity Model

Commodities are platform-governed master definitions. SiloMath V6 contains 24 approved commodities:

**Cereals:** Common Wheat, Durum Wheat, Spelt, Corn/Maize, Barley, Malting Barley, Oats, Rye, Triticale, Sorghum, Millet, Rice.  
**Pseudocereal:** Buckwheat.  
**Oilseeds:** Sunflower Seed, Rapeseed/Canola, Soybean, Mustard Seed, Linseed/Flaxseed.  
**Pulses:** Field Peas, Field/Faba Beans, Lupins, Dry Beans, Chickpeas, Lentils.

There is no generic Other commodity.

The fundamental quantity input is **Actual Bulk Density**, normalized to `kg/m³`. Reference Test Weight is reference information and must never be used as an automatic fallback for Actual Bulk Density.

Moisture and Impurities do not automatically correct Physical Quantity in V6.

Commodity definitions use Active/Inactive lifecycle and preserve historical references.

## 8. Ownership and Client

Ownership is mandatory for non-empty stocktaking and has exactly two values:

- **Owned**
- **Third-Party**

When Ownership is Third-Party, Client is mandatory.

When Ownership is Owned, Client is not required.

Client is organization-scoped master data and is referenced by stable identity rather than free-text ownership names.

For Mark Empty, Commodity, Actual Bulk Density, Ownership, and Client are not required and must not be fabricated.

## 9. Inventory Campaigns

Inventory Campaign statuses are exactly:

- **Active**
- **Completed**

There is no Draft, Upcoming, In Progress, or Review campaign status. Review is a workflow process.

Inventory Type is exactly:

- **Annual**
- **Monthly**
- **Control**

Each campaign has:

- technical stable identity;
- Campaign Code;
- Campaign Name;
- Organization;
- Inventory Type;
- Start Date;
- Planned End Date;
- Campaign Scope;
- Campaign Team;
- lifecycle timestamps and audit metadata.

Start Date is mandatory. Planned End Date is mandatory and cannot precede Start Date. Planned End Date does not automatically close a campaign.

### 9.1 Campaign Code

Campaign Code is system-generated and read-only.

Canonical format:

`INV-YYYY-NNNN`

The official code is generated atomically by the server and is unique within the Organization and calendar year. The technical UUID remains distinct from the human-facing Campaign Code.

### 9.2 Save & Launch

Campaign creation uses **Save & Launch**. There is no Draft state.

Launch establishes the campaign as Active and freezes its Campaign Scope. Scope cannot be modified after launch.

Campaign Team contains at least one member. The Admin who launches the campaign is not automatically required to be a team member.

Save & Launch does not mean that the campaign is Offline Ready.

## 10. Campaign Snapshot

At launch, the system preserves the campaign context necessary for deterministic operation and historical traceability.

The Campaign Snapshot includes, at minimum:

- campaign identity and configuration;
- Organization;
- exact Campaign Scope;
- hierarchy context;
- Geometry Family/Profile/Version and calculation parameters;
- Commodity identity/version data where relevant;
- Campaign Team;
- Inventory Type;
- Start Date and Planned End Date;
- calculation/schema/version identifiers required by the campaign.

The snapshot prevents later master-data changes from silently altering the historical meaning of the campaign.

## 11. Stocktaking

Canonical stocktaking navigation follows:

**Stocktaking Overview → Location Stocktaking → Storage Unit Stocktaking → Storage Space Stocktaking**

Operational classifications are:

- **To Do**
- **Issues**
- **Validated**

**All** is an aggregate UI filter, not a persistent business state.

A Stocktaking Record is conceptually unique for a Campaign × Storage Space.

### 11.1 Measurements

A non-empty Storage Space supports between one and five measurements.

V6 uses ullage/distance-down measurements from the approved measurement datum.

All measurements used in one calculation must:

- refer to the same Geometry Profile Version;
- use the same datum;
- use the same measurement type;
- be normalizable to canonical units.

Geometry is read-only during stocktaking. An operator must report a geometry anomaly through Report Issue rather than modifying the geometry to make a measurement fit.

### 11.2 Mark Empty

Mark Empty is a workflow action, not a fifth stocktaking status.

It produces:

- `Validated`;
- `isEmpty = true`;
- Product Volume = `0`;
- Physical Quantity = `0`.

Measurements, Commodity, Actual Bulk Density, Ownership, and Client are not required for Mark Empty.

Actor and timestamp are audited. Notes and permitted evidence may be retained.

### 11.3 Validation

A Storage Space is complete only when Validated.

Validated data cannot be overwritten by stale offline data.

Validation does not automatically resolve an Open Issue.

## 12. Calculation Baseline

The canonical calculation pipeline is:

**Measurements → Normalization → Aggregation → Effective Fill Height → Geometry Engine → Product Volume → Quantity Engine → Physical Quantity**

For non-empty records:

`u_eff = arithmetic mean(u1 … un)`

`h = measurementReferenceHeightM - u_eff`

`V = V_family(h)`

`M = V × ρ`

where:

- `u_eff` = Effective Ullage;
- `h` = Effective Fill Height;
- `V` = Product Volume in `m³`;
- `ρ` = Actual Bulk Density in `kg/m³`;
- `M` = Physical Quantity in `kg`.

Canonical calculation identifiers are:

- `surfaceModelVersion = LEVEL_BASED_V1`
- `aggregationMethod = MEAN_ULLAGE`
- `aggregationRuleVersion = 1`
- combined rule identifier: `MEAN_ULLAGE_V1`

The engine does not automatically discard outliers.

Invalid or ambiguous calculation inputs block calculation/validation. The system must not silently clamp invalid values or invent missing dimensional interpretations.

Canonical units are:

- length: `m`;
- area: `m²`;
- volume: `m³`;
- density: `kg/m³`;
- quantity: `kg`.

`1 kg/hl = 10 kg/m³`.

Calculations use deterministic decimal processing with no intermediate rounding. Approved persistence boundary is six decimal places using `ROUND_HALF_UP`; PostgreSQL numeric values use `NUMERIC(20,6)` where this contract applies.

Historical calculation results are never recalculated from current master data.

## 13. Issues

Issues are contextual to stocktaking. There is no separate global Issues module.

Issue status is exactly:

- **Open**
- **Resolved**

Report Issue requires Issue Type and Description. Evidence may be attached according to the Evidence specification.

Issue resolution is explicit and audited.

Validate and Mark Empty do not automatically resolve an Open Issue.

For inventory close, unresolved matters must be handled according to Close Guards. Where an Admin explicitly accepts an issue for follow-up, the Issue transitions from Open to Resolved with an explicit reason and audit record; it does not remain Open while the campaign is closed.

## 14. Evidence

V6 evidence types are:

- **Photo**
- **Note**

Maximum photo limits are:

- five photos per Stocktaking Record;
- five photos per Issue.

Photos are normalized to JPEG, EXIF orientation is corrected, long edge is limited to 2048 px, target size is at most 1.5 MB, and hard maximum after processing is 3 MB. SHA-256 is mandatory.

Binary evidence is stored in app-private local storage while offline and synchronized to Supabase Storage as the canonical remote Evidence object store. Room/PostgreSQL store metadata, object references, hashes, relations, and synchronization state rather than large image BLOBs.

After validation, evidence belonging to the validated historical context is immutable/read-only according to the evidence contract.

## 15. Review and Inventory Close

Review is a process, not a Campaign status.

Inventory Close is Admin-only and requires connectivity.

Close Guards require, among other applicable validations:

- Campaign is Active;
- all scoped Storage Spaces are in a valid final state;
- no remaining To Do work;
- mandatory data is valid;
- critical operations required for close are synchronized;
- Open Issues are resolved according to the approved issue-resolution process;
- server-side authorization and consistency checks pass.

The server atomically revalidates Close Guards through an approved Supabase server-authoritative path backed by PostgreSQL transactional semantics.

A successful Close:

1. creates exactly one immutable Final Inventory Snapshot for the Campaign;
2. records close actor and timestamp;
3. changes Campaign status to Completed.

A failed Close leaves the Campaign Active and must not create a partial final state.

A Completed campaign cannot return to Active.

## 16. Final Inventory Snapshot and History

The Final Inventory Snapshot is the authoritative historical source for a Completed campaign.

It contains campaign-level context and per-Storage-Space entries sufficient to preserve:

- Campaign and Campaign Code;
- Organization;
- Location / Storage Unit / Storage Space context;
- frozen geometry/version and calculation metadata;
- Commodity data;
- Ownership and Client where applicable;
- Empty state;
- measurements and effective operational values;
- Product Volume;
- Actual Bulk Density where applicable;
- Physical Quantity;
- validation actor/time;
- issue references;
- Notes/Evidence references;
- close timestamp and actor;
- version/fingerprint metadata required for audit and reproducibility.

The Final Inventory Snapshot is immutable.

Inventory History compares Final Inventory Snapshots from two or more Completed campaigns. It reports Physical Inventory Change rather than reconciliation variance.

Historical comparability classifications include:

- **Comparable**
- **Scope Added**
- **Scope Removed**
- **Context Changed**

Inventory History does not provide forecasting or prediction.

## 17. Reports

SiloMath V6 contains exactly nine report types:

1. Physical Inventory
2. Inventory by Location
3. Inventory by Commodity
4. Inventory by Ownership
5. Storage Space Worksheet
6. Capacity Report
7. Inventory History
8. Digital Inventory Dossier
9. Audit Trail

Completed-campaign official reports use Final Inventory Snapshot data.

Active-campaign monitoring may use current operational data but must be clearly identified as non-final.

Reports use a Single Authoritative Report Dataset principle so UI presentation and exported representations derive from the same canonical dataset.

Reports are available to Admin and Standard within authorized scope. Audit Trail is Admin-only.

## 18. Offline Working Set

Offline support is campaign-scoped.

A device becomes **Offline Ready** for a campaign only after the complete authorized Campaign Offline Working Set has been downloaded and its integrity verified.

The working set includes the information necessary to execute the assigned campaign offline, including relevant:

- campaign header and status;
- Campaign Snapshot and scope;
- hierarchy;
- pinned geometry/schema/commodity data;
- calculation compatibility metadata;
- Campaign Team authorization;
- existing stocktaking records;
- Ownership/Client data;
- evidence metadata/content required by the working set;
- synchronization metadata;
- integrity manifest.

The system must not download arbitrary whole-organization datasets merely to support offline stocktaking.

## 19. Synchronization and Conflict Handling

Local business writes are durable.

A supported offline mutation commits the business change and its Sync Operation in the same Room transaction. The UI reports Saved only after the local transaction commits.

Canonical Sync Operation states are:

- `PENDING`
- `IN_FLIGHT`
- `ACKNOWLEDGED`
- `RETRYABLE_ERROR`
- `CONFLICT`
- `PERMANENT_ERROR`

Retries reuse the same idempotency key.

Optimistic concurrency uses entity versioning. A stale mutation results in a conflict rather than silent last-write-wins behavior.

The system must not automatically merge competing measurements.

Critical invariants include:

- Validated data cannot be overwritten by stale offline data;
- Completed cannot be downgraded to Active;
- Final Inventory Snapshot is immutable.

Synchronization respects same-entity ordering and operation dependencies. Critical business actions are not silently coalesced.

WorkManager uses controlled retry with exponential backoff for retryable operations.

Supabase Realtime may notify clients that authoritative data changed, but Realtime delivery does not acknowledge durable Sync Operations, resolve conflicts, replace idempotency/entityVersion checks, or become an authoritative synchronization state machine.

## 20. Notifications, Settings, Sync Status, and Support

SiloMath includes an in-app Notification Center with:

- unread/read state;
- timestamp;
- title;
- message;
- context reference where applicable;
- contextual navigation;
- Mark Read;
- Mark All Read.

Notifications are not Audit Trail events.

Settings distinguishes:

- **User Preference**
- **Organization Configuration**
- **System-Controlled Rule**

User preferences may control representation such as quantity display unit, density representation where applicable, date/time format, theme, non-critical notification preferences, and optional default report format.

Organization Configuration is Admin-only for explicitly configurable organization settings.

System-controlled rules are not user-editable. This includes formulas, calculation-engine rules, aggregation, maximum measurement count, canonical units, Commodity definitions, workflow statuses, ownership/client rules, Close Guards, snapshot behavior, sync conflict strategy, evidence hashing, retention rules, authentication rules, and platform limits.

There is no language selector in V6; the UI language is English.

Sync Status provides operational visibility but does not expose server URL, custom sync interval, force overwrite, disable-sync controls, or user-selectable conflict strategy.

About information is consolidated into Support. There is no separate About screen.

Sign Out must not automatically destroy unsynchronized local business data.

## 21. Authentication

Authentication provides:

- Sign In;
- Forgot Password;
- Reset Password;
- secure automatic session restoration.

V6 does not provide:

- public Sign Up;
- Remember Me;
- Google SSO;
- Microsoft SSO.

Supabase Auth is the canonical authentication/identity platform. Authorization is revalidated server-side and is not trusted solely to Android UI visibility. RLS and server-authoritative functions/Edge Functions enforce the applicable protected-operation boundaries.

## 22. Canonical UI Inventory

The V6 major UI inventory contains **36 full screens and 7 sheets**, for a total of **43 major UI components**.

### 22.1 Full Screens

`AUT-01` Sign In  
`AUT-02` Reset Password  
`DSH-01` Dashboard  
`ORG-01` Select Organization  
`ORG-02` Create/Edit Organization  
`LOC-01` Locations  
`LOC-02` Storage Units in Location  
`STU-02` Storage Unit Details  
`STS-02` Storage Space Details  
`GEO-01` Geometry Profiles  
`GEO-02` Geometry Profile Details  
`COM-01` Commodities  
`COM-02` Commodity Details  
`CLI-01` Clients  
`CLI-02` Client Details  
`CAM-01` Inventory Campaigns  
`CAM-02` Campaign Details  
`CAM-03` Create & Launch Campaign  
`STK-01` Stocktaking Overview  
`STK-02` Location Stocktaking  
`STK-03` Storage Unit Stocktaking  
`STK-05` Storage Space Stocktaking  
`CLS-01` Campaign Review & Close  
`CLS-02` Final Inventory Snapshot  
`RPT-01` Reports  
`RPT-02` Report Viewer  
`HIS-01` Inventory History  
`DOS-01` Digital Inventory Dossier  
`AUD-01` Audit Trail  
`USR-01` Users  
`USR-02` User Details  
`MOR-01` More  
`NOT-01` Notification Center  
`SYN-01` Sync Status  
`SET-01` Settings  
`SUP-01` Support

### 22.2 Sheets

`AUT-SH-01` Forgot Password  
`P1-SH-01` Add/Edit Location  
`P1-SH-02` Add/Edit Storage Unit  
`P1-SH-03` Add/Edit Storage Space  
`GEO-SH-01` Add/Edit Geometry Profile  
`CLI-SH-01` Add/Edit Client  
`P0-SH-01` Report Issue

Campaign Scope Selector and Team Assignment are contextual components of CAM-03 rather than separate major UI components.

The operational bottom navigation is fixed:

**Dashboard / Inventory Campaigns / Stocktaking / Locations / More**

Authentication is outside the operational shell.

`LOC-01` includes synchronized **List / Map** presentation of the same authorized Location dataset. The map does not create a new top-level destination. When no basemap is available, the authoritative list/hierarchy remains fully usable.

## 23. Functional Phases

SiloMath V6 is organized into:

- **P0 — Core Platform & Dashboard**
- **P1 — Master Data**
- **P2 — Inventory Campaigns**
- **P3 — Stocktaking**
- **P4 — Inventory Close**
- **P5 — Reports**
- **P6 — Administration**

The canonical end-to-end flow is:

**Sign In → Organization → Campaign → Location → Storage Unit → Storage Space → Measurement → Calculation → Validate / Report Issue → Review → Close → Immutable Final Inventory Snapshot → Reports**

## 24. Target Module Architecture

The target Gradle architecture is pragmatic and modular:

- `:app`
- `:core:domain`
- `:core:model`
- `:core:common`
- `:core:database`
- `:core:network`
- `:core:sync`
- `:core:designsystem`
- `:core:navigation`
- `:core:testing`
- `:feature:auth`
- `:feature:dashboard`
- `:feature:masterdata`
- `:feature:campaigns`
- `:feature:stocktaking`
- `:feature:close`
- `:feature:reports`
- `:feature:administration`
- `:calculation-engine`

Feature modules may depend on core modules. Core modules must not depend on feature modules. The app module composes application navigation and dependency injection and contains minimal business logic.

The Calculation Engine is pure Kotlin and must not depend on Compose, Android UI, Room, Retrofit, or ViewModel.

Migration toward this topology is incremental and must follow an audit of the existing dependency graph. The architecture does not require one Gradle module per screen or entity.

The server-side target topology is Supabase-only for core application runtime: Supabase Auth, PostgreSQL/RLS/functions, Edge Functions where orchestration/HTTP boundary is needed, and Supabase Storage. The existing separate Kotlin/Ktor backend is transitional implementation and is superseded as a target architecture by FD-76.

## 24.1 Implementation-Closure Rule

The V6 baseline is implementation-ready. Product semantics, business behavior, authorization, state machines, persistence/API semantics, offline/sync behavior, UI inventory, and visual requirements necessary to implement the approved product are closed by Documents 01–21.

During implementation, **no additional Product/Baseline approval is required** for a technical choice that:

- preserves all frozen V6 behavior and invariants;
- stays inside the frozen 19-module Android topology and dependency direction;
- uses supported Android/Kotlin/Jetpack APIs consistent with Documents 07/08/18/20;
- stays inside the FD-76 Supabase-only core backend boundary;
- does not add a screen, sheet, business state, permission, data field, API semantic, workflow, or user-visible capability not already authorized by Documents 01–21.

Such choices are implementation decisions and may be recorded as an IADR when architecturally material. A Baseline Change is required for an intentional change to the frozen product contract, including replacing Supabase as the canonical core backend platform or making another service authoritative for core business state.

## 25. Security and Privacy Baseline

SiloMath applies least-privilege authorization, organization isolation, campaign scope restrictions, RLS defense-in-depth for client-accessible data paths, and server-side authorization checks.

Sensitive credentials and secrets must not be stored in Git, embedded in the APK, written to logs, or included in diagnostic packages. Supabase service-role or equivalent privileged credentials are server-only. Client applications may contain only credentials explicitly designed for public/client use and must still rely on RLS and server-side authorization.

Evidence collection follows data-minimization principles. Unnecessary EXIF metadata is removed. GPS/device identifiers are not automatically collected merely for evidence.

Diagnostic packages contain safe technical metadata by default and exclude secrets, photos, and full business payloads unless an explicitly approved support workflow requires otherwise.

## 26. Retention Baseline

Minimum retention requirements include:

- Final Inventory Snapshot: minimum 10 years after close;
- critical Audit events: minimum 10 years;
- official Evidence tied to validated/final history: minimum 10 years;
- Issues and resolution history: minimum 10 years;
- Campaign Snapshot: minimum 10 years;
- historically referenced master data: preserved without destructive deletion.

Acknowledged/synchronized operation diagnostics may be retained online for 90 days before purge/archive where allowed, while the corresponding critical business event remains in the long-term audit history.

Unresolved failed operations are not automatically deleted.

Notifications are retained server-side for 12 months.

Device cache cleanup is permitted only when required server durability and synchronization conditions have been satisfied.

## 27. Production Operations Baseline

Production environments are exactly:

- Development
- Staging
- Production

Each environment has an isolated controlled Supabase backend context covering the relevant Auth, PostgreSQL/RLS/functions, Edge Functions, Storage, secrets/configuration and telemetry. No separate Render/Ktor application-backend deployment is part of the target production topology.

Database migrations must be forward-safe. Risky schema changes use an Expand → Migrate → Contract strategy.

PostgreSQL is backed up automatically at least daily. Point-in-time recovery is used where supported. Backup retention is at least 30 days, with backup before relevant risky migrations/releases.

Restore testing is performed quarterly in an isolated environment.

Operational objectives are:

- **RPO ≤ 1 hour**
- **RTO ≤ 4 hours**

The release path is:

**Build → Automated Tests → Migration Validation → Staging → Production → Smoke Test**

Calculation formula changes require golden regression tests.

Production monitoring covers API/Edge Functions, authentication, database/RLS/functions, synchronization, queues, conflicts, Evidence Storage, backups, background jobs, Close operations, and calculation errors.

When consistency is uncertain, integrity takes priority over availability. The system must block Inventory Close rather than create an inconsistent Final Inventory Snapshot. Offline stocktaking may continue when the authorized working set is complete and valid.

## 28. System-Wide Invariants

The following invariants are mandatory across UI, domain, persistence, API, synchronization, and tests:

1. Campaign statuses are only Active and Completed.
2. Completed cannot transition to Active.
3. Campaign Scope is immutable after launch.
4. Save & Launch does not create a Draft.
5. Save & Launch does not imply Offline Ready.
6. Stocktaking mutations require Campaign Team membership.
7. Master Data mutations are Admin-only.
8. Geometry is read-only during stocktaking.
9. A non-empty Stocktaking Record uses one to five valid measurements.
10. Mark Empty produces Validated with zero volume and zero quantity.
11. Validate and Mark Empty do not automatically resolve Open Issues.
12. Validated data cannot be overwritten by stale offline data.
13. Third-Party ownership requires Client for non-empty stocktaking.
14. Reference Test Weight is not an automatic substitute for Actual Bulk Density.
15. Historical calculations are not recalculated from current master data.
16. Inventory Close is Admin-only and online.
17. Close revalidates server-side.
18. Successful Close creates exactly one immutable Final Inventory Snapshot.
19. Failed Close creates no partial final state.
20. Completed official reports use the Final Inventory Snapshot.
21. Final Inventory Snapshot is immutable.
22. Historically referenced master data is not destructively deleted.
23. Sync conflicts are explicit; critical data does not use silent last-write-wins.
24. Authorization is enforced server-side in addition to client-side UX restrictions.
25. System-controlled rules cannot be modified through Settings.
26. Supabase is the sole canonical backend platform for core SiloMath application runtime.
27. Critical mutations cannot rely on direct client writes that bypass approved server-authoritative PostgreSQL/Edge Function paths.
28. External auxiliary services cannot become authoritative for core business state without an approved Baseline Change.

## 29. Out of Scope for V6

The V6 product does not include:

- a Reconciliation module;
- Book Stock / accounting variance workflow;
- forecasting or predictive inventory history;
- live user/device location tracking, background location collection, turn-by-turn navigation, or route guidance;
- public user registration;
- Google or Microsoft SSO;
- user-configurable calculation formulas;
- user-created Geometry Families;
- user-created Commodity definitions;
- arbitrary whole-organization offline replication;
- user-configurable synchronization conflict strategy;
- force-overwrite synchronization;
- a separate About screen;
- a separate global Issues module;
- a separate application backend/external compute service as canonical core SiloMath runtime.

## 29.1 Android Platform Interaction Baseline

The Android implementation is edge-to-edge safe and handles system bars, display cutouts, gesture insets, and IME insets so required content/actions are never obscured. Back navigation uses supported system/Jetpack APIs and participates in predictive back where available. Compose observes ViewModel state lifecycle-aware. Durable business outcomes are represented in durable state; transient UI/navigation mechanics cannot become an alternative authority for business state.

The canonical SiloMath semantic color system remains authoritative; Android system dynamic color does not override semantic/brand tokens.

Release engineering includes R8 full-mode optimization, Baseline/Startup Profiles for critical user journeys where supported, and Macrobenchmark regression evidence for representative startup and interaction paths.

## 30. Documentation Governance

This Product Baseline defines the product-level contract. The remaining V6 specifications elaborate the same frozen system in specialized domains.

The final documentation set is:

1. `PRODUCT_BASELINE`
2. `DECISION_REGISTER`
3. `BUSINESS_RULES`
4. `USER_ROLES_PERMISSIONS`
5. `WORKFLOWS_STATE_MACHINES`
6. `UI_SCREEN_SPECIFICATION_AND_NAVIGATION`
7. `ARCHITECTURE`
8. `MODULE_ARCHITECTURE`
9. `DATA_MODEL`
10. `DATABASE_SCHEMA`
11. `API_SPECIFICATION`
12. `OFFLINE_SYNC_SPECIFICATION`
13. `CALCULATION_ENGINE_SPECIFICATION`
14. `COMMODITY_SPECIFICATION`
15. `EVIDENCE_STORAGE_SPECIFICATION`
16. `REPORTING_SPECIFICATION`
17. `SECURITY_AUDIT_PRIVACY`
18. `TEST_STRATEGY_ACCEPTANCE`
19. `RETENTION_POLICY`
20. `PRODUCTION_OPERATIONS`
21. `UI_DESIGN_SYSTEM_FINAL`

Detailed specifications may add implementation detail within their assigned domain but must preserve the terminology, state machines, permissions, invariants, and product boundaries established by the frozen V6 contract.

---

**End of Document — 01_PRODUCT_BASELINE**

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

## Approved Country contract — CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74

The canonical Country catalogue is the complete ISO 3166-1 list. The canonical persisted and API value is an uppercase ISO 3166-1 alpha-2 code: Romania=`RO`, Germany=`DE`, United States=`US`. The human-readable country name is a presentation label only. Country selectors do not permit free text for new values; new values require catalogue membership and fail-closed validation. Complete selector options are available locally/cached for local-first/offline-first operation. Existing optional/null semantics remain unchanged.

Already persisted non-conforming Country values are preserved unchanged and require a separate audit before any migration. No implicit normalization, country-name-to-code conversion, truncation or historical/snapshot rewriting is authorized. This clarification does not assign a catalogue version identifier or authorize any data/schema migration.

## Approved Supabase canonical backend contract — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

Supabase is the sole canonical backend platform for SiloMath application runtime. PostgreSQL remains the authoritative enterprise datastore. Server-authoritative business operations are implemented through Supabase PostgreSQL functions and/or Supabase Edge Functions. Android may access Supabase services directly only where the API/security contract explicitly permits the path and RLS/authorization enforce the required boundaries. Critical mutations remain server-authoritative, atomic and idempotent. The frozen offline-first/local-first model, Calculation Engine contract, historical integrity and `/api/v1` semantics remain unchanged except for the approved platform/topology mapping.

No separate Render/Ktor or equivalent external compute backend is part of the target core runtime architecture. Non-core auxiliary services such as source control/CI, application distribution or explicitly approved observability/integration services may be used, but they may not become authoritative for core SiloMath business state without a future approved Baseline Change.

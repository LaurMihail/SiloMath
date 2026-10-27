# SiloMath V6 — 02_DECISION_REGISTER

## Decision Register

**Document ID:** 02_DECISION_REGISTER  
**Version:** V6 Final Documentation — Decision Register 1.1  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-27 - CR-CALC-VERSION-CONTRACT-001 approved calculation-version clarification
**Product:** SiloMath  
**Normative role:** Canonical register of approved V6 decisions

## 1. Purpose

This register records the approved decisions that define SiloMath V6. Every entry is APPROVED / FROZEN unless a specialist V6 specification refines implementation detail without changing the decision's normative meaning.

The register uses two stable traceability namespaces: D01–D36 for core consolidation decisions and FD-01–FD-73 for final freeze decisions approved during the Pre-Documentation Freeze, documentation-structure freeze, and the approved UI Design System authority extension.

## 2. Decision Status Model

| Status | Meaning |
| --- | --- |
| APPROVED / FROZEN | Normative V6 decision. Implementation and documentation must conform. |
| REFINEMENT ALLOWED | Technical detail may be elaborated in the designated specification while preserving the decision's behavior and invariants. |

All decisions listed below have status APPROVED / FROZEN.

## 3. Core Decision Register — D01–D36

| ID | Decision | Normative outcome | Primary domain |
| --- | --- | --- | --- |
| D01 | Campaign Code exists | Inventory Campaign has a distinct human-facing Campaign Code in addition to its technical stable ID. | Campaigns |
| D02 | Campaign Code is generated and read-only | Campaign Code is system-generated and cannot be edited by the user. | Campaigns |
| D03 | Campaign Code format and uniqueness | Official format is INV-YYYY-NNNN. The server generates the sequence atomically. The code is unique within Organization and calendar year. UUID remains the technical identity. | Campaigns / API / Database |
| D04 | Inventory Type enumeration | Inventory Type is exactly Annual, Monthly, or Control. | Campaigns |
| D05 | Start Date | Start Date is mandatory and is distinct from creation and launch timestamps. | Campaigns |
| D06 | Planned End Date | Planned End Date is mandatory, must be on or after Start Date, and does not automatically close a Campaign. | Campaigns |
| D07 | Campaign list search and filters | CAM-01 supports search by Campaign Name or Campaign Code, status filtering by Active/Completed, and optional Inventory Type filtering. | UI / Campaigns |
| D08 | Campaign Team minimum | Campaign Team contains at least one member. The Admin who launches the Campaign is not automatically required to belong to the Campaign Team. | Campaigns / Authorization |
| D09 | Large Storage Unit stocktaking UI | STK-03 must efficiently support Storage Units containing many Storage Spaces through lazy/adaptive loading. Fixed pagination such as 10 items per page is not a business rule. | UI / Stocktaking |
| D10 | Stocktaking back-context restoration | Returning from STK-05 to STK-03 restores the relevant Storage Unit context and list/scroll state. | UI / Navigation |
| D11 | Measurement aggregation model | Non-empty stocktaking uses 1–5 readings, MEAN_ULLAGE_V1, one common datum, no automatic outlier discard, and the approved level-based simplification. | Stocktaking / Calculation |
| D12 | Mark Empty workflow | Mark Empty is an action, not a status. It results in Validated, isEmpty=true, Product Volume = 0 and Physical Quantity = 0. Measurements, Commodity, density, Ownership and Client are not required for an empty record. | Stocktaking |
| D13 | Stocktaking Notes | Notes are optional evidence/context on a Stocktaking Record, are preserved after validation and in history, and do not substitute for an Issue. | Stocktaking / Evidence |
| D14 | Photo evidence optionality | Photo evidence is optional unless a more specific approved rule explicitly requires it. | Evidence |
| D15 | Evidence and storage contract | Evidence types are Photo and Note. Photo limits, JPEG normalization, EXIF orientation correction, 2048 px long edge, size limits, SHA-256, app-private local storage, object storage, metadata persistence and durable offline synchronization follow the approved Evidence/Storage specification. | Evidence / Storage |
| D16 | Issue statuses | Issue status is exactly Open or Resolved. Issue history is preserved. | Issues |
| D17 | Issue resolution | Issue resolution is explicit and audited. Validate and Mark Empty do not automatically resolve an Open Issue. | Issues / Audit |
| D18 | Inventory Close semantics | Close is Admin-only and online. The server atomically revalidates Close Guards. All scoped Storage Spaces must be final/Validated, no To Do work may remain, mandatory data must be valid, required critical operations must be synchronized, and Open Issues must be resolved. An Admin may accept an unresolved matter for follow-up only by explicitly transitioning the Issue to Resolved with reason and audit. Success creates exactly one Final Inventory Snapshot and sets Campaign to Completed; failure creates no partial final state. | Close / Snapshot / Sync |
| D19 | Campaign Snapshot minimum content | Campaign Snapshot preserves campaign configuration and identity, Organization, exact scope, hierarchy context, geometry family/profile/version and calculation parameters, relevant Commodity identity/version data, team, Inventory Type, Start/Planned End dates, and required calculation/schema/version identifiers. | Campaign Snapshot |
| D20 | Final Inventory Snapshot content | The immutable Final Inventory Snapshot preserves campaign and hierarchy context, frozen geometry, Commodity, Ownership/Client, empty state, measurements, operational values, volume, quantity, calculation metadata, validation actor/time, issue references, evidence/notes references, and close actor/time. Historical results are not recalculated from current master data. | Final Snapshot |
| D21 | Canonical report inventory | V6 contains exactly nine report types: Physical Inventory, Inventory by Location, Inventory by Commodity, Inventory by Ownership, Storage Space Worksheet, Capacity Report, Inventory History, Digital Inventory Dossier, and Audit Trail. Completed official reports use Final Inventory Snapshot data. | Reporting |
| D22 | Reporting design | Reports use a Single Authoritative Report Dataset for UI and exports. Completed-campaign outputs are snapshot-based; active-campaign monitoring is clearly non-final. Export capabilities follow each report contract. | Reporting |
| D23 | Inventory History design | Inventory History compares Final Inventory Snapshots from 2..N Completed Campaigns. It supports Close Date, Campaign, Commodity, Location, Ownership and Physical Quantity comparison, absolute and percentage change, and comparability classifications Comparable, Scope Added, Scope Removed, and Context Changed. It does not forecast. | History / Reporting |
| D24 | User lifecycle | User lifecycle is Active / Disabled. Disabled blocks new authentication, new operations and new assignments while preserving history. Reactivation uses the same identity. | Users / Security |
| D25 | Session restoration | V6 does not use a Remember Me control. Session restoration is automatic and secure. | Authentication |
| D26 | SSO scope | Google and Microsoft SSO are not part of V6. | Authentication |
| D27 | Notification Center | V6 includes an in-app Notification Center with read/unread state, timestamp, title, message, contextual reference/navigation, Mark Read and Mark All Read. Notifications are distinct from Audit Trail. | Notifications |
| D28 | Settings design | Settings distinguishes User Preferences, Admin-only Organization Configuration, and non-editable System-Controlled Rules. UI language is English. Sync Status is operational visibility, not sync-engine configuration. About is consolidated into Support. Diagnostic packages are safe by default. Sign Out does not automatically destroy unsynchronized business data. | Settings / Support |
| D29 | Campaign Offline Working Set | A Campaign is OFFLINE_READY only after its complete authorized campaign-scoped working set is downloaded and integrity-verified. Save & Launch does not imply Offline Ready. Arbitrary whole-organization replication is not required. | Offline |
| D30 | Offline sync and conflict resolution | Supported local mutations commit business change and SyncOperation atomically. Sync uses durable outbox operations, idempotency, entity versioning, explicit conflicts, dependency/order preservation and retry with backoff. Competing measurements are not auto-merged. Validated cannot be overwritten by stale data; Completed cannot be downgraded; Final Snapshot is immutable. | Sync |
| D31 | Calculation formulas and families | Calculation Engine V2 supports the ten approved geometry families F01–F10 and the approved level-based calculation pipeline. F08–F10 use verified constant area with provenance and are invalid where cross-section varies with height. | Calculation |
| D32 | Calculation precision and canonical units | Canonical units are m, m², m³, kg/m³ and kg. Calculation is deterministic, uses at least 15 significant digits for intermediate processing, controlled constants, and no intermediate rounding. Persistence uses six decimal places with ROUND_HALF_UP; PostgreSQL numeric values use NUMERIC(20,6) where applicable. | Calculation / Database |
| D33 | Commodity governance | V6 uses the approved platform-governed set of 24 Commodities. Actual Bulk Density is the fundamental quantity input. Reference Test Weight is never an automatic fallback. Moisture and Impurities do not automatically correct Physical Quantity. | Commodity |
| D34 | Master-data lifecycle | Historically referenced master data uses Active/Inactive lifecycle and is not hard-deleted in a way that destroys historical references. | Master Data / History |
| D35 | Data retention | Final Snapshot, critical Audit events, official Evidence, Issues/resolution history and Campaign Snapshot are retained for at least 10 years. Synced diagnostic operations may be retained online for 90 days; unresolved failures are not auto-deleted; notifications are retained server-side for 12 months. Device cleanup is guarded by durability/sync conditions. | Retention |
| D36 | Production operations | Environments are Development, Staging and Production. Migrations are forward-safe; risky changes use Expand→Migrate→Contract. PostgreSQL is backed up at least daily with minimum 30-day retention and PITR where supported. Restore tests are quarterly. Objectives are RPO ≤1 hour and RTO ≤4 hours. Release, monitoring, secret handling and integrity-first failure behavior follow the approved Production Operations contract. | Operations |

## 4. Final Freeze Decision Register — FD-01–FD-73

| ID | Decision | Normative outcome | Primary domain |
| --- | --- | --- | --- |
| FD-01 | P4 UI decomposition | Inventory Close uses exactly two full screens: CLS-01 Campaign Review & Close and CLS-02 Final Inventory Snapshot. Review is a process, not a Campaign status. | UI / Close |
| FD-02 | Reports UI decomposition | Reports use RPT-01 Reports, reusable RPT-02 Report Viewer, dedicated HIS-01 Inventory History, DOS-01 Digital Inventory Dossier, and AUD-01 Audit Trail. The nine report types remain unchanged. | UI / Reporting |
| FD-03 | Users UI | User administration uses USR-01 Users and USR-02 User Details, without an additional full-screen Add/Edit destination. | UI / Users |
| FD-04 | Storage Unit Details | STU-02 Storage Unit Details is retained as the master-data detail destination between Storage Units in Location and Storage Space Details. | UI / Master Data |
| FD-05 | Geometry Profile UI | Geometry uses GEO-01 Geometry Profiles, GEO-02 Geometry Profile Details, and GEO-SH-01 Add/Edit Geometry Profile. Geometry Families have no customer administration screen. | UI / Geometry |
| FD-06 | Commodity UI | Commodity uses COM-01 Commodities and read-only COM-02 Commodity Details. Organization users cannot Add/Edit platform-governed Commodity definitions. | UI / Commodity |
| FD-07 | Client master data | Client is organization-scoped master data with CLI-01 Clients, CLI-02 Client Details, and CLI-SH-01 Add/Edit Client. Third-Party stocktaking references a stable Client identity. | UI / Domain |
| FD-08 | Organization UI | Organization uses ORG-01 Select Organization and ORG-02 Create/Edit Organization; there is no separate Organization Details screen. | UI / Organization |
| FD-09 | Notification UI | NOT-01 Notification Center is a full screen. There is no separate Notification Details screen; contextual notifications navigate to their target. | UI / Notifications |
| FD-10 | Sync UI | SYN-01 Sync Status is a full screen. Sync conflicts are handled contextually without a separate SYN-02 destination. | UI / Sync |
| FD-11 | Settings and Support UI | V6 uses SET-01 Settings and SUP-01 Support. About is part of Support. Organization Configuration is an Admin-only section in Settings. No separate About/Diagnostics/Organization Settings full screen is created. | UI / Settings |
| FD-12 | Campaign Details | CAM-02 Campaign Details is the dedicated detail screen for both Active and Completed Campaigns. It is distinct from CAM-03 Create & Launch Campaign. | UI / Campaigns |
| FD-13 | Storage Space Stocktaking workspace | STK-05 Storage Space Stocktaking is the single operational workspace for geometry context, measurements, calculation result, Commodity/density, Ownership/Client, evidence, Validate, Mark Empty and Report Issue. No separate Measurements/Calculation/Validation full screens are created. | UI / Stocktaking |
| FD-14 | Dashboard behavior | DSH-01 Dashboard is the single Dashboard screen. Cards and indicators navigate to existing canonical functional destinations rather than Dashboard-specific drill-down screens. | UI / Navigation |
| FD-15 | Password recovery UI | Authentication includes AUT-01 Sign In, AUT-SH-01 Forgot Password, and AUT-02 Reset Password. | UI / Authentication |
| FD-16 | Location UI | Location navigation is LOC-01 Locations → LOC-02 Storage Units in Location; there is no separate Location Details screen. Add/Edit uses P1-SH-01. | UI / Master Data |
| FD-17 | Storage Space editing | P1-SH-03 is Add/Edit Storage Space. Bulk/range creation is available only in Add mode; bulk edit is not introduced. | UI / Master Data |
| FD-18 | Issues remain contextual | There is no global ISS-01 or ISS-02. P0-SH-01 Report Issue creates Issues, while viewing and resolving Issues remains contextual to Stocktaking. | UI / Issues |
| FD-19 | More navigation hub | MOR-01 More is a full-screen navigation hub with no business logic of its own. | UI / Navigation |
| FD-20 | Audit Trail access | AUD-01 Audit Trail is Admin-only. Contextual audit metadata required for operational understanding may still be visible to Standard users where authorized. | Permissions / Audit |
| FD-21 | Report access by role | Reports are available to Admin and Standard within authorized scope. AUD-01 remains Admin-only. | Permissions / Reporting |
| FD-22 | Master-data mutation permission | All customer-managed Master Data mutations are Admin-only. Standard has read-only access within authorized scope. | Permissions / Master Data |
| FD-23 | Campaign Team controls stocktaking mutation | Stocktaking mutations require Campaign Team membership for both Admin and Standard. Administrative privileges remain separate from operational team membership. | Permissions / Stocktaking |
| FD-24 | Final UI inventory rule | The final UI inventory is derived from approved V6 functionality rather than a predetermined historical count. The frozen inventory is 36 full screens + 7 sheets = 43 major UI components. | UI |
| FD-25 | Campaign creation contextual components | Campaign Scope Selector and Team Assignment remain contextual components of CAM-03 Create & Launch Campaign, not separate major full-screen destinations. | UI / Campaigns |
| FD-26 | Offline status UI | No separate Offline Working Set screen is created. Offline readiness is shown contextually in Campaign/Stocktaking and in detail through SYN-01. | UI / Offline |
| FD-27 | Evidence UI | No separate Evidence Gallery screen is created. Evidence is contextual to Stocktaking, Issues, Final Snapshot and Dossier. | UI / Evidence |
| FD-28 | Campaign permissions | Create/Launch Campaign, Campaign Scope management and Campaign Team assignment are Admin-only. | Permissions / Campaigns |
| FD-29 | Review and Close permissions | Authorized users may inspect Review information, but Inventory Close is Admin-only and requires connectivity. | Permissions / Close |
| FD-30 | Issue permissions at close | Campaign Team members may Report Issue within authorized Stocktaking. Issue resolution is explicit and authorized. Admin-only close handling may accept a matter for follow-up only through an audited resolution with reason. | Permissions / Issues |
| FD-31 | Bottom navigation | Operational bottom navigation is exactly Dashboard / Inventory Campaigns / Stocktaking / Locations / More, mapped to DSH-01, CAM-01, STK-01, LOC-01, MOR-01. Authentication is outside the operational shell. | Navigation |
| FD-32 | Navigation identity and restoration | Routes transport stable IDs rather than complete business objects. Deep links pass through authentication/authorization. STK-05 → STK-03 restores relevant Storage Unit and list context. | Navigation |
| FD-33 | Stocktaking uniqueness | There is conceptually at most one Stocktaking Record for a given Campaign × Storage Space. | Domain / Database |
| FD-34 | Snapshot cardinality | Successful Close creates exactly one Final Inventory Snapshot per Campaign, containing per-Storage-Space snapshot entries. | Domain / Database |
| FD-35 | Client scoping | Client is Organization-scoped master data. Third-Party records reference Client by stable ID. | Domain / Database |
| FD-36 | Derived read models | Report datasets and Inventory History are derived/read models, not mutable sources of truth. | Domain / Reporting |
| FD-37 | Organization data scoping | Tenant-specific business data has explicit Organization ownership/scoping or an unambiguous relationship to Organization. | Data / Security |
| FD-38 | Evidence binary persistence | Large Evidence binaries are not stored as Room/PostgreSQL BLOBs. Databases store metadata, references and hashes; binaries use private local/object storage. | Data / Evidence |
| FD-39 | Database identity and deletion rules | Business entities use stable UUID-style technical identities. Campaign Code remains separate. Destructive cascades are not used where historical references could be invalidated. | Database |
| FD-40 | Concurrency and outbox persistence | Synchronizable entities use entity versioning. Local business mutation and SyncOperation are persisted atomically in the durable outbox model. | Database / Sync |
| FD-41 | REST and server authorization | The backend contract is REST-based and server-side authorization is authoritative for protected operations. | API / Security |
| FD-42 | Idempotent synchronized mutations | Synchronizable mutations carry idempotency key and base/entity version; retries reuse the same idempotency key. | API / Sync |
| FD-43 | Atomic Close API | Close is an online atomic server operation: revalidate guards, create Final Snapshot and transition Campaign to Completed as one logical transaction. | API / Close |
| FD-44 | Server-generated Campaign Code | The official Campaign Code is generated by the server during Save & Launch; the Android client does not generate the official sequence. | API / Campaigns |
| FD-45 | Offline package API | Offline Working Set download is Campaign-scoped, authorized and integrity-verifiable through manifest/metadata. | API / Offline |
| FD-46 | Evidence transfer | Evidence uses controlled binary upload, hash verification and metadata; image binaries are not embedded as ordinary business JSON payloads. | API / Evidence |
| FD-47 | Historical report source | Completed historical reports read Final Inventory Snapshot data and do not recalculate history from current master data. | API / Reporting |
| FD-48 | Target modular architecture | SiloMath adopts pragmatic Gradle modularization: app composition, core modules, feature modules and a separate Calculation Engine. Micro-modularization by screen/entity is not required. | Architecture |
| FD-49 | Dependency direction | Feature modules may depend on Core; Core must not depend on Feature. :app composes navigation/DI and contains minimal business logic. | Architecture |
| FD-50 | Pure Kotlin Calculation Engine | Calculation Engine is a separate pure Kotlin module with no Compose, Android UI, Room, Retrofit or ViewModel dependencies. | Architecture / Calculation |
| FD-51 | Incremental modular migration | Migration of the existing Android project toward the target module topology is incremental and follows dependency-graph audit; no big-bang rewrite is required. | Architecture |
| FD-52 | Traceability model | Final documentation and implementation verification use Requirement/Decision → Business Rule → UI → Domain → DB/API → Test traceability. | Documentation / QA |
| FD-53 | Decision coverage | D01–D36 and all approved freeze decisions must be represented in final documentation and, where implementable, in acceptance or automated tests. | Documentation / QA |
| FD-54 | Cross-document consistency audit | Documentation freeze requires a transversal audit of terminology, enums, permissions, state transitions, cardinalities, UI/navigation, schema, API, offline/sync and test coverage. | Documentation / QA |
| FD-55 | Final documentation scope | The final V6 documentation describes only the approved current product contract. The authoritative set consists of 21 documents, with Markdown as the semantic source and DOCX as the publication-format counterpart. Documents 01–20 retain their existing product, domain, architecture, operational, and functional authority; Document 21 is the canonical visual-design authority within the boundary defined by FD-57. | Documentation |
| FD-56 | UI specification document | Document 06 is UI_SCREEN_SPECIFICATION_AND_NAVIGATION and is the canonical detailed specification for all 36 screens + 7 sheets and the complete Navigation Graph. | Documentation / UI |
| FD-57 | UI Design System authority | Document 21 is 21_UI_DESIGN_SYSTEM and is the canonical visual-design authority for SiloMath V6. It defines visual tokens, typography, spacing, shapes, shared component presentation, accessibility presentation, iconography, and adaptive visual rules. It MUST NOT redefine screen inventory, Navigation Graph, product behavior, permissions, business rules, domain states, calculations, data contracts, API behavior, or offline/sync semantics. Document 06 remains the canonical detailed UI functional/navigation specification. If Document 21 conflicts semantically or behaviorally with Documents 01–20, Documents 01–20 prevail and the conflict must be reported rather than silently resolved. | Documentation / UI Design |
| FD-58 | Current Jetpack navigation | SiloMath uses Jetpack Navigation 3 for the single-activity Compose navigation stack. Destination keys are type-safe/serializable, stable business IDs are passed as arguments, and product navigation semantics remain those frozen by Document 06. | Architecture / Navigation |
| FD-59 | Predictive Back | All supported back behavior uses supported system/Jetpack back APIs and participates in predictive back where available without bypassing authorization, state restoration, or frozen workflow guards. | Navigation / Android |
| FD-60 | Edge-to-edge and insets | Production Compose UI is edge-to-edge safe and handles system bars, cutouts, gesture insets, and IME insets; required actions/content may not be obscured. | UI / Android |
| FD-61 | Lifecycle-aware UI state | Compose observes ViewModel state lifecycle-aware; inactive destinations do not keep unnecessary upstream UI collection active. | Architecture / UI |
| FD-62 | State-driven operation outcomes | Durable business outcomes and critical success/failure are represented in durable domain/UI state. Ephemeral navigation/presentation commands never become an alternative business-state authority. | Architecture / UI |
| FD-63 | Window-adaptive semantics | Compact/Medium/Expanded describe the current application window, not a permanent device class. The same frozen product semantics apply during resize, multi-window, orientation and supported posture changes. | UI / Adaptive |
| FD-64 | Stitch visual-quality floor | Approved SiloMath Stitch references are the minimum production visual-quality benchmark for hierarchy, usable density, component refinement, state coverage and enterprise polish; they never override Documents 01–20 business semantics. | UI Design / QA |
| FD-65 | Dynamic color policy | Android system dynamic color does not override canonical SiloMath semantic or brand tokens. Platform-derived color may only be used in explicitly non-semantic decorative contexts that preserve contrast and brand consistency. | UI Design |
| FD-66 | Android performance release engineering | Release engineering uses R8 full-mode optimization and verifies representative critical user journeys with Baseline/Startup Profiles and Macrobenchmark where supported; no main-thread blocking I/O is permitted in production paths. | Architecture / Testing / Operations |
| FD-67 | Location geospatial facility coordinates | Location may hold an optional authoritative latitude/longitude pair for facility map presentation. Coordinates are Admin-managed Organization-scoped master data, never user/device tracking, and are either both present or both absent. | Master Data / UI / Data |
| FD-68 | Location List/Map UX | LOC-01 presents the same authorized Location dataset as synchronized List and Map modes. Map selection routes/selects the existing Location hierarchy; lack of basemap/network never blocks the authoritative List/hierarchy. No GPS/background-location or turn-by-turn capability is introduced. | UI / Locations / Offline |
| FD-69 | Implementation closure | The approved V6 documentation set is implementation-ready. Non-semantic technical choices that preserve Documents 01–21 are implementation decisions and do not require Product/Baseline approval; architecturally material choices may be recorded through IADR. Baseline Change is required only when frozen product behavior or structure is intentionally changed. | Governance / Implementation |
| FD-70 | User versus Membership transport provenance | API projections that combine User and OrganizationMembership data preserve the canonical metadata/version of each entity separately. User metadata must never be fabricated from Membership metadata. `UserMembershipResponse` is an explicit composition of `UserResponse` and `MembershipResponse`. | API / Identity / Data |
| FD-71 | Environment endpoint provisioning | Concrete Development/Staging/Production hostnames/base URLs, certificates and provider endpoints are deployment configuration, not Product Baseline values. They are supplied through controlled environment configuration consistent with the frozen `/api/v1` contract and require no Product/Baseline approval when semantics/security boundaries remain unchanged. | Operations / API / Implementation |
| FD-72 | CampaignSnapshot Commodity compatibility physical mapping | CampaignSnapshot `commodityCompatibilitySnapshot` is persisted as an explicit immutable N-valued child collection through canonical physical table `campaign_snapshot_commodity_items`, containing `snapshot_commodity_item_id`, `campaign_snapshot_id`, `commodity_id` and `commodity_version_id`. It represents launch-time Commodity compatibility context and must not be replaced by opaque JSON or inferred later from mutable current Commodity state. | Campaign Snapshot / Data / Database |
| FD-73 | Approved Calculation Version Contract | `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Correct Doc13 section20 aggregationMethod wording; preserve all existing formulaCode values and mathematical semantics. Approved through CR-CALC-VERSION-CONTRACT-001. | Calculation / API / Offline / History |

## 5. Consolidated Decision Effects

### 5.1 Product lifecycle

- Campaign lifecycle is Active → Completed; Review is a process, not a Campaign status.
- Campaign Scope is immutable after Save & Launch.
- Master data uses Active/Inactive where applicable and preserves historical references.
- User lifecycle is Active/Disabled; Issue lifecycle is Open/Resolved.
- Final Inventory Snapshot is immutable.

### 5.2 Authorization

- Admin and Standard are the only organization roles.
- Master Data mutations are Admin-only.
- Campaign creation, scope management, team assignment and Inventory Close are Admin-only.
- Stocktaking mutations require Campaign Team membership for either role.
- Reports are available within authorized scope; Audit Trail is Admin-only.
- Server-side authorization is authoritative for protected operations.

### 5.3 Historical integrity

- Campaign Snapshot freezes launch-time campaign context.
- CampaignSnapshot Commodity compatibility is frozen through the explicit immutable `campaign_snapshot_commodity_items` child collection and is not reconstructed from later mutable Commodity state.
- Final Inventory Snapshot freezes completed inventory history.
- Completed historical reports read the Final Inventory Snapshot.
- Historical calculations are never recalculated from current master data.
- Historically referenced master data is not destructively deleted.

### 5.4 Offline and synchronization

- Save & Launch does not imply Offline Ready.
- Offline readiness requires complete campaign-scoped package download and integrity verification.
- Local business mutation and SyncOperation commit atomically.
- Synchronization uses idempotency, entity versioning, explicit conflict states and dependency-aware ordering.
- Validated data cannot be overwritten by stale offline data; Completed cannot be downgraded to Active.

### 5.5 Calculation

- V6 uses the approved ten Geometry Families F01–F10.
- Non-empty records use 1–5 compatible measurements and MEAN_ULLAGE_V1 under LEVEL_BASED_V1.
- Actual Bulk Density is the quantity input; Reference Test Weight is not an automatic fallback.
- Canonical units are m, m², m³, kg/m³ and kg.
- No intermediate rounding is permitted; persistence uses six decimals and ROUND_HALF_UP, with PostgreSQL NUMERIC(20,6) where applicable.

### 5.6 UI and navigation

- Final major UI inventory is 36 full screens + 7 sheets = 43 components.
- Bottom navigation is Dashboard / Inventory Campaigns / Stocktaking / Locations / More.
- STK-05 is the single Storage Space stocktaking workspace.
- P4 uses Campaign Review & Close followed by Final Inventory Snapshot.
- Reports use a catalog, reusable viewer, and dedicated History/Dossier/Audit destinations.
- UI navigation passes stable IDs and preserves required stocktaking context.
- Document 21 is the canonical visual-design authority; Document 06 remains canonical for UI functionality, screen inventory, controls, behavior and navigation.

### 5.7 Architecture

- Native Android, Kotlin, Jetpack Compose, Clean Architecture, MVVM/UDF and offline-first remain the application baseline.
- Gradle modularization is pragmatic rather than screen-by-screen.
- Feature → Core dependencies are allowed; Core → Feature dependencies are prohibited.
- Calculation Engine is a separate pure Kotlin module.

## 6. Traceability Requirements

Every applicable decision must be traceable through:

Decision → Business Rule → Workflow/State Machine → UI → Domain → Database/API → Test

Not every decision requires an artifact in every layer. A layer may be marked not applicable only where the decision genuinely has no responsibility in that layer.

- decision identifier coverage;
- consistent canonical terminology;
- enum/state consistency;
- role and scope consistency;
- cardinality and invariant consistency;
- UI/navigation consistency;
- persistence/API validation consistency;
- offline/sync consistency;
- calculation/version metadata consistency;
- acceptance-test coverage for enforceable decisions.

## 7. Governance

01_PRODUCT_BASELINE defines the overall product contract. This Decision Register provides stable identifiers for the approved decisions behind that contract. Specialist documents may add implementation detail, schemas, endpoint structures, validation mechanics, UI behavior, visual design rules, or test cases, but must not alter the normative outcome of an approved decision. For UI work, Document 06 governs functional UI/navigation requirements and Document 21 governs visual design within the FD-57 boundary.

If a future product change intentionally changes one of these decisions, it must be handled as an explicit new approved change to the V6 contract rather than as an undocumented implementation deviation.

**End of Document — 02_DECISION_REGISTER**

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

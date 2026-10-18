# SiloMath V6 — Business Rules

**Document ID:** 03_BUSINESS_RULES  
**Version:** V6 Final Documentation — Business Rules 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-17 — Android/UX implementation-closure refreeze  
**Product:** SiloMath  
**Normative role:** Canonical registry of enforceable V6 business rules. Rules in this document are implementation-independent and apply across UI, domain, persistence, API, synchronization, reporting, security, and tests as relevant.

---

## 1. Purpose

This document converts the approved V6 product decisions into stable, testable business rules. It is the canonical business-rule registry for SiloMath V6.

Each rule has a stable `BR-*` identifier. Specialist specifications may define implementation mechanics, data types, endpoint structures, UI controls, or test cases, but must preserve the normative meaning of these rules.

## 2. Rule Interpretation

The terms **must**, **must not**, **only**, **exactly**, and **requires** are normative. A rule applies in every technical layer that has responsibility for enforcing or representing it.

Where a rule references an approved decision, the decision identifier provides traceability to `02_DECISION_REGISTER`. Rules do not create alternative product behavior.

## 3. Rule Registry

| Rule ID | Rule | Normative requirement | Decision trace | Domain |
|---|---|---|---|---|
| **BR-GOV-001** | Organization is the tenant boundary | All tenant-specific business data must belong to an Organization directly or through an unambiguous relationship. Cross-organization access is prohibited unless an explicitly approved platform operation requires it. | D24, FD-37, FD-41 | Security; Data |
| **BR-GOV-002** | Canonical roles | The only organization roles are Admin and Standard. | D24 | Authorization |
| **BR-GOV-003** | User lifecycle | User lifecycle is Active or Disabled. Disabled users cannot start new authentication, operations, or assignments; historical identity remains preserved. Reactivation uses the same identity. | D24 | Users |
| **BR-GOV-004** | Server authorization | Protected operations must be authorized server-side; UI visibility is not an authorization boundary. | FD-41 | Security; API |
| **BR-GOV-005** | Stable technical identity | Business entities use stable technical IDs. Human-facing codes such as Campaign Code are separate identifiers. | D01, D03, FD-39 | Data |
| **BR-AUTHZ-001** | Master Data mutations | Customer-managed Master Data create/edit/activate/inactivate operations are Admin-only. Standard access is read-only within authorized scope. | FD-22 | Authorization; Master Data |
| **BR-AUTHZ-002** | Campaign administration | Create/Launch Campaign, Campaign Scope management, and Campaign Team assignment are Admin-only. | FD-28 | Authorization; Campaigns |
| **BR-AUTHZ-003** | Stocktaking mutation eligibility | A user may mutate Stocktaking data only when the user is authorized for the Organization/Campaign and is a Campaign Team member, regardless of whether the organization role is Admin or Standard. | D08, FD-23 | Authorization; Stocktaking |
| **BR-AUTHZ-004** | Inventory Close | Inventory Close is Admin-only and requires connectivity. | D18, FD-29 | Authorization; Close |
| **BR-AUTHZ-005** | Reports | Admin and Standard may access reports within authorized scope. Audit Trail is Admin-only. | D21, FD-20, FD-21 | Authorization; Reporting |
| **BR-AUTHZ-006** | Issue actions | Authorized Campaign Team members may report Issues in Stocktaking. Resolution is explicit and authorized. Close-time follow-up acceptance is Admin-only and requires an audited reason. | D17, D18, FD-30 | Authorization; Issues |
| **BR-MD-001** | Physical hierarchy | The canonical physical hierarchy is Organization → Location → Storage Unit → Storage Space. | FD-37 | Master Data |
| **BR-MD-002** | Master lifecycle | Historically referenced customer-managed Master Data uses Active/Inactive lifecycle and must not be destructively deleted in a way that breaks history. | D34, FD-39 | Master Data; History |
| **BR-MD-003** | Client scope | Client is Organization-scoped Master Data and is referenced by stable identity. | FD-07, FD-35 | Master Data |
| **BR-MD-004** | Geometry governance | Geometry Families and their mathematical definitions are platform-governed. Customer Admin may manage Geometry Profiles only within approved Geometry Families. | D31, FD-05 | Geometry |
| **BR-MD-005** | Geometry versioning | Geometry Profiles used for calculation must be versioned. Campaign and historical calculation context must retain the exact Geometry Profile Version used. | D19, D20 | Geometry; History |
| **BR-MD-006** | Maximum Capacity | Maximum Capacity is derived automatically from approved geometry parameters and the applicable geometry formula. | D31 | Geometry |
| **BR-MD-007** | Commodity governance | Commodity definitions are platform-governed and use the approved V6 set of 24 Commodities. Organization users cannot create arbitrary Commodity definitions. | D33, FD-06 | Commodity |
| **BR-MD-008** | Commodity lifecycle and history | Commodity definitions may be Active/Inactive, but historical references and required version context must be preserved. | D33, D34 | Commodity; History |
| **BR-MD-009** | Storage Space bulk creation | Add Storage Space may support approved multi-apply/range creation. Bulk/range behavior is Add-only and does not imply bulk edit. | FD-17 | Master Data; UI |
| **BR-MD-010** | Location facility coordinates | A Location may store an optional authoritative facility coordinate pair. Latitude and longitude are either both present or both absent; latitude is within -90..90 and longitude within -180..180. | FD-67 | Master Data; Geo |
| **BR-MD-011** | Location coordinate authority | Facility coordinates are Admin-managed Organization-scoped Master Data. Setting/moving/clearing the map position is an audited/versioned Location mutation and Standard remains read-only. | FD-67 | Master Data; Authorization |
| **BR-MD-012** | No user-location tracking | Facility map presentation does not authorize device GPS acquisition, background location, user tracking, route guidance, or turn-by-turn navigation. Android location permission is not required for the approved V6 map capability. | FD-67, FD-68 | Privacy; UI |
| **BR-MD-013** | List/map equivalence and offline fallback | LOC-01 List and Map represent the same authorized Location query/filter result. Business data and hierarchy remain authoritative and usable offline even when map basemap content is unavailable. | FD-68 | UI; Offline |
| **BR-CAM-001** | Campaign statuses | Campaign status is exactly Active or Completed. Review is a workflow process, not a status. | D18, FD-01 | Campaigns |
| **BR-CAM-002** | No Draft | Campaign creation uses Save & Launch and does not create a Draft state. | D03, FD-28 | Campaigns |
| **BR-CAM-003** | Campaign Code | Campaign Code is system-generated, read-only, formatted `INV-YYYY-NNNN`, and generated atomically by the server. It is unique within Organization and calendar year. | D01, D02, D03, FD-44 | Campaigns; API |
| **BR-CAM-004** | Inventory Type | Inventory Type is exactly Annual, Monthly, or Control. | D04 | Campaigns |
| **BR-CAM-005** | Campaign dates | Start Date and Planned End Date are mandatory. Planned End Date must be on or after Start Date and does not trigger automatic close. | D05, D06 | Campaigns |
| **BR-CAM-006** | Campaign Team minimum | A Campaign Team contains at least one member. The launching Admin need not be a team member. | D08 | Campaigns |
| **BR-CAM-007** | Scope freeze | Campaign Scope becomes immutable after Save & Launch. | D19, FD-25 | Campaigns |
| **BR-CAM-008** | Campaign Snapshot | Launch must preserve a Campaign Snapshot containing the exact scope and sufficient hierarchy, geometry, Commodity, team, date, inventory-type, schema and calculation-version context required for deterministic operation and history. | D19 | Campaigns; History |
| **BR-CAM-009** | Save & Launch vs Offline Ready | Save & Launch does not make a Campaign Offline Ready. | D29, FD-26 | Campaigns; Offline |
| **BR-CAM-010** | Campaign list discovery | Campaign list supports search by Name or Code, Active/Completed filtering, and optional Inventory Type filtering. | D07 | Campaigns; UI |
| **BR-STK-001** | Stocktaking record uniqueness | There is at most one conceptual Stocktaking Record for a given Campaign × Storage Space. | FD-33 | Stocktaking; Data |
| **BR-STK-002** | Operational classifications | Stocktaking operational classifications are To Do, Issues, and Validated. All is an aggregate UI filter and is not a persistent business state. | D12, FD-13 | Stocktaking |
| **BR-STK-003** | Completion condition | A Storage Space is complete for campaign close only when its Stocktaking Record is Validated, including the Mark Empty path. | D12, D18 | Stocktaking; Close |
| **BR-STK-004** | Measurement count | A non-empty Stocktaking Record uses 1–5 valid measurements. | D11 | Stocktaking; Calculation |
| **BR-STK-005** | Measurement compatibility | Measurements aggregated together must use the same Geometry Profile Version, same measurement datum, same measurement type, and units that can be normalized to canonical units. | D11, D31 | Stocktaking; Calculation |
| **BR-STK-006** | Geometry read-only during stocktaking | Geometry cannot be edited from Stocktaking. A geometry anomaly must be handled through Report Issue. | FD-13, FD-18 | Stocktaking; Geometry |
| **BR-STK-007** | Mark Empty semantics | Mark Empty produces Validated with `isEmpty=true`, Product Volume = 0 and Physical Quantity = 0. Measurements, Commodity, Actual Bulk Density, Ownership and Client are not required for an empty record. | D12 | Stocktaking |
| **BR-STK-008** | Mark Empty audit | Mark Empty records actor and timestamp. Permitted Notes and Photos may be retained. | D12, D13, D14 | Stocktaking; Audit |
| **BR-STK-009** | Validation immutability against stale data | Validated data cannot be overwritten by stale offline data. | D30 | Stocktaking; Sync |
| **BR-STK-010** | Notes | Stocktaking Notes are optional, preserved after validation/history, and do not substitute for an Issue. | D13 | Stocktaking; Evidence |
| **BR-STK-011** | Photos | Photo evidence is optional unless a more specific approved rule explicitly requires it. | D14 | Stocktaking; Evidence |
| **BR-STK-012** | Large-unit performance | Storage Unit stocktaking must support large Storage Space collections through efficient/lazy/adaptive loading. A fixed 10-items-per-page rule is not a business constraint. | D09 | Stocktaking; UI |
| **BR-STK-013** | Back-context preservation | Returning from Storage Space Stocktaking to Storage Unit Stocktaking restores the relevant Storage Unit and list/scroll context. | D10, FD-32 | Stocktaking; Navigation |
| **BR-OWN-001** | Ownership values | For non-empty stocktaking, Ownership is mandatory and is exactly Owned or Third-Party. | D12, D33 | Stocktaking |
| **BR-OWN-002** | Third-Party Client | For non-empty Third-Party stocktaking, Client is mandatory and referenced by stable Client identity. | FD-07, FD-35 | Stocktaking; Client |
| **BR-OWN-003** | Owned Client | For Owned stocktaking, Client is not required. | FD-35 | Stocktaking; Client |
| **BR-COM-001** | Actual Bulk Density | Actual Bulk Density is the operational density input used to compute Physical Quantity and is normalized to kg/m³. | D33 | Commodity; Calculation |
| **BR-COM-002** | Reference Test Weight | Reference Test Weight must never be used automatically as a fallback for missing Actual Bulk Density. | D33 | Commodity; Calculation |
| **BR-COM-003** | Moisture and Impurities | Moisture and Impurities do not automatically correct Physical Quantity in V6. | D33 | Commodity; Calculation |
| **BR-CALC-001** | Calculation pipeline | For non-empty stocktaking the canonical pipeline is Measurements → Normalization → Aggregation → Effective Fill Height → Geometry Engine → Product Volume → Quantity Engine → Physical Quantity. | D31 | Calculation |
| **BR-CALC-002** | Aggregation | V6 aggregation is `MEAN_ULLAGE_V1`: arithmetic mean of 1–5 valid compatible ullage readings. Automatic outlier discard is prohibited. | D11, D31 | Calculation |
| **BR-CALC-003** | Surface model | V6 uses `LEVEL_BASED_V1` for the approved level-based surface simplification. | D11, D31 | Calculation |
| **BR-CALC-004** | Effective fill height | Effective Fill Height is `measurementReferenceHeightM - effectiveUllageM` and must remain within the approved geometry fill range. | D31 | Calculation |
| **BR-CALC-005** | Quantity formula | For a non-empty record, Physical Quantity in kg is Product Volume in m³ multiplied by Actual Bulk Density in kg/m³. | D31, D33 | Calculation |
| **BR-CALC-006** | Canonical units | Canonical units are m, m², m³, kg/m³ and kg. `1 kg/hl = 10 kg/m³`. | D32 | Calculation |
| **BR-CALC-007** | No silent correction | Invalid or ambiguous dimensional/calculation inputs block the operation. The engine must not silently clamp invalid values or invent missing interpretations. | D31 | Calculation |
| **BR-CALC-008** | Precision | Calculation is deterministic, uses at least 15 significant digits for intermediate processing, controlled mathematical constants, and no intermediate rounding. | D32 | Calculation |
| **BR-CALC-009** | Persistence rounding | Where the numeric persistence contract applies, values persist at six decimal places using `ROUND_HALF_UP`; PostgreSQL uses `NUMERIC(20,6)`. | D32 | Calculation; Database |
| **BR-CALC-010** | Calculation metadata | Calculation results preserve formula/schema/engine/surface/aggregation versions, datum/reference/fill metadata, inputs, outputs, fingerprint and other required reproducibility metadata. | D19, D20, D31 | Calculation; History |
| **BR-CALC-011** | Historical calculation immutability | Historical calculations are not recalculated from current Geometry, Commodity or other master data. | D20, FD-47 | Calculation; History |
| **BR-CALC-012** | Verified-area families | F08–F10 require a positive verified constant cross-sectional area with provenance. They are not applicable where cross-section varies with height. | D31 | Calculation; Geometry |
| **BR-ISS-001** | Issue statuses | Issue status is exactly Open or Resolved. | D16 | Issues |
| **BR-ISS-002** | Report Issue minimum data | Report Issue requires Issue Type and Description. Evidence is optional. | D15, D16 | Issues |
| **BR-ISS-003** | Explicit resolution | Issue resolution is explicit, authorized, and audited. | D17 | Issues; Audit |
| **BR-ISS-004** | No automatic resolution | Validate and Mark Empty do not automatically resolve an Open Issue. | D17 | Issues |
| **BR-ISS-005** | Close-time follow-up | A matter accepted for follow-up at Close must be explicitly transitioned from Open to Resolved with reason and audit by an authorized Admin; an Open Issue is not silently carried through Close. | D18, FD-30 | Issues; Close |
| **BR-EVD-001** | Evidence types | Evidence types are Photo and Note. | D15 | Evidence |
| **BR-EVD-002** | Photo limits | Maximum photo count is five per Stocktaking Record and five per Issue. | D15 | Evidence |
| **BR-EVD-003** | Photo normalization | Photos are normalized to JPEG; EXIF orientation is corrected; long edge is limited to 2048 px; target size is ≤1.5 MB and hard maximum after processing is 3 MB. | D15 | Evidence |
| **BR-EVD-004** | Evidence integrity | SHA-256 is mandatory for evidence binaries. | D15 | Evidence; Security |
| **BR-EVD-005** | Evidence storage | Binary evidence uses app-private local storage and synchronized object storage. Room/PostgreSQL store metadata, references, hashes, relationships and sync state rather than large image BLOBs. | D15, FD-38 | Evidence; Storage |
| **BR-EVD-006** | Offline evidence durability | Offline evidence save must persist the local private file and metadata/outbox durably before UI success is reported. | D15, D30 | Evidence; Offline |
| **BR-EVD-007** | Evidence sync states | Evidence synchronization uses Local Pending → Uploading → Synced, with Sync Error for failed transfer and retry. | D15 | Evidence; Sync |
| **BR-EVD-008** | Validated evidence immutability | Evidence belonging to a validated historical context becomes read-only/immutable according to the evidence contract. | D15 | Evidence; History |
| **BR-EVD-009** | Evidence privacy | Unnecessary EXIF is stripped. GPS/device identifiers are not automatically collected merely for evidence. | D15 | Evidence; Privacy |
| **BR-CLS-001** | Close connectivity | Inventory Close requires connectivity and cannot be finalized offline. | D18, FD-29 | Close |
| **BR-CLS-002** | Close guards | Before Close, Campaign must be Active; all scoped Storage Spaces must be final/Validated; no To Do work may remain; mandatory data must be valid; required critical operations must be synchronized; Issues must satisfy the approved resolution rule; and authorization must pass. | D18 | Close |
| **BR-CLS-003** | Server revalidation | The server revalidates Close Guards atomically at Close time. | D18, FD-43 | Close; API |
| **BR-CLS-004** | Exactly one final snapshot | Successful Close creates exactly one Final Inventory Snapshot per Campaign with per-Storage-Space entries. | D18, FD-34 | Close; Snapshot |
| **BR-CLS-005** | Completed transition | Successful Close changes Campaign from Active to Completed and records close actor/time. | D18 | Close |
| **BR-CLS-006** | No partial close | If Close fails, Campaign remains Active and no partial Final Inventory Snapshot/final state is committed. | D18, FD-43 | Close |
| **BR-CLS-007** | Completed terminality | Completed cannot transition back to Active. | D30 | Close; Sync |
| **BR-SNP-001** | Final Snapshot authority | For a Completed Campaign, the Final Inventory Snapshot is the authoritative historical inventory source. | D20, D21 | Snapshot; Reporting |
| **BR-SNP-002** | Final Snapshot content | Final Snapshot preserves campaign/hierarchy context, frozen geometry/version, Commodity, Ownership/Client where applicable, empty state, measurements, operational values, volume, density where applicable, quantity, calculation metadata, validation actor/time, issue/evidence references, and close actor/time. | D20 | Snapshot |
| **BR-SNP-003** | Final Snapshot immutability | Final Inventory Snapshot is immutable after successful Close. | D20, D30 | Snapshot |
| **BR-RPT-001** | Canonical report set | V6 contains exactly nine report types: Physical Inventory, Inventory by Location, Inventory by Commodity, Inventory by Ownership, Storage Space Worksheet, Capacity Report, Inventory History, Digital Inventory Dossier, and Audit Trail. | D21 | Reporting |
| **BR-RPT-002** | Completed report source | Official reports for Completed Campaigns use Final Inventory Snapshot data. | D21, FD-47 | Reporting |
| **BR-RPT-003** | Active reporting | Active-Campaign reporting/monitoring may use current operational data but must be clearly identified as non-final. | D22 | Reporting |
| **BR-RPT-004** | Single authoritative dataset | A report's UI and export representations derive from one authoritative report dataset for that report request. | D22 | Reporting |
| **BR-HIS-001** | History source | Inventory History compares Final Inventory Snapshots from two or more Completed Campaigns. | D23 | History |
| **BR-HIS-002** | History dimensions | History supports comparison by Close Date, Campaign, Commodity, Location, Ownership and Physical Quantity. | D23 | History |
| **BR-HIS-003** | Change metrics | Inventory History may show absolute and percentage Physical Inventory Change. | D23 | History |
| **BR-HIS-004** | Comparability | History comparability states include Comparable, Scope Added, Scope Removed and Context Changed. | D23 | History |
| **BR-HIS-005** | No forecasting | Inventory History does not provide forecasts or predictions. | D23 | History |
| **BR-RPT-005** | Derived reporting models | Report datasets and Inventory History are derived/read models and are not mutable business sources of truth. | FD-36 | Reporting; Domain |
| **BR-OFF-001** | Campaign-scoped offline set | Offline data is limited to the authorized Campaign Offline Working Set required for campaign execution; arbitrary whole-Organization replication is not required. | D29 | Offline |
| **BR-OFF-002** | Offline Ready condition | A device is Offline Ready for a Campaign only after the complete authorized package is downloaded and integrity verified. | D29, FD-45 | Offline |
| **BR-OFF-003** | Offline working-set content | The working set includes campaign header/status/snapshot/scope, required hierarchy, pinned geometry/schema/Commodity/calculation compatibility data, team authorization, existing stocktaking data, Ownership/Client data, required evidence, sync metadata and integrity manifest. | D29 | Offline |
| **BR-SYNC-001** | Atomic local write | Supported local business mutation and its SyncOperation are committed in the same Room transaction. UI Saved state occurs only after local commit. | D30, FD-40 | Sync |
| **BR-SYNC-002** | Sync operation states | Sync Operation states are Pending, In Flight, Acknowledged, Retryable Error, Conflict and Permanent Error. | D30 | Sync |
| **BR-SYNC-003** | Idempotency | Synchronizable mutations use an idempotency key; retry reuses the same key. | D30, FD-42 | Sync; API |
| **BR-SYNC-004** | Optimistic concurrency | Synchronizable entities use entity versioning/base server version. Stale writes produce explicit Conflict rather than silent overwrite. | D30, FD-40, FD-42 | Sync |
| **BR-SYNC-005** | No silent LWW | Critical competing business changes must not be resolved by silent last-write-wins. | D30 | Sync |
| **BR-SYNC-006** | No automatic competing-measurement merge | Competing measurements are not automatically merged. | D30 | Sync |
| **BR-SYNC-007** | Ordering and dependencies | Synchronization preserves same-entity FIFO ordering and required operation dependencies. Critical operations are not silently coalesced. | D30 | Sync |
| **BR-SYNC-008** | Retry | Retryable synchronization failures use controlled retry with exponential backoff. | D30 | Sync |
| **BR-SYNC-009** | Conflict durability | Sync conflicts remain explicit/durable until resolved according to the approved conflict workflow. | D30, D35 | Sync |
| **BR-NOT-001** | Notification Center data | Notification includes read/unread state, timestamp, title, message and contextual reference where applicable, and supports Mark Read / Mark All Read. | D27 | Notifications |
| **BR-NOT-002** | Notification vs audit | Notifications are not Audit Trail events and must not substitute for audit history. | D27 | Notifications; Audit |
| **BR-SET-001** | Settings classes | Settings distinguishes User Preference, Organization Configuration and System-Controlled Rule. | D28 | Settings |
| **BR-SET-002** | Organization Configuration | Organization Configuration is Admin-only and limited to explicitly configurable organization settings. | D28, FD-11 | Settings; Authorization |
| **BR-SET-003** | System-controlled rules | Formula definitions, engine/aggregation rules, maximum measurement count, canonical units, Commodity definitions, workflow statuses, Ownership/Client rule, Close Guards, snapshot behavior, conflict strategy, evidence hash rules, retention, authentication and platform limits are not user-editable settings. | D28 | Settings |
| **BR-SET-004** | UI language | V6 UI language is English and has no language selector. | D28 | Settings; UI |
| **BR-SET-005** | Sync Status safety | Sync Status provides operational visibility but does not expose custom server URL, user-defined sync interval, disable sync, force overwrite, or selectable conflict strategy. | D28, FD-10 | Settings; Sync |
| **BR-SET-006** | Support | About information is integrated into Support. Diagnostic packages are safe-by-default and exclude secrets, photos and full business payloads unless an explicitly approved support workflow requires otherwise. | D28, FD-11 | Support; Privacy |
| **BR-SET-007** | Sign Out safety | Sign Out must not automatically destroy unsynchronized local business data. | D28 | Authentication; Offline |
| **BR-AUTH-001** | Authentication capabilities | V6 supports Sign In, Forgot Password, Reset Password and secure automatic session restoration. | D25, FD-15 | Authentication |
| **BR-AUTH-002** | Authentication exclusions | V6 does not provide public Sign Up, Remember Me, Google SSO or Microsoft SSO. | D25, D26 | Authentication |
| **BR-RET-001** | Ten-year historical retention | Final Inventory Snapshot, critical Audit events, official Evidence tied to validated/final history, Issues/resolution history and Campaign Snapshot are retained for at least 10 years. | D35 | Retention |
| **BR-RET-002** | Sync diagnostic retention | Acknowledged/Synced operation diagnostics may be retained online for 90 days before purge/archive where allowed; the corresponding critical business event remains in long-term audit history. | D35 | Retention |
| **BR-RET-003** | Failed/conflict retention | Unresolved failed operations are not auto-deleted. Conflict records persist through resolution plus the approved diagnostic retention period; critical resolution remains auditable. | D35 | Retention |
| **BR-RET-004** | Notification retention | Notifications are retained server-side for 12 months. | D35 | Retention |
| **BR-RET-005** | Device cleanup guards | Device cache cleanup is allowed only when pending operations/conflicts/evidence durability and required server-side historical durability conditions are satisfied. | D35 | Retention; Offline |
| **BR-OPS-001** | Environments | Production environments are exactly Development, Staging and Production. | D36 | Operations |
| **BR-OPS-002** | Migration safety | Database migrations are forward-safe. Risky changes use Expand → Migrate → Contract and must not destroy historical integrity. | D36 | Operations; Database |
| **BR-OPS-003** | Backup | PostgreSQL is backed up automatically at least daily, with at least 30 days backup retention and PITR where supported; relevant risky migrations/releases require a pre-change backup. | D36 | Operations |
| **BR-OPS-004** | Restore testing | Restore tests are performed quarterly in an isolated environment. | D36 | Operations |
| **BR-OPS-005** | Recovery objectives | Operational objectives are RPO ≤ 1 hour and RTO ≤ 4 hours. | D36 | Operations |
| **BR-OPS-006** | Release path | Release follows Build → Automated Tests → Migration Validation → Staging → Production → Smoke Test. | D36 | Operations |
| **BR-OPS-007** | Calculation regression | Calculation formula changes require golden regression tests. | D36 | Operations; Testing |
| **BR-OPS-008** | Secrets | Secrets must not be stored in Git, embedded in the APK, emitted to logs, or included in normal diagnostic packages. | D36 | Operations; Security |
| **BR-OPS-009** | Integrity over availability | When consistency is uncertain, the system blocks Inventory Close rather than creating an inconsistent Final Inventory Snapshot. Valid offline stocktaking may continue when its working set is complete. | D36 | Operations; Close |

## 4. Cross-Domain Invariants

These invariants are especially important because they cross multiple modules and must be enforced consistently:

1. A Completed Campaign cannot transition back to Active.
2. Campaign Scope cannot change after Save & Launch.
3. Save & Launch does not create a Draft and does not imply Offline Ready.
4. Stocktaking mutations require Campaign Team membership.
5. Master Data mutations are Admin-only.
6. Geometry is read-only during Stocktaking.
7. A non-empty Stocktaking Record uses one to five compatible measurements.
8. Mark Empty produces Validated with zero Product Volume and zero Physical Quantity.
9. Validate and Mark Empty do not automatically resolve Open Issues.
10. Validated data cannot be overwritten by stale offline data.
11. Third-Party non-empty stocktaking requires a Client.
12. Reference Test Weight cannot substitute automatically for Actual Bulk Density.
13. Historical calculations are not recalculated from current master data.
14. Inventory Close is Admin-only, online, and server-revalidated.
15. Successful Close creates exactly one immutable Final Inventory Snapshot.
16. Failed Close creates no partial final state.
17. Completed official reports use Final Inventory Snapshot data.
18. Historically referenced Master Data is not destructively deleted.
19. Synchronization conflicts are explicit; critical data does not use silent last-write-wins.
20. System-Controlled Rules cannot be changed through Settings.

## 5. Validation and Enforcement Principles

### 5.1 Client and server

The Android client should prevent invalid actions early and provide clear feedback, but client-side validation is not sufficient for protected or integrity-critical operations. The server must revalidate authorization and server-owned invariants for remote mutations.

### 5.2 Offline operations

Rules that can be evaluated from a complete authorized Offline Working Set may be enforced locally to permit supported offline work. Synchronization must revalidate applicable server-side authorization, version, and integrity constraints.

### 5.3 Close

Inventory Close is a special integrity boundary. The server is authoritative for the final guard evaluation, snapshot creation, and Active → Completed transition.

### 5.4 Historical data

Historical snapshots, calculation metadata, audit events, and historically referenced identities must remain interpretable for the required retention period. Current master-data edits must not alter completed historical meaning.

## 6. Rule-to-Test Requirement

Every enforceable business rule must have one or more acceptance criteria in `18_TEST_STRATEGY_ACCEPTANCE`. Rules with deterministic domain behavior should also have automated unit or integration coverage at the lowest appropriate layer.

At minimum:

- calculation rules require pure-domain tests and golden regression coverage where formulas are involved;
- state transitions and Close Guards require domain/API integration tests;
- authorization rules require server-side authorization tests and relevant UI visibility/action tests;
- database constraints and history protections require persistence/integration tests;
- offline/sync rules require retry, idempotency, ordering, stale-version and conflict tests;
- UI-only navigation/context rules require navigation/UI tests where practical.

## 7. Governance

`01_PRODUCT_BASELINE` defines the product-level contract. `02_DECISION_REGISTER` records the approved decisions. This document expresses those decisions as stable business rules. The remaining V6 documents must reference and implement these rules without redefining their meaning.

If a future approved product change modifies a business rule, the affected decision, business rule, specialist specifications, traceability, and tests must be updated together.

---

**End of Document — 03_BUSINESS_RULES**
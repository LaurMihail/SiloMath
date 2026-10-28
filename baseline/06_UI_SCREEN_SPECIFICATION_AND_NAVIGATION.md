# SiloMath V6 — UI Screen Specification and Navigation

**Document ID:** `06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION`  
**Version:** V6 Final Documentation — UI Specification 2.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-28 - CR-API-STORAGE-SPACE-BATCH-001 approved atomic Storage Space batch-create clarification
**Product:** SiloMath  
**Normative role:** Canonical UI inventory, per-component behavior, access, validation, offline behavior, adaptive behavior, and Navigation Graph for V6.

---

## 1. Purpose

This document is the canonical implementation specification for all **36 full screens + 7 sheets = 43 major UI components** in SiloMath V6. It specializes Documents 01–05.

Each component uses: **ID → Name → Purpose → Access → Entry points → Layout/sections → Displayed data → User actions → Validation → UI states → Offline behavior → Permissions → Navigation → Back behavior → Adaptive behavior → Related business rules → Acceptance criteria.**

Presentation states are UI-only and must not become new persistent business lifecycle states.

## 2. Global UI Contract

### 2.1 Platform and presentation
- Native Android, Kotlin, Jetpack Compose.
- Single-activity Jetpack Navigation 3 with type-safe, serializable destination keys.
- Window-adaptive UI across compact/medium/expanded application windows, portrait/landscape, resize, multi-window, and supported posture changes.
- UI language: English.
- Compose renders state and dispatches events; business calculations/direct DB access are outside composables.
- ViewModels coordinate UI state and approved use cases. Compose observes ViewModel state lifecycle-aware.
- Durable business outcomes and critical success/failure are represented in durable UiState/domain state; transient navigation/presentation commands cannot become business-state authority.
- Production UI is edge-to-edge safe and handles system bars, cutouts, gesture insets, and IME insets.

### 2.2 Operational shell
Authentication is outside the operational shell.

**Bottom navigation, exactly:** `Dashboard / Inventory Campaigns / Stocktaking / Locations / More`

Operational header: app identity/title + sync + notifications + avatar/user context. Back appears on detail/add/edit contexts.

### 2.3 Navigation identity
- Jetpack Navigation 3 is the canonical navigation stack for V6. Destination keys are type-safe and serializable.
- Stable technical IDs are passed between destinations, not complete mutable business objects.
- Deep links pass authentication and authorization.
- System back uses supported Jetpack/system APIs and predictive back where available. Custom back interception is permitted only for an explicit frozen workflow guard such as confirmed unsaved-edit handling.
- Organization boundaries are preserved.
- `STK-05 → STK-03` restores Storage Unit and relevant list/scroll context.
- Campaign Scope Selector and Team Assignment are contextual CAM-03 components.
- Issue handling, Evidence, calculation, validation, and offline readiness remain contextual.

### 2.4 Authorization
- Roles exactly `Admin` and `Standard`.
- Master Data mutations: Admin-only.
- Campaign create/launch, scope/team administration, Close: Admin-only.
- Stocktaking mutation: Campaign Team membership required for both roles.
- Reports: authorized scope; Audit Trail Admin-only.
- UI visibility never replaces server authorization.

### 2.5 Offline and sync
- Save & Launch ≠ Offline Ready.
- Offline-capable mutation reports Saved only after atomic local business + SyncOperation commit.
- Sync state is not business lifecycle state.
- Conflict/error is explicit; no false success.
- Inventory Close is online.

### 2.6 Canonical presentation
- Campaign: `Active / Completed`.
- Inventory Type: `Annual / Monthly / Control`.
- Stocktaking: `To Do / Issues / Validated / All`; `All` UI-only.
- Issue: `Open / Resolved`.
- Mark Empty is an action resulting in Validated + `isEmpty=true`.
- Completed official reporting reads immutable Final Inventory Snapshot.

## 3. Canonical Navigation Graph

```text
AUT-01 Sign In
 ├─ AUT-SH-01 Forgot Password → AUT-02 Reset Password → AUT-01
 └─ authenticated → ORG-01 Select Organization → DSH-01

Operational shell:
DSH-01 | CAM-01 | STK-01 | LOC-01 | MOR-01

CAM-01 → CAM-02
CAM-01 → CAM-03 → [Scope + Team contextual components] → Save & Launch → CAM-02
CAM-02 → STK-01 → STK-02 → STK-03 → STK-05
                                      └─ P0-SH-01 Report Issue
CAM-02 → CLS-01 → CLS-02

LOC-01 → LOC-02 → STU-02 → STS-02
GEO-01 → GEO-02 → GEO-SH-01
COM-01 → COM-02
CLI-01 → CLI-02 → CLI-SH-01

RPT-01 → RPT-02 / HIS-01 / DOS-01 / AUD-01
USR-01 → USR-02
SET-01 → SYN-01 / SUP-01
MOR-01 routes to authorized non-bottom-navigation destinations.
```

## 4. Major UI Inventory

### 4.1 Full screens — 36
1. `AUT-01` — Sign In
2. `AUT-02` — Reset Password
3. `DSH-01` — Dashboard
4. `ORG-01` — Select Organization
5. `ORG-02` — Create/Edit Organization
6. `LOC-01` — Locations
7. `LOC-02` — Storage Units in Location
8. `STU-02` — Storage Unit Details
9. `STS-02` — Storage Space Details
10. `GEO-01` — Geometry Profiles
11. `GEO-02` — Geometry Profile Details
12. `COM-01` — Commodities
13. `COM-02` — Commodity Details
14. `CLI-01` — Clients
15. `CLI-02` — Client Details
16. `CAM-01` — Inventory Campaigns
17. `CAM-02` — Campaign Details
18. `CAM-03` — Create & Launch Campaign
19. `STK-01` — Stocktaking Overview
20. `STK-02` — Location Stocktaking
21. `STK-03` — Storage Unit Stocktaking
22. `STK-05` — Storage Space Stocktaking
23. `CLS-01` — Campaign Review & Close
24. `CLS-02` — Final Inventory Snapshot
25. `RPT-01` — Reports
26. `RPT-02` — Report Viewer
27. `HIS-01` — Inventory History
28. `DOS-01` — Digital Inventory Dossier
29. `AUD-01` — Audit Trail
30. `USR-01` — Users
31. `USR-02` — User Details
32. `MOR-01` — More
33. `NOT-01` — Notification Center
34. `SYN-01` — Sync Status
35. `SET-01` — Settings
36. `SUP-01` — Support

### 4.2 Sheets — 7
1. `AUT-SH-01` — Forgot Password
2. `P1-SH-01` — Add/Edit Location
3. `P1-SH-02` — Add/Edit Storage Unit
4. `P1-SH-03` — Add/Edit Storage Space
5. `GEO-SH-01` — Add/Edit Geometry Profile
6. `CLI-SH-01` — Add/Edit Client
7. `P0-SH-01` — Report Issue

---

# 5. Full Screen Specifications

## AUT-01 — Sign In

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Authenticate an existing Active user and establish a secure session. |
| **Access** | Public / unauthenticated |
| **Entry points** | App start without valid session; session restoration failure/expiry. |
| **Layout / sections** | Product identity; credential inputs; Sign In primary action; Forgot Password secondary action; validation/error area. |
| **Displayed / edited data** | Authentication credentials and non-sensitive session feedback. |
| **User actions** | Sign In; open Forgot Password. |
| **Validation** | Required credential fields; server authentication and Active-user state are authoritative. |
| **UI states** | Initial; submitting; field validation error; authentication error; success. |
| **Offline behavior** | Authentication requires server access; no offline sign-in authority is created. |
| **Permissions** | No authenticated role yet; Disabled users cannot start new authentication. |
| **Navigation** | Success → ORG-01 when organization selection is required, otherwise authorized operational context; Forgot Password → AUT-SH-01. |
| **Back behavior** | System back exits/dismisses authentication flow according to Android navigation; no operational destination is exposed. |
| **Adaptive behavior** | Single-column phone; centered constrained form on larger widths; portrait/landscape supported. |
| **Related business rules** | `BR-GOV-003, BR-GOV-004` |
| **Acceptance criteria** | Valid Active user can sign in; invalid/Disabled user cannot; no Remember Me, public Sign Up, Google SSO, or Microsoft SSO controls. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| AUT01-01 | Email | Email text field | Email | Yes | Yes | — | Valid email syntax | Trim; non-empty; valid email | Always | Authentication request | Public | Online required |
| AUT01-02 | Password | Password field | Secret | Yes | Yes | — | Server-controlled password policy | Non-empty; never logged/persisted as plaintext | Always | Authentication request | Public | Online required |
| AUT01-03 | Show/Hide Password | Icon toggle | Boolean | No | Yes | Hidden | Hidden / Visible | Presentation only | With password | Local UI state | Public | Local |
| AUT01-04 | Sign In | Primary button | Action | — | Yes | — | — | Enabled only when required inputs valid | Always | Auth use case | Public | Online required |
| AUT01-05 | Forgot Password | Text action | Action | — | Yes | — | — | — | Always | Navigation | Public | Local navigation |


## AUT-02 — Reset Password

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Set a new password using a valid recovery context. |
| **Access** | Public recovery context |
| **Entry points** | Valid recovery link/context from password recovery. |
| **Layout / sections** | Recovery context indicator; new password; confirmation; primary Reset Password action; validation/error area. |
| **Displayed / edited data** | Recovery context/token state; password fields. |
| **User actions** | Reset Password. |
| **Validation** | Valid recovery context; password confirmation and authentication-policy validation; server acceptance. |
| **UI states** | Initial; submitting; validation error; expired/invalid recovery context; success. |
| **Offline behavior** | Requires server validation; not an offline workflow. |
| **Permissions** | No organization role required; recovery context is authoritative. |
| **Navigation** | Success → AUT-01; invalid/expired context remains in recovery/error flow. |
| **Back behavior** | Back → AUT-01 or previous recovery context without entering operational shell. |
| **Adaptive behavior** | Single-column form; constrained width on tablets. |
| **Related business rules** | `BR-AUTH-001` |
| **Acceptance criteria** | Successful reset returns to authentication flow; invalid/expired context cannot reset credentials. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| AUT02-01 | New Password | Password field | Secret | Yes | Yes | — | Server-controlled policy | Must satisfy current server policy | Valid recovery context | Reset request | Recovery context | Online required |
| AUT02-02 | Confirm New Password | Password field | Secret | Yes | Yes | — | — | Must equal New Password | Valid recovery context | Local validation | Recovery context | Local |
| AUT02-03 | Show/Hide Password | Icon toggle | Boolean | No | Yes | Hidden | Hidden / Visible | Presentation only | Password fields visible | Local UI state | Recovery context | Local |
| AUT02-04 | Reset Password | Primary button | Action | — | Yes | — | — | Valid recovery context + matching valid passwords | Always | Auth API | Recovery context | Online required |


## DSH-01 — Dashboard

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Provide the operational landing view and entry points into authorized SiloMath work. |
| **Access** | Admin / Standard within authorized Organization |
| **Entry points** | Operational shell after organization selection; bottom-nav Dashboard. |
| **Layout / sections** | Header with app identity/title, sync, notifications, avatar; operational summary/cards; persistent bottom navigation. |
| **Displayed / edited data** | Authorized organization context; campaign/stocktaking/report/master-data summary information available to the user. |
| **User actions** | Open canonical destinations from cards; use header sync/notifications/avatar; bottom-nav navigation. |
| **Validation** | No business mutation is performed directly by Dashboard cards. |
| **UI states** | Loading; content; empty/first-use; partial/offline data; error. |
| **Offline behavior** | May render authorized locally cached operational summaries; must not imply data is synchronized when it is not. |
| **Permissions** | Cards/actions are permission-filtered; UI visibility never replaces server authorization. |
| **Navigation** | Cards route to canonical destinations only; bottom nav → CAM-01/STK-01/LOC-01/MOR-01. |
| **Back behavior** | Back follows Android task behavior; no synthetic business back stack. |
| **Adaptive behavior** | Responsive card grid/list; bottom navigation remains operational; portrait/landscape. |
| **Related business rules** | `BR-GOV-004` |
| **Acceptance criteria** | Dashboard exposes no duplicate business workflow and routes to canonical screens. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DSH01-01 | Organization | Filter selector | UUID | No | Yes | Active organization | Authorized organizations | Authorized membership only | Always | Organization read model | Admin / Standard | Cached options |
| DSH01-02 | Campaign | Filter selector | UUID | No | Yes | Current/All | Authorized campaigns | Authorized scope | Always | Campaign read model | Admin / Standard | Cached options |
| DSH01-03 | Location | Filter selector | UUID | No | Yes | All | Authorized locations | Must belong to active Organization | Always | Master-data read model | Admin / Standard | Cached options |
| DSH01-04 | Commodity | Filter selector | UUID | No | Yes | All | Governed commodities | Active/current catalog for live views | Always | Commodity read model | Admin / Standard | Cached options |
| DSH01-05 | Ownership | Filter selector | Enum | No | Yes | All | Owned / Third-Party | Canonical enum | Always | Dashboard query | Admin / Standard | Local query |
| DSH01-06 | Client | Filter selector | UUID | No | Yes | All | Organization clients | Only organization-scoped clients | Always | Client read model | Admin / Standard | Cached options |
| DSH01-07 | Current Inventory by Commodity | KPI/card | Read model | — | No | — | — | No mutation | Always | Dashboard read model | Admin / Standard | Cached/clearly stale if offline |
| DSH01-08 | Facility / Bin Status | KPI/card | Read model | — | No | — | — | No mutation | Always | Dashboard read model | Admin / Standard | Cached |
| DSH01-09 | Active Campaigns | KPI/card | Read model | — | No | — | Active only | No Upcoming/In Progress | Always | Campaign read model | Admin / Standard | Cached |
| DSH01-10 | Open Issues | KPI/card | Read model | — | No | — | Open | Canonical Issue state | Always | Issue read model | Admin / Standard | Cached |


## ORG-01 — Select Organization

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Select an Organization for which the user has valid membership. |
| **Access** | Authenticated user |
| **Entry points** | Post-authentication when organization context is unresolved; organization switch flow where permitted. |
| **Layout / sections** | Header/title; authorized Organization list; selection state. |
| **Displayed / edited data** | Organizations available through current user's valid memberships. |
| **User actions** | Select Organization. |
| **Validation** | Selected Organization must be authorized for the authenticated user. |
| **UI states** | Loading; list; empty/no authorized organizations; error. |
| **Offline behavior** | Previously available authorized context may be shown only where locally safe; organization authorization is revalidated when remote operations occur. |
| **Permissions** | Only memberships of current user are visible. |
| **Navigation** | Selection → DSH-01 operational shell scoped to Organization. |
| **Back behavior** | Back returns to authentication/previous authorized context without crossing tenant boundaries. |
| **Adaptive behavior** | List on phone; wider list/pane treatment permitted on tablets without changing behavior. |
| **Related business rules** | `BR-GOV-001, BR-GOV-004` |
| **Acceptance criteria** | Cross-Organization unauthorized selection is impossible; active Organization context is explicit. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ORG01-01 | Search | Search field | String | No | Yes | Empty | Name / Code | Trim; case-insensitive search | Always | Organization read model | Admin / Standard | Local search |
| ORG01-02 | Organization list | Lazy list | Collection | — | No | — | Authorized only | Membership authorization | Always | Organization read model | Admin / Standard | Cached organizations |
| ORG01-03 | Organization Name | List item text | String | — | No | — | — | — | Per item | Organization.name | Admin / Standard | Cached |
| ORG01-04 | Organization Code | List item text | String | — | No | — | — | — | Per item | Organization.code | Admin / Standard | Cached |
| ORG01-05 | Create Organization | Action | Action | — | Yes | — | — | Authorized provisioning context only | Provisioning-authorized only | Provisioning flow | Authorized provisioning Admin | Online when provisioning |


## ORG-02 — Create/Edit Organization

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Create or edit Organization data permitted by the V6 organization contract. |
| **Access** | Admin for edit; authorized creation context |
| **Entry points** | Authorized administration entry from More/organization administration context. |
| **Layout / sections** | Back header; organization form; Save/Cancel actions; validation/error area. |
| **Displayed / edited data** | Approved Organization fields only. |
| **User actions** | Create or save permitted Organization changes; cancel. |
| **Validation** | Required fields and server authorization; no System-Controlled Rules are editable here. |
| **UI states** | Loading existing data; create/edit; saving; validation error; server error; success. |
| **Offline behavior** | Do not report success until durable accepted operation according to applicable sync/server rules. |
| **Permissions** | Admin-only for organization mutation. |
| **Navigation** | Success returns to prior organization/admin context; Cancel/Back returns without saving. |
| **Back behavior** | Back discards only uncommitted UI edits after normal confirmation behavior if needed. |
| **Adaptive behavior** | Adaptive form; constrained width on tablets. |
| **Related business rules** | `BR-AUTHZ-001, BR-GOV-004` |
| **Acceptance criteria** | Standard cannot mutate Organization; only approved fields are editable. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ORG02-01 | Organization Name | Text field | String(150) | Yes | Yes | — | — | Trim; non-empty; max 150 | Always | Organization.name | Authorized Admin/provisioning | Local-first edit when existing; creation provisioning rules apply |
| ORG02-02 | Organization Code | Text field | String(50) | Yes | Create/edit subject to history rule | — | Uppercase normalized | Trim + uppercase; unique; case-insensitive; stable once historically referenced | Always | Organization.code | Authorized Admin/provisioning | Server uniqueness confirmed on sync |
| ORG02-03 | Address | Text field | String(250) | No | Yes | — | — | Max 250 | Always | Organization.address | Authorized Admin | Local-first |
| ORG02-04 | City | Text field | String(100) | No | Yes | — | — | Max 100 | Always | Organization.city | Authorized Admin | Local-first |
| ORG02-05 | Country | Selector (no free text) | ISO 3166-1 alpha-2 code | No | Yes | — | Complete ISO 3166-1 catalogue | Uppercase canonical code; catalogue membership; fail-closed new-value validation (FD-74) | Always | Organization.country; human-readable labels from local/cached catalogue | Authorized Admin | Complete options available locally/cached |
| ORG02-06 | Status | Selector | Enum | Yes | Yes | Active | Active / Inactive | Canonical lifecycle | Existing organization; creation defaults Active | Organization.lifecycleStatus | Authorized Admin | Local-first + sync |
| ORG02-07 | Save | Primary action | Action | — | Yes | — | — | All required/uniqueness rules | Always | Organization use case | Authorized Admin/provisioning | Local-first where allowed |


## LOC-01 — Locations

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Browse authorized Locations and provide Admin entry to Location creation/edit. |
| **Access** | Admin / Standard |
| **Entry points** | Bottom-nav Locations; master-data navigation. |
| **Layout / sections** | Header; search/filter; List / Map view selector; synchronized Location list or facility map; lifecycle indication; Admin add/edit affordance; bottom navigation. |
| **Displayed / edited data** | Authorized Organization Locations, lifecycle state, and optional stored facility map position. List and Map modes use the same filtered dataset. |
| **User actions** | Search/filter; switch List / Map; select list row or map marker → LOC-02 (or equivalent large-window detail pane retaining the same destination identity); Admin Add/Edit → P1-SH-01. |
| **Validation** | Stable IDs; lifecycle rules; mutation authorization. |
| **UI states** | Loading; list; map; empty; filtered-empty; map-with-no-positioned-results; basemap-unavailable/offline with authoritative list still usable; error. |
| **Offline behavior** | Read from authorized local master data when available; mutations follow durable sync rules. |
| **Permissions** | Standard read-only; Admin may mutate customer-managed master data. |
| **Navigation** | Location → LOC-02; Add/Edit → P1-SH-01. |
| **Back behavior** | Back returns to prior shell destination; bottom nav remains persistent. |
| **Adaptive behavior** | Compact windows show one primary mode at a time; medium/expanded windows may present coordinated list+map/master-detail panes while preserving LOC-01/LOC-02 destination identity and filters. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-001` |
| **Acceptance criteria** | No separate Location Details destination is introduced. List and Map show the same authorized/filter result; markers exist only for Locations with valid coordinates; Locations without coordinates remain accessible in List; no device location permission is requested for this capability. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| LOC01-01 | Search | Search field | String | No | Yes | Empty | Name / Code | Trim; case-insensitive | Always | Location read model | Admin / Standard | Local |
| LOC01-02 | Lifecycle Filter | Filter | Enum | No | Yes | Active | Active / Inactive / All | Canonical lifecycle | Always | Location query | Admin / Standard | Local |
| LOC01-03 | Location list | Lazy list | Collection | — | No | — | Authorized organization | Organization scoped | Always | Location read model | Admin / Standard | Cached |
| LOC01-04 | Add Location | FAB/button | Action | — | Yes | — | — | — | Admin only | Navigation to P1-SH-01 | Admin | Local navigation |
| LOC01-05 | View | Segmented control | Enum | No | Yes | List | List / Map | Presentation-only; same query/filter | Always | Local UI state | Admin / Standard | Local |
| LOC01-06 | Location Map | Map view | Collection<LocationCoordinate> | — | No | — | Valid-coordinate Locations from current filtered result | Same Organization/filters as list; absent/invalid coordinates never fabricated | View=Map; may be shown beside List on wider windows | Location read model | Admin / Standard | Stored coordinates local; basemap best-effort |


## LOC-02 — Storage Units in Location

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Browse Storage Units belonging to one selected Location. |
| **Access** | Admin / Standard |
| **Entry points** | LOC-01 Location selection. |
| **Layout / sections** | Back header with Location context; Storage Unit list; Admin add/edit affordances; persistent bottom nav. |
| **Displayed / edited data** | Location identity and its authorized Storage Units/lifecycle. |
| **User actions** | Open STU-02; Admin Add/Edit via P1-SH-02. |
| **Validation** | Location ID must be valid in active Organization; lifecycle/history rules apply. |
| **UI states** | Loading; list; empty; offline/local; error. |
| **Offline behavior** | Authorized cached hierarchy and stored Location coordinates are browsable offline. Basemap/tile availability is best-effort and must never block List/hierarchy use. |
| **Permissions** | Standard read-only; Admin mutation. |
| **Navigation** | Storage Unit → STU-02; Add/Edit → P1-SH-02. |
| **Back behavior** | Back → LOC-01 preserving relevant list context. |
| **Adaptive behavior** | Adaptive hierarchy list; tablet may use master-detail presentation while retaining same routes. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-001` |
| **Acceptance criteria** | Only units belonging to selected Location are shown. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| LOC02-01 | Location Name | Header/read-only | String | — | No | — | — | — | Always | Location snapshot/current | Admin / Standard | Cached |
| LOC02-02 | Location Code | Header/read-only | String | — | No | — | — | — | Always | Location.code | Admin / Standard | Cached |
| LOC02-03 | Search Storage Units | Search field | String | No | Yes | Empty | Name / Code | Trim; case-insensitive | Always | StorageUnit read model | Admin / Standard | Local |
| LOC02-04 | Lifecycle Filter | Filter | Enum | No | Yes | Active | Active / Inactive / All | Canonical lifecycle | Always | StorageUnit query | Admin / Standard | Local |
| LOC02-05 | Storage Unit list | Lazy list | Collection | — | No | — | Units in selected Location | Parent Location fixed | Always | StorageUnit read model | Admin / Standard | Cached |
| LOC02-06 | Add Storage Unit | Action | Action | — | Yes | — | — | — | Admin only | P1-SH-02 | Admin | Local navigation |


## STU-02 — Storage Unit Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Display one Storage Unit and its Storage Spaces. |
| **Access** | Admin / Standard |
| **Entry points** | LOC-02 Storage Unit selection. |
| **Layout / sections** | Back header; unit identity/details; Storage Space list; Admin edit/add affordances; bottom navigation. |
| **Displayed / edited data** | Storage Unit master data; child Storage Spaces; lifecycle. |
| **User actions** | Open STS-02; Admin edit unit via P1-SH-02; Admin add/edit space via P1-SH-03. |
| **Validation** | Stable hierarchy and lifecycle rules; no destructive historical mutation. |
| **UI states** | Loading; content; empty spaces; offline/local; error. |
| **Offline behavior** | Authorized cached hierarchy readable offline. |
| **Permissions** | Standard read-only; Admin mutation. |
| **Navigation** | Space → STS-02; edit/add sheets as above. |
| **Back behavior** | Back → LOC-02 preserving Location context. |
| **Adaptive behavior** | Responsive details + list; larger widths may use two-pane layout. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-001` |
| **Acceptance criteria** | STU-02 remains the detail destination between LOC-02 and STS-02. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STU02-01 | Storage Unit Name | Read-only value | String | — | No | — | — | — | Always | StorageUnit.name | Admin / Standard | Cached |
| STU02-02 | Storage Unit Code | Read-only value | String | — | No | — | — | — | Always | StorageUnit.code | Admin / Standard | Cached |
| STU02-03 | Description | Read-only value | String | — | No | — | — | — | When present | StorageUnit.description | Admin / Standard | Cached |
| STU02-04 | Status | Status chip | Enum | — | No | — | Active / Inactive | Canonical lifecycle | Always | StorageUnit.lifecycleStatus | Admin / Standard | Cached |
| STU02-05 | Storage Spaces | Lazy list | Collection | — | No | — | Children of unit | Parent fixed | Always | StorageSpace read model | Admin / Standard | Cached |
| STU02-06 | Edit Storage Unit | Action | Action | — | Yes | — | — | — | Admin only | P1-SH-02 | Admin | Local |
| STU02-07 | Add Storage Space | Action | Action | — | Yes | — | — | — | Admin only | P1-SH-03 | Admin | Local |


## STS-02 — Storage Space Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Display Storage Space master data, geometry assignment/version context and Maximum Capacity. |
| **Access** | Admin / Standard |
| **Entry points** | STU-02 Storage Space selection. |
| **Layout / sections** | Back header; identity/hierarchy; geometry profile/version summary; geometry parameters shown with two decimals; Maximum Capacity; lifecycle; Admin edit affordance. |
| **Displayed / edited data** | Storage Space data; Geometry Profile/Version; approved geometry parameters; calculated Maximum Capacity. |
| **User actions** | Admin edit via P1-SH-03; navigate to geometry detail where context supports it. |
| **Validation** | Geometry/profile references valid; displayed geometry parameters use two decimals; historical protection applies. |
| **UI states** | Loading; content; missing/inactive-reference warning where applicable; offline/local; error. |
| **Offline behavior** | Authorized cached master data readable offline. |
| **Permissions** | Standard read-only; Admin mutation of customer-managed Storage Space only. |
| **Navigation** | Edit → P1-SH-03; geometry reference → GEO-02 when navigable. |
| **Back behavior** | Back → STU-02 preserving unit context. |
| **Adaptive behavior** | Adaptive details; parameter grid may expand on tablets. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-004, BR-MD-005, BR-MD-006` |
| **Acceptance criteria** | Geometry parameters render with two decimals and no business calculation occurs in Compose. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STS02-01 | Storage Space Code | Read-only value | String | — | No | — | — | — | Always | StorageSpace.code | Admin / Standard | Cached |
| STS02-02 | Description | Read-only value | String | — | No | — | — | — | When present | StorageSpace.description | Admin / Standard | Cached |
| STS02-03 | Geometry Profile | Read-only link/value | UUID | — | No | — | — | Must resolve valid profile/version | Always | StorageSpace.geometryProfile | Admin / Standard | Cached |
| STS02-04 | Max Capacity | Read-only calculated value | Decimal | — | No | — | m³ | Derived from Geometry Profile; never manual | Always | Geometry engine/profile | Admin / Standard | Cached derived value |
| STS02-05 | Status | Status chip | Enum | — | No | — | Active / Inactive | Canonical lifecycle | Always | StorageSpace.lifecycleStatus | Admin / Standard | Cached |
| STS02-06 | Edit Storage Space | Action | Action | — | Yes | — | — | Historical/reference protections apply | Admin only | P1-SH-03 | Admin | Local |


## GEO-01 — Geometry Profiles

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Browse Organization Geometry Profiles created within platform-governed Geometry Families. |
| **Access** | Admin / Standard |
| **Entry points** | More/master-data entry. |
| **Layout / sections** | Header; profile list; family/lifecycle summary; Admin add/edit affordance. |
| **Displayed / edited data** | Geometry Profiles, current version/lifecycle, family identity. |
| **User actions** | Open GEO-02; Admin Add/Edit → GEO-SH-01. |
| **Validation** | Geometry Family/formula is platform-controlled; customer may manage Profiles only. |
| **UI states** | Loading; list; empty; offline/local; error. |
| **Offline behavior** | Authorized cached profiles readable offline. |
| **Permissions** | Standard read-only; Admin manages Profiles; neither role edits Geometry Families/formulas. |
| **Navigation** | Profile → GEO-02; Add/Edit → GEO-SH-01. |
| **Back behavior** | Back to prior More/master-data context. |
| **Adaptive behavior** | Adaptive list/grid; tablet detail pane permitted. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-004, BR-MD-005` |
| **Acceptance criteria** | No customer Geometry Family administration is exposed. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| GEO01-01 | Search | Search field | String | No | Yes | Empty | Profile Name / Code if exposed | Case-insensitive | Always | GeometryProfile read model | Admin / Standard | Local |
| GEO01-02 | Lifecycle Filter | Filter | Enum | No | Yes | Active | Active / Inactive / All | Canonical lifecycle | Always | GeometryProfile query | Admin / Standard | Local |
| GEO01-03 | Geometry Profile list | Lazy list | Collection | — | No | — | Organization profiles | Version-aware | Always | GeometryProfile read model | Admin / Standard | Cached |
| GEO01-04 | Add Geometry Profile | Action | Action | — | Yes | — | — | — | Admin only | GEO-SH-01 | Admin | Local |


## GEO-02 — Geometry Profile Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Show a Geometry Profile, its Geometry Family, current/versioned dimensions and calculation metadata relevant to master data. |
| **Access** | Admin / Standard |
| **Entry points** | GEO-01 selection; geometry reference from Storage Space. |
| **Layout / sections** | Back header; profile identity; family; version; dimensions; reference height/max fill; verified area/provenance where applicable; lifecycle; Admin edit action. |
| **Displayed / edited data** | Geometry Profile and versioned approved parameters. |
| **User actions** | Admin edit → GEO-SH-01. |
| **Validation** | Family-specific required parameters; no formula editing; values must represent approved schema. |
| **UI states** | Loading; content; inactive; offline/local; error. |
| **Offline behavior** | Authorized cached profile/version data readable offline. |
| **Permissions** | Standard read-only; Admin edits Profile data only. |
| **Navigation** | Edit → GEO-SH-01; Back → originating context. |
| **Back behavior** | Back → originating context |
| **Adaptive behavior** | Adaptive parameter sections. |
| **Related business rules** | `BR-MD-004, BR-MD-005, BR-AUTHZ-001` |
| **Acceptance criteria** | Family/formula controls are absent; versioned profile information remains explicit. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| GEO02-01 | Profile Name | Read-only value | String | — | No | — | — | — | Always | GeometryProfile.name | Admin / Standard | Cached |
| GEO02-02 | Geometry Family | Read-only value | Enum | — | No | — | F01–F10 | Platform-governed family | Always | GeometryProfileVersion.family | Admin / Standard | Cached |
| GEO02-03 | Version | Read-only value | Version | — | No | — | — | Immutable historical version identity | Always | GeometryProfileVersion.version | Admin / Standard | Cached |
| GEO02-04 | Geometry Parameters | Read-only group | Decimal collection | — | No | — | Family-specific | Valid per Calculation Engine v2; display two decimals | Always | GeometryProfileVersion.parameters | Admin / Standard | Cached |
| GEO02-05 | Max Capacity | Read-only calculated value | Decimal | — | No | — | m³ | Geometry-engine derived | Always | Geometry result | Admin / Standard | Cached |
| GEO02-06 | Status | Status chip | Enum | — | No | — | Active / Inactive | Canonical lifecycle | Always | GeometryProfile.lifecycleStatus | Admin / Standard | Cached |
| GEO02-07 | Edit Profile | Action | Action | — | Yes | — | — | Creates/uses version-safe update; no historical rewrite | Admin only | GEO-SH-01 | Admin | Local-first metadata; sync |


## COM-01 — Commodities

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Browse the 24 platform-governed Commodity definitions. |
| **Access** | Admin / Standard |
| **Entry points** | More/master-data entry; commodity reference navigation. |
| **Layout / sections** | Header; commodity list grouped/filterable by approved category if useful; Active/Inactive indication. |
| **Displayed / edited data** | 24 approved Commodity definitions and lifecycle. |
| **User actions** | Open COM-02. |
| **Validation** | No generic Other; no customer create/edit controls. |
| **UI states** | Loading; list; offline/local; error. |
| **Offline behavior** | Approved commodity master may be cached for offline campaign use. |
| **Permissions** | Read-only for both organization roles. |
| **Navigation** | Commodity → COM-02. |
| **Back behavior** | Back to originating context. |
| **Adaptive behavior** | Efficient list; adaptive width. |
| **Related business rules** | `BR-COM-001, BR-AUTHZ-001` |
| **Acceptance criteria** | Exactly approved commodities are presented; no mutation affordance. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| COM01-01 | Search | Search field | String | No | Yes | Empty | Commodity name | Case-insensitive | Always | Commodity catalog | Admin / Standard | Cached |
| COM01-02 | Lifecycle Filter | Filter | Enum | No | Yes | Active | Active / Inactive / All | Platform lifecycle | Always | Commodity query | Admin / Standard | Local |
| COM01-03 | Commodity list | Lazy list | Collection | — | No | — | 24 governed commodities | No customer Add/Edit | Always | Commodity read model | Admin / Standard | Cached |


## COM-02 — Commodity Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Display a platform-governed Commodity and reference information. |
| **Access** | Admin / Standard |
| **Entry points** | COM-01 selection; stocktaking reference where appropriate. |
| **Layout / sections** | Back header; identity/category; lifecycle/version; Reference Test Weight information where defined; explanatory density context. |
| **Displayed / edited data** | Commodity definition/version/reference data. |
| **User actions** | Read-only navigation only. |
| **Validation** | Reference Test Weight must not be presented as automatic Actual Bulk Density fallback. |
| **UI states** | Loading; content; offline/local; error. |
| **Offline behavior** | Readable from pinned/cached campaign master data where available. |
| **Permissions** | Read-only for both roles. |
| **Navigation** | Back → COM-01 or originating stocktaking context. |
| **Back behavior** | Back → COM-01 or originating stocktaking context |
| **Adaptive behavior** | Adaptive detail layout. |
| **Related business rules** | `BR-COM-002` |
| **Acceptance criteria** | No edit action; density semantics are unambiguous. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| COM02-01 | Commodity Name | Read-only value | String | — | No | — | 24 governed values | Platform-governed | Always | CommodityVersion.name | Admin / Standard | Cached |
| COM02-02 | Version | Read-only value | Version | — | No | — | — | Historical version preserved | Always | CommodityVersion.version | Admin / Standard | Cached |
| COM02-03 | Lifecycle Status | Status chip | Enum | — | No | — | Active / Inactive | Platform governed | Always | Commodity.lifecycleStatus | Admin / Standard | Cached |
| COM02-04 | Density Guidance | Read-only information | Text | — | No | — | Actual Bulk Density required operationally | Reference Test Weight never fallback | Always | Commodity specification | Admin / Standard | Cached |


## CLI-01 — Clients

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Browse Organization-scoped Clients used by Third-Party stocktaking. |
| **Access** | Admin / Standard |
| **Entry points** | More/master-data entry. |
| **Layout / sections** | Header; client list; lifecycle; Admin add/edit affordance. |
| **Displayed / edited data** | Authorized Organization Clients. |
| **User actions** | Open CLI-02; Admin Add/Edit → CLI-SH-01. |
| **Validation** | Organization scope and lifecycle/history protection. |
| **UI states** | Loading; list; empty; offline/local; error. |
| **Offline behavior** | Authorized cached clients readable offline and included where required in campaign working set. |
| **Permissions** | Standard read-only; Admin mutation. |
| **Navigation** | Client → CLI-02; Add/Edit → CLI-SH-01. |
| **Back behavior** | Back to prior master-data context. |
| **Adaptive behavior** | Adaptive list. |
| **Related business rules** | `BR-AUTHZ-001, BR-OWN-001` |
| **Acceptance criteria** | Clients never cross Organization boundary. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CLI01-01 | Search | Search field | String | No | Yes | Empty | Name / Code | Case-insensitive | Always | Client read model | Admin / Standard | Local |
| CLI01-02 | Lifecycle Filter | Filter | Enum | No | Yes | Active | Active / Inactive / All | Canonical lifecycle | Always | Client query | Admin / Standard | Local |
| CLI01-03 | Client list | Lazy list | Collection | — | No | — | Organization clients | Organization scoped | Always | Client read model | Admin / Standard | Cached |
| CLI01-04 | Add Client | Action | Action | — | Yes | — | — | — | Admin only | CLI-SH-01 | Admin | Local |


## CLI-02 — Client Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Display one Client and its approved master-data fields. |
| **Access** | Admin / Standard |
| **Entry points** | CLI-01 selection; Third-Party reference navigation. |
| **Layout / sections** | Back header; client details; lifecycle; Admin edit action. |
| **Displayed / edited data** | Client identity/details/lifecycle. |
| **User actions** | Admin edit → CLI-SH-01. |
| **Validation** | Organization scope; lifecycle/history protection. |
| **UI states** | Loading; content; offline/local; error. |
| **Offline behavior** | Authorized cached Client readable offline. |
| **Permissions** | Standard read-only; Admin mutation. |
| **Navigation** | Edit → CLI-SH-01; Back → origin. |
| **Back behavior** | Back → origin |
| **Adaptive behavior** | Adaptive details. |
| **Related business rules** | `BR-AUTHZ-001, BR-OWN-001` |
| **Acceptance criteria** | Stable Client identity is used rather than free-text ownership name. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CLI02-01 | Client Name | Read-only value | String | — | No | — | — | — | Always | Client.name | Admin / Standard | Cached |
| CLI02-02 | Client Code | Read-only value | String | — | No | — | — | — | Always | Client.code | Admin / Standard | Cached |
| CLI02-03 | Registration / Tax ID | Read-only value | String | — | No | — | — | — | When present | Client.registrationTaxId | Admin / Standard | Cached |
| CLI02-04 | Notes | Read-only value | String | — | No | — | — | — | When present | Client.notes | Admin / Standard | Cached |
| CLI02-05 | Status | Status chip | Enum | — | No | — | Active / Inactive | Canonical lifecycle | Always | Client.lifecycleStatus | Admin / Standard | Cached |
| CLI02-06 | Edit Client | Action | Action | — | Yes | — | — | Historical references protected | Admin only | CLI-SH-01 | Admin | Local |


## CAM-01 — Inventory Campaigns

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | List and find Inventory Campaigns. |
| **Access** | Admin / Standard within authorized scope |
| **Entry points** | Bottom-nav Inventory Campaigns; Dashboard cards; other campaign links. |
| **Layout / sections** | Header; search by Name/Code; status filter Active/Completed; optional Inventory Type filter; campaign list; Admin Create action; bottom nav. |
| **Displayed / edited data** | Authorized Campaigns with Code, Name, Type, dates, status and useful progress summary. |
| **User actions** | Open CAM-02; Admin Create → CAM-03; search/filter. |
| **Validation** | Only Active/Completed statuses; filters use canonical values. |
| **UI states** | Loading; list; empty; filtered-empty; offline/local; error. |
| **Offline behavior** | Previously synchronized authorized campaigns may remain visible offline; sync state must be distinguishable where relevant. |
| **Permissions** | Admin/Standard view authorized scope; Create is Admin-only. |
| **Navigation** | Campaign → CAM-02; Create → CAM-03. |
| **Back behavior** | Back follows shell; bottom nav persistent. |
| **Adaptive behavior** | Efficient lazy list; tablet wider rows/panes. |
| **Related business rules** | `BR-CAM-001, BR-CAM-009, BR-AUTHZ-002` |
| **Acceptance criteria** | Search covers Name/Code; no Draft/Upcoming/In Progress status UI. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CAM01-01 | Search | Search field | String | No | Yes | Empty | Campaign Name / Campaign Code | Case-insensitive | Always | Campaign read model | Admin / Standard | Cached |
| CAM01-02 | Status Filter | Filter | Enum | No | Yes | Active | Active / Completed / All | Only canonical statuses | Always | Campaign query | Admin / Standard | Local |
| CAM01-03 | Inventory Type Filter | Filter | Enum | No | Yes | All | Annual / Monthly / Control / All | Canonical types | Always | Campaign query | Admin / Standard | Local |
| CAM01-04 | Campaign list | Lazy list | Collection | — | No | — | Authorized campaigns | Organization scoped | Always | Campaign read model | Admin / Standard | Cached |
| CAM01-05 | Create & Launch Campaign | Action | Action | — | Yes | — | — | Admin only | Admin only | CAM-03 | Admin | Requires online launch |


## CAM-02 — Campaign Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Display Active or Completed Campaign context and route to operational, close, offline and historical functions. |
| **Access** | Admin / Standard within authorized campaign scope |
| **Entry points** | CAM-01 selection; contextual navigation/deep link after auth. |
| **Layout / sections** | Back header; Campaign Code/Name/Type/dates/status; scope/team summaries; progress; offline/sync summary; contextual actions. |
| **Displayed / edited data** | Campaign identity/configuration; frozen scope; team; snapshot summary; progress; status; sync/offline readiness; close/final snapshot availability. |
| **User actions** | Active: open Stocktaking, review/close, prepare/refresh offline working set as authorized. Completed: open Final Snapshot/reports. Read scope/team. |
| **Validation** | Scope is read-only after launch; status controls follow lifecycle; Close guards are not bypassed. |
| **UI states** | Loading; Active; Completed; offline/local; sync-warning; error. |
| **Offline behavior** | Active campaign may be used offline only after Offline Ready; Completed history can use locally available snapshot data where present. |
| **Permissions** | View authorized scope; Stocktaking mutation still needs Team membership; Close Admin-only. |
| **Navigation** | → STK-01; → CLS-01; Completed → CLS-02; report links → RPT-01/RPT-02 as applicable. |
| **Back behavior** | Back → CAM-01 preserving list/filter context. |
| **Adaptive behavior** | Adaptive summary sections; tablet two-column permitted. |
| **Related business rules** | `BR-CAM-001, BR-CAM-005, BR-AUTHZ-003, BR-AUTHZ-004, BR-OFF-001` |
| **Acceptance criteria** | No edit of frozen scope; Active and Completed behavior is distinct. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CAM02-01 | Campaign Code | Read-only value | String | — | No | — | INV-YYYY-NNNN | Server-generated | Always | Campaign.code | Admin / Standard | Cached |
| CAM02-02 | Campaign Name | Read-only value | String | — | No | — | — | — | Always | Campaign.name | Admin / Standard | Cached |
| CAM02-03 | Inventory Type | Read-only value | Enum | — | No | — | Annual / Monthly / Control | Canonical | Always | Campaign.inventoryType | Admin / Standard | Cached |
| CAM02-04 | Start Date | Read-only date | LocalDate | — | No | — | — | Calendar date; no timezone conversion | Always | Campaign.startDate | Admin / Standard | Cached |
| CAM02-05 | Planned End Date | Read-only date | LocalDate | — | No | — | — | >= Start Date | Always | Campaign.plannedEndDate | Admin / Standard | Cached |
| CAM02-06 | Status | Status chip | Enum | — | No | — | Active / Completed | Canonical | Always | Campaign.status | Admin / Standard | Cached |
| CAM02-07 | Scope | Read-only hierarchy summary | Collection | — | No | — | Frozen after launch | Stable IDs | Always | CampaignScope/Snapshot | Admin / Standard | Offline package |
| CAM02-08 | Campaign Team | Read-only member list | Collection | — | No | — | Min 1 | Membership preserved | Always | CampaignTeamAssignment | Admin / Standard | Offline package |
| CAM02-09 | Open Stocktaking | Action | Action | — | Yes | — | — | Authorized access | Active campaign | STK-01 | Admin / Standard | Offline if package ready |
| CAM02-10 | Review / Final Snapshot | Action | Action | — | Yes | — | — | State/permission dependent | Active→CLS-01; Completed→CLS-02 | Navigation | Per role | Close online; snapshot read cached if available |


## CAM-03 — Create & Launch Campaign

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Create and immediately launch a Campaign using the approved V6 configuration. |
| **Access** | Admin only |
| **Entry points** | CAM-01 Create action. |
| **Layout / sections** | Back header; Campaign Name; read-only/generated Campaign Code presentation; Inventory Type; Start Date; Planned End Date; contextual Scope Selector; contextual Team Assignment; validation summary; Save & Launch. |
| **Displayed / edited data** | Name; server-generated Code; Type Annual/Monthly/Control; dates; exact scope; team ≥1; launch snapshot inputs. |
| **User actions** | Configure fields; select scope/team; Save & Launch; Cancel. |
| **Validation** | Required fields; Planned End ≥ Start; team ≥1; valid scope; server generates `INV-YYYY-NNNN` atomically unique per Organization/year; no Draft. |
| **UI states** | Initial; editing; validation errors; launching; server error; success. |
| **Offline behavior** | Final Save & Launch requires server authority; launch success does not imply Offline Ready. |
| **Permissions** | Admin-only. Launching Admin need not be Campaign Team member. |
| **Navigation** | Success → CAM-02 for created Active Campaign; Cancel/Back → CAM-01. |
| **Back behavior** | Back/cancel leaves no launched Campaign; normal unsaved-edit confirmation may be used. |
| **Adaptive behavior** | Adaptive form; Scope/Team contextual components may expand on tablets. |
| **Related business rules** | `BR-CAM-002, BR-CAM-003, BR-CAM-004, BR-CAM-005, BR-CAM-006, BR-CAM-007, BR-CAM-008, BR-AUTHZ-002` |
| **Acceptance criteria** | Successful launch creates Active Campaign, freezes scope and preserves Campaign Snapshot; failed launch creates no partial final campaign. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CAM03-01 | Campaign Code | Read-only value | String | Yes | No | Server assigned | INV-YYYY-NNNN | Server-generated atomically; unique Organization + calendar year | Always | Server campaign creation | Admin | Online launch |
| CAM03-02 | Campaign Name | Text field | String | Yes | Yes | — | — | Trim; non-empty | Always | Campaign.name | Admin | Draft UI state only until Save & Launch |
| CAM03-03 | Inventory Type | Selector | Enum | Yes | Yes | — | Annual / Monthly / Control | Canonical enum | Always | Campaign.inventoryType | Admin | Local selection |
| CAM03-04 | Start Date | Date picker | LocalDate | Yes | Yes | — | Calendar date | Required | Always | Campaign.startDate | Admin | Local selection |
| CAM03-05 | Planned End Date | Date picker | LocalDate | Yes | Yes | — | Calendar date | Required; >= Start Date | Always | Campaign.plannedEndDate | Admin | Local selection |
| CAM03-06 | Campaign Scope | Hierarchy multi-selector | Stable ID set | Yes | Yes | — | Eligible active master data | Non-empty; valid hierarchy; eligible geometry/master data | Before launch | CampaignScope | Admin | Selection local; launch server validation |
| CAM03-07 | Campaign Team | Member multi-selector | User ID set | Yes | Yes | — | Active organization members | Minimum 1; launching Admin need not be member | Before launch | OrganizationMembership | Admin | Cached members; server revalidate |
| CAM03-08 | Save & Launch | Primary action | Action | — | Yes | — | — | All fields valid; online; server atomic code + snapshot + launch | Always | Campaign launch use case | Admin | Online required |
| CAM03-09 | Cancel | Secondary action | Action | — | Yes | — | — | No campaign persisted as Draft | Always | Navigation | Admin | Local |


## STK-01 — Stocktaking Overview

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Provide Campaign-level Stocktaking progress and navigation by Location. |
| **Access** | Admin / Standard within authorized campaign scope |
| **Entry points** | Bottom-nav Stocktaking with campaign context; CAM-02. |
| **Layout / sections** | Header with campaign context; tabs To Do / Issues / Validated / All; progress; Location list; bottom nav. |
| **Displayed / edited data** | Scoped Locations and derived Stocktaking classification/progress. |
| **User actions** | Switch tabs; open STK-02; select/confirm campaign context where needed. |
| **Validation** | `All` is UI-only; classifications are canonical; mutation not performed directly here. |
| **UI states** | Loading; content; empty per tab; offline-ready/local; not-offline-ready warning; error. |
| **Offline behavior** | Reads campaign working set when Offline Ready; offline capability must be explicit. |
| **Permissions** | View authorized scope; later mutations require Campaign Team. |
| **Navigation** | Location → STK-02. |
| **Back behavior** | Back → CAM-02 or shell context; bottom nav persistent. |
| **Adaptive behavior** | Lazy/adaptive list; tabs remain usable in portrait/landscape. |
| **Related business rules** | `BR-STK-001, BR-OFF-001` |
| **Acceptance criteria** | Tabs are exactly To Do / Issues / Validated / All; All is not persisted. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STK01-01 | Campaign Context | Read-only selector/context | UUID | — | No | Current campaign | Authorized Active campaigns | Must be authorized | Always | Campaign context | Admin / Standard | Offline package |
| STK01-02 | To Do | Tab | Classification | — | Yes | — | TO_DO | Canonical classification | Always | Stocktaking read model | Admin / Standard | Local |
| STK01-03 | Issues | Tab | Classification | — | Yes | — | ISSUES | Derived from Issue condition; not campaign status | Always | Stocktaking/Issue read model | Admin / Standard | Local |
| STK01-04 | Validated | Tab | Classification | — | Yes | — | VALIDATED | Canonical classification | Always | Stocktaking read model | Admin / Standard | Local |
| STK01-05 | All | Tab | UI filter | — | Yes | — | ALL | Filter only | Always | Stocktaking query | Admin / Standard | Local |
| STK01-06 | Location work list / progress | Lazy list | Collection | — | No | — | Scoped locations | Campaign scope fixed | Always | Stocktaking read model | Admin / Standard | Offline package |
| STK01-07 | Resume / Start | Action | Action | — | Yes | — | — | Target in authorized scope | Active campaign | STK-02/03/05 | Campaign Team for mutations | Offline-ready |


## STK-02 — Location Stocktaking

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Show Stocktaking progress for Storage Units in one scoped Location. |
| **Access** | Admin / Standard within authorized campaign scope |
| **Entry points** | STK-01 Location selection. |
| **Layout / sections** | Back header with Campaign + Location; classification filter/context; Storage Unit list; bottom nav. |
| **Displayed / edited data** | Scoped Storage Units and derived progress/counts. |
| **User actions** | Open STK-03. |
| **Validation** | Only Campaign Scope hierarchy is shown. |
| **UI states** | Loading; list; empty; offline/local; error. |
| **Offline behavior** | Uses Offline Working Set when available. |
| **Permissions** | View authorized scope. |
| **Navigation** | Storage Unit → STK-03. |
| **Back behavior** | Back → STK-01 preserving campaign/tab context. |
| **Adaptive behavior** | Adaptive list/pane. |
| **Related business rules** | `BR-STK-001, BR-OFF-001` |
| **Acceptance criteria** | No out-of-scope Storage Unit is exposed. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STK02-01 | Campaign | Context header | String | — | No | — | — | — | Always | Campaign snapshot | Admin / Standard | Offline package |
| STK02-02 | Location | Context header | String | — | No | — | — | — | Always | Campaign scope snapshot | Admin / Standard | Offline package |
| STK02-03 | Storage Unit list | Lazy list | Collection | — | No | — | Scoped units | Campaign scope only | Always | Stocktaking read model | Admin / Standard | Offline package |
| STK02-04 | Progress / classification counts | Read-only summary | Counts | — | No | — | To Do / Issues / Validated | Derived; no extra state | Always | Stocktaking read model | Admin / Standard | Local |
| STK02-05 | Open Storage Unit | List action | Action | — | Yes | — | — | Stable ID navigation | Per item | STK-03 | Admin / Standard | Offline |


## STK-03 — Storage Unit Stocktaking

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Efficiently present all scoped Storage Spaces for one Storage Unit and route into the workspace. |
| **Access** | Admin / Standard within authorized campaign scope |
| **Entry points** | STK-02 selection; return from STK-05. |
| **Layout / sections** | Back header; Campaign/Location/Unit context; tabs/filter as relevant; efficient lazy Storage Space list; status/issue indicators; bottom nav. |
| **Displayed / edited data** | Scoped Storage Spaces with operational classification, progress and sync indicators. |
| **User actions** | Open STK-05; switch classification view. |
| **Validation** | Large-unit performance; only scoped spaces; All is UI-only. |
| **UI states** | Loading; list; empty; offline/local; error. |
| **Offline behavior** | Efficiently reads local campaign working set offline. |
| **Permissions** | View authorized scope; workspace mutation eligibility evaluated at STK-05. |
| **Navigation** | Space → STK-05. |
| **Back behavior** | Back → STK-02; return from STK-05 restores relevant list/scroll context. |
| **Adaptive behavior** | Lazy list is mandatory for large sets; adaptive row density; tablet pane permitted. |
| **Related business rules** | `BR-STK-001, BR-STK-002` |
| **Acceptance criteria** | Return from STK-05 restores Unit context and relevant list/scroll position. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STK03-01 | Campaign / Location / Storage Unit | Context header | Context IDs | — | No | — | — | Preserve exact context | Always | Navigation/read model | Admin / Standard | Offline package |
| STK03-02 | Classification Filter | Tabs/filter | Enum | No | Yes | All | To Do / Issues / Validated / All | Canonical | Always | Stocktaking query | Admin / Standard | Local |
| STK03-03 | Storage Space list | Lazy list | Collection | — | No | — | Scoped spaces | Large-list efficient; no mandatory network pagination | Always | Stocktaking read model | Admin / Standard | Offline package |
| STK03-04 | Space Code | List item | String | — | No | — | — | Structural order/code preferred | Per item | StorageSpace snapshot | Admin / Standard | Offline |
| STK03-05 | Operational Classification | Status label | Enum | — | No | — | To Do / Issues / Validated | Never color-only | Per item | Derived read model | Admin / Standard | Offline |
| STK03-06 | Open Space | List action | Action | — | Yes | — | — | Stable IDs | Per item | STK-05 | Admin / Standard | Offline |


## STK-05 — Storage Space Stocktaking

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Single complete workspace for measurement, calculation, ownership/product context, evidence, Issue reporting and validation. |
| **Access** | Admin / Standard; mutation conditional on Campaign Team membership |
| **Entry points** | STK-03 Storage Space selection; authorized contextual deep link. |
| **Layout / sections** | Back header with Campaign/Location/Unit/Space; read-only geometry section; stock/product section; 1–5 measurement editor; calculated Effective Ullage/Fill/Volume/Physical Quantity; Ownership/Client; Notes/Photos; sync state; actions Validate, Mark Empty, Report Issue. |
| **Displayed / edited data** | Pinned Geometry Profile Version and datum; measurements; Commodity; Actual Bulk Density; optional Moisture/Impurities; Ownership/Client; calculation result/metadata; evidence; Issues; validation/sync state. |
| **User actions** | Add/edit/remove measurement up to 5; set product data; calculate; add evidence; Report Issue; Validate; Mark Empty. |
| **Validation** | 1–5 compatible measurements for non-empty; same datum/profile/version/type; density >0; ownership mandatory non-empty; Client mandatory Third-Party; geometry read-only; invalid calculation blocks; Mark Empty exceptions apply. |
| **UI states** | Loading; editable To Do; Issues context; Validated read-only/final context; calculation validation error; offline saved/pending; sync error/conflict; general error. |
| **Offline behavior** | Supported mutations can save offline only with complete authorized working set; local business change + SyncOperation atomic; UI Saved only after commit; Close unavailable here. |
| **Permissions** | Read allowed in authorized scope; all Stocktaking mutations require Campaign Team membership for both roles. |
| **Navigation** | Report Issue → P0-SH-01; back → STK-03 with restored context; commodity/client/geometry references may open canonical details without changing business state. |
| **Back behavior** | Back preserves Unit/list context; unsaved UI edits handled before leaving. |
| **Adaptive behavior** | Phone uses stacked sections; tablet may use split details/work area; no separate Measurement/Calculation/Validation screens. |
| **Related business rules** | `BR-AUTHZ-003, BR-STK-003, BR-STK-004, BR-STK-005, BR-STK-006, BR-STK-007, BR-STK-008, BR-STK-009, BR-CALC-001, BR-OWN-001, BR-ISS-001, BR-EVD-001` |
| **Acceptance criteria** | Geometry is read-only; max 5 measurements; calculation deterministic; Validate/Mark Empty do not auto-resolve Open Issues; stale data cannot overwrite Validated. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| STK05-01 | Campaign / Location / Storage Unit / Storage Space | Context header | Stable IDs + labels | — | No | — | — | Must remain visible/identifiable | Always | Campaign snapshot/navigation | Admin / Standard | Offline package |
| STK05-02 | Geometry Family/Profile/Version | Read-only group | Snapshot refs | — | No | — | — | Read-only during stocktaking | Always | Campaign snapshot | Admin / Standard | Offline package |
| STK05-03 | Geometry Parameters | Read-only group | Decimals | — | No | — | Family-specific | No field correction here | Always | Campaign snapshot | Admin / Standard | Offline package |
| STK05-04 | Measurement 1–5 | Numeric inputs | Decimal list | Non-empty: 1–5 | Yes until Validated | — | Ullage compatible unit | 1..5; same datum/type/profile; valid range; no auto outlier discard | Hidden/not required when Mark Empty | Measurement | Campaign Team | Local-first transactional |
| STK05-05 | Effective Ullage | Read-only calculated value | Decimal | — | No | — | MEAN_ULLAGE_V1 | No intermediate rounding | Non-empty after valid measurements | CalculationResult | Campaign Team/readers | Local calculation |
| STK05-06 | Effective Fill Height | Read-only calculated value | Decimal | — | No | — | m | h = H_ref - u_eff; 0..maximumFillHeight | Non-empty | CalculationResult | Admin / Standard | Local calculation |
| STK05-07 | Commodity | Selector | UUID/version | Non-empty: Yes | Yes until Validated | — | Active governed commodities | Required non-empty; version captured | Hidden/not required when Empty | CommodityVersion | Campaign Team | Offline catalog |
| STK05-08 | Actual Bulk Density | Numeric input | Decimal | Non-empty: Yes | Yes until Validated | — | kg/m³ or kg/hl UI input | >0; canonical conversion; reference test weight never fallback | Hidden/not required when Empty | StocktakingRevision.actualBulkDensityKgM3 | Campaign Team | Local-first |
| STK05-09 | Ownership | Selector | Enum | Non-empty: Yes | Yes until Validated | — | Owned / Third-Party | Required non-empty | Hidden/not required when Empty | StocktakingRevision.ownership | Campaign Team | Local-first |
| STK05-10 | Client | Selector | UUID | Third-Party: Yes | Yes until Validated | — | Active organization clients | Required iff Third-Party | Only when Ownership=Third-Party and non-empty | StocktakingRevision.clientId → Client | Campaign Team | Offline catalog |
| STK05-11 | Product Volume | Read-only result | Decimal | — | No | — | m³ | Calculation Engine v2; no clamp | After valid calculation or 0 if Empty | CalculationResult | Admin / Standard | Local calculation |
| STK05-12 | Physical Quantity | Read-only result | Decimal | — | No | — | kg; UI may deterministically show t | V × actual bulk density; 0 if Empty | After valid calculation/Empty | CalculationResult | Admin / Standard | Local calculation |
| STK05-13 | Notes | Multiline input | String | No | Yes until Validated | — | Free text | Optional; revision-specific; preserved; not substitute for Issue | Always | StocktakingRevision.notes | Campaign Team | Local-first |
| STK05-14 | Photos | Photo capture/attachment | Evidence | No | Yes until Validated | 0 | Max 5 per StocktakingRevision | JPEG normalization; <=3MB hard max; target <=1.5MB; SHA-256 | Always | Evidence | Campaign Team | App-private local + sync |
| STK05-15 | Report Issue | Action | Action | — | Yes | — | — | Contextual issue capture | Active campaign | P0-SH-01 | Campaign Team | Offline create allowed |
| STK05-16 | Mark Empty | Action | Action | — | Yes until final | — | — | Explicit; isEmpty=true; volume=0; quantity=0; actor/time audited | Active, not already final | Stocktaking use case | Campaign Team | Offline validation allowed |
| STK05-17 | Validate | Primary action | Action | — | Yes until final | — | — | All non-empty mandatory inputs + successful calc; does not resolve Open Issue | Active, not final | Validation use case | Campaign Team | Offline validation allowed |
| STK05-18 | Reset Validation | Protected action | Action | — | Yes | — | — | Admin only; before Close; explicit audited new working revision; prior validation retained in audit | Validated + Campaign Active | Validation correction use case | Admin | Must respect sync/version; no Completed reset |


## CLS-01 — Campaign Review & Close

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Review Campaign completion and Close Guards; perform atomic Close when authorized. |
| **Access** | Admin / Standard review; Close Admin-only |
| **Entry points** | CAM-02 Active Campaign. |
| **Layout / sections** | Back header; campaign summary; scope completion; To Do/Issues/Validated counts; sync readiness; Close Guard checklist; unresolved Issue handling links; Close action for eligible Admin. |
| **Displayed / edited data** | Active Campaign state, scoped stocktaking completion, Issues, mandatory data validity, critical sync state, authorization/connectivity. |
| **User actions** | Inspect review; navigate to unresolved work; Admin resolve/accept follow-up through approved Issue flow; Admin Close. |
| **Validation** | Close only when all guards pass; server revalidates; Open Issues require explicit resolution; no offline Close. |
| **UI states** | Loading; review-ready; guards-failing; closing; close error; close success. |
| **Offline behavior** | Review may use local data, but final Close requires connectivity and server authority. |
| **Permissions** | Review accessible to authorized users; Close Admin-only. |
| **Navigation** | Unfinished item → relevant STK screens; successful Close → CLS-02. |
| **Back behavior** | Back → CAM-02 while Campaign remains Active if not successfully closed. |
| **Adaptive behavior** | Checklist/list adaptive; close action remains clearly separated. |
| **Related business rules** | `BR-AUTHZ-004, BR-CLS-001, BR-CLS-002, BR-CLS-003, BR-CLS-004, BR-CLS-005, BR-ISS-005` |
| **Acceptance criteria** | Any failed Close leaves Campaign Active and creates no partial Final Snapshot. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CLS01-01 | Campaign Summary | Read-only group | Snapshot/read model | — | No | — | — | Active only for Close | Always | Campaign read model | Authorized users | Cached |
| CLS01-02 | Scope Completion | Guard indicator | Counts | — | No | — | All scoped spaces Validated | Server revalidated | Always | Close guard read model | Authorized users | Cached but Close online |
| CLS01-03 | Open Issues | Guard/list | Collection | — | No | — | Open / Resolved | No Open Issue at Close unless explicitly resolved-for-follow-up | Always | Issue read model | Authorized users | Cached |
| CLS01-04 | Resolve / Accept for Follow-up | Protected action | Action | — | Yes | — | Resolved with reason | Explicit reason + actor/time audit | Open issue | Issue resolution use case | Authorized campaign user; follow-up acceptance Admin at Close | Local resolution may sync; Close waits critical sync |
| CLS01-05 | Critical Sync State | Guard indicator | Sync state | — | No | — | No blocking pending/conflict/error | Required critical operations synchronized | Always | Sync read model | Admin / Standard | Local |
| CLS01-06 | Close Campaign | Primary protected action | Action | — | Yes | — | — | Admin + online + all server Close Guards; atomic | Active campaign | Close API | Admin | Online required |


## CLS-02 — Final Inventory Snapshot

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Present the immutable final result created by successful Inventory Close. |
| **Access** | Admin / Standard within authorized completed campaign scope |
| **Entry points** | Successful CLS-01 Close; CAM-02 Completed Campaign; report/history links. |
| **Layout / sections** | Back header; Campaign/close metadata; immutable summary; per-space snapshot entries; links to reports/dossier/history as authorized. |
| **Displayed / edited data** | Final Inventory Snapshot and entries including frozen campaign, hierarchy, geometry, commodity/ownership/client, measurements, calculation metadata, validation/issues/evidence references and close metadata. |
| **User actions** | Browse snapshot; open reports/dossier/history; no edit. |
| **Validation** | Exactly one snapshot per successfully closed Campaign; immutable; historical values not recalculated. |
| **UI states** | Loading; content; locally available historical content; error. |
| **Offline behavior** | May display durable local snapshot when available; no mutation. |
| **Permissions** | Authorized read; no edit for either role. |
| **Navigation** | → RPT-01/RPT-02/HIS-01/DOS-01 as applicable; Back → CAM-02. |
| **Back behavior** | Back → CAM-02 |
| **Adaptive behavior** | Adaptive summary/table/list. |
| **Related business rules** | `BR-CLS-003, BR-CLS-006, BR-RPT-002` |
| **Acceptance criteria** | Snapshot is immutable and authoritative for completed official reporting. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CLS02-01 | Campaign Identity | Read-only group | Snapshot | — | No | — | — | Immutable | Always | FinalInventorySnapshot | Admin / Standard | Cached if available |
| CLS02-02 | Close Actor / Time | Read-only values | User + Instant | — | No | — | UTC authoritative; local display | Immutable | Always | Final snapshot | Admin / Standard | Cached |
| CLS02-03 | Per-Space Final Entries | Read-only list | Collection | — | No | — | All scoped spaces | Never recalc current master | Always | FinalInventorySnapshotEntry | Admin / Standard | Cached |
| CLS02-04 | Measurements / Commodity / Ownership / Client / Empty | Read-only details | Snapshot values | — | No | — | — | Immutable historical values | Per entry | Final snapshot entry | Admin / Standard | Cached |
| CLS02-05 | Volume / Quantity / Calculation Metadata | Read-only details | Snapshot values | — | No | — | — | Immutable; reproducible | Per entry | Final snapshot entry | Admin / Standard | Cached |
| CLS02-06 | Issue / Evidence References | Read-only links | References | — | No | — | — | Historical refs preserved | When present | Final snapshot entry | Admin / Standard | Cached metadata |


## RPT-01 — Reports

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Catalog the nine approved report types and collect report context/parameters. |
| **Access** | Admin / Standard within authorized scope |
| **Entry points** | More; Campaign details/final snapshot; Dashboard if linked. |
| **Layout / sections** | Header; report catalog; context/parameter controls appropriate to selected report; generation/open action. |
| **Displayed / edited data** | Authorized Organization/Campaign/report context. |
| **User actions** | Choose one of nine reports; provide allowed parameters; open RPT-02 or dedicated HIS/DOS/AUD destination. |
| **Validation** | Exactly nine report types; completed official report source is Final Snapshot; Audit Trail access Admin-only. |
| **UI states** | Loading; catalog; parameter validation; unavailable context; error. |
| **Offline behavior** | May prepare/view locally available datasets; exports requiring remote service follow availability. |
| **Permissions** | Admin/Standard reports within scope; Audit Trail Admin-only. |
| **Navigation** | Physical/Location/Commodity/Ownership/Worksheet/Capacity → RPT-02; History → HIS-01; Dossier → DOS-01; Audit → AUD-01. |
| **Back behavior** | Back → origin. |
| **Adaptive behavior** | Adaptive catalog cards/list. |
| **Related business rules** | `BR-RPT-001, BR-RPT-002, BR-AUTHZ-005` |
| **Acceptance criteria** | No Book Stock/Variance/Reconciliation report is introduced. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| RPT01-01 | Report Catalog | List/grid | Enum | — | No | — | Exactly 9 canonical reports | No extra report types | Always | Report registry | Admin / Standard subject to Audit restriction | Local catalog |
| RPT01-02 | Physical Inventory | Report action | Action | — | Yes | — | — | Completed official uses Final Snapshot | Always | RPT-02 | Admin / Standard | Online/cached dataset as available |
| RPT01-03 | Inventory by Location | Report action | Action | — | Yes | — | — | Same authoritative dataset | Always | RPT-02 | Admin / Standard | As available |
| RPT01-04 | Inventory by Commodity | Report action | Action | — | Yes | — | — | Same authoritative dataset | Always | RPT-02 | Admin / Standard | As available |
| RPT01-05 | Inventory by Ownership | Report action | Action | — | Yes | — | — | Same authoritative dataset | Always | RPT-02 | Admin / Standard | As available |
| RPT01-06 | Storage Space Worksheet | Report action | Action | — | Yes | — | — | Same authoritative dataset | Always | RPT-02 | Admin / Standard | As available |
| RPT01-07 | Capacity Report | Report action | Action | — | Yes | — | — | Same authoritative dataset | Always | RPT-02 | Admin / Standard | As available |
| RPT01-08 | Inventory History | Navigation action | Action | — | Yes | — | — | 2..N Completed Campaigns | Always | HIS-01 | Admin / Standard | Requires available snapshots |
| RPT01-09 | Digital Inventory Dossier | Navigation action | Action | — | Yes | — | — | Canonical dossier | Always | DOS-01 | Admin / Standard | As available |
| RPT01-10 | Audit Trail | Navigation action | Action | — | Yes | — | — | Admin-only | Admin only | AUD-01 | Admin | As available |


## RPT-02 — Report Viewer

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Render the selected standard report from one authoritative report dataset and expose supported output actions. |
| **Access** | Admin / Standard within authorized report scope |
| **Entry points** | RPT-01 standard report selection; contextual report links. |
| **Layout / sections** | Back header; report title/context; filters/metadata; tabular/printable content; supported PDF/CSV/Print actions according to report contract. |
| **Displayed / edited data** | Single authoritative report dataset; Final Snapshot for completed official reports. |
| **User actions** | View; export supported formats; print. |
| **Validation** | Dataset and export use same semantics; units/precision follow V6 rules. |
| **UI states** | Loading; content; empty; export in progress/error; offline/unavailable. |
| **Offline behavior** | Locally available datasets may be viewed; remote export behavior depends on implementation contract without changing report semantics. |
| **Permissions** | Authorized report access. |
| **Navigation** | Back → RPT-01 or originating context. |
| **Back behavior** | Back → RPT-01 or originating context |
| **Adaptive behavior** | Responsive table/list; horizontal handling for wide report data. |
| **Related business rules** | `BR-RPT-002, BR-RPT-003, BR-RPT-004` |
| **Acceptance criteria** | UI/PDF/CSV derive from the same authoritative dataset. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| RPT02-01 | Report Type | Read-only title | Enum | — | No | — | One of reusable report types | Canonical | Always | Navigation/report request | Admin / Standard | Local |
| RPT02-02 | Campaign | Selector | UUID | Yes for campaign reports | Yes | — | Authorized campaign(s) | Completed official reports use Final Snapshot | As report requires | Report query | Admin / Standard | Cached list |
| RPT02-03 | Report Filters | Dynamic filter group | Filter values | No | Yes | Report defaults | Only filters defined by report contract | No invented dimensions | As applicable | Report query | Admin / Standard | Local |
| RPT02-04 | Report Dataset | Table/list | Read model | — | No | — | — | Single Authoritative Report Dataset | Always | Report service/read model | Admin / Standard | Cached if supported |
| RPT02-05 | Export PDF | Action | Action | — | Yes | — | PDF | Same dataset as UI | When report exportable | Report export | Admin / Standard | May require online/data availability |
| RPT02-06 | Export CSV | Action | Action | — | Yes | — | CSV | Same dataset as UI | When report exportable | Report export | Admin / Standard | May require online/data availability |


## HIS-01 — Inventory History

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Compare 2..N Completed Campaign Final Snapshots. |
| **Access** | Admin / Standard within authorized scope |
| **Entry points** | RPT-01; CLS-02. |
| **Layout / sections** | Back header; Organization/context; multi-select Completed Campaigns; comparison dimensions; results with comparability state and Physical Inventory Change. |
| **Displayed / edited data** | Selected Final Snapshots; Close Date, Campaign, Commodity, Location, Ownership, Physical Quantity; scope/context comparability. |
| **User actions** | Select 2..N campaigns; run comparison; inspect/export where supported. |
| **Validation** | Only Completed Campaigns; at least 2; absolute and percentage change; comparability states canonical; no forecast. |
| **UI states** | Initial; selection; loading; results; insufficient selection; no comparable data; error. |
| **Offline behavior** | Can use available immutable snapshots; no source mutation. |
| **Permissions** | Authorized report/history scope. |
| **Navigation** | Back → RPT-01/CLS-02. |
| **Back behavior** | Back → RPT-01/CLS-02 |
| **Adaptive behavior** | Adaptive comparison table/list. |
| **Related business rules** | `BR-HIS-001, BR-HIS-002, BR-HIS-003, BR-HIS-004, BR-HIS-005` |
| **Acceptance criteria** | Uses Physical Inventory Change terminology and shows scope/context comparability. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HIS01-01 | Completed Campaigns | Multi-selector | UUID set | Yes | Yes | — | 2..N Completed | Minimum 2; authorized | Always | Final snapshots | Admin / Standard | Available snapshots |
| HIS01-02 | Close Date | Comparison dimension | Date | — | No | — | — | Snapshot value | Always | History read model | Admin / Standard | Cached |
| HIS01-03 | Campaign | Comparison dimension | String | — | No | — | — | — | Always | History read model | Admin / Standard | Cached |
| HIS01-04 | Commodity | Filter/dimension | UUID/version | No | Yes | All | Snapshot commodities | Snapshot-based | Always | History query | Admin / Standard | Local |
| HIS01-05 | Location | Filter/dimension | UUID | No | Yes | All | Snapshot locations | Snapshot-based | Always | History query | Admin / Standard | Local |
| HIS01-06 | Ownership | Filter/dimension | Enum | No | Yes | All | Owned / Third-Party | Canonical | Always | History query | Admin / Standard | Local |
| HIS01-07 | Physical Quantity | Read-only metric | Decimal | — | No | — | kg/t presentation | Snapshot quantity | Always | History read model | Admin / Standard | Cached |
| HIS01-08 | Absolute Change | Read-only metric | Decimal | — | No | — | — | Deterministic comparison | When comparable pair | History calculation | Admin / Standard | Local |
| HIS01-09 | Percentage Change | Read-only metric | Decimal | — | No | — | — | Defined only when denominator permits | When meaningful | History calculation | Admin / Standard | Local |
| HIS01-10 | Comparability | Status label | Enum | — | No | — | COMPARABLE / SCOPE_ADDED / SCOPE_REMOVED / CONTEXT_CHANGED | No forecasting | Always | History read model | Admin / Standard | Local |


## DOS-01 — Digital Inventory Dossier

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Present the completed Campaign's official dossier assembled from immutable historical data and approved evidence references. |
| **Access** | Admin / Standard within authorized completed campaign scope |
| **Entry points** | RPT-01; CLS-02. |
| **Layout / sections** | Back header; dossier sections; campaign/final snapshot context; evidence/issue/audit references allowed by report contract; PDF/Print actions. |
| **Displayed / edited data** | Final Snapshot-backed dossier dataset and authorized evidence/reference metadata. |
| **User actions** | View; PDF; Print. |
| **Validation** | Completed campaign authoritative data only; immutable source. |
| **UI states** | Loading; content; missing locally cached evidence notice; export error. |
| **Offline behavior** | View locally available dossier data; binary evidence availability may depend on local/remote presence. |
| **Permissions** | Authorized non-audit report access. |
| **Navigation** | Back → RPT-01/CLS-02. |
| **Back behavior** | Back → RPT-01/CLS-02 |
| **Adaptive behavior** | Adaptive document-like sections. |
| **Related business rules** | `BR-RPT-001, BR-RPT-002, BR-EVD-008` |
| **Acceptance criteria** | Dossier does not mutate historical data. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DOS01-01 | Campaign | Selector | UUID | Yes | Yes | — | Authorized Completed campaigns | Dossier based on authoritative historical data | Always | Final snapshot | Admin / Standard | Cached list |
| DOS01-02 | Dossier Contents | Read-only structured view | Dataset | — | No | — | Campaign/snapshot/audit/evidence refs per dossier contract | No current-master recalculation | After campaign selection | Dossier service | Admin / Standard | Metadata cached as available |
| DOS01-03 | Export Dossier PDF | Action | Action | — | Yes | — | PDF | Same authoritative dossier dataset | When data available | Dossier export | Admin / Standard | May require online/evidence availability |
| DOS01-04 | Evidence / Attachments | Read-only links | References | — | No | — | Authorized retained evidence | Integrity/hash rules apply | When present | Evidence store metadata | Admin / Standard | Local/remote availability indicated |


## AUD-01 — Audit Trail

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Inspect authorized audit events for traceability. |
| **Access** | Admin only |
| **Entry points** | RPT-01; More/administration where linked. |
| **Layout / sections** | Back header; audit filters; chronological/tabular events; export/print actions supported by reporting contract. |
| **Displayed / edited data** | Authorized Audit Events with actor/time/action/context and safe details. |
| **User actions** | Filter/search within approved fields; view; export PDF/tabular format; print. |
| **Validation** | Audit events are append-only/immutable; Notifications are not Audit Trail events. |
| **UI states** | Loading; content; empty; export error; error. |
| **Offline behavior** | May display locally available audit data if present; authoritative access remains protected. |
| **Permissions** | Admin-only. |
| **Navigation** | Back → RPT-01/origin. |
| **Back behavior** | Back → RPT-01/origin |
| **Adaptive behavior** | Responsive table/list. |
| **Related business rules** | `BR-AUTHZ-005, BR-NOT-002, BR-RET-001, BR-RPT-001` |
| **Acceptance criteria** | Standard cannot access; audit data is not editable. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| AUD01-01 | Search / Filter | Filter group | Query | No | Yes | — | Actor/action/entity/date/context | Must not mutate audit | Always | Audit query | Admin | Online/cached according to policy |
| AUD01-02 | Event ID | Read-only value | UUID | — | No | — | — | Immutable | Per event | AuditEvent.eventId | Admin | Cached |
| AUD01-03 | Actor | Read-only value | User ID/name | — | No | — | — | Historical identity preserved | Per event | AuditEvent.actorUserId | Admin | Cached |
| AUD01-04 | Action / Entity | Read-only value | String | — | No | — | — | Machine-stable action semantics | Per event | AuditEvent | Admin | Cached |
| AUD01-05 | Timestamp | Read-only value | Instant | — | No | — | UTC authoritative/local display | Immutable | Per event | AuditEvent.timestampUtc | Admin | Cached |
| AUD01-06 | Campaign / Correlation / Source | Read-only metadata | IDs | — | No | — | — | When applicable | Per event | AuditEvent metadata | Admin | Cached |
| AUD01-07 | Before / After Change Metadata | Expandable read-only | Structured data | — | No | — | — | Only where relevant; sensitive values excluded | When recorded | AuditEvent change metadata | Admin | Cached |


## USR-01 — Users

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | List Organization users/memberships and administer lifecycle/access. |
| **Access** | Admin only |
| **Entry points** | More/administration. |
| **Layout / sections** | Header; user list; Active/Disabled state; role/membership summary; user selection. |
| **Displayed / edited data** | Organization users/memberships and lifecycle. |
| **User actions** | Open USR-02; initiate approved user administration through detail flow. |
| **Validation** | Exactly Admin/Standard roles; lifecycle Active/Disabled. |
| **UI states** | Loading; list; empty; error. |
| **Offline behavior** | Administrative mutations require server validation; do not invent offline authority. |
| **Permissions** | Admin-only. |
| **Navigation** | User → USR-02. |
| **Back behavior** | Back → MOR-01/administration origin. |
| **Adaptive behavior** | Adaptive list. |
| **Related business rules** | `BR-GOV-002, BR-GOV-003` |
| **Acceptance criteria** | No third role is presented. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| USR01-01 | Search | Search field | String | No | Yes | Empty | Full Name / Email | Case-insensitive | Always | User/membership read model | Admin | Local |
| USR01-02 | Status Filter | Filter | Enum | No | Yes | Active | Active / Disabled / All | Canonical lifecycle | Always | User query | Admin | Local |
| USR01-03 | Role Filter | Filter | Enum | No | Yes | All | Admin / Standard / All | Exactly two roles | Always | Membership query | Admin | Local |
| USR01-04 | User list | Lazy list | Collection | — | No | — | Organization members | Organization scoped | Always | Membership read model | Admin | Cached |
| USR01-05 | Open User | List action | Action | — | Yes | — | — | Stable user ID | Per item | USR-02 | Admin | Local |


## USR-02 — User Details

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Inspect and administer one user's Organization membership, role and lifecycle using approved controls. |
| **Access** | Admin only |
| **Entry points** | USR-01 selection. |
| **Layout / sections** | Back header; identity; role; Active/Disabled; assignment eligibility/context; approved administration actions. |
| **Displayed / edited data** | User identity, membership, role, lifecycle, relevant assignment references. |
| **User actions** | Change permitted role/lifecycle; disable/reactivate; manage approved membership attributes. |
| **Validation** | Roles only Admin/Standard; Disabled blocks new auth/ops/assignments; history preserved; reactivation same identity. |
| **UI states** | Loading; content; saving; validation/server error. |
| **Offline behavior** | Protected user administration requires server authority. |
| **Permissions** | Admin-only. |
| **Navigation** | Back → USR-01. |
| **Back behavior** | Back → USR-01 |
| **Adaptive behavior** | Adaptive details/form. |
| **Related business rules** | `BR-GOV-002, BR-GOV-003, BR-GOV-004` |
| **Acceptance criteria** | Disable/reactivate preserves historical identity. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| USR02-01 | Full Name | Text field | String | Yes | Yes | — | — | Trim; non-empty | Always | User.fullName | Admin | Server-backed/local cache |
| USR02-02 | Email | Email field | Email | Yes | Yes subject to identity policy | — | Valid email | Unique; valid syntax | Always | User.email | Admin | Server validation |
| USR02-03 | Role | Selector | Enum | Yes | Yes | — | Admin / Standard | Exactly two roles | Always | OrganizationMembership.role | Admin | Server authorization |
| USR02-04 | Organization Membership | Selector/list | Organization IDs | Yes | Yes | Current org | Authorized memberships | At least current authorized membership as applicable | Always | OrganizationMembership | Admin | Server authoritative |
| USR02-05 | Account Status | Selector | Enum | Yes | Yes | Active | Active / Disabled | Disabled cannot start new auth/ops; history retained | Always | User.lifecycleStatus | Admin | Server authoritative |
| USR02-06 | Save | Primary action | Action | — | Yes | — | — | All required fields + authorization | Always | User admin use case | Admin | Online/server-authorized |


## MOR-01 — More

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Navigation hub for functions not present in bottom navigation. |
| **Access** | Admin / Standard |
| **Entry points** | Bottom-nav More. |
| **Layout / sections** | Header; grouped navigation entries filtered by authorization; bottom nav. |
| **Displayed / edited data** | Available destinations for current user such as Reports, Geometry Profiles, Commodities, Clients, Users (Admin), Notifications/Sync/Settings/Support as applicable. |
| **User actions** | Navigate only; no business mutation. |
| **Validation** | Entries respect authorization and canonical destination inventory. |
| **UI states** | Content; empty section if none; offline availability indication where relevant. |
| **Offline behavior** | Navigation remains available; destination capability handles offline constraints. |
| **Permissions** | Permission-filtered entries; hiding does not replace destination/server authorization. |
| **Navigation** | Routes to canonical screens only. |
| **Back behavior** | Back follows shell; bottom nav persistent. |
| **Adaptive behavior** | Adaptive grouped list/grid. |
| **Related business rules** | `BR-GOV-004` |
| **Acceptance criteria** | No business logic or duplicate workflow is implemented in More. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| MOR01-01 | Reports | Navigation item | Action | — | Yes | — | — | Authorized | Always | RPT-01 | Admin / Standard | Local navigation |
| MOR01-02 | Users | Navigation item | Action | — | Yes | — | — | Admin only | Admin only | USR-01 | Admin | Local |
| MOR01-03 | Geometry Profiles | Navigation item | Action | — | Yes | — | — | Authorized | Always | GEO-01 | Admin / Standard | Local |
| MOR01-04 | Commodities | Navigation item | Action | — | Yes | — | — | Read-only catalog | Always | COM-01 | Admin / Standard | Local |
| MOR01-05 | Clients | Navigation item | Action | — | Yes | — | — | Authorized | Always | CLI-01 | Admin / Standard | Local |
| MOR01-06 | Users / Audit Trail | Navigation items | Action | — | Yes | — | — | Admin only where applicable | Admin only | USR-01/AUD-01 | Admin | Local |
| MOR01-07 | Notifications / Sync / Settings / Support | Navigation items | Action | — | Yes | — | — | Role-filtered | Always | NOT-01/SYN-01/SET-01/SUP-01 | Admin / Standard | Local |
| MOR01-08 | Sign Out | Action | Action | — | Yes | — | — | Clear/revoke session credentials; never discard unsynced business data | Always | Auth use case | Admin / Standard | Can execute locally; revocation sync as supported |


## NOT-01 — Notification Center

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Show in-app notifications and navigate to authorized referenced context. |
| **Access** | Admin / Standard |
| **Entry points** | Header notification action; More if linked. |
| **Layout / sections** | Back header; unread/read list; timestamp/title/message; Mark Read; Mark All Read. |
| **Displayed / edited data** | Authorized notifications with optional context reference. |
| **User actions** | Open context; mark one read; mark all read. |
| **Validation** | Context navigation must pass authentication/authorization; notification is not an Audit Event. |
| **UI states** | Loading; list; empty; offline/local; error. |
| **Offline behavior** | Locally available notifications may be read/marked locally with synchronization as applicable. |
| **Permissions** | Only user's authorized notifications/context. |
| **Navigation** | Context ref → canonical destination after authorization. |
| **Back behavior** | Back → origin. |
| **Adaptive behavior** | Adaptive list. |
| **Related business rules** | `BR-NOT-001` |
| **Acceptance criteria** | Unread/read behavior works and no Notification Details screen is required. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| NOT01-01 | Notification list | Lazy list | Collection | — | No | — | Retained notifications | 12-month retention | Always | Notification read model | Admin / Standard | Cached |
| NOT01-02 | Type | Status/icon/text | Enum | — | No | — | SYNC_ERROR / SYNC_CONFLICT / CAMPAIGN_ASSIGNMENT / ISSUE_REQUIRES_ATTENTION / CAMPAIGN_COMPLETED | Canonical taxonomy | Per item | Notification.type | Admin / Standard | Cached |
| NOT01-03 | Read State | Read/unread indicator | Boolean | — | Yes | Unread | Unread / Read | Notification is not source of truth | Per item | Notification.readAt/state | Admin / Standard | Local-first + sync as applicable |
| NOT01-04 | Title / Message / Timestamp | Read-only values | Text + Instant | — | No | — | — | No sensitive secrets; UTC authoritative/local display | Per item | Notification | Admin / Standard | Cached |
| NOT01-05 | Open Context | Action | Deep link | — | Yes | — | Optional target | Re-authorize target; stable IDs | When target exists | Navigation | Admin / Standard | Target may require local/online data |


## SYN-01 — Sync Status

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Provide operational visibility into synchronization, pending work, errors and conflicts without exposing unsafe strategy controls. |
| **Access** | Admin / Standard |
| **Entry points** | Header sync action; More/Settings. |
| **Layout / sections** | Back header; connectivity/readiness summary; pending/in-flight/ack/error/conflict counts/items; retry/status information permitted by sync contract. |
| **Displayed / edited data** | SyncOperation/Evidence Sync/Offline readiness metadata safe for user display. |
| **User actions** | Inspect status; trigger approved retry/refresh actions if defined by sync contract; navigate to affected context. |
| **Validation** | No server URL, custom sync interval, disable sync, force overwrite, or user-selectable conflict strategy. |
| **UI states** | Loading; healthy; pending; offline; retryable error; conflict; permanent error. |
| **Offline behavior** | Designed to operate during offline periods using durable local sync metadata. |
| **Permissions** | Both roles may inspect within authorized scope; no bypass of protected mutations. |
| **Navigation** | Affected item → canonical business context where possible. |
| **Back behavior** | Back → origin. |
| **Adaptive behavior** | Adaptive status list. |
| **Related business rules** | `BR-SYNC-002, BR-SYNC-004, BR-SET-003` |
| **Acceptance criteria** | Conflict is visible and never represented as silent success. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SYN01-01 | Overall Sync State | Status | Enum | — | No | — | Synced / Pending / Offline / Error | Never color-only | Always | SyncStatus read model | Admin / Standard | Local |
| SYN01-02 | Last Successful Sync | Read-only timestamp | Instant | — | No | — | — | UTC authoritative/local display | When available | SyncStatus.lastSuccessfulSync | Admin / Standard | Local |
| SYN01-03 | Pending / Failed / Conflict Counts | Read-only counters | Int | — | No | 0 | >=0 | Derived from durable rows | Always | SyncStatus | Admin / Standard | Local |
| SYN01-04 | Campaign Package Status | Read-only list | Collection | — | No | — | Package state/version/last verified | No raw internals unless troubleshooting | When packages exist | OfflineWorkingSetManifest | Admin / Standard | Local |
| SYN01-05 | Retry | Action | Action | — | Yes | — | — | Retries eligible operations only; no auto conflict resolution | When retryable work exists | Sync coordinator | Admin / Standard | Requires connectivity to progress |


## SET-01 — Settings

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Manage representation-level User Preferences and approved Organization Configuration while exposing system-controlled rules as non-editable where relevant. |
| **Access** | Admin / Standard; Organization Configuration section Admin-only |
| **Entry points** | More. |
| **Layout / sections** | Back header; User Preferences; Admin-only Organization Configuration; links to Sync Status and Support; Sign Out. |
| **Displayed / edited data** | Display quantity unit; density representation; date/time format; theme; noncritical notification preferences; optional default report format; approved org config. |
| **User actions** | Change user preferences; Admin change approved org config; open SYN-01/SUP-01; Sign Out. |
| **Validation** | Preferences change representation only; no language selector; System-Controlled Rules not editable; Sign Out does not destroy unsynced business data. |
| **UI states** | Loading; content; saving; error. |
| **Offline behavior** | Local preferences can persist offline; organization config synchronization follows protected mutation rules. |
| **Permissions** | Both roles user preferences; Admin-only organization configuration. |
| **Navigation** | → SYN-01; → SUP-01; Sign Out → AUT-01 after secure session termination. |
| **Back behavior** | Back → MOR-01/origin. |
| **Adaptive behavior** | Adaptive settings sections. |
| **Related business rules** | `BR-SET-001, BR-SET-002, BR-SET-003` |
| **Acceptance criteria** | No setting retroactively changes inventory or snapshot data. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SET01-01 | Theme | Selector | Enum | No | Yes | System | System / Light / Dark | User preference only | Always | UserPreference | Admin / Standard | Local-first |
| SET01-02 | Notification Preferences | Controlled settings group | Booleans | No | Yes | System defaults | Only notification types explicitly configurable | Must not disable required integrity alerts unless contract permits | Only when supported | UserPreference | Admin / Standard | Local-first + sync if applicable |
| SET01-03 | Organization Configuration | Settings section | Key/value governed config | No | Yes only for approved keys | — | Only approved organization-level keys | No invented calculation/sync/security rules | Admin only; valid empty/not-configured state | OrganizationConfiguration | Admin | Server-authorized; cached |
| SET01-04 | System-Controlled Rules | Read-only info | Policy values | — | No | — | — | Not editable: calculation, max measurements, rounding, sync interval, server URL, conflict strategy, retention, geometry/commodity governance | If surfaced | System config | Admin / Standard | Cached |
| SET01-05 | Language | Not present | — | — | No | English UI | — | No language selector in V6 | Never | — | — | — |


## SUP-01 — Support

| Field | Specification |
|---|---|
| **Type** | Full screen |
| **Purpose** | Provide support/about information and safe diagnostics entry points. |
| **Access** | Admin / Standard |
| **Entry points** | SET-01; More. |
| **Layout / sections** | Back header; support information; app/version/build/schema/calculation version information where safe; diagnostic package action; About content. |
| **Displayed / edited data** | Safe technical metadata only by default. |
| **User actions** | View support/about; create approved diagnostic package. |
| **Validation** | No secrets, photos, full business payloads, or unnecessary personal data by default. |
| **UI states** | Content; diagnostic generation; success/error. |
| **Offline behavior** | Can show local version/support data offline. |
| **Permissions** | Both roles; diagnostic contents remain privacy-safe. |
| **Navigation** | Back → SET-01/MOR-01. |
| **Back behavior** | Back → SET-01/MOR-01 |
| **Adaptive behavior** | Adaptive informational sections. |
| **Related business rules** | `BR-SET-006, BR-OPS-008` |
| **Acceptance criteria** | About is consolidated here; diagnostic package is safe by default. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SUP01-01 | App Version | Read-only value | String | — | No | — | — | — | Always | Build metadata | Admin / Standard | Local |
| SUP01-02 | Environment | Read-only value | Enum | — | No | — | Development / Staging / Production | Build-controlled; user cannot switch | When appropriate | Build config | Admin / Standard | Local |
| SUP01-03 | Support Context | Read-only/context helper | IDs/labels | — | No | — | Campaign/Location/Unit/Space/action/error where available | Exclude secrets/tokens | When relevant | Safe diagnostics | Admin / Standard | Local |
| SUP01-04 | Diagnostics | Read-only/support action | Safe metadata | — | No | — | Safe IDs/versions/counts/sync timestamps | No raw auth tokens/secrets | When troubleshooting | Diagnostics service | Admin / Standard | Local/cached |


# 6. Sheet Specifications

## AUT-SH-01 — Forgot Password

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Initiate password recovery from Sign In. |
| **Access** | Public recovery context |
| **Entry points** | AUT-01 Forgot Password. |
| **Layout / sections** | Recovery identifier input; Submit; Cancel; feedback. |
| **Displayed / edited data** | Recovery identifier and server response. |
| **User actions** | Submit recovery; cancel. |
| **Validation** | Valid input; server accepts recovery request. |
| **UI states** | Initial; submitting; validation/server feedback. |
| **Offline behavior** | Requires server access. |
| **Permissions** | No role. |
| **Navigation** | Success continues approved recovery flow toward AUT-02; Cancel returns AUT-01. |
| **Back behavior** | Dismiss → AUT-01. |
| **Adaptive behavior** | Bottom sheet/dialog adaptive to device; larger widths constrained. |
| **Related business rules** | `BR-AUTH-001` |
| **Acceptance criteria** | No account enumeration-sensitive feedback is introduced by UI wording. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| AUTSH01-01 | Email | Email field | Email | Yes | Yes | — | Valid email | Trim; valid syntax | Always | Password recovery request | Public | Online required |
| AUTSH01-02 | Send Reset Instructions | Primary action | Action | — | Yes | — | — | Generic response; must not disclose account existence | Always | Auth recovery API | Public | Online required |
| AUTSH01-03 | Cancel / Back | Action | Action | — | Yes | — | — | — | Always | Navigation | Public | Local |


## P1-SH-01 — Add/Edit Location

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Create or edit customer-managed Location. |
| **Access** | Admin only |
| **Entry points** | LOC-01 Add/Edit. |
| **Layout / sections** | Approved Location fields; optional facility Map Position editor; lifecycle; Save/Cancel. |
| **Displayed / edited data** | Location master data including optional authoritative facility coordinate pair. |
| **User actions** | Save; cancel. |
| **Validation** | Required fields; Organization scope; history/lifecycle constraints; coordinates are either both absent or both present; latitude -90..90 and longitude -180..180. |
| **UI states** | Create/edit; saving; validation/error. |
| **Offline behavior** | Durable local-first mutation/sync rules apply to coordinates. Basemap availability is not required to enter explicit coordinates. |
| **Permissions** | Admin-only. |
| **Navigation** | Success/dismiss → LOC-01. |
| **Back behavior** | Dismiss returns without committing uncommitted edits. |
| **Adaptive behavior** | Adaptive sheet; full-height on compact devices if needed. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-001` |
| **Acceptance criteria** | Standard has no mutation action. Coordinate changes are audited/versioned. The approved capability does not request device location permission and does not perform GPS tracking, address geocoding, or route guidance. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P1SH01-01 | Location Name | Text field | String(150) | Yes | Yes | — | — | Trim; non-empty; max 150 | Always | Location.name | Admin | Local-first |
| P1SH01-02 | Location Code | Text field | String(50) | Yes | Create/edit subject to history rule | — | Uppercase normalized | Unique in Organization; trim+uppercase; stable if historically referenced | Always | Location.code | Admin | Local-first + server uniqueness |
| P1SH01-03 | Address | Text field | String(250) | No | Yes | — | — | Max 250 | Always | Location.address | Admin | Local-first |
| P1SH01-04 | City | Text field | String(100) | No | Yes | — | — | Max 100 | Always | Location.city | Admin | Local-first |
| P1SH01-05 | Country | Selector (no free text) | ISO 3166-1 alpha-2 code | No | Yes | — | Complete ISO 3166-1 catalogue | Uppercase canonical code; catalogue membership; fail-closed new-value validation (FD-74) | Always | Location.country; human-readable labels from local/cached catalogue | Admin | Complete options available locally/cached |
| P1SH01-06 | Notes | Multiline field | String(500) | No | Yes | — | — | Max 500 | Always | Location.notes | Admin | Local-first |
| P1SH01-07 | Status | Selector | Enum | Yes | Yes | Active | Active / Inactive | Inactivation historical/campaign guards | Always | Location.lifecycleStatus | Admin | Local-first |
| P1SH01-08 | Map Position | Map pin + coordinate editor | CoordinatePair? | No | Yes | None | Latitude/Longitude pair or None | Pair invariant; latitude -90..90; longitude -180..180 | Always | Location.latitude/location.longitude | Admin | Local-first; basemap optional |
| P1SH01-09 | Clear Map Position | Secondary action | Action | — | Yes | — | — | Clears both coordinates atomically | Only when coordinates exist | Location mutation | Admin | Local-first |
| P1SH01-10 | Save | Primary action | Action | — | Yes | — | — | All required rules including coordinate pair/range | Always | Master-data use case | Admin | Local-first + sync |


## P1-SH-02 — Add/Edit Storage Unit

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Create or edit a Storage Unit under a Location. |
| **Access** | Admin only |
| **Entry points** | LOC-02/STU-02 Add/Edit. |
| **Layout / sections** | Parent Location context; approved unit fields; lifecycle; Save/Cancel. |
| **Displayed / edited data** | Storage Unit master data. |
| **User actions** | Save; cancel. |
| **Validation** | Valid parent Location; required fields; historical protection. |
| **UI states** | Create/edit; saving; validation/error. |
| **Offline behavior** | Durable mutation/sync rules apply. |
| **Permissions** | Admin-only. |
| **Navigation** | Success returns to LOC-02/STU-02 context. |
| **Back behavior** | Dismiss preserves parent context. |
| **Adaptive behavior** | Adaptive sheet. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-001` |
| **Acceptance criteria** | Storage Unit remains under selected Location. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P1SH02-01 | Storage Unit Name | Text field | String(150) | Yes | Yes | — | — | Trim; non-empty; max 150 | Always | StorageUnit.name | Admin | Local-first |
| P1SH02-02 | Storage Unit Code | Text field | String(50) | Yes | Create/edit subject to history rule | — | Uppercase normalized | Unique in Location; trim+uppercase; stable if historically referenced | Always | StorageUnit.code | Admin | Local-first + server uniqueness |
| P1SH02-03 | Description | Multiline field | String(500) | No | Yes | — | — | Max 500 | Always | StorageUnit.description | Admin | Local-first |
| P1SH02-04 | Status | Selector | Enum | Yes | Yes | Active | Active / Inactive | Inactivation historical/campaign guards | Always | StorageUnit.lifecycleStatus | Admin | Local-first |
| P1SH02-05 | Location | Read-only context | UUID | Yes | No | Selected parent | Current Location | Parent fixed in normal form | Always | Navigation context | Admin | Cached |
| P1SH02-06 | Save | Primary action | Action | — | Yes | — | — | All required rules | Always | Master-data use case | Admin | Local-first + sync |


## P1-SH-03 — Add/Edit Storage Space

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Create/edit Storage Space; Add mode supports approved multi-apply/range creation C01–C08. |
| **Access** | Admin only |
| **Entry points** | STU-02/STS-02 Add/Edit. |
| **Layout / sections** | Parent Unit; identity/range controls in Add; Geometry Profile selection; approved fields; Save/Cancel. |
| **Displayed / edited data** | Storage Space master data and geometry assignment. |
| **User actions** | Create one/multiple spaces in Add; edit one space in Edit; cancel. |
| **Validation** | Valid parent Unit/profile; range expansion valid; no bulk/range behavior in Edit; history/lifecycle constraints. |
| **UI states** | Add; edit; validation; saving; error. |
| **Offline behavior** | Durable mutation/sync rules apply. |
| **Permissions** | Admin-only. |
| **Navigation** | Success → STU-02/STS-02 context. |
| **Back behavior** | Dismiss preserves hierarchy context. |
| **Adaptive behavior** | Adaptive form; range controls remain usable in portrait/landscape. |
| **Related business rules** | `BR-AUTHZ-001, BR-MD-001, BR-MD-004` |
| **Acceptance criteria** | Bulk/range creation exists only in Add mode. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P1SH03-01 | Storage Space Code / Range | Text/range input | String/range | Yes | Yes | — | Single code or C01–C08-style range | Unique in Storage Unit; normalized code; range expansion deterministic | Always | StorageSpace create use case | Admin | Local-first |
| P1SH03-02 | Description | Text field | String | No | Yes | — | — | Within approved DB length once fixed | Always | StorageSpace.description | Admin | Local-first |
| P1SH03-03 | Geometry Profile | Selector | UUID/version | Yes | Yes | — | Eligible Active profiles | Valid profile/version; required dimensions valid | Always | GeometryProfileVersion | Admin | Cached |
| P1SH03-04 | Max Capacity | Read-only calculated value | Decimal | Yes | No | Derived | m³ | Auto-calculated from geometry; never manual | After profile selected | Geometry engine | Admin | Local calculation |
| P1SH03-05 | Status | Selector | Enum | Yes | Yes | Active | Active / Inactive | Canonical lifecycle; historical/campaign guards | Always | StorageSpace.lifecycleStatus | Admin | Local-first |
| P1SH03-06 | Apply to Range | Action | Action | — | Yes | — | — | Creates C01–C08 etc. with same selected profile/settings; duplicate codes blocked | Range mode | Master-data use case | Admin | Local-first |
| P1SH03-07 | Save | Primary action | Action | — | Yes | — | — | All required rules | Always | Master-data use case | Admin | Local-first + sync |


## GEO-SH-01 — Add/Edit Geometry Profile

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Create/edit an Organization Geometry Profile inside an approved Geometry Family. |
| **Access** | Admin only |
| **Entry points** | GEO-01/GEO-02 Add/Edit. |
| **Layout / sections** | Geometry Family selector/reference; family-specific dimensions; measurementReferenceHeightM; maximumFillHeightM; verified area/provenance where applicable; Save/Cancel. |
| **Displayed / edited data** | Profile/version parameters allowed by selected family. |
| **User actions** | Create/edit Profile/version; cancel. |
| **Validation** | Family-specific schema; F01 d≥D; positive dimensions; F08–F10 verified area/provenance; no formula editing. |
| **UI states** | Create/edit; validation; saving; error. |
| **Offline behavior** | Durable master-data mutation/sync rules apply. |
| **Permissions** | Admin-only; Geometry Family/formula remains platform-controlled. |
| **Navigation** | Success → GEO-01/GEO-02. |
| **Back behavior** | Dismiss returns without committed changes. |
| **Adaptive behavior** | Adaptive family-specific form. |
| **Related business rules** | `BR-MD-004, BR-MD-005, BR-CALC-012, BR-AUTHZ-001` |
| **Acceptance criteria** | Missing/invalid family parameters block save; no customer formula controls. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| GEOSH01-01 | Profile Name | Text field | String | Yes | Yes | — | — | Trim; non-empty | Always | GeometryProfile.name | Admin | Local-first |
| GEOSH01-02 | Geometry Family | Selector | Enum | Yes | Create/new version | — | F01–F10 | Platform-governed family; no customer family creation | Always | GeometryFamily | Admin | Cached |
| GEOSH01-03 | Family Parameters | Dynamic numeric fields | Decimal map | Yes | Yes | — | Per selected family | All required dimensions >0 and family constraints; canonical m | After family selection | GeometryProfileVersion.parameters | Admin | Local validation/calculation |
| GEOSH01-04 | Verified Area | Numeric field | Decimal | F08–F10: Yes | Yes | — | m² | >0 | Only F08/F09/F10 | GeometryProfileVersion.verifiedArea | Admin | Local-first |
| GEOSH01-05 | Verified Area Provenance | Text/reference field | String | F08–F10: Yes | Yes | — | Approved provenance text/ref | Non-empty when verified area required | Only F08/F09/F10 | GeometryProfileVersion.provenance | Admin | Local-first |
| GEOSH01-06 | Max Capacity | Read-only calculated value | Decimal | Yes | No | Derived | m³ | Geometry Engine v2; invalid profile blocks save | After valid params | Calculation engine | Admin | Local calculation |
| GEOSH01-07 | Status | Selector | Enum | Yes | Yes | Active | Active / Inactive | Historical versions retained | Always | GeometryProfile.lifecycleStatus | Admin | Local-first |
| GEOSH01-08 | Save | Primary action | Action | — | Yes | — | — | Creates version-safe profile data; never rewrites historical snapshots | Always | Geometry use case | Admin | Local-first + sync |


## CLI-SH-01 — Add/Edit Client

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Create/edit Organization-scoped Client master data. |
| **Access** | Admin only |
| **Entry points** | CLI-01/CLI-02 Add/Edit. |
| **Layout / sections** | Approved Client fields; lifecycle; Save/Cancel. |
| **Displayed / edited data** | Client master data. |
| **User actions** | Save; cancel. |
| **Validation** | Organization scope; required fields; lifecycle/history rules. |
| **UI states** | Create/edit; saving; validation/error. |
| **Offline behavior** | Durable mutation/sync rules apply. |
| **Permissions** | Admin-only. |
| **Navigation** | Success → CLI-01/CLI-02. |
| **Back behavior** | Dismiss preserves origin. |
| **Adaptive behavior** | Adaptive sheet. |
| **Related business rules** | `BR-AUTHZ-001, BR-OWN-001` |
| **Acceptance criteria** | Client remains Organization-scoped. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CLISH01-01 | Client Name | Text field | String(150) | Yes | Yes | — | — | Trim; non-empty; max 150 | Always | Client.name | Admin | Local-first |
| CLISH01-02 | Client Code | Text field | String(50) | Yes | Create/edit subject to history rule | — | Uppercase normalized | Unique in Organization; trim+uppercase; stable if historically referenced | Always | Client.code | Admin | Local-first + server uniqueness |
| CLISH01-03 | Registration / Tax ID | Text field | String(50) | No | Yes | — | — | Max 50 | Always | Client.registrationTaxId | Admin | Local-first |
| CLISH01-04 | Notes | Multiline field | String(500) | No | Yes | — | — | Max 500 | Always | Client.notes | Admin | Local-first |
| CLISH01-05 | Status | Selector | Enum | Yes | Yes | Active | Active / Inactive | Inactive unavailable for new operational selection; history preserved | Always | Client.lifecycleStatus | Admin | Local-first |
| CLISH01-06 | Save | Primary action | Action | — | Yes | — | — | All required rules | Always | Client use case | Admin | Local-first + sync |


## P0-SH-01 — Report Issue

| Field | Specification |
|---|---|
| **Type** | Sheet |
| **Purpose** | Create an Open Issue from Stocktaking. |
| **Access** | Campaign Team member in authorized Stocktaking context |
| **Entry points** | STK-05 Report Issue. |
| **Layout / sections** | Stocktaking context summary; Issue Type; Description; optional Photo/Note evidence; Submit/Cancel. |
| **Displayed / edited data** | Campaign/Storage Space/Stocktaking context; Issue Type; Description; optional evidence. |
| **User actions** | Submit Issue; add/remove optional evidence before submit; cancel. |
| **Validation** | Issue Type and Description mandatory; evidence optional; evidence limits/storage rules apply. |
| **UI states** | Initial; evidence processing; submitting; validation/storage/sync error; success. |
| **Offline behavior** | May create durably offline when complete authorized working set exists; Issue/evidence local write follows outbox/evidence rules. |
| **Permissions** | Campaign Team membership required for reporting. |
| **Navigation** | Success dismisses to STK-05 with Open Issue reflected. |
| **Back behavior** | Dismiss → STK-05. |
| **Adaptive behavior** | Adaptive sheet; evidence controls usable on phone/tablet. |
| **Related business rules** | `BR-AUTHZ-006, BR-ISS-001, BR-ISS-002, BR-EVD-001` |
| **Acceptance criteria** | Successful report creates Open Issue; failure never shows false success. |

### Fields & Controls — exhaustive V6 contract

| Field ID | UI Label | Control Type | Data Type | Required | Editable | Default | Allowed Values | Validation | Visibility / Condition | Data Source / Binding | Permission | Offline Behavior |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P0SH01-01 | Issue Category | Selector | Enum | Yes | Yes | — | Measurement Problem / Access / Safety / Geometry Discrepancy / Commodity Uncertainty / Storage Space Condition / Other | Controlled canonical category | Always | Issue.category | Campaign Team | Offline local-first |
| P0SH01-02 | Description | Multiline field | String | Yes | Yes | — | — | Trim; non-empty for all categories | Always | Issue.description | Campaign Team | Offline local-first |
| P0SH01-03 | Photos | Photo capture/attachment | Evidence | No | Yes | 0 | Max 5 per Issue | JPEG normalization; EXIF orientation; long edge 2048; target <=1.5MB; hard max 3MB; SHA-256 | Always | Evidence | Campaign Team | App-private local + sync |
| P0SH01-04 | Campaign / Location / Unit / Space | Read-only context | Stable IDs | Yes | No | Current context | — | Captured automatically | Always | Navigation/stocktaking context | Campaign Team | Offline package |
| P0SH01-05 | Reporter / Timestamp | System-captured | User + Instant | Yes | No | Current actor/time | UTC authoritative | Audited | On save | Issue metadata | Campaign Team | Local-first |
| P0SH01-06 | Submit Issue | Primary action | Action | — | Yes | — | — | Required category+description; does not auto-validate | Always | Issue use case | Campaign Team | Offline allowed |



## 7. Frozen Cross-Cutting Field and Implementation Contracts

These rules are normative for every applicable UI component and are carried forward into Data Model, Database Schema, API Specification, Security, Sync, and Test documentation.

### 7.1 Field-contract rule
Every applicable visible or interactive element is specified as a field/control, including inputs, read-only values, status labels, tabs, filters, search, list rows, KPIs, evidence controls, primary/secondary actions, protected actions, and navigation actions. Absence from this contract is not permission to invent a product field.

### 7.1.1 Android platform interaction contract
Production screens are edge-to-edge safe. Required content/actions handle status/navigation bars, display cutouts, gesture insets and IME insets. Back behavior integrates with predictive back where available and must preserve frozen authorization/state restoration. UI state collection is lifecycle-aware.

### 7.1.2 Visual-quality floor
The approved SiloMath Stitch board/reference is the minimum visual-quality floor for information hierarchy, usable density, component refinement, complete UI states, and enterprise polish. Stitch never authorizes new business semantics. Where a readable per-screen Stitch reference is unavailable, Documents 06 and 21 are sufficient authority and implementation proceeds without additional Product approval.

### 7.1.3 Location map contract
LOC-01 List and Map are two presentations of the same authorized filtered Location read model. Coordinates are stored facility master data; no runtime device-location permission or user tracking is part of V6. Map provider/SDK selection is an implementation decision subject to Documents 17/20 and may not become an additional source of business truth.

### 7.2 Business identifiers
Organization Code, Location Code, Storage Unit Code, Storage Space Code, and Client Code are business identifiers. They are trimmed and normalized to uppercase; uniqueness is case-insensitive in the applicable parent scope. UUIDs remain technical stable identifiers. A code that is already historically referenced must not be changed in a way that breaks historical identity. Campaign Code is different: it is server-generated, read-only, and follows `INV-YYYY-NNNN`, unique per Organization and calendar year.

### 7.3 Date, time, and timezone
Authoritative instants are persisted/transmitted in UTC and displayed in the device/user local timezone. Calendar-only dates such as Campaign Start Date and Planned End Date remain date values and are not timezone-shifted. API date/time representation uses ISO-8601.

### 7.4 Numeric units and precision
Canonical calculation units are m, m², m³, kg/m³, and kg. Density may be entered/displayed as kg/hl where operationally appropriate and is normalized using `1 kg/hl = 10 kg/m³`. Physical Quantity is canonical kg; deterministic tonne presentation is permitted. Geometry parameters are displayed with two decimals. Calculation uses no intermediate rounding; persistence uses scale 6 and ROUND_HALF_UP at the approved persistence/presentation boundary. Imperial units are not part of V6.

### 7.5 Master-data lifecycle and deletion
Customer-controlled Master Data uses Active/Inactive. Inactive records cannot be selected for new operational configuration but remain visible wherever historical/snapshot references require them. No destructive cascade or UI hard delete may remove business/master data referenced by operational or historical records.

### 7.6 Validated correction
A Validated Stocktaking Record is read-only in normal operation. Before Campaign Close, an Admin may invoke explicit audited Reset Validation, creating a new working revision while preserving the prior validation in audit/history. Standard users cannot reset validation. Completed Campaign data and Final Snapshots cannot be reset or edited.

### 7.7 Issue categories and lifecycle
Canonical Issue categories are Measurement Problem, Access / Safety, Geometry Discrepancy, Commodity Uncertainty, Storage Space Condition, and Other. Description is mandatory for every category. Issue state remains exactly Open/Resolved. Resolution is explicit and audited; Validate and Mark Empty never auto-resolve an Issue.

### 7.8 Audit minimum metadata
Audited mutations preserve at least event ID, Organization ID, actor user ID, action, entity type, entity ID, UTC timestamp, optional Campaign ID, correlation ID, source, and structured before/after or change metadata where relevant. Audit is append-only and user-immutable.

### 7.9 Session and security behavior
Session lifetime is server-controlled. Secure automatic restoration/refresh may be used; secrets are stored only through approved secure Android mechanisms and never logged or stored in plaintext. A Disabled user cannot start new authentication or new operations. Sign Out clears/revokes session credentials but does not discard unsynchronized business data. No new offline authentication authority is created.

### 7.10 Notifications
Canonical notification types are SYNC_ERROR, SYNC_CONFLICT, CAMPAIGN_ASSIGNMENT, ISSUE_REQUIRES_ATTENTION, and CAMPAIGN_COMPLETED. Notifications have Unread/Read state, timestamp, title/message, and optional contextual deep link. A notification is never the authoritative business record.

### 7.11 Reports and exports
The report inventory remains exactly nine report types. Standard report export formats are PDF and CSV. Digital Inventory Dossier supports PDF dossier output and authorized evidence/attachment references according to the dossier contract. UI and exports use the same authoritative report dataset.

### 7.12 Sync conflict UX
Critical Stocktaking conflicts are never silently merged or force-overwritten. Conflict handling preserves the attempted local value and authoritative server value, requires reload/re-entry where applicable, prohibits local overwrite of Validated/Completed/snapshot state, and records resolution in audit. There is no generic Force Overwrite control.

### 7.13 Standard mutation metadata
Mutable entities use createdAt, createdBy, updatedAt, updatedBy, and entityVersion where applicable. Immutable snapshots preserve creation actor/time and schema/version identifiers and do not expose semantic update controls.

### 7.14 API error presentation contract
UI consumes stable machine-readable API error codes and optional field errors, correlation ID, and retryable classification. Business behavior must not be inferred by parsing human-readable error text. Field errors bind to the relevant control; global errors use a non-destructive error surface.

### 7.15 Large collections and pagination
Android uses lazy rendering for large lists. Server collection APIs may use cursor pagination. Offline Campaign working sets must remain navigable without requiring a network request for a next page. STK-03 does not require mandatory network pagination.

### 7.16 Environment configuration
Development, Staging, and Production endpoints are build/environment configuration. Server URL is not user-editable. Production builds cannot be switched by the user to Development or Staging.

### 7.17 Organization provisioning
An Organization Admin may edit Organizations for which they hold authorized Admin membership. Creation of a new Organization/tenant is available only in an explicitly authorized provisioning/onboarding context. Normal Organization Admin status alone does not authorize arbitrary tenant creation.

# 8. Cross-Screen Navigation and Context Rules

1. Authentication destinations never expose operational bottom navigation.
2. Operational top-level destinations are exactly DSH-01, CAM-01, STK-01, LOC-01, MOR-01.
3. Detail Back restores the immediate canonical parent context.
4. Stocktaking preserves Campaign → Location → Storage Unit → Storage Space identity with stable IDs.
5. STK-05 → STK-03 restores relevant Unit and list/scroll context.
6. Completed Campaign routes never imply Completed → Active.
7. Contextual sheets return to invoking screen and are not navigation roots.
8. Notification/deep-link references pass authentication, Organization scope, role, Campaign access, and entity-state checks.
9. Navigation cannot bypass Campaign Team membership for Stocktaking mutation or Admin authorization for Close/Master Data mutation.
10. MOR-01 is navigation only.

# 9. UI State Rules

Presentation-only states may include Loading, Content, Empty, Validation Error, Recoverable Error, Access Error, Offline/Local Data, Sync Pending/Error/Conflict, and operation progress. They are not additional domain enums.

# 10. Accessibility and Adaptive Requirements

- Android accessibility semantics, labels, focus order, touch targets and content descriptions are required.
- Critical status is not communicated only by color.
- Validation messages associate with the relevant field/action.
- Large text must not clip primary actions or critical values.
- Compact/Medium/Expanded window contexts, portrait/landscape, resize and multi-window preserve business capabilities and permissions.
- Maps expose accessible Location alternatives through the synchronized List; map-only interaction is never required.
- Large datasets use lazy/efficient containers; STK-03 is explicitly efficient for large Storage Units.
- Wide tables adapt without changing report semantics.

# 11. UI Acceptance Invariants

1. Exactly **36 full screens + 7 sheets = 43** major UI components.
2. No convenience full screen/sheet where approved contextual flow exists.
3. Bottom nav exactly **Dashboard / Inventory Campaigns / Stocktaking / Locations / More**.
4. Campaign UI does not introduce Draft, Upcoming, In Progress, or Review as status.
5. Stocktaking does not persist All or Mark Empty as lifecycle status.
6. Geometry is read-only during Stocktaking.
7. Commodity definitions and Geometry Families/formulas have no customer edit controls.
8. Campaign Scope is not editable after Save & Launch.
9. Admin alone does not enable Stocktaking mutation without Campaign Team membership.
10. Close is not shown complete until server atomic Close succeeds.
11. Completed historical data uses immutable Final Inventory Snapshot.
12. Sync/conflict/error never appears as synchronized success.
13. Sign Out does not imply deletion of unsynchronized business data.
14. Settings cannot edit System-Controlled Rules.
15. Navigation uses type-safe Navigation 3 destination keys, passes stable IDs, and preserves required Stocktaking context.
16. Predictive Back and edge-to-edge/inset/IME behavior satisfy the global platform interaction contract.
17. LOC-01 List/Map equivalence is preserved; stored facility coordinates do not authorize device-location tracking.

# 12. Traceability and Governance

Every component traces to applicable business rules/workflows. Later specialist documents implement this UI contract without redefining product behavior.

A requirement for a new major screen, sheet, persistent status, role, business action, or navigation root requires explicit V6 contract review rather than implementation invention.

---

**End of Document — 06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION**

## Approved Country contract — CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74

The canonical Country catalogue is the complete ISO 3166-1 list. The canonical persisted and API value is an uppercase ISO 3166-1 alpha-2 code: Romania=`RO`, Germany=`DE`, United States=`US`. The human-readable country name is a presentation label only. Country selectors do not permit free text for new values; new values require catalogue membership and fail-closed validation. Complete selector options are available locally/cached for local-first/offline-first operation. Existing optional/null semantics remain unchanged.

Already persisted non-conforming Country values are preserved unchanged and require a separate audit before any migration. No implicit normalization, country-name-to-code conversion, truncation or historical/snapshot rewriting is authorized. This clarification does not assign a catalogue version identifier or authorize any data/schema migration.

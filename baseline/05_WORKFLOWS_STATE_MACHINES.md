# SiloMath V6 — Workflows & State Machines

**Document ID:** 05_WORKFLOWS_STATE_MACHINES  
**Version:** V6 Final Documentation — Workflows & State Machines 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-28 - CR-API-STORAGE-SPACE-BATCH-001 approved atomic Storage Space batch-create clarification
**Product:** SiloMath  
**Normative role:** Canonical lifecycle, workflow, transition, guard, failure, and terminal-state contract for V6.

---

## 1. Purpose

This document defines the canonical operational workflows and state machines of SiloMath V6. It specifies legal transitions, triggering actions, guards, side effects, failure behavior, terminal states, and cross-domain workflow invariants.

It specializes `01_PRODUCT_BASELINE`, `02_DECISION_REGISTER`, `03_BUSINESS_RULES`, and `04_USER_ROLES_PERMISSIONS`. UI document 06 must implement these workflows without inventing additional persistent states.

## 2. State-Machine Principles

1. Persistent business states and canonical operational classifications are limited to the values defined by the V6 contract.
2. UI filters, workflow steps, loading indicators, and review processes are not automatically persistent business states.
3. A transition occurs only after its required durable operation succeeds.
4. Failed transitions must not leave partial final business state.
5. Offline-capable transitions persist locally with their SyncOperation atomically before the UI reports success.
6. Server-owned integrity boundaries, especially Campaign Code generation and Inventory Close, are authoritative on the server.
7. Historical terminal state cannot be weakened by stale offline data.

## 3. Canonical State Enumerations

| Domain | Canonical states / values | Notes |
|---|---|---|
| User lifecycle | `ACTIVE`, `DISABLED` | Same user identity is preserved across disable/reactivate. |
| Organization role | `ADMIN`, `STANDARD` | Role is not a lifecycle state. |
| Master Data lifecycle | `ACTIVE`, `INACTIVE` | Historical references preserved. |
| Campaign | `ACTIVE`, `COMPLETED` | Completed is terminal. Review is not a status. |
| Inventory Type | `ANNUAL`, `MONTHLY`, `CONTROL` | Campaign classification, not lifecycle. |
| Ownership | `OWNED`, `THIRD_PARTY` | Third-Party requires Client for non-empty stocktaking. |
| Stocktaking operational classification | `TO_DO`, `ISSUES`, `VALIDATED` | `ALL` is UI-only aggregate filter. Issue lifecycle remains independent from validation. |
| Issue | `OPEN`, `RESOLVED` | Resolution is explicit and audited. |
| Sync Operation | `PENDING`, `IN_FLIGHT`, `ACKNOWLEDGED`, `RETRYABLE_ERROR`, `CONFLICT`, `PERMANENT_ERROR` | Durable synchronization lifecycle. |
| Evidence Sync | `LOCAL_PENDING`, `UPLOADING`, `SYNCED`, `SYNC_ERROR` | Binary transfer lifecycle. |
| History comparability | `COMPARABLE`, `SCOPE_ADDED`, `SCOPE_REMOVED`, `CONTEXT_CHANGED` | Derived comparison classification. |
| Environment | `DEVELOPMENT`, `STAGING`, `PRODUCTION` | Operational environment enum. |

## 4. End-to-End Product Workflow

**Sign In → Organization → Campaign → Location → Storage Unit → Storage Space → Measurement → Calculation → Validate / Report Issue → Review → Close → Immutable Final Inventory Snapshot → Reports**

Key branching behavior:

- Campaign creation ends in Active through Save & Launch; there is no Draft.
- Save & Launch and Offline Ready are separate milestones.
- Stocktaking may proceed through normal measurement/calculation/validation or through Mark Empty.
- Report Issue is contextual and may occur during Stocktaking.
- Review does not change Campaign status.
- Close is online, Admin-only, server-revalidated and atomic.
- Completed Campaigns are historical and report from the Final Inventory Snapshot.

## 5. Workflow Registry

| ID | Workflow | Start | Trigger | Successful result | Guards | Side effects | Failure behavior | Rule trace |
|---|---|---|---|---|---|---|---|---|
| WF-AUTH-01 | Authentication & session | No session | AUT-01 Sign In | Authenticated session | Active user; valid credentials/session | Authenticate; establish secure session | Disabled user rejected; auth error shown | BR-GOV-003, BR-AUTH-001, BR-GOV-004 |
| WF-AUTH-02 | Session restoration | Stored secure session | App start | Authenticated session | Session valid; user still Active/authorized | Restore securely without Remember Me control | Invalid/expired/Disabled → Sign In | BR-AUTH-001, BR-AUTH-002, BR-GOV-003 |
| WF-AUTH-03 | Password recovery | Sign In | Forgot Password | Recovery initiated | Valid recovery input/context | Issue recovery flow | Validation/server error; remain in recovery flow | BR-AUTH-001 |
| WF-AUTH-04 | Password reset | Recovery initiated | Reset Password | Password reset complete | Valid reset context/token and valid new password | Commit reset; return to authentication flow | Invalid/expired context or validation failure | BR-AUTH-001 |
| WF-ORG-01 | Organization selection | Authenticated | Select authorized Organization | Operational shell scoped to Organization | Valid membership/authorization | Set active Organization context | Unauthorized Organization rejected | BR-GOV-001, BR-GOV-004 |
| WF-MD-01 | Set Location map position | Existing/new Location edit | Admin confirms map pin or explicit coordinate pair, or clears position | Location saved with coordinate pair or no coordinates | Admin; valid Organization; pair/range validation | Persist Location mutation/version/audit; synchronize using normal master-data path | Invalid/partial/out-of-range coordinates rejected; no device-location permission or tracking | BR-AUTHZ-001, BR-MD-010, BR-MD-011, BR-MD-012 |
| WF-CAM-01 | Create & launch Campaign | No new Campaign | Save & Launch | Active Campaign | Admin; valid required fields; team ≥1; valid scope; server available | Server validates, generates Campaign Code, snapshots campaign context, persists scope/team, launches Active | No Draft or partial launch; validation/error returned | BR-AUTHZ-002, BR-CAM-002, BR-CAM-003, BR-CAM-005, BR-CAM-006, BR-CAM-007, BR-CAM-008 |
| WF-OFF-01 | Prepare offline working set | Active Campaign, not Offline Ready | Download / refresh offline package | Offline Ready | Authorized campaign; complete package retrievable; integrity passes | Persist complete campaign-scoped working set + manifest | Remain not Offline Ready; expose error/retry | BR-OFF-001, BR-OFF-002, BR-OFF-003 |
| WF-STK-01 | Open Stocktaking workspace | Authorized Campaign | Navigate Campaign→Location→Unit→Space | STK-05 context | Authorized scope; record accessible | Load record, pinned geometry/context, sync state and evidence | Read/mutation capability limited by authorization/offline readiness | BR-STK-001, BR-AUTHZ-003 |
| WF-STK-02 | Save non-empty stocktaking | To Do or editable non-final record | Save measurements/product data | Saved operational record | Campaign Team member; 1–5 valid compatible measurements; Commodity/density/Ownership rules satisfied for requested operation | Atomic local business write + SyncOperation; UI Saved only after commit | Validation failure or local transaction failure → no false Saved state | BR-STK-004, BR-STK-005, BR-COM-001, BR-OWN-001, BR-SYNC-001 |
| WF-STK-03 | Calculate physical quantity | Valid non-empty inputs | Calculate/recalculate | CalculationResult | 1–5 compatible measurements; valid datum/geometry/density/version metadata | Normalize→mean ullage→fill height→geometry volume→quantity; persist reproducibility metadata as applicable | Invalid/ambiguous input blocks; no silent clamp/fallback | BR-CALC-001, BR-CALC-002, BR-CALC-004, BR-CALC-005, BR-CALC-007, BR-CALC-010 |
| WF-STK-04 | Validate stocktaking | To Do or Issues classification with valid record | Validate | Validated | Campaign Team member; all applicable validation rules pass | Record validation actor/time; preserve evidence/context; enqueue sync atomically | Failure leaves record non-Validated; Open Issues are not auto-resolved | BR-STK-003, BR-STK-009, BR-ISS-004, BR-SYNC-001 |
| WF-STK-05 | Mark Empty | To Do or Issues classification | Mark Empty | Validated + isEmpty=true | Campaign Team member; authorized context | Set zero volume/quantity, audit actor/time, preserve allowed notes/photos, enqueue sync | Failure leaves prior state; Open Issues not auto-resolved | BR-STK-007, BR-STK-008, BR-ISS-004 |
| WF-ISS-01 | Report Issue | Authorized Stocktaking context | Report Issue | Open Issue | Campaign Team member; Issue Type + Description | Create Open Issue; optional evidence; durable local/sync handling | Validation/storage failure → Issue not falsely reported | BR-AUTHZ-006, BR-ISS-001, BR-ISS-002 |
| WF-ISS-02 | Resolve Issue | Open Issue | Resolve with resolution data | Resolved Issue | Authorized campaign user; explicit resolution data | Persist resolution actor/time/reason/history; sync | Failure keeps Issue Open | BR-ISS-003 |
| WF-ISS-03 | Accept follow-up at Close | Open Issue | Admin accepts matter for follow-up | Resolved Issue | Admin; Close context; explicit reason | Open→Resolved with audited reason before Close may proceed | Without explicit resolution, Close guard remains failed | BR-ISS-005, BR-AUTHZ-006 |
| WF-SYNC-01 | Synchronize pending operation | PENDING | Worker selects eligible operation | IN_FLIGHT | Dependencies satisfied; connectivity; authorization context | Send mutation with same idempotency key and base/entity version | Transport/server result determines next sync state | BR-SYNC-002, BR-SYNC-003, BR-SYNC-004, BR-SYNC-007 |
| WF-SYNC-02 | Acknowledge sync | IN_FLIGHT | Server success | ACKNOWLEDGED | Server accepted idempotent operation | Persist server identity/version/ack metadata as applicable | N/A | BR-SYNC-002, BR-SYNC-003 |
| WF-SYNC-03 | Retryable sync failure | IN_FLIGHT | Retryable transport/server failure | RETRYABLE_ERROR → PENDING on retry | Failure classified retryable | Persist error; exponential backoff; reuse idempotency key | Escalate only if later classified conflict/permanent | BR-SYNC-002, BR-SYNC-003, BR-SYNC-008 |
| WF-SYNC-04 | Sync conflict | IN_FLIGHT | Stale version / competing protected change | CONFLICT | Server detects optimistic-concurrency conflict | Persist durable SyncConflict; require approved resolution path | No silent overwrite/auto-merge | BR-SYNC-004, BR-SYNC-005, BR-SYNC-006, BR-SYNC-009 |
| WF-SYNC-05 | Permanent sync failure | IN_FLIGHT | Non-retryable rejection | PERMANENT_ERROR | Failure classified permanent | Persist actionable error; do not retry blindly | User/admin remediation required as defined by context | BR-SYNC-002 |
| WF-EVD-01 | Evidence capture offline | Stocktaking/Issue context | Add Photo/Note | LOCAL_PENDING | Authorized context; limits/validation pass | Store binary privately (Photo), metadata/hash and outbox durably | Failure before durable commit → no UI success | BR-EVD-001, BR-EVD-002, BR-EVD-003, BR-EVD-004, BR-EVD-005, BR-EVD-006 |
| WF-EVD-02 | Evidence upload | LOCAL_PENDING | Upload starts | UPLOADING | Connectivity; dependencies satisfied | Controlled upload with hash/object metadata | Transfer failure → SYNC_ERROR | BR-EVD-007 |
| WF-EVD-03 | Evidence upload success | UPLOADING | Verified server/object-storage success | SYNCED | Hash/integrity verification succeeds | Persist remote object reference/sync metadata | Integrity/transfer failure → SYNC_ERROR | BR-EVD-004, BR-EVD-007 |
| WF-EVD-04 | Evidence retry | SYNC_ERROR | Retry condition satisfied | UPLOADING | Retry allowed; source binary still durable | Retry controlled transfer | Remain SYNC_ERROR if unsuccessful | BR-EVD-007 |
| WF-CLS-01 | Review Campaign | Active Campaign | Open CLS-01 | Review state/process | Authorized campaign access | Evaluate progress, Issues, sync state and Close Guards | No Campaign status transition merely by reviewing | BR-CAM-001, BR-AUTHZ-004 |
| WF-CLS-02 | Close Campaign | Active Campaign | Close | Completed Campaign + Final Snapshot | Admin; online; all Close Guards pass; server authorization | Atomic server revalidation → create exactly one Final Snapshot → record close actor/time → Active→Completed | Any failure: Campaign remains Active; no partial Final Snapshot | BR-CLS-001, BR-CLS-002, BR-CLS-003, BR-CLS-004, BR-CLS-005, BR-CLS-006 |
| WF-RPT-01 | Generate completed official report | Completed Campaign | Request report | Rendered/exportable report dataset | Authorized report access | Read Final Inventory Snapshot → authoritative report dataset → UI/export | No recalculation from current master data | BR-RPT-001, BR-RPT-002, BR-RPT-004 |
| WF-HIS-01 | Compare inventory history | 2..N Completed Campaigns | Run comparison | History comparison result | Authorized access; selected snapshots available | Compare Final Snapshots; compute physical inventory change; classify comparability | No forecasting; no mutation of source snapshots | BR-HIS-001, BR-HIS-002, BR-HIS-003, BR-HIS-004, BR-HIS-005 |
| WF-USR-01 | Disable user | Active user | Admin disables | Disabled user | Authorized Admin | Persist Disabled; block new auth/ops/assignments; preserve history | Existing historical references unchanged | BR-GOV-003 |
| WF-USR-02 | Reactivate user | Disabled user | Admin reactivates | Active user | Authorized Admin | Restore Active on same identity | No replacement identity created | BR-GOV-003 |

## 6. Canonical State Machines

### 6.1 Campaign

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| Creation intent | Save & Launch | Active | Admin; required campaign fields/scope/team valid; server success | Campaign Code generated; Campaign Snapshot created; Scope frozen |
| Active | Close | Completed | Admin; online; server Close Guards pass | Exactly one Final Inventory Snapshot created atomically |

### 6.2 Stocktaking operational classification

`TO_DO`, `ISSUES`, and `VALIDATED` are the canonical Stocktaking operational classifications used by the product. They must not be modeled as one simple mutually-exclusive lifecycle state machine because Issue lifecycle is independent from Stocktaking validation.

| Condition / action | Resulting classification behavior | Guard | Required effect |
|---|---|---|---|
| Scoped Storage Space has not reached validation | To Do applies when no issue condition takes precedence in the operational view | Authorized campaign context | Work remains incomplete |
| Report Issue succeeds | Issues classification/view applies while the Stocktaking context requires issue attention | Issue successfully created | Open Issue exists independently of validation lifecycle |
| Validate succeeds | Validated lifecycle result is recorded | Non-empty validation succeeds | Record is complete; Open Issue, if any, is not auto-resolved |
| Mark Empty succeeds | Validated lifecycle result is recorded | Mark Empty succeeds | isEmpty=true; volume=0; quantity=0; Open Issue, if any, is not auto-resolved |
| Issue is explicitly resolved | Issue becomes Resolved; Stocktaking classification is re-derived from the remaining Stocktaking/Issue condition | Authorized explicit resolution | No invented direct `ISSUES → TO_DO` persistent transition |

### 6.3 Issue

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| Open | Resolve | Resolved | Authorized explicit resolution | Actor/time/reason/history preserved |

### 6.4 User

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| Active | Disable | Disabled | Authorized Admin | Block new auth/ops/assignments; history preserved |
| Disabled | Reactivate | Active | Authorized Admin | Same identity restored |

### 6.5 Master Data lifecycle

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| Active | Inactivate | Inactive | Authorized Admin; history protection | Existing historical references preserved |
| Inactive | Reactivate | Active | Authorized Admin | Same stable identity |

### 6.6 Sync Operation

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| PENDING | Dispatch | IN_FLIGHT | Dependencies/connectivity satisfied | Same idempotency key |
| IN_FLIGHT | Server success | ACKNOWLEDGED | Accepted idempotent mutation | Persist acknowledgement/version |
| IN_FLIGHT | Retryable failure | RETRYABLE_ERROR | Retryable classification | Backoff; later retry |
| RETRYABLE_ERROR | Retry due | PENDING | Backoff elapsed/conditions satisfied | Same idempotency key |
| IN_FLIGHT | Version conflict | CONFLICT | Optimistic concurrency detects stale/competing write | Durable conflict; no silent overwrite |
| IN_FLIGHT | Permanent rejection | PERMANENT_ERROR | Non-retryable classification | Actionable terminal error until remediation |

### 6.7 Evidence Sync

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| LOCAL_PENDING | Upload | UPLOADING | Connectivity/dependencies satisfied | Binary remains durable |
| UPLOADING | Verified success | SYNCED | Hash/integrity accepted | Remote object reference persisted |
| UPLOADING | Failure | SYNC_ERROR | Transfer/integrity failure | Error retained |
| SYNC_ERROR | Retry | UPLOADING | Retry allowed | Controlled retry |

### 6.8 Offline readiness

| From | Trigger | To | Guard | Required effect |
|---|---|---|---|---|
| Not Offline Ready | Download package | Verifying | Authorized Campaign; package retrieved | Complete package staged |
| Verifying | Integrity success | OFFLINE_READY | Manifest/package integrity valid | Supported offline work enabled |
| Verifying | Integrity failure | Not Offline Ready | Integrity invalid/incomplete | Offline readiness withheld |

## 7. Campaign Lifecycle

### 7.1 Creation and launch

`CAM-03 Create & Launch Campaign` collects the approved campaign configuration, scope, and team. Save & Launch is a single product workflow. On server success, the Campaign becomes Active, the official Campaign Code is generated, Campaign Snapshot is preserved, and Campaign Scope becomes immutable.

There is no persistent Draft state. A failed Save & Launch must not create a partially launched Campaign.

### 7.2 Active

Active is the operational Campaign state. Authorized users may inspect progress; Campaign Team members may perform permitted Stocktaking mutations. The campaign may be prepared for offline work independently of its Active status.

### 7.3 Review

Review is a process on `CLS-01`, not a Campaign state. It evaluates completion, Issues, synchronization, and Close Guards without changing Active status.

### 7.4 Close and Completed

Close requires Admin, connectivity, server authorization, and all Close Guards. The server atomically revalidates guards, creates exactly one Final Inventory Snapshot, records close metadata, and transitions Active → Completed. If any part fails, the Campaign remains Active and no partial final state is committed.

Completed is terminal. Completed → Active is prohibited.

## 8. Stocktaking Workflow

### 8.1 To Do

To Do represents scoped Storage Space work not yet final where the operational view does not classify the context under Issues. A non-empty path collects 1–5 compatible measurements and the required Commodity, Actual Bulk Density, Ownership, and Client data where applicable.

### 8.2 Issues

Issues indicates that the Stocktaking context contains a reported problem requiring attention. It is an operational classification/view, not an independent Issue lifecycle state. Issue lifecycle remains separate from Stocktaking validation. Reporting an Issue creates an Open Issue; resolving it requires an explicit audited action.

### 8.3 Validated

Validated is the final Stocktaking validation result for campaign completion. It may be reached through normal validation or Mark Empty. An Open Issue is not auto-resolved by validation, and Close still requires Issue handling under the approved Close rules. Stale offline data cannot overwrite a Validated result.

### 8.4 Mark Empty branch

Mark Empty is an action, not a state. It results in Validated with `isEmpty=true`, Product Volume = 0, and Physical Quantity = 0. Measurements, Commodity, Actual Bulk Density, Ownership, and Client are not required. Actor/time are audited. Mark Empty does not auto-resolve an Open Issue.

## 9. Calculation Workflow

For non-empty Stocktaking, the canonical calculation workflow is:

**Measurements → Normalization → Aggregation → Effective Fill Height → Geometry Engine → Product Volume → Quantity Engine → Physical Quantity**

Aggregation uses `MEAN_ULLAGE_V1` under `LEVEL_BASED_V1`. Inputs must share Geometry Profile Version, datum, measurement type, and normalizable units. Invalid or ambiguous input blocks calculation; no automatic outlier discard, silent clamping, or density fallback is permitted.

Calculation metadata required for reproducibility is preserved with the result/snapshot. Historical calculations are not recalculated from current master data.

## 10. Issue Workflow

An Issue is created Open through contextual Report Issue. Issue Type and Description are mandatory; evidence is optional. Resolution is explicit, authorized, and audited.

Validate and Mark Empty do not automatically resolve Open Issues. At Close, an Admin may accept a matter for follow-up only by explicitly resolving the Issue with a reason and audit record. An Open Issue is not silently carried through Close.

## 11. Offline and Synchronization Workflow

### 11.1 Offline readiness

An Active Campaign is not automatically Offline Ready. The authorized Campaign Offline Working Set must be completely downloaded and integrity-verified. Only then is the device `OFFLINE_READY` for that Campaign.

### 11.2 Local mutation

A supported offline mutation commits the business change and its SyncOperation in one Room transaction. UI success is reported only after that transaction commits.

### 11.3 Synchronization

Eligible `PENDING` operations move to `IN_FLIGHT`. Success becomes `ACKNOWLEDGED`. Retryable failure becomes `RETRYABLE_ERROR` and later retries with exponential backoff using the same idempotency key. Stale/competing writes become `CONFLICT`; non-retryable rejection becomes `PERMANENT_ERROR`.

Same-entity FIFO ordering and operation dependencies are respected. Critical operations are not silently coalesced. Competing measurements are not automatically merged.

## 12. Evidence Workflow

Photo/Note evidence is saved durably before UI success. Photo binaries use app-private local storage while pending and controlled object-storage upload during synchronization. Metadata, SHA-256, relationships, and sync state are persisted separately.

Evidence transfer follows `LOCAL_PENDING → UPLOADING → SYNCED`, with `SYNC_ERROR` for failed transfer. After validation, evidence belonging to the historical validated context is read-only according to the evidence contract.

## 13. Inventory Close Guard Workflow

Before the server may commit Close, all applicable guards must pass:

1. Campaign is Active.
2. Requesting user is an authorized Admin.
3. Connectivity/server finalization is available.
4. Every scoped Storage Space is final/Validated, including Mark Empty where used.
5. No scoped To Do work remains.
6. Mandatory business data is valid.
7. Critical operations required for Close are synchronized.
8. Open Issues have been explicitly resolved according to the approved Issue workflow.
9. Server-side authorization, version, and consistency checks pass.

Close is one logical atomic boundary: **revalidate → create exactly one Final Inventory Snapshot → record close metadata → transition to Completed**.

## 14. Reporting and History Workflow

Completed official reports read immutable Final Inventory Snapshot data. Active Campaign monitoring may read current operational data but is clearly non-final.

Inventory History selects 2..N Completed Campaigns, compares their Final Inventory Snapshots, computes absolute/percentage Physical Inventory Change, and classifies comparability as Comparable, Scope Added, Scope Removed, or Context Changed. It does not forecast.

## 15. Navigation Workflow Constraints

- Authentication is outside the operational shell.
- Operational bottom navigation is Dashboard / Inventory Campaigns / Stocktaking / Locations / More.
- Navigation transports stable IDs rather than complete mutable business objects.
- Deep links pass through authentication and authorization.
- `STK-05 → STK-03` restores the relevant Storage Unit and list/scroll context.
- Campaign Scope Selector and Team Assignment are contextual parts of CAM-03, not separate major destinations.
- Issue handling, Evidence, and offline readiness remain contextual rather than creating additional global workflow modules.

## 16. Prohibited Transitions and Behaviors

| Prohibited behavior | Reason |
|---|---|
| `COMPLETED → ACTIVE` Campaign transition | Completed is terminal and snapshot-backed. |
| Editing Campaign Scope after launch | Scope is frozen. |
| Treating Review as Campaign status | Review is a process. |
| Treating Mark Empty as Stocktaking status | It is an action resulting in Validated. |
| Treating All as persistent Stocktaking status | All is a UI filter. |
| Auto-resolving Issue on Validate/Mark Empty | Issue resolution is explicit/audited. |
| Overwriting Validated with stale offline data | Historical/operational integrity invariant. |
| Silent last-write-wins on protected conflicts | Conflicts are explicit. |
| Auto-merging competing measurements | Prohibited sync behavior. |
| Offline Inventory Close | Close requires online server atomicity. |
| Partial Final Snapshot on failed Close | Close is atomic. |
| Recalculating completed history from current masters | Final Snapshot is authoritative. |

## 17. Workflow Test Requirements

`18_TEST_STRATEGY_ACCEPTANCE` must cover positive and negative transition paths. At minimum:

- Save & Launch success and validation/server failure with no Draft/partial launch;
- Campaign Code server generation and scope freeze;
- Offline Ready only after complete verified package;
- Stocktaking Team-member vs non-team mutation paths;
- 1–5 measurement validation and incompatible-measurement rejection;
- normal Validate and Mark Empty paths;
- Issue Open→Resolved and prohibition of automatic resolution;
- local atomic business+SyncOperation commit;
- sync success, retryable error, conflict, permanent error, ordering, idempotency and stale-version protection;
- evidence Local Pending/Uploading/Synced/Sync Error paths;
- every Close Guard and atomic Close rollback behavior;
- prohibition of Completed→Active;
- Completed report source and Inventory History comparability behavior.

## 18. Governance

No UI, API, database migration, synchronization handler, or feature implementation may introduce an additional persistent lifecycle state without an explicit approved contract change.

Document 06 may define presentation states such as loading, empty, error, confirmation, selection, or progress indicators. Such UI states must not be persisted or interpreted as new business states unless this document explicitly defines them.

---

**End of Document — 05_WORKFLOWS_STATE_MACHINES**
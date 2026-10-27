# SiloMath V6 — 12_OFFLINE_SYNC_SPECIFICATION

**Version:** V6 Final Documentation — Offline & Sync Specification 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-27 - CR-CALC-VERSION-CONTRACT-001 approved calculation-version clarification
**Normative role:** Canonical offline-first, synchronization, conflict, package, retry and recovery contract for SiloMath V6.

## 1. Purpose, authority and scope

This document defines how SiloMath continues authorized Campaign stocktaking without connectivity and later converges durable Android state with the authoritative backend without data loss, silent overwrite, duplicate business effects or historical corruption. It specializes the frozen V6 contract through Document 11 and the approved OS-D01–OS-D41 decisions.

- Documents 01–11 remain authoritative. This document may specialize synchronization mechanics but cannot weaken business rules, authorization, data-model/database invariants, API semantics, snapshot immutability or calculation reproducibility.
- Business state and synchronization state are orthogonal. ACTIVE, VALIDATED, OPEN, RESOLVED and COMPLETED are never replaced by generic sync statuses.
- Offline-first means local durable operation followed by authoritative server verification; it never creates a second independent source of truth.
- Reconciliation, ERP, Book Stock, Variance and AJG concepts are outside this subsystem and remain outside V6.

## 2. Frozen synchronization invariants

- Every supported offline-capable business mutation and its SyncOperation commit atomically in one Room transaction before the UI reports success.
- Offline-created mutable identities use stable UUIDs before enqueue.
- Every logical mutation has one durable SyncOperation and one stable idempotencyKey. V6 does not coalesce logical mutations.
- Optimistic concurrency uses baseEntityVersion/entityVersion. Retry does not invent a new base version.
- Same-entity operations preserve FIFO; explicit SyncOperationDependency controls cross-entity prerequisites.
- Validated critical data, immutable versions, CampaignSnapshot and FinalInventorySnapshot are never silently overwritten.
- Campaign Scope and pinned geometry/calculation context come from the frozen Campaign Snapshot/package, not from current mutable master data.
- Conflicts are explicit durable records. No last-write-wins, generic force overwrite or automatic field-level merge exists.
- Inventory Close is online, server-authoritative and atomic.
- Sign Out, session expiry, process death, network loss and package refresh failure do not delete durable unsynchronized business work.

## 3. Approved OS-D01–OS-D41 register

| ID | Frozen decision |
|---|---|
| OS-D01 | At most one active sync run per user/device profile. New triggers schedule/reuse the existing orchestration rather than creating a duplicate concurrent engine. |
| OS-D02 | Android WorkManager is the durable background execution mechanism. Foreground/manual Retry invokes the same sync orchestration and state machine. |
| OS-D03 | Sync triggers: authenticated app foreground, connectivity restoration, manual Retry, debounced post-mutation push when online, pre-authoritative-action reconciliation such as Close, and bounded periodic background opportunity when Android permits. |
| OS-D04 | Network availability is not server reachability. After reconnect, authenticated server/session reachability is verified before a run is treated as usable online synchronization. |
| OS-D05 | Local Save never waits for network. UI success follows the atomic Room business mutation + SyncOperation commit; network synchronization is scheduled separately. |
| OS-D06 | Operations in the same entity stream are processed FIFO according to durable local operation order. |
| OS-D07 | SyncOperationDependency overrides eligibility: a dependent operation is not dispatched until all required parent operations are ACKNOWLEDGED. |
| OS-D08 | Independent entity streams may be batched or processed concurrently within bounded implementation limits while preserving same-entity FIFO and explicit dependencies. |
| OS-D09 | Push batch maximum is 100 operations. A smaller operational batch may be configured without changing sync semantics. |
| OS-D10 | V6 performs no semantic operation coalescing. Every logical mutation remains its own durable SyncOperation. |
| OS-D11 | A SyncOperation is not removed or considered complete on send. Only authoritative server acknowledgement produces ACKNOWLEDGED. |
| OS-D12 | Retry backoff baseline is 2s, 5s, 15s, 30s, 60s, then a 5-minute cap, with jitter. |
| OS-D13 | No-connectivity/timeouts and retryable 5xx/503 failures retry automatically using the same operation identity and idempotency key. |
| OS-D14 | HTTP 429 retries automatically, but server Retry-After takes precedence over the local backoff schedule. |
| OS-D15 | HTTP 401 during sync permits one secure session refresh attempt for the run/operation context; then the same operation may retry. Failed refresh stops protected sync and requires Sign In without deleting unsynced work. |
| OS-D16 | 403, permanent validation/domain rejection, immutable/state rejection and other non-retryable failures do not enter a blind retry loop; they become typed permanent/access errors or conflicts as classified by the server. |
| OS-D17 | Manual Retry may make eligible retryable work immediately due, but cannot bypass Retry-After, dependency, authentication, conflict or permanent-error guards. |
| OS-D18 | V6 adds GET /api/v1/sync/pull using a server-issued opaque checkpoint as an additive v1 API completion. The client never interprets checkpoint internals. |
| OS-D19 | Pull page default is 200 changes; the server controls the supported maximum. nextCheckpoint is opaque. |
| OS-D20 | Each pull change carries entityType, entityId, authoritative entityVersion, changeType, serverChangedAt, authoritative representation/reference, and server ordering/checkpoint metadata. |
| OS-D21 | A pulled authoritative UPSERT over a clean local entity is applied transactionally and updates the local authoritative entityVersion/canonical representation. |
| OS-D22 | A pulled server change overlapping an entity with pending local mutation creates/preserves SyncConflict and never silently overwrites the local attempted state. |
| OS-D23 | V6 has no generic automatic field-level merge for dirty business entities, including apparently disjoint fields. Reconciliation occurs through acknowledgement or explicit conflict/reload flow. |
| OS-D24 | Known server COMPLETED/immutable/access-revoked state has authority over incompatible pending local mutation; such mutation is rejected/conflicted, not applied over authoritative state. |
| OS-D25 | Local package lifecycle is NOT_DOWNLOADED -> DOWNLOAD_PENDING -> DOWNLOADING -> VERIFYING -> READY, with DOWNLOAD_ERROR. It is technical local state, never CampaignStatus. |
| OS-D26 | Offline package refresh downloads into a staging version. Only complete successful integrity verification permits an atomic switch to the new current manifest/package. |
| OS-D27 | Failed refresh does not destroy the last verified package. That package remains usable only while still authorized and compatible. Incomplete/corrupt packages never become READY. |
| OS-D28 | Manifest verification covers packageVersion, Campaign/device identity, required content counts/version inventory and cryptographic content hashes. V6 relies on authenticated TLS/server transport for provenance and does not introduce a separate package-signing PKI. |
| OS-D29 | Geometry master changes after Campaign launch do not invalidate the package because pinned versions are used. Calculation engine/schema incompatibility blocks Calculate/Validate until compatibility is restored. |
| OS-D30 | When Campaign COMPLETED is known locally, the package becomes operationally read-only. Known authorization/Team revocation blocks new protected mutations. Unsynced local work is retained for reconciliation. |
| OS-D31 | Revocation while a device is offline takes effect locally when authoritative refresh/sync reveals it; the server rejects later unauthorized pushes and the package becomes access-locked. No cached package can expand authority. |
| OS-D32 | On startup, an IN_FLIGHT operation without durable acknowledgement is recovered into retryable processing with the same operation ID/idempotency key; success or failure is never guessed. |
| OS-D33 | Startup rebuilds pending/error/conflict counters from durable rows, revalidates current package manifests/integrity, and never discards unsynced Measurements, Evidence or locally validated revisions. |
| OS-D34 | Orphan technical binary cleanup after crash is allowed only after proving no committed metadata/outbox reference exists. A committed Evidence/outbox reference protects its required local binary from cleanup. |
| OS-D35 | PHOTO synchronization is ordered as Evidence metadata/business operation -> binary upload dependency -> server verification/final Evidence acknowledgement. SyncOperationDependency represents required ordering. |
| OS-D36 | Binary retry reuses the same Evidence identity and SHA-256. It never creates duplicate Evidence. Evidence sync states remain LOCAL_PENDING -> UPLOADING -> SYNCED, with SYNC_ERROR and controlled retry. |
| OS-D37 | SYN-01 top-level user-facing sync states are exactly SYNCED, PENDING, OFFLINE and ERROR, with lastSuccessfulSync, pendingCount, failedCount, conflictCount and package state/version/lastVerifiedAt. Conflict contributes to ERROR rather than becoming a fifth top-level state. |
| OS-D38 | ACKNOWLEDGED SyncOperations may be purged after 90 days when no unresolved dependency/conflict needs them and durable business state is confirmed. PERMANENT_ERROR and unresolved CONFLICT are never auto-purged. Package cache compaction must not delete unsynced work, the only local Evidence copy, or required official history. |
| OS-D39 | Location facility coordinates are synchronized as ordinary versioned Location master-data fields and are included wherever the authorized Location projection/working set includes that Location. |
| OS-D40 | Map basemap/tiles/provider cache are technical presentation cache only: they are not part of the authoritative Offline Working Set manifest, SyncOperation business payload, or OFFLINE_READY integrity decision. |
| OS-D41 | Offline loss of basemap/network never blocks Location List/hierarchy or stored-coordinate availability; no device location permission/tracking is introduced by sync/offline behavior. |



## 4. Architecture and component responsibilities

The Android synchronization subsystem sits below use cases/repositories and above Room/Retrofit/file transfer. Compose never owns durable sync truth. Repositories expose local-first domain state; sync workers reconcile that state in the background.

| Component | Responsibility | Must not do |
|---|---|---|
| Repository / Use Case | Validate local domain command; execute atomic business write + outbox; expose Room-backed state. | Wait for network before Save success; bypass frozen domain guards. |
| Room database | Durable business entities, SyncOperation, SyncOperationDependency, SyncConflict, manifests/checkpoints and local sync metadata. | Store authentication secrets or treat technical sync status as business lifecycle. |
| Sync Orchestrator | Single-run locking, trigger coalescing, push/pull scheduling, retry classification and checkpoint progression. | Create competing sync engines or delete unresolved work. |
| WorkManager | Durable Android background scheduling under OS constraints. | Define business semantics. |
| Retrofit/API client | Execute /api/v1 sync/package/evidence calls and map typed transport outcomes. | Perform silent merge or authorization decisions. |
| Package Manager | Stage, verify and atomically activate Campaign offline packages. | Overwrite the current verified package before verification. |
| Evidence Transfer | Durable JPEG upload/retry/hash verification and remote-reference reconciliation. | Embed binary into normal sync JSON or mint duplicate Evidence on retry. |
| Conflict Resolver | Persist/display explicit conflicts and execute only approved resolution types. | Offer generic Use Local/Force Overwrite. |
| SYN-01 Read Model | Aggregate durable queue/package/conflict state for user display. | Expose transient in-memory worker state as authoritative truth. |



## 5. Local transaction and outbox contract

A local mutation is considered Saved only after both the business mutation and its SyncOperation have committed. The transaction boundary is mandatory for Measurement edits, working-revision changes, Validate, Mark Empty, Issue operations, permitted Evidence metadata changes and other offline-capable mutations.

```text
BEGIN ROOM TRANSACTION
  validate frozen local domain guards
  mutate/insert local business entity or revision
  insert SyncOperation(
      operationId,
      idempotencyKey,
      organizationId,
      campaignId?,
      entityType,
      entityId,
      operationType,
      baseEntityVersion?,
      eventAt,
      payload,
      state=PENDING
  )
  insert required SyncOperationDependency rows
COMMIT
→ only now may UI expose Saved
```

- If the transaction rolls back, neither the business mutation nor queue operation may appear committed.
- SyncOperation payload is the canonical mutation intent required by the frozen API; it is not an uncontrolled database-row replacement.
- Authentication secrets, access tokens and refresh credentials are never serialized into outbox payloads.
- localDirty is derived from durable unresolved operations rather than maintained as an independent business truth.

## 6. SyncOperation lifecycle

| From | Trigger | To | Required effect |
|---|---|---|---|
| PENDING | Eligible dispatch | IN_FLIGHT | Persist attempt metadata; retain same operation/idempotency identity. |
| IN_FLIGHT | Authoritative success | ACKNOWLEDGED | Apply authoritative entityVersion/canonical response and acknowledgement transactionally. |
| IN_FLIGHT | Retryable transport/server failure | RETRYABLE_ERROR | Persist error/attempt; calculate bounded backoff. |
| RETRYABLE_ERROR | Retry becomes due | PENDING | Same operation and idempotency key become eligible again. |
| IN_FLIGHT | Optimistic/state/access conflict classified as conflict | CONFLICT | Create/link durable OPEN SyncConflict; preserve local attempted + server authoritative context. |
| IN_FLIGHT | Non-retryable rejection | PERMANENT_ERROR | Retain actionable terminal failure until explicit remediation. |
| IN_FLIGHT | Process dies/no durable ACK | PENDING or retryable recovery | Startup recovery never guesses success; same identity is replay-safe. |



ACKNOWLEDGED is the only successful terminal synchronization state. A send attempt, HTTP connection completion or local worker completion is not an acknowledgement.

## 7. Queue eligibility, ordering and batching

- Acquire the single sync-run lock for the active user/device profile.
- Select PENDING or due RETRYABLE_ERROR operations whose dependencies are ACKNOWLEDGED and whose authorization/context is eligible.
- Preserve FIFO for the same entity stream. Independent streams may run concurrently within bounded limits.
- Dispatch no more than 100 operations in one push batch.
- Mark selected operations IN_FLIGHT in a Room transaction before network dispatch.
- Apply every per-operation server result transactionally. A mixed batch may contain ACKNOWLEDGED, CONFLICT, RETRYABLE_ERROR and PERMANENT_ERROR outcomes.
- Do not remove queue evidence on send. Diagnostic cleanup is governed by Section 24.

## 8. Sync orchestration and triggers

| Trigger | Behavior |
|---|---|
| Authenticated app foreground | If server is reachable, schedule reconciliation without blocking navigation. |
| Connectivity restored | Verify authenticated server reachability, then schedule one sync run. |
| Durable local mutation while online | Debounce/schedule background push; Save itself is already complete locally. |
| Manual Retry | Make eligible retryable work due and schedule orchestration; do not bypass hard guards. |
| Before authoritative Close | Require reconciliation/readiness; Close remains server-authoritative and online. |
| Periodic OS opportunity | WorkManager may reconcile within Android scheduling constraints; no aggressive polling. |



Multiple triggers collapse into the same durable orchestration. They do not create concurrent duplicate workers for the same profile.

## 9. Connectivity, authentication and reachability

- Android network callbacks are hints only. A network interface does not prove the SiloMath backend is reachable.
- Protected synchronization requires a valid/restorable session and current server authorization.
- 401 permits one secure refresh attempt in the current sync context. Failure stops protected sync and requires Sign In.
- 403 is not a connectivity failure and must not create an automatic replay loop.
- Session loss never deletes queued mutations, conflicts or Evidence required for later reconciliation.

## 10. Push protocol

Push uses the frozen `POST /api/v1/sync/batch` endpoint. Each operation carries its stable identity, idempotency key, organization/campaign context, entity type/id, operation type, base entity version, canonical mutation payload, dependency IDs and device/client metadata required by the API.

```json
{
  "deviceInstallationId": "uuid",
  "clientVersion": "v6-client",
  "checkpointContext": "opaque-or-null",
  "operations": [{
    "syncOperationId": "uuid",
    "idempotencyKey": "uuid",
    "organizationId": "uuid",
    "campaignId": "uuid",
    "entityType": "STOCKTAKING_REVISION",
    "entityId": "uuid",
    "operationType": "ACTION",
    "baseEntityVersion": 7,
    "eventAtUtc": "2026-09-30T08:00:00Z",
    "dependencyIds": [],
    "payload": {}
  }]
}
```

Per-operation results are applied independently and durably. Exact replay of an already accepted idempotency key reconciles to the original semantic result; a materially different request under the same key is a conflict/error.

## 11. Pull protocol — additive V6 API completion

V6 synchronization requires authoritative server-to-device change retrieval. OS-D18 defines **GET `/api/v1/sync/pull`** as an additive compatible v1 endpoint, and Document 11 already includes it in the frozen endpoint catalogue. The client never interprets opaque checkpoint internals.

| Query/field | Contract |
|---|---|
| checkpoint | Optional server-issued opaque checkpoint. Client stores and returns it verbatim; never parses it. |
| limit | Requested page size. Default 200; server controls supported maximum. |
| organizationId | Required authorized organization context. |
| campaignId | Optional Campaign narrowing where supported by the server sync stream. |
| changes[] | Ordered authoritative change envelopes. |
| nextCheckpoint | Opaque server-issued checkpoint persisted only after the page is successfully applied. |
| hasMore | If true, pull continues using nextCheckpoint. |



```json
{
  "changes": [{
    "entityType": "STOCKTAKING_RECORD",
    "entityId": "uuid",
    "entityVersion": 8,
    "changeType": "UPSERT",
    "serverChangedAt": "2026-09-30T06:20:00Z",
    "data": {}
  }],
  "nextCheckpoint": "opaque",
  "hasMore": false
}
```

## 12. Pull apply rules

| Local condition | Required V6 behavior |
|---|---|
| No pending local mutation for entity | Apply authoritative representation/entityVersion transactionally. |
| Pending local mutation and server change overlaps entity | Preserve local attempted state and create/update SyncConflict; no overwrite. |
| Apparently disjoint server/local fields on dirty business entity | No generic auto-merge. Reconcile through own ACK or explicit conflict/reload path. |
| Server reports immutable/COMPLETED state | Freeze authoritative local representation; incompatible pending work is rejected/conflicted. |
| Authorization/Team revoked | Access-lock protected package/actions when known; pending unauthorized push is rejected/access-conflicted. |
| Unknown parent/reference in page | Defer apply until required parent is available in same/later authoritative data; checkpoint advances only after safe page application. |



The checkpoint is advanced only after the entire received page has been durably and safely reconciled. A crash before that commit causes safe replay of the same page.

## 13. Offline Working Set content

An assigned authorized user must obtain a complete Campaign-scoped package before offline field work is READY. Arbitrary whole-Organization replication is not required.

| Package content | Required |
|---|---|
| Campaign identity/header/status and immutable CampaignSnapshot | Yes |
| Exact Campaign Scope and Location → Storage Unit → Storage Space hierarchy | Yes |
| Stored Location latitude/longitude when present for Locations included in the authorized package/read projection | Yes; ordinary Location master data |
| Pinned GeometryProfileVersion/family/parameter/reference-datum data | Yes |
| Calculation schema/engine/formula/aggregation compatibility metadata | Yes |
| Commodity/CommodityVersion data required by the assigned Campaign | Yes |
| Current Team/authorization context needed for offline capability decisions | Yes |
| Current StocktakingRecord/revisions/status required by assigned scope | Yes |
| Ownership/Client data required for scoped stocktaking | Yes |
| Relevant Issues and Issue history/context | Yes |
| Evidence metadata/content references and required locally available content | Yes |
| Sync/entityVersion/checkpoint compatibility metadata | Yes |
| Manifest counts, versions and cryptographic hashes | Yes |



## 14. Package lifecycle, staging and atomic activation

| State | Meaning |
|---|---|
| NOT_DOWNLOADED | No verified package exists for this Campaign×DeviceInstallation. |
| DOWNLOAD_PENDING | A package acquisition/refresh is scheduled. |
| DOWNLOADING | Package bytes/records are being written to staging. |
| VERIFYING | Completeness, identity, version inventory and hashes are being checked. |
| READY | Current authorized compatible package has passed all checks and was atomically activated. |
| DOWNLOAD_ERROR | Latest acquisition/verification failed; error is retained. |



- These are local package states, not CampaignStatus values.
- Every new package version is staged separately from the current verified package.
- Activation is an atomic pointer/current-manifest switch after verification; partially downloaded records never become current.
- If refresh fails, the prior verified package remains intact and may remain usable only if current authorization and compatibility still permit it.
- OFFLINE_READY is derived from READY + current authorization/compatibility/integrity; it is not persisted as a Campaign lifecycle status.

## 15. Manifest integrity and compatibility

- Manifest identity binds organizationId, campaignId, deviceInstallationId and packageVersion.
- Completeness metadata includes required content counts/version inventory sufficient to prove all package components are present.
- Cryptographic content hashes detect corruption/incomplete content. V6 does not add a separate package-signing PKI; authenticated TLS/server API provides transport provenance.
- Geometry master changes outside the launched Campaign do not invalidate the package because exact pinned versions are frozen.
- Unsupported calculation engine/schema/formula compatibility blocks Calculate and Validate; read-only inspection remains available where safe.
- Corruption of required package content withholds READY and requires controlled re-download/repair.

## 16. Authorization changes and package invalidation

| Condition | Effect |
|---|---|
| Campaign COMPLETED becomes known | Package becomes operationally read-only; final authoritative history is preserved. |
| Campaign Team assignment removed / membership disabled / access revoked becomes known | Block new protected local mutations; package becomes access-locked for protected operations. |
| Revocation occurs while device is offline | Cached package does not gain authority. Server rejects unauthorized pushes at next contact; local state is then access-locked. |
| Geometry master edited outside Campaign | No invalidation; use frozen pinned version. |
| Package superseded by newer compatible version | Stage/verify newer version; atomically activate only on success. |
| Unsynced local work exists during invalidation | Retain it for explicit server reconciliation/conflict; never erase automatically. |



## 17. Stocktaking offline workflow

```text
Verified authorized Campaign package
        ↓
Open scoped Storage Space
        ↓
Edit working revision / ownership / commodity / density
        ↓
Add 1–5 compatible Measurements
        ↓
Calculate locally using pinned V6 calculation contract
        ↓
Optional Issue / Evidence capture
        ↓
Validate locally or Mark Empty if frozen guards pass
        ↓
Atomic Room business mutation + SyncOperation(s)
        ↓
UI confirms local Save / local validated state
        ↓
Reconnect / background sync
        ↓
Server re-authorizes and revalidates
        ├─ ACKNOWLEDGED → reconcile authoritative version
        └─ CONFLICT/PERMANENT_ERROR → preserve local evidence and require controlled remediation
```

A locally validated revision means the device successfully applied the same frozen domain guards against pinned compatible context. It is not equivalent to authoritative server acknowledgement.

## 18. Offline calculation and validation

- Required CampaignSnapshot/Storage Space context, exact GeometryProfileVersion parameters, compatible calculation engine/schema, CommodityVersion and required non-empty inputs must be local.
- Measurements remain limited to 1–5 per StocktakingRevision and must share compatible datum/profile/type/unit semantics.
- Calculation uses the frozen LEVEL_BASED_V1 / MEAN_ULLAGE_V1 pipeline and deterministic decimal rules; Document 13 defines exact formulas without changing sync behavior.
- CalculationResult/input fingerprint/version metadata must prove which inputs generated the locally validated result.
- If inputs change after calculation, the result becomes stale and Validate is blocked until recalculated.
- Server may recompute/verify the deterministic result/fingerprint and remains authoritative for acceptance.
- Mark Empty follows the frozen empty semantics and does not fabricate Measurements, Commodity, density, Ownership or Client.

## 19. Conflict detection and resolution

A SyncConflict is created when the server cannot safely accept or apply local intent without violating version, state, authorization, uniqueness, immutability or other frozen domain rules.

| Conflict class | Example | Automatic overwrite/merge |
|---|---|---|
| VERSION_CONFLICT | Local baseEntityVersion is stale. | No |
| STATE_CONFLICT | Local Validate arrives after incompatible server state change. | No |
| ACCESS_CONFLICT | Membership/Team/scope authority was revoked. | No |
| DUPLICATE_BUSINESS_KEY | Offline-created normalized code collides server-side. | No |
| IMMUTABILITY_CONFLICT | Mutation targets validated/final snapshot/version data that cannot change. | No |
| DEPENDENCY_CONFLICT | Required parent cannot be accepted/resolved. | No |
| IDEMPOTENCY_CONFLICT | Same idempotency key reused with different logical request. | No |
| SAFE_DUPLICATE | Exact accepted request is replayed. | Server returns/reconciles original semantic result; not a second business effect. |



- SyncConflict lifecycle is OPEN → RESOLVED.
- Conflict preserves operationId, entity identity, local attempted payload/version, authoritative server representation/version, detection metadata and resolution metadata.
- Allowed resolution types are exactly RELOAD_AND_REENTER, DISCARD_LOCAL_ATTEMPT and NO_ACTION_REQUIRED.
- RELOAD_AND_REENTER first accepts authoritative state, then any new user correction becomes a new logical mutation with a new SyncOperation/idempotency key.
- DISCARD_LOCAL_ATTEMPT resolves the failed local intent without changing authoritative server state; audit/diagnostic evidence remains according to retention.
- NO_ACTION_REQUIRED is used when reconciliation proves no further business mutation is needed, for example an exact idempotent duplicate already accepted.
- No Force Overwrite, Use Local blindly, silent last-write-wins or automatic field merge exists.

## 20. Retry classification and backoff

| Failure | Automatic retry | Rule |
|---|---|---|
| No connectivity / timeout | Yes | Same operation; retry when reachable using backoff. |
| HTTP 429 | Yes | Honor Retry-After; same operation. |
| Retryable 5xx / 503 | Yes | 2s → 5s → 15s → 30s → 60s → 5min cap + jitter. |
| 401 expired session | Conditional | Refresh once; retry same operation; otherwise require Sign In. |
| 403 authorization | No blind loop | Refresh/lock access context; classify access error/conflict. |
| 409 version/domain conflict | No blind retry | Persist conflict; explicit resolution. |
| 422 validation | No unchanged retry | Requires domain/user correction/new logical mutation. |
| Immutable/completed rejection | No | Permanent/state conflict according to server code. |
| Evidence transfer interruption | Yes if eligible | Same Evidence identity/hash; controlled retry. |



Manual Retry may bypass the local waiting timer once for otherwise eligible retryable work, but never bypasses server Retry-After or hard guards.

## 21. Evidence synchronization

- NOTE is ordinary Evidence metadata/content under the frozen Evidence contract; PHOTO has durable metadata plus app-private binary.
- PHOTO capture commits metadata/hash/local reference and required outbox/dependency state durably before UI success.
- PHOTO limit remains maximum five per StocktakingRevision and five per Issue; NOTE does not consume the photo limit.
- Binary is normalized JPEG, EXIF orientation corrected, unnecessary EXIF stripped, long edge ≤2048 px, target ≤1.5 MB, hard maximum 3 MB, SHA-256 required.
- Metadata/business acceptance precedes or establishes the server Evidence identity/context required for binary upload.
- Binary upload is an explicitly dependent technical operation. Final Evidence SYNCED requires server hash/integrity verification and durable remote reference/acknowledgement.
- Retry uses the same Evidence ID and SHA-256 and never creates duplicate Evidence.
- Validated/frozen Evidence remains immutable even if a stale local operation attempts to alter it.

| Evidence sync state | Transition |
|---|---|
| LOCAL_PENDING | Durable local metadata/binary exists; upload dependency not yet completed. |
| UPLOADING | Eligible controlled transfer is in progress. |
| SYNCED | Server accepted metadata and verified binary/integrity; remote reference persisted. |
| SYNC_ERROR | Transfer/integrity error retained; eligible retry returns to UPLOADING. |



## 22. Crash and restart recovery

- Open and integrity-check Room/local app-private storage.
- Recover abandoned IN_FLIGHT operations without ACK into retryable flow using the same IDs/idempotency keys.
- Rebuild pending, failed and conflict counters from durable tables; never trust stale in-memory worker counters.
- Validate current package manifest/current-pointer integrity.
- Protect every binary referenced by committed Evidence/outbox state.
- Clean only proven orphan technical files that have no committed metadata/outbox reference.
- Restore safe navigation context where possible; navigation restoration cannot bypass authorization/package readiness.
- If authenticated connectivity is available, schedule normal orchestration; do not perform a separate recovery sync algorithm.

## 23. User-facing SYN-01 Sync Status

| State | Definition |
|---|---|
| SYNCED | No pending critical operations, no unresolved sync error/conflict requiring attention, and last reconciliation completed successfully for the displayed scope. |
| PENDING | At least one eligible/queued/in-flight/retry-waiting mutation or pull reconciliation remains. |
| OFFLINE | No usable authenticated server path; durable local operations remain available according to package authorization/readiness. |
| ERROR | At least one permanent/retry-exhausted-visible error or unresolved conflict requires attention. Conflict count is shown separately. |



SYN-01 also exposes `lastSuccessfulSync`, `pendingCount`, `failedCount`, `conflictCount`, and relevant Campaign package `state`, `packageVersion` and `lastVerifiedAt`. It does not expose raw tokens, payloads or implementation internals.

Retry schedules eligible queue/pull work. It never resolves a conflict, rewrites a permanent error or bypasses domain authorization.

## 24. Retention, cleanup and package compaction

| Data | V6 cleanup rule |
|---|---|
| ACKNOWLEDGED SyncOperation | Eligible for purge after 90 days if no unresolved dependency/conflict needs it and durable business state is confirmed. |
| RETRYABLE_ERROR/PENDING/IN_FLIGHT recovery work | Never age-purged while unresolved. |
| PERMANENT_ERROR | Never auto-purged while unresolved/remediation evidence is required. |
| OPEN SyncConflict | Never auto-purged. |
| RESOLVED SyncConflict | Retain according to diagnostic/audit policy and any referenced operation/history requirements. |
| Local Evidence binary | Never remove while it is the only required copy for pending/failed sync or a retained local offline package requires it. |
| Completed Campaign package | May be compacted only when no unsynced work depends on it and official immutable history remains safely available from Final Snapshot/server/cache policy. |
| CampaignSnapshot / FinalInventorySnapshot / critical AuditEvent | Follow the frozen ≥10-year historical retention; sync cleanup cannot shorten it. |
| Notification | 12-month server retention per frozen model; not controlled by queue cleanup. |



The 90-day SyncOperation cleanup window is diagnostic cleanup, not permission to delete business history.

## 25. Inventory Close synchronization safety

- Offline Review/readiness preview may inspect complete local data but is not authoritative Close.
- Final Close requires authenticated online server contact and Admin authorization.
- Before Close, reconciliation must establish that required critical operations are acknowledged and no unresolved SyncConflict prevents authoritative guard evaluation.
- Server revalidates all Close Guards atomically. Local sync state cannot force a Close.
- Success creates exactly one FinalInventorySnapshot and transitions Campaign ACTIVE → COMPLETED atomically.
- Any Close failure leaves Campaign ACTIVE and creates no partial Final Snapshot.

## 26. Immutable snapshots and historical integrity

| Object | Offline/sync rule |
|---|---|
| CampaignSnapshot | Cache exact immutable launch context; never merge later master changes into it. |
| GeometryProfileVersion / CommodityVersion | Pinned immutable versions remain interpretable even after current master version changes. |
| Validated StocktakingRevision | Never silently rewrite; Reset Validation creates a new working revision through the approved workflow. |
| FinalInventorySnapshot + entries | Read-only authoritative completed history; never regenerated from current master data. |
| AuditEvent | Append-only authoritative history; submitted event time and server acceptance/correlation remain distinguishable. |
| Completed report/history data | Read from immutable Final Snapshot; sync does not recalculate historical quantity from current data. |



## 27. Time, ordering and clocks

- `eventAt` records when the business event occurred at its source and is retained as evidence/context.
- `createdAtLocal` records durable local queue creation time.
- `lastAttemptAt` records latest transport attempt.
- `syncedAt` records authoritative acknowledgement/reconciliation time.
- Device clocks are not trusted to order events across devices. Server sequence/checkpoint/entityVersion governs authoritative ordering.
- All instants use UTC persistence/API semantics. Date-only Campaign fields remain stable calendar dates.

## 28. Security and local data protection

- Tokens/refresh credentials are stored only in OS-secure credential storage, never in business Room tables/outbox/diagnostics.
- Production transport is HTTPS/TLS only.
- Offline package is limited to currently authorized Campaign scope and DeviceInstallation; it cannot expand tenant/user access.
- Every push and pull is re-authorized server-side. Cached authorization is a local capability hint, not server authority.
- Evidence binaries remain app-private locally and are never exposed through durable public object URLs.
- Package/content hashes provide integrity, not authentication.
- Diagnostics exclude secrets, passwords, tokens and raw sensitive payloads.
- Sign Out/revocation may lock protected cached content according to authorization state but must not silently destroy unresolved business work needed for reconciliation.

## 29. Observability and support diagnostics

| Signal | Required meaning |
|---|---|
| syncRunId | Correlation identity for one orchestration cycle. |
| syncOperationId | Trace one durable mutation from device through server result. |
| API correlationId | Server/request diagnostic correlation. |
| queueDepth | Counts of pending/in-flight/retryable/permanent operations by safe category. |
| conflictCount | Open conflicts requiring explicit resolution. |
| oldestPendingAge | Age of oldest unresolved eligible operation. |
| lastSuccessfulSync | Last completed successful reconciliation timestamp. |
| pullCheckpointAge | How stale the last authoritative pull checkpoint may be. |
| packageIntegrityStatus | READY/error/current version/last verified per relevant Campaign. |
| errorCodeHistogram | Sanitized typed transport/domain/sync error counts. |



Support diagnostics may include safe UUIDs, versions, counts and timestamps. They must not include credentials or unrestricted raw business payloads.

## 30. Performance and resource policy

- Push batch maximum 100 operations; independent streams may be concurrent within bounded limits.
- Pull default page 200 changes; continue until `hasMore=false` or the run yields according to OS/resource constraints.
- Use HTTPS compression for suitable JSON/package payloads where supported.
- Large Evidence binaries are never embedded in normal sync JSON.
- Background work is event-driven/opportunistic and WorkManager-bounded; no aggressive polling loop.
- Local Save latency is governed by local validation/Room commit, not network latency.
- Package staging must avoid corrupting current verified data even under low-storage/interruption conditions; insufficient storage fails the refresh safely.

## 31. API alignment and additive completion

| API surface | Document 12 use |
|---|---|
| POST /api/v1/sync/batch | Authoritative push of bounded ordered SyncOperations with per-operation outcome. |
| GET /api/v1/sync/status | Operational server-side sync visibility; not a force-overwrite interface. |
| GET /api/v1/sync/pull | Additive V6 completion approved by OS-D18; opaque-checkpoint authoritative pull. |
| GET campaign offline-manifest | Retrieve current authorized manifest metadata for Campaign×DeviceInstallation. |
| GET campaign offline-package | Retrieve complete authorized package/stream for staging and verification. |
| POST Evidence metadata / upload | Separate metadata/business and binary transfer with dependency/integrity semantics. |
| POST Campaign :close | Online authoritative Close only after sync/readiness guards. |



Cross-document consistency status: Document 11 already contains GET `/api/v1/sync/pull`; no further API approval or documentation action is required for this endpoint.

## 32. Acceptance and release gates

| ID | Gate | Acceptance criterion |
|---|---|---|
| SYNC-AC01 | Local durability | Every offline-capable mutation survives process restart before network acknowledgement. |
| SYNC-AC02 | Atomic queueing | Business mutation and SyncOperation are one local transaction. |
| SYNC-AC03 | Single orchestration | Concurrent triggers cannot produce competing sync engines for one profile. |
| SYNC-AC04 | Ordering | Same-entity FIFO and explicit dependencies are preserved across batches/retries. |
| SYNC-AC05 | Idempotency | Replay cannot create duplicate business effects. |
| SYNC-AC06 | Package completeness | READY/OFFLINE_READY only follows complete verified authorized package activation. |
| SYNC-AC07 | Package safety | Failed/corrupt refresh cannot replace the last verified package. |
| SYNC-AC08 | Determinism | Local/server calculation uses identical pinned versioned inputs and decimal semantics. |
| SYNC-AC09 | Conflict safety | Dirty overlapping authoritative changes never silently overwrite or auto-merge. |
| SYNC-AC10 | Authorization | Offline cache never expands scope; every server push/pull is re-authorized. |
| SYNC-AC11 | Crash recovery | IN_FLIGHT work without durable ACK is replay-safe after restart. |
| SYNC-AC12 | Evidence | Binary retry preserves Evidence identity/hash and validated Evidence immutability. |
| SYNC-AC13 | Pull checkpoint | Checkpoint advances only after safe durable page reconciliation. |
| SYNC-AC14 | Close safety | Pending critical work/conflicts/incomplete authoritative state cannot be bypassed by Close. |
| SYNC-AC15 | Historical integrity | Campaign/Final snapshots and immutable versions are never regenerated from current master data. |
| SYNC-AC16 | Retention | Queue cleanup cannot delete unresolved work or shorten frozen business-history retention. |
| SYNC-AC17 | Security | Secrets are absent from Room business payloads, sync diagnostics and package manifests. |
| SYNC-AC18 | Observability | SYN-01 counters/states reconcile to durable queue/conflict/package data. |
| SYNC-AC19 | Performance | Background synchronization never makes ordinary local Save network-dependent. |
| SYNC-AC20 | API compatibility | Additive sync/pull preserves existing /api/v1 semantics and opaque-checkpoint behavior. |
| SYNC-AC21 | Location map data | Stored Location coordinates synchronize/version/conflict exactly like other Location master data; basemap availability never affects business sync authority. |
| SYNC-AC22 | Geo privacy/offline | No device-location permission/tracking is introduced; offline Location list/hierarchy remains usable without basemap. |



## 33. Mandatory test matrix

| ID | Scenario | Expected result |
|---|---|---|
| SYNC-T01 | Offline mutation survives process restart | Business mutation and SyncOperation remain durable; UI did not depend on network. |
| SYNC-T02 | Same operation replayed twice | One business effect; second replay returns/reconciles original semantic result. |
| SYNC-T03 | Crash after IN_FLIGHT before ACK persistence | Startup retries same operation/idempotency key; no duplicate business effect. |
| SYNC-T04 | Dependency parent pending | Child is not dispatched until parent ACKNOWLEDGED. |
| SYNC-T05 | Network drops mid-batch | Acknowledged results remain acknowledged; unacknowledged operations retry safely. |
| SYNC-T06 | HTTP 429 | Retry-After is respected; operation identity unchanged. |
| SYNC-T07 | HTTP 503/timeouts | Bounded exponential backoff + jitter; local Save remains available. |
| SYNC-T08 | 401 during sync | One secure refresh attempt; retry same operation; failed refresh requires Sign In without data deletion. |
| SYNC-T09 | 403 after Team/access revocation | No blind retry; access error/conflict surfaced; new protected mutations blocked once revocation is known. |
| SYNC-T10 | Clean local row receives pull UPSERT | Authoritative representation and entityVersion applied transactionally. |
| SYNC-T11 | Dirty local row receives overlapping server change | SyncConflict created/preserved; no automatic merge or overwrite. |
| SYNC-T12 | Server Campaign completed while device offline | After refresh/pull, package becomes read-only; incompatible pending writes rejected/conflicted. |
| SYNC-T13 | Corrupted package download | VERIFYING fails; current verified package remains if still authorized/compatible; corrupt version never READY. |
| SYNC-T14 | Interrupted package refresh | Staging data cannot replace current verified package. |
| SYNC-T15 | Geometry master changes after launch | Pinned Campaign geometry remains unchanged; no package invalidation. |
| SYNC-T16 | Calculation engine incompatible | Calculate/Validate blocked until compatible client/package. |
| SYNC-T17 | Five measurements then sixth offline | Local frozen validation rejects sixth before enqueue. |
| SYNC-T18 | Offline Validate then unchanged server | Server accepts; revision reconciles to acknowledged authoritative state. |
| SYNC-T19 | Offline Validate then competing server edit | Conflict retained; validated local evidence not silently overwritten. |
| SYNC-T20 | Evidence upload interrupted | Same Evidence ID/hash retries; no duplicate photo record. |
| SYNC-T21 | Crash creates orphan unreferenced file | Recovery may remove only proven orphan; referenced binaries retained. |
| SYNC-T22 | Manual Retry with Retry-After active | Retry does not bypass server delay. |
| SYNC-T23 | Sign Out/session expiry with pending work | Credentials handled securely; durable unsynced business work is not deleted. |
| SYNC-T24 | Two devices mutate same stocktaking record | Optimistic version/state conflict; no last-write-wins. |
| SYNC-T25 | Close with pending critical queue/conflict | Close remains blocked until authoritative guards pass. |
| SYNC-T26 | Completed Final Snapshot cached then master changes | Historical snapshot remains immutable and unchanged. |
| SYNC-T27 | Pull checkpoint restart | Client resumes from durable opaque checkpoint without interpreting its contents. |
| SYNC-T28 | 100+ eligible operations | Batches are bounded at max 100; ordering/dependencies preserved across batches. |
| SYNC-T29 | Package access revoked while offline | Cached package cannot authorize server mutation; revocation is enforced at next authoritative contact. |
| SYNC-T30 | 90-day cleanup | Only eligible ACKNOWLEDGED diagnostics are purged; unresolved conflict/permanent error and required dependencies remain. |
| SYNC-T31 | Location coordinate edited offline | Pair validates and commits with Location mutation + SyncOperation atomically; server reconciliation returns authoritative version. |
| SYNC-T32 | Map opened with no network/basemap | Stored Locations/coordinates and List navigation remain usable; no false sync/error state is created from tile unavailability. |



## 34. Cross-document traceability

| Concern | Frozen upstream contract | Document 12 specialization / downstream handoff |
|---|---|---|
| Offline-first local Save | Architecture/Workflows/Data Model/DB | Atomic Room write+outbox; WorkManager orchestration. |
| DeviceInstallation & manifest | DM-D19–DM-D22, DB mapping | Package staging/current activation/integrity. |
| SyncOperation | DM-D42, workflow state machine, DB | FIFO, batch, retry, crash recovery, cleanup. |
| SyncOperationDependency | DM-D43–DM-D44 | Eligibility/order including Evidence transfer. |
| SyncConflict | DM-D40–DM-D41, IC-11 | Detection, no auto merge, three approved resolutions. |
| Campaign Team revocation | DM-D05–DM-D08 | Known revocation locks actions; offline pushes re-authorized. |
| Stocktaking revisions | DM-D23–DM-D30 | Local working/validated semantics; server ACK remains authoritative. |
| Evidence | DM-D35–DM-D39, Document 11 | Metadata/binary ordering and durable retry; Document 15 may refine storage mechanics only. |
| Calculation | D31–D32 / frozen calculation contract | Offline compatibility/fingerprint; Document 13 defines exact formulas. |
| Close | D18–D20 / workflow/API | Online authoritative atomic Close after reconciliation. |
| Retention | D35 / Data Model | 90-day acknowledged sync diagnostics; no reduction of ≥10-year history. |
| API | Document 11 | Uses frozen endpoints plus approved additive GET /api/v1/sync/pull. |



## 35. Non-goals and prohibited implementations

- No second offline source of truth.
- No direct client synchronization of PostgreSQL rows.
- No last-write-wins for inventory-critical data.
- No automatic field-level merge on dirty business entities.
- No generic Force Overwrite or Use Local conflict action.
- No new Campaign status for offline readiness.
- No deletion of unsynced work on logout, restart, package refresh failure or temporary authorization loss.
- No whole-Organization replication merely to support one Campaign.
- No network-dependent local Save.
- No binary Evidence embedded in normal sync batch JSON.
- No recalculation of Completed historical inventory from current master data.
- No device hardware/advertising identifiers; DeviceInstallation remains privacy-safe app installation UUID.

## 36. Freeze statement

This document is the canonical APPROVED / FROZEN SiloMath V6 Offline & Sync contract after the 2026-10-20 CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze. Implementation may tune bounded worker concurrency, WorkManager scheduling windows, smaller batch sizes, compression, map-tile technical caching, and transport plumbing without Product/Baseline approval only if it preserves OS-D01–OS-D41, all acceptance gates, durable local-first semantics, authorization, idempotency, ordering, explicit conflicts, package integrity, Evidence identity, Close safety and historical immutability.


---
**End of Document — 12_OFFLINE_SYNC_SPECIFICATION**

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

Package compatibility checks the actual executing registry against pinned metadata. Matching manifest text alone is insufficient and the approval itself does not set READY/OFFLINE_READY.

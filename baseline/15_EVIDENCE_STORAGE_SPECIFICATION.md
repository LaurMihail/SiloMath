# SiloMath V6 — 15_EVIDENCE_STORAGE_SPECIFICATION

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-27 - CR-CALC-VERSION-CONTRACT-001 approved calculation-version clarification

## 1. Purpose and authority

This document is the normative SiloMath V6 Evidence and binary-storage contract. It consolidates the frozen Product Baseline, Business Rules, Data Model, Database/API/Offline contracts and the explicitly approved EV-D01–EV-D44 decisions.

It defines Evidence semantics, ownership, lifecycle, image normalization, privacy, local/remote storage, offline durability, synchronization, integrity, cleanup and long-term historical preservation.

## 2. Scope and non-goals

- Evidence types PHOTO and NOTE.
- StocktakingRecord/StocktakingRevision and Issue ownership semantics.
- Working removal and validated immutability.
- PHOTO normalization and integrity metadata.
- App-private local binary storage and private remote object storage.
- Offline save, binary upload, retry and dependency ordering.
- Privacy/data-minimization requirements.
- Cleanup and retention of official evidence.

Out of scope: video/audio evidence, arbitrary document attachments, public media hosting, OCR, EXIF-based geolocation, image editing after validation, and any evidence type not explicitly approved in V6.

## 3. Canonical Evidence model

| Field | PHOTO | NOTE | Contract |
|---|---|---|---|
| evidenceId | Required | Required | Stable UUID Evidence identity. |
| organizationId | Required | Required | Tenant boundary. |
| campaignId | Required | Required | Campaign context. |
| stocktakingRecordId | Conditional | Conditional | Principal owner when Issue is null. |
| issueId | Conditional | Conditional | Principal owner when StocktakingRecord is null. |
| stocktakingRevisionId | Conditional | Conditional | Used only for Stocktaking-owned Evidence; frozen when included in validated revision. |
| evidenceType | PHOTO | NOTE | Exactly one discriminator. |
| noteText | Forbidden | Required | Trimmed non-empty; max 2000 Unicode characters. |
| localPrivateRef | Device-side | Forbidden | Opaque app-private local binary reference. |
| remoteObjectRef | After remote upload | Forbidden | Opaque private object-store key/reference. |
| sha256 | Required | Forbidden | Lowercase SHA-256 of canonical JPEG bytes. |
| mimeType | image/jpeg | Forbidden | Canonical PHOTO media type. |
| byteSize | Required | Forbidden | Canonical processed JPEG size; hard max 3 MB. |
| pixelWidth/pixelHeight | Required | Forbidden | Canonical JPEG dimensions; long edge <=2048 px. |
| syncState | Required | Required | PHOTO uses binary lifecycle; NOTE uses record synchronization without binary phase. |
| removedAt/removedBy | Conditional | Conditional | Audited pre-validation logical removal. |
| createdAt/createdBy | Required | Required | SiloMath Evidence provenance. |

## 4. Ownership and revision binding

Every Evidence has exactly one principal owner:

```text
(stocktakingRecordId != null) XOR (issueId != null)
```

- Stocktaking-owned Evidence belongs to the stable StocktakingRecord and, when included in a validated revision, is bound to that exact StocktakingRevision.
- Before validation, a working Stocktaking-owned Evidence record may not yet have stocktakingRevisionId.
- Validation freezes the revision binding; Reset Validation never moves prior validated Evidence into the new revision retrospectively.
- Issue-owned Evidence belongs to the Issue and survives Reset Validation because the Issue belongs to the StocktakingRecord/campaign-space context rather than one revision.
- Evidence is optional for normal stocktaking and for Report Issue unless a separately frozen rule explicitly makes a specific item mandatory.

## 5. Lifecycle and mutability

| Context | Add | Edit content | Remove | Result |
|---|---|---|---|---|
| Working Stocktaking/Issue | Authorized | Authorized before validation/resolution constraints | Authorized before validation where applicable | Mutable working evidence. |
| Validated StocktakingRevision | No mutation of frozen evidence | No | No | Immutable historical evidence. |
| Issue before Campaign completion | According to authorized issue workflow | Working evidence only | Only if not frozen by validated context | Issue evidence remains issue-owned. |
| COMPLETED Campaign | No | No | No | Historical read-only. |

Removal is logical and audited using removedAt/removedBy. Binary cleanup is a later technical action and must not be confused with the business removal event.

## 6. PHOTO count limits

The canonical V6 limit is:

- maximum **5 active PHOTO per StocktakingRevision**;
- maximum **5 active PHOTO per Issue**;
- NOTE does not consume a PHOTO limit.

The older wording 'per Stocktaking Record' is superseded by the later frozen Data Model clarification 'per StocktakingRevision'. The stable StocktakingRecord remains the principal owner; revision binding determines the stocktaking PHOTO limit and historical set.

Limits are enforced locally for immediate UX and server-side authoritatively. A working PHOTO that has been durably removed no longer consumes the working limit. Validated PHOTO cannot be removed merely to free capacity.

## 7. NOTE contract

- `noteText` is required, trimmed and non-empty.
- Maximum length is 2000 Unicode characters.
- NOTE has no binary object, localPrivateRef, remoteObjectRef, SHA-256, MIME type, byte size or pixel dimensions.
- NOTE synchronizes through normal business metadata/outbox processing and does not enter a binary upload phase.
- NOTE is subject to the same owner, authorization, validation immutability, audit and retention principles applicable to its historical context.

## 8. PHOTO normalization pipeline

The source image is input material only. The official Evidence binary is the canonical processed JPEG.

```text
Capture/select source
  -> decode safely
  -> apply EXIF orientation to pixels
  -> remove unnecessary metadata
  -> resize preserving aspect ratio, long edge <= 2048 px, no upscale
  -> JPEG encode, target <= 1.5 MB
  -> reject if processed output > 3 MB
  -> calculate dimensions + byteSize
  -> SHA-256 exact canonical JPEG bytes
  -> durable app-private save
  -> metadata + outbox durable
  -> UI success
```

The implementation may reduce JPEG quality deterministically to pursue the 1.5 MB target. The target is not a second hard rejection threshold: a valid canonical JPEG above 1.5 MB but at or below 3 MB may be accepted after normalization attempts. Output above 3 MB is rejected.

## 9. Canonical PHOTO metadata

| Attribute | Rule |
|---|---|
| mimeType | Exactly image/jpeg. |
| byteSize | Size of canonical processed JPEG; <=3 MB hard maximum. |
| pixelWidth / pixelHeight | Dimensions of canonical JPEG; max(width,height) <=2048. |
| sha256 | SHA-256 over exact canonical JPEG bytes. |
| localPrivateRef | Opaque local app-private reference; not a public filesystem path. |
| remoteObjectRef | Opaque private object-store key/reference; not a permanent public URL. |
| createdAt / createdBy | SiloMath authoritative Evidence provenance. |
| EXIF | Unnecessary EXIF removed after orientation is materialized. |

## 10. Privacy and provenance

- GPS coordinates are not automatically collected merely because a source photo contains them.
- Device model, serial, IMEI, advertising identifier and similar hardware identifiers are not Evidence business metadata.
- Original filesystem paths are not persisted as business metadata.
- Camera EXIF timestamp/author/device information is not authoritative SiloMath provenance.
- createdAt/createdBy and the normal AuditEvent model provide authoritative application provenance.
- Diagnostic packages exclude photos by default and must not leak raw Evidence binaries or secrets.

## 11. Local storage

- Canonical PHOTO bytes are stored in Android app-private storage.
- Room stores Evidence metadata, relationships, hashes, state and opaque localPrivateRef; it does not store large image BLOBs.
- The source image need not be retained after the canonical private copy is durably created.
- File naming/path layout is an implementation detail behind localPrivateRef and must not be used as business identity.
- Local save is not successful until the canonical file plus metadata/outbox are durable.

## 12. Remote object storage

- Remote PHOTO binaries are stored in private object storage.
- PostgreSQL stores Evidence metadata and remoteObjectRef, not the binary BLOB.
- `remoteObjectRef` is opaque and is not a permanent public URL.
- Backend authorization mediates upload/download/access according to authenticated Organization, Campaign and record context.
- Clients do not derive public object URLs or treat object keys as authorization credentials.
- Logical storage namespace: `organizationId/campaignId/evidenceId/contentHash.jpg`. This is storage organization only; Evidence identity remains evidenceId.

## 13. Authorization

Evidence operations require the same authenticated tenant/campaign/stocktaking or issue authorization as the owning business context. Possession of an evidenceId, localPrivateRef, remoteObjectRef or object key does not grant access.

Server-side checks must validate Organization isolation, Campaign context, principal owner, actor permission, lifecycle/immutability and revision constraints before accepting Evidence metadata or binary operations.

## 14. Offline durability

Offline Evidence is a first-class supported workflow. For PHOTO, UI success requires all of the following to be durable:

- canonical app-private JPEG;
- Evidence metadata including integrity fields;
- the Evidence attachment/owner mutation that creates or changes the owning relationship;
- durable SyncOperation/outbox record and required dependency edges.

A process death or device restart after UI success must not produce 'metadata without file' or 'file without queued business synchronization' as an accepted logical save state.

## 15. PHOTO synchronization state machine

```text
LOCAL_PENDING
   | eligible upload
   v
UPLOADING
   | verified server acknowledgement
   v
SYNCED

LOCAL_PENDING / UPLOADING
   | retryable transfer failure
   v
SYNC_ERROR
   | Retry under normal sync rules
   +--------------------------> LOCAL_PENDING / UPLOADING
```

The UI state is evidence-transfer status, not a replacement for the durable SyncOperation state machine.

## 16. Binary upload ordering and idempotency

- One logical Evidence mutation retains stable evidenceId and idempotency lineage across retry.
- Binary transfer may be a separate technical operation.
- SyncOperationDependency prevents a dependent binary/finalization operation from overtaking prerequisite Evidence/owner metadata operations.
- Retry uses the same Evidence identity and SHA-256 and must not create duplicate Evidence records or duplicate logical objects.
- Large binary bytes are not embedded in ordinary sync JSON batches.

## 17. Server-side integrity verification

The server/object-storage finalization path verifies the uploaded canonical PHOTO against metadata. Final `SYNCED` requires agreement on:

- Evidence identity/context
- canonical MIME type image/jpeg
- byte size
- SHA-256 recalculated from uploaded bytes

Hash, MIME or size mismatch prevents finalization and produces a safe retry/error path. The server never marks corrupted or mismatched bytes as official synchronized Evidence.

## 18. Validation and Close interaction

Validation freezes the Evidence set included in the validated StocktakingRevision. Evidence content/binding that has become historical cannot be mutated afterward.

Evidence is optional as a business concept, but once an official PHOTO is attached to a validated/final context its required synchronization becomes part of critical durability. Close cannot treat such attached evidence as synchronized while transfer remains LOCAL_PENDING, UPLOADING or SYNC_ERROR. Close remains online and server-authoritative.

## 19. Reset Validation

- Reset Validation creates a new working StocktakingRevision.
- Evidence bound to the prior validated revision remains immutable historical evidence.
- Prior evidence is not silently moved/rebound to the new revision.
- New working Evidence may be added under the new revision's limits and authorization rules.
- Issue-owned Evidence remains with the Issue and is unaffected by revision replacement except where the Issue workflow itself changes.

## 20. Removal and orphan handling

Business removal and physical binary deletion are separate.

- Pre-validation removal records removedAt/removedBy and an audit event.
- A removed working PHOTO may become eligible for local/remote orphan cleanup only after it is proven unreferenced.
- Cleanup must account for outbox operations, retries, conflicts, upload finalization and historical references.
- An orphan cleanup job must never infer safety merely from UI invisibility.
- Validated/final Evidence is not an orphan.

## 21. Local cleanup

A local canonical PHOTO may be deleted only when remote durability and integrity have been confirmed and no unsynchronized operation/conflict still depends on the local bytes. Cache pressure does not override this guard.

If the application still needs offline display after safe local cleanup, it may re-fetch through an authorized remote path when online; offline availability is not guaranteed after deliberate safe cache compaction unless the Campaign working-set policy requires retention.

## 22. Retention

Official Evidence tied to validated/final history is retained for at least **10 years** under the frozen retention baseline.

- Metadata and remote binary retention remain coherent.
- FinalInventorySnapshot keeps Evidence references and required frozen metadata; binary content remains in object storage.
- Object cleanup must never leave dangling historical references.
- Historically required Evidence is not hard-deleted because a current master/entity lifecycle changes.
- Retention expiration and any later lawful disposal are governed by Doc19; Doc15 establishes the minimum V6 evidence integrity constraint.

## 23. Final Inventory Snapshot

FinalInventorySnapshot/Entry preserves Evidence references and required frozen metadata according to the frozen Data Model. Large binaries are not copied into PostgreSQL snapshot rows; the immutable snapshot points to retained Evidence/object-storage content through stable references.

## 24. API alignment

The API contract must keep Evidence metadata operations separate from large binary transfer. API payloads use stable evidenceId, owner context, evidenceType and integrity metadata; binary upload/finalization uses the dedicated transfer path defined by the API implementation contract.

Authorization and idempotency are enforced server-side. A client cannot promote itself to `SYNCED` without authoritative verification/acknowledgement.

## 25. Database alignment

- Evidence metadata is authoritative relational data; PHOTO bytes are not PostgreSQL/Room large BLOBs.
- Owner XOR is enforced structurally/transactionally.
- PHOTO/NOTE discriminator controls required/forbidden fields.
- PHOTO limits are enforced transactionally server-side and consistently in Room for immediate local behavior.
- Validated Evidence content/binding cannot be mutated by later generic update paths.
- removedAt/removedBy is controlled pre-validation removal metadata, not a generic deletedAt lifecycle for all Evidence.

## 26. Offline package alignment

Campaign Offline Working Sets include the Evidence metadata/content references declared by the authorized package manifest. PHOTO binary content is included locally only when the verified manifest declares that binary as part of the campaign package. Package verification does not weaken Evidence SHA-256 verification; package-manifest integrity and per-Evidence binary integrity are complementary.

## 27. Error classes

| Condition | Required behavior |
|---|---|
| Owner XOR invalid | Reject Evidence mutation. |
| NOTE blank or >2000 chars | Reject. |
| PHOTO limit exceeded | Reject; no silent eviction. |
| PHOTO canonical output >3 MB | Reject. |
| Unsupported/non-normalized official MIME | Reject/fail normalization. |
| SHA-256 mismatch | Do not finalize; expose safe sync error/retry. |
| Unauthorized owner/campaign/tenant | Reject authorization. |
| Attempt to mutate validated/completed Evidence | Reject immutable operation. |
| Missing local canonical file before first upload | SYNC_ERROR/recovery path; never fabricate SYNCED. |
| Remote upload timeout/5xx | Retry same identity/idempotency according to Doc12. |
| Permanent domain/auth failure | No blind retry; surface actionable error. |
| Unsafe cleanup preconditions | Do not delete local/remote binary. |

## 28. Audit requirements

- Evidence creation records actor/time through normal metadata/audit rules.
- Pre-validation removal is audited with Evidence identity, owner context, actor and time.
- Validated historical Evidence content is not stored in AuditEvent binary payloads.
- Audit before/after structures may contain safe Evidence metadata/references but not image bytes or secrets.
- System cleanup operations use SYSTEM actor/source metadata where audit is required.

## 29. Observability and diagnostics

Safe operational telemetry may include evidenceId, sync state, byte size, hash prefix or full hash where approved as non-secret integrity metadata, retry/error code, upload latency and correlation/operation IDs. Logs and diagnostic packages must exclude PHOTO bytes, auth tokens, publicized private object URLs and unnecessary business payloads.

## 30. Security requirements

- Private local app storage and private remote object storage are mandatory.
- Transport uses the frozen HTTPS/TLS API contract.
- Authorization is checked server-side for metadata and binary access.
- Object references are not bearer credentials.
- Integrity uses SHA-256 over canonical bytes.
- Data minimization removes unnecessary EXIF and avoids automatic GPS/device identifier collection.
- Secrets and credentials are never embedded in Evidence metadata, object keys or diagnostic output.

## 31. Required test matrix

| ID | Scenario | Expected result |
|---|---|---|
| EVD-T01 | Create NOTE with non-empty text | Saved as NOTE; no binary fields. |
| EVD-T02 | Create NOTE with blank text | Rejected. |
| EVD-T03 | NOTE >2000 Unicode chars | Rejected. |
| EVD-T04 | Stocktaking revision has 5 active PHOTO; add sixth | Rejected locally and server-side. |
| EVD-T05 | Issue has 5 active PHOTO; add sixth | Rejected. |
| EVD-T06 | NOTE added when 5 PHOTO exist | Allowed; NOTE does not consume photo limit. |
| EVD-T07 | Remove working PHOTO then add replacement | Allowed after removal is durably recorded. |
| EVD-T08 | Remove validated PHOTO | Rejected. |
| EVD-T09 | Source JPEG with EXIF orientation | Pixels normalized; orientation metadata no longer required. |
| EVD-T10 | Source image long edge >2048 | Canonical output long edge <=2048, aspect ratio preserved. |
| EVD-T11 | Source image long edge <2048 | No upscale. |
| EVD-T12 | Canonical processed image <=1.5 MB | Accepted if all other checks pass. |
| EVD-T13 | Canonical processed image >1.5 MB but <=3 MB after target attempts | Accepted; target is not the hard limit. |
| EVD-T14 | Canonical processed image >3 MB | Rejected. |
| EVD-T15 | PHOTO metadata MIME | Exactly image/jpeg. |
| EVD-T16 | SHA-256 verification | Hash matches exact canonical JPEG bytes. |
| EVD-T17 | Tampered uploaded bytes | Server hash mismatch; binary not finalized. |
| EVD-T18 | Offline PHOTO save then process restart | Canonical file + metadata + outbox survive. |
| EVD-T19 | Upload timeout | Same Evidence/idempotency lineage retries; no duplicate. |
| EVD-T20 | PHOTO transfer failure | State becomes SYNC_ERROR; Retry remains available subject to sync rules. |
| EVD-T21 | Issue evidence then Reset Validation | Issue-owned evidence remains attached to Issue. |
| EVD-T22 | Stocktaking evidence included in Validation | Bound to exact validated StocktakingRevision and becomes immutable. |
| EVD-T23 | Validated evidence after Reset Validation | Prior evidence remains with prior validated revision; not silently moved. |
| EVD-T24 | Attempt owner XOR violation | Rejected. |
| EVD-T25 | Attempt public object URL as authoritative ref | Rejected by contract; opaque private reference required. |
| EVD-T26 | GPS/device EXIF present | Not retained as Evidence business metadata. |
| EVD-T27 | Close with attached PHOTO still LOCAL_PENDING | Close guard fails until required evidence transfer is synchronized. |
| EVD-T28 | Cleanup local PHOTO before remote durability | Blocked. |
| EVD-T29 | Cleanup local PHOTO after verified remote durability and no dependency | Permitted by cleanup policy. |
| EVD-T30 | Historical official evidence at 10-year horizon | Still resolvable according to minimum retention contract. |

## 32. Acceptance / release gates

| Gate | Acceptance criterion |
|---|---|
| EVD-AC01 | Evidence discriminator is exactly PHOTO/NOTE. |
| EVD-AC02 | Owner XOR is enforced for StocktakingRecord versus Issue. |
| EVD-AC03 | Stocktaking validation freezes exact revision binding for included evidence. |
| EVD-AC04 | Issue-owned evidence survives stocktaking Reset Validation. |
| EVD-AC05 | PHOTO limits are five per StocktakingRevision and five per Issue. |
| EVD-AC06 | NOTE never consumes PHOTO limit and has no binary object. |
| EVD-AC07 | NOTE text is non-empty after trim and <=2000 Unicode characters. |
| EVD-AC08 | All official PHOTO binaries are canonical JPEG. |
| EVD-AC09 | Orientation correction, <=2048 px long edge, <=1.5 MB target and 3 MB hard limit are enforced. |
| EVD-AC10 | SHA-256 covers exact canonical bytes and is verified remotely. |
| EVD-AC11 | Unnecessary EXIF/GPS/device identifiers are not retained automatically. |
| EVD-AC12 | Official local binaries remain app-private and are not Room BLOBs. |
| EVD-AC13 | Remote object storage is private and references are opaque. |
| EVD-AC14 | Evidence access is subject to backend tenant/campaign/record authorization. |
| EVD-AC15 | Offline save is durable before UI success. |
| EVD-AC16 | PHOTO transfer states and retry/idempotency follow the frozen sync contract. |
| EVD-AC17 | Binary transfer dependencies cannot overtake required Evidence/business operations. |
| EVD-AC18 | Validated/completed evidence is immutable. |
| EVD-AC19 | Local cleanup cannot destroy the only durable required binary or an unsynchronized dependency. |
| EVD-AC20 | Official validated/final Evidence remains coherent and retained for at least 10 years. |

## 33. Approved EV-D01–EV-D44 decision register

| ID | Frozen V6 outcome |
|---|---|
| **EV-D01** | Evidence types are exactly PHOTO and NOTE. |
| **EV-D02** | Every Evidence has exactly one principal owner: StocktakingRecord XOR Issue. |
| **EV-D03** | Stocktaking-owned Evidence may exist in working context without stocktakingRevisionId; when included in a validated revision it is bound to that StocktakingRevision and the binding becomes immutable. |
| **EV-D04** | Issue-owned Evidence belongs to the Issue, not to a StocktakingRevision, and survives Reset Validation with the Issue. |
| **EV-D05** | Evidence is optional for normal Stocktaking and Report Issue unless another explicit approved rule requires it. |
| **EV-D06** | Working Evidence may be removed only before Validation by an authorized actor. |
| **EV-D07** | Removal is logical and audited through removedAt/removedBy; metadata is not immediately hard-deleted. |
| **EV-D08** | Evidence included in validated history is immutable/read-only; COMPLETED Campaign evidence cannot be edited, moved or removed. |
| **EV-D09** | Maximum five PHOTO per StocktakingRevision. |
| **EV-D10** | Maximum five PHOTO per Issue. |
| **EV-D11** | NOTE does not consume a PHOTO limit. |
| **EV-D12** | PHOTO limits are enforced locally before save and authoritatively server-side during mutation/sync. |
| **EV-D13** | A durably removed working PHOTO no longer consumes the working limit; validated PHOTO cannot be removed to free capacity. |
| **EV-D14** | NOTE requires trimmed, non-empty noteText. |
| **EV-D15** | NOTE noteText maximum length is 2000 Unicode characters. |
| **EV-D16** | NOTE has no localPrivateRef, remoteObjectRef, sha256, image dimensions or binary upload. |
| **EV-D17** | NOTE synchronizes as normal metadata/business payload; its sync state has no binary-transfer phase. |
| **EV-D18** | Camera/gallery/file-picker input is normalized before Evidence save; the canonical official PHOTO binary is JPEG. |
| **EV-D19** | EXIF orientation is applied to pixels before unnecessary metadata is stripped. |
| **EV-D20** | Resize preserves aspect ratio, limits the long edge to 2048 px and never upscales a smaller image. |
| **EV-D21** | JPEG encoding targets <=1.5 MB; implementation may reduce JPEG quality deterministically to pursue the target. |
| **EV-D22** | Processed PHOTO >3 MB is rejected and is not saved as valid Evidence. |
| **EV-D23** | Canonical PHOTO mimeType is image/jpeg. |
| **EV-D24** | pixelWidth, pixelHeight and byteSize describe the canonical processed JPEG. |
| **EV-D25** | SHA-256 is calculated over the exact bytes of the canonical processed JPEG. |
| **EV-D26** | The selected/captured source image is not the official Evidence binary and may be released after the canonical private copy is durably saved. |
| **EV-D27** | Canonical JPEG strips EXIF not functionally required. |
| **EV-D28** | GPS, device model/serial/hardware identifiers and original filesystem path are not collected as Evidence business metadata. |
| **EV-D29** | Orientation is materialized in pixels; EXIF orientation need not be retained. |
| **EV-D30** | SiloMath createdAt/createdBy is official provenance; camera EXIF timestamp/author/device metadata is not authoritative provenance. |
| **EV-D31** | Canonical PHOTO is stored locally only in app-private storage; it is not stored as a Room BLOB or official public/shared-media copy. |
| **EV-D32** | Room stores metadata plus opaque localPrivateRef; PostgreSQL stores metadata plus opaque remoteObjectRef; neither stores large image BLOBs. |
| **EV-D33** | Remote PHOTO binary is stored in private object storage; the container/bucket is not public. |
| **EV-D34** | remoteObjectRef is an opaque object key/reference, not a permanent public URL. |
| **EV-D35** | Remote download/access is authorized through the backend using Organization/Campaign/record authorization; clients do not construct public object URLs. |
| **EV-D36** | Logical object-key namespace is organizationId/campaignId/evidenceId/contentHash.jpg; it is storage organization, not business identity. |
| **EV-D37** | PHOTO save reports success only after canonical file, Evidence metadata and business/outbox operation are durable. |
| **EV-D38** | PHOTO transfer lifecycle is LOCAL_PENDING -> UPLOADING -> SYNCED, with SYNC_ERROR for failed transfer/retry. |
| **EV-D39** | Retry reuses the same evidenceId, SHA-256 and logical idempotency lineage and must not create duplicate Evidence. |
| **EV-D40** | Binary upload may be a separate technical operation and must preserve explicit SyncOperationDependency ordering with Evidence metadata/business mutation. |
| **EV-D41** | Server finalizes a PHOTO binary only when MIME, byte size and recalculated SHA-256 agree with the Evidence metadata. |
| **EV-D42** | Close cannot treat attached official PHOTO evidence as synchronized while its required transfer is LOCAL_PENDING, UPLOADING or SYNC_ERROR. |
| **EV-D43** | Local PHOTO cleanup is allowed only after confirmed remote durability/integrity and when no unsynchronized operation/conflict still depends on the local bytes; removed working orphan binaries require proven unreferenced status. |
| **EV-D44** | Official Evidence tied to validated/final history is retained for at least 10 years; metadata, snapshot references and remote objects must remain coherent without dangling historical references. |

## 34. Cross-document handoff

- Doc16 Reporting Specification must use stable Evidence references/frozen metadata and must not require large binaries inside report read models unless a specific approved export explicitly embeds them.
- Doc17 Security/Audit/Privacy must preserve private storage, tenant authorization, EXIF/GPS minimization, integrity verification and safe diagnostics.
- Doc18 Test Strategy/Acceptance must include the normalization, owner XOR, limits, offline durability, retry, integrity, immutability and cleanup tests defined here.
- Doc19 Retention Policy must preserve official validated/final Evidence for at least 10 years and define disposal only after all legal/product/historical guards are satisfied.
- Doc20 Production Operations must cover object-storage durability, monitoring, backup/recovery expectations, orphan cleanup safety and integrity-first failure handling.

## 35. Freeze statement

This document is **APPROVED / FROZEN V6** and freezes the SiloMath V6 Evidence/Storage contract: PHOTO/NOTE semantics, ownership and revision binding, limits, canonical JPEG processing, SHA-256 integrity, privacy, app-private/object storage, offline durability, transfer state, retry/dependency behavior, immutability, cleanup and minimum historical retention. Any semantic change requires an explicit approved V6 Baseline Change and cross-document consistency update. Provider/library/storage implementation choices that preserve these requirements require no Product/Baseline approval.

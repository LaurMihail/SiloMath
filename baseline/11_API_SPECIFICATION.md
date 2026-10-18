# SiloMath V6 — 11_API_SPECIFICATION

**Version:** V6 Final Documentation — API Specification 1.0 Candidate  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-17 — Android/UX implementation-closure refreeze  
**Normative role:** Canonical external/backend REST contract for Android ↔ server communication.

## 1. Purpose, authority and scope

This document defines the complete V6 HTTP/API contract required by the frozen product, business, authorization, workflow, architecture, data-model and database contracts through Document 10. It defines resources, endpoints, DTO semantics, authentication transport, organization scope, authorization, optimistic concurrency, idempotency, pagination, errors, offline package transfer, synchronization, Evidence transfer, Close, reports, history, audit, notifications and configuration.

It does **not** make SQL/Room structures public API, does not redefine business rules, and does not create new workflow states. Where a later specialist document refines transfer scheduling, formula detail, binary storage, report layout, security hardening, retention execution or operations, that refinement must preserve this API's frozen semantic outcomes.

### 1.1 Normative sources

- 01_PRODUCT_BASELINE through 10_DATABASE_SCHEMA, including D01–D36, FC-01–FC-11, IC-01–IC-17, DM-D01–DM-D64 and DB-D01–DB-D68.
- Server-side authorization is authoritative; Android visibility is UX only.
- The backend is the enterprise source of truth; Android operational screens read repository/Room state rather than rendering transient HTTP responses directly.

## 2. Protocol and versioning

- Production transport: **HTTPS/TLS only**; cleartext is prohibited.
- API style: REST/HTTP JSON for ordinary resources and commands; dedicated binary/stream transfer for Evidence and offline package/export content.
- Base path: **`/api/v1`**. Breaking HTTP contract changes require a new major API path/version; additive compatible fields may be introduced within v1.
- Media type for JSON: `application/json; charset=utf-8`.
- IDs are UUID strings. Business Codes are separate values.
- JSON property naming: lower camelCase. Stable enum codes are uppercase machine codes.
- Unknown additive response fields must be ignored by clients. Servers must reject unknown/unsupported mutation enum values that would alter business meaning.
- Server timestamps are authoritative UTC ISO-8601. Date-only campaign values remain `YYYY-MM-DD` and are never timezone shifted.
- Governed decimals are represented losslessly and mapped to decimal types; calculation/business code must not parse them through binary floating point.

## 3. Standard request headers

| Header | Required | Contract |
|---|---:|---|
| `Authorization: Bearer <access-token>` | Protected endpoints | Secure server-issued session credential. Token internals are not a client/domain contract. |
| `X-Correlation-Id` | Recommended; server creates if absent | UUID/string trace identifier returned on response and errors. Never authorization. |
| `Idempotency-Key` | Synchronized/retryable mutations | Stable UUID/key for one logical mutation. Retry reuses exactly the same key. |
| `If-Match-Entity-Version` | Mutable synchronized updates where applicable | Decimal integer base entity version. Equivalent body field is allowed for sync batch; semantics must be identical. |
| `X-Organization-Id` | Optional context aid | May carry selected organization context but never substitutes for path/resource authorization. |
| `X-Device-Installation-Id` | Offline/package/sync when the endpoint contract explicitly requires it | Privacy-safe installation UUID; never IMEI/advertising/hardware ID. |
| `Accept` / `Content-Type` | According to the endpoint contract | JSON, JPEG, PDF, CSV or package stream according to endpoint. |

### 3.1 Standard response headers

- `X-Correlation-Id` on every server response.
- `ETag` may be emitted for cache efficiency, but optimistic business concurrency is defined by `entityVersion`, not by opaque HTTP cache semantics.
- `Retry-After` is used with 429/503 where the server can provide a retry interval.
- Binary responses provide safe `Content-Type`, `Content-Length` where known, and integrity metadata when the endpoint contract explicitly requires it.

## 4. Authentication and session contract

- V6 supports email/password Sign In, Forgot Password, Reset Password and secure automatic session restoration.
- No public Sign Up, Remember Me, Google SSO or Microsoft SSO.
- Password rules and credential/session lifetime are server-controlled. The API may return machine-readable password requirements/errors; Android must not invent a competing policy.
- A Disabled User cannot establish a new valid session or perform new operations.
- Sign Out revokes/removes session credentials as supported but never instructs Android to delete unsynchronized business data.
- Offline continuation is not offline authentication: it is use of previously authorized cached campaign context. Remote protected actions always re-authorize server-side.
- Access and refresh credentials are secrets: never returned in logs/errors, never placed in URLs, and never stored in ordinary Room business tables.

## 5. Authorization evaluation

For every protected request the server evaluates, according to the endpoint contract: authenticated Active User → valid OrganizationMembership → role Admin/Standard → Organization/data scope → Campaign access → Campaign Team membership for Stocktaking mutations → entity/workflow state → connectivity/server-authority guard → optimistic version/integrity rules.

Admin role alone does not grant Stocktaking mutation rights. Campaign Team membership remains required. Close is Admin-only and online. Audit Trail is Admin-only. Organization tenant creation requires platform/onboarding authority, not ordinary Organization Admin status.

## 6. Common DTO and serialization contract

| Contract | Fields / rule |
|---|---|
| CommonMutationControl | idempotencyKey UUID; baseEntityVersion long where mutable entity exists; clientOperationId UUID; occurredAtUtc optional client fact; deviceInstallationId when sync/offline context requires it. |
| Page<T> | items[]; nextCursor nullable; hasMore boolean. Cursor is opaque; clients must not construct/parse it. |
| FieldError | field; code; message. |
| ApiError | code; message; fieldErrors[]; correlationId; retryable; details optional structured safe object. |
| Entity metadata | id UUID; createdAt/By; updatedAt/By; entityVersion for mutable synchronized entities. |
| Lifecycle codes | ACTIVE/INACTIVE for master; ACTIVE/DISABLED for User/membership; ACTIVE/COMPLETED Campaign; OPEN/RESOLVED Issue. |
| Date/time | UTC timestamps as ISO-8601 with Z/offset; campaign dates as YYYY-MM-DD. |
| Decimals | JSON decimal numbers/strings must round-trip exactly; Android maps to BigDecimal, never Double for governed calculation values. |
| Facility coordinates | `latitude` decimal degrees and `longitude` decimal degrees are optional only as one pair; latitude -90..90, longitude -180..180. They are Location master data, not device/user position. |

### 6.1 Mutation response rule

A successful mutation returns the authoritative server representation or an explicit acknowledgement containing the authoritative `entityVersion`. Android reconciles that response into Room. A successful HTTP response never authorizes UI to bypass local repository reconciliation.

### 6.2 Null, absent and patch semantics

- `PATCH` changes only explicitly supplied mutable fields.
- Omitted property means unchanged. Explicit `null` is accepted only for fields whose frozen domain contract permits null/clearing.
- Immutable IDs, frozen version IDs, historical snapshot fields and server-controlled fields are rejected if supplied as mutation overrides.

## 7. Cursor pagination, filtering and search

- Large server collections use opaque cursor pagination.
- Query shape: `cursor`, `limit`, plus endpoint-specific filters/search/sort. `nextCursor` is opaque.
- Default/max page size is server-controlled operational configuration; it is **not** a business rule and must not create fixed UI pagination.
- Stable ordering includes a deterministic UUID tie-breaker.
- Search/filter behavior must preserve tenant authorization and frozen field/search contracts.
- Offline Campaign package is complete for assigned scope and is not dependent on paginating the next operational Stocktaking item.

## 8. Error contract

All non-success JSON API errors use:

```json
{"code":"STABLE_MACHINE_CODE","message":"User-safe message","fieldErrors":[{"field":"name","code":"REQUIRED","message":"Required"}],"correlationId":"uuid","retryable":false,"details":{}}
```

Android maps `code` to typed outcomes. Business behavior must never parse free-text `message`. `details` is optional, safe, structured context and must never contain secrets or raw sensitive payloads.

### 8.1 HTTP status semantics

| HTTP | Meaning |
|---:|---|
| 200/201 | Successful read/create/command with body. |
| 202 | Accepted asynchronous/non-sensitive acknowledgement where immediate completion is not required. |
| 204 | Successful command/delete with no body. |
| 400 | Malformed request/protocol syntax. |
| 401 | Missing/invalid/expired authentication. |
| 403 | Authenticated but not authorized, disabled, or outside permitted scope. |
| 404 | Resource not available in authorized lookup context. |
| 409 | State/version/idempotency/conflict/immutability violation. |
| 413 | Evidence payload exceeds hard transfer limit. |
| 422 | Well-formed request violates field/domain validation. |
| 429 | Rate limited; retryable according to response metadata. |
| 5xx | Server/transient/unexpected failure; no client assumption of mutation success without idempotent reconciliation. |

### 8.2 Canonical machine errors

| Code | HTTP | Retryable | Meaning |
|---|---:|---:|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | false | Sign-in credentials rejected. |
| `AUTH_SESSION_EXPIRED` | 401 | false | Session cannot be restored/refreshed. |
| `AUTH_USER_DISABLED` | 403 | false | User lifecycle is DISABLED. |
| `AUTH_FORBIDDEN` | 403 | false | Authenticated subject lacks effective permission. |
| `TENANT_SCOPE_VIOLATION` | 403 | false | Requested resource is outside authorized Organization scope. |
| `RESOURCE_NOT_FOUND` | 404 | false | Authorized lookup has no matching resource. |
| `VALIDATION_FAILED` | 422 | false | One or more field/domain validations failed; fieldErrors supplied where applicable. |
| `ENTITY_VERSION_CONFLICT` | 409 | false | baseEntityVersion is stale; authoritative version/context returned or retrievable. |
| `IDEMPOTENCY_KEY_REUSE_MISMATCH` | 409 | false | Same key was reused with a different logical request. |
| `CAMPAIGN_SCOPE_FROZEN` | 409 | false | Attempt to modify launched scope. |
| `CAMPAIGN_COMPLETED_IMMUTABLE` | 409 | false | Mutation attempted against completed immutable context. |
| `CAMPAIGN_TEAM_REQUIRED` | 422 | false | Launch requires at least one team member. |
| `LAST_ACTIVE_ADMIN` | 409 | false | Membership mutation would leave no ACTIVE Admin. |
| `STOCKTAKING_NOT_TEAM_MEMBER` | 403 | false | Stocktaking mutation requires active Campaign Team assignment. |
| `STOCKTAKING_REVISION_STALE` | 409 | false | Working revision/currentRevision changed. |
| `STOCKTAKING_VALIDATION_FAILED` | 422 | false | Validation invariant failed. |
| `CALCULATION_STALE_FINGERPRINT` | 409 | false | Calculation no longer matches current inputs. |
| `CALCULATION_INVALID_INPUT` | 422 | false | Invalid/mixed datum, invalid fill, unsupported version or required formula data missing. |
| `EVIDENCE_PHOTO_LIMIT` | 422 | false | Maximum five PHOTO for the applicable revision/Issue. |
| `EVIDENCE_HASH_MISMATCH` | 422 | false | Uploaded binary does not match declared SHA-256. |
| `EVIDENCE_TOO_LARGE` | 413 | false | PHOTO exceeds hard 3 MB limit. |
| `EVIDENCE_IMMUTABLE` | 409 | false | Validated/frozen Evidence cannot be removed/rebound. |
| `ISSUE_ALREADY_STATE` | 409 | false | Requested transition does not change current Issue state. |
| `ISSUE_COMPLETED_CAMPAIGN` | 409 | false | Issue cannot be reopened after Campaign completion. |
| `CLOSE_GUARDS_FAILED` | 409 | false | Atomic Close guard evaluation failed; structured guard failures returned. |
| `SYNC_CONFLICT` | 409 | false | Critical mutation conflicts with authoritative server state. |
| `SYNC_DEPENDENCY_PENDING` | 409 | true | Dependent operation cannot apply before required parent acknowledgement. |
| `RATE_LIMITED` | 429 | true | Client should honor Retry-After. |
| `SERVICE_UNAVAILABLE` | 503 | true | Transient server/service dependency failure. |
| `INTERNAL_ERROR` | 500 | false | Unexpected server error; correlationId required. |

## 9. Idempotency and optimistic concurrency

- Every synchronized/retryable logical mutation has one stable `Idempotency-Key` / `idempotencyKey`. Retrying does not mint a new key.
- Server stores/recognizes the logical result sufficiently to prevent duplicate business effects.
- Reuse of the same key with a materially different request is rejected with `IDEMPOTENCY_KEY_REUSE_MISMATCH`.
- Mutable synchronized entities use `baseEntityVersion`. If stale, server returns `ENTITY_VERSION_CONFLICT`/typed conflict and never silently overwrites.
- Validated data cannot be overwritten by stale offline data; Completed Campaign cannot be downgraded; immutable snapshots/versions cannot be updated.
- Same-entity FIFO and explicit SyncOperation dependencies are preserved by sync processing.

## 10. Endpoint catalogue

| Method | Endpoint | Auth | Effective permission | Request | Response | Contract note |
|---|---|---|---|---|---|---|
| POST | /api/v1/auth/sign-in | Public | Active User | SignInRequest | SessionResponse | Email/password. Disabled user rejected. |
| POST | /api/v1/auth/forgot-password | Public | Public | ForgotPasswordRequest | AcceptedResponse | Non-enumerating response. |
| POST | /api/v1/auth/reset-password | Recovery context | Valid recovery context | ResetPasswordRequest | AcceptedResponse | Password policy remains server-controlled. |
| POST | /api/v1/auth/session/refresh | Refresh credential | Valid session subject | RefreshSessionRequest | SessionResponse | Rotation/revocation is server security policy. |
| POST | /api/v1/auth/sign-out | Bearer | Authenticated | SignOutRequest | 204 | Revokes supported server session/refresh credential; does not delete local unsynced data. |
| GET | /api/v1/me | Bearer | Authenticated Active User | — | CurrentUserResponse | Identity + memberships + effective server capabilities/context. |
| GET | /api/v1/organizations | Bearer | Authorized memberships | CursorQuery | Page<OrganizationSummary> | Only organizations user may access. |
| GET | /api/v1/organizations/{organizationId} | Bearer | Authorized membership | — | OrganizationResponse | Tenant-scoped. |
| POST | /api/v1/organizations | Bearer/platform | Provisioning authority | OrganizationCreateRequest | OrganizationResponse | Not granted merely by Organization Admin role. |
| PATCH | /api/v1/organizations/{organizationId} | Bearer | Admin membership | OrganizationPatchRequest | OrganizationResponse | Lifecycle/code rules enforced. |
| GET | /api/v1/organizations/{organizationId}/locations | Bearer | Authorized membership | CursorQuery + search/status | Page<LocationResponse> | Cursor-paged. |
| POST | /api/v1/organizations/{organizationId}/locations | Bearer | Admin | LocationCreateRequest | LocationResponse | Idempotent mutation contract. |
| GET | /api/v1/organizations/{organizationId}/locations/{locationId} | Bearer | Authorized membership | — | LocationResponse | Tenant chain checked. |
| PATCH | /api/v1/organizations/{organizationId}/locations/{locationId} | Bearer | Admin | LocationPatchRequest | LocationResponse | baseEntityVersion required. |
| GET | /api/v1/organizations/{organizationId}/locations/{locationId}/storage-units | Bearer | Authorized membership | CursorQuery + search/status | Page<StorageUnitResponse> | Cursor-paged. |
| POST | /api/v1/organizations/{organizationId}/locations/{locationId}/storage-units | Bearer | Admin | StorageUnitCreateRequest | StorageUnitResponse | Canonical code validation. |
| GET | /api/v1/organizations/{organizationId}/storage-units/{storageUnitId} | Bearer | Authorized membership | — | StorageUnitResponse | Location/tenant consistency checked. |
| PATCH | /api/v1/organizations/{organizationId}/storage-units/{storageUnitId} | Bearer | Admin | StorageUnitPatchRequest | StorageUnitResponse | Optimistic concurrency. |
| GET | /api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces | Bearer | Authorized membership | CursorQuery + search/status | Page<StorageSpaceResponse> | Large-list cursor pagination. |
| POST | /api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces | Bearer | Admin | StorageSpaceCreateRequest | StorageSpaceResponse | Single create. |
| POST | /api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces:batch-create | Bearer | Admin | StorageSpaceBatchCreateRequest | StorageSpaceBatchCreateResponse | Add-only range C01–C08 style; transaction semantics explicit. |
| GET | /api/v1/organizations/{organizationId}/storage-spaces/{storageSpaceId} | Bearer | Authorized membership | — | StorageSpaceResponse | Includes current geometry profile/version summary and calculated capacity. |
| PATCH | /api/v1/organizations/{organizationId}/storage-spaces/{storageSpaceId} | Bearer | Admin | StorageSpacePatchRequest | StorageSpaceResponse | Cannot rewrite launched snapshot history. |
| GET | /api/v1/organizations/{organizationId}/clients | Bearer | Authorized membership | CursorQuery + search/status | Page<ClientResponse> | Tenant-scoped. |
| POST | /api/v1/organizations/{organizationId}/clients | Bearer | Admin | ClientCreateRequest | ClientResponse | Third-party owner master. |
| GET | /api/v1/organizations/{organizationId}/clients/{clientId} | Bearer | Authorized membership | — | ClientResponse | — |
| PATCH | /api/v1/organizations/{organizationId}/clients/{clientId} | Bearer | Admin | ClientPatchRequest | ClientResponse | Optimistic concurrency. |
| GET | /api/v1/geometry-families | Bearer | Authenticated | — | List<GeometryFamilyResponse> | Platform-governed F01–F10, read-only. |
| GET | /api/v1/organizations/{organizationId}/geometry-profiles | Bearer | Authorized membership | CursorQuery + search/status | Page<GeometryProfileSummary> | Search Name + Code. |
| POST | /api/v1/organizations/{organizationId}/geometry-profiles | Bearer | Admin | GeometryProfileCreateRequest | GeometryProfileResponse | Creates logical profile + first immutable version. |
| GET | /api/v1/organizations/{organizationId}/geometry-profiles/{profileId} | Bearer | Authorized membership | — | GeometryProfileResponse | Includes currentVersion. |
| PATCH | /api/v1/organizations/{organizationId}/geometry-profiles/{profileId} | Bearer | Admin | GeometryProfilePatchRequest | GeometryProfileResponse | Calculation-affecting change creates new version. |
| GET | /api/v1/organizations/{organizationId}/geometry-profiles/{profileId}/versions | Bearer | Authorized membership | CursorQuery | Page<GeometryProfileVersionResponse> | Immutable versions. |
| GET | /api/v1/commodities | Bearer | Authenticated | CursorQuery + search/status/version | Page<CommodityResponse> | Platform-governed read-only to organization users. |
| GET | /api/v1/commodities/{commodityId} | Bearer | Authenticated | — | CommodityResponse | Includes current version. |
| GET | /api/v1/commodities/{commodityId}/versions/{versionId} | Bearer | Authenticated | — | CommodityVersionResponse | Immutable governed version. |
| GET | /api/v1/organizations/{organizationId}/campaigns | Bearer | Authorized membership | CursorQuery + search/status/type | Page<CampaignSummary> | Search Name/Code; Active/Completed; optional type. |
| POST | /api/v1/organizations/{organizationId}/campaigns:launch | Bearer | Admin | CampaignLaunchRequest | CampaignResponse | Atomic Save & Launch; official Campaign Code server-generated; no Draft. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId} | Bearer | Authorized membership | — | CampaignResponse | Includes snapshot/scope/team summaries. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/scope | Bearer | Authorized membership | CursorQuery | Page<CampaignScopeItemResponse> | Frozen after launch. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/team | Bearer | Authorized membership | CursorQuery | Page<CampaignTeamAssignmentResponse> | History/current filter supported. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/team | Bearer | Admin; Campaign ACTIVE | CampaignTeamAddRequest | CampaignTeamAssignmentResponse | Post-launch team changes allowed; scope unchanged. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/team/{userId}:remove | Bearer | Admin; Campaign ACTIVE | MutationControl | CampaignTeamAssignmentResponse | Immediate server revocation for new mutations; history retained. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/snapshot | Bearer | Authorized membership | — | CampaignSnapshotResponse | Immutable launch snapshot. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-manifest | Bearer | Authorized membership + campaign access | deviceInstallationId/packageVersion? | OfflineManifestResponse | Returns current authorized manifest metadata. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-package | Bearer | Authorized membership + campaign access | deviceInstallationId + manifest/version | Binary/stream package | Campaign-scoped complete authorized package; integrity headers. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking | Bearer | Authorized campaign access | CursorQuery + location/unit/status | Page<StocktakingRecordResponse> | Read progress; mutation rights separate. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId} | Bearer | Authorized campaign access | — | StocktakingRecordResponse | Current revision + contextual issues/evidence summaries. |
| PUT | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}/working-revision | Bearer | Campaign Team; ACTIVE | StocktakingRevisionUpsertRequest | StocktakingRevisionResponse | Synchronized mutation; idempotency + base version. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}:validate | Bearer | Campaign Team; ACTIVE | ValidateStocktakingRequest | StocktakingRevisionResponse | Server revalidates non-empty/measurement/calculation/evidence rules. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}:mark-empty | Bearer | Campaign Team; ACTIVE | MarkEmptyRequest | StocktakingRevisionResponse | Creates Validated empty semantics; does not auto-resolve issues. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}:reset-validation | Bearer | Admin; ACTIVE | ResetValidationRequest | StocktakingRevisionResponse | Creates new working revision; prior validated revision immutable. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}/revisions | Bearer | Authorized campaign access | CursorQuery | Page<StocktakingRevisionResponse> | Revision history. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}/issues | Bearer | Campaign Team; ACTIVE | IssueCreateRequest | IssueResponse | Six frozen categories; description mandatory. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/stocktaking/{recordId}/issues | Bearer | Authorized campaign access | CursorQuery + status | Page<IssueResponse> | Contextual issues; no global issue module. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/issues/{issueId}:resolve | Bearer | Authorized campaign user | IssueResolveRequest | IssueResponse | Explicit reason/audit. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/issues/{issueId}:reopen | Bearer | Authorized campaign user; ACTIVE | IssueReopenRequest | IssueResponse | Not allowed after COMPLETED. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/issues/{issueId}:accept-follow-up | Bearer | Admin; Close context | IssueResolveRequest | IssueResponse | OPEN→RESOLVED with CLOSE_FOLLOW_UP_ACCEPTANCE. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/evidence | Bearer | Campaign Team; ACTIVE | EvidenceMetadataCreateRequest | EvidenceResponse | Creates NOTE or PHOTO metadata; PHOTO then uses binary transfer. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/evidence/{evidenceId}/upload | Bearer | Authorized owner context | multipart/binary JPEG | EvidenceResponse | Controlled binary transfer; SHA-256 verification; hard max 3 MB. |
| DELETE | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/evidence/{evidenceId} | Bearer | Authorized actor; working only | MutationControl | 204 | Forbidden once validated/frozen; audited cleanup semantics. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/evidence/{evidenceId}/content | Bearer | Authorized contextual read | — | Binary JPEG | Authorized signed/streamed content; no public object URL. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/close-readiness | Bearer | Authorized campaign access | — | CloseReadinessResponse | Advisory read; Close endpoint revalidates atomically. |
| POST | /api/v1/organizations/{organizationId}/campaigns/{campaignId}:close | Bearer | Admin; online | CloseCampaignRequest | FinalInventorySnapshotResponse | Atomic guard revalidation + snapshot + COMPLETED. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/final-snapshot | Bearer | Authorized scope | — | FinalInventorySnapshotResponse | Completed immutable historical source. |
| GET | /api/v1/organizations/{organizationId}/reports | Bearer | Authorized scope | ReportCatalogQuery | ReportCatalogResponse | Exactly nine canonical report types; Audit Trail Admin-only. |
| POST | /api/v1/organizations/{organizationId}/reports/{reportType}:dataset | Bearer | Authorized; Audit Admin | ReportDatasetRequest | ReportDatasetResponse | Single Authoritative Report Dataset. |
| POST | /api/v1/organizations/{organizationId}/reports/{reportType}:export | Bearer | Authorized; Audit Admin | ReportExportRequest | Binary PDF/CSV | Export derives from same canonical dataset semantics. |
| POST | /api/v1/organizations/{organizationId}/inventory-history:compare | Bearer | Authorized scope | InventoryHistoryRequest | InventoryHistoryResponse | 2..N completed snapshots; comparability classifications. |
| GET | /api/v1/organizations/{organizationId}/campaigns/{campaignId}/dossier | Bearer | Authorized scope | — | DossierResponse | Completed snapshot/evidence/audit/context dossier. |
| GET | /api/v1/organizations/{organizationId}/audit-events | Bearer | Admin | CursorQuery + filters | Page<AuditEventResponse> | Append-only authoritative audit read. |
| GET | /api/v1/organizations/{organizationId}/users | Bearer | Admin | CursorQuery + search/status/role | Page<UserMembershipResponse> | Organization administration. |
| GET | /api/v1/organizations/{organizationId}/users/{userId} | Bearer | Admin | — | UserMembershipResponse | Historical identity retained. |
| PATCH | /api/v1/organizations/{organizationId}/memberships/{membershipId} | Bearer | Admin | MembershipPatchRequest | MembershipResponse | Role/lifecycle; cannot remove last active Admin. |
| GET | /api/v1/notifications | Bearer | Authenticated | CursorQuery + readState | Page<NotificationResponse> | Own notifications. |
| POST | /api/v1/notifications/{notificationId}:mark-read | Bearer | Recipient | MutationControl | NotificationResponse | Notification only, not audit. |
| POST | /api/v1/notifications:mark-all-read | Bearer | Authenticated | MutationControl | AcceptedResponse | Own notifications. |
| GET | /api/v1/preferences | Bearer | Authenticated | — | UserPreferenceResponse | Synchronized approved preferences. |
| PATCH | /api/v1/preferences | Bearer | Authenticated | UserPreferencePatchRequest | UserPreferenceResponse | Cannot alter system rules. |
| GET | /api/v1/organizations/{organizationId}/configuration | Bearer | Authorized membership | — | OrganizationConfigurationResponse | Standard may read authorized effective config where needed; mutation Admin-only. |
| PATCH | /api/v1/organizations/{organizationId}/configuration | Bearer | Admin | OrganizationConfigurationPatchRequest | OrganizationConfigurationResponse | Approved registry keys/types only. |
| POST | /api/v1/sync/batch | Bearer | Authorized active context | SyncBatchRequest | SyncBatchResponse | Bounded ordered operations; per-op ACK/conflict/error; same idempotency keys. |
| GET | /api/v1/sync/status | Bearer | Authenticated | organizationId/campaignId? | SyncServerStatusResponse | Operational server-side visibility; no force-overwrite controls. |
| GET | /api/v1/sync/pull | Bearer | Authorized active context | checkpoint?, limit? | SyncPullResponse | Additive V1 server-to-device change retrieval; opaque checkpoint; default page 200; server controls maximum. |

## 11. Authentication DTOs

### SignInRequest
`email` required; `password` required. Credentials are never echoed.

### SessionResponse
Contains user identity, server session/access credential, refresh/session-restoration material where issued, expiry metadata, and authorized membership summaries needed to enter the app. Credential representation is transport/security infrastructure, not a business-domain identifier.

### ForgotPasswordRequest / ResetPasswordRequest
Forgot request contains email. Reset request contains the server-issued recovery context/token plus new password fields required by the server policy. Forgot response must not reveal whether an email exists.

## 12. Organization, membership and User API

- Organization read/write fields follow FC-01 exactly; lifecycle is ACTIVE/INACTIVE.
- User fields follow FC-06: Full Name, Email and Active/Disabled lifecycle. Role is a property of OrganizationMembership, never of the global User entity.
- `UserResponse` contains the canonical User fields required by Document 09, including `userId`, `fullName`, `email`, `lifecycleStatus`, `createdAt`, `createdBy` when present by the canonical actor rule, `updatedAt`, `updatedBy`, and User `entityVersion`.
- `MembershipResponse` contains canonical OrganizationMembership fields including `membershipId`, `organizationId`, `userId`, `role`, `lifecycleStatus`, `disabledAt`, `disabledBy`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, and Membership `entityVersion`.
- `UserMembershipResponse` is an explicit composition `{ user: UserResponse, membership: MembershipResponse }`. It is not a flattened record with one ambiguous metadata/version set.
- `CurrentUserResponse` includes the authenticated canonical `UserResponse` plus authorized membership representations/summaries sufficient for organization selection and capability evaluation; any included canonical Membership metadata retains its own provenance/version.
- A client may persist a canonical `User` entity only from canonical User metadata. It must never fabricate User `createdAt/createdBy/updatedAt/updatedBy/entityVersion` from Membership metadata. If only a display projection is returned/needed, it remains a projection and is not promoted to a canonical User entity.
- Membership has its own ACTIVE/DISABLED lifecycle and Admin/Standard role.
- Role changes are audited and versioned. Historical snapshot/audit role meaning is not rewritten.
- Server rejects disabling/demoting the last ACTIVE Admin.
- Organization creation is restricted to platform provisioning/onboarding authority.

## 13. Master-data API

Location, Storage Unit, Storage Space and Client requests/responses follow FC-02–FC-05, including code normalization/uniqueness, field limits and lifecycle. `LocationResponse`, `LocationCreateRequest`, and `LocationPatchRequest` additionally expose optional `latitude` and `longitude` as one authoritative facility-coordinate pair. Both are supplied together or both absent/null; latitude is -90..90 and longitude -180..180. PATCH may atomically set/move or clear the pair and requires normal `baseEntityVersion`/idempotency controls. Admin may mutate; Standard reads authorized data. Inactive historical records remain resolvable but are excluded from new selections where the frozen rules require Active values.

No V6 endpoint exposes device/user live location, GPS tracking, background location, or turn-by-turn routing. Map SDK/provider selection and basemap tile transport are not business API contracts and must not receive SiloMath access tokens or authoritative business payloads.

Storage Space batch creation is Add-only. A batch request declares the intended range/pattern and common approved fields; the server validates all generated codes and returns per-created identities only if the batch contract succeeds according to its atomicity. It must not silently overwrite existing spaces.

## 14. Geometry and Commodity API

- Geometry Families F01–F10 are platform-governed read-only reference resources.
- Geometry Profile is Organization-scoped and Admin-managed. Calculation-affecting edits create a new immutable GeometryProfileVersion; they never rewrite an existing version.
- Geometry Profile responses expose exact current version identity, family, approved parameters, reference datum/provenance and calculated Maximum Capacity.
- Exact F01–F10 parameter schema is versioned and must match the Calculation Engine contract; no client/server invention.
- Commodities are platform-governed; organization users read Commodity/CommodityVersion but cannot create/edit definitions.
- Stocktaking non-empty state pins exact `commodityVersionId` and `geometryProfileVersionId` required for reproducibility.

## 15. Campaign API

### 15.1 Save & Launch

`POST .../campaigns:launch` is Admin-only and atomic. Request contains Campaign Name, Inventory Type (`ANNUAL|MONTHLY|CONTROL`), Start Date, Planned End Date, exact Storage Space scope, and initial Team assignments. Campaign Code is not client-generated. Server validates dates, scope, at least one Team member, authorization and active/selectable dependencies; allocates `INV-YYYY-NNNN`; creates ACTIVE Campaign, frozen Scope and immutable CampaignSnapshot in one logical transaction.

Failure creates no partially launched Campaign. There is no Draft endpoint/state.

### 15.2 Scope and Team

Campaign Scope is read-only after launch. Team is separately mutable by Admin while Campaign is ACTIVE. Add/remove is audited; removed assignment history remains. Launch snapshot preserves launch-time team and is not rewritten by later team changes.

## 16. Offline Working Set API

Offline package is Campaign×DeviceInstallation scoped. Manifest identifies campaign, organization, device installation, packageVersion, required content counts/versions/hashes, compatibility metadata, generation time and integrity verification data.

Package content includes the complete authorized campaign header/status/snapshot/scope, hierarchy, pinned geometry/profile/schema/Commodity/calculation compatibility data, Team authorization, existing Stocktaking data, Ownership/Client data, required Evidence metadata/content references, sync metadata and integrity manifest. It must not download arbitrary whole-Organization data.

`OFFLINE_READY` is never returned as a Campaign status. Android derives readiness only after current authorized package download completes and integrity verification succeeds.

Authorization/package changes may invalidate/refresh a manifest without erasing unsynchronized local work.

## 17. Stocktaking API

### 17.1 Stable record and revisions

One StocktakingRecord exists per Campaign×StorageSpace. The root exposes `currentRevisionId`; revision history is separate. Working changes operate on the current working revision. Reset Validation creates a new working revision based on the prior validated revision; prior validated history remains immutable.

### 17.2 Working revision payload

For a non-empty revision the payload can contain 1–5 Measurements, exact GeometryProfileVersion, CommodityVersion, Actual Bulk Density, Ownership (`OWNED|THIRD_PARTY`), conditional Client, Notes/Evidence references and calculation metadata. Measurements include sequence, raw value/unit, normalized ullage, datum metadata, geometry version, capture actor/time and entity version where synchronized.

Ownership is mandatory for non-empty; Client is mandatory only for THIRD_PARTY and must be absent/not required for OWNED according to the frozen rule.

### 17.3 CalculationResult

API transmits the frozen Calculation Engine metadata: `surfaceModelVersion=LEVEL_BASED_V1`, aggregation method/version, effective ullage/fill height, product volume, actual bulk density, physical quantity, geometry family/profile version, engine/formula version, input fingerprint, rounding policy and calculated actor/source/time. Official persisted outputs use NUMERIC(20,6)-equivalent scale-6 semantics with ROUND_HALF_UP only at the governed persistence/presentation boundary; intermediate arithmetic is not rounded. Aggregation is MEAN_ULLAGE_V1.

### 17.4 Validate

Validate is a command, not a generic status patch. Server revalidates Team authorization, ACTIVE Campaign, current revision/version, 1–5 compatible readings, required non-empty fields, exact version/datum compatibility, calculation fingerprint and Evidence rules. Success freezes the validated revision. It does not auto-resolve an Open Issue.

### 17.5 Mark Empty

Mark Empty is a command. Success yields a VALIDATED revision with `isEmpty=true`, Product Volume=0 and Physical Quantity=0. Measurements, Commodity, density, Ownership and Client are not fabricated/required. Permitted Notes/Photos may remain. Open Issues are not auto-resolved.

## 18. Issue API

Issue categories are exactly: `MEASUREMENT_PROBLEM`, `ACCESS_SAFETY`, `GEOMETRY_DISCREPANCY`, `COMMODITY_UNCERTAINTY`, `STORAGE_SPACE_CONDITION`, `OTHER`. Description is mandatory.

Issue current status is `OPEN|RESOLVED`. Resolution requires explicit reason/actor/time and appends IssueResolution + IssueStatusEvent/audit. A RESOLVED Issue may be reopened only while Campaign is ACTIVE. Close-time follow-up acceptance is Admin-only and is an explicit OPEN→RESOLVED transition with `CLOSE_FOLLOW_UP_ACCEPTANCE`, never a third status.

## 19. Evidence API

- Evidence type: `PHOTO|NOTE`; exactly one principal owner: StocktakingRecord or Issue.
- PHOTO maximum: five per StocktakingRevision and five per Issue. NOTE does not consume photo limit.
- PHOTO is normalized JPEG, EXIF orientation corrected, long edge ≤2048 px, target ≤1.5 MB, hard maximum 3 MB, SHA-256 required.
- Metadata creation and binary upload are separate controlled operations. Binary is never embedded as ordinary business JSON.
- Server verifies declared hash and authorized owner/revision binding.
- Unnecessary EXIF is stripped; GPS/device identifiers are not automatically collected.
- Working Evidence may be removed by authorized actor before validation. Validated/frozen Evidence is immutable.
- Binary reads require contextual authorization; object-store internals/public URLs are not exposed as durable business references.

## 20. Sync API

`GET /sync/pull` retrieves authoritative server-to-device changes after an optional server-issued opaque checkpoint. The client never interprets checkpoint internals. Default page size is 200 changes and the server controls the supported maximum. Each change carries entityType, entityId, authoritative entityVersion, changeType, serverChangedAt, authoritative representation/reference and ordering/checkpoint metadata. The response returns an opaque nextCheckpoint for subsequent pulls.

`POST /sync/batch` accepts a bounded ordered list of SyncOperation DTOs. Each operation includes operation ID, idempotency key, organization/campaign context, entity type/id, operation type, base entity version, canonical mutation payload, dependency IDs and client/device metadata required by the sync contract.

The response is per-operation and never hides partial batch outcomes:

```json
{"results":[{"syncOperationId":"uuid","outcome":"ACKNOWLEDGED|CONFLICT|RETRYABLE_ERROR|PERMANENT_ERROR","entityVersion":12,"error":null,"conflict":null}],"serverTimeUtc":"...","correlationId":"..."}
```

- ACKNOWLEDGED means the server business effect is durably accepted.
- CONFLICT preserves local attempted value plus server authoritative value/version sufficient for controlled resolution.
- Retryable failure does not change logical operation/idempotency identity.
- Dependency failure prevents dependent apply; critical operations are not silently coalesced.
- Resolution types remain exactly `RELOAD_AND_REENTER`, `DISCARD_LOCAL_ATTEMPT`, `NO_ACTION_REQUIRED`; no force overwrite/auto merge.

## 21. Close API

`POST .../campaigns/{campaignId}:close` is Admin-only, online and atomic. Request carries idempotency/correlation/base campaign version plus any explicit approved close context. The server revalidates all guards in the transaction: Campaign ACTIVE; every scoped Storage Space final/Validated; no To Do; mandatory data valid; critical operations synchronized; no Open Issues; authorization/integrity pass.

Success creates exactly one FinalInventorySnapshot and exactly one entry for every scoped Storage Space, transitions Campaign to COMPLETED and records close audit context. Failure leaves Campaign ACTIVE and creates no partial final snapshot.

Final snapshot responses freeze hierarchy, geometry/version, Commodity, Ownership/Client, Empty state, Measurements, density/operational values, volume, quantity, calculation metadata, validation actor/time, Issue/Evidence references and close context. Completed history is never recalculated from current master data.

## 22. Reporting, History and Dossier API

Exactly nine report types are exposed:
- `PHYSICAL_INVENTORY`
- `INVENTORY_BY_LOCATION`
- `INVENTORY_BY_COMMODITY`
- `INVENTORY_BY_OWNERSHIP`
- `STORAGE_SPACE_WORKSHEET`
- `CAPACITY_REPORT`
- `INVENTORY_HISTORY`
- `DIGITAL_INVENTORY_DOSSIER`
- `AUDIT_TRAIL`

Admin and Standard may access authorized reports; `AUDIT_TRAIL` is Admin-only. Completed official report datasets use FinalInventorySnapshot. Active-campaign monitoring must be clearly non-final.

The dataset endpoint and PDF/CSV export endpoint use the **same Single Authoritative Report Dataset semantics**. Export code may format but must not independently recalculate business totals/history.

Inventory History accepts 2..N Completed Campaign IDs and supported dimensions: Close Date, Campaign, Commodity, Location, Ownership and Physical Quantity. It returns absolute/percentage change and classifications `COMPARABLE`, `SCOPE_ADDED`, `SCOPE_REMOVED`, `CONTEXT_CHANGED`; no forecasting.

Digital Inventory Dossier reads the completed snapshot plus approved Evidence/audit/context references. Evidence binary retrieval remains separately authorized.

## 23. Audit API

Audit read is Admin-only. AuditEvent response includes stable event ID, organization, actor (nullable only for SYSTEM with system actor metadata), action, entity type/id, timestamp UTC, optional campaign, safe structured before/after/change metadata, correlationId and source.

Audit API is read-only to organization users. No ordinary endpoint updates/deletes AuditEvent. Local pending audit facts, if surfaced by Android, must be distinguishable from server-confirmed audit.

## 24. Notifications API

Canonical notification types: `SYNC_ERROR`, `SYNC_CONFLICT`, `CAMPAIGN_ASSIGNMENT`, `ISSUE_REQUIRES_ATTENTION`, `CAMPAIGN_COMPLETED`. Response contains unread/read state, timestamp, title/message and optional contextual navigation reference. Deep-link destination rechecks authentication/authorization. Notification is never source of truth or substitute for Audit.

## 25. Preferences and Organization Configuration API

User preferences are limited to approved representation/notification preferences such as quantity display, density representation where applicable, date/time format, Theme (`SYSTEM|LIGHT|DARK`), non-critical notification preferences and optional default report format. Mandatory critical notification classes cannot be disabled if classified mandatory.

Organization Configuration is Admin-mutable only for approved registry keys and typed values. Absence is valid/default behavior. API must reject attempts to configure formulas, engine/aggregation, max measurements, canonical units, Commodity definitions, workflow statuses, Ownership/Client rules, Close Guards, snapshot behavior, sync conflict strategy, evidence hashing, retention, authentication rules, platform limits, server URL, sync interval, disable-sync or force-overwrite.

## 26. Cache and conditional request behavior

- Immutable Geometry/Commodity versions, CampaignSnapshot and FinalSnapshot may use long-lived conditional caching keyed by immutable identity/version, subject to authorization.
- Mutable resources must not be treated as authoritative from HTTP cache after mutation; Room reconciliation/entityVersion governs app state.
- Sensitive authenticated responses must use server cache headers appropriate to privacy; shared/public caching of tenant data is prohibited.

## 27. Retry classification

| Class | Client behavior |
|---|---|
| Network timeout/connection/503/selected 5xx | Retry durable eligible operation with backoff and same idempotency key. |
| 429 | Honor Retry-After/backoff; same logical operation. |
| 401 | Attempt supported secure refresh once; otherwise require Sign In. Do not delete unsynced work. |
| 403 | Do not blind retry; refresh authorization/context where appropriate. |
| 409 entity/sync conflict | Persist explicit conflict and route to controlled resolution/reload. |
| 422 permanent validation | Do not blind retry unchanged payload; surface typed actionable failure. |
| Evidence transfer interruption | Resume/retry according to Evidence/Sync contract without duplicating Evidence identity. |

## 28. Security and privacy requirements at API boundary

- TLS only in Production; environment/base URL is build-controlled, not user-editable.
- Server authorizes every protected resource/mutation; organization IDs from client are untrusted input.
- No token/password/secret in URL, logs, error details, analytics or audit before/after payloads.
- Correlation IDs are diagnostic only and carry no privilege.
- Error behavior must avoid cross-tenant information leakage.
- Evidence download/upload requires contextual authorization; no durable public object URL.
- API does not collect device hardware identifiers or automatic GPS Evidence metadata.
- Sensitive payload logging is sanitized; full raw business payloads are not default production logs.

## 28.1 Retention-visible API behavior

- Final Inventory Snapshot, Campaign Snapshot, critical AuditEvent, official validated/final Evidence metadata and Issue/resolution history remain API-resolvable for at least **10 years** according to authorization and archival availability.
- Notifications are retained server-side for **12 months**; expired notifications are not a historical source of truth.
- ACKNOWLEDGED/synced diagnostic SyncOperation records may be purged/archived after the approved **90 days** online diagnostic period; unresolved failures/conflicts are not auto-deleted.
- Retention cleanup must not cause completed report/history APIs to depend on mutable current master data.

## 29. Compatibility and evolution

- `/api/v1` changes must remain backward compatible for supported client rollout unless a new major API version is introduced.
- Additive response fields are allowed; removing/renaming fields, changing enum meaning, changing requiredness, changing numeric semantics or reinterpreting status codes is breaking.
- New enum values that change client behavior require coordinated compatibility handling; clients must not silently map unknown workflow values to an existing value.
- Immutable historical DTOs retain enough schema/engine/version metadata for long-term interpretation.
- Server/database migrations and API rollout follow forward-safe compatibility; API must tolerate the approved overlap window between client versions.

## 30. Endpoint-level acceptance criteria

- **API-AC01** All protected endpoints perform server-side authorization; UI role visibility is never trusted as authority.
- **API-AC02** Every tenant resource is organization-scoped and cross-tenant references are rejected.
- **API-AC03** Campaign launch is Admin-only, atomic, no Draft, and server-generates the official Campaign Code.
- **API-AC04** Campaign Scope has no post-launch mutation endpoint; Team remains Admin-mutable only while ACTIVE.
- **API-AC05** Stocktaking mutations require Campaign Team membership for both Admin and Standard.
- **API-AC06** Validate and Mark Empty are explicit commands and never auto-resolve Issues.
- **API-AC07** Reset Validation is Admin-only and creates a new working revision without rewriting validated history.
- **API-AC08** Geometry and Commodity historical versions are addressable and immutable.
- **API-AC09** Evidence binary transfer is separate from JSON and enforces hash/size/authorization/frozen-state rules.
- **API-AC10** Offline package is campaign-scoped, device-installation-aware, complete/integrity-verifiable and never defines OFFLINE_READY as Campaign status.
- **API-AC11** Retryable synchronized mutations preserve idempotency key and base entity version.
- **API-AC12** Sync batch preserves per-operation outcome, dependencies and explicit conflict data; no last-write-wins/force overwrite.
- **API-AC13** Close is Admin-only online atomic and creates no partial Final Snapshot.
- **API-AC14** Completed reports/history read FinalInventorySnapshot and never recalculate current master data.
- **API-AC15** Exactly nine report types exist; Audit Trail is Admin-only.
- **API-AC16** PDF/CSV exports use the same authoritative dataset semantics as UI.
- **API-AC17** Errors expose stable code, safe message, field errors, correlationId and retryable classification.
- **API-AC18** Large server collections support opaque cursor pagination without imposing a fixed UI page size.
- **API-AC19** UTC/date-only/decimal serialization preserves frozen time and numerical semantics.
- **API-AC20** Disabled Users/memberships and removed Team assignments cannot authorize new operations while historical identity remains readable.
- **API-AC21** Organization Configuration cannot mutate System-Controlled Rules.
- **API-AC22** Sign Out/session expiry never instructs deletion of durable unsynchronized local business data.
- **API-AC23** Audit write is not exposed to ordinary organization clients; authoritative audit remains append-only.
- **API-AC24** API DTOs do not expose SQL/Room entities as the domain contract.
- **API-AC25** API contract contains no endpoint that edits Geometry during Stocktaking, modifies Completed Campaigns, modifies frozen Scope, or force-overwrites a sync conflict.

## 31. Traceability matrix

| Concern | Frozen source | API surface |
|---|---|---|
| Authentication/session | D25/D26, FD-15; BR-AUTH-001/002 | /auth/*, /me |
| Organization/master data | FC-01–FC-06; BR-AUTHZ-001, BR-MD-* | /organizations/*, /locations, /storage-units, /storage-spaces, /clients, /users |
| Campaign launch | D01–D08; BR-CAM-*; FD-43/44 | POST /campaigns:launch |
| Offline package | D29; BR-OFF-*; FD-45 | /offline-manifest, /offline-package |
| Stocktaking/calculation | D09–D14, D31–D33; BR-STK/OWN/CALC | /stocktaking/* commands |
| Issues | D16–D18; BR-ISS-* | /issues create/resolve/reopen/accept-follow-up |
| Evidence | D15; BR-EVD-*; FD-46; DM-D35–39 | /evidence metadata/upload/content/delete |
| Sync/conflict | D30; BR-SYNC-*; IC-11; DM-D40–44; OS-D18–OS-D24 | POST /sync/batch; GET /sync/status; GET /sync/pull |
| Close/snapshot | D18–D20; BR-CLS/SNP; FD-43 | close-readiness, :close, final-snapshot |
| Reports/history | D21–D23; BR-RPT/HIS; FD-47 | /reports, :dataset, :export, inventory-history, dossier |
| Audit | IC-03; BR-AUD; DM-D50/51 | /audit-events |
| Notifications/settings | D27/D28; BR-NOT/SET | /notifications, /preferences, /configuration |

## 32. Cross-document handoff

- Document 12 may define batch sizing, scheduling, pull/delta strategy, retry timing and cleanup in more detail, but cannot alter idempotency, explicit dependencies, conflict outcomes, authorization or durable operation identity defined here.
- Document 13 defines exact F01–F10 parameter/formula schemas consumed by GeometryProfileVersion/CalculationResult DTOs without changing API reproducibility/version semantics.
- Document 15 refines object-storage/upload lifecycle while preserving separate binary transfer, hash verification, privacy and immutable historical Evidence.
- Document 16 defines exact fields/layout/filters for each of the nine report datasets/exports while preserving report inventory, permissions, snapshot source and Single Authoritative Report Dataset.
- Documents 17–20 may harden security/testing/retention/operations without weakening this contract.

## 33. Freeze statement

This document is the canonical APPROVED / FROZEN SiloMath V6 API contract after the 2026-10-17 implementation-closure refreeze. Implementation may refine non-semantic route plumbing, transport optimization, map provider plumbing, concrete environment host/base-URL provisioning, and deployment mechanics without Product/Baseline approval only if endpoint semantics, authorization, canonical User/Membership metadata provenance, DTO meaning, idempotency/concurrency, error behavior, historical immutability, offline/sync integrity, Evidence separation, Close atomicity and report source rules remain unchanged.

---
**End of Document — 11_API_SPECIFICATION**
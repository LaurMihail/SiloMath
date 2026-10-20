# SiloMath V6 - 17_SECURITY_AUDIT_PRIVACY

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-20 — CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze

## 1. Purpose and normative scope

This document is the normative SiloMath V6 security, audit and privacy contract. It consolidates the frozen product, authorization, API, offline/sync, evidence, reporting and retention constraints with the explicitly approved SEC-D01-SEC-D149 decision package.

It defines trust boundaries, authentication/session controls, transport and local-data protection, backend/object-storage controls, auditability, privacy/data minimization, logging/diagnostics, abuse controls, encryption/key management, backup/environment separation and security failure behavior.

## 2. Security objectives
- Preserve confidentiality through tenant isolation, least privilege, protected credentials and private storage.
- Preserve integrity through server-side authorization, entity/version validation, immutable history, hashes and fail-closed controls.
- Preserve availability without weakening authorization: supported offline work remains possible inside a verified authorized package.
- Preserve accountability through append-only audit history and safe operational telemetry.
- Minimize personal and device data while preserving legally/operationally required historical attribution.

## 3. Security architecture and trust boundaries
- Defense in depth, least privilege and deny-by-default are the V6 security model.
- Canonical trust boundaries are Android app<->Backend API, Backend<->PostgreSQL, Backend<->private object storage, and Android app<->Android secure/app-private storage.
- The Android client is untrusted for authoritative enforcement; every protected server operation is re-authorized server-side.
- Organization isolation is mandatory for all tenant-owned resources; possession of an identifier from another Organization grants no access.
- Effective authorization is authenticated identity  AND  Active User  AND  active OrganizationMembership  AND  role  AND  authorized scope  AND  Campaign/Team rules  AND  workflow/entity state.
- UI hiding/disabled controls are UX controls, not security boundaries.
- UUIDs, Campaign codes, object-storage references and local references are not credentials.
- Unauthorized cross-tenant requests must not disclose protected resource data.

## 4. Authentication
- V6 authentication is Email + Password through the backend/configured authentication service.
- V6 has no Remember Me, public Sign Up, Google SSO or Microsoft SSO.
- Android never persists the password in Room, DataStore, logs, analytics, AuditEvent or diagnostics.
- Invalid Sign In uses a generic response that does not confirm account existence.
- Forgot Password uses a generic confirmation to reduce account enumeration.
- Password reset is online-only and requires valid recovery context/token.
- Successful password reset invalidates all active refresh/session credentials for that User.
- A Disabled User cannot initiate a new authentication.
- MFA is not a V6 requirement; adding it requires an explicit contract change.
- V6 does not mandate a particular backend password-hash implementation, but plaintext or reversible password storage is forbidden.

## 5. Session and credential lifecycle
- Access credentials are short-lived; refresh/session credentials are longer-lived and revocable.
- Access-token lifetime is 15 minutes; refresh/session maximum lifetime is 30 days.
- Refresh credentials rotate on successful refresh; a confirmed previous credential is not reusable.
- Sign Out invalidates the current server-side refresh/session credential and removes local active authentication credentials.
- Access/refresh credentials and protection keys are stored only through Android Keystore-backed secure storage.
- Tokens never enter Room, business outbox payloads, logs, analytics, crash reports, AuditEvent or diagnostic packages.
- App restart may restore a session only through protected credential/session material; this is not a user-selectable Remember Me feature.
- 401 permits one secure refresh attempt and retry of the same operation; refresh failure returns the user to Sign In.
- First-time Sign In is not possible offline.
- Continue Offline requires a previously authenticated local identity and authorized verified offline context.
- Expired/revoked online credentials do not become permanent offline authority.
- When disablement/revocation becomes known, incompatible new operations are blocked; recoverable unsynced work is preserved rather than arbitrarily deleted.

| Control | V6 value |
|---|---|
| Access-token maximum lifetime | 15 minutes |
| Refresh/session maximum lifetime | 30 days |
| Refresh behavior | Rotate on successful refresh |
| 401 behavior | One secure refresh attempt, then Sign In on failure |
| Remember Me | Not present |
| First-time offline Sign In | Not allowed |

## 6. Transport and API security
- Production API and Evidence transfer use HTTPS/TLS only.
- Production Android Network Security Configuration disables cleartext HTTP.
- Minimum transport is TLS 1.2; TLS 1.3 is preferred where negotiated.
- Production uses normal certificate-chain and hostname validation; trust-all/bypass logic is forbidden.
- Certificate pinning is not mandatory in V6 and requires a separate ADR with rotation/recovery strategy if introduced.
- Authentication and authorization are evaluated for every protected API request; client-supplied organizationId/userId is not proof of authority.
- API errors never expose stack traces, SQL, secrets, token values or internal object-storage credentials.
- Idempotency keys, entityVersion and correlation IDs support integrity/traceability but are not authentication credentials.

## 7. Android local-data security
- Offline business data uses the Android application sandbox and app-private storage.
- PHOTO Evidence remains app-private and is not automatically copied to public gallery/shared storage.
- Room contains neither password/token secrets nor PHOTO BLOBs.
- Production Room/offline business data is encrypted at rest; key material is protected through Android Keystore.
- Database-encryption keys are not hardcoded, committed, embedded as plaintext configuration or synchronized as business data.
- Platform backup/restore excludes auth credentials, database keys and sensitive offline Campaign/Evidence stores.
- Secrets are not copied/shared through clipboard/export/share workflows; business exports use approved report flows only.
- Root detection/device attestation is not a V6 authorization boundary.

### 7.1 Encryption-at-rest hierarchy

| Asset | Required V6 protection |
|---|---|
| Room/offline business database | Application/database encryption in Production; random per-installation secret protected through Android Keystore. |
| Local PHOTO Evidence | App-private plus application-level encryption in Production using Keystore-protected key material. |
| Authentication credentials | Android Keystore-backed secure storage; never Room/DataStore business storage. |
| Android backup | Protected stores, auth secrets and encryption keys excluded. |

## 8. Backend, PostgreSQL and object storage
- Android never connects directly to PostgreSQL; business access goes through the Backend API.
- Database and object-storage credentials are environment secrets and are never included in the Android APK.
- Production database transport is encrypted across trust boundaries; public unauthenticated database exposure is forbidden.
- Evidence object storage is private; remoteObjectRef is opaque and is not a public URL or bearer credential.
- Evidence upload/download authorization is enforced through backend Organization/Campaign/owner context.
- Storage lifecycle/cleanup must not delete retained historical Evidence or create dangling references.
- Production secrets are supplied by environment/secret-management facilities, not committed configuration.
- Development, Staging and Production use separate credentials/secrets; Production credentials are not reused in lower environments.

## 9. Input validation and integrity
- Server validates client-controlled IDs, enums, lengths, numeric constraints, entityVersion and workflow transitions.
- SQL access uses parameterized/prepared queries or safe ORM binding; client input is never concatenated into executable SQL.
- Client text is treated as data, not executable markup/code; render/export layers encode/escape for their format.
- PHOTO becomes official Evidence only after canonical JPEG processing and MIME/size/SHA-256 verification defined by Doc15.
- Offline/sync payloads cannot bypass immutability, history or authorization rules.
- Conflict resolution provides no force-overwrite and cannot turn stale local state into authoritative state.

## 10. Authorization enforcement
- Authorization is evaluated from identity, User lifecycle, OrganizationMembership, role, scope, Campaign/Team rules and workflow state.
- Admin does not automatically receive Stocktaking mutation permission; Campaign Team membership remains required.
- Standard never receives Admin-only administration or Audit Trail access.
- Offline package possession does not supersede later-known revocation.
- Protected remote operations are re-authorized at the server even when the Android UI previously allowed the action.
- Resource identifiers are never treated as capabilities.

## 11. Audit architecture
- Only security-relevant events with accountability/detection value are audited; routine technical requests do not become AuditEvents.
- Successful Sign In generates a security AuditEvent without storing password/token material.
- Failed Sign In is recorded in security telemetry for abuse detection; a business AuditEvent is not mandatory for every wrong password.
- Sign Out, completed password reset, User enable/disable, Membership disable/reactivate and role change are auditable.
- Sensitive authorization denials may generate security telemetry/event; routine UI denials do not pollute Audit Trail.
- Campaign Team changes, Reset Validation, Issue resolution/reopen, Campaign Close, master-data lifecycle changes and report/export events remain auditable under their frozen contracts.
- AuditEvent preserves stable actor identity and frozen actor display information where history requires it.
- actorUserId may be null only for SYSTEM; system/source context must identify the actor context.
- Audit records never contain passwords, access/refresh/reset tokens, Authorization headers, DB/object-store credentials or cryptographic keys.
- PHOTO bytes and other binary payloads never enter AuditEvent; only approved identifiers/metadata may appear.
- Before/after values are limited to relevant fields and redact secret/sensitive fields.
- AuditEvent is append-only; arbitrary correction/deletion of historical events is forbidden.
- Server-accepted actions use server-authoritative UTC occurredAt; offline event context may preserve client occurrence separately without falsifying server acceptance time.
- Correlation IDs may connect requests, AuditEvent and telemetry but contain no PII and are not credentials.

### 11.1 Audit versus telemetry

| Concern | Audit Trail | Security/operations telemetry |
|---|---|---|
| Purpose | Accountability and immutable business/security history | Detection, diagnostics and operations |
| Retention | Critical events follow long-term frozen retention | Operational retention according to Doc19/Doc20 |
| Payload | Structured approved event metadata; no secrets/binaries | Sanitized technical signals; no secrets/full business payloads |
| Failed Sign In | Not necessarily one business event per failure | Captured for abuse detection |
| Routine sync retry noise | Not fabricated as AuditEvent | May be represented as aggregate/sanitized telemetry |

## 12. Privacy and data minimization
- V6 applies data minimization: collect only data necessary for authentication, authorization, inventory workflow, auditability, reporting and support/operations.
- Principal personal data includes Full Name, Email, OrganizationMembership/Role, Campaign Team association and actor attribution in workflow/audit history.
- V6 does not require date of birth, sex, personal address, personal phone number, biometric data, or user/device precise location. Optional facility latitude/longitude belongs to Location master data and is not a user/device tracking signal.
- Android GPS/location permission is not required for V6 Evidence or the approved facility map and is not collected automatically.
- DeviceInstallation uses an app-generated installation UUID; IMEI, hardware serial and advertising ID are excluded.
- PHOTO normalization removes unnecessary EXIF; GPS/device model/serial metadata does not become business Evidence metadata.
- Original filesystem paths are not stored as business metadata.
- Disabling a User does not erase historical actor attribution; stable identity and frozen display context remain.
- Profile data no longer operationally required may follow retention/deletion policy, but retained immutable business/audit attribution is preserved.
- Tenant data is not used for cross-tenant profiling or advertising in V6.
- Operational analytics, if introduced, never receive passwords, tokens, Evidence binaries, arbitrary Notes, full reports or unrestricted business payloads.
- Crash/diagnostic collection is minimized and redacted before external telemetry.
- Privacy-sensitive content is not automatically copied to clipboard, public storage or external share targets.
- Authorized PDF/CSV exports may include necessary business/personal attribution; creation remains explicit, authorized and audited.
- V6 has no self-service deletion that destroys immutable corporate inventory/audit history; such requests follow organization/legal retention processes.
- Final retention/deletion is governed by Doc19 and must not create dangling references or falsify retained history.

### 12.1 Personal/device data inventory

| Data | V6 treatment |
|---|---|
| Full Name / Email | Required identity/account data; authorization and attribution. |
| Role / Membership / Campaign Team | Required authorization context. |
| Historical actor attribution | Retained for every immutable business/audit record whose frozen contract carries actor attribution. |
| User/device precise GPS | Not required, not automatically collected, and no location permission is requested for the approved facility map. |
| Facility coordinates | Optional Organization-scoped Location master data; protected by normal tenant authorization, audit/versioning and retention. |
| IMEI / hardware serial / advertising ID | Not collected for DeviceInstallation. |
| DeviceInstallation ID | App-generated installation UUID. |
| PHOTO EXIF | Unnecessary EXIF stripped; GPS/device identifiers excluded. |
| Original PHOTO filesystem path | Not business metadata. |

## 13. Logging, diagnostics and observability
- Production uses structured logging with explicit redaction.
- Logs never contain passwords, tokens, Authorization/Cookie headers, cryptographic keys or secret configuration.
- Logs never contain PHOTO bytes/base64, complete Evidence binaries or database dumps.
- Arbitrary Note text and full request/response business payloads are not logged by default.
- Safe context may include timestamp, environment, component, severity, correlationId, endpoint/operation ID, sanitized error code, duration and non-secret technical IDs.
- Email/full user display data is not logged by default when stable internal userId is sufficient.
- Protected server telemetry may contain stack traces for unexpected internal errors; API clients never receive them.
- Production Android builds do not leave verbose/debug payload logging enabled for sensitive workflows.
- Diagnostic packages are explicit and exclude auth secrets, database-encryption keys, raw credential stores and PHOTO binaries.
- Diagnostic packages may include app version, environment identifier, non-persistent compatibility metadata, sanitized sync state/counts, error codes and correlation references.
- Diagnostic export supports review/redaction before support transfer when business identifiers may be present.
- SyncOperation/SyncConflict/Audit before-after payloads are not copied wholesale into generic production logs.
- Security telemetry and business Audit Trail are separate: telemetry serves detection/operations; Audit Trail serves accountability/history.
- Observability must detect authentication abuse, abnormal authorization denials, sync-failure spikes, report-integrity errors, repeated Evidence upload/hash failures and unexpected server errors.

### 13.1 Forbidden production log/diagnostic content
- Passwords and reset/access/refresh tokens
- Authorization/Cookie headers
- Cryptographic/database/object-storage credentials and keys
- PHOTO bytes/base64 and database dumps
- Arbitrary Note text by default
- Full request/response business payloads by default
- Full SyncConflict/SyncOperation/Audit before-after envelopes

## 14. Abuse protection and rate limiting
- Authentication endpoints are protected by server-side rate limiting/throttling.
- Sign In allows at most 5 failed attempts per 15 minutes per normalized account identifier before temporary throttling.
- Additional deployment-configurable source/network protection mitigates distributed credential guessing and is not used alone for permanent account lock.
- There is no permanent automatic account lock based only on failed password attempts.
- Successful authentication may reset account-level failed-attempt counters according to backend policy.
- Forgot/Reset Password endpoints are separately rate-limited by account identifier and abuse source.
- Forgot Password accepts at most 3 reset requests per 60 minutes per normalized account identifier while keeping the response generic.
- Reset tokens are single-use and valid for 30 minutes.
- Consumed, superseded or expired reset tokens cannot be reused.
- Authenticated API rate limits may be deployment-configurable but must not break legitimate sync batches or report exports.
- HTTP 429 uses Retry-After when available; the frozen sync engine respects it.
- Rate limiting is not authorization; every request remains subject to normal authentication/authorization.
- Idempotent retry of the same SyncOperation is not automatically treated as a malicious duplicate mutation.
- Evidence upload controls may limit size/concurrency/rate without changing the frozen 3 MB PHOTO hard maximum or Evidence semantics.
- Report/export controls may limit concurrency/rate without producing partial or semantically different datasets.
- Repeated abuse-limit activation is security telemetry and does not automatically create a business AuditEvent for every rejection.

| Endpoint/control | V6 policy |
|---|---|
| Sign In | 5 failed attempts / 15 minutes / normalized account identifier -> temporary throttling. |
| Forgot Password | Max 3 accepted reset requests / 60 minutes / normalized account identifier; generic response. |
| Reset token | Single-use; 30-minute validity. |
| Permanent password-failure lock | Not used. |
| 429 | Retry-After honored where supplied. |
| General API | Deployment-configurable protective limits; must not break legitimate sync/export semantics. |

## 15. Encryption and key management
- Room encryption uses a random per-installation database secret that is not derived from the user's password.
- The database secret is wrapped/protected by Android Keystore-backed key material.
- Keystore/database keys are not synchronized to backend and are excluded from Android backup.
- If secure key material becomes permanently unavailable/corrupt, encryption is not bypassed; recovery/re-authentication/re-download is used while preserving recoverable unsynced data where technically possible.
- Local PHOTO Evidence is encrypted at application level in Production using key material protected by Android Keystore, in addition to app-private sandboxing.
- Remote Evidence uses object-storage/platform encryption at rest.
- Production PostgreSQL storage and backups use infrastructure-level encryption at rest.
- Backup encryption keys and Production data-encryption credentials are managed through Production secret/KMS facilities, not source control.

## 16. Backup, restore and environment separation
- Production database backups are encrypted and limited to authorized operators/service identities.
- Backup restore is privileged, operationally audited and periodically tested; RPO/RTO are governed by Doc20.
- Android platform/cloud backup excludes auth secrets, encryption keys and protected offline business/Evidence stores.
- Development/Staging do not use Production snapshots with real business/personal data except through an explicitly authorized and sanitized process.
- Production API base URL and non-secret environment identifiers may be build configuration; credentials/secrets are never plaintext build constants.
- Debug and Production builds are separated; debug trust overrides/logging/configuration cannot enter Production artifacts.
- Production signing material is protected in CI/CD/secure signing infrastructure and never committed.

## 17. Security response and failure behavior
- Protected server operations fail closed on security failure.
- Network failure is not an authorization failure; previously authorized offline work continues only within the frozen offline package.
- 401 triggers one secure refresh attempt; failure returns to Sign In.
- 403 is not blindly retried; the operation remains rejected until authorization context changes.
- Cross-tenant/resource authorization failure must not disclose protected resource existence or metadata.
- Suspected integrity compromise is never bypassed by ignoring hash/verification; affected operations block and use safe recovery/re-download.
- Rejected offline mutations preserve attempted local state required for conflict/recovery and are never represented as authoritative success.
- Unexpected server errors return the generic API error envelope while detailed diagnostics remain protected server-side.
- Security-sensitive errors never expose password/token internals, SQL, stack traces or object-storage details to the user.

## 18. Evidence security/privacy integration
- Official PHOTO Evidence follows Doc15 canonical JPEG processing, size limits and SHA-256 verification.
- Unnecessary EXIF is stripped and GPS/device identifiers are not automatically collected.
- Local PHOTO binary is app-private and application-level encrypted in Production.
- Remote PHOTO binary resides in private encrypted object storage and is accessed only after backend authorization.
- remoteObjectRef is opaque and never a permanent public URL.
- Evidence binary bytes never enter AuditEvent, generic logs or diagnostic packages.
- Historical Evidence retention and cleanup remain governed by Doc15/Doc19 and cannot create dangling references.

## 19. Offline and sync security
- Offline work requires a previously authenticated identity and verified authorized Campaign working set.
- Local save does not weaken server-side authorization at synchronization time.
- Known revocation/disablement blocks incompatible new protected actions.
- 401/403 behavior follows the frozen sync contract; 403 is not blindly retried.
- Integrity hashes prove content integrity, not identity or authorization.
- Rejected offline mutations preserve recoverable attempted state and never become authoritative success.
- Unsynced business work is not deleted merely because authentication must be restored.

## 20. Reporting and export security
- Report query and export are server-authorized to Organization/Campaign scope.
- Audit Trail remains Admin-only.
- Digital Inventory Dossier does not bypass Audit Trail authorization.
- Exports may contain authorized business/personal attribution only when required by the report contract.
- Export creation is explicit and auditable; report payload is not copied wholesale into AuditEvent.
- Offline export is allowed only from complete authorized local data and preserves auditability through durable outbox behavior.

## 21. Retention/privacy boundary
- Final Inventory Snapshot, critical Audit Events, official validated/final Evidence, Issues/resolution history and Campaign Snapshot follow the frozen minimum 10-year retention baseline.
- User disablement or privacy lifecycle operations do not rewrite immutable historical attribution.
- Privacy deletion must not create dangling references or falsify retained history.
- Operational diagnostics and telemetry are not automatically promoted to long-term business history; exact retention is finalized in Doc19.

## 22. Explicit V6 exclusions
- No MFA requirement.
- No public Sign Up.
- No Remember Me.
- No Google/Microsoft SSO.
- No mandatory certificate pinning.
- No trust-all TLS.
- No IMEI/hardware serial/advertising-ID authorization.
- No automatic GPS Evidence collection.
- No live user/device location tracking, background location, turn-by-turn navigation, or route guidance.
- No root detection/device attestation as an authorization boundary.
- No permanent account lock based only on failed passwords.
- No public object-storage URLs as Evidence authorization.
- No self-service deletion that destroys immutable corporate inventory/audit history.

## 23. Required security test matrix

| ID | Scenario | Expected result |
|---|---|---|
| SEC-T01 | Cross-tenant UUID probing | No protected data disclosed; request rejected. |
| SEC-T02 | Standard attempts Admin operation | Server rejects even if request is crafted manually. |
| SEC-T03 | Admin not on Campaign Team mutates stocktaking | Rejected according to Campaign Team authorization. |
| SEC-T04 | Disabled user signs in | Authentication denied without destroying historical identity. |
| SEC-T05 | Unknown vs known email Sign In failure | Externally indistinguishable generic failure. |
| SEC-T06 | Forgot Password known vs unknown email | Generic confirmation; no enumeration. |
| SEC-T07 | Access token >15 min | Rejected/refresh required according to token validity. |
| SEC-T08 | Refresh credential beyond 30 days | Cannot restore session. |
| SEC-T09 | Refresh rotation replay | Previously rotated credential rejected. |
| SEC-T10 | Sign Out then refresh | Revoked current session credential rejected. |
| SEC-T11 | Token search in Room/logs/diagnostics | No token material present. |
| SEC-T12 | Production cleartext HTTP | Blocked. |
| SEC-T13 | Invalid TLS certificate/hostname | Connection fails; no trust-all fallback. |
| SEC-T14 | Object-storage ref without authorization | Cannot download Evidence. |
| SEC-T15 | SQL-injection payload | Treated as data; parameterized query remains safe. |
| SEC-T16 | Offline mutation after server-known revocation | Rejected; attempted state preserved for recovery. |
| SEC-T17 | Production Room file inspected at rest | Business database is encrypted. |
| SEC-T18 | Local PHOTO file inspected outside app flow | Application-level encrypted content is not plaintext Evidence. |
| SEC-T19 | Android backup extraction | No auth secrets, DB keys or protected offline stores. |
| SEC-T20 | PHOTO with GPS EXIF | Canonical Evidence strips GPS/unnecessary EXIF. |
| SEC-T21 | Device installation metadata | Uses generated installation UUID; no IMEI/serial/ad ID. |
| SEC-T22 | Audit secret redaction | No password/token/key in AuditEvent before/after/metadata. |
| SEC-T23 | SYSTEM AuditEvent | actorUserId null allowed only with SYSTEM/source context. |
| SEC-T24 | Failed Sign In flood | Security telemetry captures abuse without flooding business Audit Trail. |
| SEC-T25 | Production log inspection | No secrets, PHOTO bytes, arbitrary Notes or full business payloads. |
| SEC-T26 | Diagnostic package | Contains only approved sanitized technical metadata. |
| SEC-T27 | 5 failed Sign Ins within 15 min | Temporary account-identifier throttling activates. |
| SEC-T28 | Successful Sign In after permitted recovery | Account-level failed-attempt counter follows reset policy. |
| SEC-T29 | 4th reset request within 60 min | Rate-limited while response remains non-enumerating. |
| SEC-T30 | Reset token after 30 min | Rejected. |
| SEC-T31 | Reset token replay | Rejected after first successful use. |
| SEC-T32 | 429 during sync | Retry-After honored; no semantic mutation loss. |
| SEC-T33 | Database key unavailable | No encryption bypass; safe recovery path used. |
| SEC-T34 | Production secret scan in APK/repository | No DB/object-store/token/signing secrets embedded. |
| SEC-T35 | Debug trust/logging in release artifact | Absent. |
| SEC-T36 | Unauthorized cross-tenant 404/403 behavior | Does not reveal protected resource metadata. |
| SEC-T37 | Corrupt Evidence hash | Finalization blocked; safe retry/recovery. |
| SEC-T38 | Internal server exception | Generic API error to client; protected telemetry has diagnostic detail. |
| SEC-T39 | User disabled after historical actions | Historical actor attribution remains intact. |
| SEC-T40 | Privacy deletion process | Cannot destroy immutable retained inventory/audit history. |
| SEC-T41 | LOC-01 map opened | No Android location permission is requested; only stored authorized facility coordinates are used. |
| SEC-T42 | Map provider integration | SiloMath access/refresh tokens, Organization/user names, business payloads and Evidence are not sent to the map provider; provider receives only technically required map requests. |
| SEC-T43 | Cross-tenant facility coordinates | Unauthorized Organization facility coordinates cannot be queried/rendered. |

## 24. Acceptance / release gates

| Gate | Acceptance criterion |
|---|---|
| SEC-AC01 | All protected server operations are server-authorized; UI gating is never sufficient. |
| SEC-AC02 | Organization isolation is enforced for every tenant-owned resource. |
| SEC-AC03 | V6 auth is Email+Password only: no Remember Me, public Sign Up or SSO. |
| SEC-AC04 | Passwords/tokens/keys/secrets are absent from business storage, logs, audit and diagnostics. |
| SEC-AC05 | Access token lifetime is 15 minutes; refresh/session maximum lifetime is 30 days. |
| SEC-AC06 | Production traffic is HTTPS/TLS only with cleartext disabled and no trust-all bypass. |
| SEC-AC07 | Production Room/offline database is encrypted with Keystore-protected key material. |
| SEC-AC08 | Local PHOTO Evidence is application-level encrypted in Production. |
| SEC-AC09 | Remote Evidence/object storage is private, authorized and encrypted at rest. |
| SEC-AC10 | Production PostgreSQL storage/backups are encrypted at rest. |
| SEC-AC11 | Evidence privacy excludes unnecessary EXIF, GPS and persistent hardware identifiers. |
| SEC-AC12 | Facility-map functionality requires no user/device location permission or tracking. |
| SEC-AC13 | Stored facility coordinates are tenant-authorized and omitted/redacted from unsafe logs/telemetry. |
| SEC-AC12 | AuditEvent is append-only and never stores secrets or binaries. |
| SEC-AC13 | Production logging and diagnostics use minimization/redaction. |
| SEC-AC14 | Sign In throttling enforces 5 failed attempts / 15 minutes per normalized account identifier. |
| SEC-AC15 | Forgot Password enforces 3 accepted requests / 60 minutes per normalized account identifier. |
| SEC-AC16 | Reset tokens are single-use with 30-minute validity. |
| SEC-AC17 | There is no permanent auto-lock based only on failed passwords. |
| SEC-AC18 | Offline authorization never becomes permanent authority after known revocation. |
| SEC-AC19 | Security failures fail closed without destroying recoverable unsynced work. |
| SEC-AC20 | Production, Staging and Development secrets are separated. |
| SEC-AC21 | Production artifact contains no debug trust overrides or sensitive verbose logging. |
| SEC-AC22 | Backups exclude Android auth/key/offline protected stores and server backups are encrypted. |
| SEC-AC23 | Historical actor attribution survives user disablement/privacy lifecycle operations. |
| SEC-AC24 | No MFA/certificate pinning/root-attestation requirement is silently introduced into V6. |
| SEC-AC25 | Doc17 introduces no destructive privacy rule that contradicts frozen retention/history contracts. |

## 25. Approved SEC-D01-SEC-D149 decision register

| ID | Area | Frozen V6 outcome |
|---|---|---|
| **SEC-D001** | Security model and trust boundaries | Defense in depth, least privilege and deny-by-default are the V6 security model. |
| **SEC-D002** | Security model and trust boundaries | Canonical trust boundaries are Android app<->Backend API, Backend<->PostgreSQL, Backend<->private object storage, and Android app<->Android secure/app-private storage. |
| **SEC-D003** | Security model and trust boundaries | The Android client is untrusted for authoritative enforcement; every protected server operation is re-authorized server-side. |
| **SEC-D004** | Security model and trust boundaries | Organization isolation is mandatory for all tenant-owned resources; possession of an identifier from another Organization grants no access. |
| **SEC-D005** | Security model and trust boundaries | Effective authorization is authenticated identity  AND  Active User  AND  active OrganizationMembership  AND  role  AND  authorized scope  AND  Campaign/Team rules  AND  workflow/entity state. |
| **SEC-D006** | Security model and trust boundaries | UI hiding/disabled controls are UX controls, not security boundaries. |
| **SEC-D007** | Security model and trust boundaries | UUIDs, Campaign codes, object-storage references and local references are not credentials. |
| **SEC-D008** | Security model and trust boundaries | Unauthorized cross-tenant requests must not disclose protected resource data. |
| **SEC-D009** | Authentication | V6 authentication is Email + Password through the backend/configured authentication service. |
| **SEC-D010** | Authentication | V6 has no Remember Me, public Sign Up, Google SSO or Microsoft SSO. |
| **SEC-D011** | Authentication | Android never persists the password in Room, DataStore, logs, analytics, AuditEvent or diagnostics. |
| **SEC-D012** | Authentication | Invalid Sign In uses a generic response that does not confirm account existence. |
| **SEC-D013** | Authentication | Forgot Password uses a generic confirmation to reduce account enumeration. |
| **SEC-D014** | Authentication | Password reset is online-only and requires valid recovery context/token. |
| **SEC-D015** | Authentication | Successful password reset invalidates all active refresh/session credentials for that User. |
| **SEC-D016** | Authentication | A Disabled User cannot initiate a new authentication. |
| **SEC-D017** | Authentication | MFA is not a V6 requirement; adding it requires an explicit contract change. |
| **SEC-D018** | Authentication | V6 does not mandate a particular backend password-hash implementation, but plaintext or reversible password storage is forbidden. |
| **SEC-D019** | Session and token security | Access credentials are short-lived; refresh/session credentials are longer-lived and revocable. |
| **SEC-D020** | Session and token security | Access-token lifetime is 15 minutes; refresh/session maximum lifetime is 30 days. |
| **SEC-D021** | Session and token security | Refresh credentials rotate on successful refresh; a confirmed previous credential is not reusable. |
| **SEC-D022** | Session and token security | Sign Out invalidates the current server-side refresh/session credential and removes local active authentication credentials. |
| **SEC-D023** | Session and token security | Access/refresh credentials and protection keys are stored only through Android Keystore-backed secure storage. |
| **SEC-D024** | Session and token security | Tokens never enter Room, business outbox payloads, logs, analytics, crash reports, AuditEvent or diagnostic packages. |
| **SEC-D025** | Session and token security | App restart may restore a session only through protected credential/session material; this is not a user-selectable Remember Me feature. |
| **SEC-D026** | Session and token security | 401 permits one secure refresh attempt and retry of the same operation; refresh failure returns the user to Sign In. |
| **SEC-D027** | Session and token security | First-time Sign In is not possible offline. |
| **SEC-D028** | Session and token security | Continue Offline requires a previously authenticated local identity and authorized verified offline context. |
| **SEC-D029** | Session and token security | Expired/revoked online credentials do not become permanent offline authority. |
| **SEC-D030** | Session and token security | When disablement/revocation becomes known, incompatible new operations are blocked; recoverable unsynced work is preserved rather than arbitrarily deleted. |
| **SEC-D031** | Transport and API security | Production API and Evidence transfer use HTTPS/TLS only. |
| **SEC-D032** | Transport and API security | Production Android Network Security Configuration disables cleartext HTTP. |
| **SEC-D033** | Transport and API security | Minimum transport is TLS 1.2; TLS 1.3 is preferred where negotiated. |
| **SEC-D034** | Transport and API security | Production uses normal certificate-chain and hostname validation; trust-all/bypass logic is forbidden. |
| **SEC-D035** | Transport and API security | Certificate pinning is not mandatory in V6 and requires a separate ADR with rotation/recovery strategy if introduced. |
| **SEC-D036** | Transport and API security | Authentication and authorization are evaluated for every protected API request; client-supplied organizationId/userId is not proof of authority. |
| **SEC-D037** | Transport and API security | API errors never expose stack traces, SQL, secrets, token values or internal object-storage credentials. |
| **SEC-D038** | Transport and API security | Idempotency keys, entityVersion and correlation IDs support integrity/traceability but are not authentication credentials. |
| **SEC-D039** | Local device security | Offline business data uses the Android application sandbox and app-private storage. |
| **SEC-D040** | Local device security | PHOTO Evidence remains app-private and is not automatically copied to public gallery/shared storage. |
| **SEC-D041** | Local device security | Room contains neither password/token secrets nor PHOTO BLOBs. |
| **SEC-D042** | Local device security | Production Room/offline business data is encrypted at rest; key material is protected through Android Keystore. |
| **SEC-D043** | Local device security | Database-encryption keys are not hardcoded, committed, embedded as plaintext configuration or synchronized as business data. |
| **SEC-D044** | Local device security | Platform backup/restore excludes auth credentials, database keys and sensitive offline Campaign/Evidence stores. |
| **SEC-D045** | Local device security | Secrets are not copied/shared through clipboard/export/share workflows; business exports use approved report flows only. |
| **SEC-D046** | Local device security | Root detection/device attestation is not a V6 authorization boundary. |
| **SEC-D047** | Backend, database and object storage | Android never connects directly to PostgreSQL; business access goes through the Backend API. |
| **SEC-D048** | Backend, database and object storage | Database and object-storage credentials are environment secrets and are never included in the Android APK. |
| **SEC-D049** | Backend, database and object storage | Production database transport is encrypted across trust boundaries; public unauthenticated database exposure is forbidden. |
| **SEC-D050** | Backend, database and object storage | Evidence object storage is private; remoteObjectRef is opaque and is not a public URL or bearer credential. |
| **SEC-D051** | Backend, database and object storage | Evidence upload/download authorization is enforced through backend Organization/Campaign/owner context. |
| **SEC-D052** | Backend, database and object storage | Storage lifecycle/cleanup must not delete retained historical Evidence or create dangling references. |
| **SEC-D053** | Backend, database and object storage | Production secrets are supplied by environment/secret-management facilities, not committed configuration. |
| **SEC-D054** | Backend, database and object storage | Development, Staging and Production use separate credentials/secrets; Production credentials are not reused in lower environments. |
| **SEC-D055** | Input and data integrity | Server validates client-controlled IDs, enums, lengths, numeric constraints, entityVersion and workflow transitions. |
| **SEC-D056** | Input and data integrity | SQL access uses parameterized/prepared queries or safe ORM binding; client input is never concatenated into executable SQL. |
| **SEC-D057** | Input and data integrity | Client text is treated as data, not executable markup/code; render/export layers encode/escape for their format. |
| **SEC-D058** | Input and data integrity | PHOTO becomes official Evidence only after canonical JPEG processing and MIME/size/SHA-256 verification defined by Doc15. |
| **SEC-D059** | Input and data integrity | Offline/sync payloads cannot bypass immutability, history or authorization rules. |
| **SEC-D060** | Input and data integrity | Conflict resolution provides no force-overwrite and cannot turn stale local state into authoritative state. |
| **SEC-D061** | Security and authentication audit events | Only security-relevant events with accountability/detection value are audited; routine technical requests do not become AuditEvents. |
| **SEC-D062** | Security and authentication audit events | Successful Sign In generates a security AuditEvent without storing password/token material. |
| **SEC-D063** | Security and authentication audit events | Failed Sign In is recorded in security telemetry for abuse detection; a business AuditEvent is not mandatory for every wrong password. |
| **SEC-D064** | Security and authentication audit events | Sign Out, completed password reset, User enable/disable, Membership disable/reactivate and role change are auditable. |
| **SEC-D065** | Security and authentication audit events | Sensitive authorization denials may generate security telemetry/event; routine UI denials do not pollute Audit Trail. |
| **SEC-D066** | Security and authentication audit events | Campaign Team changes, Reset Validation, Issue resolution/reopen, Campaign Close, master-data lifecycle changes and report/export events remain auditable under their frozen contracts. |
| **SEC-D067** | Security and authentication audit events | AuditEvent preserves stable actor identity and frozen actor display information where history requires it. |
| **SEC-D068** | Security and authentication audit events | actorUserId may be null only for SYSTEM; system/source context must identify the actor context. |
| **SEC-D069** | Security and authentication audit events | Audit records never contain passwords, access/refresh/reset tokens, Authorization headers, DB/object-store credentials or cryptographic keys. |
| **SEC-D070** | Security and authentication audit events | PHOTO bytes and other binary payloads never enter AuditEvent; only approved identifiers/metadata may appear. |
| **SEC-D071** | Security and authentication audit events | Before/after values are limited to relevant fields and redact secret/sensitive fields. |
| **SEC-D072** | Security and authentication audit events | AuditEvent is append-only; arbitrary correction/deletion of historical events is forbidden. |
| **SEC-D073** | Security and authentication audit events | Server-accepted actions use server-authoritative UTC occurredAt; offline event context may preserve client occurrence separately without falsifying server acceptance time. |
| **SEC-D074** | Security and authentication audit events | Correlation IDs may connect requests, AuditEvent and telemetry but contain no PII and are not credentials. |
| **SEC-D075** | Privacy and personal-data protection | V6 applies data minimization: collect only data necessary for authentication, authorization, inventory workflow, auditability, reporting and support/operations. |
| **SEC-D076** | Privacy and personal-data protection | Principal personal data includes Full Name, Email, OrganizationMembership/Role, Campaign Team association and actor attribution in workflow/audit history. |
| **SEC-D077** | Privacy and personal-data protection | V6 does not require date of birth, sex, personal address, personal phone number, biometric data or precise location. |
| **SEC-D078** | Privacy and personal-data protection | GPS/location permission is not required for V6 Evidence and is not collected automatically. |
| **SEC-D079** | Privacy and personal-data protection | DeviceInstallation uses an app-generated installation UUID; IMEI, hardware serial and advertising ID are excluded. |
| **SEC-D080** | Privacy and personal-data protection | PHOTO normalization removes unnecessary EXIF; GPS/device model/serial metadata does not become business Evidence metadata. |
| **SEC-D081** | Privacy and personal-data protection | Original filesystem paths are not stored as business metadata. |
| **SEC-D082** | Privacy and personal-data protection | Disabling a User does not erase historical actor attribution; stable identity and frozen display context remain. |
| **SEC-D083** | Privacy and personal-data protection | Profile data no longer operationally required may follow retention/deletion policy, but retained immutable business/audit attribution is preserved. |
| **SEC-D084** | Privacy and personal-data protection | Tenant data is not used for cross-tenant profiling or advertising in V6. |
| **SEC-D085** | Privacy and personal-data protection | Operational analytics, if introduced, never receive passwords, tokens, Evidence binaries, arbitrary Notes, full reports or unrestricted business payloads. |
| **SEC-D086** | Privacy and personal-data protection | Crash/diagnostic collection is minimized and redacted before external telemetry. |
| **SEC-D087** | Privacy and personal-data protection | Privacy-sensitive content is not automatically copied to clipboard, public storage or external share targets. |
| **SEC-D088** | Privacy and personal-data protection | Authorized PDF/CSV exports may include necessary business/personal attribution; creation remains explicit, authorized and audited. |
| **SEC-D089** | Privacy and personal-data protection | V6 has no self-service deletion that destroys immutable corporate inventory/audit history; such requests follow organization/legal retention processes. |
| **SEC-D090** | Privacy and personal-data protection | Final retention/deletion is governed by Doc19 and must not create dangling references or falsify retained history. |
| **SEC-D091** | Logging, diagnostics and observability | Production uses structured logging with explicit redaction. |
| **SEC-D092** | Logging, diagnostics and observability | Logs never contain passwords, tokens, Authorization/Cookie headers, cryptographic keys or secret configuration. |
| **SEC-D093** | Logging, diagnostics and observability | Logs never contain PHOTO bytes/base64, complete Evidence binaries or database dumps. |
| **SEC-D094** | Logging, diagnostics and observability | Arbitrary Note text and full request/response business payloads are not logged by default. |
| **SEC-D095** | Logging, diagnostics and observability | Safe context may include timestamp, environment, component, severity, correlationId, endpoint/operation ID, sanitized error code, duration and non-secret technical IDs. |
| **SEC-D096** | Logging, diagnostics and observability | Email/full user display data is not logged by default when stable internal userId is sufficient. |
| **SEC-D097** | Logging, diagnostics and observability | Protected server telemetry may contain stack traces for unexpected internal errors; API clients never receive them. |
| **SEC-D098** | Logging, diagnostics and observability | Production Android builds do not leave verbose/debug payload logging enabled for sensitive workflows. |
| **SEC-D099** | Logging, diagnostics and observability | Diagnostic packages are explicit and exclude auth secrets, database-encryption keys, raw credential stores and PHOTO binaries. |
| **SEC-D100** | Logging, diagnostics and observability | Diagnostic packages may include app version, environment identifier, non-persistent compatibility metadata, sanitized sync state/counts, error codes and correlation references. |
| **SEC-D101** | Logging, diagnostics and observability | Diagnostic export supports review/redaction before support transfer when business identifiers may be present. |
| **SEC-D102** | Logging, diagnostics and observability | SyncOperation/SyncConflict/Audit before-after payloads are not copied wholesale into generic production logs. |
| **SEC-D103** | Logging, diagnostics and observability | Security telemetry and business Audit Trail are separate: telemetry serves detection/operations; Audit Trail serves accountability/history. |
| **SEC-D104** | Logging, diagnostics and observability | Observability must detect authentication abuse, abnormal authorization denials, sync-failure spikes, report-integrity errors, repeated Evidence upload/hash failures and unexpected server errors. |
| **SEC-D105** | Abuse protection and rate limiting | Authentication endpoints are protected by server-side rate limiting/throttling. |
| **SEC-D106** | Abuse protection and rate limiting | Sign In allows at most 5 failed attempts per 15 minutes per normalized account identifier before temporary throttling. |
| **SEC-D107** | Abuse protection and rate limiting | Additional deployment-configurable source/network protection mitigates distributed credential guessing and is not used alone for permanent account lock. |
| **SEC-D108** | Abuse protection and rate limiting | There is no permanent automatic account lock based only on failed password attempts. |
| **SEC-D109** | Abuse protection and rate limiting | Successful authentication may reset account-level failed-attempt counters according to backend policy. |
| **SEC-D110** | Abuse protection and rate limiting | Forgot/Reset Password endpoints are separately rate-limited by account identifier and abuse source. |
| **SEC-D111** | Abuse protection and rate limiting | Forgot Password accepts at most 3 reset requests per 60 minutes per normalized account identifier while keeping the response generic. |
| **SEC-D112** | Abuse protection and rate limiting | Reset tokens are single-use and valid for 30 minutes. |
| **SEC-D113** | Abuse protection and rate limiting | Consumed, superseded or expired reset tokens cannot be reused. |
| **SEC-D114** | Abuse protection and rate limiting | Authenticated API rate limits may be deployment-configurable but must not break legitimate sync batches or report exports. |
| **SEC-D115** | Abuse protection and rate limiting | HTTP 429 uses Retry-After when available; the frozen sync engine respects it. |
| **SEC-D116** | Abuse protection and rate limiting | Rate limiting is not authorization; every request remains subject to normal authentication/authorization. |
| **SEC-D117** | Abuse protection and rate limiting | Idempotent retry of the same SyncOperation is not automatically treated as a malicious duplicate mutation. |
| **SEC-D118** | Abuse protection and rate limiting | Evidence upload controls may limit size/concurrency/rate without changing the frozen 3 MB PHOTO hard maximum or Evidence semantics. |
| **SEC-D119** | Abuse protection and rate limiting | Report/export controls may limit concurrency/rate without producing partial or semantically different datasets. |
| **SEC-D120** | Abuse protection and rate limiting | Repeated abuse-limit activation is security telemetry and does not automatically create a business AuditEvent for every rejection. |
| **SEC-D121** | Encryption and key management | Room encryption uses a random per-installation database secret that is not derived from the user's password. |
| **SEC-D122** | Encryption and key management | The database secret is wrapped/protected by Android Keystore-backed key material. |
| **SEC-D123** | Encryption and key management | Keystore/database keys are not synchronized to backend and are excluded from Android backup. |
| **SEC-D124** | Encryption and key management | If secure key material becomes permanently unavailable/corrupt, encryption is not bypassed; recovery/re-authentication/re-download is used while preserving recoverable unsynced data where technically possible. |
| **SEC-D125** | Encryption and key management | Local PHOTO Evidence is encrypted at application level in Production using key material protected by Android Keystore, in addition to app-private sandboxing. |
| **SEC-D126** | Encryption and key management | Remote Evidence uses object-storage/platform encryption at rest. |
| **SEC-D127** | Encryption and key management | Production PostgreSQL storage and backups use infrastructure-level encryption at rest. |
| **SEC-D128** | Encryption and key management | Backup encryption keys and Production data-encryption credentials are managed through Production secret/KMS facilities, not source control. |
| **SEC-D129** | Backup, restore and environment separation | Production database backups are encrypted and limited to authorized operators/service identities. |
| **SEC-D130** | Backup, restore and environment separation | Backup restore is privileged, operationally audited and periodically tested; RPO/RTO are governed by Doc20. |
| **SEC-D131** | Backup, restore and environment separation | Android platform/cloud backup excludes auth secrets, encryption keys and protected offline business/Evidence stores. |
| **SEC-D132** | Backup, restore and environment separation | Development/Staging do not use Production snapshots with real business/personal data except through an explicitly authorized and sanitized process. |
| **SEC-D133** | Backup, restore and environment separation | Production API base URL and non-secret environment identifiers may be build configuration; credentials/secrets are never plaintext build constants. |
| **SEC-D134** | Backup, restore and environment separation | Debug and Production builds are separated; debug trust overrides/logging/configuration cannot enter Production artifacts. |
| **SEC-D135** | Backup, restore and environment separation | Production signing material is protected in CI/CD/secure signing infrastructure and never committed. |
| **SEC-D136** | Security response and failure behavior | Protected server operations fail closed on security failure. |
| **SEC-D137** | Security response and failure behavior | Network failure is not an authorization failure; previously authorized offline work continues only within the frozen offline package. |
| **SEC-D138** | Security response and failure behavior | 401 triggers one secure refresh attempt; failure returns to Sign In. |
| **SEC-D139** | Security response and failure behavior | 403 is not blindly retried; the operation remains rejected until authorization context changes. |
| **SEC-D140** | Security response and failure behavior | Cross-tenant/resource authorization failure must not disclose protected resource existence or metadata. |
| **SEC-D141** | Security response and failure behavior | Suspected integrity compromise is never bypassed by ignoring hash/verification; affected operations block and use safe recovery/re-download. |
| **SEC-D142** | Security response and failure behavior | Rejected offline mutations preserve attempted local state required for conflict/recovery and are never represented as authoritative success. |
| **SEC-D143** | Security response and failure behavior | Unexpected server errors return the generic API error envelope while detailed diagnostics remain protected server-side. |
| **SEC-D144** | Security response and failure behavior | Security-sensitive errors never expose password/token internals, SQL, stack traces or object-storage details to the user. |
| **SEC-D145** | Facility geodata classification | Location latitude/longitude are protected Organization-scoped facility master data, not user/device location. |
| **SEC-D146** | No location permission/tracking | The approved Location map never requires Android location permission, foreground/background GPS, user tracking, or route guidance. |
| **SEC-D147** | Map provider data minimization | Map-provider integration must not receive SiloMath credentials, user/Organization names, business payloads, Evidence, or audit data; only provider-required map/viewport requests may leave the SiloMath boundary. |
| **SEC-D148** | Geo logging/telemetry | Facility coordinates are not written to ordinary client logs/crash breadcrumbs and are included in support/telemetry only when explicitly necessary, protected, and minimized. |
| **SEC-D149** | Geo tenant isolation | Facility-coordinate reads/writes follow the same Organization authorization, Admin mutation and optimistic-version rules as Location master data. |

## 26. Cross-document handoff
- Doc18 Test Strategy & Acceptance must make the security test matrix and release gates executable/traceable.
- Doc19 Retention Policy must assign exact retention/purge behavior to security telemetry, diagnostics, profile data and all already-frozen long-term records without weakening immutable history.
- Doc20 Production Operations must implement secret management, encrypted backup/restore, monitoring/alerting, environment separation, release controls and incident/operational procedures consistent with this document.
- Security changes that alter authentication factors, token policy, encryption model, authorization boundaries or retained-history semantics require explicit contract change and cross-document consistency review.

## 27. Freeze statement

This document is the canonical APPROVED / FROZEN SiloMath V6 security, audit and privacy contract after the 2026-10-20 CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze, defined by SEC-D01-SEC-D149, including authorization boundaries, 15-minute access tokens, 30-day refresh/session maximum, Production Room and local PHOTO encryption, privacy minimization, append-only audit rules, safe telemetry, throttling/reset controls, encrypted infrastructure storage/backups and fail-closed security behavior.

# SiloMath V6 - 20_PRODUCTION_OPERATIONS

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-29 - CR-ARCH-SUPABASE-BACKEND-001 approved Supabase-only canonical core backend platform

## 1. Purpose and authority

This document is the normative V6 production-operations contract. It consolidates frozen operational requirements from Documents 01-19 and the approved PO-D01-PO-D293 decisions. It governs deployment, runtime operations, observability, backup and recovery, incident handling, privileged operations, retention-job operation and Production acceptance. It does not redefine business, security, retention or test semantics owned by earlier frozen documents.

## 2. Non-negotiable production objectives
- Exactly three environment classes: Development, Staging and Production.
- Frozen release path: Build -> Automated Tests -> Migration Validation -> Staging -> Production -> Smoke Test.
- Risky database change pattern: Expand -> Migrate -> Contract.
- PostgreSQL full/base backup at least daily; backup retention minimum 30 days.
- PITR/recovery-point capture sufficient to demonstrate RPO <=1 hour.
- Disaster-recovery target RTO <=4 hours, measured through controlled service restoration after integrity validation.
- Restore exercise at least quarterly in an isolated environment.
- Data integrity takes precedence over availability when the two conflict.

## 3. Environment matrix

| Environment | Purpose | V6 control |
|---|---|---|
| Development | Implementation/debugging | No Production credentials/secrets; non-official data. |
| Staging | Production-like validation, migration rehearsal, UAT | No implicit Production business data; mirrors relevant Production controls. |
| Production | Official business operations | Protected release path, official data, strict operational controls. |

## 4. Operational severity matrix

| Severity | Definition | Required response |
|---|---|---|
| P1 Critical | Outage or immediate data-loss/corruption risk; tenant isolation or official snapshot integrity failure | Immediate response/escalation and ownership. |
| P2 High | Major degradation without confirmed corruption; backup/Evidence/severe sync/service failure | Prompt response and tracked remediation. |
| P3 Medium | Limited degradation, capacity warning, elevated retries, isolated fault | Tracked operational work. |
| P4 Low | Informational/maintenance condition | Follow-up through normal operations. |


## 5. Production operating model
- Doc20 is the V6 authority for deployment, runtime operations, observability, backup/recovery, incident handling and operational runbooks.
- There are exactly three environment classes: Development, Staging and Production.
- Production is the only environment that serves official business operations/data.
- Staging is Production-like for schema, API/security configuration, object-storage behavior, migration rehearsal and UAT, but does not implicitly use Production business data.
- Development is for implementation/debugging and does not use Production credentials/secrets.
- Environment isolation includes at minimum API/backend, database, object storage, secrets and operational telemetry context.
- Production failure handling favors data integrity over availability.
- If integrity required for Close is uncertain, Close is blocked; valid offline Stocktaking may continue only within the verified working set.

## 6. Build variants and environment configuration
- Android uses environment-aware build configuration for development/debug, staging and Production release.
- Production artifact uses only Production API/environment configuration.
- Debug/staging endpoint or trust override cannot be present or active in Production artifact.
- Non-secret environment values may be injected through Gradle/build configuration.
- Secrets are not Gradle constants, source code, Git, APK resources or plaintext configuration files.
- Backend configuration is externalized through environment/runtime configuration and secret-management facilities.
- Environment identity is operationally observable to prevent accidental deployment to the wrong environment.
- Production configuration change is a controlled operational change and is not performed through Organization Settings.
- Concrete environment hostnames/base URLs, certificates and external provider endpoints are supplied through controlled environment/runtime configuration. Their values are not frozen Product constants and do not require Product/Baseline approval when the `/api/v1`, tenant, authentication, TLS and other security semantics remain unchanged.

## 7. CI/CD and artifact provenance
- Production build is created only by a protected CI/CD pipeline.
- Production Android release uses R8 in full optimization mode unless a documented, reproducible toolchain incompatibility prevents it; any temporary exception is an implementation risk record and cannot weaken security or release gates.
- Baseline/Startup Profile generation and verification are part of the Android release-engineering path for startup and critical user journeys defined by Doc18.
- Macrobenchmark evidence is produced for the representative critical journeys and compared with the version-controlled engineering performance baseline before Production promotion.
- Release artifact is not built manually on a workstation and then promoted as a Production artifact.
- Each release candidate has immutable version/build identity.
- Pipeline preserves the link between source revision/commit, build identity, test evidence and released artifact.
- Production Android artifact is signed only through a protected signing mechanism.
- Signing keys are not kept in the repository or developer workstation as the normal release mechanism.
- Dependency acquisition/build is reproducible within reasonable limits through pinned/version-controlled build configuration.
- Release evidence from Doc18 is associated with the exact promoted artifact, not an almost-identical build.
- An artifact that did not pass the exact mandatory release gates cannot be promoted merely by changing a label/tag.

## 8. Release path and promotion
- Frozen release path remains Build -> Automated Tests -> Migration Validation -> Staging -> Production -> Smoke Test.
- Doc18 exact release gates must be PASS before Production promotion, including visual/adaptive/accessibility verification and the Android performance-engineering gates applicable to the changed release.
- Staging validation uses the same release-candidate logic/artifact lineage intended for Production.
- UAT sign-off refers to an identifiable release candidate.
- Production deployment requires explicit release authorization/approval in the pipeline.
- Production smoke tests are non-destructive.
- Smoke tests verify at minimum service/API health, authentication path, authorized read path and required client compatibility without arbitrarily creating or closing an official Campaign.
- Failed Production smoke test triggers incident/release-response workflow; the release is not declared healthy merely because technical deployment completed.
- Release status distinguishes DEPLOYED from VERIFIED HEALTHY; these are operational states, not business enums.

## 9. Database migration operations
- All Production schema migrations are versioned and reviewable.
- Destructive migration of retained business/history data is prohibited.
- Risky migration uses Expand -> Migrate -> Contract.
- Expand introduces backward-compatible structures before old structures are removed.
- Migrate moves/backfills data with explicit validation and restart-safe execution.
- Contract runs only after verification that the new application no longer depends on the old structure and migration invariants are satisfied.
- Relevant risky Production migration/release requires a verified pre-change backup/recovery point.
- Migration rehearsal runs in Staging or isolated representative populated data before Production.
- Migration is fail-safe: failure cannot be masked as a successful schema state.
- Long-running backfill is resumable/idempotent or has an explicit recovery mechanism.
- Historical versions/snapshots are not rewritten to simplify migration.
- Room migration compatibility is a release gate separate from backend PostgreSQL migration validation.

## 10. Deployment compatibility and rollback
- Production rollback prefers application rollback/forward-fix rather than destructive database downgrade.
- Schema migration is designed so the previous compatible application version can continue during the Expand/Migrate window where rollout strategy requires it.
- After an irreversible Contract step, rollback to an incompatible version is prohibited.
- If DB migration is applied but application rollout fails, response is compatible app rollback or forward-fix according to migration compatibility, not schema destruction.
- Restore from backup is a disaster-recovery action, not a normal release rollback mechanism.
- Rollback decision protects offline clients and pending SyncOperations; the server contract is not withdrawn in a way that turns valid queued operations into corruption.
- API compatibility changes use staged rollout/versioning according to the frozen /api/v1 contract and client compatibility.
- Production rollback/forward-fix is an auditable operational event.

## 11. Service health model
- Backend exposes health/readiness signals sufficient for orchestration/monitoring without business payload.
- Liveness indicates process/service responsiveness; readiness indicates whether an instance can safely serve required dependencies.
- Readiness considers at minimum database connectivity and mandatory runtime configuration.
- Object-storage degradation may make Evidence operations unavailable without falsely declaring Evidence upload healthy.
- Health endpoint exposes no secrets, connection strings, stack traces or sensitive environment details.
- Health status does not replace business-integrity checks for Close, reporting or sync.

## 12. Observability, metrics and dashboards
- Production observability has four base signals: logs, metrics, traces and alerts.
- All backend request paths use a correlation ID propagated through request processing and relevant downstream operations.
- Logs are structured and include at minimum UTC timestamp, severity, service/component, environment, correlation ID and safe event/message.
- Logs exclude passwords, access/refresh/reset tokens, secret keys, Evidence binary, arbitrary Notes and full sensitive business payloads.
- Production dashboard shows at minimum API health, error rate, request latency, DB health, sync health, Evidence upload health, job health and backup status.
- API metrics are segmented at least by endpoint/operation class and response category, without tenant-sensitive high-cardinality labels.
- Background-job metrics include last success, last failure, duration and next/expected execution where scheduled.
- Crash/exception telemetry is sanitized and correlates with application version/environment.
- Performance traces are mandatory for campaign package load, DB migration, sync batch and report generation.
- Calculation diagnostics expose engine/version/result-state metadata needed for troubleshooting, but not full sensitive calculation payload in Production logs.
- Monitoring failure does not block a business transaction after business persistence succeeded.
- Missing telemetry cannot transform a failed business operation into success.

## 13. Alerting, severities and thresholds
- Operational alerts use P1 Critical, P2 High, P3 Medium and P4 Low severities.
- P1 means outage or immediate risk of data loss/corruption, tenant-isolation failure, official snapshot-integrity failure, or generalized inability to use Production service.
- P2 means major degradation without confirmed data corruption, including severe sync backlog, repeated job failure, Evidence service unavailability, backup failure or substantial API failure.
- P3 means limited/non-critical degradation, capacity warning, elevated retries or isolated operational fault.
- P4 means informational/maintenance condition requiring follow-up rather than immediate incident response.
- Any failed scheduled Production backup generates at least P2.
- Backup age that could violate RPO <=1h generates P1 before the recovery objective is silently violated.
- Confirmed tenant-isolation/security-boundary failure is P1.
- Confirmed immutable Final Snapshot corruption/inconsistency is P1.
- Alert thresholds for latency/error/backlog are baseline-derived and environment-configured but never redefine business rules or release acceptance.
- Threshold tuning is auditable operational configuration and is not an Organization setting.
- Alert deduplication/grouping is allowed; suppression cannot hide an unresolved P1.

## 14. Sync operational monitoring
- Sync dashboard includes queue depth, oldest pending age, retry count and conflict count.
- PENDING, IN_FLIGHT, ACKNOWLEDGED, permanent failure and conflicts are monitored separately.
- Oldest pending age is measured separately for retryable and blocked/dependency/conflict conditions.
- Persistent growth in queue depth/oldest age generates an operational alert before becoming systemic backlog.
- Duplicate/idempotency anomaly is security/data-integrity relevant and is not treated merely as generic retry telemetry.
- Pull/checkpoint failures and push failures are observable separately.
- Package download/verification failures are observable separately from mutation sync.
- Conflict count is not operationally resolved through auto-merge or auto-delete.
- Operations cannot manually mark ACKNOWLEDGED without authoritative server acknowledgement.
- Sync telemetry never contains the full mutation payload.

## 15. Evidence and object-storage monitoring
- Evidence metrics include upload attempts/success/failure, verification failure, upload latency, orphan-cleanup result and storage availability.
- SHA-256 mismatch/server verification failure is a data-integrity alert.
- Evidence metadata committed without the required durable official object must be detectable and cannot remain a silent inconsistent state.
- Object-storage authorization failure is distinguished from network/transient failure.
- Repeated Evidence upload failure for official pending Evidence contributes to Close-blocking operational visibility.
- Orphan cleanup reports examined/deleted/skipped/error counts without logging image bytes.
- Object-storage capacity/quota warnings are detected before write exhaustion.
- Object-storage cleanup follows Doc19 retention/hold/dependency rules; generic bucket cleanup cannot bypass those guards.

## 16. Report and calculation integrity monitoring
- Report-generation metrics include request count, success/failure and generation duration per report type.
- Final-report integrity failure or mismatch with the immutable authoritative dataset is a data-integrity incident and misleading export is blocked.
- Calculation-engine version is observable for diagnostics/release correlation.
- Unexpected calculation-invariant failure is captured as sanitized error telemetry and is never converted to zero/default quantity.
- Calculation regression is not detected through Production telemetry instead of golden tests; the golden suite remains a release gate.
- Historical calculation/report paths never invoke current formulas to repair old results.
- Abnormal increase in calculation/report failures produces an alert, but statistical threshold never redefines calculation validity.
- Dossier/Audit export authorization failures are monitored without logging protected output.

## 17. Database operational monitoring
- PostgreSQL monitoring includes availability, connections/pool pressure, storage utilization, transaction failures, slow-query indicators, migration state and backup/PITR health.
- DB storage capacity has warning before exhaustion and critical alert before write failure.
- Repeated constraint failures are observable, but database constraints are never disabled for availability.
- Deadlock/transaction rollback rate is monitored.
- Migration execution has explicit start/end/result/version telemetry.
- A migration marked failed cannot be operationally rewritten as success without remediation and revalidation.
- Manual Production DB mutation is not a normal incident-resolution mechanism.
- Emergency privileged DB change, if absolutely necessary, requires a documented incident/change record, backup consideration, explicit validation and audit trail.
- DB monitoring does not expose query parameter values containing sensitive business data.
- Connection-pool exhaustion and inability to establish DB connection affect readiness and alerting.

## 18. Backup and PITR mechanics
- PostgreSQL has automated full/base backup at least daily.
- Backup retention is minimum 30 days, according to Doc19.
- To support RPO <=1h, transaction/WAL/PITR recovery points are captured so the maximum targeted recoverable data gap is <=60 minutes.
- PITR is enabled in Production where supported; a platform/configuration unable to satisfy RPO <=1h is not Production-acceptable.
- Backup success means the artifact is created and cataloged; job start alone is not success.
- Relevant risky migration/release has a verified pre-change recovery point/backup before change.
- Backup storage is access-controlled and encrypted at rest.
- Backup credentials are separate from normal application credentials.
- Backup failure does not delete or replace the last known-good backup chain.
- Backup inventory/age/last-success/PITR continuity are monitored.
- Object-storage Evidence has recovery protection sufficient for official Evidence retention; DB backup alone is not complete disaster recovery.
- Restore from backup follows Doc19 disposition reapplication so expired/purged data does not become permanently active.

## 19. Restore and disaster recovery
- Restore test is executed at least quarterly in an isolated environment.
- Restore test uses real backup/recovery artifacts of the Production mechanism without modifying Production.
- Restore verification includes DB integrity, schema version, representative immutable history, tenant isolation, Evidence references and ability to start a compatible backend.
- PITR exercise verifies selection of a recovery point and recoverability within the RPO.
- DR procedure explicitly follows declare incident -> select recovery point -> restore -> validate -> reapply disposition controls -> controlled service restoration.
- Restored environment receives no Production traffic before integrity validation.
- If recovered state has consistency uncertainty, Close remains blocked.
- DR never uses manual recreation of Final Snapshot as a substitute for restore.
- DR exercise produces retained operational evidence according to Doc19/Doc18.
- Failed quarterly restore test is at least P2 and blocks the claim that DR capability is verified.

## 20. RPO and RTO verification
- RPO <=1h means at most one hour of authoritative committed Production data loss at the declared recovery point.
- RTO <=4h is measured from declaration of a service-disrupting recovery incident to controlled restoration of required Production service.
- RTO is not met merely because HTTP responds when integrity validation is incomplete.
- Quarterly DR evidence measures achieved RPO and achieved RTO.
- A test exceeding RPO/RTO is a failed recovery objective, not PASS with note.
- Production recovery architecture is remediated if it repeatedly cannot demonstrate the objectives.
- Evidence/object storage is included in DR readiness; DB-only restoration does not prove full SiloMath recovery.
- Offline device copies are not authoritative backup mechanisms for RPO/RTO.

## 21. Incident severity and response
- Incident severity uses the same P1-P4 levels as alerting.
- P1 requires immediate response/escalation and incident ownership.
- P2 requires prompt operational response and tracked remediation.
- P3/P4 enter normal tracked operational work according to impact.
- Every P1/P2 has an incident record with timeline, impact, decisions, mitigation, recovery and closure.
- A data-integrity incident is not closed merely because service availability returned; integrity must be verified.
- Incident actions modifying Production data/configuration are auditable.
- Post-incident review is mandatory for P1 and for P2 with data/security/recovery impact.
- Post-incident review tracks root cause and preventive actions without rewriting frozen business history.
- Incident evidence follows Doc17 privacy/security and Doc19 retention.

## 22. Security incident operations
- Suspected credential compromise permits rapid revocation/rotation of affected secret/session material.
- Confirmed tenant-isolation breach is P1.
- Security incident preserves relevant telemetry through RetentionHold when necessary.
- Incident investigation never justifies temporary logging of passwords, tokens or Evidence binaries.
- Production access may be further restricted during an incident without disabling authorization/tenant guards.
- Compromised signing/secret material is never reused after confirmed compromise.
- Security remediation is verified in Staging/controlled path before normal deployment, except emergency containment that is subsequently formalized and validated.
- Security event and remediation preserve correlation/timeline evidence without unnecessary personal/business payload.

## 23. Secrets and key rotation
- Production secrets are managed through an approved secret store/runtime identity mechanism.
- Secrets are not stored in Git, APK, logs or normal diagnostics.
- Secrets are scoped per environment; Dev/Staging secrets do not provide Production access.
- Database, object-storage, CI signing/deployment and other privileged credentials use least privilege.
- Secret rotation permits controlled replacement without source-code change.
- Rotation is mandatory on confirmed/suspected compromise and on ownership/access change that requires it.
- V6 does not invent a universal periodicity for every secret type; provider/security policy may require a stricter interval.
- Rotation success is verified before old credential revocation when overlap is needed for availability.
- Old credential is revoked after successful cutover and does not remain an indefinite fallback.
- Secret access/rotation privileged operations are auditable without recording secret value.

## 24. Dependency and vulnerability management
- CI performs dependency vulnerability and license checks according to frozen architecture.
- Production dependencies are version-controlled; uncontrolled floating dependencies are not accepted for reproducible release.
- Known vulnerability is triaged by exploitability, exposure and impact, not CVSS alone.
- Critical exploitable vulnerability in a Production path blocks release until remediation or an explicitly documented security-risk decision that cannot override a frozen mandatory security gate.
- Dependency update affecting DB/API/calculation/sync/security passes the relevant regression suites.
- Emergency security dependency update may use an expedited release path but cannot skip integrity-critical tests.
- Unsupported/EOL runtime or library without security fixes is planned for replacement before becoming a Production security gap.
- Vulnerability-scan output never includes secrets.

## 25. Capacity and performance operations
- Capacity monitoring includes DB storage, object storage, DB connections, job backlog and platform-relevant resource saturation.
- Capacity warning occurs before hard exhaustion.
- Scaling/capacity change cannot relax tenant isolation, durability or transaction guarantees.
- Performance baseline is measured on representative Production-like workload in Staging.
- V6 does not introduce an arbitrary universal API latency SLA absent from the frozen product contract.
- Significant regression from the approved release baseline is investigated before Production promotion.
- Large sync backlog, report generation and package download are evaluated separately because their workload characteristics differ.
- Capacity testing includes Evidence/object storage, not only PostgreSQL/API.
- Performance optimization cannot change calculation precision, snapshot content or business semantics.
- Load shedding/degradation, if needed, favors non-critical derived/diagnostic work before risking authoritative-write corruption/loss.

## 26. Android release, distribution and compatibility
- Production Android package has a unique monotonically increasing versionCode and human-readable versionName.
- Android compatibility verification covers target-SDK platform behavior relevant to the supported release, including enforced edge-to-edge/system-inset behavior and predictive-back behavior on applicable Android versions.
- Release verification exercises system bars, display cutouts, gesture navigation, IME/insets, predictive back and process recreation on the representative device/window matrix from Doc18.
- SiloMath does not request Android precise/coarse location permission for the V6 facility-location map; facility coordinates are master data, not device/user tracking.
- Any map/basemap provider configuration is environment-controlled and must follow Doc17 data-minimization rules. Provider SDK/tiles may receive only the minimum technical map requests needed to render the basemap; SiloMath credentials, tenant payloads and protected business objects are not sent to the map provider.
- Map/basemap provider outage or unavailable cached tiles degrades only the visual map layer. Authoritative Location list/detail, hierarchy, local facility coordinates and supported offline business operations remain available from SiloMath local data.
- Production package is signed with a stable protected application-signing identity.
- Distribution channel preserves authenticity/integrity of the signed artifact.
- Release never deletes Room/offline data through destructive migration.
- Upgrade from every supported Production schema/client baseline is migration-tested according to Doc18.
- Backend rollout preserves compatibility required by supported clients and queued offline operations.
- A client too old for safe protocol/schema behavior receives explicit controlled incompatibility/update handling rather than silent data corruption.
- Forced update cannot be used to delete unsynced local business data.
- Android crash/error telemetry correlates with versionCode/versionName/environment.
- Min SDK remains 26; Production support matrix cannot go below the frozen baseline without a change decision.

## 27. Maintenance and change windows
- Planned Production change has a change record with scope, risk, validation, recovery/rollback strategy and responsible operator.
- Risky DB/storage/security change uses a planned maintenance/change window when impact requires it.
- Maintenance never justifies bypassing retention, tenant isolation or immutable-history controls.
- Client-visible planned outage/degradation is communicated through an approved operational channel; Notification Center is not automatically assumed to be an external status service.
- Emergency change may precede a normal window for P1 containment but is documented and retrospectively validated.
- Concurrent risky changes are avoided when they would make diagnosis/rollback ambiguous.
- Change completion requires validation, not merely successful execution of a deployment command.
- Failed change enters incident/recovery path and is not hidden through manual state alteration.

## 28. Privileged operational access
- Production operational access uses named individual identities; shared permanent administrator credentials are prohibited.
- Privileged access follows least privilege and environment separation.
- Normal application Admin role does not imply infrastructure/database/platform administrator privileges.
- Production privileged actions are logged/auditable.
- Direct Production DB access is restricted and is not a normal business-support workflow.
- Emergency elevated access is time-bounded/revocable where supported and associated with an incident/change record.
- Personnel/access change triggers prompt revocation of access no longer required.
- Privileged operator cannot bypass immutable Final Snapshot, retention hold or tenant rules through normal tooling.
- Read-only diagnostic access is preferred to write access when investigation does not require mutation.
- Production data export for support/investigation is minimized and controlled according to Doc17/Doc19.

## 29. Doc19 retention, purge and archive jobs
- Retention eligibility evaluation runs server-side as a scheduled operational job.
- The job is idempotent and can safely resume after interruption.
- The job checks expiry, RetentionHold, dependencies, sync/conflict state and class-specific guards before disposition.
- Purge/archive batch size is bounded to avoid long unbounded transactions/resource spikes.
- Job metrics include examined, retained, archived, purged, skipped-by-hold, skipped-by-dependency and failed counts.
- Archive verification must succeed before source purge.
- Purge failure is not auto-converted to success on the next schedule without explicit retry/result tracking.
- Retention job never uses destructive cascade to avoid dependency checks.
- Backup restore reapplies disposition/tombstone controls before full service restoration.
- Manual exceptional disposition remains privileged and auditable according to Doc19.

## 30. Mandatory operational runbooks
- V6 has a runbook for Production deployment and smoke verification.
- V6 has a runbook for failed deployment and rollback or forward-fix.
- V6 has a runbook for database migration failure.
- V6 has a runbook for backup failure.
- V6 has a runbook for PITR, full restore and disaster recovery.
- V6 has a runbook for sync backlog/conflict surge.
- V6 has a runbook for Evidence/object-storage outage or integrity failure.
- V6 has a runbook for database capacity/availability incident.
- V6 has a runbook for security/credential compromise.
- V6 has a runbook for suspected tenant-isolation breach.
- V6 has a runbook for retention/archive/purge job failure.
- V6 has a runbook for calculation/report integrity incident.
- Every runbook defines detection/entry criteria, immediate safeguards, diagnostics, decision points, recovery, validation, escalation and closure evidence.
- No runbook may instruct an operator to bypass frozen business/security/integrity controls for a quick fix.

## 31. Exact Production smoke checklist
- Confirm deployed application/backend version and environment identity.
- Confirm backend liveness and readiness.
- Confirm PostgreSQL connectivity/health and expected schema migration version.
- Confirm object-storage health required by the Evidence path.
- Confirm authentication path using the designated non-personal Production smoke account/procedure.
- Confirm authorized tenant-scoped read and verify cross-tenant data is not exposed.
- Confirm representative Campaign/list read path.
- Confirm sync status/service path is reachable and no systemic queue failure appeared after release.
- Confirm report service can execute the approved non-destructive smoke path.
- Confirm monitoring/logging/correlation and alert pipeline receives expected deployment/health telemetry.
- Confirm backup/PITR monitoring remained healthy through deployment.
- Confirm no unexpected spike in API errors, crashes, DB failures, Evidence failures or sync failures during smoke observation.
- Smoke procedure must not create fake official inventory, Close a real Campaign or mutate immutable history.
- Smoke result is recorded against the exact release artifact.
- Any integrity/security failure causes smoke FAIL regardless of other successful checks.

## 32. Production operational acceptance gates
- Exact Production artifact passed Doc18 mandatory release gates.
- Staging validation/UAT corresponds to the promoted release lineage.
- Production environment contains no debug endpoint/trust override or embedded secret.
- Required PostgreSQL migration validation is PASS.
- Required Room migration regression is PASS.
- Calculation golden regression is PASS when calculation-affecting code changed.
- Backup is current and PITR continuity satisfies RPO <=1h.
- No unresolved P1 affects safe release.
- Required backend/DB/object-storage health is green before promotion.
- Protected artifact signing/provenance is verified.
- Retention/purge/archive changes have Doc19/Doc18 regression PASS.
- Production smoke checklist PO-D249-PO-D263 passes after deployment.
- Tenant-isolation/security-boundary smoke or mandatory verification has no failure.
- No known Final Snapshot/calculation/report integrity failure is open for the released path.
- Deployment is not declared healthy until post-deploy validation succeeds.
- Failed mandatory operational gate cannot be waived merely by relabeling severity or artifact.
- Emergency P1 containment may use expedited deployment, but integrity/security-critical verification remains mandatory.
- Quarterly restore verification must not be overdue for Production to claim DR capability verified.
- Secrets/signing credentials required for release are valid, controlled and not known compromised.
- Monitoring and alerting required for changed components are operational.
- Release/change/incident evidence is retained under Doc19.
- Operational acceptance does not supersede business acceptance; both Doc18 and Doc20 gates must pass.
- Android release evidence includes applicable R8/full-mode, Baseline/Startup Profile and Macrobenchmark verification from Doc18.
- Android compatibility evidence has no unresolved edge-to-edge, inset/IME, predictive-back, adaptive-window or critical accessibility regression for the supported release matrix.
- When map presentation changed, Location List remains authoritative and operational under map-provider/basemap failure, and map privacy/data-minimization verification passes.

## 33. Mandatory runbook inventory

1. Production deployment / smoke verification
2. Failed deployment / rollback or forward-fix
3. Database migration failure
4. Backup failure
5. PITR / full restore / disaster recovery
6. Sync backlog / conflict surge
7. Evidence / object-storage outage or integrity failure
8. Database capacity / availability incident
9. Security / credential compromise
10. Suspected tenant-isolation breach
11. Retention / archive / purge job failure
12. Calculation / report integrity incident

## 34. Operational execution sequence

- Build the immutable release candidate in protected CI/CD.
- Execute Doc18 mandatory tests and release gates against that exact lineage, including applicable visual/adaptive/accessibility and Android performance-engineering evidence.
- Validate PostgreSQL and Room migrations; rehearse risky migrations on representative populated data.
- Promote to Staging and complete required validation/UAT.
- Verify current backup/PITR continuity and required recovery point before risky change.
- Authorize and deploy the approved artifact to Production.
- Execute the non-destructive PO-D249-PO-D263 Production smoke checklist.
- Declare VERIFIED HEALTHY only after integrity/security/health validation passes.
- Monitor post-deployment signals and enter incident/recovery workflow on failed mandatory conditions.
- Retain release/change/incident evidence under Doc19.

## 35. PO-D01-PO-D293 approved decision register

| ID | Area | Frozen V6 outcome |
|---|---|---|
| **PO-D001** | Production operating model | Doc20 is the V6 authority for deployment, runtime operations, observability, backup/recovery, incident handling and operational runbooks. |
| **PO-D002** | Production operating model | There are exactly three environment classes: Development, Staging and Production. |
| **PO-D003** | Production operating model | Production is the only environment that serves official business operations/data. |
| **PO-D004** | Production operating model | Staging is Production-like for schema, API/security configuration, object-storage behavior, migration rehearsal and UAT, but does not implicitly use Production business data. |
| **PO-D005** | Production operating model | Development is for implementation/debugging and does not use Production credentials/secrets. |
| **PO-D006** | Production operating model | Environment isolation includes at minimum API/backend, database, object storage, secrets and operational telemetry context. |
| **PO-D007** | Production operating model | Production failure handling favors data integrity over availability. |
| **PO-D008** | Production operating model | If integrity required for Close is uncertain, Close is blocked; valid offline Stocktaking may continue only within the verified working set. |
| **PO-D009** | Build variants and environment configuration | Android uses environment-aware build configuration for development/debug, staging and Production release. |
| **PO-D010** | Build variants and environment configuration | Production artifact uses only Production API/environment configuration. |
| **PO-D011** | Build variants and environment configuration | Debug/staging endpoint or trust override cannot be present or active in Production artifact. |
| **PO-D012** | Build variants and environment configuration | Non-secret environment values may be injected through Gradle/build configuration. |
| **PO-D013** | Build variants and environment configuration | Secrets are not Gradle constants, source code, Git, APK resources or plaintext configuration files. |
| **PO-D014** | Build variants and environment configuration | Backend configuration is externalized through environment/runtime configuration and secret-management facilities. |
| **PO-D015** | Build variants and environment configuration | Environment identity is operationally observable to prevent accidental deployment to the wrong environment. |
| **PO-D016** | Build variants and environment configuration | Production configuration change is a controlled operational change and is not performed through Organization Settings. |
| **PO-D017** | CI/CD and artifact provenance | Production build is created only by a protected CI/CD pipeline. |
| **PO-D018** | CI/CD and artifact provenance | Release artifact is not built manually on a workstation and then promoted as a Production artifact. |
| **PO-D019** | CI/CD and artifact provenance | Each release candidate has immutable version/build identity. |
| **PO-D020** | CI/CD and artifact provenance | Pipeline preserves the link between source revision/commit, build identity, test evidence and released artifact. |
| **PO-D021** | CI/CD and artifact provenance | Production Android artifact is signed only through a protected signing mechanism. |
| **PO-D022** | CI/CD and artifact provenance | Signing keys are not kept in the repository or developer workstation as the normal release mechanism. |
| **PO-D023** | CI/CD and artifact provenance | Dependency acquisition/build is reproducible within reasonable limits through pinned/version-controlled build configuration. |
| **PO-D024** | CI/CD and artifact provenance | Release evidence from Doc18 is associated with the exact promoted artifact, not an almost-identical build. |
| **PO-D025** | CI/CD and artifact provenance | An artifact that did not pass the exact mandatory release gates cannot be promoted merely by changing a label/tag. |
| **PO-D026** | Release path and promotion | Frozen release path remains Build -> Automated Tests -> Migration Validation -> Staging -> Production -> Smoke Test. |
| **PO-D027** | Release path and promotion | Doc18 exact release gates must be PASS before Production promotion. |
| **PO-D028** | Release path and promotion | Staging validation uses the same release-candidate logic/artifact lineage intended for Production. |
| **PO-D029** | Release path and promotion | UAT sign-off refers to an identifiable release candidate. |
| **PO-D030** | Release path and promotion | Production deployment requires explicit release authorization/approval in the pipeline. |
| **PO-D031** | Release path and promotion | Production smoke tests are non-destructive. |
| **PO-D032** | Release path and promotion | Smoke tests verify at minimum service/API health, authentication path, authorized read path and required client compatibility without arbitrarily creating or closing an official Campaign. |
| **PO-D033** | Release path and promotion | Failed Production smoke test triggers incident/release-response workflow; the release is not declared healthy merely because technical deployment completed. |
| **PO-D034** | Release path and promotion | Release status distinguishes DEPLOYED from VERIFIED HEALTHY; these are operational states, not business enums. |
| **PO-D035** | Database migration operations | All Production schema migrations are versioned and reviewable. |
| **PO-D036** | Database migration operations | Destructive migration of retained business/history data is prohibited. |
| **PO-D037** | Database migration operations | Risky migration uses Expand -> Migrate -> Contract. |
| **PO-D038** | Database migration operations | Expand introduces backward-compatible structures before old structures are removed. |
| **PO-D039** | Database migration operations | Migrate moves/backfills data with explicit validation and restart-safe execution. |
| **PO-D040** | Database migration operations | Contract runs only after verification that the new application no longer depends on the old structure and migration invariants are satisfied. |
| **PO-D041** | Database migration operations | Relevant risky Production migration/release requires a verified pre-change backup/recovery point. |
| **PO-D042** | Database migration operations | Migration rehearsal runs in Staging or isolated representative populated data before Production. |
| **PO-D043** | Database migration operations | Migration is fail-safe: failure cannot be masked as a successful schema state. |
| **PO-D044** | Database migration operations | Long-running backfill is resumable/idempotent or has an explicit recovery mechanism. |
| **PO-D045** | Database migration operations | Historical versions/snapshots are not rewritten to simplify migration. |
| **PO-D046** | Database migration operations | Room migration compatibility is a release gate separate from backend PostgreSQL migration validation. |
| **PO-D047** | Deployment compatibility and rollback | Production rollback prefers application rollback/forward-fix rather than destructive database downgrade. |
| **PO-D048** | Deployment compatibility and rollback | Schema migration is designed so the previous compatible application version can continue during the Expand/Migrate window where rollout strategy requires it. |
| **PO-D049** | Deployment compatibility and rollback | After an irreversible Contract step, rollback to an incompatible version is prohibited. |
| **PO-D050** | Deployment compatibility and rollback | If DB migration is applied but application rollout fails, response is compatible app rollback or forward-fix according to migration compatibility, not schema destruction. |
| **PO-D051** | Deployment compatibility and rollback | Restore from backup is a disaster-recovery action, not a normal release rollback mechanism. |
| **PO-D052** | Deployment compatibility and rollback | Rollback decision protects offline clients and pending SyncOperations; the server contract is not withdrawn in a way that turns valid queued operations into corruption. |
| **PO-D053** | Deployment compatibility and rollback | API compatibility changes use staged rollout/versioning according to the frozen /api/v1 contract and client compatibility. |
| **PO-D054** | Deployment compatibility and rollback | Production rollback/forward-fix is an auditable operational event. |
| **PO-D055** | Service health model | Backend exposes health/readiness signals sufficient for orchestration/monitoring without business payload. |
| **PO-D056** | Service health model | Liveness indicates process/service responsiveness; readiness indicates whether an instance can safely serve required dependencies. |
| **PO-D057** | Service health model | Readiness considers at minimum database connectivity and mandatory runtime configuration. |
| **PO-D058** | Service health model | Object-storage degradation may make Evidence operations unavailable without falsely declaring Evidence upload healthy. |
| **PO-D059** | Service health model | Health endpoint exposes no secrets, connection strings, stack traces or sensitive environment details. |
| **PO-D060** | Service health model | Health status does not replace business-integrity checks for Close, reporting or sync. |
| **PO-D061** | Observability, metrics and dashboards | Production observability has four base signals: logs, metrics, traces and alerts. |
| **PO-D062** | Observability, metrics and dashboards | All backend request paths use a correlation ID propagated through request processing and relevant downstream operations. |
| **PO-D063** | Observability, metrics and dashboards | Logs are structured and include at minimum UTC timestamp, severity, service/component, environment, correlation ID and safe event/message. |
| **PO-D064** | Observability, metrics and dashboards | Logs exclude passwords, access/refresh/reset tokens, secret keys, Evidence binary, arbitrary Notes and full sensitive business payloads. |
| **PO-D065** | Observability, metrics and dashboards | Production dashboard shows at minimum API health, error rate, request latency, DB health, sync health, Evidence upload health, job health and backup status. |
| **PO-D066** | Observability, metrics and dashboards | API metrics are segmented at least by endpoint/operation class and response category, without tenant-sensitive high-cardinality labels. |
| **PO-D067** | Observability, metrics and dashboards | Background-job metrics include last success, last failure, duration and next/expected execution where scheduled. |
| **PO-D068** | Observability, metrics and dashboards | Crash/exception telemetry is sanitized and correlates with application version/environment. |
| **PO-D069** | Observability, metrics and dashboards | Performance traces are mandatory for campaign package load, DB migration, sync batch and report generation. |
| **PO-D070** | Observability, metrics and dashboards | Calculation diagnostics expose engine/version/result-state metadata needed for troubleshooting, but not full sensitive calculation payload in Production logs. |
| **PO-D071** | Observability, metrics and dashboards | Monitoring failure does not block a business transaction after business persistence succeeded. |
| **PO-D072** | Observability, metrics and dashboards | Missing telemetry cannot transform a failed business operation into success. |
| **PO-D073** | Alerting, severities and thresholds | Operational alerts use P1 Critical, P2 High, P3 Medium and P4 Low severities. |
| **PO-D074** | Alerting, severities and thresholds | P1 means outage or immediate risk of data loss/corruption, tenant-isolation failure, official snapshot-integrity failure, or generalized inability to use Production service. |
| **PO-D075** | Alerting, severities and thresholds | P2 means major degradation without confirmed data corruption, including severe sync backlog, repeated job failure, Evidence service unavailability, backup failure or substantial API failure. |
| **PO-D076** | Alerting, severities and thresholds | P3 means limited/non-critical degradation, capacity warning, elevated retries or isolated operational fault. |
| **PO-D077** | Alerting, severities and thresholds | P4 means informational/maintenance condition requiring follow-up rather than immediate incident response. |
| **PO-D078** | Alerting, severities and thresholds | Any failed scheduled Production backup generates at least P2. |
| **PO-D079** | Alerting, severities and thresholds | Backup age that could violate RPO <=1h generates P1 before the recovery objective is silently violated. |
| **PO-D080** | Alerting, severities and thresholds | Confirmed tenant-isolation/security-boundary failure is P1. |
| **PO-D081** | Alerting, severities and thresholds | Confirmed immutable Final Snapshot corruption/inconsistency is P1. |
| **PO-D082** | Alerting, severities and thresholds | Alert thresholds for latency/error/backlog are baseline-derived and environment-configured but never redefine business rules or release acceptance. |
| **PO-D083** | Alerting, severities and thresholds | Threshold tuning is auditable operational configuration and is not an Organization setting. |
| **PO-D084** | Alerting, severities and thresholds | Alert deduplication/grouping is allowed; suppression cannot hide an unresolved P1. |
| **PO-D085** | Sync operational monitoring | Sync dashboard includes queue depth, oldest pending age, retry count and conflict count. |
| **PO-D086** | Sync operational monitoring | PENDING, IN_FLIGHT, ACKNOWLEDGED, permanent failure and conflicts are monitored separately. |
| **PO-D087** | Sync operational monitoring | Oldest pending age is measured separately for retryable and blocked/dependency/conflict conditions. |
| **PO-D088** | Sync operational monitoring | Persistent growth in queue depth/oldest age generates an operational alert before becoming systemic backlog. |
| **PO-D089** | Sync operational monitoring | Duplicate/idempotency anomaly is security/data-integrity relevant and is not treated merely as generic retry telemetry. |
| **PO-D090** | Sync operational monitoring | Pull/checkpoint failures and push failures are observable separately. |
| **PO-D091** | Sync operational monitoring | Package download/verification failures are observable separately from mutation sync. |
| **PO-D092** | Sync operational monitoring | Conflict count is not operationally resolved through auto-merge or auto-delete. |
| **PO-D093** | Sync operational monitoring | Operations cannot manually mark ACKNOWLEDGED without authoritative server acknowledgement. |
| **PO-D094** | Sync operational monitoring | Sync telemetry never contains the full mutation payload. |
| **PO-D095** | Evidence and object-storage monitoring | Evidence metrics include upload attempts/success/failure, verification failure, upload latency, orphan-cleanup result and storage availability. |
| **PO-D096** | Evidence and object-storage monitoring | SHA-256 mismatch/server verification failure is a data-integrity alert. |
| **PO-D097** | Evidence and object-storage monitoring | Evidence metadata committed without the required durable official object must be detectable and cannot remain a silent inconsistent state. |
| **PO-D098** | Evidence and object-storage monitoring | Object-storage authorization failure is distinguished from network/transient failure. |
| **PO-D099** | Evidence and object-storage monitoring | Repeated Evidence upload failure for official pending Evidence contributes to Close-blocking operational visibility. |
| **PO-D100** | Evidence and object-storage monitoring | Orphan cleanup reports examined/deleted/skipped/error counts without logging image bytes. |
| **PO-D101** | Evidence and object-storage monitoring | Object-storage capacity/quota warnings are detected before write exhaustion. |
| **PO-D102** | Evidence and object-storage monitoring | Object-storage cleanup follows Doc19 retention/hold/dependency rules; generic bucket cleanup cannot bypass those guards. |
| **PO-D103** | Report and calculation integrity monitoring | Report-generation metrics include request count, success/failure and generation duration per report type. |
| **PO-D104** | Report and calculation integrity monitoring | Final-report integrity failure or mismatch with the immutable authoritative dataset is a data-integrity incident and misleading export is blocked. |
| **PO-D105** | Report and calculation integrity monitoring | Calculation-engine version is observable for diagnostics/release correlation. |
| **PO-D106** | Report and calculation integrity monitoring | Unexpected calculation-invariant failure is captured as sanitized error telemetry and is never converted to zero/default quantity. |
| **PO-D107** | Report and calculation integrity monitoring | Calculation regression is not detected through Production telemetry instead of golden tests; the golden suite remains a release gate. |
| **PO-D108** | Report and calculation integrity monitoring | Historical calculation/report paths never invoke current formulas to repair old results. |
| **PO-D109** | Report and calculation integrity monitoring | Abnormal increase in calculation/report failures produces an alert, but statistical threshold never redefines calculation validity. |
| **PO-D110** | Report and calculation integrity monitoring | Dossier/Audit export authorization failures are monitored without logging protected output. |
| **PO-D111** | Database operational monitoring | PostgreSQL monitoring includes availability, connections/pool pressure, storage utilization, transaction failures, slow-query indicators, migration state and backup/PITR health. |
| **PO-D112** | Database operational monitoring | DB storage capacity has warning before exhaustion and critical alert before write failure. |
| **PO-D113** | Database operational monitoring | Repeated constraint failures are observable, but database constraints are never disabled for availability. |
| **PO-D114** | Database operational monitoring | Deadlock/transaction rollback rate is monitored. |
| **PO-D115** | Database operational monitoring | Migration execution has explicit start/end/result/version telemetry. |
| **PO-D116** | Database operational monitoring | A migration marked failed cannot be operationally rewritten as success without remediation and revalidation. |
| **PO-D117** | Database operational monitoring | Manual Production DB mutation is not a normal incident-resolution mechanism. |
| **PO-D118** | Database operational monitoring | Emergency privileged DB change, if absolutely necessary, requires a documented incident/change record, backup consideration, explicit validation and audit trail. |
| **PO-D119** | Database operational monitoring | DB monitoring does not expose query parameter values containing sensitive business data. |
| **PO-D120** | Database operational monitoring | Connection-pool exhaustion and inability to establish DB connection affect readiness and alerting. |
| **PO-D121** | Backup and PITR mechanics | PostgreSQL has automated full/base backup at least daily. |
| **PO-D122** | Backup and PITR mechanics | Backup retention is minimum 30 days, according to Doc19. |
| **PO-D123** | Backup and PITR mechanics | To support RPO <=1h, transaction/WAL/PITR recovery points are captured so the maximum targeted recoverable data gap is <=60 minutes. |
| **PO-D124** | Backup and PITR mechanics | PITR is enabled in Production where supported; a platform/configuration unable to satisfy RPO <=1h is not Production-acceptable. |
| **PO-D125** | Backup and PITR mechanics | Backup success means the artifact is created and cataloged; job start alone is not success. |
| **PO-D126** | Backup and PITR mechanics | Relevant risky migration/release has a verified pre-change recovery point/backup before change. |
| **PO-D127** | Backup and PITR mechanics | Backup storage is access-controlled and encrypted at rest. |
| **PO-D128** | Backup and PITR mechanics | Backup credentials are separate from normal application credentials. |
| **PO-D129** | Backup and PITR mechanics | Backup failure does not delete or replace the last known-good backup chain. |
| **PO-D130** | Backup and PITR mechanics | Backup inventory/age/last-success/PITR continuity are monitored. |
| **PO-D131** | Backup and PITR mechanics | Object-storage Evidence has recovery protection sufficient for official Evidence retention; DB backup alone is not complete disaster recovery. |
| **PO-D132** | Backup and PITR mechanics | Restore from backup follows Doc19 disposition reapplication so expired/purged data does not become permanently active. |
| **PO-D133** | Restore and disaster recovery | Restore test is executed at least quarterly in an isolated environment. |
| **PO-D134** | Restore and disaster recovery | Restore test uses real backup/recovery artifacts of the Production mechanism without modifying Production. |
| **PO-D135** | Restore and disaster recovery | Restore verification includes DB integrity, schema version, representative immutable history, tenant isolation, Evidence references and ability to start a compatible backend. |
| **PO-D136** | Restore and disaster recovery | PITR exercise verifies selection of a recovery point and recoverability within the RPO. |
| **PO-D137** | Restore and disaster recovery | DR procedure explicitly follows declare incident -> select recovery point -> restore -> validate -> reapply disposition controls -> controlled service restoration. |
| **PO-D138** | Restore and disaster recovery | Restored environment receives no Production traffic before integrity validation. |
| **PO-D139** | Restore and disaster recovery | If recovered state has consistency uncertainty, Close remains blocked. |
| **PO-D140** | Restore and disaster recovery | DR never uses manual recreation of Final Snapshot as a substitute for restore. |
| **PO-D141** | Restore and disaster recovery | DR exercise produces retained operational evidence according to Doc19/Doc18. |
| **PO-D142** | Restore and disaster recovery | Failed quarterly restore test is at least P2 and blocks the claim that DR capability is verified. |
| **PO-D143** | RPO and RTO verification | RPO <=1h means at most one hour of authoritative committed Production data loss at the declared recovery point. |
| **PO-D144** | RPO and RTO verification | RTO <=4h is measured from declaration of a service-disrupting recovery incident to controlled restoration of required Production service. |
| **PO-D145** | RPO and RTO verification | RTO is not met merely because HTTP responds when integrity validation is incomplete. |
| **PO-D146** | RPO and RTO verification | Quarterly DR evidence measures achieved RPO and achieved RTO. |
| **PO-D147** | RPO and RTO verification | A test exceeding RPO/RTO is a failed recovery objective, not PASS with note. |
| **PO-D148** | RPO and RTO verification | Production recovery architecture is remediated if it repeatedly cannot demonstrate the objectives. |
| **PO-D149** | RPO and RTO verification | Evidence/object storage is included in DR readiness; DB-only restoration does not prove full SiloMath recovery. |
| **PO-D150** | RPO and RTO verification | Offline device copies are not authoritative backup mechanisms for RPO/RTO. |
| **PO-D151** | Incident severity and response | Incident severity uses the same P1-P4 levels as alerting. |
| **PO-D152** | Incident severity and response | P1 requires immediate response/escalation and incident ownership. |
| **PO-D153** | Incident severity and response | P2 requires prompt operational response and tracked remediation. |
| **PO-D154** | Incident severity and response | P3/P4 enter normal tracked operational work according to impact. |
| **PO-D155** | Incident severity and response | Every P1/P2 has an incident record with timeline, impact, decisions, mitigation, recovery and closure. |
| **PO-D156** | Incident severity and response | A data-integrity incident is not closed merely because service availability returned; integrity must be verified. |
| **PO-D157** | Incident severity and response | Incident actions modifying Production data/configuration are auditable. |
| **PO-D158** | Incident severity and response | Post-incident review is mandatory for P1 and for P2 with data/security/recovery impact. |
| **PO-D159** | Incident severity and response | Post-incident review tracks root cause and preventive actions without rewriting frozen business history. |
| **PO-D160** | Incident severity and response | Incident evidence follows Doc17 privacy/security and Doc19 retention. |
| **PO-D161** | Security incident operations | Suspected credential compromise permits rapid revocation/rotation of affected secret/session material. |
| **PO-D162** | Security incident operations | Confirmed tenant-isolation breach is P1. |
| **PO-D163** | Security incident operations | Security incident preserves relevant telemetry through RetentionHold when necessary. |
| **PO-D164** | Security incident operations | Incident investigation never justifies temporary logging of passwords, tokens or Evidence binaries. |
| **PO-D165** | Security incident operations | Production access may be further restricted during an incident without disabling authorization/tenant guards. |
| **PO-D166** | Security incident operations | Compromised signing/secret material is never reused after confirmed compromise. |
| **PO-D167** | Security incident operations | Security remediation is verified in Staging/controlled path before normal deployment, except emergency containment that is subsequently formalized and validated. |
| **PO-D168** | Security incident operations | Security event and remediation preserve correlation/timeline evidence without unnecessary personal/business payload. |
| **PO-D169** | Secrets and key rotation | Production secrets are managed through an approved secret store/runtime identity mechanism. |
| **PO-D170** | Secrets and key rotation | Secrets are not stored in Git, APK, logs or normal diagnostics. |
| **PO-D171** | Secrets and key rotation | Secrets are scoped per environment; Dev/Staging secrets do not provide Production access. |
| **PO-D172** | Secrets and key rotation | Database, object-storage, CI signing/deployment and other privileged credentials use least privilege. |
| **PO-D173** | Secrets and key rotation | Secret rotation permits controlled replacement without source-code change. |
| **PO-D174** | Secrets and key rotation | Rotation is mandatory on confirmed/suspected compromise and on ownership/access change that requires it. |
| **PO-D175** | Secrets and key rotation | V6 does not invent a universal periodicity for every secret type; provider/security policy may require a stricter interval. |
| **PO-D176** | Secrets and key rotation | Rotation success is verified before old credential revocation when overlap is needed for availability. |
| **PO-D177** | Secrets and key rotation | Old credential is revoked after successful cutover and does not remain an indefinite fallback. |
| **PO-D178** | Secrets and key rotation | Secret access/rotation privileged operations are auditable without recording secret value. |
| **PO-D179** | Dependency and vulnerability management | CI performs dependency vulnerability and license checks according to frozen architecture. |
| **PO-D180** | Dependency and vulnerability management | Production dependencies are version-controlled; uncontrolled floating dependencies are not accepted for reproducible release. |
| **PO-D181** | Dependency and vulnerability management | Known vulnerability is triaged by exploitability, exposure and impact, not CVSS alone. |
| **PO-D182** | Dependency and vulnerability management | Critical exploitable vulnerability in a Production path blocks release until remediation or an explicitly documented security-risk decision that cannot override a frozen mandatory security gate. |
| **PO-D183** | Dependency and vulnerability management | Dependency update affecting DB/API/calculation/sync/security passes the relevant regression suites. |
| **PO-D184** | Dependency and vulnerability management | Emergency security dependency update may use an expedited release path but cannot skip integrity-critical tests. |
| **PO-D185** | Dependency and vulnerability management | Unsupported/EOL runtime or library without security fixes is planned for replacement before becoming a Production security gap. |
| **PO-D186** | Dependency and vulnerability management | Vulnerability-scan output never includes secrets. |
| **PO-D187** | Capacity and performance operations | Capacity monitoring includes DB storage, object storage, DB connections, job backlog and platform-relevant resource saturation. |
| **PO-D188** | Capacity and performance operations | Capacity warning occurs before hard exhaustion. |
| **PO-D189** | Capacity and performance operations | Scaling/capacity change cannot relax tenant isolation, durability or transaction guarantees. |
| **PO-D190** | Capacity and performance operations | Performance baseline is measured on representative Production-like workload in Staging. |
| **PO-D191** | Capacity and performance operations | V6 does not introduce an arbitrary universal API latency SLA absent from the frozen product contract. |
| **PO-D192** | Capacity and performance operations | Significant regression from the approved release baseline is investigated before Production promotion. |
| **PO-D193** | Capacity and performance operations | Large sync backlog, report generation and package download are evaluated separately because their workload characteristics differ. |
| **PO-D194** | Capacity and performance operations | Capacity testing includes Evidence/object storage, not only PostgreSQL/API. |
| **PO-D195** | Capacity and performance operations | Performance optimization cannot change calculation precision, snapshot content or business semantics. |
| **PO-D196** | Capacity and performance operations | Load shedding/degradation, if needed, favors non-critical derived/diagnostic work before risking authoritative-write corruption/loss. |
| **PO-D197** | Android release, distribution and compatibility | Production Android package has a unique monotonically increasing versionCode and human-readable versionName. |
| **PO-D198** | Android release, distribution and compatibility | Production package is signed with a stable protected application-signing identity. |
| **PO-D199** | Android release, distribution and compatibility | Distribution channel preserves authenticity/integrity of the signed artifact. |
| **PO-D200** | Android release, distribution and compatibility | Release never deletes Room/offline data through destructive migration. |
| **PO-D201** | Android release, distribution and compatibility | Upgrade from every supported Production schema/client baseline is migration-tested according to Doc18. |
| **PO-D202** | Android release, distribution and compatibility | Backend rollout preserves compatibility required by supported clients and queued offline operations. |
| **PO-D203** | Android release, distribution and compatibility | A client too old for safe protocol/schema behavior receives explicit controlled incompatibility/update handling rather than silent data corruption. |
| **PO-D204** | Android release, distribution and compatibility | Forced update cannot be used to delete unsynced local business data. |
| **PO-D205** | Android release, distribution and compatibility | Android crash/error telemetry correlates with versionCode/versionName/environment. |
| **PO-D206** | Android release, distribution and compatibility | Min SDK remains 26; Production support matrix cannot go below the frozen baseline without a change decision. |
| **PO-D207** | Maintenance and change windows | Planned Production change has a change record with scope, risk, validation, recovery/rollback strategy and responsible operator. |
| **PO-D208** | Maintenance and change windows | Risky DB/storage/security change uses a planned maintenance/change window when impact requires it. |
| **PO-D209** | Maintenance and change windows | Maintenance never justifies bypassing retention, tenant isolation or immutable-history controls. |
| **PO-D210** | Maintenance and change windows | Client-visible planned outage/degradation is communicated through an approved operational channel; Notification Center is not automatically assumed to be an external status service. |
| **PO-D211** | Maintenance and change windows | Emergency change may precede a normal window for P1 containment but is documented and retrospectively validated. |
| **PO-D212** | Maintenance and change windows | Concurrent risky changes are avoided when they would make diagnosis/rollback ambiguous. |
| **PO-D213** | Maintenance and change windows | Change completion requires validation, not merely successful execution of a deployment command. |
| **PO-D214** | Maintenance and change windows | Failed change enters incident/recovery path and is not hidden through manual state alteration. |
| **PO-D215** | Privileged operational access | Production operational access uses named individual identities; shared permanent administrator credentials are prohibited. |
| **PO-D216** | Privileged operational access | Privileged access follows least privilege and environment separation. |
| **PO-D217** | Privileged operational access | Normal application Admin role does not imply infrastructure/database/platform administrator privileges. |
| **PO-D218** | Privileged operational access | Production privileged actions are logged/auditable. |
| **PO-D219** | Privileged operational access | Direct Production DB access is restricted and is not a normal business-support workflow. |
| **PO-D220** | Privileged operational access | Emergency elevated access is time-bounded/revocable where supported and associated with an incident/change record. |
| **PO-D221** | Privileged operational access | Personnel/access change triggers prompt revocation of access no longer required. |
| **PO-D222** | Privileged operational access | Privileged operator cannot bypass immutable Final Snapshot, retention hold or tenant rules through normal tooling. |
| **PO-D223** | Privileged operational access | Read-only diagnostic access is preferred to write access when investigation does not require mutation. |
| **PO-D224** | Privileged operational access | Production data export for support/investigation is minimized and controlled according to Doc17/Doc19. |
| **PO-D225** | Doc19 retention, purge and archive jobs | Retention eligibility evaluation runs server-side as a scheduled operational job. |
| **PO-D226** | Doc19 retention, purge and archive jobs | The job is idempotent and can safely resume after interruption. |
| **PO-D227** | Doc19 retention, purge and archive jobs | The job checks expiry, RetentionHold, dependencies, sync/conflict state and class-specific guards before disposition. |
| **PO-D228** | Doc19 retention, purge and archive jobs | Purge/archive batch size is bounded to avoid long unbounded transactions/resource spikes. |
| **PO-D229** | Doc19 retention, purge and archive jobs | Job metrics include examined, retained, archived, purged, skipped-by-hold, skipped-by-dependency and failed counts. |
| **PO-D230** | Doc19 retention, purge and archive jobs | Archive verification must succeed before source purge. |
| **PO-D231** | Doc19 retention, purge and archive jobs | Purge failure is not auto-converted to success on the next schedule without explicit retry/result tracking. |
| **PO-D232** | Doc19 retention, purge and archive jobs | Retention job never uses destructive cascade to avoid dependency checks. |
| **PO-D233** | Doc19 retention, purge and archive jobs | Backup restore reapplies disposition/tombstone controls before full service restoration. |
| **PO-D234** | Doc19 retention, purge and archive jobs | Manual exceptional disposition remains privileged and auditable according to Doc19. |
| **PO-D235** | Mandatory operational runbooks | V6 has a runbook for Production deployment and smoke verification. |
| **PO-D236** | Mandatory operational runbooks | V6 has a runbook for failed deployment and rollback or forward-fix. |
| **PO-D237** | Mandatory operational runbooks | V6 has a runbook for database migration failure. |
| **PO-D238** | Mandatory operational runbooks | V6 has a runbook for backup failure. |
| **PO-D239** | Mandatory operational runbooks | V6 has a runbook for PITR, full restore and disaster recovery. |
| **PO-D240** | Mandatory operational runbooks | V6 has a runbook for sync backlog/conflict surge. |
| **PO-D241** | Mandatory operational runbooks | V6 has a runbook for Evidence/object-storage outage or integrity failure. |
| **PO-D242** | Mandatory operational runbooks | V6 has a runbook for database capacity/availability incident. |
| **PO-D243** | Mandatory operational runbooks | V6 has a runbook for security/credential compromise. |
| **PO-D244** | Mandatory operational runbooks | V6 has a runbook for suspected tenant-isolation breach. |
| **PO-D245** | Mandatory operational runbooks | V6 has a runbook for retention/archive/purge job failure. |
| **PO-D246** | Mandatory operational runbooks | V6 has a runbook for calculation/report integrity incident. |
| **PO-D247** | Mandatory operational runbooks | Every runbook defines detection/entry criteria, immediate safeguards, diagnostics, decision points, recovery, validation, escalation and closure evidence. |
| **PO-D248** | Mandatory operational runbooks | No runbook may instruct an operator to bypass frozen business/security/integrity controls for a quick fix. |
| **PO-D249** | Exact Production smoke checklist | Confirm deployed application/backend version and environment identity. |
| **PO-D250** | Exact Production smoke checklist | Confirm backend liveness and readiness. |
| **PO-D251** | Exact Production smoke checklist | Confirm PostgreSQL connectivity/health and expected schema migration version. |
| **PO-D252** | Exact Production smoke checklist | Confirm object-storage health required by the Evidence path. |
| **PO-D253** | Exact Production smoke checklist | Confirm authentication path using the designated non-personal Production smoke account/procedure. |
| **PO-D254** | Exact Production smoke checklist | Confirm authorized tenant-scoped read and verify cross-tenant data is not exposed. |
| **PO-D255** | Exact Production smoke checklist | Confirm representative Campaign/list read path. |
| **PO-D256** | Exact Production smoke checklist | Confirm sync status/service path is reachable and no systemic queue failure appeared after release. |
| **PO-D257** | Exact Production smoke checklist | Confirm report service can execute the approved non-destructive smoke path. |
| **PO-D258** | Exact Production smoke checklist | Confirm monitoring/logging/correlation and alert pipeline receives expected deployment/health telemetry. |
| **PO-D259** | Exact Production smoke checklist | Confirm backup/PITR monitoring remained healthy through deployment. |
| **PO-D260** | Exact Production smoke checklist | Confirm no unexpected spike in API errors, crashes, DB failures, Evidence failures or sync failures during smoke observation. |
| **PO-D261** | Exact Production smoke checklist | Smoke procedure must not create fake official inventory, Close a real Campaign or mutate immutable history. |
| **PO-D262** | Exact Production smoke checklist | Smoke result is recorded against the exact release artifact. |
| **PO-D263** | Exact Production smoke checklist | Any integrity/security failure causes smoke FAIL regardless of other successful checks. |
| **PO-D264** | Production operational acceptance gates | Exact Production artifact passed Doc18 mandatory release gates. |
| **PO-D265** | Production operational acceptance gates | Staging validation/UAT corresponds to the promoted release lineage. |
| **PO-D266** | Production operational acceptance gates | Production environment contains no debug endpoint/trust override or embedded secret. |
| **PO-D267** | Production operational acceptance gates | Required PostgreSQL migration validation is PASS. |
| **PO-D268** | Production operational acceptance gates | Required Room migration regression is PASS. |
| **PO-D269** | Production operational acceptance gates | Calculation golden regression is PASS when calculation-affecting code changed. |
| **PO-D270** | Production operational acceptance gates | Backup is current and PITR continuity satisfies RPO <=1h. |
| **PO-D271** | Production operational acceptance gates | No unresolved P1 affects safe release. |
| **PO-D272** | Production operational acceptance gates | Required backend/DB/object-storage health is green before promotion. |
| **PO-D273** | Production operational acceptance gates | Protected artifact signing/provenance is verified. |
| **PO-D274** | Production operational acceptance gates | Retention/purge/archive changes have Doc19/Doc18 regression PASS. |
| **PO-D275** | Production operational acceptance gates | Production smoke checklist PO-D249-PO-D263 passes after deployment. |
| **PO-D276** | Production operational acceptance gates | Tenant-isolation/security-boundary smoke or mandatory verification has no failure. |
| **PO-D277** | Production operational acceptance gates | No known Final Snapshot/calculation/report integrity failure is open for the released path. |
| **PO-D278** | Production operational acceptance gates | Deployment is not declared healthy until post-deploy validation succeeds. |
| **PO-D279** | Production operational acceptance gates | Failed mandatory operational gate cannot be waived merely by relabeling severity or artifact. |
| **PO-D280** | Production operational acceptance gates | Emergency P1 containment may use expedited deployment, but integrity/security-critical verification remains mandatory. |
| **PO-D281** | Production operational acceptance gates | Quarterly restore verification must not be overdue for Production to claim DR capability verified. |
| **PO-D282** | Production operational acceptance gates | Secrets/signing credentials required for release are valid, controlled and not known compromised. |
| **PO-D283** | Production operational acceptance gates | Monitoring and alerting required for changed components are operational. |
| **PO-D284** | Production operational acceptance gates | Release/change/incident evidence is retained under Doc19. |
| **PO-D285** | Production operational acceptance gates | Operational acceptance does not supersede business acceptance; both Doc18 and Doc20 gates must pass. |
| **PO-D286** | Android release engineering | Production Android release uses R8 full optimization mode unless a documented reproducible toolchain incompatibility requires a temporary controlled exception. |
| **PO-D287** | Android release engineering | Baseline/Startup Profiles are generated/verified for startup and critical journeys according to Doc18. |
| **PO-D288** | Android release engineering | Macrobenchmark evidence is compared with the version-controlled engineering baseline before Production promotion when applicable. |
| **PO-D289** | Android platform compatibility | Release verification covers edge-to-edge/insets/IME, adaptive-window behavior, predictive back, process recreation and the supported Android/window matrix from Doc18. |
| **PO-D290** | Location map operations/privacy | The V6 facility-location map requires no device location permission; any map provider is environment-controlled and receives only minimum technical map requests, never SiloMath credentials or protected business payloads. |
| **PO-D291** | Location map resilience | Map-provider/basemap failure degrades the visual map only; authoritative Location list/detail, stored facility coordinates and supported offline workflows remain available. |
| **PO-D292** | Implementation closure | Non-semantic operational/tooling/provider choices that preserve Docs 01–21 require no Product/Baseline approval; architecturally material choices use the IADR process, while semantic changes require Baseline Change. |
| **PO-D293** | Environment endpoint provisioning | Concrete Development/Staging/Production hostnames/base URLs, certificates and provider endpoints are controlled deployment configuration, not Product Baseline constants; changing them within the frozen API/security contract requires operational change control, not Product/Baseline approval. |

## 36. Cross-document handoff and final-set consistency

- Doc18 remains the verification authority for mandatory test/release evidence; Doc20 operational gates add to, and never replace, Doc18 gates.
- Doc19 remains the retention/disposition authority; Doc20 operates retention, purge, archive, hold and restore re-disposition jobs without weakening Doc19.
- Doc17 remains the security/privacy authority for secrets, logging, authorization, tenant isolation and incident evidence.
- Doc12 remains the offline/sync behavior authority; operational monitoring cannot auto-merge, auto-ACK or discard unresolved work.
- Doc13 remains the calculation authority; Production operations cannot repair historical results by recalculation.
- Doc16 remains the reporting authority; operational availability cannot justify misleading or non-authoritative final exports.
- Documents 01–21 are the complete authoritative SiloMath V6 baseline. Doc21 is the canonical visual-design authority; Doc20 does not supersede its UI/design/accessibility requirements.
- The 2026-10-20 CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze is complete only when the 21-document cross-document audit passes with no known unresolved semantic gap that would require a Product/Baseline decision during implementation.

## 37. Freeze statement

This document is **APPROVED / FROZEN V6** and freezes the SiloMath V6 Production Operations contract defined by PO-D01-PO-D293, including environments, CI/CD and artifact provenance, release promotion, database migration and rollback, health/observability/alerting, sync/Evidence/report/database monitoring, backup/PITR and disaster recovery, RPO/RTO verification, incident and security operations, secret rotation, vulnerability management, capacity, Android release/performance/platform compatibility, Location-map operational resilience/privacy, maintenance/change control, privileged access, Doc19 operational jobs, mandatory runbooks, the exact Production smoke checklist and final operational acceptance gates. Non-semantic implementation choices that preserve Docs 01–21 require no Product/Baseline approval; architecturally material choices may use IADR, and any semantic/product change requires an approved Baseline Change.

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

Release evidence must identify supported full tuples, concrete handlers, golden vectors and parity results. Software artifact version and numerical engineVersion are distinct; approval alone does not satisfy deployment/release gates.

## Approved Country contract cross-reference

CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74: Release evidence must cover the complete ISO 3166-1 catalogue locally/cached, canonical uppercase alpha-2 new-write validation and UI-label/value separation. Controlled catalogue packaging/cache mechanics must not create a network-only dependency for supported offline Country selectors. This clarification assigns no new catalogue version identifier, refresh interval, endpoint or service. Existing non-conforming values require a separate audit before migration; no implicit normalization/backfill or external environment mutation is authorized.

## Approved Supabase production topology — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

Development, Staging and Production remain the only canonical environments. Each environment uses an isolated controlled Supabase backend context covering Auth, PostgreSQL schema/RLS/functions, Edge Functions, Storage, secrets/configuration and required telemetry. A separate Render/Ktor or equivalent external application-backend deployment is not part of the target core production topology.

The release sequence remains Build → Automated Tests → Migration Validation → Staging → Production → Smoke Test. Promotion must cover database migrations/functions, RLS/security policy, Edge Functions/API contracts, Storage configuration and Auth configuration as applicable. PostgreSQL backup/PITR, restore tests, RPO/RTO, integrity-first failure behavior, secret handling and incident evidence remain unchanged.

Core business-state availability/readiness is assessed at the Supabase platform boundaries actually used by SiloMath. No production gate may be declared green merely because an auxiliary external service is healthy; conversely, non-core CI/distribution/observability services may be used without becoming authoritative for core business state.

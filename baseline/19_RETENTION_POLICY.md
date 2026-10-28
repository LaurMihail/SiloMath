# SiloMath V6 - 19_RETENTION_POLICY

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-28 - CR-API-STORAGE-SPACE-BATCH-001 approved atomic Storage Space batch-create clarification

## 1. Purpose and authority

This document is the normative V6 retention, archival, purge and safe-cleanup contract. It consolidates frozen retention requirements from Documents 01-18 and the approved RT-D01-RT-D200 decisions. It does not redefine business lifecycle, authorization, immutable history or reporting semantics.

## 2. Core principles
- Doc19 is the V6 authority for retention, archival, purge and safe cleanup; it does not redefine business lifecycle.
- Retention is applied by data class, not through a generic global deletedAt.
- Minimum retention means data cannot be destroyed before the term; longer retention is permitted for an approved legal or operational requirement.
- Expiry does not mean instant deletion; the record becomes eligible for controlled disposition.
- Disposition is PURGE, ARCHIVE or RETAIN according to class and dependencies; these are retention actions, not business lifecycle statuses.
- Purge cannot break referential integrity, historical reproducibility, auditability or interpretation of retained snapshots.
- Data under legal/administrative hold is not deleted even when normal minimum retention has expired.
- Retention rules are system-controlled and are not editable from Organization Settings.

## 3. Exact retention matrix

| Data class | Minimum / cleanup window | Disposition rule |
|---|---|---|
| FinalInventorySnapshot + entries | 10 years after Close | Immutable; no hard delete during minimum retention. |
| CampaignSnapshot | 10 years after Close | Immutable; preserve launch context. |
| Required Stocktaking revisions / Measurements / CalculationResults | Through retained official snapshot dependency | No independent purge that breaks reproducibility. |
| Official validated/final Evidence | 10 years after Close | Metadata + official binary remain consistent. |
| Issue + resolution/status history | 10 years after Close | Issue Evidence follows longer applicable period. |
| Campaign-related Critical Audit | At least through Close + 10 years | Append-only; dependency may extend. |
| Non-Campaign Critical Audit | 10 years from event | Append-only. |
| Historically referenced master/version data | Through last retained historical dependency | No destructive deletion. |
| Notifications | 12 months | May purge after window; never source of truth. |
| ACK/SYNCED sync diagnostics | 90 days online | Then safe archive/purge. |
| Resolved SyncConflict | 90 days after resolution | Critical resolution remains in Audit. |
| Unresolved failed/conflict operations | No automatic purge | Retain until resolved/dispositioned safely. |
| Unreferenced DeviceInstallation | 90 days inactivity | Only when no retained dependency exists. |
| Operational application/server logs | 30 days online | Subject to incident hold. |
| Security telemetry | 90 days online | Subject to incident hold; Critical Audit separately long-term. |
| Temporary server report/export | 24 hours maximum | Derived artifact only. |
| Temporary support package | 7 days maximum | No prohibited payloads. |
| Production release evidence / UAT / RVM snapshot | 3 years after release | Exact release candidate evidence. |
| Routine non-release CI artifacts | 90 days | Unless defect/security hold. |
| Revoked/expired session non-secret metadata | 30 days | No usable credential retained. |
| Reset-token non-secret metadata | 30 days | No plaintext reset token retained. |
| PostgreSQL backups | Minimum 30 days | PITR per operational contract. |
| Temporary image-processing artifacts | Immediate safe cleanup | After canonicalization or safe abandonment. |
| Local synced PHOTO/cache | After durability + integrity + dependency guards | Official remote Evidence remains retained. |
| Data under ACTIVE RetentionHold | Until hold release | Normal expiry is suspended. |


## 4. Retention clocks
- FinalInventorySnapshot retention starts at Campaign.closedAt.
- CampaignSnapshot retention starts at Campaign.closedAt for a Completed Campaign; while Active it is mandatory.
- Official validated/final Stocktaking Evidence retention starts at Campaign Close.
- Issue plus resolution/status-history retention starts at Campaign Close.
- Campaign-related Critical AuditEvent is retained at least through Campaign.closedAt plus 10 years even when the event occurred earlier.
- Critical non-Campaign AuditEvent retention starts at event timestampUtc.
- Notification retention starts at notification creation timestamp.
- ACKNOWLEDGED sync diagnostic 90-day online window starts at authoritative acknowledgement/completion timestamp.
- Resolved SyncConflict cleanup clock starts at resolution timestamp; unresolved conflict has no active purge clock.

## 5. Official inventory history
- FinalInventorySnapshot plus entries: minimum 10 years after Close.
- Final Snapshot is immutable during retention and cannot be hard-deleted individually.
- CampaignSnapshot: minimum 10 years after Close.
- Stocktaking records/revisions, Measurements and CalculationResults needed to reproduce a retained Final Snapshot are retained at least as long as the snapshot they support.
- Frozen GeometryProfileVersion and CommodityVersion referenced by retained history are retained at least through the last retained historical reference.
- Client identity/frozen context needed by Third-Party history is preserved through snapshot/frozen metadata even if Client becomes Inactive.
- Historical User attribution needed by retained history is preserved through stable ID plus frozen display context.
- Completed Campaign header/context needed to interpret Final Snapshot is not destructively removed while the snapshot is retained.
- No authoritative child record is independently purged if doing so makes retained official history non-reproducible.

## 6. Master data and identity lifecycle
- Organization, Location, Storage Unit, Storage Space, Client, Geometry Profile and other master records use Active/Inactive lifecycle rather than delete for business retirement.
- A master record with no current or historical reference may become purge-eligible only if a future explicitly approved cleanup rule permits it; V6 has no user-facing hard delete.
- Location facility latitude/longitude, when present, are part of the Location master-data record and follow the same lifecycle, historical-reference protection, tenant offboarding, archive and privacy-disposition rules as that Location; there is no separate tracking/location-history retention stream.
- Historically referenced master data is not hard-deleted.
- Immutable historically referenced GeometryProfileVersion/CommodityVersion records are not independently purged from retained history.
- User lifecycle is Active/Disabled; Disable does not erase User identity or historical attribution.
- OrganizationMembership Active/Disabled is retained where authorization/audit history requires it; reactivation uses the same membership identity.
- CampaignTeamAssignment history is retained to interpret assignment/removal and launch context while the historical Campaign context is retained.

## 7. Evidence and Issues
- Official Evidence linked to validated/final history is retained minimum 10 years after Campaign Close.
- Evidence metadata and binary remain mutually consistent throughout retention.
- Official Evidence binary cannot be deleted while retaining only metadata before expiry unless a future explicit legal policy changes the historical contract.
- Working Evidence removed before validation may have its binary cleaned after removal is durable, synchronized and demonstrably unreferenced.
- Orphan cleanup requires proof that the binary is not referenced by active/validated Evidence, pending SyncOperation or unresolved conflict.
- Issue, IssueResolution and IssueStatusEvent are retained minimum 10 years after Campaign Close.
- Issue Evidence follows the longer of Evidence retention and Issue/history retention.
- Reset Validation never shortens retention of a prior validated revision or its Evidence.
- Evidence used by Final Snapshot/Dossier cannot be removed by device cache cleanup before remote durability/integrity is confirmed.

## 8. Audit retention
- Critical AuditEvent retention is minimum 10 years.
- Campaign-related critical audit is retained at least through closedAt plus 10 years.
- Critical audit independent of Campaign is retained minimum 10 years from timestampUtc.
- AuditEvent is append-only; retention purge cannot become selective history editing.
- If an AuditEvent is required to interpret a retained immutable record, it is not purged before that record.
- Before/after metadata follows AuditEvent retention while secrets and binaries remain prohibited regardless of retention duration.

## 9. Notifications, preferences and configuration
- Notifications are retained server-side for 12 months.
- After 12 months a Notification may be purged regardless of read/unread because it is not source of truth.
- Notification purge never deletes the referenced Campaign, Issue or Audit business state.
- UserPreference is retained while User identity is active/retained and may be replaced by current preference values; it is not historical business record.
- OrganizationConfiguration retains the current value plus AuditEvent history; configuration does not replace audit of changes.
- Old configuration values need not remain active rows when Audit before/after data and required snapshot context preserve necessary history.

## 10. Sync, conflict and operational queue
- ACKNOWLEDGED/SYNCED SyncOperation diagnostics have 90 days online retention, then safe archive/purge.
- Corresponding critical business events remain in long-term Audit after technical SyncOperation purge.
- PENDING/IN_FLIGHT operations are never purge-eligible.
- Permanent FAILED operation is not auto-purged while unresolved.
- OPEN SyncConflict is not auto-purged.
- RESOLVED SyncConflict remains at least through the approved diagnostic window; critical resolution remains in Audit.

## 11. DeviceInstallation and offline package/cache
- DeviceInstallation is a technical installation identity, not hardware identity and not permanent business history merely because it existed.
- DeviceInstallation is retained while referenced by session/security context, OfflineWorkingSetManifest, SyncOperation, SyncConflict or another required technical record.
- DeviceInstallation without active/retained references becomes purge-eligible after 90 days of inactivity.
- Purging DeviceInstallation never deletes AuditEvent or business history; retained records keep only already-frozen technical context needed for interpretation.
- OfflineWorkingSetManifest versions are retained locally while the current package or recovery/integrity diagnostics require them.
- After a new package is verified and atomically activated, superseded local versions may be cleaned when no dependency, conflict or recovery requirement remains.
- An Active Campaign package is not automatically removed while the user remains authorized and the package is needed for offline operation.
- A Completed Campaign working package may be compacted/removed after server Close is authoritative, local mutations are durable/resolved and required immutable history/report data remains available.
- A revoked/out-of-scope package may be removed only after protecting unsynced work/conflict evidence needed for recovery.
- Master cache may remove out-of-scope/inactive copies only when no retained local history or pending operation references them.

## 12. Local PHOTO and cache cleanup
- Canonical local PHOTO is retained until the remote object is durably confirmed, integrity-verified and no pending dependency/conflict requires local bytes.
- SYNCED alone does not justify cleanup when remote integrity/durability has not been confirmed.
- After all cleanup guards pass, local PHOTO bytes may be removed to save space; Evidence metadata and remoteObjectRef remain according to retention.
- Validated/final Evidence does not require a duplicate local copy for 10 years when the official remote binary is retained under contract.
- Local cleanup never changes Evidence business state, hash, metadata or historical references.
- If official remote Evidence cannot be verified, local cleanup is blocked until the integrity condition is resolved.
- Temporary image-processing files are removed immediately after successful canonicalization or safe abandonment.
- Source/original PHOTO may be released after the canonical JPEG is durable; the original never becomes official retained Evidence.

## 13. Generated reports and exports
- Generated PDF/CSV export is a derived artifact, not an authoritative inventory record.
- The server need not permanently retain every generated export when it can be reproduced from retained authoritative data.
- Server-side temporary generated export files have maximum 24-hour retention, then are purge-eligible.
- Client-side temporary/share-cache export files are best-effort cleaned after the workflow and no later than normal cache cleanup.
- An export explicitly saved by the user to user-selected storage leaves SiloMath lifecycle control; the application cannot later claim it has deleted that copy.
- Export cleanup never deletes report-generation AuditEvent or authoritative source data.
- Digital Inventory Dossier PDF follows the same derived-export rule; authoritative retained components are snapshot/evidence/audit/context, not the PDF copy.
- If a future generated document becomes an official legally retained artifact, that requires an explicit change decision; V6 does not assume it.

## 14. Diagnostics, logs and security telemetry
- Production application/server operational logs have 30 days online retention.
- Security telemetry relevant to incident investigation has 90 days online retention.
- A security event that is also Critical AuditEvent separately follows minimum 10-year Audit retention; technical telemetry may expire at 90 days.
- Diagnostic/support packages are not permanently retained by default.
- A generated support package stored temporarily server-side expires within maximum 7 days.
- Support-case attachment retention outside SiloMath is not defined by the V6 application retention contract.
- Logs, telemetry and support data never contain passwords, tokens, secret keys, Evidence binaries or prohibited full business payloads regardless of retention duration.
- Correlation IDs, safe entity IDs, versions, counts and timestamps may be retained through the diagnostic window.
- Active incident investigation may place a hold on relevant logs/telemetry before normal expiry.

## 15. Test evidence, CI and UAT
- Exact Production release evidence defined by Doc18 is retained minimum 3 years from release.
- Release evidence includes mandatory-suite results, RVM snapshot, migration validation/rehearsal evidence, security gates and UAT sign-off.
- Routine CI artifacts for non-release builds have 90 days retention.
- Failed CI diagnostic artifacts may be removed after 90 days unless under defect/security hold.
- UAT evidence for a Production release candidate follows the 3-year release-evidence retention.
- Test fixtures and synthetic test databases have no business-history retention and are purgeable after test/environment lifecycle.
- Test evidence must not include Production personal/business data when synthetic/sanitized evidence is sufficient.

## 16. Authentication, sessions and reset artifacts
- Active refresh/session records are retained only while valid/revocable plus the minimum technical grace required for replay/revocation enforcement.
- Expired/revoked refresh credentials are not retained as usable secrets.
- A non-reversible session identifier/hash plus revocation metadata may be retained for 30 days after expiry/revocation for replay/security investigation.
- Password-reset token is single-use and valid maximum 30 minutes; plaintext reset token is not retained after completion/expiry.
- Non-reversible reset-token identifier/security metadata may be retained 30 days for abuse/investigation.
- Password-reset request/completion that creates a security/audit event follows that event class retention without retaining the token secret.
- Sign Out removes/revokes credential authority but never deletes unsynchronized business data.
- Password reset/session invalidation never deletes historical User attribution.

## 17. Backups and PITR
- PostgreSQL backup retention remains minimum 30 days.
- PITR recovery window is configured within platform capability to support frozen RPO <=1 hour; exact mechanics belong to Doc20.
- A record purged from the live database may temporarily remain in immutable backup until that backup expires naturally.
- Purge does not require retroactive rewriting of already-created backups.
- Restore from an older backup must reapply a retention/purge tombstone ledger or equivalent disposition control before expired/purged data becomes operationally accessible.
- Legal hold applicable to restored data blocks post-restore purge until the hold is released.
- Pre-change backup for risky migration/release follows the minimum backup window and cannot be used to bypass retention disposition.
- Object-storage Evidence backup/versioning, when used, follows the same eventual-expiry logic as official Evidence.

## 18. Legal and administrative hold
- V6 defines server-side RetentionHold for records/scopes under legal, audit, investigation or administrative preservation.
- RetentionHold has stable ID, Organization, scope/reference, reason category, createdAt/By, optional releasedAt/By and ACTIVE/RELEASED status.
- RetentionHold does not change business entity state and is not part of Campaign workflow.
- ACTIVE hold suspends purge for covered data regardless of normal retention expiry.
- RetentionHold never grants additional data access; authorization is unchanged.
- Create/release hold is a privileged platform/authorized administrative operation and creates Critical AuditEvent.
- Releasing a hold recalculates eligibility; it does not instantly delete data without normal purge validation.
- A hold may cover Organization, Campaign, entity/reference set or technical incident dataset.

## 19. Organization offboarding and tenant deletion
- V6 provides no Organization-level destructive Delete Organization action to normal application Admins.
- Organization offboarding disables access/memberships through an authorized operational procedure and does not immediately delete retained history.
- Offboarding never shortens minimum 10-year official retention.
- Tenant data may be purged only after all applicable retention periods expire and all holds are released.
- Organization purge is a server/platform administrative operation, not normal business UI.
- Organization purge is dependency-aware; retained snapshots, audit, Evidence, Issues and version context are treated as a coherent retention set.
- Organization purge cannot leave orphan Evidence objects, dangling database references or reusable authorization credentials.
- Completion of final tenant purge creates permanent administrative disposition evidence with tenant ID, action, timestamp, actor/system source and result, without retaining deleted business payload.

## 20. Privacy requests versus immutable history
- V6 provides no self-service Delete My Data capability that can destroy mandatory retained inventory/audit history.
- User disablement is distinct from privacy erasure.
- Privacy/erasure requests use an authorized operational process and are evaluated against applicable retention and hold obligations.
- Data with no remaining retention/legal/business-history requirement may be dispositioned through the approved request.
- Data required for immutable retained history is not destructively removed before expiry merely because of a UI deletion request.
- Where possible without compromising frozen history, nonessential personal attributes may be minimized/pseudonymized while stable historical references and required frozen attribution remain.
- Privacy disposition is audited without copying removed personal payload into Audit.
- Doc19 defines product retention behavior and does not replace applicable jurisdictional/legal review by the operator.

## 21. Purge execution and authorization
- Retention purge executes server-side; Android is never authority for official server deletion.
- Routine policy-driven purge may be executed by SYSTEM.
- Manual exceptional purge requires a privileged platform/authorized administrative operation; Organization Admin does not receive this right implicitly.
- Before purge, the system verifies retention expiry, no active hold, no retained dependency, no unresolved sync/conflict dependency and all class-specific guards.
- Purge is idempotent and retry-safe.
- Partial purge failure cannot report success for the whole disposition set.
- Evidence metadata/object deletion is coordinated so official references cannot silently dangle.
- Each purge batch creates a structured disposition log containing policy/class, count, execution time, result and correlation ID.
- Destruction of historically/security-significant data creates Critical AuditEvent/disposition evidence.
- Purge jobs never rely on destructive database cascade to bypass dependency validation.

## 22. Archive contract
- ARCHIVE means moving data into an approved storage tier/repository without losing retention obligations.
- Archived authoritative data preserves Organization identity, entity identity, schema/version context, retention class and integrity metadata.
- Archive is encrypted at rest and access-controlled at least equivalently to source data.
- Archive package/object uses SHA-256 or platform-equivalent approved cryptographic integrity verification.
- Archive must verify successfully before the source copy becomes purge-eligible.
- Archive failure leaves source retained; V6 never deletes first and archives later.
- Archived official history remains restorable/readable for authorized audit/history need during retention.
- Restore never changes immutable business semantics or historical IDs.
- Archive restoration is auditable.
- Archiving never resets the retention clock.

## 23. Retention testing
- Retention clock calculations use UTC and deterministic test clocks.
- Every retention class in the matrix has before-expiry, at-expiry and after-expiry tests.
- Ten-year classes test exact Close/event anchor semantics.
- Purge eligibility tests include active dependency and active hold negatives.
- Historically referenced master/version records have negative hard-delete tests.
- Unresolved SyncOperation/SyncConflict has explicit no-auto-purge tests.
- Notification expiry verifies removal does not affect authoritative referenced state.
- Evidence cleanup tests remote durability, hash integrity, pending dependencies and orphan detection.
- Archive tests verify-before-delete, corruption detection and restore fidelity.
- Backup restore testing verifies previously purged data is not silently resurrected into active service.
- Legal hold tests block both automatic and manual purge.
- Hold release tests recalculate eligibility rather than immediately deleting.
- Organization offboarding tests prove access removal without premature historical deletion.
- Tenant final purge tests absence of dangling database/object-storage references.
- Privacy disposition tests preserve mandatory history while removing eligible non-required data.
- Generated-export cleanup tests never affect authoritative report source data.
- Device/cache cleanup tests never delete unsynchronized business work.
- Retention regression suite is mandatory before Production release when retention/purge/archive logic changes.

## 24. Retention acceptance gates
- No mandatory 10-year class can be configured below its frozen minimum.
- Notifications enforce the frozen 12-month server retention behavior.
- ACK/SYNCED diagnostic cleanup cannot remove unresolved failures/conflicts or required Audit history.
- No retained immutable history has a dangling master/version/Evidence dependency.
- No device cleanup can discard unsynchronized business data.
- Evidence cleanup passes remote durability/integrity/dependency checks.
- Active hold blocks every applicable purge path.
- Purge is server-authoritative, idempotent, dependency-aware and auditable.
- Archive is encrypted, integrity-verified and restorable before source deletion.
- Backup/PITR behavior cannot permanently resurrect disposed live data.
- Tenant offboarding cannot bypass official retention.
- Privacy disposition cannot silently invalidate immutable official history.
- Generated exports/support artifacts expire independently of authoritative source records.
- Production release evidence required by Doc18 is retained for the approved 3-year period.
- Retention jobs expose operational success/failure metrics without leaking protected payloads.
- A failed purge/archive batch cannot be represented as successful completion.
- Retention configuration remains system-controlled and inaccessible as Organization business settings.
- Retention/purge/archive changes require Doc18 regression evidence before Production.

## 25. Disposition decision sequence
- Identify retention class and authoritative retention anchor.
- Calculate minimum expiry using UTC.
- Check for ACTIVE RetentionHold.
- Check historical, referential, sync/conflict, Evidence and archive dependencies.
- Choose RETAIN, ARCHIVE or PURGE according to the class contract.
- For ARCHIVE, verify encryption, integrity and restorability before source disposition.
- For PURGE, execute server-side idempotently and dependency-aware.
- Record structured disposition result and required Critical AuditEvent.
- After backup restore, reapply disposition controls before restored expired data becomes operationally accessible.

## 26. RT-D01-RT-D200 approved decision register

| ID | Area | Frozen V6 outcome |
|---|---|---|
| **RT-D001** | General retention model | Doc19 is the V6 authority for retention, archival, purge and safe cleanup; it does not redefine business lifecycle. |
| **RT-D002** | General retention model | Retention is applied by data class, not through a generic global deletedAt. |
| **RT-D003** | General retention model | Minimum retention means data cannot be destroyed before the term; longer retention is permitted for an approved legal or operational requirement. |
| **RT-D004** | General retention model | Expiry does not mean instant deletion; the record becomes eligible for controlled disposition. |
| **RT-D005** | General retention model | Disposition is PURGE, ARCHIVE or RETAIN according to class and dependencies; these are retention actions, not business lifecycle statuses. |
| **RT-D006** | General retention model | Purge cannot break referential integrity, historical reproducibility, auditability or interpretation of retained snapshots. |
| **RT-D007** | General retention model | Data under legal/administrative hold is not deleted even when normal minimum retention has expired. |
| **RT-D008** | General retention model | Retention rules are system-controlled and are not editable from Organization Settings. |
| **RT-D009** | Retention clocks | FinalInventorySnapshot retention starts at Campaign.closedAt. |
| **RT-D010** | Retention clocks | CampaignSnapshot retention starts at Campaign.closedAt for a Completed Campaign; while Active it is mandatory. |
| **RT-D011** | Retention clocks | Official validated/final Stocktaking Evidence retention starts at Campaign Close. |
| **RT-D012** | Retention clocks | Issue plus resolution/status-history retention starts at Campaign Close. |
| **RT-D013** | Retention clocks | Campaign-related Critical AuditEvent is retained at least through Campaign.closedAt plus 10 years even when the event occurred earlier. |
| **RT-D014** | Retention clocks | Critical non-Campaign AuditEvent retention starts at event timestampUtc. |
| **RT-D015** | Retention clocks | Notification retention starts at notification creation timestamp. |
| **RT-D016** | Retention clocks | ACKNOWLEDGED sync diagnostic 90-day online window starts at authoritative acknowledgement/completion timestamp. |
| **RT-D017** | Retention clocks | Resolved SyncConflict cleanup clock starts at resolution timestamp; unresolved conflict has no active purge clock. |
| **RT-D018** | Official inventory history | FinalInventorySnapshot plus entries: minimum 10 years after Close. |
| **RT-D019** | Official inventory history | Final Snapshot is immutable during retention and cannot be hard-deleted individually. |
| **RT-D020** | Official inventory history | CampaignSnapshot: minimum 10 years after Close. |
| **RT-D021** | Official inventory history | Stocktaking records/revisions, Measurements and CalculationResults needed to reproduce a retained Final Snapshot are retained at least as long as the snapshot they support. |
| **RT-D022** | Official inventory history | Frozen GeometryProfileVersion and CommodityVersion referenced by retained history are retained at least through the last retained historical reference. |
| **RT-D023** | Official inventory history | Client identity/frozen context needed by Third-Party history is preserved through snapshot/frozen metadata even if Client becomes Inactive. |
| **RT-D024** | Official inventory history | Historical User attribution needed by retained history is preserved through stable ID plus frozen display context. |
| **RT-D025** | Official inventory history | Completed Campaign header/context needed to interpret Final Snapshot is not destructively removed while the snapshot is retained. |
| **RT-D026** | Official inventory history | No authoritative child record is independently purged if doing so makes retained official history non-reproducible. |
| **RT-D027** | Master data and identity lifecycle | Organization, Location, Storage Unit, Storage Space, Client, Geometry Profile and other master records use Active/Inactive lifecycle rather than delete for business retirement. |
| **RT-D028** | Master data and identity lifecycle | A master record with no current or historical reference may become purge-eligible only if a future explicitly approved cleanup rule permits it; V6 has no user-facing hard delete. |
| **RT-D029** | Master data and identity lifecycle | Historically referenced master data is not hard-deleted. |
| **RT-D030** | Master data and identity lifecycle | Immutable historically referenced GeometryProfileVersion/CommodityVersion records are not independently purged from retained history. |
| **RT-D031** | Master data and identity lifecycle | User lifecycle is Active/Disabled; Disable does not erase User identity or historical attribution. |
| **RT-D032** | Master data and identity lifecycle | OrganizationMembership Active/Disabled is retained where authorization/audit history requires it; reactivation uses the same membership identity. |
| **RT-D033** | Master data and identity lifecycle | CampaignTeamAssignment history is retained to interpret assignment/removal and launch context while the historical Campaign context is retained. |
| **RT-D034** | Evidence and Issues | Official Evidence linked to validated/final history is retained minimum 10 years after Campaign Close. |
| **RT-D035** | Evidence and Issues | Evidence metadata and binary remain mutually consistent throughout retention. |
| **RT-D036** | Evidence and Issues | Official Evidence binary cannot be deleted while retaining only metadata before expiry unless a future explicit legal policy changes the historical contract. |
| **RT-D037** | Evidence and Issues | Working Evidence removed before validation may have its binary cleaned after removal is durable, synchronized and demonstrably unreferenced. |
| **RT-D038** | Evidence and Issues | Orphan cleanup requires proof that the binary is not referenced by active/validated Evidence, pending SyncOperation or unresolved conflict. |
| **RT-D039** | Evidence and Issues | Issue, IssueResolution and IssueStatusEvent are retained minimum 10 years after Campaign Close. |
| **RT-D040** | Evidence and Issues | Issue Evidence follows the longer of Evidence retention and Issue/history retention. |
| **RT-D041** | Evidence and Issues | Reset Validation never shortens retention of a prior validated revision or its Evidence. |
| **RT-D042** | Evidence and Issues | Evidence used by Final Snapshot/Dossier cannot be removed by device cache cleanup before remote durability/integrity is confirmed. |
| **RT-D043** | Audit retention | Critical AuditEvent retention is minimum 10 years. |
| **RT-D044** | Audit retention | Campaign-related critical audit is retained at least through closedAt plus 10 years. |
| **RT-D045** | Audit retention | Critical audit independent of Campaign is retained minimum 10 years from timestampUtc. |
| **RT-D046** | Audit retention | AuditEvent is append-only; retention purge cannot become selective history editing. |
| **RT-D047** | Audit retention | If an AuditEvent is required to interpret a retained immutable record, it is not purged before that record. |
| **RT-D048** | Audit retention | Before/after metadata follows AuditEvent retention while secrets and binaries remain prohibited regardless of retention duration. |
| **RT-D049** | Notifications, preferences and configuration | Notifications are retained server-side for 12 months. |
| **RT-D050** | Notifications, preferences and configuration | After 12 months a Notification may be purged regardless of read/unread because it is not source of truth. |
| **RT-D051** | Notifications, preferences and configuration | Notification purge never deletes the referenced Campaign, Issue or Audit business state. |
| **RT-D052** | Notifications, preferences and configuration | UserPreference is retained while User identity is active/retained and may be replaced by current preference values; it is not historical business record. |
| **RT-D053** | Notifications, preferences and configuration | OrganizationConfiguration retains the current value plus AuditEvent history; configuration does not replace audit of changes. |
| **RT-D054** | Notifications, preferences and configuration | Old configuration values need not remain active rows when Audit before/after data and required snapshot context preserve necessary history. |
| **RT-D055** | Sync, conflict and operational queue | ACKNOWLEDGED/SYNCED SyncOperation diagnostics have 90 days online retention, then safe archive/purge. |
| **RT-D056** | Sync, conflict and operational queue | Corresponding critical business events remain in long-term Audit after technical SyncOperation purge. |
| **RT-D057** | Sync, conflict and operational queue | PENDING/IN_FLIGHT operations are never purge-eligible. |
| **RT-D058** | Sync, conflict and operational queue | Permanent FAILED operation is not auto-purged while unresolved. |
| **RT-D059** | Sync, conflict and operational queue | OPEN SyncConflict is not auto-purged. |
| **RT-D060** | Sync, conflict and operational queue | RESOLVED SyncConflict remains at least through the approved diagnostic window; critical resolution remains in Audit. |
| **RT-D061** | DeviceInstallation and offline package/cache | DeviceInstallation is a technical installation identity, not hardware identity and not permanent business history merely because it existed. |
| **RT-D062** | DeviceInstallation and offline package/cache | DeviceInstallation is retained while referenced by session/security context, OfflineWorkingSetManifest, SyncOperation, SyncConflict or another required technical record. |
| **RT-D063** | DeviceInstallation and offline package/cache | DeviceInstallation without active/retained references becomes purge-eligible after 90 days of inactivity. |
| **RT-D064** | DeviceInstallation and offline package/cache | Purging DeviceInstallation never deletes AuditEvent or business history; retained records keep only already-frozen technical context needed for interpretation. |
| **RT-D065** | DeviceInstallation and offline package/cache | OfflineWorkingSetManifest versions are retained locally while the current package or recovery/integrity diagnostics require them. |
| **RT-D066** | DeviceInstallation and offline package/cache | After a new package is verified and atomically activated, superseded local versions may be cleaned when no dependency, conflict or recovery requirement remains. |
| **RT-D067** | DeviceInstallation and offline package/cache | An Active Campaign package is not automatically removed while the user remains authorized and the package is needed for offline operation. |
| **RT-D068** | DeviceInstallation and offline package/cache | A Completed Campaign working package may be compacted/removed after server Close is authoritative, local mutations are durable/resolved and required immutable history/report data remains available. |
| **RT-D069** | DeviceInstallation and offline package/cache | A revoked/out-of-scope package may be removed only after protecting unsynced work/conflict evidence needed for recovery. |
| **RT-D070** | DeviceInstallation and offline package/cache | Master cache may remove out-of-scope/inactive copies only when no retained local history or pending operation references them. |
| **RT-D071** | Local PHOTO and cache cleanup | Canonical local PHOTO is retained until the remote object is durably confirmed, integrity-verified and no pending dependency/conflict requires local bytes. |
| **RT-D072** | Local PHOTO and cache cleanup | SYNCED alone does not justify cleanup when remote integrity/durability has not been confirmed. |
| **RT-D073** | Local PHOTO and cache cleanup | After all cleanup guards pass, local PHOTO bytes may be removed to save space; Evidence metadata and remoteObjectRef remain according to retention. |
| **RT-D074** | Local PHOTO and cache cleanup | Validated/final Evidence does not require a duplicate local copy for 10 years when the official remote binary is retained under contract. |
| **RT-D075** | Local PHOTO and cache cleanup | Local cleanup never changes Evidence business state, hash, metadata or historical references. |
| **RT-D076** | Local PHOTO and cache cleanup | If official remote Evidence cannot be verified, local cleanup is blocked until the integrity condition is resolved. |
| **RT-D077** | Local PHOTO and cache cleanup | Temporary image-processing files are removed immediately after successful canonicalization or safe abandonment. |
| **RT-D078** | Local PHOTO and cache cleanup | Source/original PHOTO may be released after the canonical JPEG is durable; the original never becomes official retained Evidence. |
| **RT-D079** | Generated reports and exports | Generated PDF/CSV export is a derived artifact, not an authoritative inventory record. |
| **RT-D080** | Generated reports and exports | The server need not permanently retain every generated export when it can be reproduced from retained authoritative data. |
| **RT-D081** | Generated reports and exports | Server-side temporary generated export files have maximum 24-hour retention, then are purge-eligible. |
| **RT-D082** | Generated reports and exports | Client-side temporary/share-cache export files are best-effort cleaned after the workflow and no later than normal cache cleanup. |
| **RT-D083** | Generated reports and exports | An export explicitly saved by the user to user-selected storage leaves SiloMath lifecycle control; the application cannot later claim it has deleted that copy. |
| **RT-D084** | Generated reports and exports | Export cleanup never deletes report-generation AuditEvent or authoritative source data. |
| **RT-D085** | Generated reports and exports | Digital Inventory Dossier PDF follows the same derived-export rule; authoritative retained components are snapshot/evidence/audit/context, not the PDF copy. |
| **RT-D086** | Generated reports and exports | If a future generated document becomes an official legally retained artifact, that requires an explicit change decision; V6 does not assume it. |
| **RT-D087** | Diagnostics, logs and security telemetry | Production application/server operational logs have 30 days online retention. |
| **RT-D088** | Diagnostics, logs and security telemetry | Security telemetry relevant to incident investigation has 90 days online retention. |
| **RT-D089** | Diagnostics, logs and security telemetry | A security event that is also Critical AuditEvent separately follows minimum 10-year Audit retention; technical telemetry may expire at 90 days. |
| **RT-D090** | Diagnostics, logs and security telemetry | Diagnostic/support packages are not permanently retained by default. |
| **RT-D091** | Diagnostics, logs and security telemetry | A generated support package stored temporarily server-side expires within maximum 7 days. |
| **RT-D092** | Diagnostics, logs and security telemetry | Support-case attachment retention outside SiloMath is not defined by the V6 application retention contract. |
| **RT-D093** | Diagnostics, logs and security telemetry | Logs, telemetry and support data never contain passwords, tokens, secret keys, Evidence binaries or prohibited full business payloads regardless of retention duration. |
| **RT-D094** | Diagnostics, logs and security telemetry | Correlation IDs, safe entity IDs, versions, counts and timestamps may be retained through the diagnostic window. |
| **RT-D095** | Diagnostics, logs and security telemetry | Active incident investigation may place a hold on relevant logs/telemetry before normal expiry. |
| **RT-D096** | Test evidence, CI and UAT | Exact Production release evidence defined by Doc18 is retained minimum 3 years from release. |
| **RT-D097** | Test evidence, CI and UAT | Release evidence includes mandatory-suite results, RVM snapshot, migration validation/rehearsal evidence, security gates and UAT sign-off. |
| **RT-D098** | Test evidence, CI and UAT | Routine CI artifacts for non-release builds have 90 days retention. |
| **RT-D099** | Test evidence, CI and UAT | Failed CI diagnostic artifacts may be removed after 90 days unless under defect/security hold. |
| **RT-D100** | Test evidence, CI and UAT | UAT evidence for a Production release candidate follows the 3-year release-evidence retention. |
| **RT-D101** | Test evidence, CI and UAT | Test fixtures and synthetic test databases have no business-history retention and are purgeable after test/environment lifecycle. |
| **RT-D102** | Test evidence, CI and UAT | Test evidence must not include Production personal/business data when synthetic/sanitized evidence is sufficient. |
| **RT-D103** | Authentication, sessions and reset artifacts | Active refresh/session records are retained only while valid/revocable plus the minimum technical grace required for replay/revocation enforcement. |
| **RT-D104** | Authentication, sessions and reset artifacts | Expired/revoked refresh credentials are not retained as usable secrets. |
| **RT-D105** | Authentication, sessions and reset artifacts | A non-reversible session identifier/hash plus revocation metadata may be retained for 30 days after expiry/revocation for replay/security investigation. |
| **RT-D106** | Authentication, sessions and reset artifacts | Password-reset token is single-use and valid maximum 30 minutes; plaintext reset token is not retained after completion/expiry. |
| **RT-D107** | Authentication, sessions and reset artifacts | Non-reversible reset-token identifier/security metadata may be retained 30 days for abuse/investigation. |
| **RT-D108** | Authentication, sessions and reset artifacts | Password-reset request/completion that creates a security/audit event follows that event class retention without retaining the token secret. |
| **RT-D109** | Authentication, sessions and reset artifacts | Sign Out removes/revokes credential authority but never deletes unsynchronized business data. |
| **RT-D110** | Authentication, sessions and reset artifacts | Password reset/session invalidation never deletes historical User attribution. |
| **RT-D111** | Backups and PITR | PostgreSQL backup retention remains minimum 30 days. |
| **RT-D112** | Backups and PITR | PITR recovery window is configured within platform capability to support frozen RPO <=1 hour; exact mechanics belong to Doc20. |
| **RT-D113** | Backups and PITR | A record purged from the live database may temporarily remain in immutable backup until that backup expires naturally. |
| **RT-D114** | Backups and PITR | Purge does not require retroactive rewriting of already-created backups. |
| **RT-D115** | Backups and PITR | Restore from an older backup must reapply a retention/purge tombstone ledger or equivalent disposition control before expired/purged data becomes operationally accessible. |
| **RT-D116** | Backups and PITR | Legal hold applicable to restored data blocks post-restore purge until the hold is released. |
| **RT-D117** | Backups and PITR | Pre-change backup for risky migration/release follows the minimum backup window and cannot be used to bypass retention disposition. |
| **RT-D118** | Backups and PITR | Object-storage Evidence backup/versioning, when used, follows the same eventual-expiry logic as official Evidence. |
| **RT-D119** | Legal and administrative hold | V6 defines server-side RetentionHold for records/scopes under legal, audit, investigation or administrative preservation. |
| **RT-D120** | Legal and administrative hold | RetentionHold has stable ID, Organization, scope/reference, reason category, createdAt/By, optional releasedAt/By and ACTIVE/RELEASED status. |
| **RT-D121** | Legal and administrative hold | RetentionHold does not change business entity state and is not part of Campaign workflow. |
| **RT-D122** | Legal and administrative hold | ACTIVE hold suspends purge for covered data regardless of normal retention expiry. |
| **RT-D123** | Legal and administrative hold | RetentionHold never grants additional data access; authorization is unchanged. |
| **RT-D124** | Legal and administrative hold | Create/release hold is a privileged platform/authorized administrative operation and creates Critical AuditEvent. |
| **RT-D125** | Legal and administrative hold | Releasing a hold recalculates eligibility; it does not instantly delete data without normal purge validation. |
| **RT-D126** | Legal and administrative hold | A hold may cover Organization, Campaign, entity/reference set or technical incident dataset. |
| **RT-D127** | Organization offboarding and tenant deletion | V6 provides no Organization-level destructive Delete Organization action to normal application Admins. |
| **RT-D128** | Organization offboarding and tenant deletion | Organization offboarding disables access/memberships through an authorized operational procedure and does not immediately delete retained history. |
| **RT-D129** | Organization offboarding and tenant deletion | Offboarding never shortens minimum 10-year official retention. |
| **RT-D130** | Organization offboarding and tenant deletion | Tenant data may be purged only after all applicable retention periods expire and all holds are released. |
| **RT-D131** | Organization offboarding and tenant deletion | Organization purge is a server/platform administrative operation, not normal business UI. |
| **RT-D132** | Organization offboarding and tenant deletion | Organization purge is dependency-aware; retained snapshots, audit, Evidence, Issues and version context are treated as a coherent retention set. |
| **RT-D133** | Organization offboarding and tenant deletion | Organization purge cannot leave orphan Evidence objects, dangling database references or reusable authorization credentials. |
| **RT-D134** | Organization offboarding and tenant deletion | Completion of final tenant purge creates permanent administrative disposition evidence with tenant ID, action, timestamp, actor/system source and result, without retaining deleted business payload. |
| **RT-D135** | Privacy requests versus immutable history | V6 provides no self-service Delete My Data capability that can destroy mandatory retained inventory/audit history. |
| **RT-D136** | Privacy requests versus immutable history | User disablement is distinct from privacy erasure. |
| **RT-D137** | Privacy requests versus immutable history | Privacy/erasure requests use an authorized operational process and are evaluated against applicable retention and hold obligations. |
| **RT-D138** | Privacy requests versus immutable history | Data with no remaining retention/legal/business-history requirement may be dispositioned through the approved request. |
| **RT-D139** | Privacy requests versus immutable history | Data required for immutable retained history is not destructively removed before expiry merely because of a UI deletion request. |
| **RT-D140** | Privacy requests versus immutable history | Where possible without compromising frozen history, nonessential personal attributes may be minimized/pseudonymized while stable historical references and required frozen attribution remain. |
| **RT-D141** | Privacy requests versus immutable history | Privacy disposition is audited without copying removed personal payload into Audit. |
| **RT-D142** | Privacy requests versus immutable history | Doc19 defines product retention behavior and does not replace applicable jurisdictional/legal review by the operator. |
| **RT-D143** | Purge execution and authorization | Retention purge executes server-side; Android is never authority for official server deletion. |
| **RT-D144** | Purge execution and authorization | Routine policy-driven purge may be executed by SYSTEM. |
| **RT-D145** | Purge execution and authorization | Manual exceptional purge requires a privileged platform/authorized administrative operation; Organization Admin does not receive this right implicitly. |
| **RT-D146** | Purge execution and authorization | Before purge, the system verifies retention expiry, no active hold, no retained dependency, no unresolved sync/conflict dependency and all class-specific guards. |
| **RT-D147** | Purge execution and authorization | Purge is idempotent and retry-safe. |
| **RT-D148** | Purge execution and authorization | Partial purge failure cannot report success for the whole disposition set. |
| **RT-D149** | Purge execution and authorization | Evidence metadata/object deletion is coordinated so official references cannot silently dangle. |
| **RT-D150** | Purge execution and authorization | Each purge batch creates a structured disposition log containing policy/class, count, execution time, result and correlation ID. |
| **RT-D151** | Purge execution and authorization | Destruction of historically/security-significant data creates Critical AuditEvent/disposition evidence. |
| **RT-D152** | Purge execution and authorization | Purge jobs never rely on destructive database cascade to bypass dependency validation. |
| **RT-D153** | Archive contract | ARCHIVE means moving data into an approved storage tier/repository without losing retention obligations. |
| **RT-D154** | Archive contract | Archived authoritative data preserves Organization identity, entity identity, schema/version context, retention class and integrity metadata. |
| **RT-D155** | Archive contract | Archive is encrypted at rest and access-controlled at least equivalently to source data. |
| **RT-D156** | Archive contract | Archive package/object uses SHA-256 or platform-equivalent approved cryptographic integrity verification. |
| **RT-D157** | Archive contract | Archive must verify successfully before the source copy becomes purge-eligible. |
| **RT-D158** | Archive contract | Archive failure leaves source retained; V6 never deletes first and archives later. |
| **RT-D159** | Archive contract | Archived official history remains restorable/readable for authorized audit/history need during retention. |
| **RT-D160** | Archive contract | Restore never changes immutable business semantics or historical IDs. |
| **RT-D161** | Archive contract | Archive restoration is auditable. |
| **RT-D162** | Archive contract | Archiving never resets the retention clock. |
| **RT-D163** | Retention testing | Retention clock calculations use UTC and deterministic test clocks. |
| **RT-D164** | Retention testing | Every retention class in the matrix has before-expiry, at-expiry and after-expiry tests. |
| **RT-D165** | Retention testing | Ten-year classes test exact Close/event anchor semantics. |
| **RT-D166** | Retention testing | Purge eligibility tests include active dependency and active hold negatives. |
| **RT-D167** | Retention testing | Historically referenced master/version records have negative hard-delete tests. |
| **RT-D168** | Retention testing | Unresolved SyncOperation/SyncConflict has explicit no-auto-purge tests. |
| **RT-D169** | Retention testing | Notification expiry verifies removal does not affect authoritative referenced state. |
| **RT-D170** | Retention testing | Evidence cleanup tests remote durability, hash integrity, pending dependencies and orphan detection. |
| **RT-D171** | Retention testing | Archive tests verify-before-delete, corruption detection and restore fidelity. |
| **RT-D172** | Retention testing | Backup restore testing verifies previously purged data is not silently resurrected into active service. |
| **RT-D173** | Retention testing | Legal hold tests block both automatic and manual purge. |
| **RT-D174** | Retention testing | Hold release tests recalculate eligibility rather than immediately deleting. |
| **RT-D175** | Retention testing | Organization offboarding tests prove access removal without premature historical deletion. |
| **RT-D176** | Retention testing | Tenant final purge tests absence of dangling database/object-storage references. |
| **RT-D177** | Retention testing | Privacy disposition tests preserve mandatory history while removing eligible non-required data. |
| **RT-D178** | Retention testing | Generated-export cleanup tests never affect authoritative report source data. |
| **RT-D179** | Retention testing | Device/cache cleanup tests never delete unsynchronized business work. |
| **RT-D180** | Retention testing | Retention regression suite is mandatory before Production release when retention/purge/archive logic changes. |
| **RT-D181** | Retention acceptance gates | No mandatory 10-year class can be configured below its frozen minimum. |
| **RT-D182** | Retention acceptance gates | Notifications enforce the frozen 12-month server retention behavior. |
| **RT-D183** | Retention acceptance gates | ACK/SYNCED diagnostic cleanup cannot remove unresolved failures/conflicts or required Audit history. |
| **RT-D184** | Retention acceptance gates | No retained immutable history has a dangling master/version/Evidence dependency. |
| **RT-D185** | Retention acceptance gates | No device cleanup can discard unsynchronized business data. |
| **RT-D186** | Retention acceptance gates | Evidence cleanup passes remote durability/integrity/dependency checks. |
| **RT-D187** | Retention acceptance gates | Active hold blocks every applicable purge path. |
| **RT-D188** | Retention acceptance gates | Purge is server-authoritative, idempotent, dependency-aware and auditable. |
| **RT-D189** | Retention acceptance gates | Archive is encrypted, integrity-verified and restorable before source deletion. |
| **RT-D190** | Retention acceptance gates | Backup/PITR behavior cannot permanently resurrect disposed live data. |
| **RT-D191** | Retention acceptance gates | Tenant offboarding cannot bypass official retention. |
| **RT-D192** | Retention acceptance gates | Privacy disposition cannot silently invalidate immutable official history. |
| **RT-D193** | Retention acceptance gates | Generated exports/support artifacts expire independently of authoritative source records. |
| **RT-D194** | Retention acceptance gates | Production release evidence required by Doc18 is retained for the approved 3-year period. |
| **RT-D195** | Retention acceptance gates | Retention jobs expose operational success/failure metrics without leaking protected payloads. |
| **RT-D196** | Retention acceptance gates | A failed purge/archive batch cannot be represented as successful completion. |
| **RT-D197** | Retention acceptance gates | Retention configuration remains system-controlled and inaccessible as Organization business settings. |
| **RT-D198** | Retention acceptance gates | Retention/purge/archive changes require Doc18 regression evidence before Production. |
| **RT-D199** | Location facility coordinates | Location latitude/longitude follow the same retention, offboarding, archive, privacy-disposition and historical-reference rules as the parent Location record; V6 creates no separate user/device tracking-location history. |
| **RT-D200** | Map technical cache | Basemap tiles/provider caches are non-authoritative technical cache and may be evicted independently when safe; they are never retained as business history and their eviction cannot delete Location master data or unsynchronized work. |

## 27. Cross-document handoff
- Doc20 PRODUCTION_OPERATIONS must implement backup/PITR mechanics, retention jobs, archive storage, hold administration, purge scheduling, monitoring, restore re-disposition and operational runbooks without weakening this policy.
- Doc18 remains the verification authority: any change to retention/purge/archive logic requires the mandatory retention regression evidence frozen here.
- Documents 01-17 remain authoritative for the business records, immutable semantics, security controls and data classes whose lifecycle this policy governs.

## 28. Freeze statement

This document is the canonical APPROVED / FROZEN SiloMath V6 Retention Policy after the 2026-10-20 CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze, defined by RT-D01-RT-D200, including retention clocks, minimum periods, dependency preservation, safe device/Evidence cleanup, logs and diagnostics windows, release-evidence retention, session/reset metadata, backup interaction, RetentionHold, tenant offboarding, privacy disposition, server-side purge, archive integrity/restoration, retention tests and release gates.

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

Retain actual pinned identifiers and reproduction capability for retained history. Do not manufacture old metadata or relabel unknown history as engine2.0.

## Approved Country contract cross-reference

CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74: Non-conforming historical Country values remain preserved exactly as stored. The complete ISO 3166-1/uppercase alpha-2 rule governs canonical new values; it does not authorize rewriting historical literals, inferring codes from country names, rebuilding snapshots or shortening retention. Audit the existing non-conforming population separately before proposing or executing any migration. Human-readable labels remain presentation only.

# SiloMath V6 — 10_DATABASE_SCHEMA

**Physical persistence contract — PostgreSQL + Room**

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-28 - CR-API-STORAGE-SPACE-BATCH-001 approved atomic Storage Space batch-create clarification

## 1. Purpose and authority

This document is the canonical V6 **physical persistence contract** for SiloMath. It maps the logical model frozen in `09_DATA_MODEL` to PostgreSQL server persistence and Android Room/SQLite persistence. It does not redefine domain semantics.

Authority chain for this document: `01_PRODUCT_BASELINE` → approved/frozen decisions → `09_DATA_MODEL` + DM-D01–DM-D64 → approved DB-D01–DB-D68. If a physical convenience conflicts with a frozen semantic invariant, the semantic invariant wins.

### 1.1 Non-goals
- This document does not define REST DTOs/endpoints (Document 11).
- It does not redefine sync lifecycle/business behavior beyond persistence mechanics (Document 12).
- It does not redefine F01–F10 mathematics (Document 13).
- It does not make read models authoritative.

## 2. Database topology

- **Server:** PostgreSQL, application schema `silomath`.
- **Android:** Room over SQLite for the authorized local/offline working set.
- **Evidence binary storage:** app-private files locally; object storage remotely. Databases store metadata/references/hash only.
- **Local settings:** DataStore may own presentation-only settings; synchronized domain preferences use database persistence.
- **Derived datasets:** views/materialized views/Room projections are permitted but rebuildable.

## 3. Canonical physical type mapping

| Semantic type | PostgreSQL | Room / SQLite | Rule |
|---|---|---|---|
| UUID | `UUID` | `TEXT` | Canonical lowercase UUID string locally. |
| UTC instant | `TIMESTAMPTZ` | `INTEGER` | Epoch milliseconds in Room; UTC semantics. |
| LocalDate | `DATE` | `TEXT` | ISO `YYYY-MM-DD`; no timezone conversion. |
| Decimal official | `NUMERIC(20,6)` | `TEXT` | Canonical decimal; never SQLite REAL. |
| Version/counter | `BIGINT` | `INTEGER` | Non-negative; entityVersion starts at 1. |
| Boolean | `BOOLEAN` | `INTEGER` | Room constrained to 0/1. |
| Enum | `VARCHAR/TEXT + CHECK` | `TEXT` | Stable uppercase codes. |
| Hash SHA-256 | `CHAR(64)`/validated text | `TEXT` | Lowercase hex, 64 chars. |
| Structured audit/conflict envelope | `JSONB` | canonical `TEXT` JSON when locally required | Allowed only for operational/audit envelopes, not opaque authoritative business entities. |

## 4. Naming, identity and metadata

- Tables use plural `snake_case`; columns use `snake_case`.
- Stable technical UUID is the PK. Business Code remains a separate constrained attribute.
- Mutable synchronized entities carry the metadata required by Document 09: `created_at`, `created_by`, `updated_at`, `updated_by`, `entity_version`, except explicit entity-specific exceptions.
- Immutable versions/snapshots/events use creation/schema/version metadata and have no semantic `updated_at`.
- Codes are normalized before persistence; database constraints enforce canonical form where supported.
- No generic `deleted_at` is introduced.

## 5. Referential integrity and tenant isolation

- Historical/business FKs use RESTRICT/NO ACTION semantics by default.
- Business/history aggregates do not use destructive cascading deletes.
- Tenant ownership is direct through `organization_id` or through a provably immutable parent chain; security-sensitive parent/child links should additionally use composite tenant FKs where practical.
- A stable ID never grants authorization by itself.
- Platform-governed shared definitions remain global.

## 6. Authoritative physical table inventory

| Physical table | Logical record | Persistence | Core physical contract | Critical constraints |
|---|---|---|---|---|
| users | User | Server + authorized local projection | user_id UUID PK; full_name; email; email_normalized UQ; lifecycle_status; created/updated metadata; entity_version | No hard delete; disabled history retained. |
| organizations | Organization | Server + authorized local projection | organization_id UUID PK; name; code UQ; address/city/country; lifecycle_status; mutation metadata | Tenant boundary. |
| organization_memberships | OrganizationMembership | Server + local authorization projection | membership_id UUID PK; organization_id FK; user_id FK; role; lifecycle_status; mutation metadata; UQ(org,user) | Last-active-Admin rule is transactional/server-side. |
| device_installations | DeviceInstallation | Server + device local | device_installation_id UUID PK; installation metadata; created/updated metadata | No hardware/IMEI/advertising ID. |
| locations | Location | Server + scoped Room | location_id UUID PK; organization_id FK; name; code; address/city/country/notes; latitude_deg NUMERIC(9,6) NULL; longitude_deg NUMERIC(10,6) NULL; lifecycle_status; metadata | UQ(org,code); coordinate pair/range CHECKs. |
| storage_units | StorageUnit | Server + scoped Room | storage_unit_id UUID PK; organization_id; location_id FK; name; code; description; lifecycle_status; metadata | UQ(location,code). |
| storage_spaces | StorageSpace | Server + scoped Room | storage_space_id UUID PK; organization_id; storage_unit_id FK; code; description; geometry_profile_id FK; lifecycle_status; metadata | UQ(unit,code); max capacity not editable here. |
| clients | Client | Server + scoped Room | client_id UUID PK; organization_id FK; name; code; tax_id; notes; lifecycle_status; metadata | UQ(org,code). |
| geometry_families | GeometryFamily | Platform server + package Room | geometry_family_id UUID PK; family_code UQ; name; schema/formula compatibility metadata | Global governed F01–F10. |
| geometry_profiles | GeometryProfile | Server + scoped Room | geometry_profile_id UUID PK; organization_id FK; code; name; lifecycle_status; current_version_id; metadata | UQ(org,code). |
| geometry_profile_versions | GeometryProfileVersion | Server + package Room | geometry_profile_version_id UUID PK; geometry_profile_id FK; version_number; geometry_family_id FK; explicit parameters; max_capacity_m3 NUMERIC(20,6); reference datum; provenance/schema/version metadata | Immutable; UQ(profile,version). |
| commodities | Commodity | Platform server + package Room | commodity_id UUID PK; code/name/governance fields; current_version_id | Global governed identity. |
| commodity_versions | CommodityVersion | Platform server + package Room | commodity_version_id UUID PK; commodity_id FK; version_number; frozen governed/reference fields; created/schema metadata | Immutable; UQ(commodity,version). |
| inventory_campaigns | InventoryCampaign | Server + scoped Room | campaign_id UUID PK; organization_id FK; code; campaign_year; sequence; name; inventory_type; start_date; planned_end_date; status; metadata | ACTIVE/COMPLETED only; atomic code allocation. |
| campaign_scopes | CampaignScope | Server + scoped Room | campaign_scope_id UUID PK; campaign_id FK; storage_space_id FK; created metadata; launch-time reproducibility context is held by CampaignSnapshot | UQ(campaign,space); immutable after launch. |
| campaign_team_assignments | CampaignTeamAssignment | Server + scoped Room | assignment_id UUID PK; campaign_id FK; user_id FK; status; assigned_at/by; removed_at/by; metadata | Partial UQ ACTIVE(campaign,user); history retained. |
| campaign_snapshots | CampaignSnapshot | Server + package Room | campaign_snapshot_id UUID PK; campaign_id UQ FK; organization/campaign frozen header; inventory type/dates; schema/calculation compatibility; created metadata | Immutable launch header. |
| campaign_snapshot_scope_items | CampaignSnapshot child | Server + package Room | snapshot_scope_item_id UUID PK; campaign_snapshot_id FK; frozen space/hierarchy/profile/version values | Explicit frozen collection. |
| campaign_snapshot_team_items | CampaignSnapshot child | Server + package Room | snapshot_team_item_id UUID PK; campaign_snapshot_id FK; user_id + frozen display/role/team values | Launch team; later assignment changes do not rewrite. |
| campaign_snapshot_commodity_items | CampaignSnapshot child | Server + package Room | snapshot_commodity_item_id UUID PK; campaign_snapshot_id FK; commodity_id UUID; commodity_version_id UUID | Explicit immutable launch-time Commodity compatibility collection; later Commodity catalog/version changes do not rewrite. |
| offline_working_set_manifests | OfflineWorkingSetManifest | Server + device Room | manifest_id UUID PK; campaign_id; organization_id; device_installation_id; package_version; content_manifest; integrity metadata; generated/downloaded/verified times; is_current | UQ campaign/device/version; one current. |
| stocktaking_records | StocktakingRecord | Server + Room local-first | stocktaking_record_id UUID PK; organization_id; campaign_id; storage_space_id; current_revision_id; mutation metadata | UQ(campaign,space); classification derived. |
| stocktaking_revisions | StocktakingRevision | Server + Room local-first | stocktaking_revision_id UUID PK; stocktaking_record_id; revision_number; state; based_on_revision_id; is_empty; commodity_version_id; density; ownership; client_id; notes; calculation_result_id; validation/creation/version metadata | Validated immutable; UQ(root,revision_no). |
| measurements | Measurement | Server + Room local-first | measurement_id UUID PK; stocktaking_revision_id; sequence_number; raw_value; raw_unit; normalized_ullage_m; datum metadata; geometry_profile_version_id; capture/version metadata | UQ(revision,sequence); sequence 1..5. |
| calculation_results | CalculationResult | Server + Room local-first | calculation_result_id UUID PK; revision_id; surface/aggregation versions; effective ullage/fill; volume; density; quantity; geometry refs; engine/formula versions; input_fingerprint; rounding policy; calculated metadata | Validated result immutable. |
| issues | Issue | Server + Room local-first | issue_id UUID PK; organization_id; campaign_id; stocktaking_record_id; category; description; status; report/latest-resolution/version metadata | OPEN/RESOLVED current projection. |
| issue_resolutions | IssueResolution | Server + Room | issue_resolution_id UUID PK; issue_id; reason; context; resolved_at/by; audit_event_id | Append-only. |
| issue_status_events | IssueStatusEvent | Server + Room | issue_status_event_id UUID PK; issue_id; from_status; to_status; reason; changed_at/by; audit_event_id | Append-only; initial from_status nullable. |
| evidence | Evidence | Server + Room metadata; binary external | evidence_id UUID PK; organization_id; campaign_id; owner refs XOR; stocktaking_revision_id; type; note_text or photo metadata; hash; local/object refs; sync state; capture/creation metadata | No BLOB; validated evidence immutable. |
| final_inventory_snapshots | FinalInventorySnapshot | Server; completed package/history Room as needed | final_snapshot_id UUID PK; campaign_id UQ; organization_id; close actor/time; frozen campaign/schema/calculation context | Immutable; created atomically with COMPLETED. |
| final_inventory_snapshot_entries | FinalInventorySnapshotEntry | Server; history Room as needed | entry_id UUID PK; final_snapshot_id; storage_space_id; frozen hierarchy/profile/commodity/client/user/measurement/calculation/issue/evidence metadata | UQ(snapshot,space); complete historical reproducibility. |
| sync_operations | SyncOperation | Room primary outbox + server idempotency/diagnostics | sync_operation_id UUID PK; entity_type/id; operation_type; payload; idempotency_key; base/entity version; state; retry/error timestamps | One logical mutation = one operation. |
| sync_operation_dependencies | SyncOperationDependency | Room + server diagnostics where needed | dependency_id UUID PK; parent_operation_id; dependent_operation_id; dependency_type; created_at | UQ(parent,dependent); parent != dependent. |
| sync_conflicts | SyncConflict | Room + server | sync_conflict_id UUID PK; sync_operation_id; entity ref; status; local_attempt; server_value; server_version; resolution_type/actor/time | OPEN/RESOLVED; no force overwrite. |
| audit_events | AuditEvent | Server authoritative; selective Room projection | audit_event_id UUID PK; organization/context/entity/action; actor_user_id nullable only SYSTEM; source/system_actor; occurred_at; before_data/after_data/change_metadata JSONB | Append-only; no secrets/binaries. |
| notifications | Notification | Server + user Room projection | notification_id UUID PK; recipient_user_id; organization/context refs; notification_type; title/body/context; created_at; read_at/state | 12-month server retention; not audit source. |
| user_preferences | UserPreference | Server for synchronized preferences | user_preference_id UUID PK; user_id UQ; approved synchronized preference fields; metadata | Presentation-only local settings may instead use DataStore. |
| organization_configurations | OrganizationConfiguration | Server + authorized Room projection | organization_configuration_id UUID PK; organization_id; key; value_type; typed value columns; metadata | UQ(org,key); approved registry only; no arbitrary JSON. |

### 6.1 Snapshot child structures

`campaign_snapshots` and `final_inventory_snapshot_entries` are not opaque JSON dumps. N-valued launch collections are persisted through explicit child structures. CampaignSnapshot Commodity compatibility is persisted through `campaign_snapshot_commodity_items`, with one immutable child row per frozen Commodity identity/version compatibility entry. Final snapshot entries freeze sufficient scalar and structured historical values to render completed history without joining mutable current master data.

### 6.2 Geometry parameter structure

`geometry_profile_versions` uses an explicit, versioned parameter structure keyed by Geometry Family. Document 10 freezes the physical strategy: **no generic opaque business JSON**. The exact F01–F10 parameter set is transcribed from the frozen Calculation Engine contract; Document 13 may document formulas and exact parameter semantics but may not replace this persistence strategy or alter historical meaning.

## 7. Uniqueness and CHECK constraints

- `organization_memberships`: UNIQUE `(organization_id,user_id)`.
- `locations`: UNIQUE `(organization_id,code)`; `latitude_deg` and `longitude_deg` are both NULL or both non-NULL; latitude `BETWEEN -90 AND 90`; longitude `BETWEEN -180 AND 180`.
- `storage_units`: UNIQUE `(location_id,code)`.
- `storage_spaces`: UNIQUE `(storage_unit_id,code)`.
- `clients`: UNIQUE `(organization_id,code)`.
- `geometry_profiles`: UNIQUE `(organization_id,code)`.
- `geometry_profile_versions`: UNIQUE `(geometry_profile_id,version_number)`.
- `commodity_versions`: UNIQUE `(commodity_id,version_number)`.
- `campaign_scopes`: UNIQUE `(campaign_id,storage_space_id)`.
- `campaign_team_assignments`: partial UNIQUE ACTIVE `(campaign_id,user_id)`.
- `stocktaking_records`: UNIQUE `(campaign_id,storage_space_id)`.
- `stocktaking_revisions`: UNIQUE `(stocktaking_record_id,revision_number)`.
- `measurements`: UNIQUE `(stocktaking_revision_id,sequence_number)` plus `sequence_number BETWEEN 1 AND 5`.
- `final_inventory_snapshots`: UNIQUE `(campaign_id)`.
- `final_inventory_snapshot_entries`: UNIQUE `(final_snapshot_id,storage_space_id)`.
- `offline_working_set_manifests`: UNIQUE `(campaign_id,device_installation_id,package_version)` plus at most one current manifest.
- `organization_configurations`: UNIQUE `(organization_id,configuration_key)`.
- `sync_operation_dependencies`: UNIQUE `(parent_operation_id,dependent_operation_id)` and parent != dependent.
- Evidence principal owner: exactly one of StocktakingRecord or Issue.
- PHOTO/NOTE discriminator constraints enforce compatible fields.
- Cross-row/cross-table rules such as max five photos, last active Admin, Mark Empty measurement count and Close Guards are enforced transactionally rather than misrepresented as simple row CHECKs.

## 8. Index strategy

| Query family | Required index intent |
|---|---|
| Tenant/master lookup | locations(organization_id,lifecycle_status,code), clients(organization_id,lifecycle_status,code), geometry_profiles(organization_id,lifecycle_status,code) |
| Location map projection | locations(organization_id,latitude_deg,longitude_deg) partial where coordinates are non-null, or equivalent provider-independent projection/index chosen from measured query plans |
| Hierarchy navigation | storage_units(location_id,lifecycle_status,code), storage_spaces(storage_unit_id,lifecycle_status,code) |
| Campaign lists | inventory_campaigns(organization_id,status,start_date), inventory_campaigns(organization_id,campaign_year,code) |
| Campaign scope/team | campaign_scopes(campaign_id,storage_space_id), partial ACTIVE campaign_team_assignments(campaign_id,user_id) |
| Stocktaking navigation | stocktaking_records(campaign_id,storage_space_id), stocktaking_revisions(stocktaking_record_id,revision_number DESC), issues(stocktaking_record_id,status) |
| Measurements/results | measurements(stocktaking_revision_id,sequence_number), calculation_results(stocktaking_revision_id,calculated_at) |
| Evidence | evidence(stocktaking_record_id,stocktaking_revision_id,evidence_type), evidence(issue_id,evidence_type), evidence(sync_state) |
| Final history | final_inventory_snapshots(organization_id,closed_at), final_inventory_snapshot_entries(final_snapshot_id,storage_space_id) |
| Sync worker | sync_operations(state,created_at), sync_operations(entity_type,entity_id), sync_operation_dependencies(dependent_operation_id) |
| Audit/notifications | audit_events(organization_id,occurred_at DESC), audit_events(entity_type,entity_id,occurred_at DESC), notifications(recipient_user_id,read_at,created_at DESC) |

Indexes are justified by frozen navigation, tenant filtering, stocktaking, sync and history access patterns. Document 10 does not require indexing every FK blindly; execution plans and production telemetry may refine non-semantic indexes without changing this contract.

## 9. Transaction boundaries

| Operation | Store | Atomic boundary |
|---|---|---|
| Save & Launch | Server PostgreSQL | Allocate Campaign Code atomically; create ACTIVE Campaign; Scope; initial Team; CampaignSnapshot header/children; Audit. Any failure rolls back. |
| Offline business mutation | Room | Write business row(s) and exactly one logical SyncOperation in the same transaction before UI success. |
| Evidence offline save | Room + app-private file protocol | Private file must be durably written, then metadata/outbox committed; UI success only after durable state. Cleanup handles failed partial file protocol. |
| Validation | Room for supported offline validation; server on synchronization/revalidation | Revalidate revision inputs, measurement count, Empty/non-empty rules, current CalculationResult fingerprint, Evidence constraints; freeze revision and audit/outbox atomically. |
| Reset Validation | Server + synchronized Room result | Create revision N+1 based on prior validated revision; repoint currentRevision; audit/outbox effects; never mutate prior validated revision. |
| Issue resolve/reopen | Room/server | Update Issue current projection plus append IssueStatusEvent; resolution appends IssueResolution; audit in same logical mutation. |
| Inventory Close | Server PostgreSQL only | Revalidate all Close Guards; create FinalInventorySnapshot + all entries; set Campaign COMPLETED; append audit. Failure creates no partial snapshot. |
| Sync ACK/conflict | Room | ACK updates same SyncOperation; conflict stores SyncConflict and CONFLICT state atomically; no silent overwrite. |

## 10. Stocktaking revision persistence

`stocktaking_records` is the stable Campaign×StorageSpace root. `stocktaking_revisions` owns mutable/validated revision state. `current_revision_id` identifies the current revision. A validated revision, its measurements, frozen calculation result and validated Evidence bindings are immutable.

Working input changes make the current calculation stale and require a new current CalculationResult before non-empty validation. Validation checks the calculation input fingerprint. Reset Validation creates a new working revision and never edits the prior validated revision.

Mark Empty is represented by a VALIDATED revision with `is_empty=true`, zero measurements, zero product volume/physical quantity and no requirement for Commodity, density, Ownership or Client.

## 11. Evidence persistence

- PHOTO binaries are normalized externally to database persistence and are never stored as BLOBs.
- PHOTO metadata includes required hash, MIME/type, size/dimensions and local/object-storage references needed by the Evidence contract.
- NOTE stores note text and no binary object.
- Stocktaking Evidence has exactly one StocktakingRecord principal owner and may additionally bind to the relevant StocktakingRevision.
- Validated Evidence cannot be deleted/rebound through ordinary business operations.
- Maximum five PHOTO per StocktakingRevision and five PHOTO per Issue is checked inside the mutation transaction.

## 12. Sync/outbox persistence

- Every supported offline-capable mutation and its SyncOperation commit in one Room transaction.
- One logical mutation creates one SyncOperation; retries reuse its idempotency key.
- Dependencies are explicit rows, not implicit payload ordering.
- `IN_FLIGHT` is crash-recoverable without minting a new logical operation.
- Conflicts preserve local attempted and server-authoritative representations/version; no force overwrite or automatic merge.
- ACKNOWLEDGED diagnostics may later be cleaned under retention rules; unresolved errors/conflicts are retained.

## 13. Audit, notification and configuration persistence

`audit_events` is append-only. JSONB is allowed for structured changed-field/before/after metadata because it is an audit envelope; secrets and Evidence binaries are excluded. SYSTEM events may have null `actor_user_id` only with the required system source/actor metadata.

`organization_configurations` uses approved keys and typed value columns. Exactly the column matching `value_type` may contain a value. Arbitrary business JSON is prohibited.

`notifications` is mutable only for notification/read-state behavior and is not an audit source. `user_preferences` stores synchronized cross-device preferences; presentation-only local preferences may be DataStore-owned.

## 14. Room working-set boundary

| Category | V6 rule |
|---|---|
| Always/typically local for authorized working set | Organizations/membership projection, campaign package/snapshot/scope/team, hierarchy, pinned geometry/commodity versions, stocktaking roots/revisions/measurements/calculations/issues/evidence metadata, outbox/dependencies/conflicts, device manifest. |
| Server-authoritative with selective local projection | Users, configuration, notifications, audit, final history, platform reference data. |
| Not Room business tables | Evidence image binaries; presentation-only preferences when DataStore is authoritative; derived UI models that can be queried/projected. |
| Never whole-tenant by default | Offline data is campaign-scoped. The local database must not imply arbitrary full-Organization replication. |

Room schema may contain technical local columns needed for synchronization or file coordination, but such columns must not silently become new domain semantics.

## 15. Server-generated Campaign Code

The official Campaign Code follows `INV-YYYY-NNNN`. Allocation is server-owned and atomic per Organization/year. A dedicated allocator/sequence row may be locked transactionally. The issued sequence and Campaign insert participate in the same Save & Launch transaction; a failed launch must not create a partially launched Campaign. Sequence gaps caused by transaction/operational implementation are not treated as identity reuse; an issued official Code is never reassigned to another Campaign.

## 16. Inventory Close persistence

Close is online and server-authoritative. One transaction revalidates all Close Guards, inserts exactly one `final_inventory_snapshots` row, inserts exactly one entry for every scoped Storage Space, freezes required historical values/version IDs, changes Campaign to COMPLETED, and writes the authoritative audit effects. Any failure rolls back the entire finalization.

## 17. Immutability enforcement

Immutability is enforced through repository/service authorization plus database permissions/triggers where appropriate for defense in depth. At minimum, application roles cannot semantically UPDATE immutable GeometryProfileVersion, CommodityVersion, CampaignSnapshot, validated StocktakingRevision/history, FinalInventorySnapshot/Entry, IssueStatusEvent, IssueResolution or AuditEvent records.

## 18. Migration and schema-version policy

- Every schema change has an explicit version and tested migration; production never relies on destructive fallback.
- Risky server changes use Expand → Migrate → Contract. Old and new app versions must remain compatible for the approved rollout window.
- Room migrations are explicit and forward-only. Development-only destructive reset is never a production migration strategy.
- New NOT NULL columns on populated tables are introduced through safe nullable/default/backfill/constraint sequencing.
- Constraint/index creation on large server tables is planned to avoid unnecessary blocking; validation can be staged where PostgreSQL supports it.
- Immutable snapshot/version semantics are preserved during migrations; migrations may transform representation but never reinterpret completed history.
- Numeric migrations preserve exact decimal values and six-decimal persistence rules; conversion through floating-point is prohibited.
- Migration verification includes row counts, orphan checks, uniqueness checks, tenant-crossing checks, snapshot coverage, and representative calculation fingerprints.
- Before relevant risky production migrations/releases, a backup is taken; rollback strategy is operational, while data migrations themselves remain forward-safe.

## 19. Retention-aware physical behavior

- Final snapshots, Campaign snapshots, critical audit, validated/final Evidence metadata and Issue history must remain interpretable for the frozen long-term retention period.
- Historically referenced master data is preserved through lifecycle status rather than destructive deletion.
- Notifications may expire under the approved server retention.
- Acknowledged synchronized-operation diagnostics may be cleaned only after the approved diagnostic retention window and only when business durability is established.
- Local cache cleanup must never delete unsynchronized work or the only durable copy of required Evidence.

## 20. Read models and projections

Dashboard summaries, Stocktaking classifications, Sync Status, reports, Inventory History, Dossier and Audit Trail presentation are read models. PostgreSQL views/materialized views and Room query projections are permitted optimizations. They must be rebuildable and may not become alternative sources of truth.

## 21. Physical-schema acceptance criteria

- **DB-AC01** All 35 authoritative logical entities/records from Document 09 are physically mapped or explicitly represented as an approved child/embedded structure.
- **DB-AC02** Stable UUID identity is preserved independently of business Codes.
- **DB-AC03** Every tenant-sensitive reference is incapable of silently crossing Organization boundaries.
- **DB-AC04** No destructive cascade can invalidate historical interpretation.
- **DB-AC05** All frozen logical uniqueness rules have physical unique/partial-unique or transactionally equivalent enforcement.
- **DB-AC06** Official decimals are exact and never pass through SQLite REAL for persistence.
- **DB-AC07** Geometry/Commodity versions and snapshots preserve immutable historical meaning.
- **DB-AC08** Campaign Save & Launch and Inventory Close have atomic server persistence boundaries.
- **DB-AC09** Stocktaking revision/reset/validation persistence preserves every validated revision.
- **DB-AC10** Evidence owner/type/photo-limit/binary-separation rules are enforceable.
- **DB-AC11** Offline business writes and outbox writes are atomic in Room.
- **DB-AC12** Sync retry/idempotency/dependency/conflict persistence cannot produce silent last-write-wins.
- **DB-AC13** Audit is append-only and excludes secrets/binary Evidence.
- **DB-AC14** Room contains only authorized/scoped offline data plus required technical state; whole-Organization replication is not implied.
- **DB-AC15** Production migrations are forward-safe and preserve completed history.
- **DB-AC16** DB-D01–DB-D68 are represented without unresolved physical-schema decisions.
- **DB-AC17** Location coordinate pair/range constraints are enforced consistently in PostgreSQL and Room/mappers; partial coordinate persistence is impossible.
- **DB-AC18** No map tile/provider cache is modeled as authoritative business data.

## 22. Frozen physical-schema decision register — DB-D01–DB-D68

| ID | Approved V6 decision |
|---|---|
| **DB-D01** | PostgreSQL business tables use schema `silomath`, not `public`. |
| **DB-D02** | SQL naming is `snake_case`; tables are plural; Room table/column names mirror SQL names where practical. |
| **DB-D03** | Canonical UUID IDs use PostgreSQL `UUID`; Room persists them as canonical lowercase UUID `TEXT`. |
| **DB-D04** | Offline-capable identities are generated before local insert; strictly server-created identities may be server-generated. Campaign Code is always separate and server-generated. |
| **DB-D05** | Enums persist as stable uppercase text codes with CHECK constraints; PostgreSQL native ENUM is not used. |
| **DB-D06** | UTC instants use PostgreSQL `TIMESTAMPTZ`; Room uses epoch-millisecond `INTEGER`. |
| **DB-D07** | Date-only values use PostgreSQL `DATE`; Room uses ISO `YYYY-MM-DD` `TEXT`, with no timezone conversion. |
| **DB-D08** | Official deterministic decimal values use PostgreSQL `NUMERIC(20,6)` where the numeric contract applies; Room stores canonical decimal `TEXT`, never `REAL`. |
| **DB-D09** | `entity_version` uses PostgreSQL `BIGINT NOT NULL`, initially 1; Room uses `INTEGER NOT NULL`. |
| **DB-D10** | Boolean uses PostgreSQL `BOOLEAN`; Room uses constrained integer 0/1. |
| **DB-D11** | SHA-256 is stored as lowercase 64-character hexadecimal text with format validation. |
| **DB-D12** | Contractual text maxima are enforced physically; unbounded contractual text uses `TEXT`. |
| **DB-D13** | Each authoritative table uses its stable technical UUID as PK; business Code is never a PK/FK. |
| **DB-D14** | Historical/business FKs default to `ON DELETE RESTRICT/NO ACTION` and `ON UPDATE NO ACTION`. |
| **DB-D15** | No destructive `ON DELETE CASCADE` is used for business/history aggregates; technical cleanup may cascade only when later retention rules explicitly allow it. |
| **DB-D16** | Tenant tables carry direct `organization_id` where natural/useful; strict children may inherit unambiguous tenant scope through an immutable parent chain. |
| **DB-D17** | Where practical, tenant parent-child integrity is reinforced with composite `(organization_id, parent_id)` FKs. |
| **DB-D18** | Platform-governed global reference data does not receive artificial `organization_id`. |
| **DB-D19** | No generic `deleted_at` exists for business/master/history data; approved lifecycle states govern deactivation. |
| **DB-D20** | Immutable snapshots, versions and append-only events cannot be removed through business FK cascades or repointed to current master data. |
| **DB-D21** | Master Codes are normalized with trim + uppercase before persistence and constrained to canonical form. |
| **DB-D22** | Code uniqueness follows the frozen logical scope: Organization global provisioning scope; Location per Organization; Storage Unit per Location; Storage Space per Storage Unit; Client and Geometry Profile per Organization. |
| **DB-D23** | Email uses persisted canonical `email_normalized` and a unique constraint; uniqueness never depends on database collation defaults. |
| **DB-D24** | Campaign Code is unique within Organization/year; allocator state guarantees atomic `INV-YYYY-NNNN` issuance. |
| **DB-D25** | OrganizationMembership is unique per `(organization_id,user_id)` and reactivation reuses the same row. |
| **DB-D26** | CampaignScope is unique per `(campaign_id,storage_space_id)`. |
| **DB-D27** | CampaignTeamAssignment history is retained; PostgreSQL uses a partial unique index allowing at most one ACTIVE assignment per Campaign×User. |
| **DB-D28** | StocktakingRecord is unique per `(campaign_id,storage_space_id)`. |
| **DB-D29** | StocktakingRevision is unique per `(stocktaking_record_id,revision_number)`. |
| **DB-D30** | Measurement is unique per `(stocktaking_revision_id,sequence_number)` and sequence is constrained to 1..5. |
| **DB-D31** | FinalInventorySnapshot is unique by `campaign_id`; FinalInventorySnapshotEntry is unique by `(final_snapshot_id,storage_space_id)`. |
| **DB-D32** | OfflineWorkingSetManifest is unique by Campaign×DeviceInstallation×packageVersion and has at most one current manifest per Campaign×device. |
| **DB-D33** | OrganizationConfiguration is unique per `(organization_id,configuration_key)`. |
| **DB-D34** | SyncOperation idempotency key is unique in the server mutation namespace; retry reuses the same operation/key. |
| **DB-D35** | SyncOperationDependency is unique per parent/dependent pair and forbids self-dependency. |
| **DB-D36** | GeometryProfile stores logical identity/current pointer; GeometryProfileVersion stores immutable definitions. |
| **DB-D37** | GeometryProfileVersion uses monotonic `version_number` unique per profile. |
| **DB-D38** | Geometry parameters are represented by an explicit versioned physical structure, not an opaque generic business JSON blob. Exact F01–F10 parameter columns/structures must transcribe the frozen Calculation Engine contract without changing this strategy. |
| **DB-D39** | `max_capacity_m3` is persisted on GeometryProfileVersion as deterministic calculated data and is never editable StorageSpace data. |
| **DB-D40** | Commodity uses logical identity plus immutable CommodityVersion, with version number unique per Commodity. |
| **DB-D41** | Save & Launch is one PostgreSQL transaction covering code allocation, ACTIVE campaign, frozen scope/team snapshot, CampaignSnapshot and required audit effects; failure rolls back completely. |
| **DB-D42** | Campaign sequence allocation is atomic per Organization/year and server-owned. |
| **DB-D43** | CampaignSnapshot is persisted as a header plus explicit child snapshot tables for N-valued frozen collections, including canonical `campaign_snapshot_commodity_items` for Commodity compatibility identity/version entries; it is not one opaque JSON business object. |
| **DB-D44** | Snapshot rows are immutable; later current-state changes never rewrite launch snapshot meaning. |
| **DB-D45** | StocktakingRecord is the stable root and points to `current_revision_id`; revision state resides in StocktakingRevision. |
| **DB-D46** | `current_revision_id` may be null only during controlled root creation; setting/repointing it and creating the corresponding revision occurs transactionally, with same-root validation. |
| **DB-D47** | Measurement count/Empty semantics are transactionally validated at Validation; row constraints enforce sequence 1..5 but do not pretend to express cross-table business state. |
| **DB-D48** | CalculationResult stores fingerprint, engine/formula/version identifiers and official outputs; the validated result is immutable. |
| **DB-D49** | Reset Validation is one transaction creating revision N+1, linking the prior validated revision, repointing current revision, and writing audit/outbox effects. |
| **DB-D50** | Issue stores current status; IssueStatusEvent and IssueResolution are append-only histories. Reopen never erases resolution history. |
| **DB-D51** | Evidence uses one table with exactly one principal owner enforced as StocktakingRecord XOR Issue. |
| **DB-D52** | Stocktaking Evidence may bind to a StocktakingRevision; once frozen into validated history, that binding is immutable. |
| **DB-D53** | PHOTO and NOTE use discriminator constraints: PHOTO requires binary metadata; NOTE requires note text and forbids binary object fields. |
| **DB-D54** | The five-PHOTO limit is enforced transactionally per StocktakingRevision and per Issue, both server-side and locally. |
| **DB-D55** | Evidence binaries are never PostgreSQL/Room BLOBs; databases retain metadata, hashes and storage references only. |
| **DB-D56** | Room writes an offline-capable business mutation and its SyncOperation atomically before UI success. |
| **DB-D57** | Outbox payload is canonical serialized mutation data plus entity/version/idempotency metadata; it is not the business source of truth. |
| **DB-D58** | Crash recovery of IN_FLIGHT operations re-enters controlled retry without generating a new idempotency key. |
| **DB-D59** | SyncConflict may store structured JSON conflict envelopes containing local attempted and server-authoritative representations/version; this is operational conflict evidence, not generic business persistence. |
| **DB-D60** | Acknowledged sync diagnostics may be purged only under approved retention; unresolved failures/conflicts are not auto-deleted. |
| **DB-D61** | AuditEvent is append-only; structured before/after/change metadata uses PostgreSQL JSONB where appropriate, excluding secrets and Evidence binaries. |
| **DB-D62** | OrganizationConfiguration is typed key/value with dedicated typed value columns and a CHECK requiring exactly the value compatible with `value_type`; arbitrary JSON is prohibited. |
| **DB-D63** | Presentation-only local preferences may use DataStore; synchronized cross-device UserPreference data uses the authoritative server table. A preference cannot have two authoritative sources. |
| **DB-D64** | Dashboard/report/history/sync projections may use views/materialized views/Room projections but never become authoritative business tables; they remain rebuildable from canonical sources. |
| **DB-D65** | `locations.latitude_deg` uses NUMERIC(9,6) and `locations.longitude_deg` uses NUMERIC(10,6); both are nullable only as one pair. |
| **DB-D66** | Location coordinate CHECKs enforce latitude -90..90, longitude -180..180 and the both-null/both-present invariant. |
| **DB-D67** | Room stores the same semantic coordinate pair using a lossless-for-6-decimal representation/mapping and enforces pair/range validation before commit; server constraints remain authoritative. |
| **DB-D68** | Map tiles/provider cache are technical cache only and never enter the authoritative relational schema, SyncOperation business payload, audit business payload, or retention model. |

## 23. Cross-document handoff

- **11_API_SPECIFICATION** must expose the domain/API contract without making SQL/Room representation part of the public API.
- **12_OFFLINE_SYNC_SPECIFICATION** may refine worker scheduling, retry timing, package transfer and cleanup, but may not change outbox atomicity, idempotency, explicit dependencies or conflict persistence frozen here.
- **13_CALCULATION_ENGINE_SPECIFICATION** supplies the exact F01–F10 formula/parameter contract to the explicit GeometryProfileVersion parameter structure; it may not replace it with opaque business JSON or alter historical versions.
- **15_EVIDENCE_STORAGE_SPECIFICATION** may refine binary/object-storage mechanics without storing large Evidence BLOBs in Room/PostgreSQL.
- **19_RETENTION_POLICY** may define approved cleanup/archive execution but cannot authorize deletion that breaks the historical-integrity baseline.
- **20_PRODUCTION_OPERATIONS** may refine backup/restore/migration execution while preserving forward-safe schema evolution.

## 24. Freeze statement

This document is the canonical APPROVED / FROZEN SiloMath V6 physical database schema contract after the 2026-10-20 CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze. Later implementation may refine non-semantic index tuning, SQL syntax, migration scripts and operational mechanics without Product/Baseline approval, but may not change DB-D01–DB-D68, the physical representation strategy, tenant/referential integrity, transactional boundaries, historical immutability or the logical semantics frozen by Document 09 without an approved V6 change.

---
**End of Document — 10_DATABASE_SCHEMA**
## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

This approval does not alter SQL or Room schemas. Existing TEXT version columns may retain lossless decimal-text representations of integer metadata; adapters validate exact values explicitly. Never interpret legacy labels such as schema-v1 as integer1.

## Approved Country contract cross-reference

CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74: Canonical Country values for new Organization/Location writes are uppercase ISO 3166-1 alpha-2 members of the complete catalogue; labels are not persisted as the canonical value. Enforce new-value membership fail-closed without silently rewriting existing non-conforming values. Existing physical storage must preserve those values pending separate audit before any migration. The clarification alone does not authorize narrowing existing columns, DDL execution, backfill or a Room schema-version bump.

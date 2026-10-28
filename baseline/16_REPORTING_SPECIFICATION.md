# SiloMath V6 — 16_REPORTING_SPECIFICATION

**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-28 - CR-API-STORAGE-SPACE-BATCH-001 approved atomic Storage Space batch-create clarification

## 1. Purpose and authority

This document is the normative SiloMath V6 reporting contract. It consolidates the frozen Product Baseline, Decision Register, Business Rules, Roles/Permissions, UI, Data Model, Database, API, Offline Sync, Calculation, Commodity and Evidence contracts together with the explicitly approved RP-D01–RP-D107 decision package.

The contract defines the nine canonical reports, authoritative data sources, filters, grouping, totals, export behavior, historical comparison, dossier/audit boundaries, offline completeness, integrity and deterministic output.

## 2. Reporting principles

- Exactly nine report types exist in V6.
- Completed official reports are FinalInventorySnapshot-backed and never recalculate historical results.
- Active Campaign monitoring is explicitly non-final.
- One Single Authoritative Report Dataset drives UI and supported exports for one request.
- Canonical persisted values drive aggregation; presentation rounding is never a calculation input.
- Reporting is read-only. Projections may accelerate queries but remain rebuildable and non-authoritative.
- Authorization applies to report query, detail, export and referenced Evidence access.

## 3. Canonical report catalog

| # | Report Code | Report | Grain / grouping | Authoritative source | Export |
|---|---|---|---|---|---|
| R01 | PHYSICAL_INVENTORY | Physical Inventory | Storage Space | Completed: FinalInventorySnapshot; Active: current operational read model, non-final | PDF + CSV |
| R02 | INVENTORY_BY_LOCATION | Inventory by Location | Location → Commodity | Same authoritative dataset as R01 for matching scope | PDF + CSV |
| R03 | INVENTORY_BY_COMMODITY | Inventory by Commodity | Commodity | Frozen Commodity context | PDF + CSV |
| R04 | INVENTORY_BY_OWNERSHIP | Inventory by Ownership | Ownership → Client where Third-Party | Frozen Ownership/Client context | PDF + CSV |
| R05 | STORAGE_SPACE_WORKSHEET | Storage Space Worksheet | Storage Space technical trace | Frozen calculation trace | PDF + CSV |
| R06 | CAPACITY_REPORT | Capacity Report | Storage Space | Frozen Product Volume + Max Capacity | PDF + CSV |
| R07 | INVENTORY_HISTORY | Inventory History | 2..N Completed snapshots / comparison unit | FinalInventorySnapshot only | PDF + CSV |
| R08 | DIGITAL_INVENTORY_DOSSIER | Digital Inventory Dossier | Completed Campaign dossier | Final snapshot/history + authorized retained refs | PDF only |
| R09 | AUDIT_TRAIL | Audit Trail | AuditEvent | Authoritative audit records/read model | PDF + CSV |

## 4. Source selection and finality

- Completed Campaign: source is the immutable FinalInventorySnapshot and frozen entries/metadata.
- Active Campaign: source may be the current operational read model, but the output is labeled NON-FINAL / ACTIVE CAMPAIGN.
- An Active output cannot be treated as the official final inventory.
- Completed report generation must not invoke current Geometry Engine, Quantity Engine or current master values to reconstruct frozen results.
- Historical output remains reproducible even when current Geometry Profiles, Commodities, Clients, hierarchy display names or user status later change.

## 5. Single Authoritative Report Dataset

For one normalized report request, SiloMath resolves authorization, source identity, filters, grouping, ordering and measures into one authoritative report dataset. The screen, PDF and CSV use that same dataset where the report supports those formats.

A report dataset records enough source context for traceability: Organization, report code, Campaign/Final Snapshot identity where applicable, normalized filters, source/finality state, generatedAt and generatedBy.

## 6. Numeric semantics and totals

- Canonical Physical Quantity is aggregated in kg; presentation may expose kg/t without changing canonical values.
- Product Volume and Max Capacity use m³.
- No intermediate/display rounding is used to recompute report totals.
- A Storage Space contributes at most once at the applicable detail grain.
- For applicable aggregate reports: sum(detail canonical values) = report total = authoritative aggregation for the same filter scope.
- Display rounding may make the visible sum of row text differ by a final decimal from canonical total; this is not corrected by re-summing rounded display text.

## 7. Filtering

Filters operate on the selected authoritative source without mutating source identity. Filter state is deterministic, permission-scoped and represented in export metadata.

| Report | Filters |
|---|---|
| Physical Inventory | Location; Storage Unit; Commodity; Ownership; Client |
| Inventory by Location | Location; Commodity; Ownership; Client |
| Inventory by Commodity | Commodity; Location; Ownership; Client |
| Inventory by Ownership | Ownership; Client; Commodity; Location |
| Storage Space Worksheet | Location; Storage Unit; Storage Space; Commodity; validation/empty outcome |
| Capacity Report | Location; Storage Unit; Commodity; Ownership |
| Inventory History | 2..N Completed Campaigns; Commodity; Location; Ownership; Client as Third-Party context where applicable |
| Digital Inventory Dossier | Completed Campaign selection |
| Audit Trail | Actor/User; Action/Event Type; Entity Type; Campaign/context/reference; Date Range |

## 8. Export contract

- Generic V6 report exports are PDF and CSV where the report catalog permits them.
- Digital Inventory Dossier is PDF-only in V6.
- XLSX and ZIP Dossier are not V6 formats.
- PDF is print/human-oriented; CSV is deterministic machine-readable tabular output.
- Export includes the entire filtered dataset, not only the current UI page.
- Export is read-only and uses the same authoritative dataset as the report request.
- Successful export is auditable without embedding the full report payload into AuditEvent.

### 8.1 Export metadata

- Report type/code
- Organization
- Campaign where applicable
- Final Snapshot ID for Completed official reports
- generatedAt UTC
- generatedBy
- active normalized filters
- final/non-final source state where applicable

### 8.2 Filename

`SiloMath_<ReportCode>_<CampaignCode-or-ORG>_<YYYYMMDD-HHmmssZ>.<pdf|csv>`

Filesystem-invalid characters are sanitized deterministically.

### 8.3 CSV

- UTF-8
- header row required
- RFC-4180-style quoting
- `.` decimal separator for machine-readable numeric values
- units explicit in header/metadata
- deterministic row ordering

## 9. R01 — Physical Inventory

- Code: PHYSICAL_INVENTORY.
- Detail grain: one row per scoped Storage Space.
- Completed rows use frozen Location, Storage Unit, Storage Space, Empty state, Commodity, Ownership, Client when Third-Party, Product Volume, Actual Bulk Density where applicable, Physical Quantity and validation context.
- Mark Empty: Product Volume=0 and Physical Quantity=0; Commodity, Density, Ownership and Client are not fabricated.
- Totals: Product Volume and Physical Quantity.
- Canonical quantity total is summed in kg; display/export adapters may present tonnes separately.

## 10. R02 — Inventory by Location

- Code: INVENTORY_BY_LOCATION.
- Primary grouping: Location; secondary grouping: Commodity.
- Measures: Physical Quantity, Product Volume, Storage Space count.
- Provides Location subtotals and grand total.
- For the same source/filter scope, totals reconcile exactly with Physical Inventory.

## 11. R03 — Inventory by Commodity

- Code: INVENTORY_BY_COMMODITY.
- Grouping uses frozen Commodity identity/version display context.
- Empty rows are represented separately as Empty / No Commodity and never as a pseudo Commodity.
- Measures: Physical Quantity, Product Volume, Location count, Storage Space count.
- Current Commodity rename/version changes never rewrite Completed historical labels/grouping.

## 12. R04 — Inventory by Ownership

- Code: INVENTORY_BY_OWNERSHIP.
- Primary groups are exactly Owned and Third-Party.
- Third-Party may subgroup by Client.
- Measures: Product Volume, Physical Quantity and Storage Space count.
- Empty rows are represented as Empty / Ownership not applicable; they are not forced into Owned/Third-Party.

## 13. R05 — Storage Space Worksheet

- Code: STORAGE_SPACE_WORKSHEET.
- One technical section/row group per Storage Space.
- Displays frozen hierarchy and Geometry Family/Profile/Version.
- Displays measurement datum/reference, ordered measurements, effective ullage/fill height and Product Volume.
- Displays CommodityVersion, entered/normalized Actual Bulk Density, Physical Quantity, Ownership/Client and empty state.
- Displays validation metadata, Issue/Evidence references and calculation version/fingerprint metadata.
- Never recalculates a completed formula; it is a frozen calculation trace.

## 14. R06 — Capacity Report

Code: `CAPACITY_REPORT`.

For each valid row:

`utilizationPercent = ProductVolumeM3 / MaxCapacityM3 × 100`

- Max Capacity is frozen on GeometryProfileVersion and is not recalculated from current geometry.
- Mark Empty utilization is 0% when Max Capacity is valid and >0.
- Missing or incompatible historical capacity is N/A, never zero.
- Utilization above 100% is not silently clamped.
- Aggregate utilization is ΣProductVolumeM3 / ΣMaxCapacityM3 ×100 over rows with valid/comparable capacity.
- Arithmetic mean of row utilization percentages is forbidden.

## 15. R07 — Inventory History

- Code: INVENTORY_HISTORY.
- Only 2..N authorized Completed Campaigns from the same Organization may participate.
- Source is each Campaign's immutable FinalInventorySnapshot.
- Periods are ordered by closeAt, then Campaign identity as deterministic tie-breaker.
- For 2 campaigns the comparison is A→B; for >2, primary change metrics compare consecutive periods.
- Canonical dimensions: Close Date, Campaign, Commodity, Location, Ownership and Physical Quantity.
- Client may be Third-Party filter/context but is not a seventh canonical History dimension.
- Matching uses stable snapshot identities, not current display names.
- History is physical-only and never forecasts or introduces Book Stock, Variance or Reconciliation.

### 15.1 Comparability

| State | Meaning | Change presentation |
|---|---|---|
| COMPARABLE | Same comparison unit exists in both snapshots and relevant semantic context is materially unchanged. | Absolute change; percentage change when denominator permits. |
| SCOPE_ADDED | Relevant Storage Space/unit exists in newer snapshot only. | Classification/context; no misleading comparable percentage. |
| SCOPE_REMOVED | Relevant Storage Space/unit exists in previous snapshot only. | Classification/context; no misleading comparable percentage. |
| CONTEXT_CHANGED | Stable entity correlates, but relevant Location/Ownership/Commodity context changed materially. | Classification/context; direct comparability qualified. |

### 15.2 Change formulas

`absoluteChangeKg = currentPhysicalQuantityKg - previousPhysicalQuantityKg`

When `previousPhysicalQuantityKg > 0`:

`percentageChange = (current - previous) / previous × 100`

When previous quantity is zero, Percentage Change is `N/A`; the system never emits Infinity or a fabricated percentage.

## 16. R08 — Digital Inventory Dossier

- Code: DIGITAL_INVENTORY_DOSSIER.
- Completed Campaign only.
- Source: immutable Final Snapshot/history plus authorized retained references.
- Header: Organization; Campaign identity/name/code/type; Start Date; Planned End Date; close actor/time; Final Snapshot ID; dossier generation metadata.
- Includes Campaign Scope/hierarchy and final inventory summary.
- Includes per-Storage-Space frozen final result and calculation trace data required by the final snapshot.
- Includes Issue references/status/resolution history relevant to the Campaign.
- Includes Evidence/NOTE metadata and authorized retained Evidence references; PHOTO bytes are not duplicated into relational dossier data.
- Standard users can access authorized Dossier content but cannot obtain the general Admin-only Audit Trail through it.
- Admin Audit Trail remains a separate AUD-01 destination.
- V6 Dossier export is PDF only; ZIP is excluded.
- Embedding PHOTO binary content in PDF is not mandatory; approved metadata/reference representation is sufficient.
- If an Evidence binary is unavailable locally, the UI identifies that state and does not pretend the binary is locally verified.
- Dossier view/export is read-only.

## 17. R09 — Audit Trail

- Code: AUDIT_TRAIL.
- AUD-01 is Admin-only; Standard cannot obtain general Audit Trail data through UI, API, offline cache or Dossier.
- Audit data is append-only/read-only.
- Default order: occurredAt DESC, auditEventId deterministic tie-breaker.
- Filters: Actor/User; Action/Event Type; Entity Type; Campaign/context/reference; UTC-aware Date Range.
- Minimum row/detail: Event ID, occurredAt, actor identity/frozen display metadata, action/event type, entity type/ID, Campaign/context where present and source.
- Before/after values, reason/note and related references appear only when present and authorized.
- Evidence binary data and secrets never enter Audit export.
- actorUserId may be null only for SYSTEM events.
- Export: PDF + CSV.
- Audit export is itself auditable without recursive export-event generation.
- Notifications, navigation clicks, debug logs and routine sync retry noise are not fabricated as Audit Events.
- Critical Audit Events are retained for at least 10 years.

## 18. Authorization

| Capability | Admin | Standard |
|---|---|---|
| R01–R07 report/history view | Authorized Organization/Campaign scope | Authorized Organization/Campaign scope |
| R01–R07 supported export | Authorized scope | Authorized scope |
| R08 Dossier | Authorized Completed Campaign scope | Authorized Completed Campaign scope |
| R08 Dossier PDF | Authorized scope | Authorized scope |
| R09 Audit Trail | Allowed within authorized Organization scope | Not allowed |
| R09 Audit export | Allowed | Not allowed |

Server-side authorization is authoritative. UI visibility is not sufficient authorization.

## 19. Offline reporting

- A report may be shown offline only when its required authoritative dataset is locally available and demonstrably complete for the request scope.
- A partial cache is never represented as a complete report.
- When missing local data affects the result, UI identifies Offline / incomplete data and blocks final report/export.
- Completed offline reports use immutable local Final Snapshot data rather than current master reconstruction.
- Offline PDF/CSV generation requires complete local source data.
- An auditable offline export produces a durable local audit/outbox event for later synchronization.
- Previously cached authorization never overrides a revocation once that revocation is known.

## 20. Report integrity

- Applicable detail sums, report totals and authoritative-source aggregation must reconcile for identical filter scope.
- R01, R02, R03 and R04 canonical Physical Quantity totals must match for identical source/filter scope.
- An integrity mismatch is an explicit report integrity error.
- A report with a known integrity mismatch is not silently published/exported.
- Canonical totals are computed from canonical numeric values, not rounded display strings.

## 21. Pagination and deterministic ordering

- Large report UI uses cursor pagination/lazy loading.
- Pagination changes presentation only, not dataset semantics or totals.
- Export covers the full filtered dataset, not the current page.
- Hierarchy default order: Location Code → Storage Unit Code → Storage Space Code.
- Grouped reports use stable group label/code plus stable identity as tie-breaker.
- CSV and PDF detail ordering is deterministic and never depends on incidental database order.

## 22. Performance and projections

- Expensive aggregation/export does not block the primary Android interaction thread.
- Asynchronous execution is allowed but cannot change source/filter semantics.
- Materialized/cached report projections are allowed for performance.
- Every projection remains rebuildable from authoritative data and preserves source identity/version needed for traceability.
- Completed report generation never invokes the current Geometry Engine or recomputes Physical Quantity.

## 23. Audit of reporting actions

- Successful exports are auditable with actor, time, report code, source context and normalized filters.
- Audit payloads do not contain the full report, Evidence binary bytes or secrets.
- Offline auditable export events are durable and synchronize through the normal outbox contract.
- Audit Trail export logging must avoid infinite self-recursion.

## 24. Error behavior

| Condition | Required behavior |
|---|---|
| Unauthorized report/source/filter | Reject without data leakage. |
| Completed snapshot unavailable | Do not reconstruct from current master; surface unavailable/error. |
| Active report | Label NON-FINAL / ACTIVE CAMPAIGN. |
| Partial offline cache | Mark incomplete and block final export. |
| Report integrity mismatch | Explicit integrity error; no misleading export. |
| History selection <2 Completed campaigns | Validation error. |
| History percentage denominator = 0 | Percentage Change N/A. |
| Dossier requested for Active Campaign | Reject. |
| Standard requests Audit Trail | Reject. |
| Capacity denominator missing/incompatible | N/A; never divide by zero. |
| Export generation failure | Source remains unchanged; actionable export error. |
| Evidence binary unavailable in offline Dossier | Show unavailable/offline state; retain available metadata. |

## 25. API and database alignment

- Report APIs operate on normalized filters, authorized source identity and deterministic pagination/order.
- Completed report APIs resolve immutable Final Snapshot data.
- Audit APIs enforce Admin-only access server-side.
- Report projections/read models are not authoritative business tables.
- Exports are generated from the same logical dataset as the report request.
- Database numeric semantics preserve the frozen NUMERIC(20,6)/lossless decimal contract; presentation conversion is separate.

## 26. Cross-document exclusions

- XLSX is excluded from V6 reporting.
- ZIP Digital Inventory Dossier is excluded.
- Legacy Property terminology is replaced by canonical Ownership.
- Book Stock, reconciliation variance, loss/allowance and AJG semantics are outside V6.
- Inventory History does not forecast.
- Capacity utilization never divides Physical Quantity kg by Max Capacity m³.

## 27. Required test matrix

| ID | Scenario | Expected result |
|---|---|---|
| RPT-T01 | Completed Physical Inventory uses only Final Snapshot | No current-master recalculation; output matches frozen snapshot. |
| RPT-T02 | Active Campaign report | Clearly NON-FINAL / ACTIVE CAMPAIGN and cannot masquerade as final inventory. |
| RPT-T03 | Same request UI/PDF/CSV | All derive from one authoritative filtered dataset. |
| RPT-T04 | Mark Empty in Physical Inventory | Volume=0; quantity=0; no fabricated Commodity/Density/Ownership/Client. |
| RPT-T05 | R01 vs R02 same scope | Canonical Physical Quantity totals reconcile exactly. |
| RPT-T06 | R01 vs R03 same scope | Canonical Physical Quantity totals reconcile exactly. |
| RPT-T07 | R01 vs R04 same scope | Canonical Physical Quantity totals reconcile exactly. |
| RPT-T08 | Historical Commodity renamed | Completed report keeps frozen historical label/context. |
| RPT-T09 | Worksheet generated after current geometry changes | Displays frozen trace; does not run current Geometry Engine. |
| RPT-T10 | Capacity valid row | utilization = ProductVolumeM3 / MaxCapacityM3 ×100. |
| RPT-T11 | Capacity >100% | Displayed/flagged as >100%; never clamped. |
| RPT-T12 | Capacity missing/incompatible historical value | Utilization N/A; denominator is not replaced by zero. |
| RPT-T13 | Aggregate capacity utilization | Uses Σvolume/Σcapacity, not arithmetic mean of row percentages. |
| RPT-T14 | History with two Completed campaigns | A→B comparison only. |
| RPT-T15 | History with >2 campaigns | Changes calculated for consecutive close-order periods. |
| RPT-T16 | History previous quantity = 0 | Percentage Change N/A; no Infinity/fabricated percent. |
| RPT-T17 | History scope added | SCOPE_ADDED; no misleading comparable percentage. |
| RPT-T18 | History scope removed | SCOPE_REMOVED; no misleading comparable percentage. |
| RPT-T19 | History relevant context changed | CONTEXT_CHANGED and direct comparability is qualified. |
| RPT-T20 | History stable identity with renamed display label | Matching uses stable identity, not current name. |
| RPT-T21 | Dossier requested for Active Campaign | Rejected; Dossier requires Completed Campaign. |
| RPT-T22 | Standard opens Dossier | Allowed in authorized scope; general Audit Trail is not exposed. |
| RPT-T23 | Dossier Evidence binary unavailable offline | Metadata may remain; binary marked unavailable/offline. |
| RPT-T24 | Dossier export | PDF only; ZIP is not offered in V6. |
| RPT-T25 | Standard requests Audit Trail | Denied server-side and UI inaccessible. |
| RPT-T26 | Audit SYSTEM event | actorUserId may be null only with SYSTEM actor/source semantics. |
| RPT-T27 | Audit export | PDF/CSV includes active filters and stable fields; export action audited safely. |
| RPT-T28 | Offline complete Final Snapshot | Eligible completed report can be viewed/generated locally. |
| RPT-T29 | Offline partial report cache | Clearly incomplete and final report/export blocked. |
| RPT-T30 | Offline auditable export | Audit event is durable/outboxed for later sync. |
| RPT-T31 | Report total mismatch | Explicit integrity error; export blocked. |
| RPT-T32 | UI pagination | Totals remain for full filtered dataset, not current page. |
| RPT-T33 | Export with paged UI | Exports full filtered dataset. |
| RPT-T34 | CSV numeric formatting | UTF-8; header; '.' decimal separator; deterministic ordering. |
| RPT-T35 | Export filename | Matches canonical sanitized naming rule. |
| RPT-T36 | Display rounding | Canonical total remains authoritative even if displayed row sum differs at last decimal. |
| RPT-T37 | Completed report generation | No Geometry Engine invocation and no Physical Quantity recalculation. |
| RPT-T38 | Unauthorized filter/context probing | No data leakage outside Organization/authorized Campaign scope. |
| RPT-T39 | Audit export recursion | Export event does not recursively create unbounded export events. |
| RPT-T40 | Materialized report cache rebuilt | Rebuilt output preserves authoritative source semantics and source identity/version. |

## 28. Acceptance / release gates

| Gate | Acceptance criterion |
|---|---|
| RPT-AC01 | Exactly nine canonical report types exist; no hidden tenth V6 report. |
| RPT-AC02 | Completed official reporting is FinalInventorySnapshot-backed and immutable. |
| RPT-AC03 | Active Campaign output is visibly non-final. |
| RPT-AC04 | Single Authoritative Report Dataset governs UI and supported exports. |
| RPT-AC05 | PDF and CSV are the canonical generic export formats; Dossier is PDF-only. |
| RPT-AC06 | No XLSX, ZIP Dossier, Book Stock, Variance, Reconciliation or forecast semantics are introduced. |
| RPT-AC07 | R01/R02/R03/R04 canonical Physical Quantity totals reconcile for identical scope. |
| RPT-AC08 | Mark Empty is represented without fabricated business values. |
| RPT-AC09 | Worksheet is a frozen calculation trace and never recalculates completed results. |
| RPT-AC10 | Capacity utilization uses volume/volume and never kg/m³-incompatible arithmetic. |
| RPT-AC11 | Capacity aggregate uses weighted Σvolume/Σcapacity semantics. |
| RPT-AC12 | History supports 2..N Completed Campaigns and all four frozen comparability classifications. |
| RPT-AC13 | History percentage change is N/A when previous quantity is zero. |
| RPT-AC14 | History terminology is Physical Inventory Change; no forecasting. |
| RPT-AC15 | Dossier is Completed-only and cannot bypass Audit Trail authorization. |
| RPT-AC16 | Audit Trail is Admin-only and immutable/read-only. |
| RPT-AC17 | Offline report/export is allowed only from demonstrably complete authoritative local data. |
| RPT-AC18 | Integrity mismatch blocks misleading publication/export. |
| RPT-AC19 | Pagination does not change totals and export covers the full filtered dataset. |
| RPT-AC20 | Completed report generation never invokes current Geometry/Quantity recalculation. |
| RPT-AC21 | CSV serialization is deterministic and machine-readable. |
| RPT-AC22 | Report generation/export is read-only and auditable. |
| RPT-AC23 | Authorization is enforced server-side for report data and export. |
| RPT-AC24 | Historical labels/version context remain frozen despite current master changes. |
| RPT-AC25 | Report projections/caches remain rebuildable and non-authoritative. |

## 29. Approved RP-D01–RP-D107 decision register

| ID | Frozen V6 outcome |
|---|---|
| **RP-D01** | V6 contains exactly nine report types: Physical Inventory, Inventory by Location, Inventory by Commodity, Inventory by Ownership, Storage Space Worksheet, Capacity Report, Inventory History, Digital Inventory Dossier, and Audit Trail. |
| **RP-D02** | Every report request produces one Single Authoritative Report Dataset; UI, PDF and CSV representations derive from that same dataset where those exports are supported. |
| **RP-D03** | For a Completed Campaign, the official reporting source is the immutable FinalInventorySnapshot plus its entries and frozen metadata. |
| **RP-D04** | Completed historical geometry, volume, quantity and other frozen results are not recalculated during report generation. |
| **RP-D05** | An Active-Campaign report may use the current operational read model but must be explicitly marked NON-FINAL / ACTIVE CAMPAIGN. |
| **RP-D06** | Active-Campaign output cannot be presented or exported as an official final inventory. |
| **RP-D07** | Reporting read models and projections are derived/rebuildable and are not the business source of truth. |
| **RP-D08** | A Storage Space contributes at most once to one report dataset request at its applicable detail grain. |
| **RP-D09** | Aggregations use canonical persisted values; display rounding never becomes an aggregation input. |
| **RP-D10** | V6 report export formats are PDF and CSV where the report contract supports export; XLSX is not part of V6. |
| **RP-D11** | PDF is human-readable/print-oriented; CSV is machine-readable tabular output. |
| **RP-D12** | Export uses the same filtered authoritative dataset as the corresponding report request and does not execute a different business semantic. |
| **RP-D13** | Export metadata includes report type, Organization, Campaign where applicable, Final Snapshot ID for Completed official reports, generatedAt, generatedBy and active filters. |
| **RP-D14** | Canonical export filename is SiloMath_<ReportCode>_<CampaignCode-or-ORG>_<YYYYMMDD-HHmmssZ>.<pdf|csv>, with filesystem-invalid characters sanitized. |
| **RP-D15** | Export generation timestamp is UTC; UI may present local time separately. |
| **RP-D16** | CSV uses UTF-8, a header row, RFC-4180-style quoting, and '.' as decimal separator for machine-readable numeric values. |
| **RP-D17** | PDF/CSV makes units explicit in headers/metadata so numeric values are not ambiguous. |
| **RP-D18** | Report/export generation is read-only and does not mutate Snapshot or report source data. |
| **RP-D19** | Successful export generation is auditable with report type, context, filters, actor and time, without embedding the full report in AuditEvent. |
| **RP-D20** | Physical Inventory report code is PHYSICAL_INVENTORY. |
| **RP-D21** | Physical Inventory detail grain is one row per scoped Storage Space. |
| **RP-D22** | Completed Physical Inventory rows include frozen Location, Storage Unit, Storage Space, Empty state, Commodity, Ownership, Client where Third-Party, Product Volume, Actual Bulk Density where applicable, Physical Quantity and validation context. |
| **RP-D23** | A Mark Empty row reports Product Volume=0 and Physical Quantity=0 and does not fabricate Commodity, Density, Ownership or Client. |
| **RP-D24** | Physical Inventory totals include Product Volume and Physical Quantity; canonical quantity aggregation is in kg with presentation/export unit adapters separate. |
| **RP-D25** | Physical Inventory filters are Location, Commodity, Ownership, Client and Storage Unit. |
| **RP-D26** | Inventory by Location report code is INVENTORY_BY_LOCATION. |
| **RP-D27** | Inventory by Location groups primarily by Location and then Commodity. |
| **RP-D28** | Inventory by Location reports Physical Quantity, Product Volume and Storage Space count, with Location subtotals and a grand total. |
| **RP-D29** | Inventory by Location filters are Location, Commodity, Ownership and Client. |
| **RP-D30** | Inventory by Location totals reconcile exactly with Physical Inventory for the same authoritative source and filter scope. |
| **RP-D31** | Inventory by Commodity report code is INVENTORY_BY_COMMODITY. |
| **RP-D32** | Inventory by Commodity groups by frozen Commodity identity/version display context; Empty rows are represented separately as Empty / No Commodity, never as a pseudo Commodity. |
| **RP-D33** | Per Commodity the report provides Physical Quantity, Product Volume, Location count and Storage Space count. |
| **RP-D34** | Inventory by Commodity filters are Commodity, Location, Ownership and Client. |
| **RP-D35** | Current Commodity rename/version changes never rewrite Completed historical grouping or labels. |
| **RP-D36** | Inventory by Ownership report code is INVENTORY_BY_OWNERSHIP. |
| **RP-D37** | Primary ownership groups are exactly Owned and Third-Party; Third-Party may subgroup by Client. |
| **RP-D38** | Inventory by Ownership measures are Product Volume, Physical Quantity and Storage Space count. |
| **RP-D39** | Inventory by Ownership filters are Ownership, Client, Commodity and Location. |
| **RP-D40** | Empty rows are represented separately as Empty / Ownership not applicable and are not classified artificially as Owned or Third-Party. |
| **RP-D41** | Storage Space Worksheet report code is STORAGE_SPACE_WORKSHEET. |
| **RP-D42** | Storage Space Worksheet grain is one section/row group per Storage Space and is the technical calculation-trace report. |
| **RP-D43** | Worksheet includes hierarchy, frozen Geometry Family/Profile/Version, measurement datum/reference, ordered measurements, effective ullage/fill height, Product Volume, CommodityVersion, entered and normalized density, Physical Quantity, Ownership/Client, empty state, validation metadata, Issue/Evidence references and calculation version/fingerprint metadata. |
| **RP-D44** | Storage Space Worksheet never recalculates the formula; it displays the frozen calculation trace. |
| **RP-D45** | Storage Space Worksheet filters are Location, Storage Unit, Storage Space, Commodity and validation/empty outcome. |
| **RP-D46** | Capacity Report code is CAPACITY_REPORT. |
| **RP-D47** | Capacity utilization is Product Volume m³ / Max Capacity m³ × 100; numerator and denominator therefore use compatible volume units. |
| **RP-D48** | Max Capacity comes from the frozen GeometryProfileVersion and is not recalculated from current geometry. |
| **RP-D49** | Mark Empty utilization is 0% when Max Capacity is valid and greater than zero. |
| **RP-D50** | Capacity Report filters are Location, Storage Unit, Commodity and Ownership. Missing/incompatible historical capacity is N/A, utilization above 100% is not clamped, and aggregate utilization is ΣProductVolumeM3 / ΣMaxCapacityM3 ×100 over rows with valid/comparable capacity; a simple mean of row percentages is forbidden. |
| **RP-D51** | Inventory History report code is INVENTORY_HISTORY. |
| **RP-D52** | Inventory History accepts only 2..N authorized Completed Campaigns from the same Organization and their Final Snapshots. |
| **RP-D53** | Selected History campaigns are ordered by closeAt and then deterministically by Campaign identity when timestamps coincide. |
| **RP-D54** | For two campaigns History compares A→B; for more than two, change metrics are calculated between consecutive periods in RP-D53 order and first→last is not invented as the primary metric. |
| **RP-D55** | Canonical History dimensions are Close Date, Campaign, Commodity, Location, Ownership and Physical Quantity. Client may be filter/context for Third-Party but is not a seventh canonical History dimension. |
| **RP-D56** | History matching uses stable snapshot identities: Storage Space identity for scope and Commodity identity for Commodity comparison; current display names are not matching keys. |
| **RP-D57** | COMPARABLE means the same comparison unit exists in both snapshots and the semantic context required by the selected comparison has not materially changed. |
| **RP-D58** | SCOPE_ADDED means the relevant unit/Storage Space exists in the newer snapshot but not the previous snapshot. |
| **RP-D59** | SCOPE_REMOVED means the relevant unit/Storage Space exists in the previous snapshot but not the newer snapshot. |
| **RP-D60** | CONTEXT_CHANGED means a stable entity can be correlated but context relevant to the selected comparison changed materially, such as relevant Location, Ownership or Commodity assignment. |
| **RP-D61** | Absolute Change for a comparable pair is currentPhysicalQuantityKg - previousPhysicalQuantityKg. |
| **RP-D62** | Percentage Change is (current - previous) / previous × 100 only when previous > 0. |
| **RP-D63** | When previous = 0, Percentage Change is N/A, never 0%, ±100%, Infinity or a fabricated value; Absolute Change may remain when semantically valid. |
| **RP-D64** | SCOPE_ADDED, SCOPE_REMOVED and CONTEXT_CHANGED display their classification/context and do not present percentage change as if directly COMPARABLE. |
| **RP-D65** | Inventory History never produces forecasts, predictions, Book Stock, Variance or Reconciliation; the canonical term is Physical Inventory Change. |
| **RP-D66** | Digital Inventory Dossier report code is DIGITAL_INVENTORY_DOSSIER. |
| **RP-D67** | Digital Inventory Dossier exists only for Completed Campaigns and is assembled exclusively from immutable Final Snapshot/history plus authorized retained references. |
| **RP-D68** | Dossier header includes Organization, Campaign identity/name/code/type, Start Date, Planned End Date, close actor/time, Final Snapshot ID and dossier generation metadata. |
| **RP-D69** | Dossier includes Campaign Scope/hierarchy context and immutable final inventory summary. |
| **RP-D70** | Dossier includes per-Storage-Space final result: empty state, frozen geometry/version context, CommodityVersion, Ownership/Client, measurements/effective values, Product Volume, Actual Bulk Density where applicable, Physical Quantity, validation actor/time and calculation version/fingerprint metadata. |
| **RP-D71** | Dossier includes Issue references/status/resolution history relevant to the Campaign and final inventory history. |
| **RP-D72** | Dossier includes Evidence/NOTE metadata and authorized retained Evidence references; PHOTO bytes are not duplicated into the relational dossier dataset. |
| **RP-D73** | Standard users may view the Dossier in authorized scope but do not receive the general Audit Trail through the Dossier; only frozen operational actor/time/audit metadata already authorized by the business context is shown. |
| **RP-D74** | Admin may navigate separately to AUD-01; Dossier is not an authorization bypass for Admin-only Audit Trail. |
| **RP-D75** | Digital Inventory Dossier export in V6 is PDF; ZIP package export is not part of V6. |
| **RP-D76** | Dossier PDF uses the same authoritative dossier dataset as DOS-01. Embedding PHOTO binary content in the PDF is not mandatory; approved metadata/reference representation is sufficient. |
| **RP-D77** | If an Evidence binary is unavailable locally, DOS-01 marks it unavailable/offline while available historical metadata remains visible; a missing binary is never represented as locally verified. |
| **RP-D78** | Dossier view/export is read-only and does not mutate Final Snapshot, Evidence, Issue or Audit history. |
| **RP-D79** | Audit Trail report code is AUDIT_TRAIL. |
| **RP-D80** | AUD-01 is Admin-only. Standard cannot query or export the general Audit Trail through API, crafted request, offline cache or Dossier. |
| **RP-D81** | Audit Trail is append-only/read-only and derives from authoritative AuditEvent records/read models. |
| **RP-D82** | Default Audit ordering is occurredAt DESC with auditEventId as deterministic secondary ordering. |
| **RP-D83** | Audit search/filter contract supports Actor/User, Action/Event Type, Entity Type, Campaign/context/reference and UTC-aware Date Range. |
| **RP-D84** | Audit row/detail includes at minimum Event ID, occurredAt, actor identity/frozen display metadata, action/event type, entity type/ID, Campaign/context where present and source. |
| **RP-D85** | Before/after structured values, reason/note and related reference appear in detail only when present and authorized; Evidence binary content and secrets never enter Audit export. |
| **RP-D86** | actorUserId may be null only for SYSTEM events. |
| **RP-D87** | Audit Trail supports PDF and CSV export with active filters, generatedAt/by and stable event fields. |
| **RP-D88** | Audit Trail export is itself auditable as a report/export action without creating an infinite recursive chain of export events. |
| **RP-D89** | Notifications, navigation clicks, debug logs and routine sync retry noise are not fabricated as Audit Trail events. |
| **RP-D90** | Critical Audit Events follow the frozen minimum retention of at least 10 years. |
| **RP-D91** | A report may be viewed offline only when the authoritative dataset required for that request is locally available and demonstrably complete for the requested source/filter scope. |
| **RP-D92** | A partial cache cannot be presented as a complete report. UI identifies Offline / incomplete data and blocks final report/export when missing data affects the result. |
| **RP-D93** | Completed offline reports use local immutable Final Snapshot data and never reconstruct history from current master cache. |
| **RP-D94** | Offline PDF/CSV generation is permitted only when all source data necessary for the export is locally complete. |
| **RP-D95** | When an offline export creates an auditable export event, that event is durably queued and synchronized later; lack of connectivity does not justify loss of auditability. |
| **RP-D96** | Offline authorization uses previously authorized cached data only; once revocation is known locally/server-side, new access is blocked according to the frozen sync contract. |
| **RP-D97** | For every applicable aggregate report, sum(detail canonical values) = report total = aggregation of the authoritative source for the same filter scope. |
| **RP-D98** | An integrity mismatch produces an explicit report integrity error; inconsistent totals are not silently published or exported. |
| **RP-D99** | Physical Inventory, Inventory by Location, Inventory by Commodity and Inventory by Ownership produce the same canonical Physical Quantity total for the same source and filter scope. |
| **RP-D100** | Display rounding may make displayed row sums differ by a last decimal from canonical total; total is derived from canonical values, not re-summed display text. |
| **RP-D101** | Large report UI uses cursor pagination/lazy loading according to the frozen API contract; pagination never changes dataset semantics or totals. |
| **RP-D102** | Export represents the entire filtered dataset, not only the current UI page. |
| **RP-D103** | Default hierarchy-report ordering is Location Code → Storage Unit Code → Storage Space Code; grouped reports use stable group label/code plus stable identity as tie-breaker. |
| **RP-D104** | CSV row ordering and PDF detail ordering are deterministic and defined by report contract, never incidental database query order. |
| **RP-D105** | Completed report generation never runs the Geometry Engine or recalculates Physical Quantity. |
| **RP-D106** | Expensive aggregation/export must not block the primary Android interaction thread; asynchronous execution may be used without changing the authoritative dataset. |
| **RP-D107** | A report dataset may be materialized/cached as a projection but must be rebuildable from the authoritative source and preserve source identity/version needed for audit. |

## 30. Cross-document handoff

- Doc17 Security/Audit/Privacy must preserve server-side report authorization, Admin-only Audit Trail, safe export logging and protection against Evidence/secret leakage.
- Doc18 Test Strategy/Acceptance must include the reporting matrix and release gates defined here, including cross-report reconciliation and offline completeness.
- Doc19 Retention Policy must preserve Final Snapshots, critical Audit Events and retained Evidence according to their frozen minimums; report projections remain disposable/rebuildable where safe.
- Doc20 Production Operations must monitor report/export failures, integrity mismatches, projection rebuild health and performance without logging sensitive report payloads.

## 31. Freeze statement

This document is **APPROVED / FROZEN V6** and freezes the SiloMath V6 reporting contract: the exact nine reports, authoritative sources, filters, grouping, totals, PDF/CSV behavior, History comparability, Dossier/Audit authorization boundary, offline completeness, integrity checks, deterministic ordering and performance/projection constraints. Semantic changes require an explicit approved V6 Baseline Change and cross-document consistency update. Non-semantic rendering, pagination and implementation choices that preserve this contract require no Product/Baseline approval.

## Approved calculation-version cross-reference

CR-CALC-VERSION-CONTRACT-001 / FD-73: `engineVersion="2.0"`; integer `inputSchemaVersion=1`, `normalizationVersion=1`, `formulaVersion=1` for F01-F10; `surfaceModelVersion=LEVEL_BASED_V1`; `aggregationMethod=MEAN_ULLAGE`; integer `aggregationRuleVersion=1`; combined semantic identifier `MEAN_ULLAGE_V1`. Formula codes are the unchanged F01-F10 identifiers in Doc13 sections6/22.1. Domain/API version numbers are integers except engineVersion, which is the exact string "2.0". Resolve only actual implemented handlers for the full pinned tuple; unsupported versions return GEOM_VERSION_UNSUPPORTED without fallback. Preserve historical metadata verbatim; no automatic reassignment to this initial approved tuple. This clarification changes no mathematics and is not evidence of completed integration or release gates.

Reports continue reading immutable historical values; never run the current tuple over older data or substitute it for stored historical identifiers.

## Approved Country contract cross-reference

CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74: For canonical new Country values, persisted/API identity is uppercase ISO 3166-1 alpha-2 and the human-readable country name is presentation only. Labels do not replace stored codes or become an alternative historical authority. Non-conforming historical values and immutable snapshots are not normalized/rebuilt under this clarification; separate audit is required before any migration. The existing report set and report semantics remain unchanged.

# SiloMath V6 — 08 MODULE_ARCHITECTURE

**Document ID:** 08_MODULE_ARCHITECTURE  
**Version:** V6 Final Documentation — Module Architecture 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-27 - CR-COUNTRY-CATALOG-CONTRACT-001 approved Country catalogue clarification
**Product:** SiloMath  
**Normative role:** Canonical implementation contract for Gradle module boundaries, ownership, allowed dependencies, composition, dependency injection and incremental migration.

## 1. Purpose and authority

This document translates the approved V6 product and architecture contract into an implementable Gradle module topology. It is subordinate to the frozen Product Baseline, Decision Register, Business Rules, Roles & Permissions, Workflows, UI/Navigation specification and Architecture document. If a module convenience conflicts with a frozen business rule, permission, workflow, UI contract, offline rule, calculation rule or historical-integrity rule, the higher-level contract wins.

The target architecture is pragmatic modularization, not micro-modularization. There is no module per screen, entity, use case or database table. Feature modules may depend on approved Core modules; Core modules must not depend on Feature modules. `:app` is the composition root and contains minimal business logic. The Calculation Engine is a separate pure Kotlin boundary.

## 2. Frozen target module inventory

| Module | Category | Primary ownership |
| --- | --- | --- |
| :app | Application composition | Application root, Hilt/application bootstrap, top-level shell composition, root Jetpack Navigation 3 stack/scene assembly, environment wiring. |
| :core:model | Shared contract models | Small stable cross-module value models/enums/identifiers that genuinely cross feature/domain boundaries; no behavior-heavy aggregates. |
| :core:common | Cross-cutting primitives | Result/error primitives, clock/dispatcher abstractions, safe utility primitives, shared constants only when truly cross-cutting. |
| :core:domain | Domain contracts | Repository interfaces, cross-feature use cases/domain services, authorization/domain policies, canonical business invariants that are not calculation-engine formulas. |
| :core:database | Local persistence | Room database, entities, DAOs, migrations, transactions, local data sources, local read models, outbox persistence. |
| :core:network | Remote transport | Retrofit services, OkHttp configuration/interceptors, DTOs, API error decoding, auth transport support, evidence transfer clients. |
| :core:sync | Offline synchronization | Sync coordinator, WorkManager workers, outbox processing, dependency/order enforcement, retry classification, conflict persistence/orchestration, offline package refresh/integrity orchestration. |
| :core:designsystem | Reusable UI system | Material 3 theme, typography, spacing, adaptive primitives, reusable controls, status visuals, accessibility primitives. |
| :core:navigation | Navigation contracts | Navigation 3 type-safe/serializable destination keys, stable-ID arguments, back-stack/navigation interfaces/helpers and shell destination metadata. |
| :core:testing | Shared test support | Fakes/builders/test clocks/dispatchers, fixtures and assertions shared across module tests. |
| :feature:auth | Authentication feature | AUT-01, AUT-02, AUT-SH-01; session UI/state, sign-in/reset flows and auth-specific orchestration. |
| :feature:dashboard | Dashboard feature | DSH-01; authorized operational summary/read models and navigation actions. |
| :feature:masterdata | Master-data feature | ORG/LOC/STU/STS/GEO/COM/CLI screens and sheets; organization context UI, customer-managed master-data workflows and read-only governed commodity/family views. |
| :feature:campaigns | Campaign feature | CAM-01/02/03; create & launch, scope selector, team assignment, campaign detail/list. |
| :feature:stocktaking | Stocktaking feature | STK-01/02/03/05 and P0-SH-01 Report Issue; measurement, Mark Empty, Validate, Reset Validation authorization flow, issue/evidence interaction. |
| :feature:close | Inventory Close feature | CLS-01/02; review, Close Guards presentation, online atomic Close orchestration and immutable snapshot presentation. |
| :feature:reports | Reporting feature | RPT-01/02, HIS-01, DOS-01; nine canonical report types except Audit Trail UI ownership below, snapshot-based datasets and PDF/CSV export initiation. |
| :feature:administration | Administration/platform feature | USR-01/02, MOR-01, NOT-01, SYN-01, SET-01, SUP-01, AUD-01; users, settings, notifications, sync status, support and Admin-only Audit Trail. |
| :calculation-engine | Pure Kotlin calculation engine | Deterministic Measurement normalization/aggregation, EffectiveFillHeight, 10 geometry-family volume functions, quantity calculation, validation/fingerprint/version metadata. |



**Frozen target count:** 19 Gradle modules. Any additional production module requires an approved architecture change; temporary migration modules are permitted only when explicitly documented and removed after migration.

## 3. Architectural invariants

- Dependency direction points from composition/presentation toward abstractions and domain; infrastructure implements inward-facing contracts.
- Feature -> Core is allowed according to the matrix in this document. Core -> Feature is prohibited.
- Feature -> Feature implementation dependency is prohibited. Cross-feature coordination occurs through navigation contracts, domain/use-case contracts, shared models or the app composition root.
- `:app` may depend on features and core modules to assemble the runtime graph, but must not become a business-logic dumping ground.
- Compose UI never calls DAOs or Retrofit services and never performs volumetric/business calculations.
- ViewModels expose immutable UiState via StateFlow and dispatch intents/actions to use cases; they do not execute SQL or network logic. Feature Compose UI collects ViewModel state lifecycle-aware.
- Repository interfaces belong at the domain boundary; concrete offline-first implementations are wired by DI and use local/remote data sources.
- Room is the canonical client read source for operational data where available; synchronized mutations are durable local writes plus SyncOperation/outbox in one transaction.
- Protected remote operations are server-authorized; hiding UI controls is not an authorization boundary.
- Completed inventory, snapshots, audit history and historically referenced master versions cannot be destructively rewritten by module-local convenience.
- All official calculation behavior remains deterministic and independently testable outside Android.



## 4. Module catalog and boundaries

### 4.1 :app

| Aspect | Contract |
| --- | --- |
| Category | Application composition |
| Owns | Application root, Hilt/application bootstrap, top-level shell composition, root Jetpack Navigation 3 stack/scene assembly, environment wiring. |
| May depend on | All feature modules; core:navigation, core:designsystem, core:common and required runtime core implementations through DI composition. |
| Must not own/depend on | Feature/domain business rules, SQL, Retrofit calls, calculation formulas. |



### 4.2 :core:model

| Aspect | Contract |
| --- | --- |
| Category | Shared contract models |
| Owns | Small stable cross-module value models/enums/identifiers that genuinely cross feature/domain boundaries; no behavior-heavy aggregates. |
| May depend on | core:common only when unavoidable; prefer Kotlin stdlib. |
| Must not own/depend on | Android UI, Room entities/DAOs, Retrofit DTOs, feature-specific UiState. |



### 4.3 :core:common

| Aspect | Contract |
| --- | --- |
| Category | Cross-cutting primitives |
| Owns | Result/error primitives, clock/dispatcher abstractions, safe utility primitives, shared constants only when truly cross-cutting. |
| May depend on | Kotlin/JVM and minimal Android APIs only when the primitive is Android-specific. |
| Must not own/depend on | Business use cases, feature navigation, DB/network implementations. |



### 4.4 :core:domain

| Aspect | Contract |
| --- | --- |
| Category | Domain contracts |
| Owns | Repository interfaces, cross-feature use cases/domain services, authorization/domain policies, canonical business invariants that are not calculation-engine formulas. |
| May depend on | core:model, core:common, calculation-engine where a domain service must invoke it. |
| Must not own/depend on | Compose, ViewModel, Room, Retrofit, Android lifecycle, concrete repositories. |



### 4.5 :core:database

| Aspect | Contract |
| --- | --- |
| Category | Local persistence |
| Owns | Room database, entities, DAOs, migrations, transactions, local data sources, local read models, outbox persistence. |
| May depend on | core:model, core:common; Room/SQLite. |
| Must not own/depend on | Feature UI, Retrofit, navigation, business calculations. |



### 4.6 :core:network

| Aspect | Contract |
| --- | --- |
| Category | Remote transport |
| Owns | Retrofit services, OkHttp configuration/interceptors, DTOs, API error decoding, auth transport support, evidence transfer clients. |
| May depend on | core:model, core:common; Retrofit/OkHttp/serialization. |
| Must not own/depend on | Compose, Room DAOs, feature ViewModels, business rules. |



### 4.7 :core:sync

| Aspect | Contract |
| --- | --- |
| Category | Offline synchronization |
| Owns | Sync coordinator, WorkManager workers, outbox processing, dependency/order enforcement, retry classification, conflict persistence/orchestration, offline package refresh/integrity orchestration. |
| May depend on | core:domain, core:model, core:common, core:database, core:network. |
| Must not own/depend on | Feature UI implementations; direct ownership of business state machines. |



### 4.8 :core:designsystem

| Aspect | Contract |
| --- | --- |
| Category | Reusable UI system |
| Owns | Material 3 theme, typography, spacing, adaptive primitives, reusable controls, status visuals, accessibility primitives. |
| May depend on | core:model only for truly shared presentation-safe enums; Compose/Material. |
| Must not own/depend on | Repositories, DB/network, business calculations, feature navigation destinations. |



### 4.9 :core:navigation

| Aspect | Contract |
| --- | --- |
| Category | Navigation contracts |
| Owns | Navigation 3 type-safe/serializable destination keys, stable-ID arguments, back-stack/navigation interfaces/helpers and shell destination metadata. |
| May depend on | core:model, core:common. |
| Must not own/depend on | Feature implementation internals, repositories, Room/Retrofit. |



### 4.10 :core:testing

| Aspect | Contract |
| --- | --- |
| Category | Shared test support |
| Owns | Fakes/builders/test clocks/dispatchers, fixtures and assertions shared across module tests. |
| May depend on | May depend on core contracts needed by tests; test-only dependencies. |
| Must not own/depend on | Production runtime dependency from app/features. |



### 4.11 :feature:auth

| Aspect | Contract |
| --- | --- |
| Category | Authentication feature |
| Owns | AUT-01, AUT-02, AUT-SH-01; session UI/state, sign-in/reset flows and auth-specific orchestration. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation. |
| Must not own/depend on | Concrete DB/network implementations; other feature internals. |



### 4.12 :feature:dashboard

| Aspect | Contract |
| --- | --- |
| Category | Dashboard feature |
| Owns | DSH-01; authorized operational summary/read models and navigation actions. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation. |
| Must not own/depend on | Other feature implementations. |



### 4.13 :feature:masterdata

| Aspect | Contract |
| --- | --- |
| Category | Master-data feature |
| Owns | ORG/LOC/STU/STS/GEO/COM/CLI screens and sheets; organization context UI, customer-managed master-data workflows and read-only governed commodity/family views. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation, calculation-engine only through approved domain-facing contracts where capacity preview is required. |
| Must not own/depend on | Direct Room/Retrofit; campaign/stocktaking feature internals. |



### 4.14 :feature:campaigns

| Aspect | Contract |
| --- | --- |
| Category | Campaign feature |
| Owns | CAM-01/02/03; create & launch, scope selector, team assignment, campaign detail/list. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation. |
| Must not own/depend on | Stocktaking UI internals; concrete transport/storage. |



### 4.15 :feature:stocktaking

| Aspect | Contract |
| --- | --- |
| Category | Stocktaking feature |
| Owns | STK-01/02/03/05 and P0-SH-01 Report Issue; measurement, Mark Empty, Validate, Reset Validation authorization flow, issue/evidence interaction. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation, calculation-engine through domain orchestration. |
| Must not own/depend on | Direct SQL/Retrofit; Close feature internals. |



### 4.16 :feature:close

| Aspect | Contract |
| --- | --- |
| Category | Inventory Close feature |
| Owns | CLS-01/02; review, Close Guards presentation, online atomic Close orchestration and immutable snapshot presentation. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation. |
| Must not own/depend on | Direct DB/network; report recalculation. |



### 4.17 :feature:reports

| Aspect | Contract |
| --- | --- |
| Category | Reporting feature |
| Owns | RPT-01/02, HIS-01, DOS-01; nine canonical report types except Audit Trail UI ownership below, snapshot-based datasets and PDF/CSV export initiation. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation. |
| Must not own/depend on | Recalculation of completed history; direct storage/network. |



### 4.18 :feature:administration

| Aspect | Contract |
| --- | --- |
| Category | Administration/platform feature |
| Owns | USR-01/02, MOR-01, NOT-01, SYN-01, SET-01, SUP-01, AUD-01; users, settings, notifications, sync status, support and Admin-only Audit Trail. |
| May depend on | core:domain, core:model, core:common, core:designsystem, core:navigation, core:sync through exposed status/action contracts. |
| Must not own/depend on | Business data persistence internals; direct feature-to-feature implementation coupling. |



### 4.19 :calculation-engine

| Aspect | Contract |
| --- | --- |
| Category | Pure Kotlin calculation engine |
| Owns | Deterministic Measurement normalization/aggregation, EffectiveFillHeight, 10 geometry-family volume functions, quantity calculation, validation/fingerprint/version metadata. |
| May depend on | Kotlin stdlib; its own pure contracts. It may consume primitive/value types that remain JVM-pure; no Android dependency. |
| Must not own/depend on | Compose, Android UI, ViewModel, Room, Retrofit, WorkManager, Hilt Android types. |



## 5. Allowed dependency graph

| From | Allowed production dependencies | Explicitly prohibited |
| --- | --- | --- |
| :app | all feature modules; core composition modules; runtime infrastructure modules | business rules/formulas; direct SQL/HTTP orchestration in UI |
| feature:* | core:domain, core:model, core:common, core:designsystem, core:navigation; calculation-engine only where contractually needed | feature:* implementation modules; concrete Room/Retrofit APIs |
| core:domain | core:model, core:common; calculation-engine when domain orchestration requires it | Android UI, Room, Retrofit, feature:* |
| core:sync | core:domain, core:model, core:common, core:database, core:network | feature:* implementation modules |
| core:database | core:model, core:common | core:network, feature:*, Compose |
| core:network | core:model, core:common | core:database, feature:*, Compose |
| core:designsystem | presentation-safe shared models only | domain repositories/use cases, database/network/sync |
| core:navigation | core:model/core:common as needed for stable IDs/contracts | feature implementation classes, data sources |
| core:testing | test-only access to contracts under test | production runtime dependency |
| :calculation-engine | pure Kotlin/JVM contracts only | Android/Compose/Hilt/Room/Retrofit/WorkManager/ViewModel |



## 6. Dependency rules by layer

| Layer | Owned artifacts | Allowed calls | Forbidden |
| --- | --- | --- | --- |
| Presentation | Compose screens, UiState, ViewModels, presentation mappers | Use cases/domain-facing interfaces, navigation contracts | DAO, SQL, Retrofit, calculation formulas |
| Domain | Entities/value objects, policies, repository interfaces, use cases/domain services | Pure domain/calculation contracts | Android framework, concrete persistence/transport |
| Data | Repository implementations, local/remote data sources, mapping | Room/Retrofit and domain interfaces | Compose/navigation decisions |
| Sync | Outbox processing, workers, conflict/retry orchestration | Data infrastructure + domain guards | Silent merge/force overwrite/business state invention |
| Calculation | Pure deterministic numeric logic | Pure inputs/outputs | Android/data-source/presentation dependencies |



## 7. Feature ownership map

| Feature module | Owned V6 UI components / workflows |
| --- | --- |
| :feature:auth | AUT-01, AUT-02, AUT-SH-01 |
| :feature:dashboard | DSH-01 |
| :feature:masterdata | ORG-01, ORG-02, LOC-01, LOC-02, STU-02, STS-02, GEO-01, GEO-02, COM-01, COM-02, CLI-01, CLI-02, P1-SH-01/02/03, GEO-SH-01, CLI-SH-01 |
| :feature:campaigns | CAM-01, CAM-02, CAM-03 including contextual Scope Selector and Team Assignment |
| :feature:stocktaking | STK-01, STK-02, STK-03, STK-05, P0-SH-01 |
| :feature:close | CLS-01, CLS-02 |
| :feature:reports | RPT-01, RPT-02, HIS-01, DOS-01 |
| :feature:administration | USR-01, USR-02, MOR-01, NOT-01, SYN-01, SET-01, SUP-01, AUD-01 |



This ownership map covers all 36 screens and 7 sheets frozen in Document 06. Ownership is implementation responsibility, not permission: authorization remains defined by Document 04 and server/domain guards.

## 8. Domain ownership and cross-feature contracts

| Domain area | Primary contract ownership | Feature consumers |
| --- | --- | --- |
| Identity/session/membership | core:domain | auth, masterdata, campaigns, stocktaking, close, reports, administration |
| Organization/master hierarchy | core:domain | masterdata, campaigns, stocktaking, reports |
| Campaign/scope/team/snapshot | core:domain | campaigns, stocktaking, close, reports |
| Stocktaking/measurement/validation | core:domain | stocktaking, close, reports |
| Issue/resolution/evidence metadata | core:domain | stocktaking, close, reports, administration/audit |
| Final Inventory Snapshot | core:domain | close, reports |
| Sync/conflict semantics | core:domain contracts + core:sync orchestration | all mutating features; administration status |
| Calculation contracts | calculation-engine + core:domain orchestration | masterdata capacity, stocktaking, close validation/tests |



A feature may define presentation-only models locally. A model becomes shared only when two or more modules need the same stable semantic contract. Shared placement is not justified merely to avoid a small duplicate UI mapper.

## 9. Model separation rules

- `core:model` must not become a universal bucket. Persisted Room entities remain in `core:database`; Retrofit request/response DTOs remain in `core:network`; UiState and UI-only enums remain in the owning feature.
- Domain entities/value objects live in `core:domain` unless they are calculation-engine-native pure types.
- Mapping is explicit: DB entity <-> domain model <-> network DTO as required. No Room annotations or serialization annotations leak into domain models.
- Stable IDs are UUID-based technical identifiers. Business codes are separate values; Campaign Code is server-generated/read-only.
- Money-like or physical decimal values use the approved deterministic decimal policy; floating-point convenience must not change persisted or official results.



## 10. Repository architecture

Repository interfaces are inward-facing domain contracts. Concrete implementations combine local and remote data sources according to the offline-first policy. Reads normally observe Room. Writes that are offline-capable commit business state and an outbox operation atomically before reporting success. Online-only operations such as Campaign Close call the authoritative server workflow and then reconcile the local source of truth.

| Repository concern | Required behavior |
| --- | --- |
| Master data | Organization-scoped reads; version-aware refresh; historical references preserved. |
| Campaigns | Server-generated official Campaign Code at Save & Launch; frozen scope/team snapshot semantics. |
| Stocktaking | Local-first durable mutations; 1–5 measurements for non-empty; audited validation/reset rules. |
| Issues/Evidence | Offline creation where allowed; controlled binary transfer; metadata/hash durable. |
| Close | Online atomic server operation; no partial Final Snapshot. |
| Reports | Completed official outputs from Final Inventory Snapshot; no current-master recalculation. |



## 11. Dependency injection contract

- Hilt is the approved DI framework. `:app` owns application-level composition and installs/aggregates bindings needed to construct the runtime graph.
- Each infrastructure module exposes narrowly scoped bindings for its implementations; feature modules request interfaces/use cases rather than concrete DAOs or Retrofit services.
- ViewModels use constructor injection. Service locator patterns and global mutable singletons are prohibited.
- Qualifiers must be used for environment-specific endpoints, dispatchers, clocks or multiple implementations rather than ad-hoc branching.
- Test graphs replace interfaces with fakes/in-memory implementations without requiring production network/database access.
- Calculation Engine functions/classes should prefer explicit pure construction and should not require Hilt.



## 12. Navigation modularization

- `core:navigation` owns Navigation 3 destination keys, back-stack contracts and stable argument types; destinations use stable IDs, never serialized mutable business objects.
- `app` assembles the root graph and persistent operational shell: Dashboard / Inventory Campaigns / Stocktaking / Locations / More.
- Feature modules expose navigation entry contracts/builders without importing other feature implementation classes.
- Deep links must pass authentication and authorization before destination content is exposed.
- STK-05 -> STK-03 back navigation must restore Storage Unit context and relevant list/scroll state.
- Authentication remains outside the operational shell.



## 13. Offline-first module interaction

The canonical mutation path is: Feature ViewModel -> Use Case -> Repository -> Room transaction (business mutation + SyncOperation) -> UiState success -> `core:sync` schedules/replays -> `core:network` -> server authorization/idempotency/version validation -> acknowledgement/pull -> Room reconciliation.

- `core:sync` owns scheduling mechanics, not domain truth.
- Same-entity operations preserve FIFO/dependency ordering; independent streams may run concurrently where safe.
- Retries reuse the same idempotency key. Exponential backoff applies to retryable failures.
- Conflicts are durable and never silently overwritten/auto-merged for critical inventory data.
- Campaign offline working sets are campaign-scoped and integrity-verifiable; no whole-organization replication is required.
- Unsynced measurements/evidence are never discarded merely because the process/app restarts or connectivity fails.



## 14. Database boundary

- `core:database` owns the RoomDatabase, entities, DAOs, migrations, converters and transaction boundaries.
- Feature modules do not import DAO/entity packages.
- Database migrations are forward-safe and tested; destructive fallback is prohibited for production business data.
- No destructive cascade may remove operational/history records from master-data changes.
- Mutable synchronized entities carry approved version and mutation metadata; immutable snapshots/audit use immutable creation metadata.
- Exact tables/columns/keys/indexes belong to Document 10, not this document.



## 15. Network boundary

- `core:network` owns Retrofit/OkHttp transport, serialization, interceptors, API DTOs and machine-readable error decoding.
- Base URLs are environment/build configuration, never editable user settings; Production cannot be switched to Development/Staging by a user.
- Protected operations are authorized server-side. Client role/UI state is advisory only.
- Synchronized mutations carry idempotency key and base/entity version.
- API errors expose stable code, message, field errors, correlationId and retryable classification as defined by the frozen V6 contract.
- Evidence binaries use controlled transfer and hash verification, not ordinary business JSON payloads.



## 16. Calculation Engine module

`:calculation-engine` is the strongest isolation boundary. It implements Calculation Engine v2.0: normalization -> MEAN_ULLAGE_V1 aggregation -> effective fill height -> family volume -> quantity. It supports the ten frozen geometry families F01–F10, deterministic decimal arithmetic, canonical units, validation/fingerprint/version metadata and no intermediate rounding.

- No Android SDK, Compose, ViewModel, Hilt Android integration, Room, Retrofit, WorkManager or navigation dependency.
- Unit tests execute on the JVM without emulator/device.
- Geometry Family is mathematical model; Geometry Profile is concrete versioned dimensions/context outside the formula implementation.
- F08–F10 consume verified area/provenance contract; the engine must not invent area.
- Mark Empty bypasses measurement/density calculation and produces explicit zero-result semantics through domain orchestration.
- Historical snapshots retain calculation/version metadata and are never recalculated using current profiles.



## 17. Security and authorization boundaries

- Authorization policies are domain/server concerns; UI modules may hide/disable actions but cannot grant privilege.
- Organization isolation applies to repositories, offline package selection, API requests and local queries.
- Admin does not automatically have Stocktaking mutation rights; Campaign Team membership remains required.
- Close is Admin-only and online. Audit Trail is Admin-only.
- Tenant creation is platform/onboarding provisioning; ordinary organization Admin does not arbitrarily create a new tenant.
- Tokens/secrets are not stored in source, logs or ordinary Room business tables. Session credentials use approved secure Android storage.



## 18. Evidence and file ownership

- Feature Stocktaking owns capture interaction; domain owns evidence rules; database owns metadata/local references; network owns controlled transfer; sync owns durable upload orchestration.
- Photo binary storage is app-private locally and object storage remotely; DB stores metadata/hash/object references.
- JPEG normalization, EXIF orientation, 2048px long edge, target <=1.5MB, hard max 3MB and SHA-256 follow the frozen Evidence contract.
- Unnecessary EXIF is stripped; GPS/device identifiers are not automatically collected.
- Validated evidence is read-only; historical evidence references remain stable.



## 19. Reporting boundary

- `feature:reports` renders/exports the Single Authoritative Report Dataset supplied through domain/repository contracts.
- Completed official reports read immutable Final Inventory Snapshot data.
- UI and PDF/CSV export use the same authoritative dataset semantics.
- Inventory History compares 2..N Completed Campaigns with frozen comparability classifications; no forecasting.
- `AUD-01` is owned by `feature:administration` because its permission/operational nature is administration, while Audit Trail remains one of the nine canonical report types.



## 20. Administration/platform ownership

- `feature:administration` groups low-coupling platform/admin surfaces to avoid unnecessary micro-modules: users, More, notifications, sync status, settings, support and Audit Trail.
- Grouping does not merge their domain rules; permissions remain independently enforced.
- Settings cannot expose server URL, sync interval, conflict strategy, retention, calculation rules or force-overwrite controls.
- Organization Configuration exposes only approved organization-level values; absence of configured values is valid.



## 21. Coroutines, Flow and dispatchers

- Structured concurrency is mandatory; lifecycle work is scoped to ViewModel/use-case/worker lifetimes. Compose collection is lifecycle-aware and inactive destinations do not keep unnecessary UI-bound collection active.
- Repositories expose Flow for observable local state where appropriate; ViewModels convert domain streams to immutable StateFlow/UiState.
- Dispatchers are injected/abstracted where test determinism matters.
- Long-running sync uses WorkManager, not unmanaged background coroutines.
- Cancellation must not leave half-written business/outbox transactions.



## 22. Error and result propagation

- Infrastructure errors are mapped into stable domain/application error types before presentation.
- UI does not parse exception strings or HTTP message text to decide business behavior.
- Retryable transport errors, conflicts, permanent domain rejection and authentication/authorization failures remain distinguishable.
- Correlation IDs flow through diagnostics where available and must not expose secrets.



## 23. Build configuration and dependency governance

- Use Kotlin DSL and a central Gradle version catalog for dependency/plugin versions.
- Dependency versions/providers may evolve without changing module responsibilities and do not require Product/Baseline approval. A non-semantic architectural-role choice is recorded through IADR; any change to frozen module responsibilities or product behavior requires Baseline Change.
- Prefer implementation dependencies; expose `api` only when downstream compilation genuinely requires a public type.
- Ban accidental feature-to-feature and Core-to-Feature edges with automated dependency checks where practical.
- Keep debug tooling out of release runtime and never package production secrets.



## 24. Public API rules between modules

- Default visibility is internal/private. A module exposes the smallest stable surface required by consumers.
- Public contracts use domain/shared models, not Room entities or Retrofit DTOs.
- Feature entry APIs expose destination/navigation contracts and DI-friendly factories/builders only as needed.
- Breaking a public module contract requires coordinated migration and tests; avoid cyclic compatibility shims.



## 25. Test ownership

| Module family | Mandatory test focus |
| --- | --- |
| calculation-engine | Golden formula tests, boundary validation, precision, all 10 families, fingerprints/versions. |
| core:domain | Business rules, permissions, state transitions, Close Guards, ownership/client rules, reset validation. |
| core:database | DAO queries, transactions, migrations, outbox atomicity, referential protection. |
| core:network | DTO mapping, error envelope, idempotency/version headers/body, serialization. |
| core:sync | Retry/backoff classification, FIFO/dependencies, conflict handling, restart recovery, package integrity. |
| feature:* | ViewModel/UDF state, lifecycle-aware collection, state-driven operation outcomes, validation presentation, navigation actions, permissions/visibility, predictive-back/edge-to-edge/adaptive Compose UI acceptance. |
| :app | Composition/root Navigation 3/back-stack/deep-link smoke tests, predictive back, edge-to-edge shell and DI graph integration. |



`core:testing` provides reusable fakes/fixtures but is never a production runtime dependency.

## 26. Source-set and test-fixture rules

- Production code belongs in main source sets; test helpers remain in test/testFixtures/core:testing.
- Do not ship fake repositories, test credentials or synthetic production bypasses.
- Fixtures for versioned geometry/snapshots must be deterministic and version-controlled.



## 27. Module naming and package conventions

- Gradle paths are exactly those in the frozen inventory unless an approved architecture change says otherwise.
- Package namespace follows `com.silomath...` and module responsibility; package names must not recreate obsolete SiloMatch terminology.
- Feature package structure may use presentation/domain-facing adapters internally but must respect module-level dependency rules.
- Do not encode lifecycle state in package/module names.



## 28. Incremental migration strategy

The target topology is reached incrementally after auditing the existing dependency graph; no big-bang rewrite is required.

| Stage | Migration action | Exit criterion |
| --- | --- | --- |
| M1 | Inventory current Gradle modules/packages and forbidden edges; freeze behavior. | Dependency map exists; no behavior change. |
| M2 | Establish core:model/common/domain contracts and pure calculation-engine boundary. | Pure contracts compile/test independently. |
| M3 | Extract database/network infrastructure behind repository interfaces. | Features no longer depend directly on DAO/Retrofit. |
| M4 | Establish core:sync durable outbox/worker boundary. | Sync mechanics isolated and tested. |
| M5 | Extract design system/navigation contracts. | Shared UI/navigation primitives no longer duplicated. |
| M6 | Move functionality into frozen feature modules one bounded feature at a time. | No feature-to-feature implementation edges. |
| M7 | Reduce :app to composition/root navigation/DI. | Business logic removed from app. |
| M8 | Enforce architecture checks and remove temporary migration shims. | Target graph passes automated/manual audit. |



- Every migration step must compile and run tests before the next step.
- Do not combine unrelated refactors with business-rule changes.
- Preserve Room data and migrations; modularization is not permission for destructive schema reset.
- Temporary adapters must be explicitly marked and have a removal target.



## 29. Prohibited architectures and anti-patterns

- One module per screen/entity/table/use case.
- Feature-to-feature implementation imports.
- Core depending on Feature.
- Compose -> DAO/Retrofit.
- ViewModel -> SQL/Retrofit service.
- Room entity reused as API DTO/domain/UI model.
- Global service locator or mutable singleton state.
- Calculation formulas inside Compose/ViewModel/repository/network layers.
- Silent last-write-wins for critical inventory data.
- Generic Force Overwrite conflict action.
- Destructive DB migration to simplify modularization.
- Whole-organization offline replication when campaign-scoped package is sufficient.
- Business rules hidden in navigation callbacks or serializers.



## 30. Architecture enforcement

- Gradle dependency declarations are reviewed against the allowed graph.
- Static/module dependency tests should fail prohibited Core->Feature and Feature->Feature edges.
- Pure Kotlin calculation-engine build/test must succeed without Android plugin/runtime.
- Architecture review accompanies any new module, cross-module public API or dependency inversion change.
- Document 18 must include acceptance tests/checks for critical architectural invariants.



## 31. Traceability to frozen V6 decisions

| Frozen decision area | Module-architecture consequence |
| --- | --- |
| FD-48 Target modular architecture | Exact pragmatic module inventory in Section 2. |
| FD-49 Dependency direction | Feature -> Core; Core !-> Feature; app composition root. |
| FD-50 Pure Kotlin Calculation Engine | Dedicated `:calculation-engine` isolation. |
| FD-51 Incremental migration | Section 28 staged migration; no big-bang rewrite. |
| D29/D30 Offline/sync | `core:sync` + database/network/domain separation and durable outbox. |
| D18 Close | feature:close presentation/orchestration + authoritative server transaction. |
| D19/D20 snapshots | Domain/database contracts preserve immutable historical state. |
| D21/D22/D23 reporting | feature:reports snapshot-based read/export boundary. |
| FC/IC frozen field/implementation rules | Feature UI owns controls; domain/data/network modules enforce canonical semantics, metadata, security and error contracts. |



## 32. Relationship to Documents 09–20

| Document | What it will refine without changing this module contract |
| --- | --- |
| 09 DATA_MODEL | Exact entities, attributes, relationships and ownership. |
| 10 DATABASE_SCHEMA | Room/PostgreSQL tables, columns, keys, indexes, migrations. |
| 11 API_SPECIFICATION | Endpoints, DTOs, auth/idempotency/version/error contracts. |
| 12 OFFLINE_SYNC_SPECIFICATION | Queue/package/conflict algorithms and lifecycle. |
| 13 CALCULATION_ENGINE_SPECIFICATION | Exact formulas/parameters/validation/versioning. |
| 14 COMMODITY_SPECIFICATION | Governed commodity/version/density contract. |
| 15 EVIDENCE_STORAGE_SPECIFICATION | Binary/metadata/storage/upload lifecycle. |
| 16 REPORTING_SPECIFICATION | Nine report datasets/filters/exports. |
| 17 SECURITY_AUDIT_PRIVACY | Threat/security/audit/privacy controls. |
| 18 TEST_STRATEGY_ACCEPTANCE | Module tests, architecture tests and acceptance traceability. |
| 19 RETENTION_POLICY | Retention/cleanup constraints. |
| 20 PRODUCTION_OPERATIONS | Environment, release, monitoring, backup/restore and incident operations. |



## 33. Module architecture acceptance criteria

- **MA-01** The target inventory contains exactly the 19 frozen production/test-support Gradle modules listed in Section 2.
- **MA-02** No Core production module depends on a Feature module.
- **MA-03** No Feature module depends on another Feature implementation module.
- **MA-04** `:app` is the composition root and contains minimal business logic.
- **MA-05** `:calculation-engine` is pure Kotlin and Android-framework independent.
- **MA-06** Compose/ViewModels have no direct DAO/Retrofit access.
- **MA-07** Repository interfaces and domain policies are separated from concrete Room/Retrofit implementations.
- **MA-08** Room remains the canonical client read source for offline-first operational data where available.
- **MA-09** Durable offline-capable mutations persist business change + outbox atomically.
- **MA-10** `core:sync` owns synchronization mechanics without inventing business state transitions.
- **MA-11** Navigation uses Jetpack Navigation 3 type-safe/serializable destination keys, stable IDs, supported predictive back, and preserves the frozen 43-component navigation contract.
- **MA-12** All 43 UI components have an owning Feature module.
- **MA-13** Authorization is not granted by presentation; protected operations are server/domain guarded.
- **MA-14** Completed snapshots/history cannot be destructively mutated by feature/infrastructure convenience.
- **MA-15** Evidence ownership is separated across capture, domain rules, metadata storage, binary transfer and sync.
- **MA-16** Completed reporting uses Final Inventory Snapshot data and does not recalculate current master data.
- **MA-17** Hilt bindings preserve dependency inversion and are replaceable in tests.
- **MA-18** Production runtime does not depend on `core:testing`.
- **MA-19** Environment endpoints are build-controlled, not user-editable.
- **MA-20** Migration is incremental, compile/test gated and preserves production data.
- **MA-21** Forbidden dependency edges and calculation-engine purity are architecture-testable.
- **MA-22** Documents 09–21 may refine contracts but may not contradict this frozen module topology without approved change.
- **MA-23** `feature:masterdata` owns LOC-01 List/Map presentation while map provider SDKs remain infrastructure/presentation dependencies and never business-data authority.
- **MA-24** Ordinary non-semantic dependency/provider/framework decisions that preserve this contract proceed without Product/Baseline approval; IADR is used only when architecturally material.



## 34. Freeze statement

This document is **APPROVED / FROZEN V6** and freezes the V6 module topology and dependency contract. Implementation may evolve internally without Product/Baseline approval when it preserves this contract; architecturally material implementation choices may be recorded through IADR. Adding/removing production modules, reversing dependency direction, merging the Calculation Engine into Android layers, introducing feature-to-feature implementation coupling, or moving business logic into `:app` requires an explicit approved Baseline Change. Details delegated to Documents 09–21 must remain consistent with this contract.

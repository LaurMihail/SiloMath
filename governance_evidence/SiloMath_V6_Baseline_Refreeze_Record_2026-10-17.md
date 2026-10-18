# SiloMath V6 — Baseline Change & Refreeze Record

**Record date:** 2026-10-17  
**Disposition:** APPROVED AND APPLIED  
**Resulting baseline:** SiloMath V6 APPROVED / FROZEN — implementation-closure refreeze  

## 1. Approval basis

The Product owner explicitly approved the Android/UX modernization decision package and additionally required the official documentation to be implementation-ready, with known implementation-discovered documentation gaps closed so ordinary implementation does not require repeated Product approvals.

The previously separated Maps/Geo proposal is included in this refreeze because Product had explicitly requested modern Location maps and subsequently approved the closure package. Its scope is limited to facility-location presentation and master data; it does not introduce user/device tracking, GPS acquisition, background location or turn-by-turn navigation.

## 2. Applied baseline changes

1. Navigation modernization: Jetpack Navigation 3, type-safe/serializable destination keys, stable-ID arguments.
2. Predictive Back contract and testing.
3. Edge-to-edge/system-inset/cutout/gesture/IME contract and testing.
4. Lifecycle-aware Compose state observation and durable state-driven operation outcomes.
5. Window-adaptive semantics and current adaptive-primitive freedom.
6. Stitch minimum visual-quality floor with no business-semantic authority.
7. Dynamic color cannot override SiloMath canonical semantic/brand tokens.
8. Android release performance contract: R8 full mode, Baseline/Startup Profiles and Macrobenchmark evidence.
9. Location facility map capability: optional paired latitude/longitude, List/Map parity, Admin mutation, Standard read, local-first sync, privacy/security/retention/ops/test/design rules.
10. Documentation-set correction: Doc21 recognized throughout the final set.
11. `/sync/pull` documentation correction.
12. User/Membership transport provenance closure: distinct canonical metadata/entityVersion; no fabrication.
13. Environment endpoint provisioning closure: concrete environment base URLs/hostnames are controlled deployment config, not Product constants.
14. Implementation-governance closure: non-semantic choices require no Product/Baseline approval; IADR for architecturally material choices; Baseline Change only for semantic/product changes.

## 3. Documents directly modified

All 21 documents carry the refreeze revision marker. Substantive contract updates were applied where required across Documents 01–12 and 17–21. Documents 13–16 retained their domain semantics and received explicit approved/frozen implementation-closure wording.

No new official baseline document was added; the official set remains exactly 21 documents.

## 4. Frozen invariants explicitly preserved

- exactly 19 production Gradle modules;
- native Android / Kotlin / Compose;
- Feature → Core allowed, Core → Feature prohibited;
- `:app` composition root with minimal business logic;
- pure Kotlin `:calculation-engine`;
- Room/local-first read authority for supported field operations;
- business mutation + `SyncOperation` in one local transaction before UI success;
- exact roles Admin / Standard;
- Campaign lifecycle Active / Completed only;
- Save & Launch, no Draft campaign;
- exact five top-level operational destinations and order;
- Auth outside operational shell;
- exact 36 full screens + 7 sheets = 43 major UI components;
- Close Admin-only, online, atomic, exactly one Final Inventory Snapshot;
- exact 24 Commodity catalog;
- exact nine report types;
- historical/versioned/immutable integrity rules;
- `/api/v1` canonical backend boundary;
- tenant authorization and security/privacy boundaries.

## 5. Governance after refreeze

The implementation team does not seek Product/Baseline approval for ordinary non-semantic choices that preserve Documents 01–21. Where a technical choice is architecturally material, an IADR may record it without changing the Product Baseline.

A Baseline Change is required only when:

- an intentional future change alters a frozen product/business/UI/data/API/offline/security/architecture semantic; or
- a genuine semantic contradiction between frozen documents is discovered and cannot be resolved by the existing authority/precedence rules.

This rule is part of the frozen baseline through FD-69 and the specialist-document closure text.

## 6. Validation evidence

The final refreeze validator completed with **152 PASS / 0 FAIL** at the time this record was produced. The package contains a SHA-256 manifest for the exact 21 official Markdown files.

**Refreeze status: FINAL / APPROVED / FROZEN.**

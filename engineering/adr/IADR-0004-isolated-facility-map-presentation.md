# IADR-0004 — Isolated native facility-map presentation

## Metadata

- ID: IADR-0004
- Status: ACCEPTED
- Owner: SiloMath
- Decision date: 2026-10-27
- Supersedes: None
- Superseded by: None

## Context

IMP-MD-002 requires the real native facility-map presentation of the authorized local Location read model. Doc06 explicitly delegates map provider/SDK selection to implementation. This record documents that technical choice within the user-authorized LOC-01 implementation; it does not assert separate Product approval of a specific external tile service.

## Frozen baseline references

01_PRODUCT_BASELINE; 02_DECISION_REGISTER; 06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION LOC-01, LOC01-01..06 and section16 facility-map clarification; 08_MODULE_ARCHITECTURE feature:masterdata/app composition ownership; 12_OFFLINE_SYNC_SPECIFICATION basemap best-effort; 17_SECURITY_AUDIT_PRIVACY SEC-D147, SEC-AC12; 20_PRODUCTION_OPERATIONS controlled deployment configuration; 21_UI_DESIGN_SYSTEM_FINAL visual/accessibility/adaptive rules.

## Decision

Use MapLibre Native Android OpenGL 13.7.1 (`org.maplibre.gl:android-sdk-opengl`) as a presentation-only renderer in feature:masterdata. Keep existing SDK/Kotlin/Compose/OkHttp pins. Map geometry and stable IDs are created locally from the same scoped filtered Room-backed Location flow as List. The SDK is not an operational data store and does not establish offline eligibility or READY.

App supplies optional controlled per-environment HTTPS raster XYZ template and attribution text/URL through BuildConfig. There is no hardcoded public provider, remote style, remote glyph, geocoding, GPS/location component, or provider credential. Empty/invalid configuration yields an unavailable basemap while locally positioned markers and List remain available. Deployment must supply an appropriately licensed service whose terms permit the configured tile caching; this decision does not provision or approve a paid service.

Install a dedicated OkHttp client into MapLibre, with no SiloMath auth interceptors/cookies/session repository. Requests are restricted to the configured host/port, numeric XYZ path and exact static query; external targets fail closed. Requests are rebuilt as GET without original body or business/auth headers; only provider HTTP cache/range validators and fixed technical Accept/User-Agent are permitted. Redirects are disabled, SDK logging is disabled and URLs are not logged. Tile cache is bounded to 40 MiB in Android cacheDir and is disposable/non-authoritative. No facility name, ID, Organization/user name, Evidence or audit payload is sent; viewport-derived tile addresses are technically required map requests. Production manifest removes coarse/fine/background location permissions contributed by dependencies.

## Alternatives considered

- Hosted WebView/remote map page: adds remote page/script/privacy control surface and weaker native lifecycle/accessibility integration.
- Provider-specific SDK: unnecessary service/credential coupling for the frozen presentation contract.
- Static marker-only diagram: does not fulfill the actual map presentation requirement.

## Consequences

A native dependency is added, with isolated global MapLibre HTTP configuration initialized once. Native lifecycle and low-memory hooks are explicitly managed. Neutral local style and local marker source work without remote basemap. List remains the complete accessible alternative; labeled marker actions offer equivalent stable-ID navigation. Provider/network errors never change authoritative data or authentication. Cache may be evicted at any time. Operational provider availability/terms are a deployment responsibility.

## Verification impact

Direct ViewModel tests cover authorized source/search/filter parity, missing-position rows, scope revocation and stable-ID navigation. Direct interceptor tests cover allowlist, credential/header/body removal and dependency failures. Emulator Compose tests exercise native SDK initialization without a provider, accessible marker navigation, fallback/List continuity, UI states and large-font short-window scrolling. Production merged manifest is inspected for absence of location permissions. No real provider availability, paid-service contract or full tile-cache availability is claimed by local tests.

## Baseline impact check

Changes frozen V6 baseline: NO

This map-provider decision introduces no new screen identity, permission, role, business state, Country semantics or offline activation rule. P1-SH-01 and FD-74 are implemented separately within the same authorized MD-002 task; this IADR changes neither their contract nor historical values.

## Notes

API provenance consulted during implementation: https://maplibre.org/maplibre-native/android/examples/getting-started/ and official MapLibre Android API documentation for HttpRequestUtil, MapView, Style and GeoJsonSource. Exact artifact is pinned in gradle/libs.versions.toml. No Supabase mutation or external provider provisioning.

# SiloMath V6 — UI Design System

**Document ID:** 21  
**Version:** V6 UI Design System 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-28 - CR-API-STORAGE-SPACE-BATCH-001 approved atomic Storage Space batch-create clarification
**Product:** SiloMath  
**Platform:** Android native  
**UI toolkit:** Jetpack Compose  
**Scope:** Global visual and interaction design rules only  
**Normative role:** Canonical visual-design authority for SiloMath V6. Documents 01–20 remain authoritative for product semantics and behavior; Document 21 governs the visual system and shared presentation rules.

---

## 1. Purpose

This document defines the global visual system used to implement the approved SiloMath V6 user interface consistently across phones and tablets.

Document 06 remains the canonical authority for:
- screen and sheet inventory;
- fields and controls;
- navigation and back behavior;
- permissions;
- UI states;
- validation;
- offline behavior;
- screen-specific acceptance criteria.

Document 21 defines how those approved elements are presented visually. It is the canonical authority for visual tokens, typography, spacing, shapes, shared component presentation, accessibility presentation, and adaptive visual rules.

Documents 01–21 form the authoritative SiloMath V6 documentation set. Authority is domain-specific: Document 06 remains canonical for UI inventory, navigation, behavior, permissions, fields, states, and acceptance criteria; Document 21 is canonical for visual presentation. If Document 21 conflicts with Documents 01–20 on product semantics or behavior, Documents 01–20 prevail and the conflict must be reported rather than interpreted away.

The visual tokens frozen in this document are approved SiloMath V6 visual-design decisions. They are not retroactively represented as requirements originating in Documents 01–20.

No visual component may introduce a new business concept merely because that concept appears in an example, mockup, icon, status color, label, or design-system component.

Approved SiloMath Stitch references are the **minimum visual-quality floor** for production implementation. They are evidence of expected hierarchy, density, refinement, state coverage and enterprise polish, but they never override the semantic contracts in Documents 01–20. Where this document and Doc06 already define the semantic boundary, the implementation may improve composition, motion, responsive layout, visualizations and component refinement above the Stitch reference without additional Product/Baseline approval.

---

## 2. Design Principles

### 2.1 Industrial B2B
SiloMath uses a professional, restrained, high-legibility visual language suitable for physical inventory work in industrial bulk-storage environments.

### 2.2 Operational clarity
The interface prioritizes identification, measurement, validation, sync state, issue state, and traceability over decoration.

### 2.3 No superfluous chrome
Avoid:
- decorative gradients;
- glassmorphism;
- backdrop blur;
- ambient glow;
- decorative telemetry;
- ornamental charts without an approved data purpose;
- excessive shadows;
- animation that does not communicate state or navigation.

Surfaces are predominantly opaque, structurally separated by spacing, tonal contrast, and subtle borders.

### 2.4 Status is never color-only
Critical state must always include text and/or an icon in addition to color.

### 2.5 Field usability
Controls must remain usable in field conditions:
- minimum interactive target: 48dp × 48dp;
- strong contrast;
- explicit units for numeric measurements;
- obvious editable vs read-only presentation;
- predictable placement of primary actions.

### 2.6 V6 terminology only
Visual examples and reusable components must use canonical V6 terminology. Do not introduce Reconciliation, Draft Campaign, Upcoming, In Progress campaign status, LiDAR, ultrasonic sensing, temperature telemetry, seal breach, recount, book-stock variance, calibration charts, or other unapproved concepts.

### 2.7 Stitch is floor, not ceiling
The approved Stitch screen set is a minimum production visual benchmark, not a frozen pixel-for-pixel implementation. Compose implementation may exceed it through clearer hierarchy, richer authorized data visualization, better adaptive layouts, stronger accessibility and more refined interaction feedback, provided no product semantic is added or changed.

### 2.8 Platform-native modern behavior
SiloMath presentation must integrate with current Android platform behavior rather than visually emulating an older fixed-canvas application. Edge-to-edge, system insets, IME avoidance, predictive-back progress, window resizing and process recreation must preserve hierarchy, focus, state and accessibility.

---

## 3. Visual Foundation

### 3.1 Color Tokens

The palette is a restrained slate/white foundation with a deep cobalt primary accent.

| Token | Value | Use |
|---|---:|---|
| `color.background` | `#F8FAFC` | Main application canvas |
| `color.surface` | `#FFFFFF` | Cards, panels, fields, sheets |
| `color.surfaceSubtle` | `#F1F5F9` | Nested/secondary containers |
| `color.border` | `#E2E8F0` | Standard separators and card borders |
| `color.borderStrong` | `#CBD5E1` | Interactive/structural borders |
| `color.textPrimary` | `#0F172A` | Primary text |
| `color.textSecondary` | `#475569` | Secondary text and labels |
| `color.textMuted` | `#64748B` | Tertiary metadata |
| `color.disabled` | `#94A3B8` | Disabled/inactive presentation |
| `color.primary` | `#0F3460` | Primary actions and active navigation |
| `color.primaryPressed` | `#0A2342` | Pressed primary action |
| `color.primarySoft` | `#E0E7FF` | Selected/active tonal background |
| `color.success` | `#059669` | Positive/validated/synchronized semantic accent |
| `color.successSurface` | `#ECFDF5` | Success semantic surface |
| `color.warning` | `#D97706` | Warning/pending semantic accent |
| `color.warningSurface` | `#FFFBEB` | Warning semantic surface |
| `color.error` | `#DC2626` | Error/conflict/destructive semantic accent |
| `color.errorSurface` | `#FEF2F2` | Error semantic surface |

### 3.2 Semantic color rules

Colors communicate presentation semantics, not new domain states.

Approved domain labels remain authoritative:
- Campaign: `Active`, `Completed`;
- Stocktaking classification: `To Do`, `Issues`, `Validated`; `All` is a UI filter;
- Issue: `Open`, `Resolved`;
- synchronization wording follows the approved offline/sync contract.

Do not rename an approved state to match a color token.

### 3.3 Dynamic color policy
Android system dynamic color is **not** an authoritative SiloMath color source. Production UI uses the frozen SiloMath semantic/brand tokens above so status meaning, contrast and cross-device identity remain stable. Dynamic system colors may not replace or mutate primary, semantic success/warning/error, synchronization, offline or accessibility-critical tokens. Any purely technical system-surface integration must preserve these canonical tokens and requires no separate Product approval.

---

## 4. Typography

### 4.1 Font family

Use **Inter** as the canonical SiloMath UI typeface in both Stitch references and the Android implementation.

The application must package/use Inter consistently rather than allowing device-dependent typography to become part of the visual contract. A system sans-serif fallback may be used only as a technical failure fallback when Inter cannot be loaded; it is not an alternative approved visual style.

No separate decorative or telemetry font is required.

### 4.2 Type scale

| Token | Size | Weight | Line height | Typical use |
|---|---:|---:|---:|---|
| `headlineLarge` | 32sp | 700 | 40sp | Rare top-level overview heading |
| `headlineMedium` | 24sp | 600 | 32sp | Major screen/section heading |
| `headlineSmall` | 20sp | 600 | 28sp | Screen title / major subsection |
| `titleLarge` | 18sp | 600 | 24sp | Card or pane title |
| `titleMedium` | 16sp | 600 | 24sp | List/card title |
| `titleSmall` | 14sp | 600 | 20sp | Compact structural label |
| `bodyLarge` | 16sp | 400 | 24sp | Primary body/content |
| `bodyMedium` | 14sp | 400 | 20sp | Default operational content |
| `bodySmall` | 12sp | 400 | 16sp | Secondary metadata |
| `labelLarge` | 14sp | 600 | 20sp | Buttons and important labels |
| `labelMedium` | 12sp | 600 | 16sp | Filters/chips/field metadata |
| `labelSmall` | 11sp | 600 | 16sp | Compact status/supporting labels |

### 4.3 Numeric presentation
Use tabular figures for changing or aligned numeric values where supported. Measurement values and units must remain visually distinguishable. Do not use typographic styling to imply precision beyond the approved calculation/display contract.

---

## 5. Spacing and Grid

SiloMath uses an 8dp base grid with a 4dp micro-step.

| Token | Value |
|---|---:|
| `space.xs` | 4dp |
| `space.sm` | 8dp |
| `space.md` | 16dp |
| `space.lg` | 24dp |
| `space.xl` | 32dp |
| `space.2xl` | 40dp |

Default phone horizontal margin: **16dp**.  
Tablet horizontal margin: **24dp** minimum, increasing to **32dp** where width permits.

Spacing must prioritize scanability and efficient field operation rather than visual decoration.

---

## 6. Shapes and Borders

| Token | Radius | Use |
|---|---:|---|
| `radius.small` | 4dp | Fields, compact controls, small chips |
| `radius.medium` | 8dp | Cards, buttons, list containers |
| `radius.large` | 12dp | Sheets/dialog containers |
| `radius.full` | Full | Status dots/pills where semantically appropriate |

Default structural border: **1dp `#E2E8F0`**.  
Stronger interactive border: **1dp `#CBD5E1`**.  
Focused input border: **2dp `color.primary`**.  
Error input border: **2dp `color.error`**.

Avoid excessive rounding that makes the application resemble a consumer/social product.

---

## 7. Elevation

Visual hierarchy should rely primarily on spacing, tonal surfaces, and borders.

- Base canvas: no shadow.
- Standard card/list container: no shadow or very subtle Material-compatible elevation.
- Active menu/dropdown: restrained elevation.
- Top app bar: separator preferred over heavy shadow.
- Bottom sheets/dialogs/FAB: elevation permitted where needed to establish layering.
- Blur is not permitted as a standard SiloMath surface treatment.

---

## 8. Core Components

### 8.1 Top App Bar
Operational screens use a clear top app bar appropriate to their Doc06 contract.

Where required by Doc06 it may contain:
- app/screen identity;
- back navigation;
- synchronization affordance/status;
- notifications;
- avatar/user context.

Do not add destinations or account actions that are not defined by V6.

Recommended height: 64dp where a standard single-row app bar is appropriate.

### 8.2 Bottom Navigation
Operational shell destinations are exactly:

1. Dashboard
2. Inventory Campaigns
3. Stocktaking
4. Locations
5. More

Use the same order everywhere. Active destination uses `color.primary` plus a visible selected treatment. Inactive destinations use neutral text/icon treatment.

Do not replace these destinations with design-system examples.

### 8.3 Buttons
Primary:
- solid `color.primary`;
- white text;
- 8dp radius;
- minimum 48dp interactive height.

Secondary:
- white/surface background;
- structural border;
- primary or high-emphasis neutral text.

Tertiary:
- transparent surface;
- clear text/icon affordance.

Destructive:
- use error semantic styling only for genuinely destructive or irreversible actions approved by V6.

Button labels must use the exact action terminology from the relevant screen contract.

### 8.4 Text Inputs and Selectors
Default field height: 52–56dp depending on Compose component constraints.

Requirements:
- explicit label;
- clear focus state;
- clear error state;
- units visible for measurement fields where applicable;
- numeric keyboard for numeric input;
- password visibility control only where specified;
- read-only fields must not visually imitate editable inputs without a clear read-only treatment.

### 8.5 Search
Search fields use the same input family but may use a compact leading search icon. Search scope and searchable attributes are defined by Doc06/field contracts, not by this document.

### 8.6 Cards and List Rows
Cards:
- white surface;
- 1dp border;
- 8dp radius;
- 16dp typical internal padding.

Operational lists should favor efficient rows over oversized marketing cards. Large datasets must remain compatible with lazy list implementations.

### 8.7 Status Chips
Status chips must include readable text. A dot or icon may supplement the label.

Suggested semantic presentation:
- positive/final validated state: success family;
- warning/pending attention: warning family;
- error/conflict: error family;
- neutral/inactive/context: slate family.

The actual displayed state text must always come from the V6 contract.

### 8.8 Sync Indicator
Synchronization state should be visible where required by the V6 screen/offline contract.

The indicator must:
- contain text, not color alone;
- distinguish synchronized, pending/offline, syncing, error/conflict as defined by the approved sync specification;
- never imply successful synchronization merely because data exists locally.

### 8.9 Filters, Tabs and Segmented Controls
Use compact controls with at least 48dp accessible touch bounds.

For Stocktaking, labels are exactly:
`To Do / Issues / Validated / All`.

For Campaign status filtering:
`Active / Completed / All` where the relevant screen contract permits `All`.

Do not introduce Draft, Upcoming, In Progress, Recount, Reconciled, or other unapproved statuses.

### 8.10 Bottom Sheets and Dialogs
Use 12dp container radius and clear hierarchy:
- title;
- contextual explanation if required;
- fields/content;
- explicit primary/secondary actions.

Sheets must implement only the seven sheet contracts defined by Doc06 unless a later approved V6 change adds another.

### 8.11 Empty, Loading, Error and Offline States
Every screen should render the states defined in its Doc06 contract.

Empty states:
- concise;
- operational;
- no invented business action.

Loading:
- skeleton/progress presentation may be used;
- must not display stale values as current without indication.

Error:
- clear message;
- retry only where retry is valid.

Offline/local:
- clearly distinguish local availability from server synchronization.

### 8.12 Edge-to-edge, system bars and IME
- Primary Compose surfaces render edge-to-edge on supported Android versions.
- Content must consume/apply system-bar, display-cutout and gesture insets deliberately; critical content/actions may not be obscured by system UI.
- Form layouts must remain usable when the IME is visible; focused fields and primary actions must not become unreachable.
- Top/bottom app chrome must coordinate with system bar appearance while preserving SiloMath contrast tokens.
- Insets are presentation mechanics and must not alter screen semantics or navigation destinations.

### 8.13 Predictive back presentation
- Back-capable destinations, sheets and overlays must participate in Android predictive-back behavior where supported.
- Predictive-back progress may drive platform-compatible transition feedback but must not commit a business mutation before the navigation action actually completes.
- Unsaved-work/business guards defined by Doc06 remain authoritative; custom back interception is allowed only when required by those contracts.
- Back animation/transition must respect reduced-motion accessibility settings.

### 8.14 Location map presentation
The approved LOC-01 map is a presentation of the same authorized, filtered `Location` dataset as the List view.

Visual contract:
- use a restrained neutral basemap so SiloMath markers and labels remain primary;
- every marker represents an existing authorized `Location` with stored facility coordinates;
- selected marker has an explicit visual + semantic selected state;
- marker clustering is an allowed implementation technique for density/performance and does not create a business aggregate;
- map status/selection may not be encoded only by color;
- locations without coordinates remain available in List and are never hidden from the authoritative dataset;
- basemap/provider failure uses an explicit degraded-map state while List/detail remain usable;
- map-provider attribution/licensing requirements must remain visible where required;
- no user/device location affordance is shown because V6 does not request location permission or track the user/device.

Accessibility contract:
- every map-reachable Location is also reachable through an accessible List representation;
- map markers expose a meaningful Location label and selected state to accessibility services where the SDK permits;
- map-only gestures are never the sole way to reach a business action;
- zoom/pan do not replace accessible navigation through the Location list/detail hierarchy.

---

## 9. Adaptive Layout

Compact/Medium/Expanded are **window-context presentation ranges**, not permanent device classes. Layout responds to the current available window, orientation, resizing/multi-window and applicable posture. A tablet may render Compact in a constrained window; a phone may expose a wider window context in supported scenarios. Business capability and permissions never depend on device class.

Material 3 Adaptive primitives such as adaptive navigation suites, list-detail or supporting-pane scaffolds may be used when they preserve Doc06 contracts and the five canonical top-level destinations. Exact library/API selection is an implementation decision and requires no Product/Baseline approval.

### 9.1 Compact window — under 600dp
- 16dp horizontal margin;
- single-column primary workflow;
- persistent bottom navigation on operational shell screens;
- forms stack vertically;
- avoid horizontal scrolling for primary workflows.

### 9.2 Medium window — 600dp to 839dp
- 24dp horizontal margin;
- wider lists and forms;
- adaptive navigation presentation may use a navigation rail when appropriate to the approved V6 adaptive architecture;
- two-pane/master-detail treatment may be used where the relevant screen contract permits it;
- the five canonical destinations and their order/identity remain unchanged.

### 9.3 Expanded window — 840dp and above
- 32dp horizontal margin where appropriate;
- navigation rail is the preferred adaptive presentation for the same five canonical operational destinations;
- multi-column, list-detail, or supporting-pane layouts may be used where the relevant screen contract permits;
- adaptive presentation must never create, remove, rename, reorder, or semantically change canonical destinations.

### 9.4 Orientation, resize and multi-window
Portrait and landscape must both remain usable. Rotation, resize, split-screen/multi-window and posture changes may rearrange layout but must not change permissions, available actions, workflow state, selected business entity or business semantics. Transient presentation state should restore where technically applicable without fabricating domain state.

---

## 10. Accessibility

Minimum requirements:
- 48dp × 48dp interactive target;
- meaningful content descriptions for actionable icons;
- text/icon labels for critical status;
- no color-only error/state communication;
- sufficient foreground/background contrast;
- support Android font scaling without clipping critical actions or values;
- logical TalkBack traversal order;
- form errors associated with their field;
- selected state programmatically exposed for tabs/navigation;
- read-only vs editable state exposed semantically as well as visually;
- motion/transition behavior respects reduced-motion accessibility preferences where applicable;
- map content has an equivalent accessible List path and no business action is map-gesture-only;
- predictive-back presentation preserves focus/semantics and does not hide required unsaved-work warnings.

Target WCAG-style contrast:
- normal text: at least 4.5:1;
- large text / large UI text: at least 3:1;
- meaningful non-text UI boundaries and state indicators: at least 3:1 where applicable.

---

## 11. Iconography

Use one consistent Android-compatible icon family, preferably Material Symbols/Material Icons where licensing and implementation permit.

Rules:
- icons support labels; they do not replace ambiguous business terminology;
- use conventional Android symbols for back, search, visibility, notifications, sync, overflow, add, edit, delete, filter, calendar and camera;
- do not create domain icons suggesting sensors or features absent from V6;
- destructive icons use error styling only when the action is actually destructive.

---

## 12. Screen Construction Rules for Stitch and Compose

For every V6 screen:

1. Read the exact Doc06 screen contract.
2. Include every required field/control and no unapproved business control.
3. Apply this design system only to presentation.
4. Preserve canonical terminology.
5. Preserve exact navigation destinations.
6. Preserve role visibility rules.
7. Represent required loading/empty/error/offline states.
8. Keep critical status explicit in text.
9. Do not infer business data from decorative examples.
10. Treat static mockups as visual references, not proof of runtime behavior.
11. Meet or exceed the approved Stitch visual-quality floor without inventing product semantics.
12. Apply edge-to-edge/insets/IME and predictive-back behavior consistently with Doc06/07/18.
13. Adapt by current window context rather than hard-coding phone/tablet identity.
14. For LOC-01, keep List and Map synchronized to the same authorized/filter scope and preserve List fallback under map-provider failure.

For Stitch generation, prompts must explicitly state that Documents 01–20 remain authoritative for behavior and that this document supplies visual rules only. Stitch output is a visual benchmark/reference, not an independent source of business semantics.

---

## 13. Prohibited Design Drift

The following must not be introduced by the design system unless separately approved by the product contract:

- Reconciliation module or reconciliation workflow;
- ERP/book-stock comparison;
- variance acceptance workflow;
- Draft Campaign state;
- Upcoming or In Progress Campaign status;
- customer-editable Geometry Families/formulas;
- LiDAR/ultrasonic/IoT sensor telemetry;
- temperature monitoring;
- seal-breach workflows;
- recount workflows;
- calibration charts;
- additional bottom-navigation destinations;
- additional user roles;
- desktop-only navigation assumptions;
- business states encoded only by color;
- hidden destructive actions;
- visual controls that imply unsupported functionality.

---

## 14. Design Tokens — Compose Mapping

Recommended semantic token groups for `core/designsystem`:

- `SiloMathColors`
- `SiloMathTypography`
- `SiloMathSpacing`
- `SiloMathShapes`
- `SiloMathElevation`
- `SiloMathDimensions`

Reusable components should be semantic rather than screen-specific, for example:

- `SiloMathTopAppBar`
- `SiloMathBottomNavigation`
- `SiloMathPrimaryButton`
- `SiloMathSecondaryButton`
- `SiloMathTextField`
- `SiloMathSearchField`
- `SiloMathStatusChip`
- `SiloMathSyncIndicator`
- `SiloMathFilterChip`
- `SiloMathCard`
- `SiloMathListRow`
- `SiloMathEmptyState`
- `SiloMathErrorState`
- `SiloMathOfflineBanner`
- `SiloMathLocationMap`
- `SiloMathLocationMarker`
- adaptive shell/navigation primitives that preserve the five canonical destinations

`SiloMathLocationMap`/marker APIs accept presentation-safe Location read-model data and selection callbacks; they do not own authorization, filtering, persistence, geocoding or business mutation.

Component APIs must not embed business states that belong to feature/domain modules.

---

## 15. Acceptance Checklist

The following design-system contract is approved and frozen:

- [x] Color tokens approved.
- [x] Typography approved.
- [x] Spacing/grid approved.
- [x] Shapes/radii approved.
- [x] Elevation/no-blur rule approved.
- [x] Button/input/card/list styling approved.
- [x] Status and sync presentation approved.
- [x] Canonical operational navigation presentation approved.
- [x] Phone/tablet/orientation rules approved.
- [x] Accessibility baseline approved.
- [x] Icon family approved.
- [x] Prohibited design drift list accepted.
- [x] Stitch minimum visual-quality floor approved.
- [x] Dynamic-color override is prohibited for canonical SiloMath semantic/brand tokens.
- [x] Edge-to-edge, system-inset and IME presentation rules approved.
- [x] Predictive-back presentation rules approved.
- [x] Window-context adaptive/resizing rules approved.
- [x] Location List/Map presentation and accessible List fallback approved.
- [x] Map-provider degradation and no-device-location-affordance rules approved.

Implementation gate: before bulk-generating the complete Stitch screen set, generate one reference screen from Document 06 + Document 21 and verify it against both contracts. This is an implementation QA gate, not a condition on the frozen status of Document 21. Once that gate passes, later screens that conform to Doc06/21 do not require per-screen Product approval; visual QA is verified through Doc18 acceptance and regression evidence.

---

## 16. Freeze Rule

This document is **APPROVED / FROZEN V6** and is the canonical visual-design authority for SiloMath V6.

Changes to frozen colors, typography, spacing, shapes, shared semantic component presentation, accessibility baseline, canonical navigation identity, Location-map semantics or adaptive visual rules require an explicit approved Baseline Change/revision of Document 21.

Non-semantic implementation choices — including exact Compose component decomposition, internal animation implementation, adaptive library primitive, map SDK/provider, marker clustering algorithm, rendering optimization and other technical details — require **no Product/Baseline approval** when they preserve Documents 01–21. Architecturally material choices may be recorded by IADR.

Changes to product behavior must not be made here; they belong in the appropriate V6 product/functional document. The 2026-10-17 refreeze is intended to be implementation-ready: implementation applies the explicit cross-document authority/precedence rules without seeking Product approval for ordinary technical choices. Only a newly discovered genuine semantic contradiction that cannot be resolved by those authority rules, or an intentional future product change, triggers the Baseline Change process.

**End of Document 21 — SiloMath V6 UI Design System**

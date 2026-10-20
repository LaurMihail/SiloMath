# SiloMath V6 — User Roles & Permissions

**Document ID:** 04_USER_ROLES_PERMISSIONS  
**Version:** V6 Final Documentation — Roles & Permissions 1.0  
**Status:** APPROVED / FROZEN V6
**Baseline revision:** 2026-10-20 — CR-DB-CAMPAIGN-SNAPSHOT-COMMODITY-001 baseline-change refreeze
**Product:** SiloMath  
**Normative role:** Canonical authorization and permission contract for organization roles, Campaign Team membership, user lifecycle, operation-level permissions, and UI access in V6.

---

## 1. Purpose

This document defines who may view or mutate SiloMath V6 data and which additional conditions apply. It must be enforced consistently in UI behavior, use cases, repositories, API authorization, offline working-set authorization, synchronization, and tests.

## 2. Authorization Dimensions

SiloMath authorization is not determined by role alone. Effective permission is the intersection of:

1. authenticated user identity and Active lifecycle state;
2. valid Organization membership;
3. organization role: Admin or Standard;
4. authorized Organization/data scope;
5. Campaign access and, for Stocktaking mutations, Campaign Team membership;
6. entity/workflow state, such as Active vs Completed or Open vs Resolved;
7. connectivity and synchronization guards where required;
8. server-side authorization and integrity validation.

Client-side UI visibility is not an authorization boundary. Protected remote operations must be re-authorized by the server.

## 3. Canonical Roles

### 3.1 Admin

Admin is the organization-administration role. Admin can manage customer-controlled Master Data, create and launch Campaigns, define scope/team before launch, manage users and Organization Configuration, review Campaigns, close Campaigns, and access Audit Trail, subject to all workflow, scope, connectivity, and server guards.

Admin does **not** automatically receive Stocktaking mutation rights. Stocktaking mutations require Campaign Team membership.

### 3.2 Standard

Standard is the operational/read role. Standard can read authorized Master Data, Campaigns and reports and can perform Stocktaking mutations when assigned to the Campaign Team. Standard cannot mutate Master Data, administer Campaign configuration, close Campaigns, access Audit Trail, or modify Organization Configuration.

### 3.3 Campaign Team membership

Campaign Team membership is an operational authorization dimension independent from Admin/Standard. Both roles require Campaign Team membership for Stocktaking mutations. A launching Admin need not be a Campaign Team member.

## 4. User Lifecycle

| State | Authentication / operations | Assignment | History |
|---|---|---|---|
| Active | May authenticate and operate according to authorization | May receive assignments | Preserved |
| Disabled | Cannot start new authentication or new operations | Cannot receive new assignments | Identity and historical references remain preserved |

Reactivation restores the same user identity rather than creating a replacement identity.

## 5. Operation-Level Permission Matrix

| Operation | Admin | Standard | Additional conditions | Business-rule trace |
|---|---|---|---|---|
| Authenticate / restore own session | Allowed | Allowed | User must be Active; session restoration is secure and automatic. | BR-GOV-003, BR-AUTH-001 |
| Select an authorized Organization | Allowed | Allowed | Only Organizations for which the user has valid membership/authorization. | BR-GOV-001, BR-GOV-004 |
| View Dashboard | Allowed | Allowed | Within authorized Organization and data scope. | BR-GOV-001 |
| View Locations / Storage Units / Storage Spaces | Allowed | Allowed | Read access only within authorized scope. | BR-AUTHZ-001 |
| Create/Edit/Activate/Inactivate Location | Allowed | Not allowed | Admin-only customer-managed Master Data mutation. | BR-AUTHZ-001 |
| Set/Move/Clear Location map position | Allowed | Not allowed | Admin-only Location master-data mutation; stored facility coordinates only; no device-location tracking permission. | BR-AUTHZ-001, BR-MD-010, BR-MD-011, BR-MD-012 |
| Create/Edit/Activate/Inactivate Storage Unit | Allowed | Not allowed | Admin-only customer-managed Master Data mutation. | BR-AUTHZ-001 |
| Create/Edit/Activate/Inactivate Storage Space | Allowed | Not allowed | Admin-only customer-managed Master Data mutation; bulk/range behavior is Add-only. | BR-AUTHZ-001, BR-MD-009 |
| View Geometry Profiles | Allowed | Allowed | Within authorized scope. | BR-AUTHZ-001 |
| Create/Edit/Activate/Inactivate Geometry Profile | Allowed | Not allowed | Admin-only; only within platform-governed Geometry Families. | BR-AUTHZ-001, BR-MD-004 |
| Create/Edit Geometry Family or formula | Not allowed | Not allowed | Platform-governed System-Controlled Rule. | BR-MD-004, BR-SET-003 |
| View Commodities / Commodity Details | Allowed | Allowed | Commodity definitions are platform-governed and read-only to organization users. | BR-MD-007 |
| Create/Edit Commodity definition | Not allowed | Not allowed | Platform-governed System-Controlled Rule. | BR-MD-007, BR-SET-003 |
| View Clients | Allowed | Allowed | Within authorized Organization. | BR-MD-003 |
| Create/Edit/Activate/Inactivate Client | Allowed | Not allowed | Admin-only customer-managed Master Data mutation. | BR-AUTHZ-001, BR-MD-003 |
| View Inventory Campaigns / Campaign Details | Allowed | Allowed | Within authorized scope. | BR-GOV-001 |
| Create & Launch Campaign | Allowed | Not allowed | Admin-only; Save & Launch, no Draft. | BR-AUTHZ-002, BR-CAM-002 |
| Manage Campaign Scope before launch | Allowed | Not allowed | Admin-only; Scope freezes after Save & Launch. | BR-AUTHZ-002, BR-CAM-007 |
| Manage Campaign Team before launch | Allowed | Not allowed | Admin-only; at least one team member required. | BR-AUTHZ-002, BR-CAM-006 |
| Modify Campaign Scope after launch | Not allowed | Not allowed | Scope is immutable after Save & Launch. | BR-CAM-007 |
| View Stocktaking progress | Allowed | Allowed | Within authorized Campaign/Organization scope; mutation rights are separate. | BR-GOV-001, BR-AUTHZ-003 |
| Create/Edit Stocktaking measurements | Conditional | Conditional | User must be authorized and a Campaign Team member. Admin role alone does not grant mutation rights. | BR-AUTHZ-003 |
| Set/Change Commodity, density, Ownership/Client in Stocktaking | Conditional | Conditional | User must be an authorized Campaign Team member; applicable non-empty validation rules apply. | BR-AUTHZ-003, BR-OWN-001, BR-OWN-002 |
| Add Stocktaking Note/Photo | Conditional | Conditional | Authorized Campaign Team member; evidence limits/storage rules apply. | BR-AUTHZ-003, BR-STK-010, BR-STK-011 |
| Mark Empty | Conditional | Conditional | Authorized Campaign Team member; creates Validated empty record and audit metadata. | BR-AUTHZ-003, BR-STK-007, BR-STK-008 |
| Validate Stocktaking Record | Conditional | Conditional | Authorized Campaign Team member; all applicable validation rules must pass. | BR-AUTHZ-003, BR-STK-003 |
| Edit Geometry from Stocktaking | Not allowed | Not allowed | Geometry is read-only in Stocktaking; anomalies use Report Issue. | BR-STK-006 |
| Report Issue | Conditional | Conditional | Authorized Campaign Team member; Issue Type and Description mandatory. | BR-AUTHZ-006, BR-ISS-002 |
| Resolve Issue during normal campaign work | Conditional | Conditional | Requires authorized campaign user and explicit audited resolution; exact UI availability is contextual. | BR-AUTHZ-006, BR-ISS-003 |
| Accept issue matter for follow-up at Close | Allowed | Not allowed | Admin-only close handling; Issue must transition Open→Resolved with reason/audit. | BR-AUTHZ-006, BR-ISS-005 |
| View Campaign Review information | Allowed | Allowed | Authorized users may inspect Review information within scope. | BR-AUTHZ-004 |
| Close Inventory Campaign | Allowed | Not allowed | Admin-only, online, server-revalidated, all Close Guards satisfied. | BR-AUTHZ-004, BR-CLS-001, BR-CLS-002, BR-CLS-003 |
| View Final Inventory Snapshot | Allowed | Allowed | Within authorized scope. | BR-SNP-001 |
| View standard Reports | Allowed | Allowed | Within authorized scope. | BR-AUTHZ-005, BR-RPT-001 |
| View Inventory History | Allowed | Allowed | Within authorized scope; Completed Campaign snapshots only. | BR-AUTHZ-005, BR-HIS-001 |
| View Digital Inventory Dossier | Allowed | Allowed | Within authorized scope. | BR-AUTHZ-005 |
| View Audit Trail | Allowed | Not allowed | AUD-01 is Admin-only. | BR-AUTHZ-005 |
| View Users / User Details | Allowed | Not allowed | Administration function; user-management mutation is Admin-only under V6 role model. | BR-GOV-002, BR-AUTHZ-001 |
| Change user lifecycle / assignments | Allowed | Not allowed | Admin-only; Disabled users cannot receive new assignments. | BR-GOV-003 |
| View Notification Center | Allowed | Allowed | Own/authorized notifications. | BR-NOT-001 |
| Mark own notifications read | Allowed | Allowed | Notification action only; does not modify Audit Trail. | BR-NOT-001, BR-NOT-002 |
| View Sync Status | Allowed | Allowed | Operational visibility within authorized context. | BR-SET-005 |
| Change sync engine strategy / force overwrite / disable sync | Not allowed | Not allowed | System-Controlled Rule; not exposed through Settings. | BR-SET-003, BR-SET-005 |
| Change own User Preferences | Allowed | Allowed | Representation preferences only; cannot alter inventory/history/business rules. | BR-SET-001 |
| Change Organization Configuration | Allowed | Not allowed | Admin-only and only for explicitly configurable organization settings. | BR-SET-002 |
| Change System-Controlled Rules | Not allowed | Not allowed | Not editable by organization users. | BR-SET-003 |
| Use Support | Allowed | Allowed | Within normal authenticated application access. | BR-SET-006 |
| Sign Out | Allowed | Allowed | Must not automatically destroy unsynchronized local business data. | BR-SET-007 |

## 6. UI Access Matrix

UI access indicates whether the destination may be reached and what capability is available there. It does not replace operation-level authorization.

| UI component | Admin | Standard | Permission note |
|---|---|---|---|
| AUT-01 Sign In | Unauthenticated entry | Unauthenticated entry | Authentication; Disabled user cannot establish a new valid session. |
| AUT-SH-01 Forgot Password | Unauthenticated entry | Unauthenticated entry | Password recovery. |
| AUT-02 Reset Password | Recovery flow | Recovery flow | Valid recovery context required. |
| DSH-01 Dashboard | Allowed | Allowed | Authorized Organization scope. |
| ORG-01 Select Organization | Allowed | Allowed | Only authorized memberships. |
| ORG-02 Create/Edit Organization | Admin | Not allowed | Organization administration. |
| LOC-01 Locations | Read/Manage | Read | List/Map read for both roles; coordinate mutation Admin-only. |
| LOC-02 Storage Units in Location | Read/Manage | Read | Mutations Admin-only. |
| STU-02 Storage Unit Details | Read/Manage | Read | Mutations Admin-only. |
| STS-02 Storage Space Details | Read/Manage | Read | Mutations Admin-only. |
| GEO-01 Geometry Profiles | Read/Manage | Read | Geometry Profile mutations Admin-only. |
| GEO-02 Geometry Profile Details | Read/Manage | Read | Family/formula remains platform-governed. |
| COM-01 Commodities | Read | Read | Platform-governed. |
| COM-02 Commodity Details | Read | Read | Read-only. |
| CLI-01 Clients | Read/Manage | Read | Mutations Admin-only. |
| CLI-02 Client Details | Read/Manage | Read | Mutations Admin-only. |
| CAM-01 Inventory Campaigns | Allowed | Allowed | Authorized scope. |
| CAM-02 Campaign Details | Allowed | Allowed | Authorized scope. |
| CAM-03 Create & Launch Campaign | Allowed | Not allowed | Admin-only. |
| STK-01 Stocktaking Overview | Allowed | Allowed | Mutation requires Campaign Team membership. |
| STK-02 Location Stocktaking | Allowed | Allowed | Mutation requires Campaign Team membership. |
| STK-03 Storage Unit Stocktaking | Allowed | Allowed | Mutation requires Campaign Team membership. |
| STK-05 Storage Space Stocktaking | Conditional mutation | Conditional mutation | Campaign Team membership required for mutations. |
| CLS-01 Campaign Review & Close | Review + Close | Review only | Close Admin-only and online. |
| CLS-02 Final Inventory Snapshot | Allowed | Allowed | Authorized scope. |
| RPT-01 Reports | Allowed | Allowed | Authorized scope. |
| RPT-02 Report Viewer | Allowed | Allowed | Authorized scope. |
| HIS-01 Inventory History | Allowed | Allowed | Authorized scope. |
| DOS-01 Digital Inventory Dossier | Allowed | Allowed | Authorized scope. |
| AUD-01 Audit Trail | Allowed | Not allowed | Admin-only. |
| USR-01 Users | Allowed | Not allowed | Admin-only administration. |
| USR-02 User Details | Allowed | Not allowed | Admin-only administration. |
| MOR-01 More | Allowed | Allowed | Destination visibility follows permission. |
| NOT-01 Notification Center | Allowed | Allowed | Own/authorized notifications. |
| SYN-01 Sync Status | Allowed | Allowed | No force overwrite/configuration controls. |
| SET-01 Settings | Allowed | Allowed | Organization Configuration section Admin-only. |
| SUP-01 Support | Allowed | Allowed | About is included here. |
| P1-SH-01 Add/Edit Location | Allowed | Not allowed | Admin-only. |
| P1-SH-02 Add/Edit Storage Unit | Allowed | Not allowed | Admin-only. |
| P1-SH-03 Add/Edit Storage Space | Allowed | Not allowed | Admin-only; range/bulk only Add. |
| GEO-SH-01 Add/Edit Geometry Profile | Allowed | Not allowed | Admin-only. |
| CLI-SH-01 Add/Edit Client | Allowed | Not allowed | Admin-only. |
| P0-SH-01 Report Issue | Conditional | Conditional | Campaign Team member in authorized Stocktaking context. |

## 7. State-Dependent Authorization

### 7.1 Active vs Completed Campaign

- Active Campaigns permit authorized operational work subject to Campaign Team membership and other guards.
- Completed Campaigns are historical. They cannot return to Active.
- Final Inventory Snapshot is immutable after Close.
- Completed official reports are generated from Final Inventory Snapshot data.

### 7.2 Validated Stocktaking

- Validation completes a Storage Space for campaign purposes.
- Stale offline changes cannot overwrite Validated data.
- Validation does not automatically resolve Open Issues.
- Evidence in the validated historical context becomes read-only according to the evidence contract.

### 7.3 Open vs Resolved Issue

- Reporting an Issue is available to authorized Campaign Team members in Stocktaking context.
- Resolution is explicit, authorized, and audited.
- Close-time follow-up acceptance is an Admin-only action and requires an explicit Open → Resolved transition with reason and audit.

## 8. Offline Authorization

Offline operation does not bypass authorization. A Campaign Offline Working Set is user/campaign scoped and contains the authorization context required for supported offline work.

A device is Offline Ready only after the complete authorized working set is downloaded and integrity-verified. Supported offline mutations may proceed only where the local working set contains sufficient valid authorization and business context. Synchronization revalidates applicable server-side authorization, entity version, and integrity constraints.

Inventory Close is never finalized offline.

## 9. Server Enforcement

The server is authoritative for protected remote operations. At minimum, it must revalidate as applicable:

- authenticated identity and Active user state;
- Organization membership and Organization boundary;
- role permission;
- Campaign access;
- Campaign Team membership for Stocktaking mutations;
- entity/workflow state;
- entity version / optimistic concurrency;
- immutable historical constraints;
- Close Guards and connectivity-dependent finalization.

An Android client must not gain authority merely by hiding or showing controls.

## 10. Permission Invariants

1. There are exactly two organization roles: Admin and Standard.
2. Admin privileges and Campaign Team membership are separate.
3. Stocktaking mutation requires Campaign Team membership for both roles.
4. Master Data mutation is Admin-only.
5. Campaign creation, scope management, and team assignment are Admin-only.
6. Campaign Scope cannot be modified after Save & Launch.
7. Inventory Close is Admin-only and online.
8. Audit Trail is Admin-only.
9. Standard may access non-audit reports within authorized scope.
10. Platform-governed Geometry Families/formulas and Commodity definitions are not editable by either organization role.
11. System-Controlled Rules are not editable by either organization role.
12. Disabled users cannot initiate new authentication, operations, or assignments.
13. Client-side permission checks never replace server-side authorization.

## 11. Required Authorization Test Coverage

`18_TEST_STRATEGY_ACCEPTANCE` must include positive and negative authorization coverage. At minimum, tests must demonstrate:

- Admin Master Data mutation succeeds in valid scope and Standard mutation is rejected;
- Campaign create/launch and scope/team administration reject Standard;
- Admin without Campaign Team membership cannot mutate Stocktaking solely because of Admin role;
- Standard assigned to Campaign Team can perform permitted Stocktaking mutations;
- non-team users cannot mutate Stocktaking;
- Standard cannot Close a Campaign or access Audit Trail;
- Admin Close fails when Close Guards fail or connectivity/server finalization is unavailable;
- cross-Organization access is rejected;
- Disabled users cannot start new authorized operations;
- server rejects a protected operation even if a client attempts to bypass UI restrictions;
- stale/conflicting synchronized writes do not bypass state or authorization rules.

## 12. Governance

This document specializes the authorization aspects of `01_PRODUCT_BASELINE`, `02_DECISION_REGISTER`, and `03_BUSINESS_RULES`. It does not introduce a third organization role, implicit superuser behavior, or role-based exceptions to Campaign Team membership.

Later UI, API, database, offline/sync, and test specifications must implement this matrix consistently.

---

**End of Document — 04_USER_ROLES_PERMISSIONS**
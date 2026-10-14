# SiloMath V6 Baseline Change Process

## Purpose

This directory defines the repository governance process for intentional
changes to the SiloMath V6 APPROVED / FROZEN product contract.

The process exists to prevent implementation work from silently changing
frozen product behavior.

A Baseline Change / Change Request is required when a proposed change would
intentionally alter a frozen rule, structure, state, workflow, screen,
architecture decision, module boundary, data contract, API contract,
offline/synchronization behavior, calculation rule, security rule,
retention rule, production rule, or other normative behavior defined by
the SiloMath V6 baseline.

## Authority

Documents 01–21 remain the authoritative SiloMath V6 baseline.

01_PRODUCT_BASELINE and 02_DECISION_REGISTER remain global authority.
The specialist baseline document remains authoritative for its domain.

A Change Request MUST NOT be used to:

- silently override Documents 01–21;
- reinterpret a frozen requirement without explicit approval;
- convert an implementation deviation into accepted product behavior;
- resolve a contradiction between baseline documents by assumption;
- bypass an applicable frozen acceptance or release gate;
- substitute an IADR for an actual product-contract change.

If baseline documents appear contradictory, implementation must stop for
the affected decision and the contradiction must be reported explicitly.
It must not be resolved by implementation preference.

## Relationship to IADR

Implementation ADRs and Baseline Change Requests serve different purposes.

An IADR records an implementation decision that is still open within the
frozen V6 contract.

A Baseline Change Request records a proposed intentional change to the
frozen V6 contract itself.

An IADR MUST NOT authorize a frozen baseline change.

A Change Request MUST NOT be used merely to document ordinary implementation
detail that is already permitted by the frozen baseline.

## Change Request identifier

Repository Change Requests use the existing SiloMath CR naming convention:

CR-<DOMAIN>-<SUBJECT>-NNN

Example:

CR-API-ORG-MEM-001

The machine-readable register defines the canonical validation pattern.

The identifier is stable and MUST NOT be reused for a different change.

## Governance statuses

The following statuses belong only to the repository Baseline Change
governance process:

- PROPOSED
- APPROVED
- REJECTED
- SUPERSEDED

Only APPROVED authorizes the requested frozen-contract change.

PROPOSED does not authorize implementation that contradicts the current
baseline.

REJECTED does not authorize any baseline deviation.

SUPERSEDED means the record is retained for history but another Change
Request is the controlling record.

These are governance statuses and do not create or modify application
business statuses or lifecycle states.

## Required content

Every Change Request must identify:

1. the CR identifier;
2. the title and owner;
3. the current governance status;
4. the affected frozen baseline document or documents;
5. affected Requirement / Decision IDs where such IDs exist;
6. the current frozen contract;
7. the exact requested delta;
8. the reason for the requested change;
9. alternatives considered;
10. impact on workflows, states, UI, architecture, modules, data, APIs,
    offline/synchronization, calculation, evidence, reporting, security,
    privacy, retention and production operations as applicable;
11. RVM impact;
12. affected-test impact;
13. migration / compatibility impact where applicable;
14. release-gate impact;
15. the approval or rejection decision.

A Change Request must be specific enough that an implementer cannot infer
additional product behavior that was never approved.

## Approval rule

A proposed frozen-contract change is not authorized until its Change Request
is explicitly APPROVED.

An APPROVED record must identify:

- the approval date;
- the approving authority;
- the affected baseline references;
- the exact approved delta;
- the RVM impact;
- the affected-test impact.

Approval of a Change Request is not permission to ignore the remainder of
the frozen baseline.

Only the explicitly approved delta may change.

All unaffected frozen requirements remain authoritative.

## Applying an approved change

When an APPROVED Change Request is implemented:

1. the implementation must remain limited to the approved delta;
2. affected authoritative baseline documentation must be updated through
   the controlled baseline-change work;
3. requirement registries and traceability artifacts must be regenerated
   or updated where affected;
4. the RVM must be updated for the changed contract;
5. affected tests must be updated, added or removed so that obsolete tests
   are not retained as false authority;
6. applicable validation and CI gates must pass;
7. release evidence must correspond to the changed contract and exact
   implementation artifact.

A changed product behavior cannot pass a Production release gate merely
because its implementation tests are green while the RVM or affected
contract tests remain stale.

## Baseline remains authoritative until controlled change

A PROPOSED Change Request does not modify the current baseline.

A REJECTED Change Request does not modify the current baseline.

An undocumented implementation deviation does not modify the current
baseline.

Until a frozen-contract change is explicitly approved and applied through
this controlled process, Documents 01–21 remain authoritative.

## Register

The repository machine-readable register is:

engineering/baseline-change-register.json

The register records Change Requests governed by this process.

The register is not a substitute for the detailed Change Request document.

Historical Change Requests that predate this repository process must not be
backfilled with invented approval metadata. They may be registered only when
their authoritative evidence is available.

## File naming

A Change Request document must use its CR identifier as the filename prefix.

Examples:

CR-API-ORG-MEM-001.md

or:

CR-API-ORG-MEM-001-membership-contract.md

Change Request documents live under:

engineering/baseline-change/

The process documentation and template are excluded from the CR record
filename rule.

## Validation

The repository validator is:

engineering/validate-baseline-change-process.ps1

It verifies the governance artifacts and machine-readable register.

The validator does not approve Change Requests.

Approval is a governance decision, not an automated inference.

## Frozen baseline safety rule

No repository implementation may use a Change Request, IADR, issue,
comment, commit message or undocumented convention to bypass the current
SiloMath V6 baseline.

Intentional frozen-contract change must be explicit, traceable and approved.
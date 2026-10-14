# Change Request: CR-<DOMAIN>-<SUBJECT>-NNN

## Governance metadata

- CR ID: CR-<DOMAIN>-<SUBJECT>-NNN
- Title:
- Status: PROPOSED
- Baseline: SILOMATH_V6
- Changes frozen V6 baseline: YES
- Owner:
- Requested by:
- Created date:

## Approval

- Decision:
- Decision date:
- Approved / rejected by:
- Superseded by:

Only an APPROVED Change Request authorizes the explicitly approved frozen
baseline delta.

## Affected frozen baseline

### Documents

-

### Requirement / Decision IDs

-

Where the affected baseline text has no formal Requirement / Decision ID,
identify the exact document and section instead.

## Current frozen contract

Describe the exact currently authoritative behavior.

Do not reinterpret or simplify the current frozen requirement.

## Requested delta

Describe the exact requested change to the frozen contract.

The delta must be bounded and explicit.

## Reason

Explain why the frozen contract is proposed to change.

## Alternatives considered

Describe alternatives that would preserve the current frozen contract and
why they were not selected.

## Impact analysis

### Business rules / lifecycle / workflows

-

### UI / navigation / design

-

### Architecture / module boundaries

-

### Data model / database

-

### API / backend contract

-

### Offline / synchronization

-

### Calculation engine

-

### Evidence / reporting

-

### Security / privacy

-

### Retention / production operations

-

## RVM impact

Identify the Requirement Verification Matrix entries that must be added,
changed or invalidated.

## Test impact

Identify affected tests, new tests required and obsolete tests that must no
longer remain authoritative.

## Migration and compatibility impact

Describe schema, data, API, client, migration or backwards-compatibility
effects where applicable.

## Release-gate impact

Identify affected acceptance, CI, staging or Production release gates.

## Approved delta

Complete only when Status is APPROVED.

State exactly what has been approved.

Do not expand the approved scope during implementation.

## Implementation verification

Complete when the approved change is applied.

- Baseline documentation updated:
- Requirement registry updated:
- RVM updated:
- Affected tests updated:
- Applicable validators PASS:
- CI PASS:
- Implementation commit / PR:
- Release evidence:

## Authority guard

This Change Request MUST NOT be treated as authority while its status is
PROPOSED or REJECTED.

Only the explicitly APPROVED delta may alter the frozen V6 contract.

All unaffected requirements in Documents 01–21 remain authoritative.
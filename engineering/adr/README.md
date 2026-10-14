# SiloMath V6 — Implementation Architecture Decision Records

## Purpose

This directory contains repository-level Implementation Architecture Decision
Records for technical decisions that are not already fixed by the SiloMath V6
APPROVED/FROZEN baseline.

The canonical product and architecture authority remains the official set of
Documents 01–21.

## Reserved frozen ADR namespace

Document 07_ARCHITECTURE already defines the frozen architecture decisions:

- ADR-001 through ADR-017.

Those identifiers belong to the V6 baseline and MUST NOT be reused,
renumbered, superseded, or reinterpreted by repository ADRs.

Repository implementation decisions use the separate namespace:

- IADR-0001
- IADR-0002
- ...

## When an IADR is required

Create an IADR when an implementation choice:

- has meaningful architectural consequences;
- affects module boundaries, infrastructure strategy, operational mechanics,
  security implementation, migration mechanics, or other cross-cutting
  technical behavior;
- is not already determined by the frozen V6 baseline;
- has multiple technically credible alternatives worth recording.

An IADR is not required for routine local refactoring, naming, formatting,
minor implementation mechanics, or choices already dictated by Documents
01–21.

## Authority rule

An IADR MUST NOT:

- change a frozen role, state, workflow, business rule, screen, navigation
  contract, API semantic, database invariant, calculation rule, offline rule,
  permission, retention rule, security rule, or other V6 requirement;
- override Documents 01–21;
- silently reinterpret a baseline ambiguity;
- authorize implementation that conflicts with a frozen requirement.

If the proposed decision would change frozen V6 behavior, stop the IADR
process and use the Baseline Change / Change Request process.

If an apparent ambiguity cannot be resolved from the authoritative
documentation, escalate it instead of inventing behavior.

## Lifecycle

Allowed IADR statuses are:

- PROPOSED
- ACCEPTED
- REJECTED
- SUPERSEDED

Only ACCEPTED IADRs are authoritative repository implementation decisions.

A SUPERSEDED record remains in history and identifies its replacement.

## Required contents

Each IADR must record:

1. IADR identifier.
2. Title.
3. Status.
4. Decision date when accepted/rejected.
5. Owner.
6. Context.
7. Exact frozen baseline references relevant to the decision.
8. Decision.
9. Alternatives considered.
10. Consequences.
11. Explicit baseline-impact check.
12. Supersession information where applicable.

## Baseline-impact check

Every record must explicitly state:

`Changes frozen V6 baseline: NO`

If the correct answer would be YES, the record cannot be accepted as an IADR.

The proposal must instead enter the Baseline Change process.

## Registration

Every IADR is registered in:

`engineering/adr-register.json`

The register is the machine-readable index. The Markdown file is the
human-readable decision record.

The validator:

`engineering/validate-adr-process.ps1`

checks identifier uniqueness, namespace separation, required metadata,
registered files, baseline references for accepted decisions, and the
prohibition against using an IADR to modify the frozen baseline.

## File naming

Use:

`engineering/adr/IADR-NNNN-short-kebab-title.md`

Example:

`engineering/adr/IADR-0001-room-encryption-provider.md`

Do not create a repository record named ADR-001, ADR-002, etc.; those
identifiers are reserved by the frozen architecture baseline.

## Supersession

Accepted implementation decisions are never silently edited into a different
decision.

When a materially different implementation decision replaces an accepted
IADR:

1. create a new IADR;
2. mark the prior record SUPERSEDED;
3. link both records in the register;
4. retain both files.

## Relationship to the RVM

An IADR may reference Requirement/Decision IDs from the frozen baseline.

It does not create new product requirements and does not replace the RVM.

Where an implementation decision enables verification of frozen behavior,
the executable test/evidence remains mapped through the RVM.
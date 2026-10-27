# Approved dependency correction — 2026-10-27

This v2 overlay supersedes only the MD-002 first-executable assessment from reconciliation2026-10-27-cb9bb9e-v1. It preserves every original node/edge and the original XLSX unchanged. Explicit approval: current Product Owner message. Classification: dependency correction / missing architectural prerequisite, NOT execution-order exception and NOT Baseline Change.

Effective MD-002 predecessors: MD-001 + UI-007 + UI-008. UI-007 and UI-008 each retain their own GATE-M1 dependency. User-approved sequence: UI-007 GREEN/atomic commit, then UI-008 GREEN/atomic commit, then MD-002. PREIMPLEMENTED is not predecessor satisfied. No task is closed by this plan update.

GATE-M1 is recorded DONE in the audited source with PR7 CI/local build evidence; merge5d2f75f is an ancestor of current HEAD (checked locally). No reopened prerequisite was inferred from unrelated later preimplementations.

validation.json records 321nodes,607retained edges,2approved added edges,609total and a verified topological order. The TSV is the exact deterministic formal order (tie-break original Order then ID), not authorization to ignore unsatisfied predecessors. The semantic AUTH-010/READY/M3 cycle and snapshotSchemaVersion UNRESOLVED are deliberately untouched.

Current execution: UI-007 GREEN with evidence in engineering/task-evidence/IMP-UI-007.md; local commit pending; UI-008 NOT STARTED; MD-002 BLOCKED by UI foundations until both GREEN. The previous MD-002 blocker evidence is retained as history; its ordering decision is now resolved by this approval.

# Approved MD-004 dependency / execution-order correction — 2026-10-28

Explicit Product Owner approval in current task. Classification: DEPENDENCY / EXECUTION-ORDER CORRECTION, not Product Baseline Change and not an unapproved exception.

The frozen STU02-05 child list needs a canonical authorized Storage Space local reader. Existing legacy numeric-parent reader cannot supply it for MD-003 canonical Units. Extract only this read-only prerequisite from future MD-005; do not move all of MD-005 before MD-004 or create a cycle.

Effective local order:
MD-001 + MD-003 -> extracted canonical Storage Space read prerequisite -> MD-004 -> remaining full MD-005 Repository/API -> MD-006.

Original MD-004 <- MD-001/MD-003, MD-005 <- MD-001/MD-004 and MD-006 <- MD-001/MD-005 edges are retained. The extracted prerequisite is an implementation slice, not a new frozen business concept/task identifier. Other graph nodes/edges and original workbook are unchanged. Source TSV SHA-256: 62a7cdc971fe636560fae09c6dbde1f74be2359651156cf2d6502ece666b878a.

Prerequisite GREEN: StorageSpaceReader domain read contract; canonical transactional Room relation/query; exact Organization/Location/Unit lineage; explicit Result failure vs verified empty; identity/metadata mapping; authorization-aware local observation; stable Code/UUID ordering. No legacy mutation/promotion, Storage Space write/API/outbox/SyncOperation, P1-SH-03, STS-02 renderer, Geometry/Capacity work or Room schema change. Evidence: engineering/task-evidence/IMP-MD-004-read-prerequisite.md.

MD-005 remains a separate task: this slice does not declare it Done or partially complete and does not satisfy all its predecessors. After the prerequisite atomic commit, resume MD-004 automatically. AUTH/READY and snapshotSchemaVersion boundaries remain untouched.

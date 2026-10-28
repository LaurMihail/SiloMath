# Country catalogue — approved clarification supersedes proposal

Current authority: APPROVED / FROZEN CR-COUNTRY-CATALOG-CONTRACT-001 / FD-74, by explicit Product Owner approval on 2026-10-27. See engineering/baseline-change/CR-COUNTRY-CATALOG-CONTRACT-001.md and .json. The proposal below is retained as historical pre-approval evidence. It is no longer the current decision status. No historical data audit or migration has occurred.

## Historical proposal before approval

# Country catalogue — clarification required, proposal only

Status: BASELINE_CLARIFICATION_REQUIRED / NOT APPROVED. No frozen contract change applied.

Sources: Doc06 ORG02-05 / P1SH01-05 require a supported-country selector; Doc09 says Country code/value or Country value. The concrete supported catalogue and persisted encoding are absent. Product Owner explicitly confirmed the gap and instructed LOC-01 to continue independently.

Proposed for explicit approval and subsequent decision registration only:
- Complete ISO3166-1 country list.
- Persist canonical uppercase alpha-2.
- Human-readable country-name UI labels.
- Local/cached selector options; no free-text country value.
- Reject unknown/non-catalog values for new writes.
- Audit already persisted non-conforming values separately before any migration.

This proposal has NOT been implemented, entered into the frozen Decision Register or used to reinterpret historical data. P1-SH-01 remains a separate blocked boundary; LOC-01 consumes existing authorized Location read data without changing Country values. Country is not part of search/filter/map-provider requests.

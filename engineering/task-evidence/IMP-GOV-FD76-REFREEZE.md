# IMP-GOV-FD76 — Supabase canonical backend baseline refreeze

- Change Request: `CR-ARCH-SUPABASE-BACKEND-001` — APPROVED
- Frozen Decision: `FD-76`
- Approval: Product Owner, explicit, 2026-10-29
- Scope: documentation/governance refreeze only; runtime migration is not claimed complete.
- Target: Supabase-only canonical core backend runtime; PostgreSQL enterprise authority; Auth/RLS/functions/Edge Functions/Storage; Realtime non-authoritative.
- Preserved: offline-first/local-first, Room/SQLCipher durability, `/api/v1` semantics, server authorization, idempotency/entityVersion, atomic critical mutations, historical integrity and release discipline.
- Superseded target implementation: separate Kotlin/Ktor + Render/external-compute backend as canonical core runtime.
- Validation evidence: `governance_evidence/SiloMath_V6_Refreeze_Validation_2026-10-29_SUPABASE.json`.
- Manifest: `governance_evidence/SiloMath_V6_FROZEN_MANIFEST_2026-10-29_SUPABASE.json`.
- CI: pending PR Baseline Gates.

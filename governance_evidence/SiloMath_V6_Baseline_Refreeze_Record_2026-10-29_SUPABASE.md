# SiloMath V6 Supabase canonical backend refreeze — 2026-10-29

Authority: Product Owner explicitly approved `CR-ARCH-SUPABASE-BACKEND-001`, frozen as `FD-76`.

Approved delta: Supabase is the sole canonical backend platform for core SiloMath application runtime. PostgreSQL remains the authoritative enterprise datastore. Server-authoritative operations use Supabase PostgreSQL functions/RPC and/or Supabase Edge Functions. Android direct access is allowed only on explicitly permitted API/security/RLS-protected paths. Critical mutations remain server-authoritative, atomic and idempotent. No separate Render/Ktor or equivalent external compute backend is canonical for core business operations.

Material baseline changes: Docs01/02 establish the authority and frozen decision. Docs07/11/12/15/17/18/20 receive controlled FD-76 bindings for architecture, API, sync, Evidence Storage, security, testing and production operations. Docs03–06, 08–10, 13–14, 16, 19 and 21 remain semantically unchanged; their existing product/domain/UI/data/calculation/reporting/retention/design contracts continue to apply.

Preserved without weakening: offline-first/local-first; Room/SQLCipher durable local operation; PostgreSQL authority; `/api/v1` semantics where specified; server-side authorization; idempotency/entityVersion; atomic Save & Launch/Campaign Code, Storage Space batch-create and Close; immutable history; Calculation Engine semantics; Development/Staging/Production release discipline.

Supabase Realtime is explicitly non-authoritative. Supabase service-role or equivalent privileged secrets remain server-only. External auxiliary services may support CI, distribution, observability or approved integrations but cannot become authoritative for core business state without another Baseline Change.

Manifest: `SiloMath_V6_FROZEN_MANIFEST_2026-10-29_SUPABASE.json`.
Validation: `SiloMath_V6_Refreeze_Validation_2026-10-29_SUPABASE.json`.

Requirement registry/RVM are regenerated from the updated baseline in this changeset where repository tooling is available. Runtime migration evidence remains separate: documentation approval/refreeze does not falsely claim that Ktor/Render removal or Supabase Edge Function migration is already implemented.

from pathlib import Path
import hashlib
import json
import re

root = Path('.')
revision = '2026-10-29 - CR-ARCH-SUPABASE-BACKEND-001 approved Supabase-only canonical core backend platform'

appendices = {
    'baseline/07_ARCHITECTURE.md': '''## Approved Supabase canonical backend contract — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

Supabase is the sole canonical backend platform for core SiloMath application runtime. The server-side topology is Supabase Auth for identity, PostgreSQL as the authoritative enterprise datastore, RLS as mandatory defense-in-depth for client-accessible data, PostgreSQL functions/RPC for data-centric transactional commands, Supabase Edge Functions for HTTP/API orchestration where required, and Supabase Storage for remote Evidence binaries. Supabase Realtime may accelerate refresh/notification but is not an authoritative state or synchronization mechanism.

The Android repository boundary remains local-first. Room/SQLCipher remains the durable client operational store and supported offline mutations still commit business state plus SyncOperation atomically before UI success. Direct Android-to-Supabase access is allowed only for paths explicitly permitted by Documents 11 and 17; no direct path may bypass Organization/role/Campaign Team/state guards, idempotency, entityVersion or critical transaction semantics.

The canonical `/api/v1` semantic contract remains authoritative where Document 11 specifies it. Edge Functions may host those HTTP contracts and may delegate atomic data work to PostgreSQL functions. Campaign Code allocation, Save & Launch, Storage Space batch-create, Close and other critical mutations remain server-authoritative, atomic and idempotent.

A separate Ktor/Render or equivalent external compute backend is superseded as a target core-runtime architecture. Provider selection is no longer an unconstrained implementation choice for the core backend: replacing Supabase as the canonical core backend, or making another service authoritative for core business state, requires an approved Baseline Change. Non-core CI, distribution, observability and integration services remain permitted when they are non-authoritative for core business state.''',
    'baseline/11_API_SPECIFICATION.md': '''## Approved Supabase platform binding — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

The frozen HTTP/API semantics in this document remain authoritative. Supabase is the sole canonical core backend runtime platform. `/api/v1` resource and command contracts are hosted through Supabase Edge Functions where an HTTP orchestration boundary is required and may delegate atomic data work to PostgreSQL functions/RPC. PostgreSQL remains authoritative for enterprise business data.

Android may use Supabase Auth, Storage, Realtime or PostgREST/RPC directly only where this API contract and Document 17 explicitly permit that path. Such direct access is never a blanket replacement for `/api/v1`, server-side authorization, idempotency, entityVersion, atomic transaction or standard error semantics. Critical mutations including Campaign Code allocation/Save & Launch, atomic Storage Space batch-create and Inventory Close must execute through approved server-authoritative PostgreSQL/Edge Function paths and cannot be implemented as unguarded client table writes.

Supabase Auth is the canonical identity/session platform. Supabase Storage is the canonical remote Evidence object store. Realtime is non-authoritative and may only signal clients to refresh/reconcile durable state; it does not acknowledge SyncOperations or resolve conflicts.''',
    'baseline/12_OFFLINE_SYNC_SPECIFICATION.md': '''## Approved Supabase synchronization binding — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

FD-76 changes backend platform topology, not the frozen offline-first/local-first contract. Room/SQLCipher remains the durable Android operational store; supported local business mutation and SyncOperation remain one atomic Room transaction; retries reuse the same idempotency identity; entityVersion/conflict rules and dependency ordering remain authoritative.

The synchronization target is the canonical Supabase backend: approved Edge Function `/api/v1` commands and/or PostgreSQL functions/RPC enforce authorization, idempotency, entityVersion and atomic business effects. Direct table access is permitted only for explicitly approved RLS-protected paths and must never bypass critical command semantics. Supabase Realtime is optional refresh acceleration only and is never a durable queue, acknowledgement mechanism, conflict resolver or source of truth.''',
    'baseline/15_EVIDENCE_STORAGE_SPECIFICATION.md': '''## Approved Supabase Storage binding — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

Supabase Storage is the canonical remote object-storage platform for SiloMath Evidence. Existing Evidence rules remain unchanged: app-private local durability while offline, JPEG normalization and size limits, SHA-256 integrity, metadata/reference persistence in relational storage, controlled authorization, immutable validated/final historical context and durable synchronization.

Storage object access must be private and authorized through approved Supabase security/API paths. Privileged service-role credentials are server-only and must never ship in the APK. Android direct Storage access is permitted only where Documents 11 and 17 explicitly define a safe authenticated path and must not allow cross-Organization or unauthorized object access.''',
    'baseline/17_SECURITY_AUDIT_PRIVACY.md': '''## Approved Supabase security boundary — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

Supabase Auth is the canonical identity platform. PostgreSQL/RLS, PostgreSQL functions and Edge Functions collectively implement the authoritative server-side security boundary. RLS is mandatory defense-in-depth for every client-accessible tenant table/view/storage path; critical protected mutations must additionally enforce the complete Organization membership, role, Campaign Team and entity/workflow-state guards in an approved server-authoritative function/Edge Function path.

Supabase service-role or equivalent privileged credentials are server-only and prohibited from APKs, client configuration, logs and diagnostic packages. Public/client keys designed for Android use do not confer authorization by themselves and remain constrained by authenticated identity, RLS and server-side business authorization. Supabase Storage is private for Evidence under the Evidence contract. Realtime subscriptions do not grant additional read/mutation authority.''',
    'baseline/18_TEST_STRATEGY_ACCEPTANCE.md': '''## FD-76 Supabase backend acceptance coverage — CR-ARCH-SUPABASE-BACKEND-001

Refreeze adds architecture acceptance coverage without changing product workflows. Required evidence includes: Supabase Auth/session behavior; RLS tenant/role isolation for every client-accessible path; Edge Function `/api/v1` contract/error/idempotency tests; PostgreSQL function/RPC transaction and authorization tests for critical mutations; Save & Launch/Campaign Code, atomic Storage Space batch-create and Close rollback/atomicity regression; offline SyncOperation retry/entityVersion/conflict regression against Supabase; Supabase Storage Evidence authorization/hash/integrity tests; Realtime non-authority tests; and Development/Staging/Production promotion smoke evidence.

Ktor/Render/JDBC-specific mechanics cease to be baseline acceptance requirements after migration closure. They may remain temporary transition tests until the superseded backend is removed, but cannot substitute for Supabase canonical-runtime acceptance evidence.''',
    'baseline/20_PRODUCTION_OPERATIONS.md': '''## Approved Supabase production topology — CR-ARCH-SUPABASE-BACKEND-001 / FD-76

Development, Staging and Production remain the only canonical environments. Each environment uses an isolated controlled Supabase backend context covering Auth, PostgreSQL schema/RLS/functions, Edge Functions, Storage, secrets/configuration and required telemetry. A separate Render/Ktor or equivalent external application-backend deployment is not part of the target core production topology.

The release sequence remains Build → Automated Tests → Migration Validation → Staging → Production → Smoke Test. Promotion must cover database migrations/functions, RLS/security policy, Edge Functions/API contracts, Storage configuration and Auth configuration as applicable. PostgreSQL backup/PITR, restore tests, RPO/RTO, integrity-first failure behavior, secret handling and incident evidence remain unchanged.

Core business-state availability/readiness is assessed at the Supabase platform boundaries actually used by SiloMath. No production gate may be declared green merely because an auxiliary external service is healthy; conversely, non-core CI/distribution/observability services may be used without becoming authoritative for core business state.'''
}

for rel, appendix in appendices.items():
    p = root / rel
    text = p.read_text(encoding='utf-8')
    text = re.sub(r'(?m)^(\*\*Baseline revision:\*\*|Baseline revision:) .*$', lambda m: f'{m.group(1)} {revision}', text, count=1)
    if rel == 'baseline/07_ARCHITECTURE.md':
        old = 'Library versions are implementation details managed centrally. Dependency/version/provider choices that preserve this architecture do not require Product/Baseline approval. An architectural-role change is recorded through IADR when it does not change frozen behavior; a role change that alters frozen behavior requires Baseline Change.'
        new = 'Library versions and non-backend dependency choices are implementation details managed centrally. FD-76 freezes Supabase as the canonical core backend platform; replacing that platform or making another service authoritative for core business state requires an approved Baseline Change. Other architectural choices that preserve the frozen contract may be recorded through IADR where material.'
        text = text.replace(old, new)
    heading = appendix.splitlines()[0]
    if heading not in text:
        text = text.rstrip() + '\n\n' + appendix.strip() + '\n'
    p.write_text(text, encoding='utf-8')

# Evidence Storage is provider-bound by FD-76, so include Doc15 in the CR scope.
cr_md = root / 'engineering/baseline-change/CR-ARCH-SUPABASE-BACKEND-001.md'
t = cr_md.read_text(encoding='utf-8')
if '- 15_EVIDENCE_STORAGE_SPECIFICATION' not in t:
    t = t.replace('- 12_OFFLINE_SYNC_SPECIFICATION\n- 17_SECURITY_AUDIT_PRIVACY', '- 12_OFFLINE_SYNC_SPECIFICATION\n- 15_EVIDENCE_STORAGE_SPECIFICATION\n- 17_SECURITY_AUDIT_PRIVACY')
cr_md.write_text(t, encoding='utf-8')

cr_json = root / 'engineering/baseline-change/CR-ARCH-SUPABASE-BACKEND-001.json'
data = json.loads(cr_json.read_text(encoding='utf-8'))
if '15_EVIDENCE_STORAGE_SPECIFICATION' not in data['baselineRefs']:
    data['baselineRefs'].insert(data['baselineRefs'].index('17_SECURITY_AUDIT_PRIVACY'), '15_EVIDENCE_STORAGE_SPECIFICATION')
cr_json.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

reg = root / 'engineering/baseline-change-register.json'
r = json.loads(reg.read_text(encoding='utf-8'))
for rec in r['records']:
    if rec['id'] == 'CR-ARCH-SUPABASE-BACKEND-001' and '15_EVIDENCE_STORAGE_SPECIFICATION' not in rec['baselineRefs']:
        rec['baselineRefs'].insert(rec['baselineRefs'].index('17_SECURITY_AUDIT_PRIVACY'), '15_EVIDENCE_STORAGE_SPECIFICATION')
reg.write_text(json.dumps(r, indent=2) + '\n', encoding='utf-8')

# Build manifest from exact branch bytes after all controlled edits.
baseline_files = sorted((root / 'baseline').glob('[0-2][0-9]_*.md'))
manifest = {
    'baselineId': 'SILOMATH_V6',
    'refreezeId': '2026-10-29_SUPABASE',
    'status': 'APPROVED_FROZEN',
    'changeRequest': 'CR-ARCH-SUPABASE-BACKEND-001',
    'decision': 'FD-76',
    'approvedBy': 'Product Owner (explicit user approval)',
    'approvedDate': '2026-10-29',
    'documents': []
}
for p in baseline_files:
    b = p.read_bytes()
    manifest['documents'].append({'path': p.as_posix(), 'sha256': hashlib.sha256(b).hexdigest(), 'bytes': len(b)})
(root / 'governance_evidence/SiloMath_V6_FROZEN_MANIFEST_2026-10-29_SUPABASE.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')

checks = {
    'baselineId': 'SILOMATH_V6',
    'refreezeId': '2026-10-29_SUPABASE',
    'changeRequest': 'CR-ARCH-SUPABASE-BACKEND-001',
    'decision': 'FD-76',
    'checks': {
        'twentyOneBaselineDocumentsPresent': len(baseline_files) == 21,
        'productBaselineContainsFD76Contract': 'Supabase is the sole canonical backend platform' in (root/'baseline/01_PRODUCT_BASELINE.md').read_text(encoding='utf-8'),
        'decisionRegisterContainsFD76': '| FD-76 | Supabase canonical backend platform |' in (root/'baseline/02_DECISION_REGISTER.md').read_text(encoding='utf-8'),
        'architectureContainsFD76Binding': '## Approved Supabase canonical backend contract — CR-ARCH-SUPABASE-BACKEND-001 / FD-76' in (root/'baseline/07_ARCHITECTURE.md').read_text(encoding='utf-8'),
        'apiContainsFD76Binding': '## Approved Supabase platform binding — CR-ARCH-SUPABASE-BACKEND-001 / FD-76' in (root/'baseline/11_API_SPECIFICATION.md').read_text(encoding='utf-8'),
        'syncContainsFD76Binding': '## Approved Supabase synchronization binding — CR-ARCH-SUPABASE-BACKEND-001 / FD-76' in (root/'baseline/12_OFFLINE_SYNC_SPECIFICATION.md').read_text(encoding='utf-8'),
        'evidenceContainsFD76Binding': '## Approved Supabase Storage binding — CR-ARCH-SUPABASE-BACKEND-001 / FD-76' in (root/'baseline/15_EVIDENCE_STORAGE_SPECIFICATION.md').read_text(encoding='utf-8'),
        'securityContainsFD76Binding': '## Approved Supabase security boundary — CR-ARCH-SUPABASE-BACKEND-001 / FD-76' in (root/'baseline/17_SECURITY_AUDIT_PRIVACY.md').read_text(encoding='utf-8'),
        'testContainsFD76Binding': '## FD-76 Supabase backend acceptance coverage — CR-ARCH-SUPABASE-BACKEND-001' in (root/'baseline/18_TEST_STRATEGY_ACCEPTANCE.md').read_text(encoding='utf-8'),
        'operationsContainsFD76Binding': '## Approved Supabase production topology — CR-ARCH-SUPABASE-BACKEND-001 / FD-76' in (root/'baseline/20_PRODUCTION_OPERATIONS.md').read_text(encoding='utf-8')
    }
}
checks['result'] = 'PASS' if all(checks['checks'].values()) else 'FAIL'
(root / 'governance_evidence/SiloMath_V6_Refreeze_Validation_2026-10-29_SUPABASE.json').write_text(json.dumps(checks, indent=2) + '\n', encoding='utf-8')

record = '''# SiloMath V6 Supabase canonical backend refreeze — 2026-10-29

Authority: Product Owner explicitly approved `CR-ARCH-SUPABASE-BACKEND-001`, frozen as `FD-76`.

Approved delta: Supabase is the sole canonical backend platform for core SiloMath application runtime. PostgreSQL remains the authoritative enterprise datastore. Server-authoritative operations use Supabase PostgreSQL functions/RPC and/or Supabase Edge Functions. Android direct access is allowed only on explicitly permitted API/security/RLS-protected paths. Critical mutations remain server-authoritative, atomic and idempotent. No separate Render/Ktor or equivalent external compute backend is canonical for core business operations.

Material baseline changes: Docs01/02 establish the authority and frozen decision. Docs07/11/12/15/17/18/20 receive controlled FD-76 bindings for architecture, API, sync, Evidence Storage, security, testing and production operations. Docs03–06, 08–10, 13–14, 16, 19 and 21 remain semantically unchanged; their existing product/domain/UI/data/calculation/reporting/retention/design contracts continue to apply.

Preserved without weakening: offline-first/local-first; Room/SQLCipher durable local operation; PostgreSQL authority; `/api/v1` semantics where specified; server-side authorization; idempotency/entityVersion; atomic Save & Launch/Campaign Code, Storage Space batch-create and Close; immutable history; Calculation Engine semantics; Development/Staging/Production release discipline.

Supabase Realtime is explicitly non-authoritative. Supabase service-role or equivalent privileged secrets remain server-only. External auxiliary services may support CI, distribution, observability or approved integrations but cannot become authoritative for core business state without another Baseline Change.

Manifest: `SiloMath_V6_FROZEN_MANIFEST_2026-10-29_SUPABASE.json`.
Validation: `SiloMath_V6_Refreeze_Validation_2026-10-29_SUPABASE.json`.

Requirement registry/RVM are regenerated from the updated baseline in this changeset where repository tooling is available. Runtime migration evidence remains separate: documentation approval/refreeze does not falsely claim that Ktor/Render removal or Supabase Edge Function migration is already implemented.
'''
(root / 'governance_evidence/SiloMath_V6_Baseline_Refreeze_Record_2026-10-29_SUPABASE.md').write_text(record, encoding='utf-8')

evidence = '''# IMP-GOV-FD76 — Supabase canonical backend baseline refreeze

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
'''
(root / 'engineering/task-evidence/IMP-GOV-FD76-REFREEZE.md').write_text(evidence, encoding='utf-8')

if checks['result'] != 'PASS':
    raise SystemExit(json.dumps(checks, indent=2))

print(json.dumps(checks, indent=2))

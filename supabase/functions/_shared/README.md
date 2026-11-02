# Supabase Edge Function shared foundation

This directory contains reusable, business-neutral server primitives for future
FD-76 Edge Functions. It does not expose an endpoint and does not authorize
direct table writes.

Security boundaries:

- authenticated identity comes only from a verified Supabase JWT subject;
- organization membership and role come from a server-side resolver;
- privileged clients and credentials remain server-only;
- RLS remains defense-in-depth; business authorization still runs server-side;
- idempotency is implemented through a PostgreSQL transaction/RPC implementing
  `TransactionalIdempotencyPort`, never through Edge Function memory;
- logs are allowlisted and exclude tokens, request payloads, secrets, and PII.

Environment names (values are never committed):

- `SUPABASE_URL`: server runtime project URL;
- `SUPABASE_SECRET_KEY` or legacy `SUPABASE_SERVICE_ROLE_KEY`: server-only
  privileged credential, when a future function explicitly requires one.

Development, Staging, and Production must use isolated project configuration.

The pre-V6 `invite-user` and `accept-invitation` Edge Functions are retired and
have been undeployed from Supabase production because no frozen V6 invitation
API contract or Android consumer exists. Database objects exclusive to the
invitation flow (`public.organization_invitations` and its 4 exclusive helper
functions) are retired via migration `20261031164618_fd76_retire_legacy_invitation_flow.sql`.
`public.is_organization_admin(uuid)` is retained temporarily because it is
still required by legacy RLS policies on `public.organizations` and
`public.organization_members`. Historical migrations remain retained for replay
and provenance.

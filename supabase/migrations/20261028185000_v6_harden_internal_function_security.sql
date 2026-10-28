-- SiloMath V6 security hardening.
-- Implements existing Doc17/Doc20 trust-boundary requirements; no product/business contract change.
-- Internal trigger/event-trigger functions must not be directly callable through exposed database roles.
-- Functions with mutable search_path are pinned to pg_catalog; all application-table references
-- inside these functions are already schema-qualified where applicable.

begin;

-- Pin search_path for internal trigger functions reported by the Supabase database linter.
alter function public.enforce_max_measurements_per_record() set search_path = pg_catalog;
alter function public.prevent_geometry_version_mutation() set search_path = pg_catalog;
alter function public.prevent_inventory_snapshot_items_mutation() set search_path = pg_catalog;
alter function public.prevent_inventory_snapshots_mutation() set search_path = pg_catalog;
alter function public.update_master_data_sync_metadata() set search_path = pg_catalog;

alter function silomath.enforce_entity_version_increment() set search_path = pg_catalog;
alter function silomath.guard_storage_space_identity_history() set search_path = pg_catalog;
alter function silomath.guard_storage_unit_identity_history() set search_path = pg_catalog;
alter function silomath.reject_audit_history_mutation() set search_path = pg_catalog;
alter function silomath.reject_immutable_snapshot_mutation() set search_path = pg_catalog;

-- Internal trigger functions: execution is through trigger wiring, never a client RPC surface.
revoke execute on function public.enforce_max_measurements_per_record() from public, anon, authenticated;
revoke execute on function public.prevent_geometry_version_mutation() from public, anon, authenticated;
revoke execute on function public.prevent_inventory_snapshot_items_mutation() from public, anon, authenticated;
revoke execute on function public.prevent_inventory_snapshots_mutation() from public, anon, authenticated;
revoke execute on function public.update_master_data_sync_metadata() from public, anon, authenticated;

revoke execute on function silomath.enforce_entity_version_increment() from public, anon, authenticated;
revoke execute on function silomath.guard_storage_space_identity_history() from public, anon, authenticated;
revoke execute on function silomath.guard_storage_unit_identity_history() from public, anon, authenticated;
revoke execute on function silomath.reject_audit_history_mutation() from public, anon, authenticated;
revoke execute on function silomath.reject_immutable_snapshot_mutation() from public, anon, authenticated;

-- SECURITY DEFINER functions below are trigger-only and already have a safe empty search_path.
revoke execute on function public.enforce_campaign_scope_organization_consistency() from public, anon, authenticated;
revoke execute on function public.enforce_launch_snapshot_child_mutation_protection() from public, anon, authenticated;
revoke execute on function public.enforce_third_party_client_org_consistency() from public, anon, authenticated;
revoke execute on function public.prevent_campaign_launch_snapshots_mutation() from public, anon, authenticated;
revoke execute on function public.prevent_campaign_org_mismatch_on_campaign_update() from public, anon, authenticated;
revoke execute on function public.prevent_campaign_org_mismatch_on_location_update() from public, anon, authenticated;
revoke execute on function public.prevent_campaign_org_mismatch_on_space_update() from public, anon, authenticated;
revoke execute on function public.prevent_campaign_org_mismatch_on_unit_update() from public, anon, authenticated;
revoke execute on function public.prevent_launch_snapshot_id_mutation() from public, anon, authenticated;
revoke execute on function public.prevent_third_party_client_org_mismatch_on_client_update() from public, anon, authenticated;
revoke execute on function public.prevent_third_party_org_mismatch_on_campaign_update() from public, anon, authenticated;

-- Production currently contains an rls_auto_enable event-trigger helper that is not
-- reconstructed by the repository migration chain. Harden it only when present;
-- do not make the repository depend on remote-only drift.
do $$
begin
    if to_regprocedure('public.rls_auto_enable()') is not null then
        execute 'revoke execute on function public.rls_auto_enable() from public, anon, authenticated';
    end if;
end;
$$;

-- Fail closed if the intended hardening did not take effect.
do $$
declare
    v_oid oid;
    v_name text;
    v_config text;
begin
    foreach v_name in array array[
        'public.enforce_max_measurements_per_record',
        'public.prevent_geometry_version_mutation',
        'public.prevent_inventory_snapshot_items_mutation',
        'public.prevent_inventory_snapshots_mutation',
        'public.update_master_data_sync_metadata',
        'silomath.enforce_entity_version_increment',
        'silomath.guard_storage_space_identity_history',
        'silomath.guard_storage_unit_identity_history',
        'silomath.reject_audit_history_mutation',
        'silomath.reject_immutable_snapshot_mutation'
    ]
    loop
        v_oid := to_regprocedure(v_name || '()');
        if v_oid is null then
            raise exception 'Required internal function missing during security hardening: %', v_name;
        end if;

        select coalesce(array_to_string(proconfig, ','), '')
          into v_config
          from pg_proc
         where oid = v_oid;

        if v_config not like '%search_path=pg_catalog%' then
            raise exception 'Internal function search_path hardening failed: %', v_name;
        end if;
    end loop;

    foreach v_name in array array[
        'public.enforce_max_measurements_per_record',
        'public.prevent_geometry_version_mutation',
        'public.prevent_inventory_snapshot_items_mutation',
        'public.prevent_inventory_snapshots_mutation',
        'public.update_master_data_sync_metadata',
        'silomath.enforce_entity_version_increment',
        'silomath.guard_storage_space_identity_history',
        'silomath.guard_storage_unit_identity_history',
        'silomath.reject_audit_history_mutation',
        'silomath.reject_immutable_snapshot_mutation',
        'public.enforce_campaign_scope_organization_consistency',
        'public.enforce_launch_snapshot_child_mutation_protection',
        'public.enforce_third_party_client_org_consistency',
        'public.prevent_campaign_launch_snapshots_mutation',
        'public.prevent_campaign_org_mismatch_on_campaign_update',
        'public.prevent_campaign_org_mismatch_on_location_update',
        'public.prevent_campaign_org_mismatch_on_space_update',
        'public.prevent_campaign_org_mismatch_on_unit_update',
        'public.prevent_launch_snapshot_id_mutation',
        'public.prevent_third_party_client_org_mismatch_on_client_update',
        'public.prevent_third_party_org_mismatch_on_campaign_update'
    ]
    loop
        v_oid := to_regprocedure(v_name || '()');
        if v_oid is null then
            raise exception 'Required internal function missing during privilege hardening: %', v_name;
        end if;

        if has_function_privilege('anon', v_oid, 'EXECUTE')
           or has_function_privilege('authenticated', v_oid, 'EXECUTE') then
            raise exception 'Exposed role still has EXECUTE on internal function: %', v_name;
        end if;
    end loop;

    v_oid := to_regprocedure('public.rls_auto_enable()');
    if v_oid is not null
       and (
           has_function_privilege('anon', v_oid, 'EXECUTE')
           or has_function_privilege('authenticated', v_oid, 'EXECUTE')
       ) then
        raise exception 'Exposed role still has EXECUTE on optional remote event-trigger helper: public.rls_auto_enable';
    end if;
end;
$$;

commit;

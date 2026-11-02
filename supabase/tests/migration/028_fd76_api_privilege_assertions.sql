\pset pager off
\pset format unaligned
\pset tuples_only on
do $$
begin
 if exists(select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
 where n.nspname='silomath' and p.proname in (
 'client_mutate_command','organization_patch_command','campaign_team_mutate_command',
 'campaign_close_command','campaign_snapshot_get','geometry_profile_mutate_command',
 'report_dataset_get','report_inventory_rows','inventory_history_compare','inventory_dossier_get',
 'audit_report_dataset_get','report_export_record','user_preferences_get','user_preferences_patch_command',
 'notification_mark_read_command','notifications_mark_all_read_command')
 and (has_function_privilege('anon',p.oid,'EXECUTE')
 or has_function_privilege('authenticated',p.oid,'EXECUTE')
 or has_function_privilege('service_role',p.oid,'EXECUTE'))) then
 raise exception 'Canonical API primitive grants allow non-postgres execution';
 end if;
 raise notice 'FD76 API PRIVILEGE ASSERTIONS PASS';
end $$;
select p.proname,pg_get_function_identity_arguments(p.oid) as signature,
md5(regexp_replace(pg_get_functiondef(p.oid),'\s+','','g')) as definition_hash
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='silomath' and (p.proname like '%command' or p.proname in
('offline_manifest_get','offline_package_get','sync_batch_dispatch','sync_server_status','sync_pull_page'))
order by p.proname,signature;

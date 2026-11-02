begin;
do $$
declare
 a uuid:='90000000-0000-0000-0000-000000000002';
 o uuid:='90000000-0000-0000-0000-000000000001';
 n uuid:=gen_random_uuid(); r jsonb; replay jsonb;
 k text:=gen_random_uuid()::text; v bigint; failed boolean;
begin
 select entity_version into v from silomath.organizations where organization_id=o;
 r:=silomath.organization_patch_command(a,o,'{"name":"Contract Organization","country":"RO"}',v,k,'org-patch','settings-test');
 replay:=silomath.organization_patch_command(a,o,'{"name":"Contract Organization","country":"RO"}',v,k,'org-patch','settings-test');
 if r<>replay or (r->'body'->>'entityVersion')::bigint<>v+1 then
 raise exception 'Organization replay/version failed'; end if;
 failed:=false;
 begin
 perform silomath.organization_patch_command(a,o,'{"country":"ZZ"}',v+1,
 gen_random_uuid()::text,'country-invalid','settings-test');
 exception when invalid_parameter_value then failed:=true; end;
 if not failed then raise exception 'Invalid Country accepted'; end if;
 failed:=false;
 begin
 perform silomath.organization_patch_command(a,o,'{"name":"Stale"}',v,
 gen_random_uuid()::text,'org-stale','settings-test');
 exception when serialization_failure then failed:=true; end;
 if not failed then raise exception 'Stale Organization accepted'; end if;
 insert into silomath.notifications(notification_id,organization_id,recipient_user_id,
 notification_type,title,body,created_at)
 values(n,o,a,'SYNC_ERROR','Test notification','Synthetic contract assertion',now());
 r:=silomath.notification_mark_read_command(a,n,k,'notification-read');
 replay:=silomath.notification_mark_read_command(a,n,k,'notification-read');
 if r<>replay or r->>'state'<>'READ' then raise exception 'Notification read replay failed'; end if;
 failed:=false;
 begin
 perform silomath.notification_mark_read_command(gen_random_uuid(),n,k,'notification-forged');
 exception when insufficient_privilege then failed:=true; end;
 if not failed then raise exception 'Forged notification recipient accepted'; end if;
 perform silomath.notifications_mark_all_read_command(a,k,'notification-all');
 insert into silomath.notifications(notification_id,organization_id,recipient_user_id,
 notification_type,title,body,created_at)
 values(gen_random_uuid(),o,a,'SYNC_ERROR','New after mark-all','Synthetic retry assertion',now());
 perform silomath.notifications_mark_all_read_command(a,k,'notification-all');
 if not exists(select 1 from silomath.notifications where recipient_user_id=a
 and title='New after mark-all' and read_at is null) then
 raise exception 'Mark-all replay affected a later notification'; end if;
 r:=silomath.user_preferences_patch_command(a,'{"theme":"DARK"}');
 replay:=silomath.user_preferences_patch_command(a,'{"theme":"DARK"}');
 if r<>replay or r->>'theme'<>'DARK' then raise exception 'Preference retry failed'; end if;
 failed:=false;
 begin
 perform silomath.user_preferences_patch_command(a,'{"disableSync":true}');
 exception when invalid_parameter_value then failed:=true; end;
 if not failed then raise exception 'System rule preference accepted'; end if;
 raise notice 'FD76 ORGANIZATION NOTIFICATIONS PREFERENCES ASSERTIONS PASS';
end $$;
rollback;

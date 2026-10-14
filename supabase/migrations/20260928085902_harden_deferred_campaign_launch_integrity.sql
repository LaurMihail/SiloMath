-- Re-query current row state from public.campaigns at deferred commit time instead of checking stale NEW image
create or replace function public.enforce_active_campaign_launch_snapshot()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_status text;
    v_launch_snapshot_id uuid;
begin
    select status, launch_snapshot_id
    into v_status, v_launch_snapshot_id
    from public.campaigns
    where id = new.id;

    if v_status is not null and v_status in ('Active', 'Completed') and v_launch_snapshot_id is null then
        raise exception 'Active or Completed campaign requires a non-null launch_snapshot_id';
    end if;

    return new;
end;
$$;

-- Revoke EXECUTE privileges on internal trigger function from PUBLIC, anon, and authenticated
revoke execute on function public.enforce_active_campaign_launch_snapshot() from public, anon, authenticated;

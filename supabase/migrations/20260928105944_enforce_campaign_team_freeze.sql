-- 1. Create function to enforce campaign_assignments freeze after launch
create or replace function public.enforce_campaign_team_freeze()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_old_snap_id uuid;
    v_new_snap_id uuid;
begin
    if TG_OP = 'INSERT' then
        select launch_snapshot_id into v_new_snap_id
        from public.campaigns
        where id = new.campaign_id;

        if v_new_snap_id is not null then
            raise exception 'Campaign team is permanently frozen after launch';
        end if;
    elsif TG_OP = 'DELETE' then
        select launch_snapshot_id into v_old_snap_id
        from public.campaigns
        where id = old.campaign_id;

        if v_old_snap_id is not null then
            raise exception 'Campaign team is permanently frozen after launch';
        end if;
    elsif TG_OP = 'UPDATE' then
        select launch_snapshot_id into v_old_snap_id
        from public.campaigns
        where id = old.campaign_id;

        if v_old_snap_id is not null then
            raise exception 'Campaign team is permanently frozen after launch';
        end if;

        if old.campaign_id is distinct from new.campaign_id then
            select launch_snapshot_id into v_new_snap_id
            from public.campaigns
            where id = new.campaign_id;

            if v_new_snap_id is not null then
                raise exception 'Campaign team is permanently frozen after launch';
            end if;
        end if;
    end if;

    if TG_OP = 'DELETE' then
        return old;
    else
        return new;
    end if;
end;
$$;

-- 2. Attach trigger to public.campaign_assignments
create trigger enforce_campaign_team_freeze_trigger
    before insert or update or delete on public.campaign_assignments
    for each row
    execute function public.enforce_campaign_team_freeze();

-- 3. Revoke EXECUTE privileges on trigger function from PUBLIC, anon, and authenticated
revoke execute on function public.enforce_campaign_team_freeze() from public, anon, authenticated;

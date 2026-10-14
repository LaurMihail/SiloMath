-- 1. Create function to enforce campaign_scope freeze after launch
create or replace function public.enforce_campaign_scope_freeze()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_launch_snapshot_id uuid;
    v_campaign_id uuid;
begin
    if TG_OP = 'INSERT' then
        v_campaign_id := new.campaign_id;
    else
        v_campaign_id := old.campaign_id;
    end if;

    select launch_snapshot_id into v_launch_snapshot_id
    from public.campaigns
    where id = v_campaign_id;

    if v_launch_snapshot_id is not null then
        raise exception 'Campaign scope is permanently frozen after launch';
    end if;

    if TG_OP = 'DELETE' then
        return old;
    else
        return new;
    end if;
end;
$$;

-- 2. Attach trigger to public.campaign_scope
create trigger enforce_campaign_scope_freeze_trigger
    before insert or update or delete on public.campaign_scope
    for each row
    execute function public.enforce_campaign_scope_freeze();

-- 3. Revoke EXECUTE privileges on trigger function from PUBLIC, anon, and authenticated
revoke execute on function public.enforce_campaign_scope_freeze() from public, anon, authenticated;

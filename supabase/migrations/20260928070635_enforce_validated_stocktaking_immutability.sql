-- 1. Helper function to check if a stocktaking_record is Validated
create or replace function public.is_stocktaking_record_validated(p_record_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_status text;
begin
    select status into v_status
    from public.stocktaking_records
    where id = p_record_id;

    return v_status = 'Validated';
end;
$$;

-- 2. Trigger function to enforce immutability on stocktaking_records once Validated
create or replace function public.enforce_validated_stocktaking_record_immutability()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if TG_OP = 'UPDATE' then
        if old.status = 'Validated' then
            raise exception 'Validated stocktaking record is immutable';
        end if;
    elsif TG_OP = 'DELETE' then
        if old.status = 'Validated' then
            raise exception 'Validated stocktaking record is immutable';
        end if;
    end if;

    return new;
end;
$$;

create trigger enforce_validated_stocktaking_record_immutability_trigger
    before update or delete on public.stocktaking_records
    for each row
    execute function public.enforce_validated_stocktaking_record_immutability();

-- 3. Trigger function to enforce immutability on child stocktaking tables (measurements, calculation_results, issues)
create or replace function public.enforce_validated_stocktaking_child_immutability()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    target_record_id uuid;
begin
    if TG_OP = 'INSERT' then
        target_record_id := new.stocktaking_record_id;
    else
        target_record_id := old.stocktaking_record_id;
    end if;

    if public.is_stocktaking_record_validated(target_record_id) then
        raise exception 'Stocktaking data cannot be modified because the record is Validated';
    end if;

    if TG_OP = 'DELETE' then
        return old;
    else
        return new;
    end if;
end;
$$;

create trigger enforce_validated_measurements_immutability_trigger
    before insert or update or delete on public.measurements
    for each row
    execute function public.enforce_validated_stocktaking_child_immutability();

create trigger enforce_validated_calculation_results_immutability_trigger
    before insert or update or delete on public.calculation_results
    for each row
    execute function public.enforce_validated_stocktaking_child_immutability();

create trigger enforce_validated_issues_immutability_trigger
    before insert or update or delete on public.issues
    for each row
    execute function public.enforce_validated_stocktaking_child_immutability();

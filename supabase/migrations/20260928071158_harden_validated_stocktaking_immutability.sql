-- 1. Precheck existing data for validated stocktaking consistency
do $$
declare
    invalid_count integer;
begin
    -- Check if any measurements, calculation_results, or issues reference a non-existent or corrupted stocktaking record
    select count(*) into invalid_count
    from public.measurements m
    left join public.stocktaking_records sr on sr.id = m.stocktaking_record_id
    where sr.id is null;

    if invalid_count > 0 then
        raise exception 'Migration failed: % measurements reference missing stocktaking_records', invalid_count;
    end if;
end;
$$;

-- 2. Update child immutability function to check BOTH OLD and NEW parent records on UPDATE
create or replace function public.enforce_validated_stocktaking_child_immutability()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if TG_OP = 'INSERT' then
        if public.is_stocktaking_record_validated(new.stocktaking_record_id) then
            raise exception 'Stocktaking data cannot be modified because the record is Validated';
        end if;
    elsif TG_OP = 'DELETE' then
        if public.is_stocktaking_record_validated(old.stocktaking_record_id) then
            raise exception 'Stocktaking data cannot be modified because the record is Validated';
        end if;
    elsif TG_OP = 'UPDATE' then
        if public.is_stocktaking_record_validated(old.stocktaking_record_id) or
           public.is_stocktaking_record_validated(new.stocktaking_record_id) then
            raise exception 'Stocktaking data cannot be modified because the record is Validated';
        end if;
    end if;

    if TG_OP = 'DELETE' then
        return old;
    else
        return new;
    end if;
end;
$$;

-- 3. Revoke EXECUTE privileges on internal helper/trigger functions from PUBLIC, anon, authenticated
revoke execute on function public.is_stocktaking_record_validated(uuid) from public, anon, authenticated;
revoke execute on function public.enforce_validated_stocktaking_record_immutability() from public, anon, authenticated;
revoke execute on function public.enforce_validated_stocktaking_child_immutability() from public, anon, authenticated;

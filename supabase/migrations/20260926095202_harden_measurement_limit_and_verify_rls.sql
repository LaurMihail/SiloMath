-- Make max-5 measurement enforcement concurrency-safe by locking the parent stocktaking_records row
create or replace function public.enforce_max_measurements_per_record()
returns trigger
language plpgsql
as $$
declare
    current_count integer;
begin
    -- Lock parent stocktaking record row to serialize concurrent inserts for the same record
    perform 1
    from public.stocktaking_records
    where id = new.stocktaking_record_id
    for update;

    select count(*) into current_count
    from public.measurements
    where stocktaking_record_id = new.stocktaking_record_id;

    if current_count >= 5 then
        raise exception 'Maximum 5 measurements per stocktaking record exceeded';
    end if;

    return new;
end;
$$;

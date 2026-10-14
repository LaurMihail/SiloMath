-- 1. Validate that no existing stocktaking_record violates campaign_scope membership
do $$
declare
    invalid_count integer;
begin
    select count(*) into invalid_count
    from public.stocktaking_records sr
    where not exists (
        select 1 from public.campaign_scope cs
        where cs.campaign_id = sr.campaign_id
          and cs.storage_space_id = sr.storage_space_id
    );

    if invalid_count > 0 then
        raise exception 'Migration failed: % stocktaking_records reference (campaign_id, storage_space_id) pairs not present in campaign_scope', invalid_count;
    end if;
end;
$$;

-- 2. Add composite foreign key constraint from stocktaking_records(campaign_id, storage_space_id) to campaign_scope(campaign_id, storage_space_id)
alter table public.stocktaking_records
add constraint stocktaking_records_campaign_storage_space_fkey
foreign key (campaign_id, storage_space_id)
references public.campaign_scope(campaign_id, storage_space_id)
on delete restrict;

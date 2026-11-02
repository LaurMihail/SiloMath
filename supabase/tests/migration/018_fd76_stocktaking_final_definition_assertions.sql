-- FD76-12G final migration-order assertions.

begin;

do $$
declare
    v_mark oid := to_regprocedure(
        'silomath.stocktaking_mark_empty_command(uuid,uuid,uuid,uuid,bigint,text,text,text)'
    );
    v_reset oid := to_regprocedure(
        'silomath.stocktaking_reset_validation_command(uuid,uuid,uuid,uuid,bigint,uuid,text,text)'
    );
begin
    if v_mark is null or v_reset is null then
        raise exception 'canonical idempotent Stocktaking commands are missing';
    end if;

    if pg_get_functiondef(v_mark) ~
           'entity_version[[:space:]]*=[[:space:]]*entity_version[[:space:]]*\+[[:space:]]*1'
       or pg_get_functiondef(v_reset) ~
           'entity_version[[:space:]]*=[[:space:]]*entity_version[[:space:]]*\+[[:space:]]*1' then
        raise exception 'Stocktaking commands still assign server-controlled entity_version';
    end if;

    if not pg_get_functiondef(v_mark) like '%returning * into v_revision%'
       or not pg_get_functiondef(v_mark) like '%returning * into v_record%'
       or not pg_get_functiondef(v_reset) like '%returning * into v_record%' then
        raise exception 'Stocktaking commands do not consume trigger-produced versions';
    end if;

    if has_function_privilege(
        'anon',
        'silomath.stocktaking_mark_empty_command(uuid,uuid,uuid,uuid,bigint,text,text,text)',
        'EXECUTE'
    ) or has_function_privilege(
        'authenticated',
        'silomath.stocktaking_mark_empty_command(uuid,uuid,uuid,uuid,bigint,text,text,text)',
        'EXECUTE'
    ) or has_function_privilege(
        'service_role',
        'silomath.stocktaking_mark_empty_command(uuid,uuid,uuid,uuid,bigint,text,text,text)',
        'EXECUTE'
    ) or not has_function_privilege(
        'postgres',
        'silomath.stocktaking_mark_empty_command(uuid,uuid,uuid,uuid,bigint,text,text,text)',
        'EXECUTE'
    ) then
        raise exception 'Stocktaking Mark Empty privileges are not postgres-only';
    end if;
end;
$$;

rollback;

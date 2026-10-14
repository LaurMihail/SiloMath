CREATE OR REPLACE FUNCTION public.finalize_existing_user_invitation(
    p_invitation_id UUID,
    p_organization_id UUID,
    p_user_id UUID,
    p_role TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_updated_count INT;
BEGIN
    IF p_role NOT IN ('Admin', 'Standard') THEN
        RAISE EXCEPTION 'Invalid role: %', p_role;
    END IF;

    INSERT INTO public.organization_members (
        organization_id,
        user_id,
        role
    ) VALUES (
        p_organization_id,
        p_user_id,
        p_role
    );

    UPDATE public.organization_invitations
    SET
        status = 'Accepted',
        accepted_at = now()
    WHERE id = p_invitation_id
      AND organization_id = p_organization_id
      AND status = 'Pending';

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;

    IF v_updated_count = 0 THEN
        RAISE EXCEPTION 'Pending invitation not found for ID % and organization %', p_invitation_id, p_organization_id;
    END IF;

    RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.finalize_existing_user_invitation(UUID, UUID, UUID, TEXT) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.finalize_existing_user_invitation(UUID, UUID, UUID, TEXT) FROM anon;
REVOKE EXECUTE ON FUNCTION public.finalize_existing_user_invitation(UUID, UUID, UUID, TEXT) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.finalize_existing_user_invitation(UUID, UUID, UUID, TEXT) TO service_role;

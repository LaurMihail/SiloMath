CREATE OR REPLACE FUNCTION public.accept_new_user_invitation(
    p_invitation_id UUID,
    p_user_id UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_org_id UUID;
    v_role TEXT;
BEGIN
    SELECT organization_id, role
    INTO v_org_id, v_role
    FROM public.organization_invitations
    WHERE id = p_invitation_id
      AND status = 'Pending'
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pending invitation not found for ID %', p_invitation_id;
    END IF;

    INSERT INTO public.organization_members (
        organization_id,
        user_id,
        role
    ) VALUES (
        v_org_id,
        p_user_id,
        v_role
    );

    UPDATE public.organization_invitations
    SET
        status = 'Accepted',
        accepted_at = now()
    WHERE id = p_invitation_id
      AND status = 'Pending';

    RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.accept_new_user_invitation(UUID, UUID) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.accept_new_user_invitation(UUID, UUID) FROM anon;
REVOKE EXECUTE ON FUNCTION public.accept_new_user_invitation(UUID, UUID) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.accept_new_user_invitation(UUID, UUID) TO service_role;

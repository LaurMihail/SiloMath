CREATE OR REPLACE FUNCTION public.create_existing_user_membership_invitation(
    p_organization_id UUID,
    p_user_id UUID,
    p_email TEXT,
    p_display_name TEXT,
    p_role TEXT,
    p_invited_by UUID
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_invitation_id UUID;
BEGIN
    IF p_role NOT IN ('Admin', 'Standard') THEN
        RAISE EXCEPTION 'Invalid role: %', p_role;
    END IF;

    INSERT INTO public.organization_invitations (
        organization_id,
        email,
        display_name,
        role,
        status,
        invited_by,
        accepted_at
    ) VALUES (
        p_organization_id,
        lower(trim(p_email)),
        trim(p_display_name),
        p_role,
        'Accepted',
        p_invited_by,
        now()
    )
    RETURNING id INTO v_invitation_id;

    INSERT INTO public.organization_members (
        organization_id,
        user_id,
        role
    ) VALUES (
        p_organization_id,
        p_user_id,
        p_role
    );

    RETURN v_invitation_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_existing_user_membership_invitation(UUID, UUID, TEXT, TEXT, TEXT, UUID) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.create_existing_user_membership_invitation(UUID, UUID, TEXT, TEXT, TEXT, UUID) FROM anon;
REVOKE EXECUTE ON FUNCTION public.create_existing_user_membership_invitation(UUID, UUID, TEXT, TEXT, TEXT, UUID) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.create_existing_user_membership_invitation(UUID, UUID, TEXT, TEXT, TEXT, UUID) TO service_role;

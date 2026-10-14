import "@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "@supabase/server";

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    if (req.method !== "POST") {
      return Response.json(
        { ok: false, error: "METHOD_NOT_ALLOWED" },
        { status: 405 }
      );
    }

    let body: any;
    try {
      body = await req.json();
    } catch {
      return Response.json(
        { ok: false, error: "INVALID_INPUT" },
        { status: 400 }
      );
    }

    const { organizationId, email, displayName, role } = body || {};

    if (
      typeof organizationId !== "string" || organizationId.trim().length === 0 ||
      typeof email !== "string" || email.trim().length === 0 ||
      typeof displayName !== "string" || displayName.trim().length === 0 ||
      (role !== "Admin" && role !== "Standard")
    ) {
      return Response.json(
        { ok: false, error: "INVALID_INPUT" },
        { status: 400 }
      );
    }

    const { data: isAdmin, error: rpcError } = await ctx.supabase.rpc("is_organization_admin", {
      organization: organizationId,
    });

    if (rpcError) {
      return Response.json(
        { ok: false, error: "AUTHORIZATION_CHECK_FAILED" },
        { status: 500 }
      );
    }

    if (isAdmin !== true) {
      return Response.json(
        { ok: false, error: "FORBIDDEN" },
        { status: 403 }
      );
    }

    const callerUserId = ctx.userClaims?.sub;
    if (!callerUserId) {
      return Response.json(
        { ok: false, error: "INVALID_AUTH_CONTEXT" },
        { status: 401 }
      );
    }

    const normalizedEmail = email.trim().toLowerCase();

    const { data: existingUserId, error: lookupError } = await ctx.supabaseAdmin.rpc(
      "get_auth_user_id_by_email",
      { p_email: normalizedEmail }
    );

    if (lookupError) {
      return Response.json(
        { ok: false, error: "USER_LOOKUP_FAILED" },
        { status: 500 }
      );
    }

    if (existingUserId) {
      const { data: invitationId, error: atomicError } = await ctx.supabaseAdmin.rpc(
        "create_existing_user_membership_invitation",
        {
          p_organization_id: organizationId,
          p_user_id: existingUserId,
          p_email: normalizedEmail,
          p_display_name: displayName.trim(),
          p_role: role,
          p_invited_by: callerUserId,
        }
      );

      if (atomicError) {
        if (atomicError.code === "23505") {
          return Response.json(
            { ok: false, error: "USER_ALREADY_MEMBER" },
            { status: 409 }
          );
        }
        return Response.json(
          { ok: false, error: "EXISTING_USER_FINALIZE_FAILED" },
          { status: 500 }
        );
      }

      if (typeof invitationId !== "string" || invitationId.trim().length === 0) {
        return Response.json(
          { ok: false, error: "EXISTING_USER_FINALIZE_FAILED" },
          { status: 500 }
        );
      }

      return Response.json({
        ok: true,
        invitationId: invitationId,
        existingUser: true,
        existingUserId: existingUserId,
        membershipCreated: true,
        invitationStatus: "Accepted",
        inviteSent: false,
      });
    }

    const { data: insertData, error: insertError } = await ctx.supabaseAdmin
      .from("organization_invitations")
      .insert({
        organization_id: organizationId,
        email: normalizedEmail,
        display_name: displayName.trim(),
        role: role,
        status: "Pending",
        invited_by: callerUserId,
      })
      .select("id")
      .single();

    if (insertError) {
      if (insertError.code === "23505") {
        return Response.json(
          { ok: false, error: "INVITATION_ALREADY_PENDING" },
          { status: 409 }
        );
      }
      return Response.json(
        { ok: false, error: "INVITATION_CREATE_FAILED" },
        { status: 500 }
      );
    }

    const { error: authInviteError } = await ctx.supabaseAdmin.auth.admin.inviteUserByEmail(
      normalizedEmail,
      {
        redirectTo: "silomath://auth/invite",
        data: {
          invitationId: insertData.id,
        },
      }
    );

    if (authInviteError) {
      return Response.json(
        { ok: false, error: "AUTH_INVITE_FAILED" },
        { status: 500 }
      );
    }

    return Response.json({
      ok: true,
      invitationId: insertData.id,
      existingUser: false,
      existingUserId: null,
      membershipCreated: false,
      invitationStatus: "Pending",
      inviteSent: true,
    });
  }),
};

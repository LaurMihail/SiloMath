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

    const { invitationId } = body || {};

    if (typeof invitationId !== "string" || invitationId.trim().length === 0) {
      return Response.json(
        { ok: false, error: "INVALID_INPUT" },
        { status: 400 }
      );
    }

    const callerUserId = ctx.userClaims?.sub;
    if (!callerUserId) {
      return Response.json(
        { ok: false, error: "INVALID_AUTH_CONTEXT" },
        { status: 401 }
      );
    }

    const { data: acceptData, error: acceptError } = await ctx.supabaseAdmin.rpc(
      "accept_new_user_invitation",
      {
        p_invitation_id: invitationId,
        p_user_id: callerUserId,
      }
    );

    if (acceptError) {
      if (acceptError.code === "23505") {
        return Response.json(
          { ok: false, error: "MEMBERSHIP_ALREADY_EXISTS" },
          { status: 409 }
        );
      }
      return Response.json(
        { ok: false, error: "INVITATION_ACCEPT_FAILED" },
        { status: 500 }
      );
    }

    if (acceptData !== true) {
      return Response.json(
        { ok: false, error: "INVITATION_ACCEPT_FAILED" },
        { status: 500 }
      );
    }

    return Response.json({
      ok: true,
      invitationId: invitationId,
      membershipCreated: true,
      invitationStatus: "Accepted",
    });
  }),
};

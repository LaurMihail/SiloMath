import { ApiFailure } from "./errors.ts";
import type {
  AuthenticatedSubject,
  OrganizationContext,
} from "./contracts.ts";

export interface VerifiedJwtClaims {
  sub?: unknown;
  [claim: string]: unknown;
}

export type BearerVerifier = (token: string) => Promise<VerifiedJwtClaims>;

export async function requireAuthenticatedSubject(
  request: Request,
  verifyBearer: BearerVerifier,
): Promise<AuthenticatedSubject> {
  const authorization = request.headers.get("Authorization")?.trim();
  const match = authorization?.match(/^Bearer\s+(.+)$/i);
  if (!match?.[1]?.trim()) {
    throw new ApiFailure(
      401,
      "AUTH_INVALID_CREDENTIALS",
      "Authentication is required.",
    );
  }

  let claims: VerifiedJwtClaims;
  try {
    claims = await verifyBearer(match[1].trim());
  } catch {
    throw new ApiFailure(
      401,
      "AUTH_INVALID_CREDENTIALS",
      "Authentication is invalid.",
    );
  }

  const userId = typeof claims.sub === "string" ? claims.sub.trim() : "";
  if (!userId) {
    throw new ApiFailure(
      401,
      "AUTH_INVALID_CREDENTIALS",
      "Authentication is invalid.",
    );
  }
  return { userId };
}

export type OrganizationMembershipResolver = (
  organizationId: string,
  authenticatedUserId: string,
) => Promise<{ role: string; active: boolean } | null>;

export async function requireOrganizationContext(
  organizationId: string,
  subject: AuthenticatedSubject,
  resolveMembership: OrganizationMembershipResolver,
): Promise<OrganizationContext> {
  const membership = await resolveMembership(organizationId, subject.userId);
  if (!membership?.active) {
    throw new ApiFailure(
      403,
      "TENANT_SCOPE_VIOLATION",
      "The requested organization is not available.",
    );
  }
  return {
    organizationId,
    userId: subject.userId,
    role: membership.role,
  };
}

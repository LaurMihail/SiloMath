export const CORRELATION_ID_HEADER = "X-Correlation-Id";
export const IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
export const ENTITY_VERSION_HEADER = "If-Match-Entity-Version";

export const API_ERROR_CODES = [
  "AUTH_INVALID_CREDENTIALS",
  "AUTH_SESSION_EXPIRED",
  "AUTH_USER_DISABLED",
  "AUTH_FORBIDDEN",
  "TENANT_SCOPE_VIOLATION",
  "RESOURCE_NOT_FOUND",
  "VALIDATION_FAILED",
  "ENTITY_VERSION_CONFLICT",
  "IDEMPOTENCY_KEY_REUSE_MISMATCH",
  "CAMPAIGN_SCOPE_FROZEN",
  "CAMPAIGN_COMPLETED_IMMUTABLE",
  "CAMPAIGN_TEAM_REQUIRED",
  "LAST_ACTIVE_ADMIN",
  "STOCKTAKING_NOT_TEAM_MEMBER",
  "STOCKTAKING_REVISION_STALE",
  "STOCKTAKING_VALIDATION_FAILED",
  "CALCULATION_STALE_FINGERPRINT",
  "CALCULATION_INVALID_INPUT",
  "EVIDENCE_PHOTO_LIMIT",
  "EVIDENCE_HASH_MISMATCH",
  "EVIDENCE_TOO_LARGE",
  "EVIDENCE_IMMUTABLE",
  "ISSUE_ALREADY_STATE",
  "ISSUE_COMPLETED_CAMPAIGN",
  "CLOSE_GUARDS_FAILED",
  "SYNC_CONFLICT",
  "SYNC_DEPENDENCY_PENDING",
  "RATE_LIMITED",
  "SERVICE_UNAVAILABLE",
  "INTERNAL_ERROR",
] as const;

export type ApiErrorCode = typeof API_ERROR_CODES[number];

export interface ApiFieldError {
  field: string;
  code: string;
  message: string;
}

export interface ApiErrorResponse {
  code: ApiErrorCode;
  message: string;
  fieldErrors: ApiFieldError[];
  correlationId: string;
  retryable: boolean;
  details?: Record<string, unknown>;
}

export interface AuthenticatedSubject {
  userId: string;
}

export interface OrganizationContext {
  organizationId: string;
  userId: string;
  role: string;
}

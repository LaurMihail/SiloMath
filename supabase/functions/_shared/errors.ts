import { CORRELATION_ID_HEADER } from "./contracts.ts";
import type {
  ApiErrorCode,
  ApiErrorResponse,
  ApiFieldError,
} from "./contracts.ts";

const SAFE_INTERNAL_MESSAGE = "The service could not complete the request.";

export class ApiFailure extends Error {
  readonly status: number;
  readonly code: ApiErrorCode;
  readonly retryable: boolean;
  readonly fieldErrors: ApiFieldError[];
  readonly details?: Record<string, unknown>;

  constructor(
    status: number,
    code: ApiErrorCode,
    message: string,
    retryable = false,
    fieldErrors: ApiFieldError[] = [],
    details?: Record<string, unknown>,
  ) {
    super(message);
    this.status = status;
    this.code = code;
    this.retryable = retryable;
    this.fieldErrors = fieldErrors;
    this.details = details;
  }
}

export function apiErrorResponse(
  failure: ApiFailure,
  correlationId: string,
): Response {
  const internal = failure.status >= 500;
  const body: ApiErrorResponse = {
    code: internal ? (
      failure.code === "SERVICE_UNAVAILABLE" ? failure.code : "INTERNAL_ERROR"
    ) : failure.code,
    message: internal ? SAFE_INTERNAL_MESSAGE : failure.message,
    fieldErrors: internal ? [] : failure.fieldErrors,
    correlationId,
    retryable: failure.retryable,
    ...(internal || failure.details === undefined
      ? {}
      : { details: failure.details }),
  };
  return jsonResponse(body, failure.status, correlationId);
}

export function jsonResponse(
  body: unknown,
  status: number,
  correlationId: string,
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      [CORRELATION_ID_HEADER]: correlationId,
    },
  });
}

export function validationFailure(
  field: string,
  message = "Invalid value.",
): ApiFailure {
  return new ApiFailure(
    422,
    "VALIDATION_FAILED",
    "Request validation failed.",
    false,
    [{ field, code: "INVALID", message }],
  );
}

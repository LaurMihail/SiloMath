import { CORRELATION_ID_HEADER } from "./contracts.ts";
import { validationFailure } from "./errors.ts";

export function correlationIdForRequest(
  request: Request,
  generate: () => string = () => crypto.randomUUID(),
): string {
  return request.headers.get(CORRELATION_ID_HEADER)?.trim() || generate();
}

export async function requireJsonObject(
  request: Request,
): Promise<Record<string, unknown>> {
  let value: unknown;
  try {
    value = await request.json();
  } catch {
    throw validationFailure("request", "A JSON object is required.");
  }
  if (value === null || Array.isArray(value) || typeof value !== "object") {
    throw validationFailure("request", "A JSON object is required.");
  }
  return value as Record<string, unknown>;
}

export function requirePositiveEntityVersion(request: Request): number {
  const raw = request.headers.get("If-Match-Entity-Version")?.trim();
  if (!raw || !/^\d+$/.test(raw)) {
    throw validationFailure("If-Match-Entity-Version");
  }
  const version = Number(raw);
  if (!Number.isSafeInteger(version) || version < 1) {
    throw validationFailure("If-Match-Entity-Version");
  }
  return version;
}

export function requireIdempotencyKey(request: Request): string {
  const key = request.headers.get("Idempotency-Key")?.trim();
  if (!key) {
    throw validationFailure("Idempotency-Key");
  }
  return key;
}

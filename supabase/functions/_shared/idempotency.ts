export interface IdempotencyRequest {
  namespace: string;
  key: string;
  fingerprint: string;
}

export type IdempotencyOutcome<T> =
  | { kind: "EXECUTED"; value: T }
  | { kind: "REPLAY"; value: T }
  | { kind: "CONFLICT" };

/**
 * Must be implemented by one authoritative PostgreSQL transaction/RPC.
 * The implementation must lock or insert the key, compare the fingerprint,
 * execute the business mutation, and persist the result before commit.
 */
export interface TransactionalIdempotencyPort {
  executeAtomically<T>(
    request: IdempotencyRequest,
    mutation: () => Promise<T>,
  ): Promise<IdempotencyOutcome<T>>;
}

export async function executeIdempotently<T>(
  port: TransactionalIdempotencyPort,
  namespace: string,
  key: string,
  canonicalPayload: unknown,
  mutation: () => Promise<T>,
): Promise<IdempotencyOutcome<T>> {
  return port.executeAtomically(
    {
      namespace,
      key,
      fingerprint: await canonicalRequestFingerprint(canonicalPayload),
    },
    mutation,
  );
}

export async function canonicalRequestFingerprint(
  payload: unknown,
): Promise<string> {
  const bytes = new TextEncoder().encode(canonicalJson(payload));
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("");
}

export function canonicalJson(value: unknown): string {
  if (value === null || typeof value === "boolean" ||
    typeof value === "string") {
    return JSON.stringify(value);
  }
  if (typeof value === "number") {
    if (!Number.isFinite(value)) throw new TypeError("Non-finite JSON number.");
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return `[${value.map(canonicalJson).join(",")}]`;
  }
  if (typeof value === "object") {
    const record = value as Record<string, unknown>;
    return `{${
      Object.keys(record).sort().map((key) =>
        `${JSON.stringify(key)}:${canonicalJson(record[key])}`
      ).join(",")
    }}`;
  }
  throw new TypeError("Value is not canonical JSON.");
}

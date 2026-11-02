import { createClient } from "@supabase/supabase-js"
import postgres from "postgres"
import { CalculationFailure, calculateGeometryCapacity, verifyAuthoritativeCalculation, type FrozenCalculationInput } from "./authoritative-calculation.ts"

type JsonObject = Record<string, unknown>
type FieldError = { field: string; code: string; message: string }

const db = postgres(Deno.env.get("SUPABASE_DB_URL")!, {
  prepare: false,
  max: 1,
})

const UUID =
  "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"

class ApiProblem extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: FieldError[] = [],
    readonly retryable = false,
    readonly details?: JsonObject,
  ) {
    super(message)
  }
}

function cid(req: Request): string {
  return req.headers.get("X-Correlation-Id")?.trim() || crypto.randomUUID()
}

function json(correlationId: string, status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "X-Correlation-Id": correlationId,
    },
  })
}

function fail(
  correlationId: string,
  status: number,
  code: string,
  message: string,
  fieldErrors: FieldError[] = [],
  retryable = false,
  details?: JsonObject,
): Response {
  return json(correlationId, status, {
    code,
    message,
    fieldErrors,
    correlationId,
    retryable,
    ...(details ? { details } : {}),
  })
}

function header(req: Request, name: string): string | null {
  const value = req.headers.get(name)?.trim()
  return value ? value : null
}

function pathOf(req: Request): string {
  const url = new URL(req.url)
  let path = url.pathname
  const marker = "/api-v1"
  const i = path.indexOf(marker)

  if (i >= 0) path = path.slice(i + marker.length)
  if (!path.startsWith("/")) path = `/${path}`

  return path === "/" ? "/" : path.replace(/\/+$/, "")
}

function stableJson(value: unknown): string {
  if (value === null || typeof value !== "object") return JSON.stringify(value)
  if (Array.isArray(value)) return `[${value.map(stableJson).join(",")}]`

  const objectValue = value as JsonObject
  return `{${Object.keys(objectValue).sort().map(
    (key) => `${JSON.stringify(key)}:${stableJson(objectValue[key])}`,
  ).join(",")}}`
}

async function fingerprint(value: unknown): Promise<string> {
  const bytes = new TextEncoder().encode(stableJson(value))
  const digest = await crypto.subtle.digest("SHA-256", bytes)

  return Array.from(new Uint8Array(digest))
    .map((x) => x.toString(16).padStart(2, "0"))
    .join("")
}

async function authenticatedUser(req: Request) {
  const auth = header(req, "Authorization")
  if (!auth?.startsWith("Bearer ")) return null

  const token = auth.slice(7).trim()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    },
  )

  const { data, error } = await supabase.auth.getUser(token)
  if (error || !data.user) return null

  return data.user
}

type EvidenceObjectStore = {
  upload(path: string, bytes: Uint8Array): Promise<void>
  download(path: string): Promise<Uint8Array>
}

function evidenceObjectStore(configuration: (name: string) => string | undefined): EvidenceObjectStore {
  const client = () => {
    const url = configuration("SUPABASE_URL")
    const serviceRole = configuration("SUPABASE_SERVICE_ROLE_KEY")
    if (!url || !serviceRole) {
      throw new ApiProblem(
        503,
        "SERVICE_UNAVAILABLE",
        "Evidence object storage is not configured.",
        [],
        true,
      )
    }
    return createClient(url, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    })
  }

  return {
    async upload(path, bytes) {
      const { error } = await client().storage
        .from("silomath-evidence")
        .upload(path, bytes, {
          contentType: "image/jpeg",
          upsert: true,
          cacheControl: "31536000",
        })
      if (error) throw new Error("evidence object upload failed")
    },
    async download(path) {
      const { data, error } = await client().storage
        .from("silomath-evidence")
        .download(path)
      if (error || !data) throw new Error("evidence object download failed")
      return new Uint8Array(await data.arrayBuffer())
    },
  }
}

async function sha256Hex(bytes: Uint8Array): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", Uint8Array.from(bytes).buffer)
  return Array.from(new Uint8Array(digest))
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("")
}

function binaryMutationKey(req: Request): string {
  const value = header(req, "Idempotency-Key")
  if (!value || !new RegExp(`^${UUID}$`).test(value)) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "Idempotency-Key must be a UUID.",
      [{ field: "Idempotency-Key", code: "REQUIRED", message: "Valid UUID required." }],
    )
  }
  return value
}

function hasOwn(body: JsonObject, key: string): boolean {
  return Object.prototype.hasOwnProperty.call(body, key)
}

async function bodyJson(req: Request): Promise<JsonObject> {
  try {
    const body = await req.json()
    if (!body || typeof body !== "object" || Array.isArray(body)) {
      throw new Error("body")
    }
    return body as JsonObject
  } catch {
    throw new ApiProblem(
      400,
      "VALIDATION_FAILED",
      "Request body must contain a valid JSON object.",
    )
  }
}

function mutationKey(req: Request, body: JsonObject): string {
  const fromHeader = header(req, "Idempotency-Key")
  const fromBody = typeof body.idempotencyKey === "string"
    ? body.idempotencyKey.trim()
    : null

  if (fromHeader && fromBody && fromHeader !== fromBody) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "Idempotency-Key and idempotencyKey must identify the same logical mutation.",
      [{ field: "idempotencyKey", code: "MISMATCH", message: "Mutation keys differ." }],
    )
  }

  const value = fromHeader || fromBody
  if (!value) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "Idempotency key is required.",
      [{ field: "idempotencyKey", code: "REQUIRED", message: "Required" }],
    )
  }

  if (!new RegExp(`^${UUID}$`).test(value)) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "Idempotency key must be a UUID.",
      [{ field: "idempotencyKey", code: "INVALID", message: "Valid UUID required." }],
    )
  }

  return value
}

function requireClientOperationId(body: JsonObject) {
  if (typeof body.clientOperationId !== "string" ||
      !new RegExp(`^${UUID}$`).test(body.clientOperationId)) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "clientOperationId is required.",
      [{ field: "clientOperationId", code: "REQUIRED", message: "Valid UUID required." }],
    )
  }
}

function baseEntityVersion(body: JsonObject, req?: Request): number {
  const value = Number(body.baseEntityVersion)
  if (!Number.isSafeInteger(value) || value < 1) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "baseEntityVersion must be a positive integer.",
      [{ field: "baseEntityVersion", code: "INVALID", message: "Positive integer required." }],
    )
  }

  const fromHeader = req ? header(req, "If-Match-Entity-Version") : null
  if (fromHeader !== null && Number(fromHeader) !== value) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "If-Match-Entity-Version and baseEntityVersion must match.",
      [{ field: "baseEntityVersion", code: "MISMATCH", message: "Entity versions differ." }],
    )
  }

  return value
}

function enforceOrganizationContext(req: Request, organizationId: string) {
  const context = header(req, "X-Organization-Id")
  if (context && context.toLowerCase() !== organizationId.toLowerCase()) {
    throw new ApiProblem(
      403,
      "TENANT_SCOPE_VIOLATION",
      "Organization context does not match the requested resource.",
    )
  }
}

function locationDto(row: postgres.Row) {
  return {
    locationId: row.location_id,
    organizationId: row.organization_id,
    name: row.name,
    code: row.code,
    address: row.address,
    city: row.city,
    country: row.country,
    notes: row.notes,
    latitude: row.latitude_deg,
    longitude: row.longitude_deg,
    lifecycleStatus: row.lifecycle_status,
    createdAt: row.created_at,
    createdBy: row.created_by,
    updatedAt: row.updated_at,
    updatedBy: row.updated_by,
    entityVersion: Number(row.entity_version),
  }
}

function storageUnitDto(row: postgres.Row) {
  return {
    storageUnitId: row.storage_unit_id,
    organizationId: row.organization_id,
    locationId: row.location_id,
    name: row.name,
    code: row.code,
    description: row.description,
    lifecycleStatus: row.lifecycle_status,
    createdAt: row.created_at,
    createdBy: row.created_by,
    updatedAt: row.updated_at,
    updatedBy: row.updated_by,
    entityVersion: Number(row.entity_version),
  }
}

async function storageSpaceDto(row: postgres.Row) {
  const geometry = await db`
    select
      gp.geometry_profile_id,
      gp.name,
      gp.code,
      gp.current_version_id,
      gpv.version_number,
      gpv.max_capacity_m3,
      su.location_id
    from silomath.geometry_profiles gp
    join silomath.geometry_profile_versions gpv
      on gpv.geometry_profile_version_id = gp.current_version_id
    join silomath.storage_units su
      on su.storage_unit_id = ${row.storage_unit_id}::uuid
     and su.organization_id = gp.organization_id
    where gp.geometry_profile_id = ${row.geometry_profile_id}::uuid
      and gp.organization_id = ${row.organization_id}::uuid
  `

  const g = geometry[0]

  return {
    id: row.storage_space_id,
    organizationId: row.organization_id,
    storageUnitId: row.storage_unit_id,
    locationId: g?.location_id ?? null,
    code: row.code,
    description: row.description,
    geometryProfileId: row.geometry_profile_id,
    geometryProfileVersionId: g?.current_version_id ?? null,
    geometryProfileVersionNumber: g?.version_number ?? null,
    maxCapacityM3: g?.max_capacity_m3 ?? null,
    lifecycleStatus: row.lifecycle_status,
    createdAt: row.created_at,
    createdBy: row.created_by,
    updatedAt: row.updated_at,
    updatedBy: row.updated_by,
    entityVersion: Number(row.entity_version),
  }
}

function organizationDto(row: postgres.Row) {
  return {
    id: row.organization_id,
    name: row.name,
    code: row.code,
    address: row.address,
    city: row.city,
    country: row.country,
    lifecycleStatus: row.lifecycle_status,
    createdAt: row.created_at,
    createdBy: row.created_by,
    updatedAt: row.updated_at,
    updatedBy: row.updated_by,
    entityVersion: Number(row.entity_version),
  }
}

function userDto(row: postgres.Row) {
  return {
    userId: row.user_id,
    fullName: row.full_name,
    email: row.email,
    lifecycleStatus: row.user_lifecycle_status ?? row.lifecycle_status,
    createdAt: row.user_created_at ?? row.created_at,
    createdBy: row.user_created_by ?? row.created_by,
    updatedAt: row.user_updated_at ?? row.updated_at,
    updatedBy: row.user_updated_by ?? row.updated_by,
    entityVersion: Number(row.user_entity_version ?? row.entity_version),
  }
}

function membershipDto(row: postgres.Row) {
  return {
    membershipId: row.membership_id,
    organizationId: row.organization_id,
    userId: row.user_id,
    role: row.role,
    lifecycleStatus: row.membership_lifecycle_status ?? row.lifecycle_status,
    disabledAt: row.disabled_at,
    disabledBy: row.disabled_by,
    createdAt: row.membership_created_at ?? row.created_at,
    createdBy: row.membership_created_by ?? row.created_by,
    updatedAt: row.membership_updated_at ?? row.updated_at,
    updatedBy: row.membership_updated_by ?? row.updated_by,
    entityVersion: Number(row.membership_entity_version ?? row.entity_version),
  }
}

function userMembershipDto(row: postgres.Row) {
  return { user: userDto(row), membership: membershipDto(row) }
}

function campaignDto(row: postgres.Row) {
  return {
    id: row.campaign_id,
    organizationId: row.organization_id,
    campaignCode: row.code,
    name: row.name,
    inventoryType: row.inventory_type,
    startDate: row.start_date,
    plannedEndDate: row.planned_end_date,
    status: row.status,
    launchedAt: row.launched_at,
    launchedBy: row.launched_by,
    completedAt: row.completed_at ?? null,
    completedBy: row.completed_by ?? null,
    campaignSnapshotId: row.campaign_snapshot_id,
    createdAt: row.created_at,
    createdBy: row.created_by,
    updatedAt: row.updated_at,
    updatedBy: row.updated_by,
    entityVersion: Number(row.entity_version),
  }
}

function campaignSummaryDto(row: postgres.Row) {
  return {
    id: row.campaign_id,
    organizationId: row.organization_id,
    campaignCode: row.code,
    name: row.name,
    inventoryType: row.inventory_type,
    startDate: row.start_date,
    plannedEndDate: row.planned_end_date,
    status: row.status,
    entityVersion: Number(row.entity_version),
  }
}

async function requireOrganizationAccess(
  userId: string,
  organizationId: string,
  adminOnly = false,
) {
  const rows = await db`
    select m.role
    from silomath.users u
    join silomath.organization_memberships m
      on m.user_id = u.user_id
    join silomath.organizations o
      on o.organization_id = m.organization_id
    where u.user_id = ${userId}::uuid
      and u.lifecycle_status = 'ACTIVE'
      and m.organization_id = ${organizationId}::uuid
      and m.lifecycle_status = 'ACTIVE'
      and o.lifecycle_status = 'ACTIVE'
  `

  if (!rows[0]) {
    throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The requested organization resource was not found.")
  }

  if (adminOnly && rows[0].role !== "ADMIN") {
    throw new ApiProblem(403, "AUTH_FORBIDDEN", "Administrator membership is required.")
  }

  return rows[0]
}

function encodeCursor(code: string, id: string): string {
  const raw = JSON.stringify({ code, id })
  const bytes = new TextEncoder().encode(raw)
  let binary = ""
  for (const byte of bytes) binary += String.fromCharCode(byte)

  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "")
}

function decodeCursor(value: string | null): { code: string | null; id: string | null } {
  if (!value) return { code: null, id: null }

  try {
    let base64 = value.replace(/-/g, "+").replace(/_/g, "/")
    while (base64.length % 4) base64 += "="

    const binary = atob(base64)
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
    const parsed = JSON.parse(new TextDecoder().decode(bytes))

    if (
      typeof parsed.code !== "string" ||
      typeof parsed.id !== "string" ||
      !new RegExp(`^${UUID}$`).test(parsed.id)
    ) throw new Error("cursor")

    return { code: parsed.code, id: parsed.id }
  } catch {
    throw new ApiProblem(400, "VALIDATION_FAILED", "cursor is malformed.")
  }
}

function pageQuery(req: Request) {
  const url = new URL(req.url)
  const rawLimit = url.searchParams.get("limit")
  const limit = rawLimit === null ? 50 : Number(rawLimit)

  if (!Number.isInteger(limit) || limit < 1 || limit > 200) {
    throw new ApiProblem(
      422,
      "VALIDATION_FAILED",
      "limit must be an integer between 1 and 200.",
      [{ field: "limit", code: "INVALID", message: "Expected 1..200." }],
    )
  }

  const search = url.searchParams.get("search")?.trim() || null
  const rawStatus = url.searchParams.get("status")?.trim() || null
  const status = rawStatus ? rawStatus.toUpperCase() : null

  return {
    limit,
    search,
    status,
    cursor: decodeCursor(url.searchParams.get("cursor")),
  }
}

async function currentUserResponse(userId: string) {
  const users = await db`
    select
      u.user_id,
      u.full_name,
      u.email
    from silomath.users u
    where u.user_id = ${userId}::uuid
      and u.lifecycle_status = 'ACTIVE'
  `

  const canonicalUser = users[0]
  if (!canonicalUser) {
    throw new ApiProblem(
      403,
      "AUTH_FORBIDDEN",
      "The authenticated user is not active.",
    )
  }

  const memberships = await db`
    select
      om.membership_id,
      om.organization_id,
      o.name as organization_name,
      o.code as organization_code,
      om.role
    from silomath.organization_memberships om
    join silomath.organizations o
      on o.organization_id = om.organization_id
     and o.lifecycle_status = 'ACTIVE'
    where om.user_id = ${userId}::uuid
      and om.lifecycle_status = 'ACTIVE'
    order by lower(o.name), o.code, om.membership_id
  `

  return {
    user: {
      userId: canonicalUser.user_id,
      fullName: canonicalUser.full_name,
      email: canonicalUser.email,
    },
    memberships: memberships.map((membership) => ({
      membershipId: membership.membership_id,
      organizationId: membership.organization_id,
      organizationName: membership.organization_name,
      organizationCode: membership.organization_code,
      role: membership.role,
    })),
  }
}

async function requireCampaignReadAccess(
  userId: string,
  organizationId: string,
  campaignId: string,
  database: typeof db = db,
) {
  const rows = await database`
    select exists (
      select 1
      from silomath.users u
      join silomath.organization_memberships om
        on om.user_id = u.user_id
       and om.organization_id = ${organizationId}::uuid
      join silomath.organizations o
        on o.organization_id = om.organization_id
      join silomath.inventory_campaigns c
        on c.campaign_id = ${campaignId}::uuid
       and c.organization_id = om.organization_id
      where u.user_id = ${userId}::uuid
        and u.lifecycle_status = 'ACTIVE'
        and om.lifecycle_status = 'ACTIVE'
        and o.lifecycle_status = 'ACTIVE'
    ) as allowed
  `

  if (!rows[0]?.allowed) {
    throw new ApiProblem(
      404,
      "RESOURCE_NOT_FOUND",
      "The requested campaign resource was not found.",
    )
  }
}

function stocktakingRevisionDto(row: postgres.Row) {
  return {
    stocktakingRevisionId: row.stocktaking_revision_id,
    stocktakingRecordId: row.stocktaking_record_id,
    revisionNumber: Number(row.revision_number),
    revisionState: row.revision_state,
    basedOnRevisionId: row.based_on_revision_id,
    isEmpty: row.is_empty,
    commodityVersionId: row.commodity_version_id,
    actualBulkDensityKgM3: row.actual_bulk_density_kg_m3,
    ownership: row.ownership,
    clientId: row.client_id,
    notes: row.notes,
    calculationResultId: row.calculation_result_id,
    validatedAt: row.validated_at,
    validatedBy: row.validated_by,
    createdAt: row.created_at,
    createdBy: row.created_by,
    entityVersion: Number(row.entity_version),
    ...(row.measurements === undefined ? {} : { measurements: row.measurements }),
    ...(row.calculation_result === undefined
      ? {}
      : { calculationResult: row.calculation_result }),
    ...(row.evidence_summary === undefined
      ? {}
      : { evidenceSummary: row.evidence_summary }),
  }
}

function stocktakingRecordDto(row: postgres.Row) {
  return {
    stocktakingRecordId: row.stocktaking_record_id,
    organizationId: row.organization_id,
    campaignId: row.campaign_id,
    storageSpaceId: row.storage_space_id,
    currentRevisionId: row.current_revision_id,
    storageSpaceCode: row.storage_space_code,
    storageUnitId: row.storage_unit_id,
    storageUnitCode: row.storage_unit_code,
    locationId: row.location_id,
    locationCode: row.location_code,
    createdAt: row.record_created_at,
    createdBy: row.record_created_by,
    updatedAt: row.record_updated_at,
    updatedBy: row.record_updated_by,
    entityVersion: Number(row.record_entity_version),
    currentRevision: stocktakingRevisionDto(row),
    issueSummary: {
      openCount: Number(row.open_issue_count ?? 0),
      resolvedCount: Number(row.resolved_issue_count ?? 0),
    },
    evidenceSummary: row.record_evidence_summary ?? {
      photoCount: 0,
      noteCount: 0,
    },
  }
}

function postgresFailure(correlationId: string, error: unknown): Response {
  const message = String(error instanceof Error ? error.message : error)
  const sqlState = error !== null && typeof error === "object" && "code" in error
    ? String(error.code) : null
  if (message.includes("CLOSE_GUARDS_FAILED")) {
    const guards: JsonObject = {}
    if (error !== null && typeof error === "object" && "detail" in error && typeof error.detail === "string") {
      try {
        const detail = JSON.parse(error.detail)
        for (const key of ["campaignStatus", "scopeCount", "toDoCount", "openIssueCount", "pendingEvidenceCount", "pendingCriticalSyncCount", "canClose"]) {
          if (detail && hasOwn(detail, key)) guards[key] = detail[key]
        }
      } catch { /* Do not expose unstructured database diagnostics. */ }
    }
    return fail(correlationId, 409, "CLOSE_GUARDS_FAILED", "The Campaign does not satisfy the Close Guards.", [], false, { guards })
  }
  if (sqlState === "42501") {
    return fail(correlationId, 403, "AUTH_FORBIDDEN", "The operation is not permitted.")
  }
  if (sqlState === "55000" && message.toLowerCase().includes("complet")) {
    return fail(correlationId, 409, "CAMPAIGN_COMPLETED_IMMUTABLE", "Completed Campaign context cannot be changed.")
  }
  if (sqlState === "22023" || sqlState === "22P02") {
    return fail(correlationId, 422, "VALIDATION_FAILED", "One or more field values are invalid.")
  }

  if (message.includes("EVIDENCE_PHOTO_LIMIT")) {
    return fail(correlationId, 422, "EVIDENCE_PHOTO_LIMIT", "The maximum active PHOTO count has been reached.")
  }

  if (message.includes("EVIDENCE_HASH_MISMATCH")) {
    return fail(correlationId, 422, "EVIDENCE_HASH_MISMATCH", "The uploaded bytes do not match the declared Evidence metadata.")
  }

  if (message.includes("EVIDENCE_IMMUTABLE")) {
    return fail(correlationId, 409, "EVIDENCE_IMMUTABLE", "Validated or frozen Evidence cannot be changed.")
  }

  if (message.includes("ISSUE_ALREADY_STATE")) {
    return fail(correlationId, 409, "ISSUE_ALREADY_STATE", "The Issue is already in the requested state.")
  }

  if (message.includes("campaign completed")) {
    return fail(correlationId, 409, "ISSUE_COMPLETED_CAMPAIGN", "The Issue cannot be changed after Campaign completion.")
  }

  if (message.includes("STOCKTAKING_NOT_TEAM_MEMBER")) {
    return fail(correlationId, 403, "STOCKTAKING_NOT_TEAM_MEMBER", "The authenticated user is not an active Campaign Team member.")
  }

  if (message.includes("STOCKTAKING_REVISION_STALE")) {
    return fail(correlationId, 409, "STOCKTAKING_REVISION_STALE", "The stocktaking revision has changed and must be reloaded before validation.")
  }

  if (message.includes("STOCKTAKING_VALIDATION_FAILED")) {
    return fail(correlationId, 422, "STOCKTAKING_VALIDATION_FAILED", "The stocktaking revision cannot be validated.")
  }

  if (message.includes("forbidden")) {
    return fail(correlationId, 403, "AUTH_FORBIDDEN", "The operation is not permitted.")
  }

  if (message.includes("not found")) {
    return fail(correlationId, 404, "RESOURCE_NOT_FOUND", "The requested resource was not found.")
  }

  if (message.includes("entity version conflict")) {
    return fail(
      correlationId,
      409,
      "ENTITY_VERSION_CONFLICT",
      "The entity has changed since the supplied baseEntityVersion.",
    )
  }

  if (message.includes("last active administrator")) {
    return fail(
      correlationId,
      409,
      "LAST_ACTIVE_ADMIN_CONFLICT",
      "The last active administrator cannot be disabled or demoted.",
    )
  }

  if (message.includes("idempotency key payload conflict")) {
    return fail(
      correlationId,
      409,
      "IDEMPOTENCY_KEY_REUSE_MISMATCH",
      "The idempotency key was already used for a different logical request.",
    )
  }

  if (
    message.includes("Historically referenced") ||
    message.includes("immutable")
  ) {
    return fail(
      correlationId,
      409,
      "CONFLICT",
      "The requested mutation would alter immutable historical identity.",
    )
  }

  if (
    message.includes("duplicate key") ||
    message.includes("unique constraint")
  ) {
    return fail(
      correlationId,
      409,
      "CONFLICT",
      "A resource with the same business identity already exists.",
    )
  }

  if (
    message.includes("required") ||
    message.includes("invalid") ||
    message.includes("both be present")
  ) {
    return fail(
      correlationId,
      422,
      "VALIDATION_FAILED",
      message,
    )
  }

  console.error("api-v1 postgres failure", correlationId)
  return fail(
    correlationId,
    500,
    "INTERNAL_ERROR",
    "The request could not be processed.",
  )
}

function frozenFixedParameters(family: FrozenCalculationInput["geometryFamily"], dimensions: Record<string, unknown>): Record<string, string> {
  const value = (key: string): string => {
    const v = dimensions[key]
    if (v === null || v === undefined || String(v).trim() === "") throw new Error(`missing frozen geometry parameter: ${key}`)
    return String(v)
  }

  switch (family) {
    case "CYL_TRUNC_CONE_V1":
      return { diameterM: value("diameterM"), cylinderHeightM: value("cylinderHeightM"), hopperHeightM: value("hopperHeightM"), outletDiameterM: value("outletDiameterM") }
    case "CYL_FLAT_V1":
      return { diameterM: value("diameterM"), heightM: value("heightM") }
    case "SQUARE_FLAT_V1":
      return { sideM: value("sideLengthM"), heightM: value("heightM") }
    case "RECT_FLAT_V1":
      return { lengthM: value("lengthM"), widthM: value("widthM"), heightM: value("heightM") }
    case "SQUARE_PYRAMID_HOPPER_V1":
      return { sideM: value("sideLengthM"), bodyHeightM: value("bodyHeightM"), hopperHeightM: value("hopperHeightM") }
    case "RECT_PYRAMID_HOPPER_V1":
      return { lengthM: value("lengthM"), widthM: value("widthM"), bodyHeightM: value("bodyHeightM"), hopperHeightM: value("hopperHeightM") }
    case "REG_HEX_FLAT_V1":
      return { sideM: value("sideLengthM"), heightM: value("heightM") }
    case "STAR_VERIFIED_AREA_V1":
    case "POCKET_VERIFIED_AREA_V1":
    case "PERIMETER_VERIFIED_AREA_V1":
      return { areaM2: value("verifiedAreaM2"), heightM: value("heightM") }
  }
}
async function loadFrozenCalculationInput(
  organizationId: string,
  campaignId: string,
  stocktakingRecordId: string,
): Promise<{
  input: FrozenCalculationInput
  revisionEntityVersion: number
  provisionalInputFingerprint: string
  provisionalProductVolumeM3: string
  datumMetadata: JsonObject
}> {
  const rows = await db`
    select
      sr.current_revision_id,
      rev.entity_version as revision_entity_version,
      rev.revision_state,
      rev.is_empty,
      rev.actual_bulk_density_kg_m3,
      cr.input_fingerprint,
      cr.product_volume_m3,
      cr.entered_density_value,
      cr.entered_density_unit,
      cr.datum_metadata,
      cssi.snapshot_scope_item_id,
      cssi.geometry_family_id,
      cssi.geometry_family_code,
      cssi.geometry_profile_version_id,
      cssi.geometry_formula_version,
      cssi.geometry_dimensions,
      cssi.measurement_reference_height_m,
      cssi.maximum_fill_height_m,
      cs.calculation_engine_version
    from silomath.stocktaking_records sr
    join silomath.stocktaking_revisions rev
      on rev.stocktaking_revision_id = sr.current_revision_id
     and rev.stocktaking_record_id = sr.stocktaking_record_id
    join silomath.campaign_snapshots cs
      on cs.campaign_id = sr.campaign_id
     and cs.organization_id = sr.organization_id
    join silomath.campaign_snapshot_scope_items cssi
      on cssi.campaign_snapshot_id = cs.campaign_snapshot_id
     and cssi.storage_space_id = sr.storage_space_id
    join silomath.calculation_results cr
      on cr.calculation_result_id = rev.calculation_result_id
     and cr.stocktaking_revision_id = rev.stocktaking_revision_id
    where sr.stocktaking_record_id = ${stocktakingRecordId}::uuid
      and sr.organization_id = ${organizationId}::uuid
      and sr.campaign_id = ${campaignId}::uuid
    limit 1
  `

  const row = rows[0]
  if (!row) {
    throw new ApiProblem(
      404,
      "RESOURCE_NOT_FOUND",
      "Stocktaking working calculation context was not found.",
    )
  }

  if (row.revision_state !== "WORKING" || row.is_empty === true) {
    throw new ApiProblem(
      422,
      "STOCKTAKING_VALIDATION_FAILED",
      "Only a non-empty WORKING stocktaking revision can be validated.",
    )
  }

  const measurementRows = await db`
    select
      sequence_number,
      raw_value,
      raw_unit,
      datum_metadata,
      geometry_profile_version_id
    from silomath.measurements
    where stocktaking_revision_id = ${row.current_revision_id}::uuid
    order by sequence_number
  `

  if (measurementRows.length < 1 || measurementRows.length > 5) {
    throw new ApiProblem(
      422,
      "STOCKTAKING_VALIDATION_FAILED",
      "Stocktaking validation requires between 1 and 5 measurements.",
    )
  }

  const allowedFamilies = new Set([
    "CYL_TRUNC_CONE_V1",
    "CYL_FLAT_V1",
    "SQUARE_FLAT_V1",
    "RECT_FLAT_V1",
    "SQUARE_PYRAMID_HOPPER_V1",
    "RECT_PYRAMID_HOPPER_V1",
    "REG_HEX_FLAT_V1",
    "STAR_VERIFIED_AREA_V1",
    "POCKET_VERIFIED_AREA_V1",
    "PERIMETER_VERIFIED_AREA_V1",
  ])

  const family = String(row.geometry_family_code)
  if (!allowedFamilies.has(family)) {
    throw new ApiProblem(
      422,
      "STOCKTAKING_VALIDATION_FAILED",
      "The frozen geometry family is not supported by the approved calculation contract.",
    )
  }

  const dimensions = row.geometry_dimensions as Record<string, unknown>
  const firstDatum = measurementRows[0].datum_metadata as JsonObject
  const datumReference = String(firstDatum.reference ?? "").trim()
  const measurementReferenceHeightM =
    String(row.measurement_reference_height_m ?? "")

  if (!datumReference || !measurementReferenceHeightM) {
    throw new ApiProblem(
      422,
      "STOCKTAKING_VALIDATION_FAILED",
      "Frozen datum/reference metadata is incomplete.",
    )
  }

  const enteredDensityValue = String(row.entered_density_value ?? "")
  const enteredDensityUnit = String(row.entered_density_unit ?? "")

  if (!enteredDensityValue || !enteredDensityUnit) {
    throw new ApiProblem(
      422,
      "STOCKTAKING_VALIDATION_FAILED",
      "Frozen entered density is incomplete.",
    )
  }

  const input: FrozenCalculationInput = {
    frozenContextId: String(row.snapshot_scope_item_id),
    geometryFamilyId: String(row.geometry_family_id),
    geometryProfileVersionId: String(row.geometry_profile_version_id),
    geometryFamily: family as FrozenCalculationInput["geometryFamily"],
    formulaCode: family,
    formulaVersion: Number(row.geometry_formula_version),
    inputSchemaVersion: 1,
    engineVersion: String(row.calculation_engine_version),
    normalizationVersion: 1,
    surfaceModelVersion: "LEVEL_BASED_V1",
    aggregationMethod: "MEAN_ULLAGE",
    aggregationRuleVersion: 1,
    geometryOrigin: String(dimensions.geometryOrigin ?? ""),
    datumReference,
    measurementReferenceHeightM,
    maximumFillHeightM: String(row.maximum_fill_height_m ?? ""),
    fixedParameters: frozenFixedParameters(
      family as FrozenCalculationInput["geometryFamily"],
      dimensions,
    ),
    approvedAreaProvenance: family.endsWith("_VERIFIED_AREA_V1")
      ? String(dimensions.areaProvenance ?? "")
      : null,
    hasConstantCrossSection: family.endsWith("_VERIFIED_AREA_V1")
      ? true
      : null,
    readings: measurementRows.map((m: postgres.Row) => {
      const datum = m.datum_metadata as JsonObject
      return {
        sequenceNumber: Number(m.sequence_number),
        geometryProfileVersionId: String(m.geometry_profile_version_id),
        datumReference: String(datum.reference ?? ""),
        geometryOrigin: String(dimensions.geometryOrigin ?? ""),
        measurementReferenceHeightM: String(
          datum.measurementReferenceHeightM ??
            row.measurement_reference_height_m ??
            "",
        ),
        rawValue: String(m.raw_value),
        rawUnit: String(m.raw_unit),
        measurementType: "ULLAGE",
      }
    }),
    density: {
      value: enteredDensityValue,
      unit: enteredDensityUnit,
    },
  }

  return {
    input,
    revisionEntityVersion: Number(row.revision_entity_version),
    provisionalInputFingerprint: String(row.input_fingerprint),
    provisionalProductVolumeM3: String(row.product_volume_m3),
    datumMetadata: firstDatum,
  }
}
export function createApiHandler(dependencies: {
  authenticate?: typeof authenticatedUser
  database?: typeof db
  configuration?: (name: string) => string | undefined
  evidenceStore?: EvidenceObjectStore
} = {}) {
  const authenticate = dependencies.authenticate ?? authenticatedUser
  const database = dependencies.database ?? db
  const configuration = dependencies.configuration ?? ((name: string) => Deno.env.get(name))
  const objectStore = dependencies.evidenceStore ?? evidenceObjectStore(configuration)
  return async function handler(req: Request): Promise<Response> {
  const correlationId = cid(req)
  const path = pathOf(req)

  if (req.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: { "X-Correlation-Id": correlationId },
    })
  }

  if (req.method === "GET" && path === "/health") {
    return json(correlationId, 200, {
      status: "ok",
      service: "silomath-api-v1",
    })
  }

  const user = await authenticate(req)

  if (!user) {
    return fail(
      correlationId,
      401,
      "AUTH_SESSION_EXPIRED",
      "Authentication is required.",
    )
  }

  const locationCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/locations$`),
  )
  const locationItem = path.match(
    new RegExp(`^/organizations/(${UUID})/locations/(${UUID})$`),
  )
  const storageUnitCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/locations/(${UUID})/storage-units$`),
  )
  const storageUnitItem = path.match(
    new RegExp(`^/organizations/(${UUID})/storage-units/(${UUID})$`),
  )
  const storageSpaceCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/storage-units/(${UUID})/storage-spaces$`),
  )
  const storageSpaceItem = path.match(
    new RegExp(`^/organizations/(${UUID})/storage-spaces/(${UUID})$`),
  )
  const stocktakingCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking$`),
  )
  const stocktakingItem = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID})$`),
  )
  const stocktakingWorking = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID})/working-revision$`),
  )
  const stocktakingValidate = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID}):validate$`),
  )
  const stocktakingMarkEmpty = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID}):mark-empty$`),
  )
  const stocktakingResetValidation = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID}):reset-validation$`),
  )
  const stocktakingRevisions = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID})/revisions$`),
  )
  const organizationCollection = path === "/organizations"
  const organizationItem = path.match(
    new RegExp(`^/organizations/(${UUID})$`),
  )
  const organizationUsers = path.match(
    new RegExp(`^/organizations/(${UUID})/users$`),
  )
  const organizationUser = path.match(
    new RegExp(`^/organizations/(${UUID})/users/(${UUID})$`),
  )
  const membershipItem = path.match(
    new RegExp(`^/organizations/(${UUID})/memberships/(${UUID})$`),
  )
  const campaignCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns$`),
  )
  const campaignItem = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})$`),
  )
  const campaignScope = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/scope$`),
  )
  const campaignTeam = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/team$`),
  )
  const offlineManifest = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/offline-manifest$`),
  )
  const offlinePackage = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/offline-package$`),
  )
  const storageSpaceBatch = path.match(
    new RegExp(`^/organizations/(${UUID})/storage-units/(${UUID})/storage-spaces:batch-create$`),
  )
  const campaignLaunch = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns:launch$`),
  )
  const issueCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/stocktaking/(${UUID})/issues$`),
  )
  const issueResolve = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/issues/(${UUID}):resolve$`),
  )
  const issueReopen = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/issues/(${UUID}):reopen$`),
  )
  const issueAcceptFollowUp = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/issues/(${UUID}):accept-follow-up$`),
  )
  const evidenceCollection = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/evidence$`),
  )
  const evidenceUpload = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/evidence/(${UUID})/upload$`),
  )
  const evidenceItem = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/evidence/(${UUID})$`),
  )
  const evidenceContent = path.match(
    new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/evidence/(${UUID})/content$`),
  )

  try {
    const configurationRoute = path.match(new RegExp(`^/organizations/(${UUID})/configuration$`))
    if (configurationRoute && req.method === "GET") {
      const organizationId = configurationRoute[1]
      enforceOrganizationContext(req, organizationId)
      const access = await database`
        select silomath.location_actor_is_active_member(${user.id}::uuid,${organizationId}::uuid) as allowed
      `
      if (!access[0]?.allowed) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "Configuration scope not found.")
      const rows = await database`select configuration_key from silomath.organization_configurations
        where organization_id=${organizationId}::uuid`
      if (rows.length > 0) {
        throw new ApiProblem(503, "SERVICE_UNAVAILABLE", "Approved configuration registry is unavailable.", [], false)
      }
      return json(correlationId, 200, { organizationId, items: [] })
    }
    const reportExport = path.match(new RegExp(`^/organizations/(${UUID})/reports/([A-Z_]+):export$`))
    if (reportExport && req.method === "POST") {
      const body = await bodyJson(req)
      if (body.format !== "CSV" && body.format !== "PDF") {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Report format must be PDF or CSV.")
      }
      if (reportExport[2] === "DIGITAL_INVENTORY_DOSSIER" && body.format !== "PDF") {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Digital Inventory Dossier is PDF only.")
      }
      const datasetRequest = { ...body }
      delete datasetRequest.format
      const url = new URL(req.url)
      url.pathname = url.pathname.replace(/:export$/, ":dataset")
      const headers = new Headers(req.headers)
      headers.set("X-Correlation-Id", correlationId)
      const datasetResponse = await handler(new Request(url, {
        method: "POST", headers, body: JSON.stringify(datasetRequest),
      }))
      if (!datasetResponse.ok) return datasetResponse
      const dataset = await datasetResponse.json()
      if (body.format === "PDF") {
        throw new ApiProblem(503, "SERVICE_UNAVAILABLE", "The PDF renderer has not been configured.", [], false)
      }
      const rows: JsonObject[] = Array.isArray(dataset.rows) ? dataset.rows : dataset.changes ?? []
      const metadata: JsonObject = {
        organizationId: reportExport[1], reportType: reportExport[2],
        campaignId: dataset.campaignId ?? null, finalSnapshotId: dataset.finalSnapshotId ?? null,
        sourceState: dataset.sourceState ?? "FINAL", generatedAt: dataset.generatedAt,
        generatedBy: dataset.generatedBy, filters: dataset.filters ?? {},
      }
      const columns = [...new Set(rows.flatMap((row) => Object.keys(row)))].sort()
      const keys = [...Object.keys(metadata), ...columns]
      const cell = (value: unknown) => {
        const text = value === null || value === undefined ? "" : typeof value === "object" ? stableJson(value) : String(value)
        return '"' + text.replaceAll('"', '""') + '"'
      }
      const csv = [keys.map(cell).join(","), ...rows.map((row) =>
        keys.map((key) => cell(hasOwn(metadata, key) ? metadata[key] : row[key])).join(",")
      )].join("\r\n") + "\r\n"
      await database`
        select silomath.report_export_record(
          ${user.id}::uuid,${reportExport[1]}::uuid,${reportExport[2]},
          ${JSON.stringify(metadata)}::jsonb,${correlationId}
        )
      `
      return new Response(csv, { status: 200, headers: {
        "Content-Type": "text/csv; charset=utf-8",
        "Content-Disposition": `attachment; filename="SiloMath_${reportExport[2]}_${reportExport[1]}_${String(dataset.generatedAt).replaceAll(/[^0-9]/g, "")}.csv"`,
        "Cache-Control": "private, no-store", "X-Correlation-Id": correlationId,
      } })
    }
    const profiles = path.match(new RegExp(`^/organizations/(${UUID})/geometry-profiles$`))
    const profile = path.match(new RegExp(`^/organizations/(${UUID})/geometry-profiles/(${UUID})$`))
    const profileVersions = path.match(new RegExp(`^/organizations/(${UUID})/geometry-profiles/(${UUID})/versions$`))
    if ((profiles || profile || profileVersions) && ["GET", "POST", "PATCH"].includes(req.method)) {
      const organizationId = (profiles || profile || profileVersions)![1]
      enforceOrganizationContext(req, organizationId)
      const access = await database`
        select silomath.location_actor_is_active_member(${user.id}::uuid,${organizationId}::uuid) as allowed,
        silomath.location_actor_is_active_admin(${user.id}::uuid,${organizationId}::uuid) as admin
      `
      if (!access[0]?.allowed) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "Geometry scope not found.")
      if (req.method === "GET") {
        const q = pageQuery(req)
        if (profileVersions) {
          const rows = await database`
            select v.version_number,v.geometry_profile_version_id,silomath.geometry_version_to_json(v) as body
            from silomath.geometry_profile_versions v join silomath.geometry_profiles p
            on p.geometry_profile_id=v.geometry_profile_id
            where p.organization_id=${organizationId}::uuid and p.geometry_profile_id=${profileVersions[2]}::uuid
              and (${q.cursor.id}::uuid is null or (v.version_number,v.geometry_profile_version_id)>
                (${q.cursor.code}::integer,${q.cursor.id}::uuid))
            order by v.version_number,v.geometry_profile_version_id limit ${q.limit + 1}
          `
          const visible = rows.slice(0, q.limit)
          const last = visible.at(-1)
          return json(correlationId, 200, { items: visible.map((row) => row.body),
            nextCursor: rows.length > q.limit && last ? encodeCursor(String(last.version_number), String(last.geometry_profile_version_id)) : null,
            hasMore: rows.length > q.limit })
        }
        if (profile) {
          const rows = await database`select silomath.geometry_profile_to_json(p) as body from silomath.geometry_profiles p
            where p.organization_id=${organizationId}::uuid and p.geometry_profile_id=${profile[2]}::uuid`
          if (!rows[0]) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "Geometry Profile not found.")
          return json(correlationId, 200, rows[0].body)
        }
        if (q.status !== null && !["ACTIVE", "INACTIVE"].includes(q.status)) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid lifecycle filter.")
        }
        const rows = await database`
          select p.code,p.geometry_profile_id,silomath.geometry_profile_to_json(p) as body
          from silomath.geometry_profiles p where p.organization_id=${organizationId}::uuid
          and (${q.search}::text is null or p.name ilike '%'||${q.search}||'%' or p.code ilike '%'||${q.search}||'%')
          and (${q.status}::text is null or p.lifecycle_status=${q.status})
          and (${q.cursor.id}::uuid is null or (p.code,p.geometry_profile_id)>(${q.cursor.code},${q.cursor.id}::uuid))
          order by p.code,p.geometry_profile_id limit ${q.limit + 1}
        `
        const visible = rows.slice(0, q.limit)
        const last = visible.at(-1)
        return json(correlationId, 200, { items: visible.map((row) => row.body),
          nextCursor: rows.length > q.limit && last ? encodeCursor(String(last.code), String(last.geometry_profile_id)) : null,
          hasMore: rows.length > q.limit })
      }
      if (!access[0].admin) throw new ApiProblem(403, "AUTH_FORBIDDEN", "Admin membership required.")
      const create = profiles !== null && req.method === "POST"
      if (!create && !(profile && req.method === "PATCH")) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "API route not found.")
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const id = create ? body.geometryProfileId : profile![2]
      if (typeof id !== "string" || !new RegExp(`^${UUID}$`).test(id)) throw new ApiProblem(422, "VALIDATION_FAILED", "Geometry Profile UUID required.")
      const fields = ["name", "code", "lifecycleStatus", "geometryFamilyId", "dimensions",
        "measurementReferenceHeightM", "maximumFillHeightM", "geometryOrigin", "areaProvenance", "hasConstantCrossSection"]
      const controls = ["geometryProfileId", "clientOperationId", "idempotencyKey", "baseEntityVersion"]
      if (Object.keys(body).some((key) => !fields.includes(key) && !controls.includes(key))) throw new ApiProblem(422, "VALIDATION_FAILED", "Unsupported Geometry field.")
      const patch: JsonObject = {}
      for (const field of fields) if (hasOwn(body, field)) patch[field] = body[field]
      const calculationFields = ["geometryFamilyId", "dimensions", "measurementReferenceHeightM", "maximumFillHeightM", "geometryOrigin", "areaProvenance", "hasConstantCrossSection"]
      const calculationChanged = create || calculationFields.some((key) => hasOwn(body, key))
      let capacity: string | null = null
      const schema = configuration("SILOMATH_GEOMETRY_SCHEMA_VERSION")?.trim() ?? null
      if (calculationChanged) {
        let existing: JsonObject = {}
        if (!create) {
          const rows = await database`select silomath.geometry_profile_to_json(p) as body from silomath.geometry_profiles p
            where p.organization_id=${organizationId}::uuid and p.geometry_profile_id=${id}::uuid`
          if (!rows[0]) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "Geometry Profile not found.")
          existing = { ...rows[0].body.currentVersion, geometryFamilyId: rows[0].body.geometryFamilyId }
        }
        const effective = { ...existing, ...patch }
        if (typeof effective.geometryFamilyId !== "string" || !new RegExp(`^${UUID}$`).test(effective.geometryFamilyId)) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "Geometry Family UUID required.")
        }
        const families = await database`select family_code from silomath.geometry_families where geometry_family_id=${effective.geometryFamilyId}::uuid`
        const familiesByCode = ["CYL_TRUNC_CONE_V1", "CYL_FLAT_V1", "SQUARE_FLAT_V1", "RECT_FLAT_V1",
          "SQUARE_PYRAMID_HOPPER_V1", "RECT_PYRAMID_HOPPER_V1", "REG_HEX_FLAT_V1", "STAR_VERIFIED_AREA_V1",
          "POCKET_VERIFIED_AREA_V1", "PERIMETER_VERIFIED_AREA_V1"]
        const code = String(families[0]?.family_code ?? "")
        const family = familiesByCode[Number(code.slice(1)) - 1]
        if (!/^F(0[1-9]|10)$/.test(code) || !family) throw new ApiProblem(422, "VALIDATION_FAILED", "Governed Geometry Family required.")
        if (!effective.dimensions || typeof effective.dimensions !== "object" || Array.isArray(effective.dimensions)) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "Typed Geometry dimensions required.")
        }
        const dimensions = Object.fromEntries(Object.entries(effective.dimensions as JsonObject).map(([key, value]) => [key, String(value)]))
        const input = {
          frozenContextId: id, geometryProfileVersionId: id, geometryFamilyId: effective.geometryFamilyId,
          geometryFamily: family, formulaCode: family, formulaVersion: 1, inputSchemaVersion: 1,
          engineVersion: "2.0", normalizationVersion: 1, surfaceModelVersion: "LEVEL_BASED_V1",
          aggregationMethod: "MEAN_ULLAGE", aggregationRuleVersion: 1,
          geometryOrigin: String(effective.geometryOrigin ?? ""), datumReference: String(effective.geometryOrigin ?? ""),
          measurementReferenceHeightM: String(effective.measurementReferenceHeightM),
          maximumFillHeightM: String(effective.maximumFillHeightM), fixedParameters: dimensions,
          approvedAreaProvenance: typeof effective.areaProvenance === "string" ? effective.areaProvenance
            : effective.areaProvenance && typeof effective.areaProvenance === "object"
              ? stableJson(effective.areaProvenance) : null,
          hasConstantCrossSection: effective.hasConstantCrossSection === true,
          readings: [], density: null,
        } as FrozenCalculationInput
        capacity = calculateGeometryCapacity(input)
        if (!schema) throw new ApiProblem(503, "SERVICE_UNAVAILABLE", "Geometry schema configuration unavailable.", [], true)
        for (const field of calculationFields) if (effective[field] !== undefined) patch[field] = effective[field]
      }
      const hash = await fingerprint({ method: req.method, path, organizationId, body })
      const rows = await database`select silomath.geometry_profile_mutate_command(
        ${user.id}::uuid,${organizationId}::uuid,${id}::uuid,${JSON.stringify(patch)}::jsonb,${capacity}::numeric,${schema},
        ${create ? null : baseEntityVersion(body, req)}::bigint,${key},${hash},${correlationId},${create}) as result`
      return json(correlationId, Number(rows[0].result.status), rows[0].result.body)
    }
    const historyCompare = path.match(new RegExp(`^/organizations/(${UUID})/inventory-history:compare$`))
    const dossier = path.match(new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/dossier$`))
    if (historyCompare && req.method === "POST") {
      enforceOrganizationContext(req, historyCompare[1])
      const body = await bodyJson(req)
      const rows = await database`select silomath.inventory_history_compare(
        ${user.id}::uuid,${historyCompare[1]}::uuid,${JSON.stringify(body)}::jsonb) as body`
      return json(correlationId, 200, rows[0].body)
    }
    if (dossier && req.method === "GET") {
      enforceOrganizationContext(req, dossier[1])
      const rows = await database`select silomath.inventory_dossier_get(
        ${user.id}::uuid,${dossier[1]}::uuid,${dossier[2]}::uuid) as body`
      return json(correlationId, 200, rows[0].body)
    }
    const reportCatalog = path.match(new RegExp(`^/organizations/(${UUID})/reports$`))
    const reportDataset = path.match(new RegExp(`^/organizations/(${UUID})/reports/([A-Z_]+):dataset$`))
    const reportTypes = [
      "PHYSICAL_INVENTORY", "INVENTORY_BY_LOCATION", "INVENTORY_BY_COMMODITY",
      "INVENTORY_BY_OWNERSHIP", "STORAGE_SPACE_WORKSHEET", "CAPACITY_REPORT",
      "INVENTORY_HISTORY", "DIGITAL_INVENTORY_DOSSIER", "AUDIT_TRAIL",
    ]
    if (reportCatalog && req.method === "GET") {
      const organizationId = reportCatalog[1]
      enforceOrganizationContext(req, organizationId)
      const rows = await database`
        select silomath.location_actor_is_active_member(${user.id}::uuid,${organizationId}::uuid) as allowed,
        silomath.location_actor_is_active_admin(${user.id}::uuid,${organizationId}::uuid) as admin
      `
      if (!rows[0]?.allowed) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "Report scope was not found.")
      return json(correlationId, 200, {
        items: reportTypes.filter((type) => type !== "AUDIT_TRAIL" || rows[0].admin)
          .map((type) => ({ reportType: type, formats: type === "DIGITAL_INVENTORY_DOSSIER" ? ["PDF"] : ["PDF", "CSV"] })),
      })
    }
    if (reportDataset && req.method === "POST") {
      const organizationId = reportDataset[1]
      const reportType = reportDataset[2]
      if (!reportTypes.includes(reportType)) throw new ApiProblem(422, "VALIDATION_FAILED", "Unknown report type.")
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      let rows
      if (reportType === "INVENTORY_HISTORY") {
        rows = await database`select silomath.inventory_history_compare(
          ${user.id}::uuid,${organizationId}::uuid,${JSON.stringify(body)}::jsonb) as body`
      } else if (reportType === "DIGITAL_INVENTORY_DOSSIER") {
        if (typeof body.campaignId !== "string" || !new RegExp(`^${UUID}$`).test(body.campaignId)) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "A Completed Campaign UUID is required.")
        }
        rows = await database`select silomath.inventory_dossier_get(
          ${user.id}::uuid,${organizationId}::uuid,${body.campaignId}::uuid) as body`
      } else if (reportType === "AUDIT_TRAIL") {
        rows = await database`select silomath.audit_report_dataset_get(
          ${user.id}::uuid,${organizationId}::uuid,${JSON.stringify(body)}::jsonb) as body`
      } else {
        rows = await database`
          select silomath.report_dataset_get(
            ${user.id}::uuid,${organizationId}::uuid,${reportType},${JSON.stringify(body)}::jsonb
          ) as body
        `
      }
      return json(correlationId, 200, rows[0].body)
    }
    const closeReadiness = path.match(new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/close-readiness$`))
    const finalSnapshot = path.match(new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/final-snapshot$`))
    const campaignClose = path.match(new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID}):close$`))
    if ((closeReadiness || finalSnapshot) && req.method === "GET") {
      const match = (closeReadiness || finalSnapshot)!
      enforceOrganizationContext(req, match[1])
      const rows = closeReadiness
        ? await database`select silomath.campaign_close_readiness(
            ${user.id}::uuid,${match[1]}::uuid,${match[2]}::uuid) as body`
        : await database`select silomath.final_inventory_snapshot_get(
            ${user.id}::uuid,${match[1]}::uuid,${match[2]}::uuid) as body`
      return json(correlationId, 200, rows[0].body)
    }
    if (campaignClose && req.method === "POST") {
      const organizationId = campaignClose[1]
      const campaignId = campaignClose[2]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const expected = baseEntityVersion(body, req)
      const schema = configuration("SILOMATH_FINAL_SNAPSHOT_SCHEMA_VERSION")?.trim()
      if (!schema) throw new ApiProblem(503, "SERVICE_UNAVAILABLE", "Final snapshot compatibility configuration unavailable.", [], true)
      const hash = await fingerprint({ method: req.method, path, organizationId, campaignId, body })
      const rows = await database`
        select silomath.campaign_close_command(
          ${user.id}::uuid,${organizationId}::uuid,${campaignId}::uuid,
          ${expected}::bigint,${key},${hash},${correlationId},${schema}
        ) as result
      `
      return json(correlationId, Number(rows[0].result.status), rows[0].result.body)
    }
    const campaignSnapshot = path.match(new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/snapshot$`))
    if (campaignSnapshot && req.method === "GET") {
      const organizationId = campaignSnapshot[1]
      enforceOrganizationContext(req, organizationId)
      const rows = await database`
        select silomath.campaign_snapshot_get(
          ${user.id}::uuid,${organizationId}::uuid,${campaignSnapshot[2]}::uuid
        ) as body
      `
      return json(correlationId, 200, rows[0].body)
    }
    const auditEvents = path.match(new RegExp(`^/organizations/(${UUID})/audit-events$`))
    if (auditEvents && req.method === "GET") {
      const organizationId = auditEvents[1]
      enforceOrganizationContext(req, organizationId)
      const access = await database`
        select silomath.location_actor_is_active_admin(
          ${user.id}::uuid,${organizationId}::uuid
        ) as allowed
      `
      if (!access[0]?.allowed) throw new ApiProblem(403, "AUTH_FORBIDDEN", "Active Admin membership required.")
      const q = pageQuery(req)
      const params = new URL(req.url).searchParams
      const campaignId = params.get("campaignId")
      const actorId = params.get("actorUserId")
      const entityId = params.get("entityId")
      for (const id of [campaignId, actorId, entityId]) {
        if (id !== null && !new RegExp(`^${UUID}$`).test(id)) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid audit reference UUID.")
        }
      }
      const action = params.get("action")
      const entityType = params.get("entityType")
      const from = params.get("from")
      const to = params.get("to")
      for (const value of [from, to]) {
        if (value !== null && !Number.isFinite(Date.parse(value))) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid audit time range.")
        }
      }
      const rows = await database`
        select a.* from silomath.audit_events a
        where a.organization_id=${organizationId}::uuid
          and (${campaignId}::uuid is null or a.campaign_id=${campaignId}::uuid)
          and (${actorId}::uuid is null or a.actor_user_id=${actorId}::uuid)
          and (${entityId}::uuid is null or a.entity_id=${entityId}::uuid)
          and (${action}::text is null or a.action=${action})
          and (${entityType}::text is null or a.entity_type=${entityType})
          and (${from}::timestamptz is null or a.occurred_at>=${from}::timestamptz)
          and (${to}::timestamptz is null or a.occurred_at<=${to}::timestamptz)
          and (${q.cursor.id}::uuid is null or (a.occurred_at,a.audit_event_id)<
            (${q.cursor.code}::timestamptz,${q.cursor.id}::uuid))
        order by a.occurred_at desc,a.audit_event_id desc limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map((row) => ({
          auditEventId: row.audit_event_id, organizationId: row.organization_id,
          campaignId: row.campaign_id, actorUserId: row.actor_user_id,
          action: row.action, entityType: row.entity_type, entityId: row.entity_id,
          occurredAt: row.occurred_at, beforeData: row.before_data, afterData: row.after_data,
          changeMetadata: row.change_metadata, correlationId: row.correlation_id,
          source: row.source, systemActor: row.system_actor,
        })),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(new Date(last.occurred_at).toISOString(), String(last.audit_event_id)) : null,
        hasMore: rows.length > q.limit,
      })
    }
    const teamRemove = path.match(new RegExp(`^/organizations/(${UUID})/campaigns/(${UUID})/team/(${UUID}):remove$`))
    if ((campaignTeam || teamRemove) && req.method === "POST") {
      const match = (campaignTeam || teamRemove)!
      const organizationId = match[1]
      const campaignId = match[2]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const userId = teamRemove ? teamRemove[3] : body.userId
      if (typeof userId !== "string" || !new RegExp(`^${UUID}$`).test(userId)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "A canonical User UUID is required.")
      }
      const expected = teamRemove ? baseEntityVersion(body, req) : null
      const hash = await fingerprint({ method: req.method, path, organizationId, campaignId, body })
      const rows = await database`
        select silomath.campaign_team_mutate_command(
          ${user.id}::uuid,${organizationId}::uuid,${campaignId}::uuid,${userId}::uuid,
          ${Boolean(teamRemove)},${expected}::bigint,${key},${hash},${correlationId}
        ) as result
      `
      return json(correlationId, Number(rows[0].result.status), rows[0].result.body)
    }
    const notificationRead = path.match(new RegExp(`^/notifications/(${UUID}):mark-read$`))
    if (path === "/preferences" && req.method === "GET") {
      const rows = await database`select silomath.user_preferences_get(${user.id}::uuid) as body`
      return json(correlationId, 200, rows[0].body)
    }
    if (path === "/preferences" && req.method === "PATCH") {
      const body = await bodyJson(req)
      const rows = await database`
        select silomath.user_preferences_patch_command(
          ${user.id}::uuid,${JSON.stringify(body)}::jsonb
        ) as body
      `
      return json(correlationId, 200, rows[0].body)
    }
    if (path === "/notifications" && req.method === "GET") {
      const q = pageQuery(req)
      const readState = new URL(req.url).searchParams.get("readState")
      if (readState !== null && !["READ", "UNREAD"].includes(readState)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid notification readState.")
      }
      const active = await database`
        select 1 from silomath.users where user_id=${user.id}::uuid and lifecycle_status='ACTIVE'
      `
      if (!active[0]) throw new ApiProblem(403, "AUTH_FORBIDDEN", "Active User required.")
      const rows = await database`
        select n.notification_id,n.created_at,silomath.notification_to_json(n) as body
        from silomath.notifications n where n.recipient_user_id=${user.id}::uuid
          and n.created_at>=now()-interval '12 months'
          and (${readState}::text is null or n.state=${readState})
          and (${q.cursor.id}::uuid is null or (n.created_at,n.notification_id)<
            (${q.cursor.code}::timestamptz,${q.cursor.id}::uuid))
        order by n.created_at desc,n.notification_id desc limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map((row) => row.body),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(new Date(last.created_at).toISOString(), String(last.notification_id)) : null,
        hasMore: rows.length > q.limit,
      })
    }
    if (notificationRead && req.method === "POST") {
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const hash = await fingerprint({ method: req.method, path, body })
      const rows = await database`
        select silomath.notification_mark_read_command(
          ${user.id}::uuid,${notificationRead[1]}::uuid,${key},${hash}
        ) as body
      `
      return json(correlationId, 200, rows[0].body)
    }
    if (path === "/notifications:mark-all-read" && req.method === "POST") {
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const hash = await fingerprint({ method: req.method, path, body })
      const rows = await database`
        select silomath.notifications_mark_all_read_command(${user.id}::uuid,${key},${hash}) as body
      `
      return json(correlationId, 200, rows[0].body)
    }
    if (organizationItem && req.method === "PATCH") {
      const organizationId = organizationItem[1]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const version = baseEntityVersion(body, req)
      const fields = ["name", "code", "address", "city", "country", "lifecycleStatus"]
      const controls = ["clientOperationId", "idempotencyKey", "baseEntityVersion"]
      if (Object.keys(body).some((key) => !fields.includes(key) && !controls.includes(key))) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Unsupported Organization field.")
      }
      const patch: JsonObject = {}
      for (const field of fields) {
        if (hasOwn(body, field)) {
          if (body[field] !== null && typeof body[field] !== "string") {
            throw new ApiProblem(422, "VALIDATION_FAILED", "Organization fields must be strings or null.")
          }
          patch[field] = body[field]
        }
      }
      const hash = await fingerprint({ method: req.method, path, organizationId, body })
      const rows = await database`
        select silomath.organization_patch_command(
          ${user.id}::uuid,${organizationId}::uuid,${JSON.stringify(patch)}::jsonb,
          ${version}::bigint,${key},${hash},${correlationId}
        ) as result
      `
      return json(correlationId, Number(rows[0].result.status), rows[0].result.body)
    }
    const clients = path.match(new RegExp(`^/organizations/(${UUID})/clients$`))
    const client = path.match(new RegExp(`^/organizations/(${UUID})/clients/(${UUID})$`))
    if ((clients || client) && ["GET", "POST", "PATCH"].includes(req.method)) {
      const organizationId = (clients || client)![1]
      enforceOrganizationContext(req, organizationId)
      if (req.method === "GET") {
        const access = await database`
          select silomath.location_actor_is_active_member(
            ${user.id}::uuid, ${organizationId}::uuid
          ) as allowed
        `
        if (!access[0]?.allowed) {
          throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The organization resource was not found.")
        }
        const q = pageQuery(req)
        if (q.status !== null && !["ACTIVE", "INACTIVE"].includes(q.status)) {
          throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid Client lifecycle filter.")
        }
        if (client) {
          const rows = await database`
            select silomath.client_to_json(c) as body from silomath.clients c
            where c.organization_id=${organizationId}::uuid
              and c.client_id=${client[2]}::uuid
          `
          if (!rows[0]) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The Client was not found.")
          return json(correlationId, 200, rows[0].body)
        }
        const rows = await database`
          select c.client_id,c.code,silomath.client_to_json(c) as body
          from silomath.clients c
          where c.organization_id=${organizationId}::uuid
            and (${q.status}::text is null or c.lifecycle_status=${q.status})
            and (${q.search}::text is null or c.name ilike '%' || ${q.search} || '%'
                 or c.code ilike '%' || ${q.search} || '%')
            and (${q.cursor.id}::uuid is null or (c.code,c.client_id)>
                 (${q.cursor.code},${q.cursor.id}::uuid))
          order by c.code,c.client_id limit ${q.limit + 1}
        `
        const visible = rows.slice(0, q.limit)
        const last = visible.at(-1)
        return json(correlationId, 200, {
          items: visible.map((row) => row.body),
          nextCursor: rows.length > q.limit && last
            ? encodeCursor(String(last.code), String(last.client_id)) : null,
          hasMore: rows.length > q.limit,
        })
      }
      if ((clients && req.method !== "POST") || (client && req.method !== "PATCH")) {
        throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The API route was not found.")
      }
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const key = mutationKey(req, body)
      const create = req.method === "POST"
      const clientId = create ? body.clientId : client![2]
      if (typeof clientId !== "string" || !new RegExp(`^${UUID}$`).test(clientId)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "A canonical Client UUID is required.")
      }
      const patch: JsonObject = {}
      const fields = ["name", "code", "registrationTaxId", "notes", "lifecycleStatus"]
      const controls = ["clientId", "clientOperationId", "idempotencyKey", "baseEntityVersion"]
      if (Object.keys(body).some((key) => !fields.includes(key) && !controls.includes(key))) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Unsupported Client field.")
      }
      for (const field of fields) {
        if (hasOwn(body, field)) {
          if (body[field] !== null && typeof body[field] !== "string") {
            throw new ApiProblem(422, "VALIDATION_FAILED", "Client fields must be strings or null.")
          }
          patch[field] = body[field]
        }
      }
      const version = create ? null : baseEntityVersion(body, req)
      const hash = await fingerprint({ method: req.method, path, organizationId, clientId, body })
      const rows = await database`
        select silomath.client_mutate_command(
          ${user.id}::uuid,${organizationId}::uuid,${clientId}::uuid,
          ${JSON.stringify(patch)}::jsonb,${version}::bigint,
          ${key},${hash},${correlationId},${create}
        ) as result
      `
      return json(correlationId, Number(rows[0].result.status), rows[0].result.body)
    }

    if (req.method === "GET" && path === "/me") {
      return json(
        correlationId,
        200,
        await currentUserResponse(user.id),
      )
    }

    if (organizationCollection && req.method === "GET") {
      const q = pageQuery(req)
      const rows = await db`
        select o.*
        from silomath.organizations o
        join silomath.organization_memberships m
          on m.organization_id = o.organization_id
        join silomath.users u on u.user_id = m.user_id
        where u.user_id = ${user.id}::uuid
          and u.lifecycle_status = 'ACTIVE'
          and m.lifecycle_status = 'ACTIVE'
          and o.lifecycle_status = 'ACTIVE'
          and (${q.search}::text is null
               or o.name ilike '%' || ${q.search} || '%'
               or o.code ilike '%' || ${q.search} || '%')
          and (
            ${q.cursor.id}::uuid is null
            or (lower(o.name), o.organization_id) >
               (${q.cursor.code}, ${q.cursor.id}::uuid)
          )
        order by lower(o.name), o.organization_id
        limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map(organizationDto),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(String(last.name).toLowerCase(), last.organization_id)
          : null,
        hasMore: rows.length > q.limit,
      })
    }

    if (organizationItem && req.method === "GET") {
      const organizationId = organizationItem[1]
      enforceOrganizationContext(req, organizationId)
      await requireOrganizationAccess(user.id, organizationId)

      const rows = await db`
        select *
        from silomath.organizations
        where organization_id = ${organizationId}::uuid
          and lifecycle_status = 'ACTIVE'
      `
      if (!rows[0]) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The organization was not found.")
      return json(correlationId, 200, organizationDto(rows[0]))
    }

    if (organizationUsers && req.method === "GET") {
      const organizationId = organizationUsers[1]
      enforceOrganizationContext(req, organizationId)
      await requireOrganizationAccess(user.id, organizationId, true)
      const q = pageQuery(req)
      const role = new URL(req.url).searchParams.get("role")?.trim().toUpperCase() || null

      if (q.status !== null && !["ACTIVE", "DISABLED"].includes(q.status)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid membership status filter.")
      }
      if (role !== null && !["ADMIN", "STANDARD"].includes(role)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid membership role filter.")
      }

      const rows = await db`
        select
          u.user_id, u.full_name, u.email,
          u.lifecycle_status as user_lifecycle_status,
          u.created_at as user_created_at,
          u.created_by as user_created_by,
          u.updated_at as user_updated_at,
          u.updated_by as user_updated_by,
          u.entity_version as user_entity_version,
          m.membership_id, m.organization_id, m.role,
          m.lifecycle_status as membership_lifecycle_status,
          m.disabled_at, m.disabled_by,
          m.created_at as membership_created_at,
          m.created_by as membership_created_by,
          m.updated_at as membership_updated_at,
          m.updated_by as membership_updated_by,
          m.entity_version as membership_entity_version
        from silomath.organization_memberships m
        join silomath.users u on u.user_id = m.user_id
        where m.organization_id = ${organizationId}::uuid
          and (${q.search}::text is null
               or u.full_name ilike '%' || ${q.search} || '%'
               or u.email ilike '%' || ${q.search} || '%')
          and (${q.status}::text is null or m.lifecycle_status = ${q.status})
          and (${role}::text is null or m.role = ${role})
          and (
            ${q.cursor.id}::uuid is null
            or (lower(u.full_name), m.membership_id) >
               (${q.cursor.code}, ${q.cursor.id}::uuid)
          )
        order by lower(u.full_name), m.membership_id
        limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map(userMembershipDto),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(String(last.full_name).toLowerCase(), last.membership_id)
          : null,
        hasMore: rows.length > q.limit,
      })
    }

    if (organizationUser && req.method === "GET") {
      const organizationId = organizationUser[1]
      const requestedUserId = organizationUser[2]
      enforceOrganizationContext(req, organizationId)
      await requireOrganizationAccess(user.id, organizationId, true)

      const rows = await db`
        select
          u.user_id, u.full_name, u.email,
          u.lifecycle_status as user_lifecycle_status,
          u.created_at as user_created_at,
          u.created_by as user_created_by,
          u.updated_at as user_updated_at,
          u.updated_by as user_updated_by,
          u.entity_version as user_entity_version,
          m.membership_id, m.organization_id, m.role,
          m.lifecycle_status as membership_lifecycle_status,
          m.disabled_at, m.disabled_by,
          m.created_at as membership_created_at,
          m.created_by as membership_created_by,
          m.updated_at as membership_updated_at,
          m.updated_by as membership_updated_by,
          m.entity_version as membership_entity_version
        from silomath.organization_memberships m
        join silomath.users u on u.user_id = m.user_id
        where m.organization_id = ${organizationId}::uuid
          and u.user_id = ${requestedUserId}::uuid
      `
      if (!rows[0]) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The user membership was not found.")
      return json(correlationId, 200, userMembershipDto(rows[0]))
    }

    if (membershipItem && req.method === "PATCH") {
      const organizationId = membershipItem[1]
      const membershipId = membershipItem[2]
      enforceOrganizationContext(req, organizationId)
      await requireOrganizationAccess(user.id, organizationId, true)
      const body = await bodyJson(req)
      const allowed = new Set([
        "role", "lifecycleStatus", "baseEntityVersion",
        "idempotencyKey", "clientOperationId", "occurredAtUtc",
      ])
      if (Object.keys(body).some((key) => !allowed.has(key))) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Membership patch contains unsupported fields.")
      }
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, membershipId, body,
      })

      const rows = await db`
        select silomath.organization_membership_patch_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${membershipId}::uuid,
          ${typeof body.role === "string" ? body.role : null},
          ${typeof body.lifecycleStatus === "string" ? body.lifecycleStatus : null},
          ${expectedVersion},
          ${idempotencyKey},
          ${requestFingerprint},
          ${correlationId}
        ) as result
      `
      const result = rows[0].result
      return json(correlationId, Number(result.status), result.body)
    }

    if (campaignCollection && req.method === "GET") {
      const organizationId = campaignCollection[1]
      enforceOrganizationContext(req, organizationId)
      await requireOrganizationAccess(user.id, organizationId)
      const q = pageQuery(req)
      const url = new URL(req.url)
      const type = url.searchParams.get("type")?.trim().toUpperCase() || null
      if (q.status !== null && !["ACTIVE", "COMPLETED"].includes(q.status)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid campaign status filter.")
      }
      if (type !== null && !["ANNUAL", "MONTHLY", "CONTROL"].includes(type)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid campaign type filter.")
      }

      const rows = await db`
        select *
        from silomath.inventory_campaigns
        where organization_id = ${organizationId}::uuid
          and (${q.search}::text is null
               or name ilike '%' || ${q.search} || '%'
               or code ilike '%' || ${q.search} || '%')
          and (${q.status}::text is null or status = ${q.status})
          and (${type}::text is null or inventory_type = ${type})
          and (
            ${q.cursor.id}::uuid is null
            or (code, campaign_id) > (${q.cursor.code}, ${q.cursor.id}::uuid)
          )
        order by code, campaign_id
        limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map(campaignSummaryDto),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(last.code, last.campaign_id)
          : null,
        hasMore: rows.length > q.limit,
      })
    }

    if (campaignItem && req.method === "GET") {
      const organizationId = campaignItem[1]
      const campaignId = campaignItem[2]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)
      const rows = await db`
        select *
        from silomath.inventory_campaigns
        where organization_id = ${organizationId}::uuid
          and campaign_id = ${campaignId}::uuid
      `
      if (!rows[0]) throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The campaign was not found.")
      return json(correlationId, 200, campaignDto(rows[0]))
    }

    if (campaignScope && req.method === "GET") {
      const organizationId = campaignScope[1]
      const campaignId = campaignScope[2]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)
      const q = pageQuery(req)
      const rows = await db`
        select
          cs.campaign_scope_id,
          cs.campaign_id,
          cs.storage_space_id,
          cssi.snapshot_scope_item_id,
          cs.created_at,
          cs.created_by
        from silomath.campaign_scopes cs
        join silomath.inventory_campaigns c on c.campaign_id = cs.campaign_id
        join silomath.campaign_snapshot_scope_items cssi
          on cssi.campaign_snapshot_id = c.campaign_snapshot_id
         and cssi.storage_space_id = cs.storage_space_id
        where c.organization_id = ${organizationId}::uuid
          and c.campaign_id = ${campaignId}::uuid
          and (
            ${q.cursor.id}::uuid is null
            or (cs.storage_space_id, cs.campaign_scope_id) >
               (${q.cursor.code}::uuid, ${q.cursor.id}::uuid)
          )
        order by cs.storage_space_id, cs.campaign_scope_id
        limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map((row: postgres.Row) => ({
          id: row.campaign_scope_id,
          campaignId: row.campaign_id,
          storageSpaceId: row.storage_space_id,
          snapshotHierarchyRef: row.snapshot_scope_item_id,
          createdAt: row.created_at,
          createdBy: row.created_by,
        })),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(last.storage_space_id, last.campaign_scope_id)
          : null,
        hasMore: rows.length > q.limit,
      })
    }

    if (campaignTeam && req.method === "GET") {
      const organizationId = campaignTeam[1]
      const campaignId = campaignTeam[2]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)
      const q = pageQuery(req)
      const rows = await db`
        select
          a.assignment_id, a.campaign_id, a.user_id,
          m.membership_id as organization_membership_id,
          a.status, a.assigned_at, a.assigned_by,
          a.removed_at, a.removed_by,
          a.created_at, a.created_by, a.updated_at, a.updated_by,
          a.entity_version
        from silomath.campaign_team_assignments a
        join silomath.inventory_campaigns c on c.campaign_id = a.campaign_id
        join silomath.organization_memberships m
          on m.organization_id = c.organization_id
         and m.user_id = a.user_id
        where c.organization_id = ${organizationId}::uuid
          and c.campaign_id = ${campaignId}::uuid
          and (
            ${q.cursor.id}::uuid is null
            or (a.assigned_at, a.assignment_id) >
               (${q.cursor.code}::timestamptz, ${q.cursor.id}::uuid)
          )
        order by a.assigned_at, a.assignment_id
        limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map((row: postgres.Row) => ({
          id: row.assignment_id,
          campaignId: row.campaign_id,
          userId: row.user_id,
          organizationMembershipId: row.organization_membership_id,
          assignmentStatus: row.status,
          assignedAt: row.assigned_at,
          assignedBy: row.assigned_by,
          removedAt: row.removed_at,
          removedBy: row.removed_by,
          createdAt: row.created_at,
          createdBy: row.created_by,
          updatedAt: row.updated_at,
          updatedBy: row.updated_by,
          entityVersion: Number(row.entity_version),
        })),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(new Date(last.assigned_at).toISOString(), last.assignment_id)
          : null,
        hasMore: rows.length > q.limit,
      })
    }

    if (offlineManifest && req.method === "GET") {
      const organizationId = offlineManifest[1]
      const campaignId = offlineManifest[2]
      enforceOrganizationContext(req, organizationId)
      const deviceInstallationId = new URL(req.url).searchParams.get("deviceInstallationId") ?? ""
      if (!new RegExp(`^${UUID}$`).test(deviceInstallationId)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "A valid deviceInstallationId is required.")
      }
      const rows = await db`
        select silomath.offline_manifest_get(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${deviceInstallationId}::uuid
        ) as result
      `
      const manifest = rows[0].result as JsonObject
      const packageRows = await db`
        select silomath.offline_package_get(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${deviceInstallationId}::uuid,
          ${manifest.packageVersion as string}
        ) as result
      `
      const packageResult = packageRows[0].result as JsonObject
      const content = String(packageResult.content)
      const compatibility = (manifest.contentManifest as JsonObject)?.compatibility as JsonObject | undefined
      return json(correlationId, 200, {
        ...manifest,
        verificationMetadata: {
          formatVersion: 2,
          authorizedUserId: user.id,
          byteSize: new TextEncoder().encode(content).byteLength,
          snapshot_schema_version: compatibility?.campaignSnapshotSchemaVersion ?? null,
          calculation_engine_version: compatibility?.calculationEngineVersion ?? null,
          calculation_schema_versions: compatibility?.calculationSchemaVersions ?? null,
        },
      })
    }

    if (offlinePackage && req.method === "GET") {
      const organizationId = offlinePackage[1]
      const campaignId = offlinePackage[2]
      enforceOrganizationContext(req, organizationId)
      const url = new URL(req.url)
      const deviceInstallationId = url.searchParams.get("deviceInstallationId") ?? ""
      const packageVersion = url.searchParams.get("packageVersion")?.trim() ?? ""
      const manifestId = url.searchParams.get("manifestId") ?? ""
      if (!new RegExp(`^${UUID}$`).test(deviceInstallationId) ||
          !new RegExp(`^${UUID}$`).test(manifestId) ||
          !packageVersion) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Valid package identity parameters are required.")
      }
      const rows = await db`
        select silomath.offline_package_get(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${deviceInstallationId}::uuid,
          ${packageVersion}
        ) as result
      `
      const result = rows[0].result as JsonObject
      if (String(result.manifestId).toLowerCase() !== manifestId.toLowerCase()) {
        throw new ApiProblem(409, "CONFLICT", "The requested manifest does not match the current package.")
      }
      return new Response(String(result.content), {
        status: 200,
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          "X-Correlation-Id": correlationId,
          "X-Manifest-Id": String(result.manifestId),
          "X-Package-Version": String(result.packageVersion),
          "X-Package-SHA256": String(result.integrityHash),
        },
      })
    }

    if (campaignLaunch && req.method === "POST") {
      const organizationId = campaignLaunch[1]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      const idempotencyKey = mutationKey(req, body)
      const name = typeof body.name === "string" ? body.name.trim() : ""
      const inventoryType = body.inventoryType
      const validDate = (value: unknown): value is string =>
        typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value) &&
        Number.isFinite(Date.parse(value)) &&
        new Date(value).toISOString().slice(0, 10) === value
      const validUuid = (value: unknown): value is string =>
        typeof value === "string" && new RegExp(`^${UUID}$`).test(value)
      const scope = body.scopeStorageSpaceIds
      const team = body.team
      if (!name || name.length > 150 ||
          !["ANNUAL", "MONTHLY", "CONTROL"].includes(String(inventoryType)) ||
          !validDate(body.startDate) || !validDate(body.plannedEndDate) ||
          body.plannedEndDate < body.startDate ||
          !Array.isArray(scope) || scope.length === 0 || !scope.every(validUuid) ||
          !Array.isArray(team) || team.length === 0 ||
          !team.every((member) => member !== null && typeof member === "object" &&
            !Array.isArray(member) && validUuid(member.userId))) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "A valid Campaign name, inventory type, dates, scope and team are required.")
      }
      const teamUserIds = team.map((member) => member.userId as string)
      if (new Set(scope.map((id) => id.toLowerCase())).size !== scope.length ||
          new Set(teamUserIds.map((id) => id.toLowerCase())).size !== teamUserIds.length) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Campaign scope and team must not contain duplicates.")
      }
      const snapshotSchemaVersion = configuration("SILOMATH_CAMPAIGN_SNAPSHOT_SCHEMA_VERSION")?.trim()
      const calculationEngineVersion = configuration("SILOMATH_CALCULATION_ENGINE_VERSION")?.trim()
      if (!snapshotSchemaVersion || !calculationEngineVersion) {
        throw new ApiProblem(503, "SERVICE_UNAVAILABLE", "Campaign compatibility configuration is unavailable.", [], true)
      }
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, name, inventoryType,
        startDate: body.startDate, plannedEndDate: body.plannedEndDate,
        scopeStorageSpaceIds: scope, teamUserIds,
      })
      const rows = await database`
        select silomath.campaign_save_launch_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${idempotencyKey},
          ${requestFingerprint},
          ${correlationId},
          ${name},
          ${inventoryType as string},
          ${body.startDate}::date,
          ${body.plannedEndDate}::date,
          ${scope}::uuid[],
          ${teamUserIds}::uuid[],
          ${snapshotSchemaVersion},
          ${calculationEngineVersion}
        ) as result
      `
      const result = rows[0].result as { status: number; body: unknown }
      return json(correlationId, result.status, result.body)
    }

    if (storageSpaceBatch && req.method === "POST") {
      const organizationId = storageSpaceBatch[1]
      const storageUnitId = storageSpaceBatch[2]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      if (!Array.isArray(body.items) || body.items.length === 0) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "items must contain at least one Storage Space.")
      }
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, storageUnitId, items: body.items,
      })
      const rows = await db`
        select silomath.storage_space_batch_create_items_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${storageUnitId}::uuid,
          ${idempotencyKey},
          ${requestFingerprint},
          ${JSON.stringify(body.items)}::jsonb,
          ${correlationId}
        ) as result
      `
      const result = rows[0].result as JsonObject
      const items = Array.isArray(result.items) ? result.items : []
      return json(correlationId, 201, {
        items: await Promise.all(items.map(storageSpaceDto)),
      })
    }

    if (req.method === "POST" && path === "/sync/batch") {
      const body = await bodyJson(req)
      const deviceInstallationId =
        typeof body.deviceInstallationId === "string" ? body.deviceInstallationId : ""
      const clientVersion =
        typeof body.clientVersion === "string" ? body.clientVersion.trim() : ""
      const operations = body.operations

      if (!new RegExp(`^${UUID}$`).test(deviceInstallationId) ||
          !clientVersion ||
          !Array.isArray(operations)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "A valid deviceInstallationId, clientVersion and operations array are required.")
      }

      const rows = await db`
        select silomath.sync_batch_dispatch(
          ${user.id}::uuid,
          ${deviceInstallationId}::uuid,
          ${clientVersion},
          ${typeof body.checkpointContext === "string" ? body.checkpointContext : null},
          ${JSON.stringify(operations)}::jsonb,
          ${correlationId}
        ) as result
      `
      return json(correlationId, 200, rows[0].result)
    }

    if (req.method === "GET" && path === "/sync/status") {
      const url = new URL(req.url)
      const organizationId = url.searchParams.get("organizationId") ?? ""
      const campaignId = url.searchParams.get("campaignId")

      if (!new RegExp(`^${UUID}$`).test(organizationId) ||
          (campaignId !== null && !new RegExp(`^${UUID}$`).test(campaignId))) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Valid organizationId and optional campaignId are required.")
      }
      enforceOrganizationContext(req, organizationId)

      const rows = await db`
        select silomath.sync_server_status(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid
        ) as result
      `
      return json(correlationId, 200, rows[0].result)
    }

    if (req.method === "GET" && path === "/sync/pull") {
      const url = new URL(req.url)
      const organizationId = url.searchParams.get("organizationId") ?? ""
      const campaignId = url.searchParams.get("campaignId")
      const checkpointText = url.searchParams.get("checkpoint")
      const rawLimit = url.searchParams.get("limit")
      const limit = rawLimit === null ? 200 : Number(rawLimit)

      if (!new RegExp(`^${UUID}$`).test(organizationId) ||
          (campaignId !== null && !new RegExp(`^${UUID}$`).test(campaignId)) ||
          (checkpointText !== null && !/^[0-9]+$/.test(checkpointText)) ||
          !Number.isInteger(limit) || limit < 1 || limit > 500) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid sync pull query.")
      }
      enforceOrganizationContext(req, organizationId)

      const rows = await db`
        select silomath.sync_pull_page(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${checkpointText === null ? null : Number(checkpointText)}::bigint,
          ${limit}
        ) as result
      `
      return json(correlationId, 200, rows[0].result)
    }

    if (locationCollection) {
      const organizationId = locationCollection[1]
      enforceOrganizationContext(req, organizationId)

      if (req.method === "GET") {
        const q = pageQuery(req)

        const rows = await db`
          select *
          from silomath.location_list_page(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${q.search},
            ${q.status},
            ${q.cursor.code},
            ${q.cursor.id}::uuid,
            ${q.limit}
          )
        `

        const visible = rows.slice(0, q.limit)
        const hasMore = rows.length > q.limit
        const last = visible.at(-1)

        return json(correlationId, 200, {
          items: visible.map(locationDto),
          nextCursor: hasMore && last
            ? encodeCursor(last.code, last.location_id)
            : null,
          hasMore,
        })
      }

      if (req.method === "POST") {
        const body = await bodyJson(req)
        requireClientOperationId(body)
        const idempotencyKey = mutationKey(req, body)

        const requestFingerprint = await fingerprint({
          method: req.method,
          path,
          organizationId,
          body,
        })

        const rows = await db`
          select silomath.location_create_command(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${idempotencyKey},
            ${requestFingerprint},
            ${body.locationId as string}::uuid,
            ${body.name as string},
            ${body.code as string},
            ${body.address as string | null},
            ${body.city as string | null},
            ${body.country as string | null},
            ${body.notes as string | null},
            ${body.latitude as number | null},
            ${body.longitude as number | null},
            ${(body.lifecycleStatus as string) ?? "ACTIVE"}
          ) as result
        `

        const result = rows[0].result
        return json(
          correlationId,
          Number(result.status),
          locationDto(result.body),
        )
      }
    }

    if (locationItem) {
      const organizationId = locationItem[1]
      const locationId = locationItem[2]
      enforceOrganizationContext(req, organizationId)

      const currentRows = await db`
        select (silomath.location_get(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${locationId}::uuid
        )).*
      `
      const current = currentRows[0]

      if (req.method === "GET") {
        return json(correlationId, 200, locationDto(current))
      }

      if (req.method === "PATCH") {
        const body = await bodyJson(req)
        requireClientOperationId(body)
        const idempotencyKey = mutationKey(req, body)
        const expectedVersion = baseEntityVersion(body, req)

        if (hasOwn(body, "locationId") || hasOwn(body, "organizationId")) {
          throw new ApiProblem(
            422,
            "VALIDATION_FAILED",
            "Immutable identity fields cannot be patched.",
          )
        }

        const latitudeSupplied = hasOwn(body, "latitude")
        const longitudeSupplied = hasOwn(body, "longitude")

        if (latitudeSupplied !== longitudeSupplied) {
          throw new ApiProblem(
            422,
            "VALIDATION_FAILED",
            "latitude and longitude must be supplied together.",
          )
        }

        const requestFingerprint = await fingerprint({
          method: req.method,
          path,
          organizationId,
          body,
        })

        const rows = await db`
          select silomath.location_patch_command(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${idempotencyKey},
            ${requestFingerprint},
            ${locationId}::uuid,
            ${expectedVersion},
            ${hasOwn(body, "name") ? body.name : current.name},
            ${hasOwn(body, "code") ? body.code : current.code},
            ${hasOwn(body, "address") ? body.address : current.address},
            ${hasOwn(body, "city") ? body.city : current.city},
            ${hasOwn(body, "country") ? body.country : current.country},
            ${hasOwn(body, "notes") ? body.notes : current.notes},
            ${latitudeSupplied ? body.latitude : current.latitude_deg},
            ${longitudeSupplied ? body.longitude : current.longitude_deg},
            ${hasOwn(body, "lifecycleStatus")
              ? body.lifecycleStatus
              : current.lifecycle_status}
          ) as result
        `

        const result = rows[0].result
        return json(
          correlationId,
          Number(result.status),
          locationDto(result.body),
        )
      }
    }

    if (storageUnitCollection) {
      const organizationId = storageUnitCollection[1]
      const locationId = storageUnitCollection[2]
      enforceOrganizationContext(req, organizationId)

      if (req.method === "GET") {
        const q = pageQuery(req)

        const rows = await db`
          select *
          from silomath.storage_unit_list_page(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${locationId}::uuid,
            ${q.search},
            ${q.status},
            ${q.cursor.code},
            ${q.cursor.id}::uuid,
            ${q.limit}
          )
        `

        const visible = rows.slice(0, q.limit)
        const hasMore = rows.length > q.limit
        const last = visible.at(-1)

        return json(correlationId, 200, {
          items: visible.map(storageUnitDto),
          nextCursor: hasMore && last
            ? encodeCursor(last.code, last.storage_unit_id)
            : null,
          hasMore,
        })
      }

      if (req.method === "POST") {
        const body = await bodyJson(req)
        requireClientOperationId(body)
        const idempotencyKey = mutationKey(req, body)

        if (
          hasOwn(body, "locationId") &&
          String(body.locationId).toLowerCase() !== locationId.toLowerCase()
        ) {
          throw new ApiProblem(
            403,
            "TENANT_SCOPE_VIOLATION",
            "Storage Unit parent does not match the requested Location.",
          )
        }

        const requestFingerprint = await fingerprint({
          method: req.method,
          path,
          organizationId,
          body,
        })

        const rows = await db`
          select silomath.storage_unit_create_command(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${idempotencyKey},
            ${requestFingerprint},
            ${body.storageUnitId as string}::uuid,
            ${locationId}::uuid,
            ${body.name as string},
            ${body.code as string},
            ${body.description as string | null},
            ${(body.lifecycleStatus as string) ?? "ACTIVE"}
          ) as result
        `

        const result = rows[0].result
        return json(
          correlationId,
          Number(result.status),
          storageUnitDto(result.body),
        )
      }
    }

    if (storageUnitItem) {
      const organizationId = storageUnitItem[1]
      const storageUnitId = storageUnitItem[2]
      enforceOrganizationContext(req, organizationId)

      const currentRows = await db`
        select (silomath.storage_unit_get(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${storageUnitId}::uuid
        )).*
      `
      const current = currentRows[0]

      if (req.method === "GET") {
        return json(correlationId, 200, storageUnitDto(current))
      }

      if (req.method === "PATCH") {
        const body = await bodyJson(req)
        requireClientOperationId(body)
        const idempotencyKey = mutationKey(req, body)
        const expectedVersion = baseEntityVersion(body, req)

        if (
          hasOwn(body, "storageUnitId") ||
          hasOwn(body, "organizationId") ||
          hasOwn(body, "locationId")
        ) {
          throw new ApiProblem(
            422,
            "VALIDATION_FAILED",
            "Storage Unit identity and parent Location are immutable.",
          )
        }

        const requestFingerprint = await fingerprint({
          method: req.method,
          path,
          organizationId,
          body,
        })

        const rows = await db`
          select silomath.storage_unit_patch_command(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${idempotencyKey},
            ${requestFingerprint},
            ${storageUnitId}::uuid,
            ${expectedVersion},
            ${current.location_id}::uuid,
            ${hasOwn(body, "name") ? body.name : current.name},
            ${hasOwn(body, "code") ? body.code : current.code},
            ${hasOwn(body, "description")
              ? body.description
              : current.description},
            ${hasOwn(body, "lifecycleStatus")
              ? body.lifecycleStatus
              : current.lifecycle_status}
          ) as result
        `

        const result = rows[0].result
        return json(
          correlationId,
          Number(result.status),
          storageUnitDto(result.body),
        )
      }
    }

    if (storageSpaceCollection) {
      const organizationId = storageSpaceCollection[1]
      const storageUnitId = storageSpaceCollection[2]
      enforceOrganizationContext(req, organizationId)

      if (req.method === "GET") {
        const q = pageQuery(req)

        if (q.search) {
          throw new ApiProblem(
            422,
            "VALIDATION_FAILED",
            "Storage Space search fields are not explicitly frozen in the current baseline.",
            [],
            false,
            { baselineGap: "StorageSpace CursorQuery declares search but does not define searchable fields." },
          )
        }

        const rows = await db`
          select *
          from silomath.storage_space_list_page(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${storageUnitId}::uuid,
            ${q.status},
            ${q.cursor.code},
            ${q.cursor.id}::uuid,
            ${q.limit}
          )
        `

        const visible = rows.slice(0, q.limit)
        const hasMore = rows.length > q.limit
        const last = visible.at(-1)

        return json(correlationId, 200, {
          items: await Promise.all(visible.map(storageSpaceDto)),
          nextCursor: hasMore && last
            ? encodeCursor(last.code, last.storage_space_id)
            : null,
          hasMore,
        })
      }

      if (req.method === "POST") {
        const body = await bodyJson(req)
        requireClientOperationId(body)
        const idempotencyKey = mutationKey(req, body)

        if (
          hasOwn(body, "storageUnitId") &&
          String(body.storageUnitId).toLowerCase() !== storageUnitId.toLowerCase()
        ) {
          throw new ApiProblem(
            403,
            "TENANT_SCOPE_VIOLATION",
            "Storage Space parent does not match the requested Storage Unit.",
          )
        }

        const requestFingerprint = await fingerprint({
          method: req.method,
          path,
          organizationId,
          body,
        })

        const rows = await db`
          select silomath.storage_space_create_command(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${idempotencyKey},
            ${requestFingerprint},
            ${body.storageSpaceId as string}::uuid,
            ${storageUnitId}::uuid,
            ${body.code as string},
            ${body.description as string | null},
            ${body.geometryProfileId as string}::uuid,
            ${(body.lifecycleStatus as string) ?? "ACTIVE"}
          ) as result
        `

        const result = rows[0].result
        return json(
          correlationId,
          Number(result.status),
          await storageSpaceDto(result.body),
        )
      }
    }

    if (storageSpaceItem) {
      const organizationId = storageSpaceItem[1]
      const storageSpaceId = storageSpaceItem[2]
      enforceOrganizationContext(req, organizationId)

      const currentRows = await db`
        select (silomath.storage_space_get(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${storageSpaceId}::uuid
        )).*
      `
      const current = currentRows[0]

      if (req.method === "GET") {
        return json(
          correlationId,
          200,
          await storageSpaceDto(current),
        )
      }

      if (req.method === "PATCH") {
        const body = await bodyJson(req)
        requireClientOperationId(body)
        const idempotencyKey = mutationKey(req, body)
        const expectedVersion = baseEntityVersion(body, req)

        if (
          hasOwn(body, "storageSpaceId") ||
          hasOwn(body, "organizationId") ||
          hasOwn(body, "storageUnitId")
        ) {
          throw new ApiProblem(
            422,
            "VALIDATION_FAILED",
            "Storage Space identity and parent Storage Unit are immutable.",
          )
        }

        const requestFingerprint = await fingerprint({
          method: req.method,
          path,
          organizationId,
          body,
        })

        const rows = await db`
          select silomath.storage_space_patch_command(
            ${user.id}::uuid,
            ${organizationId}::uuid,
            ${idempotencyKey},
            ${requestFingerprint},
            ${storageSpaceId}::uuid,
            ${expectedVersion},
            ${current.storage_unit_id}::uuid,
            ${hasOwn(body, "code") ? body.code : current.code},
            ${hasOwn(body, "description")
              ? body.description
              : current.description},
            ${hasOwn(body, "geometryProfileId")
              ? body.geometryProfileId
              : current.geometry_profile_id}::uuid,
            ${hasOwn(body, "lifecycleStatus")
              ? body.lifecycleStatus
              : current.lifecycle_status}
          ) as result
        `

        const result = rows[0].result
        return json(
          correlationId,
          Number(result.status),
          await storageSpaceDto(result.body),
        )
      }
    }

    if (stocktakingCollection && req.method === "GET") {
      const organizationId = stocktakingCollection[1]
      const campaignId = stocktakingCollection[2]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)

      const q = pageQuery(req)
      const url = new URL(req.url)
      const locationId = url.searchParams.get("locationId")
      const storageUnitId = url.searchParams.get("storageUnitId")

      for (const [field, value] of [
        ["locationId", locationId],
        ["storageUnitId", storageUnitId],
      ] as const) {
        if (value && !new RegExp(`^${UUID}$`).test(value)) {
          throw new ApiProblem(
            422,
            "VALIDATION_FAILED",
            `${field} must be a UUID.`,
          )
        }
      }

      if (q.status && !["WORKING", "VALIDATED"].includes(q.status)) {
        throw new ApiProblem(
          422,
          "VALIDATION_FAILED",
          "status must be WORKING or VALIDATED.",
        )
      }

      const rows = await db`
        select
          sr.stocktaking_record_id,
          sr.organization_id,
          sr.campaign_id,
          sr.storage_space_id,
          sr.current_revision_id,
          sr.created_at as record_created_at,
          sr.created_by as record_created_by,
          sr.updated_at as record_updated_at,
          sr.updated_by as record_updated_by,
          sr.entity_version as record_entity_version,
          rev.*,
          ss.code as storage_space_code,
          su.storage_unit_id,
          su.code as storage_unit_code,
          l.location_id,
          l.code as location_code,
          (select count(*) from silomath.issues i
            where i.stocktaking_record_id=sr.stocktaking_record_id
              and i.status='OPEN') as open_issue_count,
          (select count(*) from silomath.issues i
            where i.stocktaking_record_id=sr.stocktaking_record_id
              and i.status='RESOLVED') as resolved_issue_count,
          jsonb_build_object(
            'photoCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=sr.stocktaking_record_id
                and e.issue_id is null and e.evidence_type='PHOTO'
                and e.removed_at is null),
            'noteCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=sr.stocktaking_record_id
                and e.issue_id is null and e.evidence_type='NOTE'
                and e.removed_at is null)
          ) as record_evidence_summary
        from silomath.stocktaking_records sr
        join silomath.stocktaking_revisions rev
          on rev.stocktaking_revision_id=sr.current_revision_id
         and rev.stocktaking_record_id=sr.stocktaking_record_id
        join silomath.storage_spaces ss
          on ss.storage_space_id=sr.storage_space_id
         and ss.organization_id=sr.organization_id
        join silomath.storage_units su
          on su.storage_unit_id=ss.storage_unit_id
         and su.organization_id=sr.organization_id
        join silomath.locations l
          on l.location_id=su.location_id
         and l.organization_id=sr.organization_id
        where sr.organization_id=${organizationId}::uuid
          and sr.campaign_id=${campaignId}::uuid
          and (${locationId}::uuid is null or l.location_id=${locationId}::uuid)
          and (${storageUnitId}::uuid is null or su.storage_unit_id=${storageUnitId}::uuid)
          and (${q.status}::text is null or rev.revision_state=${q.status})
          and (
            ${q.cursor.code}::text is null
            or (ss.code, sr.stocktaking_record_id) >
               (${q.cursor.code}, ${q.cursor.id}::uuid)
          )
        order by ss.code, sr.stocktaking_record_id
        limit ${q.limit + 1}
      `

      const visible = rows.slice(0, q.limit)
      const hasMore = rows.length > q.limit
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map(stocktakingRecordDto),
        nextCursor: hasMore && last
          ? encodeCursor(last.storage_space_code, last.stocktaking_record_id)
          : null,
        hasMore,
      })
    }

    if (stocktakingItem && req.method === "GET") {
      const organizationId = stocktakingItem[1]
      const campaignId = stocktakingItem[2]
      const stocktakingRecordId = stocktakingItem[3]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)

      const rows = await db`
        select
          sr.stocktaking_record_id,
          sr.organization_id,
          sr.campaign_id,
          sr.storage_space_id,
          sr.current_revision_id,
          sr.created_at as record_created_at,
          sr.created_by as record_created_by,
          sr.updated_at as record_updated_at,
          sr.updated_by as record_updated_by,
          sr.entity_version as record_entity_version,
          rev.*,
          ss.code as storage_space_code,
          su.storage_unit_id,
          su.code as storage_unit_code,
          l.location_id,
          l.code as location_code,
          coalesce((select jsonb_agg(to_jsonb(m) order by m.sequence_number)
            from silomath.measurements m
            where m.stocktaking_revision_id=rev.stocktaking_revision_id),'[]'::jsonb)
            as measurements,
          (select to_jsonb(cr) from silomath.calculation_results cr
            where cr.calculation_result_id=rev.calculation_result_id
              and cr.stocktaking_revision_id=rev.stocktaking_revision_id)
            as calculation_result,
          jsonb_build_object(
            'photoCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=sr.stocktaking_record_id
                and e.issue_id is null and e.evidence_type='PHOTO'
                and e.removed_at is null),
            'noteCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=sr.stocktaking_record_id
                and e.issue_id is null and e.evidence_type='NOTE'
                and e.removed_at is null)
          ) as evidence_summary,
          (select count(*) from silomath.issues i
            where i.stocktaking_record_id=sr.stocktaking_record_id
              and i.status='OPEN') as open_issue_count,
          (select count(*) from silomath.issues i
            where i.stocktaking_record_id=sr.stocktaking_record_id
              and i.status='RESOLVED') as resolved_issue_count,
          jsonb_build_object(
            'photoCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=sr.stocktaking_record_id
                and e.issue_id is null and e.evidence_type='PHOTO'
                and e.removed_at is null),
            'noteCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=sr.stocktaking_record_id
                and e.issue_id is null and e.evidence_type='NOTE'
                and e.removed_at is null)
          ) as record_evidence_summary
        from silomath.stocktaking_records sr
        join silomath.stocktaking_revisions rev
          on rev.stocktaking_revision_id=sr.current_revision_id
         and rev.stocktaking_record_id=sr.stocktaking_record_id
        join silomath.storage_spaces ss
          on ss.storage_space_id=sr.storage_space_id
        join silomath.storage_units su on su.storage_unit_id=ss.storage_unit_id
        join silomath.locations l on l.location_id=su.location_id
        where sr.stocktaking_record_id=${stocktakingRecordId}::uuid
          and sr.organization_id=${organizationId}::uuid
          and sr.campaign_id=${campaignId}::uuid
      `

      if (!rows[0]) {
        throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The stocktaking record was not found.")
      }
      return json(correlationId, 200, stocktakingRecordDto(rows[0]))
    }

    if (stocktakingRevisions && req.method === "GET") {
      const organizationId = stocktakingRevisions[1]
      const campaignId = stocktakingRevisions[2]
      const stocktakingRecordId = stocktakingRevisions[3]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)
      const q = pageQuery(req)
      const revisionCursor = q.cursor.code === null ? null : Number(q.cursor.code)
      if (revisionCursor !== null && !Number.isSafeInteger(revisionCursor)) {
        throw new ApiProblem(400, "VALIDATION_FAILED", "cursor is malformed.")
      }

      const rows = await db`
        select
          rev.*,
          coalesce((select jsonb_agg(to_jsonb(m) order by m.sequence_number)
            from silomath.measurements m
            where m.stocktaking_revision_id=rev.stocktaking_revision_id),'[]'::jsonb)
            as measurements,
          (select to_jsonb(cr) from silomath.calculation_results cr
            where cr.calculation_result_id=rev.calculation_result_id
              and cr.stocktaking_revision_id=rev.stocktaking_revision_id)
            as calculation_result,
          jsonb_build_object(
            'photoCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=rev.stocktaking_record_id
                and e.stocktaking_revision_id=rev.stocktaking_revision_id
                and e.evidence_type='PHOTO' and e.removed_at is null),
            'noteCount', (select count(*) from silomath.evidence e
              where e.stocktaking_record_id=rev.stocktaking_record_id
                and e.stocktaking_revision_id=rev.stocktaking_revision_id
                and e.evidence_type='NOTE' and e.removed_at is null)
          ) as evidence_summary
        from silomath.stocktaking_revisions rev
        join silomath.stocktaking_records sr
          on sr.stocktaking_record_id=rev.stocktaking_record_id
        where rev.stocktaking_record_id=${stocktakingRecordId}::uuid
          and sr.organization_id=${organizationId}::uuid
          and sr.campaign_id=${campaignId}::uuid
          and (
            ${revisionCursor}::integer is null
            or (rev.revision_number, rev.stocktaking_revision_id) <
               (${revisionCursor}::integer, ${q.cursor.id}::uuid)
          )
        order by rev.revision_number desc, rev.stocktaking_revision_id desc
        limit ${q.limit + 1}
      `

      const visible = rows.slice(0, q.limit)
      const hasMore = rows.length > q.limit
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map(stocktakingRevisionDto),
        nextCursor: hasMore && last
          ? encodeCursor(String(last.revision_number), last.stocktaking_revision_id)
          : null,
        hasMore,
      })
    }

    if (issueCollection && req.method === "GET") {
      const organizationId = issueCollection[1]
      const campaignId = issueCollection[2]
      const stocktakingRecordId = issueCollection[3]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId)
      const q = pageQuery(req)
      if (q.status !== null && !["OPEN", "RESOLVED"].includes(q.status)) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Invalid Issue status filter.")
      }
      const rows = await database`
        select *
        from silomath.issues
        where organization_id = ${organizationId}::uuid
          and campaign_id = ${campaignId}::uuid
          and stocktaking_record_id = ${stocktakingRecordId}::uuid
          and (${q.status}::text is null or status = ${q.status})
          and (
            ${q.cursor.id}::uuid is null
            or (reported_at, issue_id) < (${q.cursor.code}::timestamptz, ${q.cursor.id}::uuid)
          )
        order by reported_at desc, issue_id desc
        limit ${q.limit + 1}
      `
      const visible = rows.slice(0, q.limit)
      const last = visible.at(-1)
      return json(correlationId, 200, {
        items: visible.map((row: postgres.Row) => ({
          issueId: row.issue_id,
          organizationId: row.organization_id,
          campaignId: row.campaign_id,
          stocktakingRecordId: row.stocktaking_record_id,
          category: row.category,
          description: row.description,
          status: row.status,
          reportedAt: row.reported_at,
          reportedBy: row.reported_by,
          resolvedAt: row.resolved_at,
          resolvedBy: row.resolved_by,
          entityVersion: Number(row.entity_version),
        })),
        nextCursor: rows.length > q.limit && last
          ? encodeCursor(new Date(last.reported_at).toISOString(), last.issue_id)
          : null,
        hasMore: rows.length > q.limit,
      })
    }

    if (issueCollection && req.method === "POST") {
      const organizationId = issueCollection[1]
      const campaignId = issueCollection[2]
      const stocktakingRecordId = issueCollection[3]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const issueId = typeof body.issueId === "string" ? body.issueId : ""
      const category = typeof body.category === "string" ? body.category : ""
      const description = typeof body.description === "string" ? body.description.trim() : ""
      if (!new RegExp(`^${UUID}$`).test(issueId) ||
          ![
            "MEASUREMENT_PROBLEM", "ACCESS_SAFETY", "GEOMETRY_DISCREPANCY",
            "COMMODITY_UNCERTAINTY", "STORAGE_SPACE_CONDITION", "OTHER",
          ].includes(category) || !description) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Issue Type and Description are required.")
      }
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, campaignId,
        stocktakingRecordId, issueId, category, description,
      })
      const rows = await database`
        select silomath.issue_create_command(
          ${user.id}::uuid, ${organizationId}::uuid, ${campaignId}::uuid,
          ${stocktakingRecordId}::uuid, ${issueId}::uuid,
          ${category}, ${description}, ${idempotencyKey},
          ${requestFingerprint}, ${correlationId}
        ) as result
      `
      const result = rows[0].result
      return json(correlationId, Number(result.status), result.body)
    }

    const issueTransition = issueResolve || issueReopen || issueAcceptFollowUp
    if (issueTransition && req.method === "POST") {
      const organizationId = issueTransition[1]
      const campaignId = issueTransition[2]
      const issueId = issueTransition[3]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)
      const reason = typeof body.reason === "string" ? body.reason.trim() : ""
      if (!reason) {
        throw new ApiProblem(
          422, "VALIDATION_FAILED", "Issue transition reason is required.",
          [{ field: "reason", code: "REQUIRED", message: "Required" }],
        )
      }
      const targetStatus = issueReopen ? "OPEN" : "RESOLVED"
      const resolutionContext = issueAcceptFollowUp
        ? "CLOSE_FOLLOW_UP_ACCEPTANCE"
        : issueResolve ? "NORMAL" : null
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, campaignId, issueId,
        targetStatus, resolutionContext, expectedVersion, reason,
      })
      const rows = await database`
        select silomath.issue_transition_command(
          ${user.id}::uuid, ${organizationId}::uuid, ${campaignId}::uuid,
          ${issueId}::uuid, ${targetStatus}, ${reason},
          ${resolutionContext}, ${expectedVersion}::bigint,
          ${idempotencyKey}, ${requestFingerprint}, ${correlationId}
        ) as result
      `
      const result = rows[0].result
      return json(correlationId, Number(result.status), result.body)
    }

    if (evidenceCollection && req.method === "POST") {
      const organizationId = evidenceCollection[1]
      const campaignId = evidenceCollection[2]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const evidenceId = typeof body.evidenceId === "string" ? body.evidenceId : ""
      const evidenceType = typeof body.evidenceType === "string" ? body.evidenceType : ""
      const stocktakingRecordId = typeof body.stocktakingRecordId === "string"
        ? body.stocktakingRecordId : null
      const issueId = typeof body.issueId === "string" ? body.issueId : null
      const revisionId = typeof body.stocktakingRevisionId === "string"
        ? body.stocktakingRevisionId : null
      const validUuid = (value: string | null) =>
        value === null || new RegExp(`^${UUID}$`).test(value)
      if (!new RegExp(`^${UUID}$`).test(evidenceId) ||
          !["PHOTO", "NOTE"].includes(evidenceType) ||
          !validUuid(stocktakingRecordId) || !validUuid(issueId) ||
          !validUuid(revisionId) ||
          ((stocktakingRecordId !== null) === (issueId !== null))) {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Evidence owner and type are invalid.")
      }
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, campaignId, body,
      })
      const rows = await database`
        select silomath.evidence_create_command(
          ${user.id}::uuid, ${organizationId}::uuid, ${campaignId}::uuid,
          ${evidenceId}::uuid, ${stocktakingRecordId}::uuid,
          ${issueId}::uuid, ${revisionId}::uuid, ${evidenceType},
          ${typeof body.noteText === "string" ? body.noteText : null},
          ${typeof body.localPrivateRef === "string" ? body.localPrivateRef : null},
          ${typeof body.sha256 === "string" ? body.sha256 : null},
          ${typeof body.mimeType === "string" ? body.mimeType : null},
          ${body.byteSize === null || body.byteSize === undefined ? null : Number(body.byteSize)}::bigint,
          ${body.pixelWidth === null || body.pixelWidth === undefined ? null : Number(body.pixelWidth)}::integer,
          ${body.pixelHeight === null || body.pixelHeight === undefined ? null : Number(body.pixelHeight)}::integer,
          ${idempotencyKey}, ${requestFingerprint}, ${correlationId}
        ) as result
      `
      const result = rows[0].result
      return json(correlationId, Number(result.status), result.body)
    }

    if (evidenceUpload && req.method === "POST") {
      const organizationId = evidenceUpload[1]
      const campaignId = evidenceUpload[2]
      const evidenceId = evidenceUpload[3]
      enforceOrganizationContext(req, organizationId)
      const idempotencyKey = binaryMutationKey(req)
      const contentType = header(req, "Content-Type")?.split(";")[0].trim().toLowerCase()
      if (contentType !== "image/jpeg") {
        throw new ApiProblem(422, "VALIDATION_FAILED", "Evidence PHOTO content must be image/jpeg.")
      }
      const contentLength = Number(header(req, "Content-Length") ?? "0")
      if (Number.isFinite(contentLength) && contentLength > 3_000_000) {
        throw new ApiProblem(413, "EVIDENCE_TOO_LARGE", "Evidence PHOTO exceeds 3 MB.")
      }
      const bytes = new Uint8Array(await req.arrayBuffer())
      if (bytes.length < 1 || bytes.length > 3_000_000) {
        throw new ApiProblem(413, "EVIDENCE_TOO_LARGE", "Evidence PHOTO exceeds 3 MB.")
      }
      const actualHash = await sha256Hex(bytes)
      const metadata = await database`
        select sha256, byte_size
        from silomath.evidence
        where evidence_id = ${evidenceId}::uuid
          and organization_id = ${organizationId}::uuid
          and campaign_id = ${campaignId}::uuid
          and evidence_type = 'PHOTO'
          and removed_at is null
      `
      if (!metadata[0]) {
        throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The Evidence metadata was not found.")
      }
      if (metadata[0].sha256 !== actualHash ||
          Number(metadata[0].byte_size) !== bytes.length) {
        throw new ApiProblem(422, "EVIDENCE_HASH_MISMATCH", "The uploaded bytes do not match the declared Evidence metadata.")
      }
      const objectRef = `${organizationId}/${campaignId}/${evidenceId}/${actualHash}.jpg`
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, campaignId,
        evidenceId, sha256: actualHash, byteSize: bytes.length,
      })
      await objectStore.upload(objectRef, bytes)
      const rows = await database`
        select silomath.evidence_finalize_upload_command(
          ${user.id}::uuid, ${organizationId}::uuid, ${campaignId}::uuid,
          ${evidenceId}::uuid, ${objectRef}, ${actualHash},
          ${bytes.length}::bigint, ${idempotencyKey},
          ${requestFingerprint}, ${correlationId}
        ) as result
      `
      const result = rows[0].result
      return json(correlationId, Number(result.status), result.body)
    }

    if (evidenceItem && req.method === "DELETE") {
      const organizationId = evidenceItem[1]
      const campaignId = evidenceItem[2]
      const evidenceId = evidenceItem[3]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)
      const requestFingerprint = await fingerprint({
        method: req.method, path, organizationId, campaignId,
        evidenceId, expectedVersion,
      })
      const rows = await database`
        select silomath.evidence_remove_command(
          ${user.id}::uuid, ${organizationId}::uuid, ${campaignId}::uuid,
          ${evidenceId}::uuid, ${expectedVersion}::bigint,
          ${idempotencyKey}, ${requestFingerprint}, ${correlationId}
        ) as result
      `
      const result = rows[0].result
      if (Number(result.status) >= 200 && Number(result.status) < 300) {
        return new Response(null, {
          status: 204,
          headers: { "X-Correlation-Id": correlationId },
        })
      }
      return json(correlationId, Number(result.status), result.body)
    }

    if (evidenceContent && req.method === "GET") {
      const organizationId = evidenceContent[1]
      const campaignId = evidenceContent[2]
      const evidenceId = evidenceContent[3]
      enforceOrganizationContext(req, organizationId)
      await requireCampaignReadAccess(user.id, organizationId, campaignId, database)
      const rows = await database`
        select remote_object_ref
        from silomath.evidence
        where evidence_id = ${evidenceId}::uuid
          and organization_id = ${organizationId}::uuid
          and campaign_id = ${campaignId}::uuid
          and evidence_type = 'PHOTO'
          and sync_state = 'SYNCED'
          and removed_at is null
      `
      if (!rows[0]?.remote_object_ref) {
        throw new ApiProblem(404, "RESOURCE_NOT_FOUND", "The Evidence content was not found.")
      }
      const bytes = await objectStore.download(String(rows[0].remote_object_ref))
      return new Response(Uint8Array.from(bytes).buffer, {
        status: 200,
        headers: {
          "Content-Type": "image/jpeg",
          "Content-Length": String(bytes.length),
          "Cache-Control": "private, no-store",
          "X-Correlation-Id": correlationId,
        },
      })
    }

    if (stocktakingWorking && req.method === "PUT") {
      const organizationId = stocktakingWorking[1]
      const campaignId = stocktakingWorking[2]
      const stocktakingRecordId = stocktakingWorking[3]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)
      const commodityVersionId = typeof body.commodityVersionId === "string"
        ? body.commodityVersionId
        : ""
      const ownership = typeof body.ownership === "string"
        ? body.ownership
        : ""
      const clientId = body.clientId === null || body.clientId === undefined
        ? null
        : String(body.clientId)
      const density = Number(body.actualBulkDensityKgM3)

      if (!Array.isArray(body.measurements) ||
          body.measurements.length < 1 || body.measurements.length > 5 ||
          !body.calculationResult || typeof body.calculationResult !== "object" ||
          Array.isArray(body.calculationResult) ||
          !new RegExp(`^${UUID}$`).test(commodityVersionId) ||
          !Number.isFinite(density) || density <= 0 ||
          !["OWNED", "THIRD_PARTY"].includes(ownership) ||
          (ownership === "OWNED" && clientId !== null) ||
          (ownership === "THIRD_PARTY" &&
            (clientId === null || !new RegExp(`^${UUID}$`).test(clientId)))) {
        throw new ApiProblem(
          422,
          "STOCKTAKING_VALIDATION_FAILED",
          "Working revision requires 1–5 measurements and calculation metadata.",
        )
      }

      const requestFingerprint = await fingerprint({
        method: req.method,
        path,
        organizationId,
        campaignId,
        stocktakingRecordId,
        body,
      })

      const rows = await db`
        select silomath.stocktaking_working_upsert_idempotent_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${stocktakingRecordId}::uuid,
          ${expectedVersion}::bigint,
          ${commodityVersionId}::uuid,
          ${density}::numeric,
          ${ownership},
          ${clientId}::uuid,
          ${typeof body.notes === "string" ? body.notes : null},
          ${JSON.stringify(body.measurements)}::jsonb,
          ${JSON.stringify(body.calculationResult)}::jsonb,
          ${idempotencyKey},
          ${requestFingerprint}
        ) as result
      `
      return json(correlationId, 200, rows[0].result)
    }

    if (stocktakingMarkEmpty && req.method === "POST") {
      const organizationId = stocktakingMarkEmpty[1]
      const campaignId = stocktakingMarkEmpty[2]
      const stocktakingRecordId = stocktakingMarkEmpty[3]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)
      const requestFingerprint = await fingerprint({
        method: req.method,
        path,
        organizationId,
        campaignId,
        stocktakingRecordId,
        body,
      })

      const rows = await db`
        select silomath.stocktaking_mark_empty_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${stocktakingRecordId}::uuid,
          ${expectedVersion}::bigint,
          ${body.notes as string | null},
          ${idempotencyKey},
          ${requestFingerprint}
        ) as result
      `
      return json(correlationId, 200, rows[0].result)
    }

    if (stocktakingResetValidation && req.method === "POST") {
      const organizationId = stocktakingResetValidation[1]
      const campaignId = stocktakingResetValidation[2]
      const stocktakingRecordId = stocktakingResetValidation[3]
      enforceOrganizationContext(req, organizationId)
      const body = await bodyJson(req)
      requireClientOperationId(body)
      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)
      const newRevisionId = typeof body.stocktakingRevisionId === "string"
        ? body.stocktakingRevisionId
        : ""
      if (!new RegExp(`^${UUID}$`).test(newRevisionId)) {
        throw new ApiProblem(
          422,
          "VALIDATION_FAILED",
          "stocktakingRevisionId is required for Reset Validation.",
        )
      }
      const requestFingerprint = await fingerprint({
        method: req.method,
        path,
        organizationId,
        campaignId,
        stocktakingRecordId,
        body,
      })

      const rows = await db`
        select silomath.stocktaking_reset_validation_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${stocktakingRecordId}::uuid,
          ${expectedVersion}::bigint,
          ${newRevisionId}::uuid,
          ${idempotencyKey},
          ${requestFingerprint}
        ) as result
      `
      return json(correlationId, 200, rows[0].result)
    }

    // FD76 stocktaking validate handler
    if (stocktakingValidate && req.method === "POST") {
      const organizationId = stocktakingValidate[1]
      const campaignId = stocktakingValidate[2]
      const stocktakingRecordId = stocktakingValidate[3]

      enforceOrganizationContext(req, organizationId)

      const body = await bodyJson(req)
      requireClientOperationId(body)

      const idempotencyKey = mutationKey(req, body)
      const expectedVersion = baseEntityVersion(body, req)

      const requestFingerprint = await fingerprint({
        method: req.method,
        path,
        organizationId,
        campaignId,
        stocktakingRecordId,
        body,
      })

      const frozen = await loadFrozenCalculationInput(
        organizationId,
        campaignId,
        stocktakingRecordId,
      )

      const authoritative = await verifyAuthoritativeCalculation(
        frozen.input,
        frozen.provisionalInputFingerprint,
        frozen.provisionalProductVolumeM3,
      )

      const authoritativeCalculation = {
        inputFingerprint: authoritative.inputFingerprint,
        formulaCode: frozen.input.formulaCode,
        formulaVersion: frozen.input.formulaVersion,
        geometryFamilyId: frozen.input.geometryFamilyId,
        geometryProfileVersionId: frozen.input.geometryProfileVersionId,
        surfaceModelVersion: frozen.input.surfaceModelVersion,
        aggregationMethod: frozen.input.aggregationMethod,
        aggregationRuleVersion: frozen.input.aggregationRuleVersion,
        inputSchemaVersion: frozen.input.inputSchemaVersion,
        engineVersion: frozen.input.engineVersion,
        normalizationVersion: frozen.input.normalizationVersion,
        roundingPolicy: "ROUND_HALF_UP",
        frozenCalculationContext: {
          frozenContextId: frozen.input.frozenContextId,
          geometryFamilyId: frozen.input.geometryFamilyId,
          geometryProfileVersionId: frozen.input.geometryProfileVersionId,
          geometryFamily: frozen.input.geometryFamily,
          formulaCode: frozen.input.formulaCode,
          formulaVersion: frozen.input.formulaVersion,
          inputSchemaVersion: frozen.input.inputSchemaVersion,
          engineVersion: frozen.input.engineVersion,
          normalizationVersion: frozen.input.normalizationVersion,
          surfaceModelVersion: frozen.input.surfaceModelVersion,
          aggregationMethod: frozen.input.aggregationMethod,
          aggregationRuleVersion: frozen.input.aggregationRuleVersion,
        },
        datumMetadata: {
          reference: frozen.input.datumReference,
          measurementReferenceHeightM: frozen.input.measurementReferenceHeightM,
        },
        geometryInputsSnapshot: {
          geometryFamily: frozen.input.geometryFamily,
          fixedParameters: frozen.input.fixedParameters,
          approvedAreaProvenance: frozen.input.approvedAreaProvenance,
          hasConstantCrossSection: frozen.input.hasConstantCrossSection,
        },
        orderedNormalizedReadings: frozen.input.readings
          .slice()
          .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
          .map((reading) => ({
            sequenceNumber: reading.sequenceNumber,
            ullageM: reading.rawValue,
          })),
        geometryOrigin: frozen.input.geometryOrigin,
        measurementReferenceHeightM: frozen.input.measurementReferenceHeightM,
        maximumFillHeightM: frozen.input.maximumFillHeightM,
        maxCapacityM3: authoritative.maxCapacityM3,
        effectiveUllageM: authoritative.effectiveUllageM,
        effectiveFillHeightM: authoritative.effectiveFillHeightM,
        productVolumeM3: authoritative.productVolumeM3,
        enteredDensityValue: frozen.input.density!.value,
        enteredDensityUnit: frozen.input.density!.unit,
        actualBulkDensityKgM3: authoritative.normalizedDensityKgM3,
        physicalQuantityKg: authoritative.physicalQuantityKg,
      }

      const rows = await db`
        select silomath.stocktaking_validate_nonempty_command(
          ${user.id}::uuid,
          ${organizationId}::uuid,
          ${campaignId}::uuid,
          ${stocktakingRecordId}::uuid,
          ${expectedVersion}::bigint,
          ${idempotencyKey},
          ${requestFingerprint},
          ${JSON.stringify(authoritativeCalculation)}::jsonb
        ) as result
      `

      return json(correlationId, 200, rows[0].result)
    }
    return fail(
      correlationId,
      404,
      "RESOURCE_NOT_FOUND",
      "The requested API route was not found.",
    )
  } catch (error) {
    if (error instanceof ApiProblem) {
      return fail(
        correlationId,
        error.status,
        error.code,
        error.message,
        error.fieldErrors,
        error.retryable,
        error.details,
      )
    }

    if (error instanceof CalculationFailure) {
      return fail(
        correlationId,
        422,
        "STOCKTAKING_VALIDATION_FAILED",
        "The authoritative stocktaking calculation failed validation.",
        [],
        false,
        { calculationError: error.code },
      )
    }

    return postgresFailure(correlationId, error)
  }
  }
}

if (import.meta.main) Deno.serve(createApiHandler())

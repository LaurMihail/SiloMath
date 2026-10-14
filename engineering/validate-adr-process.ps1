
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$registerPath = Join-Path $PSScriptRoot "adr-register.json"
$processPath = Join-Path $PSScriptRoot "adr\README.md"
$templatePath = Join-Path $PSScriptRoot "adr\IADR_TEMPLATE.md"

$errors = @()

function Add-ValidationError {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $script:errors += $Message
}

# =========================================================================
# REQUIRED PROCESS ARTIFACTS
# =========================================================================

$requiredPaths = @(
    $registerPath,
    $processPath,
    $templatePath
)

foreach ($requiredPath in $requiredPaths) {
    if (-not (Test-Path $requiredPath)) {
        Add-ValidationError `
            "Required ADR governance artifact missing: $requiredPath"
    }
}

if ($errors.Count -gt 0) {
    Write-Host "ADR PROCESS VALIDATION: FAIL"

    foreach ($errorMessage in $errors) {
        Write-Host " - $errorMessage"
    }

    exit 1
}

# =========================================================================
# LOAD REGISTER
# =========================================================================

try {
    $register =
        Get-Content `
            -Path $registerPath `
            -Raw `
            -Encoding UTF8 |
        ConvertFrom-Json
}
catch {
    Write-Host "ADR PROCESS VALIDATION: FAIL"
    Write-Host " - adr-register.json is not valid JSON."
    Write-Host " - $($_.Exception.Message)"
    exit 1
}

# =========================================================================
# BASELINE / PROCESS IDENTITY
# =========================================================================

if ($register.baselineId -ne "SILOMATH_V6") {
    Add-ValidationError `
        "ADR register baselineId must be SILOMATH_V6."
}

if ($register.status -ne "ACTIVE") {
    Add-ValidationError `
        "ADR register status must be ACTIVE."
}

if ([int] $register.schemaVersion -ne 1) {
    Add-ValidationError `
        "ADR register schemaVersion must be 1."
}

if (
    $register.frozenArchitectureDecisionNamespace.sourceDocument -ne
    "07_ARCHITECTURE"
) {
    Add-ValidationError `
        "Frozen ADR namespace source must be 07_ARCHITECTURE."
}

if (
    $register.frozenArchitectureDecisionNamespace.reservedPattern -ne
    "^ADR-[0-9]{3}$"
) {
    Add-ValidationError `
        "Frozen architecture ADR namespace pattern is invalid."
}

# =========================================================================
# FROZEN ADR NAMESPACE
# =========================================================================

$expectedFrozenIds = @(
    1..17 |
        ForEach-Object {
            "ADR-{0:D3}" -f $_
        }
)

$actualFrozenIds = @(
    $register.frozenArchitectureDecisionNamespace.reservedIds
)

foreach ($expectedId in $expectedFrozenIds) {
    if ($expectedId -notin $actualFrozenIds) {
        Add-ValidationError (
            "Frozen architecture ADR missing from reserved namespace: " +
            $expectedId
        )
    }
}

foreach ($actualId in $actualFrozenIds) {
    if ($actualId -notin $expectedFrozenIds) {
        Add-ValidationError (
            "Unexpected frozen architecture ADR in reserved namespace: " +
            $actualId
        )
    }
}

if ($actualFrozenIds.Count -ne 17) {
    Add-ValidationError (
        "Frozen architecture ADR namespace must contain exactly 17 IDs; " +
        "found $($actualFrozenIds.Count)."
    )
}

# =========================================================================
# IMPLEMENTATION ADR NAMESPACE
# =========================================================================

if (
    $register.implementationAdrNamespace.pattern -ne
    "^IADR-[0-9]{4}$"
) {
    Add-ValidationError `
        "Implementation ADR namespace must use IADR-NNNN."
}

$nextId =
    [string] $register.implementationAdrNamespace.nextId

if ([string]::IsNullOrWhiteSpace($nextId)) {
    Add-ValidationError `
        "Implementation ADR namespace must declare nextId."
}
elseif ($nextId -notmatch '^IADR-[0-9]{4}$') {
    Add-ValidationError `
        "Implementation ADR nextId must use IADR-NNNN."
}

# =========================================================================
# STATUS REGISTRY
# =========================================================================

$expectedStatuses = @(
    "PROPOSED",
    "ACCEPTED",
    "REJECTED",
    "SUPERSEDED"
)

$actualStatuses = @(
    $register.allowedStatuses
)

foreach ($status in $expectedStatuses) {
    if ($status -notin $actualStatuses) {
        Add-ValidationError (
            "ADR status registry missing required status: $status"
        )
    }
}

foreach ($status in $actualStatuses) {
    if ($status -notin $expectedStatuses) {
        Add-ValidationError (
            "ADR status registry contains unsupported status: $status"
        )
    }
}

# =========================================================================
# RECORD VALIDATION
# =========================================================================

$records = @(
    $register.records
)

$duplicateIds = @(
    $records |
        Group-Object id |
        Where-Object {
            $_.Count -gt 1
        }
)

foreach ($duplicate in $duplicateIds) {
    Add-ValidationError (
        "Duplicate IADR id: $($duplicate.Name)"
    )
}

foreach ($record in $records) {
    $id = [string] $record.id
    $title = [string] $record.title
    $status = [string] $record.status
    $file = [string] $record.file

    if ($id -notmatch '^IADR-[0-9]{4}$') {
        Add-ValidationError (
            "Invalid implementation ADR id: $id"
        )

        continue
    }

    if ([string]::IsNullOrWhiteSpace($title)) {
        Add-ValidationError (
            "IADR $id has no title."
        )
    }

    if ($status -notin $expectedStatuses) {
        Add-ValidationError (
            "IADR $id has invalid status: $status"
        )
    }

    if ([string]::IsNullOrWhiteSpace($file)) {
        Add-ValidationError (
            "IADR $id has no registered file."
        )
    }
    else {
        $recordPath =
            Join-Path `
                $repoRoot `
                $file

        if (-not (Test-Path $recordPath)) {
            Add-ValidationError (
                "IADR $id registered file does not exist: $file"
            )
        }
        else {
            $recordFileName =
                [System.IO.Path]::GetFileName(
                    $recordPath
                )

            if (
                $recordFileName -notmatch
                '^IADR-[0-9]{4}-[a-z0-9]+(?:-[a-z0-9]+)*\.md$'
            ) {
                Add-ValidationError (
                    "IADR $id file name does not follow " +
                    "IADR-NNNN-short-kebab-title.md: $recordFileName"
                )
            }

            if (
                -not $recordFileName.StartsWith(
                    "$id-",
                    [System.StringComparison]::Ordinal
                )
            ) {
                Add-ValidationError (
                    "IADR $id file name must begin with $id-."
                )
            }
        }
    }

    if ($record.changesFrozenBaseline -ne $false) {
        Add-ValidationError (
            "IADR $id attempts to change frozen V6 baseline. " +
            "Use the Baseline Change process instead."
        )
    }

    if ($status -eq "ACCEPTED") {
        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.decisionDate
            )
        ) {
            Add-ValidationError (
                "Accepted IADR $id has no decisionDate."
            )
        }

        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.owner
            )
        ) {
            Add-ValidationError (
                "Accepted IADR $id has no owner."
            )
        }

        $baselineRefs = @(
            $record.baselineRefs |
                Where-Object {
                    -not [string]::IsNullOrWhiteSpace(
                        [string] $_
                    )
                }
        )

        if ($baselineRefs.Count -eq 0) {
            Add-ValidationError (
                "Accepted IADR $id has no frozen baseline references."
            )
        }
    }

    if ($status -eq "SUPERSEDED") {
        $supersededBy =
            [string] $record.supersededBy

        if ([string]::IsNullOrWhiteSpace($supersededBy)) {
            Add-ValidationError (
                "SUPERSEDED IADR $id has no supersededBy reference."
            )
        }
        elseif ($supersededBy -notmatch '^IADR-[0-9]{4}$') {
            Add-ValidationError (
                "SUPERSEDED IADR $id has invalid supersededBy: " +
                $supersededBy
            )
        }
    }
}

# =========================================================================
# PROCESS DOCUMENT SAFETY ASSERTIONS
# =========================================================================

$processText =
    Get-Content `
        -Path $processPath `
        -Raw `
        -Encoding UTF8

$templateText =
    Get-Content `
        -Path $templatePath `
        -Raw `
        -Encoding UTF8

$normalizedProcessText =
    [regex]::Replace(
        $processText,
        '[\u2010-\u2014\u2212]',
        '-'
    )

if (
    $normalizedProcessText -notmatch
    'Documents\s+01\s*-\s*21'
) {
    Add-ValidationError `
        "ADR process must identify Documents 01-21 as authoritative."
}

if ($processText -notmatch 'MUST NOT') {
    Add-ValidationError `
        "ADR process must contain an explicit MUST NOT authority rule."
}

if ($processText -notmatch 'Baseline Change') {
    Add-ValidationError `
        "ADR process must route frozen changes to Baseline Change."
}

if ($processText -notmatch 'ADR-001 through ADR-017') {
    Add-ValidationError `
        "ADR process must reserve frozen ADR-001 through ADR-017."
}

if ($processText -notmatch 'IADR-0001') {
    Add-ValidationError `
        "ADR process must define the IADR implementation namespace."
}

if (
    $templateText -notmatch
    'Changes frozen V6 baseline:\s*NO'
) {
    Add-ValidationError `
        "IADR template must contain the frozen-baseline impact check."
}

if (
    $templateText -notmatch
    'Frozen baseline references'
) {
    Add-ValidationError `
        "IADR template must require frozen baseline references."
}

if (
    $templateText -notmatch
    'Alternatives considered'
) {
    Add-ValidationError `
        "IADR template must require alternatives considered."
}

if (
    $templateText -notmatch
    'Verification impact'
) {
    Add-ValidationError `
        "IADR template must require verification impact."
}

# =========================================================================
# RESULT
# =========================================================================

if ($errors.Count -gt 0) {
    Write-Host "ADR PROCESS VALIDATION: FAIL"

    foreach ($errorMessage in $errors) {
        Write-Host " - $errorMessage"
    }

    exit 1
}

Write-Host "ADR PROCESS VALIDATION: PASS"
Write-Host "Baseline: SILOMATH_V6"
Write-Host "Frozen architecture ADRs reserved: 17"
Write-Host "Registered implementation ADRs: $($records.Count)"
Write-Host "Implementation ADR namespace: IADR-NNNN"
Write-Host "Frozen baseline changes through IADR: PROHIBITED"

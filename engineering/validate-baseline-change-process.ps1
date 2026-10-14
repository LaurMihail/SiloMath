
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

$registerPath =
    Join-Path `
        $PSScriptRoot `
        "baseline-change-register.json"

$processPath =
    Join-Path `
        $PSScriptRoot `
        "baseline-change\README.md"

$templatePath =
    Join-Path `
        $PSScriptRoot `
        "baseline-change\CR_TEMPLATE.md"

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
            "Required Baseline Change governance artifact missing: $requiredPath"
    }
}

if ($errors.Count -gt 0) {
    Write-Host "BASELINE CHANGE PROCESS VALIDATION: FAIL"

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
    Write-Host "BASELINE CHANGE PROCESS VALIDATION: FAIL"
    Write-Host " - baseline-change-register.json is not valid JSON."
    Write-Host " - $($_.Exception.Message)"
    exit 1
}

# =========================================================================
# REGISTER IDENTITY
# =========================================================================

if ($register.baselineId -ne "SILOMATH_V6") {
    Add-ValidationError `
        "Baseline Change register baselineId must be SILOMATH_V6."
}

if ($register.status -ne "ACTIVE") {
    Add-ValidationError `
        "Baseline Change register status must be ACTIVE."
}

if ([int] $register.schemaVersion -ne 1) {
    Add-ValidationError `
        "Baseline Change register schemaVersion must be 1."
}

$expectedPattern =
    "^CR-[A-Z0-9]+(?:-[A-Z0-9]+)*-[0-9]{3}$"

if (
    $register.changeRequestNamespace.pattern -ne
    $expectedPattern
) {
    Add-ValidationError `
        "Change Request namespace pattern is invalid."
}

if (
    $register.changeRequestNamespace.example -ne
    "CR-API-ORG-MEM-001"
) {
    Add-ValidationError `
        "Change Request namespace example must preserve CR-API-ORG-MEM-001."
}

if (
    $register.authoritativeApprovalStatus -ne
    "APPROVED"
) {
    Add-ValidationError `
        "Only APPROVED may be the authoritative Change Request status."
}

# =========================================================================
# STATUS REGISTRY
# =========================================================================

$expectedStatuses = @(
    "PROPOSED",
    "APPROVED",
    "REJECTED",
    "SUPERSEDED"
)

$actualStatuses = @(
    $register.allowedStatuses
)

foreach ($status in $expectedStatuses) {
    if ($status -notin $actualStatuses) {
        Add-ValidationError (
            "Baseline Change status registry missing required status: " +
            $status
        )
    }
}

foreach ($status in $actualStatuses) {
    if ($status -notin $expectedStatuses) {
        Add-ValidationError (
            "Baseline Change status registry contains unsupported status: " +
            $status
        )
    }
}

if ($actualStatuses.Count -ne $expectedStatuses.Count) {
    Add-ValidationError `
        "Baseline Change status registry must contain exactly four statuses."
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
        "Duplicate Change Request id: $($duplicate.Name)"
    )
}

foreach ($record in $records) {
    $id = [string] $record.id
    $title = [string] $record.title
    $status = [string] $record.status
    $file = [string] $record.file

    if ($id -notmatch $expectedPattern) {
        Add-ValidationError (
            "Invalid Change Request id: $id"
        )

        continue
    }

    if ([string]::IsNullOrWhiteSpace($title)) {
        Add-ValidationError (
            "Change Request $id has no title."
        )
    }

    if ($status -notin $expectedStatuses) {
        Add-ValidationError (
            "Change Request $id has invalid status: $status"
        )
    }

    if ($record.changesFrozenBaseline -ne $true) {
        Add-ValidationError (
            "Change Request $id must explicitly declare " +
            "changesFrozenBaseline=true."
        )
    }

    if ([string]::IsNullOrWhiteSpace($file)) {
        Add-ValidationError (
            "Change Request $id has no registered file."
        )
    }
    else {
        $recordPath =
            Join-Path `
                $repoRoot `
                $file

        if (-not (Test-Path $recordPath)) {
            Add-ValidationError (
                "Change Request $id registered file does not exist: $file"
            )
        }
        else {
            $recordFileName =
                [System.IO.Path]::GetFileName(
                    $recordPath
                )

            $escapedId =
                [regex]::Escape(
                    $id
                )

            $expectedFilePattern =
                "^$escapedId(?:-[a-z0-9]+(?:-[a-z0-9]+)*)?\.md$"

            if (
                $recordFileName -notmatch
                $expectedFilePattern
            ) {
                Add-ValidationError (
                    "Change Request $id file name must use the CR id " +
                    "as its filename prefix: $recordFileName"
                )
            }
        }
    }

    if ($status -eq "APPROVED") {
        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.decisionDate
            )
        ) {
            Add-ValidationError (
                "Approved Change Request $id has no decisionDate."
            )
        }

        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.approvedBy
            )
        ) {
            Add-ValidationError (
                "Approved Change Request $id has no approvedBy authority."
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
                "Approved Change Request $id has no frozen baseline references."
            )
        }

        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.approvedDelta
            )
        ) {
            Add-ValidationError (
                "Approved Change Request $id has no approvedDelta."
            )
        }

        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.rvmImpact
            )
        ) {
            Add-ValidationError (
                "Approved Change Request $id has no RVM impact assessment."
            )
        }

        if (
            [string]::IsNullOrWhiteSpace(
                [string] $record.testImpact
            )
        ) {
            Add-ValidationError (
                "Approved Change Request $id has no affected-test assessment."
            )
        }
    }

    if ($status -eq "SUPERSEDED") {
        $supersededBy =
            [string] $record.supersededBy

        if ([string]::IsNullOrWhiteSpace($supersededBy)) {
            Add-ValidationError (
                "SUPERSEDED Change Request $id has no supersededBy reference."
            )
        }
        elseif ($supersededBy -notmatch $expectedPattern) {
            Add-ValidationError (
                "SUPERSEDED Change Request $id has invalid supersededBy: " +
                $supersededBy
            )
        }
    }
}

# =========================================================================
# PROCESS SAFETY ASSERTIONS
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
        "Baseline Change process must identify Documents 01-21 as authoritative."
}

if ($processText -notmatch 'MUST NOT') {
    Add-ValidationError `
        "Baseline Change process must contain explicit MUST NOT safety rules."
}

if (
    $processText -notmatch
    'Only APPROVED authorizes'
) {
    Add-ValidationError `
        "Baseline Change process must state that only APPROVED authorizes change."
}

if ($processText -notmatch 'IADR') {
    Add-ValidationError `
        "Baseline Change process must distinguish Change Requests from IADRs."
}

if ($processText -notmatch 'RVM') {
    Add-ValidationError `
        "Baseline Change process must require RVM impact handling."
}

if (
    $processText -notmatch
    'affected tests'
) {
    Add-ValidationError `
        "Baseline Change process must require affected-test handling."
}

if (
    $processText -notmatch
    'contradict'
) {
    Add-ValidationError `
        "Baseline Change process must prohibit silent contradiction resolution."
}

if (
    $templateText -notmatch
    'Changes frozen V6 baseline:\s*YES'
) {
    Add-ValidationError `
        "Change Request template must explicitly identify frozen-baseline impact."
}

if (
    $templateText -notmatch
    'Current frozen contract'
) {
    Add-ValidationError `
        "Change Request template must capture the current frozen contract."
}

if (
    $templateText -notmatch
    'Requested delta'
) {
    Add-ValidationError `
        "Change Request template must capture the requested delta."
}

if (
    $templateText -notmatch
    'RVM impact'
) {
    Add-ValidationError `
        "Change Request template must capture RVM impact."
}

if (
    $templateText -notmatch
    'Test impact'
) {
    Add-ValidationError `
        "Change Request template must capture affected-test impact."
}

if (
    $templateText -notmatch
    'Approved delta'
) {
    Add-ValidationError `
        "Change Request template must capture the exact approved delta."
}

# =========================================================================
# RESULT
# =========================================================================

if ($errors.Count -gt 0) {
    Write-Host "BASELINE CHANGE PROCESS VALIDATION: FAIL"

    foreach ($errorMessage in $errors) {
        Write-Host " - $errorMessage"
    }

    exit 1
}

Write-Host "BASELINE CHANGE PROCESS VALIDATION: PASS"
Write-Host "Baseline: SILOMATH_V6"
Write-Host "Registered Change Requests: $($records.Count)"
Write-Host "Authoritative CR status: APPROVED"
Write-Host "Frozen baseline changes without approved CR: PROHIBITED"

$ErrorActionPreference = "Stop"

$registryPath = Join-Path `
    $PSScriptRoot `
    "requirement-id-registry.json"

$mappingsPath = Join-Path `
    $PSScriptRoot `
    "rvm-mappings.json"

$rvmPath = Join-Path `
    $PSScriptRoot `
    "rvm.json"

if (-not (Test-Path $registryPath)) {
    throw "Requirement registry not found: $registryPath"
}

if (-not (Test-Path $mappingsPath)) {
    throw "RVM mapping source not found: $mappingsPath"
}

if (-not (Test-Path $rvmPath)) {
    throw "RVM not found: $rvmPath"
}

$registry = Get-Content `
    -Path $registryPath `
    -Raw |
ConvertFrom-Json

$mappingsSource = Get-Content `
    -Path $mappingsPath `
    -Raw |
ConvertFrom-Json

$rvm = Get-Content `
    -Path $rvmPath `
    -Raw |
ConvertFrom-Json

$errors = @()

# -------------------------------------------------------------------------
# BASELINE IDENTITY
# -------------------------------------------------------------------------

if ($rvm.baselineId -ne "SILOMATH_V6") {
    $errors += "RVM baselineId must be SILOMATH_V6."
}

if ($rvm.schemaAuthority -ne "18_TEST_STRATEGY_ACCEPTANCE") {
    $errors += (
        "RVM schemaAuthority must be 18_TEST_STRATEGY_ACCEPTANCE."
    )
}

if ($mappingsSource.baselineId -ne "SILOMATH_V6") {
    $errors += "Mapping source baselineId must be SILOMATH_V6."
}

# -------------------------------------------------------------------------
# REGISTRY / RVM ID PARITY
# -------------------------------------------------------------------------

$registryIds = @(
    $registry.requirements.requirementId |
    Sort-Object -Unique
)

$rvmIds = @(
    $rvm.requirements.requirementDecisionId |
    Sort-Object -Unique
)

foreach ($id in $registryIds) {

    if ($id -notin $rvmIds) {
        $errors += "Registry ID missing from RVM: $id"
    }
}

foreach ($id in $rvmIds) {

    if ($id -notin $registryIds) {
        $errors += "RVM contains unknown registry ID: $id"
    }
}

if ($registryIds.Count -ne $rvmIds.Count) {

    $errors += (
        "Registry/RVM unique ID count mismatch. Registry=$($registryIds.Count), " +
        "RVM=$($rvmIds.Count)"
    )
}

# -------------------------------------------------------------------------
# MAPPING SOURCE VALIDATION
# -------------------------------------------------------------------------

$duplicateMappings = @(
    $mappingsSource.mappings |
    Group-Object requirementDecisionId |
    Where-Object {
        $_.Count -gt 1
    }
)

if ($duplicateMappings.Count -gt 0) {

    foreach ($duplicate in $duplicateMappings) {

        $errors += (
            "Duplicate mapping source row for Requirement/Decision ID: " +
            $duplicate.Name
        )
    }
}

$validResults = @(
    "Pass",
    "Fail",
    "Blocked",
    "Not Run"
)

$validTestLevels = @(
    "Unit",
    "DB",
    "Repository",
    "API",
    "Integration",
    "UI",
    "E2E",
    "Security",
    "UAT"
)

foreach ($mapping in $mappingsSource.mappings) {

    $id = [string] $mapping.requirementDecisionId

    if ($id -notin $registryIds) {
        $errors += "Mapping source references unknown ID: $id"
    }

    if ([string]::IsNullOrWhiteSpace($mapping.componentModule)) {
        $errors += "Mapping $id has no componentModule."
    }

    $tests = @(
        $mapping.testIds |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace($_)
        }
    )

    if ($tests.Count -eq 0) {
        $errors += "Mapping $id has no testIds."
    }

    foreach ($testRef in $tests) {

        if ($testRef -notmatch '#') {
            $errors += (
                "Mapping $id has invalid automated test reference: " +
                "$testRef"
            )
        }

        $parts = $testRef -split '#', 2

        if ($parts.Count -ne 2) {
            continue
        }

        $relativePath = $parts[0]
        $testName = $parts[1]

        if ([string]::IsNullOrWhiteSpace($relativePath)) {
            $errors += "Mapping $id has empty test file path."
        }

        if ([string]::IsNullOrWhiteSpace($testName)) {
            $errors += "Mapping $id has empty test function reference."
        }

        $absoluteTestPath = Join-Path `
            (Split-Path -Parent $PSScriptRoot) `
            $relativePath

        if (-not (Test-Path $absoluteTestPath)) {
            $errors += (
                "Mapping $id references missing test file: " +
                "$relativePath"
            )
        }
    }

    if ($mapping.testLevel -notin $validTestLevels) {

        $errors += (
            "Mapping $id has invalid testLevel: " +
            "$($mapping.testLevel)"
        )
    }

    if ($mapping.result -notin $validResults) {

        $errors += (
            "Mapping $id has invalid result: " +
            "$($mapping.result)"
        )
    }
}

# -------------------------------------------------------------------------
# RVM ROW VALIDATION
# -------------------------------------------------------------------------

$validMappingStatuses = @(
    "UNMAPPED",
    "MAPPED",
    "VERIFIED"
)

foreach ($row in $rvm.requirements) {

    $id = $row.requirementDecisionId

    if ([string]::IsNullOrWhiteSpace($id)) {
        $errors += "RVM row contains empty requirementDecisionId."
        continue
    }

    if (
        [string]::IsNullOrWhiteSpace(
            $row.sourceDocumentVersion
        )
    ) {
        $errors += "RVM row $id has no sourceDocumentVersion."
    }

    if ($row.result -notin $validResults) {

        $errors += (
            "RVM row $id has invalid result: " +
            "$($row.result)"
        )
    }

    if ($row.mappingStatus -notin $validMappingStatuses) {

        $errors += (
            "RVM row $id has invalid mappingStatus: " +
            "$($row.mappingStatus)"
        )
    }

    if ($row.mappingStatus -eq "UNMAPPED") {

        if (
            -not [string]::IsNullOrWhiteSpace($row.componentModule) -or
            -not [string]::IsNullOrWhiteSpace($row.testIds)
        ) {
            $errors += (
                "UNMAPPED row $id contains component/test mapping."
            )
        }
    }

    if (
        $row.mappingStatus -eq "MAPPED" -or
        $row.mappingStatus -eq "VERIFIED"
    ) {

        if ([string]::IsNullOrWhiteSpace($row.componentModule)) {
            $errors += "$($row.mappingStatus) row $id has no componentModule."
        }

        if ([string]::IsNullOrWhiteSpace($row.testIds)) {
            $errors += "$($row.mappingStatus) row $id has no testIds."
        }

        if ([string]::IsNullOrWhiteSpace($row.testLevel)) {
            $errors += "$($row.mappingStatus) row $id has no testLevel."
        }

        if ([string]::IsNullOrWhiteSpace($row.automation)) {
            $errors += "$($row.mappingStatus) row $id has no automation."
        }

        if ([string]::IsNullOrWhiteSpace($row.environment)) {
            $errors += "$($row.mappingStatus) row $id has no environment."
        }
    }

    if ($row.mappingStatus -eq "VERIFIED") {

        if ($row.result -ne "Pass") {
            $errors += "VERIFIED row $id must have result Pass."
        }

        if ([string]::IsNullOrWhiteSpace($row.evidence)) {
            $errors += "VERIFIED row $id must have evidence."
        }

        if ([string]::IsNullOrWhiteSpace($row.buildVersion)) {
            $errors += "VERIFIED row $id must have buildVersion."
        }
    }
}

# -------------------------------------------------------------------------
# SUMMARY COUNTS
# -------------------------------------------------------------------------

$mappedCount = @(
    $rvm.requirements |
    Where-Object {
        $_.mappingStatus -eq "MAPPED"
    }
).Count

$verifiedCount = @(
    $rvm.requirements |
    Where-Object {
        $_.mappingStatus -eq "VERIFIED"
    }
).Count

$unmappedCount = @(
    $rvm.requirements |
    Where-Object {
        $_.mappingStatus -eq "UNMAPPED"
    }
).Count

if (
    (
        $mappedCount +
        $verifiedCount +
        $unmappedCount
    ) -ne $rvm.requirements.Count
) {
    $errors += "RVM mapping status counts do not add up to total rows."
}

if ($rvm.summary.total -ne $rvm.requirements.Count) {
    $errors += "RVM summary.total mismatch."
}

if ($rvm.summary.mapped -ne $mappedCount) {
    $errors += "RVM summary.mapped mismatch."
}

if ($rvm.summary.verified -ne $verifiedCount) {
    $errors += "RVM summary.verified mismatch."
}

if ($rvm.summary.unmapped -ne $unmappedCount) {
    $errors += "RVM summary.unmapped mismatch."
}

# -------------------------------------------------------------------------
# RESULT
# -------------------------------------------------------------------------

if ($errors.Count -gt 0) {

    Write-Host ""
    Write-Host "RVM VALIDATION: FAILED" `
        -ForegroundColor Red

    Write-Host ""

    foreach ($errorMessage in $errors) {

        Write-Host " - $errorMessage" `
            -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "Error count: $($errors.Count)"

    exit 1
}

Write-Host ""
Write-Host "RVM VALIDATION: PASS" `
    -ForegroundColor Green

Write-Host ""
Write-Host "Baseline       : $($rvm.baselineId)"
Write-Host "Schema         : $($rvm.schemaAuthority)"
Write-Host "Total IDs      : $($rvm.requirements.Count)"
Write-Host "Mapped         : $mappedCount"
Write-Host "Verified       : $verifiedCount"
Write-Host "Unmapped       : $unmappedCount"
Write-Host ""
Write-Host "Controlled mapping source: engineering/rvm-mappings.json"
Write-Host "No semantic auto-mapping or invented evidence detected."

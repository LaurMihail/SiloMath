$ErrorActionPreference = "Stop"

$registryPath = Join-Path `
    $PSScriptRoot `
    "requirement-id-registry.json"

$baselineRegistryPath = Join-Path `
    $PSScriptRoot `
    "baseline-registry.json"

if (-not (Test-Path $registryPath)) {
    throw "Requirement registry not found: $registryPath"
}

if (-not (Test-Path $baselineRegistryPath)) {
    throw "Baseline registry not found: $baselineRegistryPath"
}

$registry = Get-Content `
    -Path $registryPath `
    -Raw |
ConvertFrom-Json

$baseline = Get-Content `
    -Path $baselineRegistryPath `
    -Raw |
ConvertFrom-Json

$errors = @()

# -------------------------------------------------------------------------
# HELPERS
# -------------------------------------------------------------------------

function Get-RegistryIds {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Pattern
    )

    return @(
        $registry.requirements |
        Where-Object {
            $_.requirementId -match $Pattern
        } |
        ForEach-Object {
            $_.requirementId
        } |
        Sort-Object -Unique
    )
}

function Test-ExactRange {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Name,

        [Parameter(Mandatory = $true)]
        [string] $Pattern,

        [Parameter(Mandatory = $true)]
        [int] $Start,

        [Parameter(Mandatory = $true)]
        [int] $End,

        [Parameter(Mandatory = $true)]
        [scriptblock] $Formatter
    )

    $expected = @(
        $Start..$End |
        ForEach-Object {
            & $Formatter $_
        }
    )

    $actual = Get-RegistryIds -Pattern $Pattern

    foreach ($id in $expected) {

        if ($id -notin $actual) {
            $script:errors += "$Name missing: $id"
        }
    }

    foreach ($id in $actual) {

        if ($id -notin $expected) {
            $script:errors += "$Name contains invalid ID: $id"
        }
    }
}

# -------------------------------------------------------------------------
# BASELINE
# -------------------------------------------------------------------------

if ($registry.baselineId -ne "SILOMATH_V6") {
    $errors += "baselineId must be SILOMATH_V6."
}

if ($registry.status -ne "APPROVED_FROZEN") {
    $errors += "Registry status must be APPROVED_FROZEN."
}

if ($baseline.documents.Count -ne 21) {
    $errors += "Baseline registry must contain exactly 21 documents."
}

if ($registry.requirements.Count -eq 0) {
    $errors += "Requirement registry is empty."
}

# -------------------------------------------------------------------------
# EXACT FROZEN RANGES
# -------------------------------------------------------------------------

Test-ExactRange `
    -Name "Core decision" `
    -Pattern '^D\d{2}$' `
    -Start 1 `
    -End 36 `
    -Formatter {
        param($n)
        "D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Final decision" `
    -Pattern '^FD-\d{2}$' `
    -Start 1 `
    -End 57 `
    -Formatter {
        param($n)
        "FD-{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Architecture decision" `
    -Pattern '^ADR-\d{3}$' `
    -Start 1 `
    -End 17 `
    -Formatter {
        param($n)
        "ADR-{0:D3}" -f $n
    }

Test-ExactRange `
    -Name "Architecture NFR" `
    -Pattern '^AR-NFR-\d{2}$' `
    -Start 1 `
    -End 12 `
    -Formatter {
        param($n)
        "AR-NFR-{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Architecture acceptance criterion" `
    -Pattern '^AR-AC-\d{2}$' `
    -Start 1 `
    -End 22 `
    -Formatter {
        param($n)
        "AR-AC-{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Module architecture acceptance criterion" `
    -Pattern '^MA-\d{2}$' `
    -Start 1 `
    -End 22 `
    -Formatter {
        param($n)
        "MA-{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Data-model decision" `
    -Pattern '^DM-D\d{2}$' `
    -Start 1 `
    -End 60 `
    -Formatter {
        param($n)
        "DM-D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Data-model acceptance criterion" `
    -Pattern '^DM-\d{2}$' `
    -Start 1 `
    -End 30 `
    -Formatter {
        param($n)
        "DM-{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Database decision" `
    -Pattern '^DB-D\d{2}$' `
    -Start 1 `
    -End 64 `
    -Formatter {
        param($n)
        "DB-D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Database acceptance gate" `
    -Pattern '^DB-AC\d{2}$' `
    -Start 1 `
    -End 16 `
    -Formatter {
        param($n)
        "DB-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "API acceptance gate" `
    -Pattern '^API-AC\d{2}$' `
    -Start 1 `
    -End 25 `
    -Formatter {
        param($n)
        "API-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Offline/sync decision" `
    -Pattern '^OS-D\d{2}$' `
    -Start 1 `
    -End 38 `
    -Formatter {
        param($n)
        "OS-D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Sync acceptance gate" `
    -Pattern '^SYNC-AC\d{2}$' `
    -Start 1 `
    -End 20 `
    -Formatter {
        param($n)
        "SYNC-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Calculation decision" `
    -Pattern '^CE-D\d{2}$' `
    -Start 1 `
    -End 42 `
    -Formatter {
        param($n)
        "CE-D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Calculation acceptance gate" `
    -Pattern '^CALC-AC\d{2}$' `
    -Start 1 `
    -End 20 `
    -Formatter {
        param($n)
        "CALC-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Commodity decision" `
    -Pattern '^CO-D\d{2}$' `
    -Start 1 `
    -End 32 `
    -Formatter {
        param($n)
        "CO-D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Commodity acceptance gate" `
    -Pattern '^COM-AC\d{2}$' `
    -Start 1 `
    -End 20 `
    -Formatter {
        param($n)
        "COM-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Evidence decision" `
    -Pattern '^EV-D\d{2}$' `
    -Start 1 `
    -End 44 `
    -Formatter {
        param($n)
        "EV-D{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Evidence acceptance gate" `
    -Pattern '^EVD-AC\d{2}$' `
    -Start 1 `
    -End 20 `
    -Formatter {
        param($n)
        "EVD-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Reporting decision" `
    -Pattern '^RP-D\d{2,3}$' `
    -Start 1 `
    -End 107 `
    -Formatter {
        param($n)

        if ($n -lt 100) {
            "RP-D{0:D2}" -f $n
        }
        else {
            "RP-D{0:D3}" -f $n
        }
    }

Test-ExactRange `
    -Name "Reporting acceptance gate" `
    -Pattern '^RPT-AC\d{2}$' `
    -Start 1 `
    -End 25 `
    -Formatter {
        param($n)
        "RPT-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Security decision" `
    -Pattern '^SEC-D\d{3}$' `
    -Start 1 `
    -End 144 `
    -Formatter {
        param($n)
        "SEC-D{0:D3}" -f $n
    }

Test-ExactRange `
    -Name "Security acceptance gate" `
    -Pattern '^SEC-AC\d{2}$' `
    -Start 1 `
    -End 25 `
    -Formatter {
        param($n)
        "SEC-AC{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Test-strategy decision" `
    -Pattern '^TA-D\d{3}$' `
    -Start 1 `
    -End 218 `
    -Formatter {
        param($n)
        "TA-D{0:D3}" -f $n
    }

Test-ExactRange `
    -Name "Release gate" `
    -Pattern '^REL-G\d{2}$' `
    -Start 1 `
    -End 20 `
    -Formatter {
        param($n)
        "REL-G{0:D2}" -f $n
    }

Test-ExactRange `
    -Name "Retention decision" `
    -Pattern '^RT-D\d{3}$' `
    -Start 1 `
    -End 198 `
    -Formatter {
        param($n)
        "RT-D{0:D3}" -f $n
    }

Test-ExactRange `
    -Name "Production Operations decision" `
    -Pattern '^PO-D\d{3}$' `
    -Start 1 `
    -End 285 `
    -Formatter {
        param($n)
        "PO-D{0:D3}" -f $n
    }

# -------------------------------------------------------------------------
# FC / IC — CONCRETE ONLY, NEVER RANGE SYNTHESIS
# -------------------------------------------------------------------------

$fcIds = Get-RegistryIds -Pattern '^FC-\d{2}$'
$icIds = Get-RegistryIds -Pattern '^IC-\d{2}$'

foreach ($id in $fcIds) {

    $number = [int] $id.Substring(3)

    if ($number -lt 1 -or $number -gt 11) {
        $errors += "FC ID outside declared range: $id"
    }
}

foreach ($id in $icIds) {

    $number = [int] $id.Substring(3)

    if ($number -lt 1 -or $number -gt 17) {
        $errors += "IC ID outside declared range: $id"
    }
}

# -------------------------------------------------------------------------
# BUSINESS RULE PRESENCE
# -------------------------------------------------------------------------

$businessRules = Get-RegistryIds -Pattern '^BR-[A-Z0-9]+-\d{3}$'

if ($businessRules.Count -eq 0) {
    $errors += "No BR-* business rules found."
}

# -------------------------------------------------------------------------
# TEST / VECTOR IDS ARE FORBIDDEN AS REQUIREMENT ROWS
# -------------------------------------------------------------------------

$forbiddenTestPatterns = [ordered]@{
    "CE-G golden vector" = '^CE-G\d{2}$'
    "CE-B boundary vector" = '^CE-B\d{2}$'
    "COM-T test" = '^COM-T\d{2}$'
    "EVD-T test" = '^EVD-T\d{2}$'
    "RPT-T test" = '^RPT-T\d{2}$'
    "SEC-T test" = '^SEC-T\d{2}$'
    "SYNC-T test" = '^SYNC-T\d{2}$'
}

foreach ($entry in $forbiddenTestPatterns.GetEnumerator()) {

    $bad = Get-RegistryIds -Pattern $entry.Value

    if ($bad.Count -gt 0) {

        $errors += (
            "$($entry.Key) IDs must not appear as Requirement rows: " +
            ($bad -join ", ")
        )
    }
}

# -------------------------------------------------------------------------
# FORBIDDEN SHORT-FORM RANGE HEADINGS
# -------------------------------------------------------------------------

$forbiddenShortPatterns = @(
    '^SEC-D\d{2}$',
    '^TA-D\d{2}$',
    '^RT-D\d{2}$',
    '^PO-D\d{2}$'
)

foreach ($pattern in $forbiddenShortPatterns) {

    $bad = Get-RegistryIds -Pattern $pattern

    if ($bad.Count -gt 0) {

        $errors += (
            "Forbidden short-form canonical decision IDs: " +
            ($bad -join ", ")
        )
    }
}

# -------------------------------------------------------------------------
# REQUIRED NAMESPACE PRESENCE
# -------------------------------------------------------------------------

$requiredNamespaces = @(
    '^D\d{2}$',
    '^FD-\d{2}$',
    '^BR-',
    '^ADR-',
    '^AR-NFR-',
    '^AR-AC-',
    '^FC-',
    '^IC-',
    '^MA-',
    '^DM-D',
    '^DM-\d{2}$',
    '^DB-D',
    '^DB-AC',
    '^API-AC',
    '^OS-D',
    '^SYNC-AC',
    '^CE-D',
    '^CALC-AC',
    '^CO-D',
    '^COM-AC',
    '^EV-D',
    '^EVD-AC',
    '^RP-D',
    '^RPT-AC',
    '^SEC-D',
    '^SEC-AC',
    '^TA-D',
    '^REL-G',
    '^RT-D',
    '^PO-D'
)

foreach ($namespace in $requiredNamespaces) {

    $exists = $registry.requirements |
        Where-Object {
            $_.requirementId -match $namespace
        } |
        Select-Object -First 1

    if ($null -eq $exists) {
        $errors += "Required source namespace absent: $namespace"
    }
}

# -------------------------------------------------------------------------
# DOCUMENT COVERAGE
# -------------------------------------------------------------------------

$coveredDocumentIds = @(
    $registry.requirements.sourceDocumentId |
    Sort-Object -Unique
)

$expectedDocumentsWithoutNamespace = @(
    "01",
    "21"
)

$actualDocumentsWithoutNamespace = @(
    $baseline.documents |
    Where-Object {
        $_.id -notin $coveredDocumentIds
    } |
    ForEach-Object {
        $_.id
    } |
    Sort-Object
)

if (
    ($actualDocumentsWithoutNamespace -join ",") -ne
    ($expectedDocumentsWithoutNamespace -join ",")
) {

    $errors += (
        "Unexpected document coverage. Expected only 01 and 21 " +
        "without dedicated source IDs; actual: " +
        ($actualDocumentsWithoutNamespace -join ", ")
    )
}

if ($coveredDocumentIds.Count -ne 19) {

    $errors += (
        "Expected exactly 19/21 documents with source IDs; actual: " +
        "$($coveredDocumentIds.Count)/21."
    )
}

# -------------------------------------------------------------------------
# SOURCE DOCUMENT / STATUS VALIDATION
# -------------------------------------------------------------------------

$knownDocumentIds = @(
    $baseline.documents.id
)

foreach ($record in $registry.requirements) {

    if ($record.sourceDocumentId -notin $knownDocumentIds) {

        $errors += (
            "ID $($record.requirementId) references unknown source document " +
            "$($record.sourceDocumentId)."
        )
    }

    if ($record.baselineStatus -ne "APPROVED_FROZEN") {

        $errors += (
            "ID $($record.requirementId) has invalid baseline status."
        )
    }
}

# -------------------------------------------------------------------------
# DUPLICATES / INTERNAL COUNTS
# -------------------------------------------------------------------------

$duplicatePairs = @(
    $registry.requirements |
    Group-Object requirementId, sourceDocumentId |
    Where-Object {
        $_.Count -gt 1
    }
)

foreach ($duplicate in $duplicatePairs) {

    $errors += (
        "Duplicate requirement/source pair: " +
        $duplicate.Name
    )
}

$uniqueIds = @(
    $registry.requirements.requirementId |
    Sort-Object -Unique
)

if (
    $registry.registryRowCount -ne
    $registry.requirements.Count
) {
    $errors += "registryRowCount mismatch."
}

if (
    $registry.uniqueRequirementCount -ne
    $uniqueIds.Count
) {
    $errors += "uniqueRequirementCount mismatch."
}

# -------------------------------------------------------------------------
# RESULT
# -------------------------------------------------------------------------

if ($errors.Count -gt 0) {

    Write-Host ""
    Write-Host "REQUIREMENT REGISTRY VALIDATION: FAILED" `
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
Write-Host "REQUIREMENT REGISTRY VALIDATION: PASS" `
    -ForegroundColor Green

Write-Host ""
Write-Host "Baseline                    : $($registry.baselineId)"
Write-Host "Status                      : $($registry.status)"
Write-Host "Baseline documents          : 21/21"
Write-Host "Registry rows               : $($registry.requirements.Count)"
Write-Host "Unique source IDs           : $($uniqueIds.Count)"
Write-Host "Documents with source IDs   : $($coveredDocumentIds.Count)/21"
Write-Host "Documents without namespace : 01, 21"
Write-Host ""
Write-Host "ADR-001..ADR-017            : PASS"
Write-Host "AR-NFR-01..AR-NFR-12        : PASS"
Write-Host "AR-AC-01..AR-AC-22          : PASS"
Write-Host "MA-01..MA-22                : PASS"
Write-Host "DM-D01..DM-D60              : PASS"
Write-Host "DM-01..DM-30                : PASS"
Write-Host "DB-AC01..DB-AC16            : PASS"
Write-Host "SYNC-AC01..SYNC-AC20        : PASS"
Write-Host "COM-AC01..COM-AC20          : PASS"
Write-Host "EVD-AC01..EVD-AC20          : PASS"
Write-Host "RPT-AC01..RPT-AC25          : PASS"
Write-Host ""
Write-Host "Excluded test/vector namespaces:"
Write-Host "CE-G / CE-B / COM-T / EVD-T / RPT-T / SEC-T / SYNC-T"
Write-Host ""
Write-Host "Requirement/source taxonomy integrity is valid."

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

$baselineRegistryPath = Join-Path `
    $PSScriptRoot `
    "baseline-registry.json"

$outputJson = Join-Path `
    $PSScriptRoot `
    "requirement-id-registry.json"

$outputCsv = Join-Path `
    $PSScriptRoot `
    "requirement-id-registry.csv"

if (-not (Test-Path $baselineRegistryPath)) {
    throw "Missing baseline registry: $baselineRegistryPath"
}

$baseline = Get-Content `
    -Path $baselineRegistryPath `
    -Raw |
ConvertFrom-Json

if ($baseline.baselineId -ne "SILOMATH_V6") {
    throw "Unexpected baseline ID: $($baseline.baselineId)"
}

if ($baseline.status -ne "APPROVED_FROZEN") {
    throw "Baseline must be APPROVED_FROZEN."
}

if ($baseline.documents.Count -ne 21) {
    throw "Baseline registry must contain exactly 21 documents."
}

# -------------------------------------------------------------------------
# SOURCE REQUIREMENT / DECISION / ACCEPTANCE / RELEASE-GATE IDS
#
# These IDs belong on the left-hand side of the RVM.
#
# Explicitly excluded test/vector namespaces:
#
#   CE-Gxx
#   CE-Bxx
#   COM-Txx
#   EVD-Txx
#   RPT-Txx
#   SEC-Txx
#   SYNC-Txx
#
# They are verification/test identifiers and must not become source
# Requirement/Decision rows.
# -------------------------------------------------------------------------

$patterns = @(

    # Core and final decisions.
    '(?<![A-Z0-9-])D(?:0[1-9]|[12][0-9]|3[0-6])\b',
    '\bFD-(?:0[1-9]|[1-6][0-9]|7[0-5])\b',

    # Business rules.
    '\bBR-[A-Z0-9]+-\d{3}\b',

    # Architecture.
    '\bADR-\d{3}\b',
    '\bAR-NFR-\d{2}\b',
    '\bAR-AC-\d{2}\b',
    '\bFC-\d{2}\b',
    '\bIC-\d{2}\b',

    # Module architecture acceptance criteria.
    '\bMA-\d{2}\b',

    # Data model / physical database.
    '\bDM-D\d{2}\b',
    '\bDM-\d{2}\b',
    '\bDB-D\d{2}\b',
    '\bDB-AC\d{2}\b',

    # API.
    '\bAPI-AC\d{2}\b',

    # Offline / synchronization.
    '\bOS-D\d{2}\b',
    '\bSYNC-AC\d{2}\b',

    # Calculation.
    '\bCE-D\d{2}\b',
    '\bCALC-AC\d{2}\b',

    # Commodity.
    '\bCO-D\d{2}\b',
    '\bCOM-AC\d{2}\b',

    # Evidence.
    '\bEV-D\d{2}\b',
    '\bEVD-AC\d{2}\b',

    # Reporting.
    '\bRP-D\d{2,3}\b',
    '\bRPT-AC\d{2}\b',

    # Security.
    '\bSEC-D\d{3}\b',
    '\bSEC-AC\d{2}\b',

    # Test Strategy requirements/decisions and release gates.
    '\bTA-D\d{3}\b',
    '\bREL-G\d{2}\b',

    # Retention / Production Operations.
    '\bRT-D\d{3}\b',
    '\bPO-D\d{3}\b'
)

$forbiddenRequirementPatterns = @(
    '^CE-G\d{2}$',
    '^CE-B\d{2}$',
    '^COM-T\d{2}$',
    '^EVD-T\d{2}$',
    '^RPT-T\d{2}$',
    '^SEC-T\d{2}$',
    '^SYNC-T\d{2}$'
)

function Get-BaselineText {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $extension = [System.IO.Path]::GetExtension($Path).ToLowerInvariant()

    if ($extension -eq ".md" -or $extension -eq ".txt") {
        return Get-Content -Path $Path -Raw -Encoding UTF8
    }

    if ($extension -ne ".docx") {
        throw "Unsupported baseline document format: $extension ($Path)"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)

    try {
        $entry = $archive.Entries |
            Where-Object { $_.FullName -eq "word/document.xml" } |
            Select-Object -First 1

        if ($null -eq $entry) {
            throw "word/document.xml not found in $Path"
        }

        $stream = $entry.Open()
        $reader = New-Object System.IO.StreamReader($stream)
        try { $xmlText = $reader.ReadToEnd() }
        finally { $reader.Dispose(); $stream.Dispose() }

        [xml] $xml = $xmlText
        $nodes = $xml.SelectNodes("//*[local-name()='t']")
        return ($nodes | ForEach-Object { $_.'#text' }) -join " "
    }
    finally { $archive.Dispose() }
}

$records = @()

foreach ($document in $baseline.documents) {

    $docPath = Join-Path `
        $repoRoot `
        $document.file

    if (-not (Test-Path $docPath)) {
        throw "Baseline file missing: $($document.file)"
    }

    Write-Host "Scanning $($document.id) $($document.name)..."

    $text = Get-BaselineText -Path $docPath

    $foundIds = New-Object `
        System.Collections.Generic.HashSet[string]

    foreach ($pattern in $patterns) {

        $matches = [regex]::Matches(
            $text,
            $pattern,
            [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
        )

        foreach ($match in $matches) {

            [void] $foundIds.Add(
                $match.Value.ToUpperInvariant()
            )
        }
    }

    foreach ($id in ($foundIds | Sort-Object)) {

        $records += [pscustomobject][ordered]@{
            requirementId = $id
            sourceDocumentId = $document.id
            sourceDocument = $document.name
            baselineStatus = "APPROVED_FROZEN"
        }
    }
}

# -------------------------------------------------------------------------
# SAFETY: FALSE STANDALONE Dxx
# -------------------------------------------------------------------------

$falseCoreIds = @(
    $records |
    Where-Object {
        $_.requirementId -match '^D\d{2}$' -and
        [int] $_.requirementId.Substring(1) -gt 36
    }
)

if ($falseCoreIds.Count -gt 0) {

    throw (
        "False standalone Dxx IDs detected: " +
        (
            $falseCoreIds.requirementId |
            Sort-Object -Unique
        ) -join ", "
    )
}

# -------------------------------------------------------------------------
# SAFETY: RANGE-HEADING SHORT FORMS
# -------------------------------------------------------------------------

$forbiddenShortForms = @(
    '^SEC-D\d{2}$',
    '^TA-D\d{2}$',
    '^RT-D\d{2}$',
    '^PO-D\d{2}$'
)

foreach ($pattern in $forbiddenShortForms) {

    $bad = @(
        $records |
        Where-Object {
            $_.requirementId -match $pattern
        }
    )

    if ($bad.Count -gt 0) {
        throw "Forbidden short-form decision IDs detected for $pattern"
    }
}

# -------------------------------------------------------------------------
# SAFETY: TEST/VECTOR IDS MUST NEVER ENTER REQUIREMENT REGISTRY
# -------------------------------------------------------------------------

foreach ($pattern in $forbiddenRequirementPatterns) {

    $bad = @(
        $records |
        Where-Object {
            $_.requirementId -match $pattern
        }
    )

    if ($bad.Count -gt 0) {

        throw (
            "Verification/test IDs entered requirement registry for " +
            "pattern $pattern"
        )
    }
}

# -------------------------------------------------------------------------
# DUPLICATE SOURCE PAIRS
# -------------------------------------------------------------------------

$duplicatePairs = @(
    $records |
    Group-Object requirementId, sourceDocumentId |
    Where-Object {
        $_.Count -gt 1
    }
)

if ($duplicatePairs.Count -gt 0) {
    throw "Duplicate requirement/document pairs detected."
}

$uniqueIds = @(
    $records.requirementId |
    Sort-Object -Unique
)

$registry = [ordered]@{
    baselineId = "SILOMATH_V6"
    status = "APPROVED_FROZEN"
    generatedFrom = "21 frozen baseline documents"
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")

    taxonomy = [ordered]@{

        sourceIdPolicy = (
            "Requirement registry contains normative Requirement, Decision, " +
            "Acceptance Criterion and Release Gate IDs."
        )

        verificationIdPolicy = (
            "Golden vectors, boundary vectors and explicit test-matrix IDs " +
            "are verification/Test IDs and are excluded from source requirement rows."
        )

        excludedVerificationNamespaces = @(
            "CE-G",
            "CE-B",
            "COM-T",
            "EVD-T",
            "RPT-T",
            "SEC-T",
            "SYNC-T"
        )
    }

    normalization = [ordered]@{

        coreDecisionRange = "D01-D36"
        finalDecisionRange = "FD-01-FD-75"

        architectureDecisionRange = "ADR-001-ADR-023"
        architectureRequirementRange = "AR-NFR-01-AR-NFR-12"
        architectureAcceptanceRange = "AR-AC-01-AR-AC-27"

        moduleArchitectureAcceptanceRange = "MA-01-MA-24"

        dataModelDecisionRange = "DM-D01-DM-D64"
        dataModelAcceptanceRange = "DM-01-DM-32"

        securityDecisionCanonicalForm = "SEC-D001-SEC-D149"
        testDecisionCanonicalForm = "TA-D001-TA-D232"
        retentionDecisionCanonicalForm = "RT-D001-RT-D200"
        productionDecisionCanonicalForm = "PO-D001-PO-D293"

        rangeHeadingRule = (
            "SEC-D01-SEC-D149, TA-D01-TA-D232, " +
            "RT-D01-RT-D200 and PO-D01-PO-D293 are range notation only; " +
            "concrete registry rows use canonical three-digit IDs."
        )

        fcIcRule = (
            "FC-* and IC-* are registered only when the concrete ID " +
            "appears explicitly; missing members are not synthesized."
        )

        approvedByProjectOwner = $true
    }

    registryRowCount = $records.Count
    uniqueRequirementCount = $uniqueIds.Count

    requirements = @(
        $records |
        Sort-Object requirementId, sourceDocumentId
    )
}

$registry |
ConvertTo-Json -Depth 12 |
Set-Content `
    -Path $outputJson `
    -Encoding UTF8

$records |
Sort-Object requirementId, sourceDocumentId |
Export-Csv `
    -Path $outputCsv `
    -NoTypeInformation `
    -Encoding UTF8

Write-Host ""
Write-Host "REQUIREMENT REGISTRY BUILD: PASS" `
    -ForegroundColor Green

Write-Host ""
Write-Host "Baseline      : SILOMATH_V6"
Write-Host "Documents     : 21/21"
Write-Host "Registry rows : $($records.Count)"
Write-Host "Unique IDs    : $($uniqueIds.Count)"
Write-Host ""
Write-Host "AR-AC         : included"
Write-Host "MA            : included"
Write-Host "DM acceptance : included"
Write-Host ""
Write-Host "Source requirements/gates only."
Write-Host "Verification/test namespaces excluded:"
Write-Host "CE-G, CE-B, COM-T, EVD-T, RPT-T, SEC-T, SYNC-T"
Write-Host ""
Write-Host "JSON          : $outputJson"
Write-Host "CSV           : $outputCsv"

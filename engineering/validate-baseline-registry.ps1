$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$registryPath = Join-Path $PSScriptRoot "baseline-registry.json"

if (-not (Test-Path $registryPath)) {
    throw "Baseline registry not found: $registryPath"
}

$registry = Get-Content `
    -Path $registryPath `
    -Raw |
ConvertFrom-Json

$errors = New-Object System.Collections.Generic.List[string]

if ($registry.baselineId -ne "SILOMATH_V6") {
    $errors.Add(
        "baselineId must be SILOMATH_V6."
    )
}

if ($registry.status -ne "APPROVED_FROZEN") {
    $errors.Add(
        "Registry status must be APPROVED_FROZEN."
    )
}


if ($registry.refreezeDate -ne "2026-10-29") {
    $errors.Add("refreezeDate must be 2026-10-29.")
}

if ($registry.documentCount -ne 21) {
    $errors.Add(
        "documentCount must be exactly 21."
    )
}

if ($registry.documents.Count -ne 21) {
    $errors.Add(
        "documents array must contain exactly 21 entries."
    )
}

$expectedIds = 1..21 |
ForEach-Object {
    $_.ToString("00")
}

$actualIds = @(
    $registry.documents |
    ForEach-Object {
        $_.id
    }
)

$duplicateIds = @(
    $actualIds |
    Group-Object |
    Where-Object {
        $_.Count -gt 1
    }
)

foreach ($duplicate in $duplicateIds) {
    $errors.Add(
        "Duplicate document ID: $($duplicate.Name)"
    )
}

foreach ($expectedId in $expectedIds) {
    if ($expectedId -notin $actualIds) {
        $errors.Add(
            "Missing document ID: $expectedId"
        )
    }
}

foreach ($document in $registry.documents) {

    if ($document.status -ne "APPROVED_FROZEN") {
        $errors.Add(
            "Document $($document.id) is not APPROVED_FROZEN."
        )
    }

    if ([string]::IsNullOrWhiteSpace($document.name)) {
        $errors.Add(
            "Document $($document.id) has no canonical name."
        )
    }

    if ([string]::IsNullOrWhiteSpace($document.authority)) {
        $errors.Add(
            "Document $($document.id) has no authority classification."
        )
    }

    if ([string]::IsNullOrWhiteSpace($document.file)) {
        $errors.Add(
            "Document $($document.id) has no file path."
        )
    }
    else {
        $documentPath = Join-Path `
            $repoRoot `
            $document.file

        if (-not (Test-Path $documentPath)) {
            $errors.Add(
                "Document file not found: $($document.file)"
            )
        }
        else {
            if ([string]::IsNullOrWhiteSpace($document.sha256)) {
                $errors.Add("Document $($document.id) has no SHA-256.")
            }
            else {
                $actualHash = (Get-FileHash -Path $documentPath -Algorithm SHA256).Hash.ToLowerInvariant()
                if ($actualHash -ne ([string]$document.sha256).ToLowerInvariant()) {
                    $errors.Add("Document hash mismatch: $($document.file)")
                }
            }
        }
    }
}

if (
    $registry.governance.overallProductAuthority -ne
    "01_PRODUCT_BASELINE"
) {
    $errors.Add(
        "Overall product authority must be 01_PRODUCT_BASELINE."
    )
}

if (
    $registry.governance.decisionAuthority -ne
    "02_DECISION_REGISTER"
) {
    $errors.Add(
        "Decision authority must be 02_DECISION_REGISTER."
    )
}

if (
    $registry.governance.uiFunctionalAuthority -ne
    "06_UI_SCREEN_SPECIFICATION_AND_NAVIGATION"
) {
    $errors.Add(
        "UI functional authority must be Document 06."
    )
}

if (
    $registry.governance.uiVisualAuthority -ne
    "21_UI_DESIGN_SYSTEM_FINAL"
) {
    $errors.Add(
        "UI visual authority must be Document 21."
    )
}

if ($errors.Count -gt 0) {

    Write-Host ""
    Write-Host "BASELINE REGISTRY VALIDATION: FAILED" `
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
Write-Host "BASELINE REGISTRY VALIDATION: PASS" `
    -ForegroundColor Green

Write-Host ""
Write-Host "Baseline : $($registry.baselineId)"
Write-Host "Status   : $($registry.status)"
Write-Host "Documents: $($registry.documents.Count)/21"
Write-Host ""
Write-Host "Canonical authority chain:"
Write-Host "  01_PRODUCT_BASELINE"
Write-Host "  02_DECISION_REGISTER"
Write-Host "  Specialist document by domain"
Write-Host ""
Write-Host "All 21 baseline files are present and registered."

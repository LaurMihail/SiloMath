
$ErrorActionPreference = "Stop"

$engineeringRoot = $PSScriptRoot

$evidencePath =
    Join-Path `
        $engineeringRoot `
        "milestone-gates\GATE-M0.md"

$errors = @()

function Add-GateError {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $script:errors += $Message
}

function Invoke-GateValidator {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Name,

        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    if (-not (Test-Path $Path)) {
        Add-GateError "Required validator missing: $Path"
        return
    }

    Write-Host ""
    Write-Host "=== $Name ==="

    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $Path

    if ($LASTEXITCODE -ne 0) {
        Add-GateError (
            "$Name failed with exit code $LASTEXITCODE."
        )
    }
}

# =========================================================================
# GATE EVIDENCE
# =========================================================================

if (-not (Test-Path $evidencePath)) {
    Write-Host "GATE-M0 VALIDATION: FAIL"
    Write-Host " - Gate evidence missing: $evidencePath"
    exit 1
}

$evidence =
    Get-Content `
        -Path $evidencePath `
        -Raw `
        -Encoding UTF8

$requiredEvidencePatterns = @(
    @{
        Pattern = 'Gate ID:\s*GATE-M0'
        Message = 'Gate evidence must identify GATE-M0.'
    },
    @{
        Pattern = 'Milestone:\s*M0'
        Message = 'Gate evidence must identify milestone M0.'
    },
    @{
        Pattern = 'Baseline:\s*SILOMATH_V6'
        Message = 'Gate evidence must identify SILOMATH_V6.'
    },
    @{
        Pattern = 'Gate decision:\s*PASS'
        Message = 'Gate evidence must contain Gate decision: PASS.'
    },
    @{
        Pattern = 'IMP-GOV-001'
        Message = 'Gate evidence must cover IMP-GOV-001.'
    },
    @{
        Pattern = 'IMP-GOV-002'
        Message = 'Gate evidence must cover IMP-GOV-002.'
    },
    @{
        Pattern = 'IMP-GOV-003'
        Message = 'Gate evidence must cover IMP-GOV-003.'
    },
    @{
        Pattern = 'IMP-GOV-004'
        Message = 'Gate evidence must cover IMP-GOV-004.'
    },
    @{
        Pattern = 'IMP-GOV-005'
        Message = 'Gate evidence must cover IMP-GOV-005.'
    },
    @{
        Pattern = 'No known blocking S0 or S1 defect'
        Message = 'Gate evidence must contain the scoped S0/S1 assessment.'
    },
    @{
        Pattern = 'does not claim that the entire unfinished product'
        Message = 'Gate evidence must limit the S0/S1 assertion to M0 scope.'
    },
    @{
        Pattern = 'does NOT mean'
        Message = 'Gate evidence must state the boundary of the M0 decision.'
    }
)

foreach ($requirement in $requiredEvidencePatterns) {
    if ($evidence -notmatch $requirement.Pattern) {
        Add-GateError $requirement.Message
    }
}

# =========================================================================
# REQUIRED GOVERNANCE ARTIFACTS
# =========================================================================

$requiredArtifacts = @(
    "baseline-registry.json",
    "requirement-id-registry.json",
    "rvm.json",
    "adr-register.json",
    "baseline-change-register.json",
    "adr\README.md",
    "adr\IADR_TEMPLATE.md",
    "baseline-change\README.md",
    "baseline-change\CR_TEMPLATE.md"
)

foreach ($relativePath in $requiredArtifacts) {
    $fullPath =
        Join-Path `
            $engineeringRoot `
            $relativePath

    if (-not (Test-Path $fullPath)) {
        Add-GateError (
            "Required M0 governance artifact missing: $relativePath"
        )
    }
}

if ($errors.Count -gt 0) {
    Write-Host "GATE-M0 VALIDATION: FAIL"

    foreach ($errorMessage in $errors) {
        Write-Host " - $errorMessage"
    }

    exit 1
}

# =========================================================================
# EXECUTABLE M0 VALIDATION
# =========================================================================

Invoke-GateValidator `
    -Name "Baseline Registry" `
    -Path (
        Join-Path `
            $engineeringRoot `
            "validate-baseline-registry.ps1"
    )

Invoke-GateValidator `
    -Name "Requirement Registry" `
    -Path (
        Join-Path `
            $engineeringRoot `
            "validate-requirement-registry.ps1"
    )

Invoke-GateValidator `
    -Name "RVM" `
    -Path (
        Join-Path `
            $engineeringRoot `
            "validate-rvm.ps1"
    )

Invoke-GateValidator `
    -Name "ADR Process" `
    -Path (
        Join-Path `
            $engineeringRoot `
            "validate-adr-process.ps1"
    )

Invoke-GateValidator `
    -Name "Baseline Change Process" `
    -Path (
        Join-Path `
            $engineeringRoot `
            "validate-baseline-change-process.ps1"
    )

Invoke-GateValidator `
    -Name "PostgreSQL V6 Contract" `
    -Path (
        Join-Path `
            $engineeringRoot `
            "validate-postgres-v6-contract.ps1"
    )

# =========================================================================
# RESULT
# =========================================================================

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "GATE-M0 VALIDATION: FAIL"

    foreach ($errorMessage in $errors) {
        Write-Host " - $errorMessage"
    }

    exit 1
}

Write-Host ""
Write-Host "GATE-M0 VALIDATION: PASS"
Write-Host "Milestone: M0"
Write-Host "Gate: Baseline Controlled"
Write-Host "Predecessor governance artifacts: PRESENT"
Write-Host "Applicable M0 validators: PASS"
Write-Host "Scoped known blocking S0/S1 defects: NONE IDENTIFIED"
Write-Host "Production release readiness claimed: NO"

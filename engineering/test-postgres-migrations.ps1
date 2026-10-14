$ErrorActionPreference = "Stop"

$LegacyBoundary = "20260928124803"

$BootstrapSource = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\000_legacy_prebaseline_bootstrap.sql"

$TemporaryBootstrapMigration = Join-Path `
    $PSScriptRoot `
    "..\supabase\migrations\20260901000000_test_legacy_prebaseline_bootstrap.sql"

$LegacyFixture = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\001_legacy_campaign_fixture.sql"

$V6Assertions = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\002_v6_assertions.sql"

function Write-Section {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Title
    )

    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Title
    Write-Host "============================================================"
}

function Assert-LastExitCode {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Description
    )

    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }
}

function Invoke-SupabaseSqlFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Description
    )

    if (-not (Test-Path $Path)) {
        throw "Required SQL file not found: $Path"
    }

    Get-Content `
        -Path $Path `
        -Raw |
        supabase db query --local

    Assert-LastExitCode $Description
}

if (-not (Get-Command "supabase" -ErrorAction SilentlyContinue)) {
    throw "Supabase CLI was not found on PATH."
}

if (-not (Test-Path $BootstrapSource)) {
    throw "Legacy prebaseline bootstrap file not found: $BootstrapSource"
}

if (-not (Test-Path $LegacyFixture)) {
    throw "Legacy campaign fixture not found: $LegacyFixture"
}

if (-not (Test-Path $V6Assertions)) {
    throw "V6 assertions file not found: $V6Assertions"
}

if (Test-Path $TemporaryBootstrapMigration) {
    throw "Temporary bootstrap migration already exists: $TemporaryBootstrapMigration"
}

try {
    Write-Section "Install test-only legacy prebaseline bootstrap"

    Copy-Item `
        -Path $BootstrapSource `
        -Destination $TemporaryBootstrapMigration `
        -Force

    Write-Host "Temporary migration:"
    Write-Host $TemporaryBootstrapMigration

    Write-Section "Start local Supabase"

    supabase start
    Assert-LastExitCode "Start local Supabase"

    Write-Section "Reset database to retained legacy migration boundary"

    supabase db reset `
        --local `
        --version $LegacyBoundary

    Assert-LastExitCode "Reset database to legacy migration boundary"

    Write-Section "Load representative populated legacy Campaign fixture"

    Invoke-SupabaseSqlFile `
        -Path $LegacyFixture `
        -Description "Load legacy Campaign fixture"

    Write-Section "Apply SiloMath V6 PostgreSQL migrations"

    supabase migration up --local
    Assert-LastExitCode "Apply V6 PostgreSQL migrations"

    Write-Section "Assert canonical V6 migration result"

    Invoke-SupabaseSqlFile `
        -Path $V6Assertions `
        -Description "Run V6 PostgreSQL migration assertions"

    Write-Section "PostgreSQL migration regression PASS"

    Write-Host "Legacy boundary : $LegacyBoundary"
    Write-Host "Bootstrap       : TEST-ONLY GAP-PG-MIG-003 reconstruction"
    Write-Host "Legacy fixture  : PASS"
    Write-Host "V6 migrations   : PASS"
    Write-Host "V6 assertions   : PASS"
}
finally {
    Write-Section "Cleanup"

    if (Test-Path $TemporaryBootstrapMigration) {
        Remove-Item `
            -Path $TemporaryBootstrapMigration `
            -Force

        Write-Host "Removed temporary bootstrap migration."
    }

    try {
        supabase stop --no-backup
    }
    catch {
        Write-Warning "Supabase cleanup failed: $($_.Exception.Message)"
    }
}

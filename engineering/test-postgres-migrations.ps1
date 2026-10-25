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

$TenantIsolationAssertions = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\003_v6_tenant_isolation_assertions.sql"

$PrimaryForeignKeyAssertions = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\004_v6_primary_foreign_key_assertions.sql"

$UniqueConstraintAssertions = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\005_v6_unique_constraint_assertions.sql"

$CheckConstraintAssertions = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\006_v6_check_constraint_assertions.sql"

$IndexStrategyAssertions = Join-Path `
    $PSScriptRoot `
    "..\supabase\tests\migration\007_v6_index_strategy_assertions.sql"

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

if (-not (Test-Path $TenantIsolationAssertions)) {
    throw "Tenant-isolation assertions file not found: $TenantIsolationAssertions"
}

if (-not (Test-Path $PrimaryForeignKeyAssertions)) {
    throw "Primary/foreign-key assertions file not found: $PrimaryForeignKeyAssertions"
}

if (-not (Test-Path $UniqueConstraintAssertions)) {
    throw "Unique-constraint assertions file not found: $UniqueConstraintAssertions"
}

if (-not (Test-Path $CheckConstraintAssertions)) {
    throw "CHECK-constraint assertions file not found: $CheckConstraintAssertions"
}

if (-not (Test-Path $IndexStrategyAssertions)) {
    throw "Index-strategy assertions file not found: $IndexStrategyAssertions"
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

    Write-Section "Assert V6 tenant-isolation key model"

    Invoke-SupabaseSqlFile `
        -Path $TenantIsolationAssertions `
        -Description "Run V6 DB002 tenant-isolation assertions"

    Write-Section "Assert V6 primary/foreign-key model"

    Invoke-SupabaseSqlFile `
        -Path $PrimaryForeignKeyAssertions `
        -Description "Run V6 DB003 primary/foreign-key assertions"

    Write-Section "Assert V6 unique-constraint model"

    Invoke-SupabaseSqlFile `
        -Path $UniqueConstraintAssertions `
        -Description "Run V6 DB004 unique-constraint assertions"

    Write-Section "Assert V6 CHECK-constraint model"

    Invoke-SupabaseSqlFile `
        -Path $CheckConstraintAssertions `
        -Description "Run V6 DB005 CHECK-constraint assertions"

    Write-Section "Assert V6 index strategy"

    Invoke-SupabaseSqlFile `
        -Path $IndexStrategyAssertions `
        -Description "Run V6 DB006 index-strategy assertions"

    Write-Section "PostgreSQL migration regression PASS"

    Write-Host "Legacy boundary   : $LegacyBoundary"
    Write-Host "Bootstrap         : TEST-ONLY GAP-PG-MIG-003 reconstruction"
    Write-Host "Legacy fixture    : PASS"
    Write-Host "V6 migrations     : PASS"
    Write-Host "V6 assertions     : PASS"
    Write-Host "DB002 tenant keys : PASS"
    Write-Host "DB003 PK/FK       : PASS"
    Write-Host "DB004 unique keys : PASS"
    Write-Host "DB005 CHECKs      : PASS"
    Write-Host "DB006 indexes     : PASS"
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

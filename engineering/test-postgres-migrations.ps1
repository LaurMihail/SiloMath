$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Description,

        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Description
    Write-Host "============================================================"

    & $Command

    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }
}

if ($null -eq (Get-Command supabase -ErrorAction SilentlyContinue)) {
    throw "Supabase CLI is required."
}

if ($null -eq (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is required by local Supabase."
}

$fixture = Join-Path `
    $repoRoot `
    "supabase\tests\migration\001_legacy_campaign_fixture.sql"

$assertions = Join-Path `
    $repoRoot `
    "supabase\tests\migration\002_v6_assertions.sql"

if (-not (Test-Path $fixture)) {
    throw "Missing fixture SQL: $fixture"
}

if (-not (Test-Path $assertions)) {
    throw "Missing assertion SQL: $assertions"
}

Invoke-Checked `
    -Description "Start local Supabase" `
    -Command {
        supabase start
    }

Invoke-Checked `
    -Description "Reset database to legacy migration boundary" `
    -Command {
        supabase db reset `
            --local `
            --version 20260928124803
    }

Invoke-Checked `
    -Description "Load representative populated legacy data" `
    -Command {
        Get-Content -Raw $fixture |
            supabase db query --local
    }

Invoke-Checked `
    -Description "Apply remaining migrations including V6" `
    -Command {
        supabase migration up --local
    }

Invoke-Checked `
    -Description "Run V6 post-migration assertions" `
    -Command {
        Get-Content -Raw $assertions |
            supabase db query --local
    }

Write-Host ""
Write-Host "POSTGRES MIGRATION TEST: PASS"

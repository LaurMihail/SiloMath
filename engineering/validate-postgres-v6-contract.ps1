$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$migrationsPath = Join-Path $repoRoot "supabase\migrations"

if (-not (Test-Path $migrationsPath)) {
    throw "Missing supabase/migrations directory."
}

$errors = New-Object System.Collections.Generic.List[string]

$v6Boundary = "20261015000000"

$sqlFiles = @(
    Get-ChildItem `
        -Path $migrationsPath `
        -File `
        -Filter "*.sql" |
    Sort-Object Name
)

$v6Files = @(
    $sqlFiles | Where-Object {
        $_.BaseName -ge $v6Boundary
    }
)

Write-Host ""
Write-Host "POSTGRES V6 CONTRACT VALIDATION"
Write-Host ""
Write-Host "Migration files total   : $($sqlFiles.Count)"
Write-Host "V6 boundary             : $v6Boundary"
Write-Host "V6 migration files      : $($v6Files.Count)"

if ($v6Files.Count -eq 0) {
    $errors.Add(
        "No V6 PostgreSQL migration exists at or after boundary $v6Boundary."
    )
}

$forbiddenPatterns = @(
    @{
        Pattern = "'Handover / Takeover'"
        Message = "Forbidden V6 Inventory Type: Handover / Takeover"
    },
    @{
        Pattern = "'Standard User'"
        Message = "Legacy role Standard User must not appear in V6 canonical SQL"
    }
)

foreach ($file in $v6Files) {
    $lines = Get-Content -Path $file.FullName

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        foreach ($rule in $forbiddenPatterns) {
            if ($line.Contains($rule.Pattern)) {
                $relativePath = $file.FullName.Substring($repoRoot.Length + 1)
                $lineNumber = $i + 1

                $errors.Add(
                    "${relativePath}:${lineNumber}: $($rule.Message) :: $($line.Trim())"
                )
            }
        }
    }
}

$allV6Sql = (
    $v6Files |
    ForEach-Object {
        Get-Content -Path $_.FullName -Raw
    }
) -join "`n"

$requiredPatterns = @(
    @{
        Pattern = '(?i)create\s+schema\s+(if\s+not\s+exists\s+)?silomath'
        Message = "Canonical silomath schema is not established."
    },
    @{
        Pattern = '(?i)silomath\.inventory_campaigns'
        Message = "Canonical inventory_campaigns table is missing."
    },
    @{
        Pattern = '(?i)silomath\.campaign_scopes'
        Message = "Canonical campaign_scopes table is missing."
    },
    @{
        Pattern = '(?i)silomath\.campaign_team_assignments'
        Message = "Canonical campaign_team_assignments table is missing."
    },
    @{
        Pattern = "inventory_type\s+in\s+\('ANNUAL',\s*'MONTHLY',\s*'CONTROL'\)"
        Message = "Canonical Inventory Type constraint is missing."
    },
    @{
        Pattern = "status\s+in\s+\('ACTIVE',\s*'COMPLETED'\)"
        Message = "Canonical Campaign status constraint is missing."
    },
    @{
        Pattern = "case\s+c\.inventory_type"
        Message = "Legacy-to-V6 Inventory Type migration mapping is missing."
    },
    @{
        Pattern = "case\s+c\.status"
        Message = "Legacy-to-V6 Campaign status migration mapping is missing."
    },
    @{
        Pattern = "V6 migration blocked"
        Message = "Migration precondition guards are missing."
    },
    @{
        Pattern = "V6 migration verification failed"
        Message = "Post-migration verification guards are missing."
    },
    @{
        Pattern = '(?i)silomath\.legacy_campaign_snapshot_provenance'
        Message = "Controlled legacy snapshot provenance staging is missing."
    },
    @{
        Pattern = "'NEEDS_PROVENANCE'"
        Message = "Legacy snapshot NEEDS_PROVENANCE state is missing."
    },
    @{
        Pattern = "snapshot_schema_version\s+text\s+null"
        Message = "Snapshot provenance schema-version field is missing."
    },
    @{
        Pattern = "engine_version\s+text\s+null"
        Message = "Snapshot provenance engine-version field is missing."
    },
    @{
        Pattern = "input_schema_version\s+text\s+null"
        Message = "Snapshot provenance input-schema-version field is missing."
    },
    @{
        Pattern = "normalization_version\s+text\s+null"
        Message = "Snapshot provenance normalization-version field is missing."
    },
    @{
        Pattern = "surface_model_version\s+text\s+null"
        Message = "Snapshot provenance surface-model-version field is missing."
    },
    @{
        Pattern = "aggregation_method\s+text\s+null"
        Message = "Snapshot provenance aggregation-method field is missing."
    },
    @{
        Pattern = "aggregation_rule_version\s+text\s+null"
        Message = "Snapshot provenance aggregation-rule-version field is missing."
    }
)

foreach ($rule in $requiredPatterns) {
    if ($allV6Sql -notmatch $rule.Pattern) {
        $errors.Add($rule.Message)
    }
}

Write-Host "Violations found        : $($errors.Count)"

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "POSTGRES V6 CONTRACT VALIDATION: FAIL"
    Write-Host ""

    foreach ($errorLine in $errors) {
        Write-Host " - $errorLine"
    }

    exit 1
}

Write-Host ""
Write-Host "POSTGRES V6 CONTRACT VALIDATION: PASS"
exit 0

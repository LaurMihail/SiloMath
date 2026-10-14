$ErrorActionPreference = "Stop"

$registryPath = Join-Path `
    $PSScriptRoot `
    "requirement-id-registry.json"

$mappingsPath = Join-Path `
    $PSScriptRoot `
    "rvm-mappings.json"

$outputJson = Join-Path `
    $PSScriptRoot `
    "rvm.json"

$outputCsv = Join-Path `
    $PSScriptRoot `
    "rvm.csv"

if (-not (Test-Path $registryPath)) {
    throw "Requirement registry not found: $registryPath"
}

if (-not (Test-Path $mappingsPath)) {
    throw "RVM mapping source not found: $mappingsPath"
}

$registry = Get-Content `
    -Path $registryPath `
    -Raw |
ConvertFrom-Json

$mappingsSource = Get-Content `
    -Path $mappingsPath `
    -Raw |
ConvertFrom-Json

if ($registry.baselineId -ne "SILOMATH_V6") {
    throw "Requirement registry does not belong to SILOMATH_V6."
}

if ($mappingsSource.baselineId -ne "SILOMATH_V6") {
    throw "RVM mapping source does not belong to SILOMATH_V6."
}

$registryIds = @(
    $registry.requirements.requirementId |
    Sort-Object -Unique
)

$mappingGroups = @{}

foreach ($mapping in $mappingsSource.mappings) {

    $id = [string] $mapping.requirementDecisionId

    if ([string]::IsNullOrWhiteSpace($id)) {
        throw "Mapping contains empty requirementDecisionId."
    }

    if ($id -notin $registryIds) {
        throw "Mapping references unknown Requirement/Decision ID: $id"
    }

    if (-not $mappingGroups.ContainsKey($id)) {
        $mappingGroups[$id] = @()
    }

    $mappingGroups[$id] += $mapping
}

$groupedRegistry = @(
    $registry.requirements |
    Group-Object requirementId |
    Sort-Object Name
)

$rows = @()

foreach ($group in $groupedRegistry) {

    $requirementId = $group.Name

    $sources = @(
        $group.Group |
        Sort-Object sourceDocumentId |
        ForEach-Object {
            "$($_.sourceDocumentId)_$($_.sourceDocument)"
        }
    )

    if ($mappingGroups.ContainsKey($requirementId)) {

        $mappingSet = @(
            $mappingGroups[$requirementId]
        )

        $componentModules = @(
            $mappingSet.componentModule |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $testIds = @(
            $mappingSet |
            ForEach-Object {
                @($_.testIds)
            } |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $testLevels = @(
            $mappingSet.testLevel |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $automationValues = @(
            $mappingSet.automation |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $environments = @(
            $mappingSet.environment |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $results = @(
            $mappingSet.result |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $evidenceValues = @(
            $mappingSet.evidence |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        $buildVersions = @(
            $mappingSet.buildVersion |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            } |
            Sort-Object -Unique
        )

        if ($results.Count -eq 0) {
            $effectiveResult = "Not Run"
        }
        elseif ($results.Count -eq 1) {
            $effectiveResult = $results[0]
        }
        elseif ("Fail" -in $results) {
            $effectiveResult = "Fail"
        }
        elseif ("Blocked" -in $results) {
            $effectiveResult = "Blocked"
        }
        elseif ("Not Run" -in $results) {
            $effectiveResult = "Not Run"
        }
        elseif (
            $results.Count -eq 1 -and
            $results[0] -eq "Pass"
        ) {
            $effectiveResult = "Pass"
        }
        else {
            $effectiveResult = "Not Run"
        }

        $hasComponent = $componentModules.Count -gt 0
        $hasTest = $testIds.Count -gt 0

        $hasEvidence =
            $evidenceValues.Count -gt 0 -and
            $buildVersions.Count -gt 0

        if (
            $hasComponent -and
            $hasTest -and
            $effectiveResult -eq "Pass" -and
            $hasEvidence
        ) {
            $mappingStatus = "VERIFIED"
        }
        elseif ($hasComponent -and $hasTest) {
            $mappingStatus = "MAPPED"
        }
        else {
            $mappingStatus = "UNMAPPED"
        }

        $row = [pscustomobject][ordered]@{
            requirementDecisionId = $requirementId
            sourceDocumentVersion = ($sources -join "; ")
            componentModule = (
                $componentModules -join "; "
            )
            testIds = (
                $testIds -join "; "
            )
            testLevel = (
                $testLevels -join "; "
            )
            automation = (
                $automationValues -join "; "
            )
            environment = (
                $environments -join "; "
            )
            result = $effectiveResult
            evidence = if ($evidenceValues.Count -gt 0) {
                $evidenceValues -join "; "
            }
            else {
                $null
            }
            buildVersion = if ($buildVersions.Count -gt 0) {
                $buildVersions -join "; "
            }
            else {
                $null
            }
            mappingStatus = $mappingStatus
        }
    }
    else {

        $row = [pscustomobject][ordered]@{
            requirementDecisionId = $requirementId
            sourceDocumentVersion = ($sources -join "; ")
            componentModule = $null
            testIds = $null
            testLevel = $null
            automation = $null
            environment = $null
            result = "Not Run"
            evidence = $null
            buildVersion = $null
            mappingStatus = "UNMAPPED"
        }
    }

    $rows += $row
}

$mappedCount = @(
    $rows |
    Where-Object {
        $_.mappingStatus -eq "MAPPED"
    }
).Count

$verifiedCount = @(
    $rows |
    Where-Object {
        $_.mappingStatus -eq "VERIFIED"
    }
).Count

$unmappedCount = @(
    $rows |
    Where-Object {
        $_.mappingStatus -eq "UNMAPPED"
    }
).Count

$rvm = [ordered]@{
    baselineId = "SILOMATH_V6"
    status = "ACTIVE_TRACEABILITY"
    schemaAuthority = "18_TEST_STRATEGY_ACCEPTANCE"
    generatedFrom = @(
        "engineering/requirement-id-registry.json",
        "engineering/rvm-mappings.json"
    )
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    requirementCount = $rows.Count

    referenceConvention = [ordered]@{
        id = "RVM-REF-001"
        automatedTestReference = (
            "<relative-file-path>#<test-function-name>"
        )
        componentModuleMeaning = (
            "Production implementation responsibility, not test file location."
        )
    }

    policy = [ordered]@{
        noInventedEvidence = $true
        noSemanticAutoMapping = $true
        unmappedRequirementsAllowedDuringImplementation = $true
        criticalUnmappedRequirementsBlockRelease = $true

        validResults = @(
            "Pass",
            "Fail",
            "Blocked",
            "Not Run"
        )

        validTestLevels = @(
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

        validMappingStatuses = @(
            "UNMAPPED",
            "MAPPED",
            "VERIFIED"
        )
    }

    summary = [ordered]@{
        total = $rows.Count
        mapped = $mappedCount
        verified = $verifiedCount
        unmapped = $unmappedCount
    }

    requirements = $rows
}

$rvm |
ConvertTo-Json -Depth 12 |
Set-Content `
    -Path $outputJson `
    -Encoding UTF8

$rows |
Export-Csv `
    -Path $outputCsv `
    -NoTypeInformation `
    -Encoding UTF8

Write-Host ""
Write-Host "RVM BUILD: PASS" `
    -ForegroundColor Green

Write-Host ""
Write-Host "Baseline        : SILOMATH_V6"
Write-Host "Unique RVM rows : $($rows.Count)"
Write-Host "Mapped          : $mappedCount"
Write-Host "Verified        : $verifiedCount"
Write-Host "Unmapped        : $unmappedCount"
Write-Host "JSON            : $outputJson"
Write-Host "CSV             : $outputCsv"
Write-Host ""
Write-Host "No evidence was invented."
Write-Host "Mapping source  : engineering/rvm-mappings.json"

$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$settingsPath = Join-Path $repositoryRoot "settings.gradle.kts"

$expectedModules = @(
    ":app",
    ":core:model",
    ":core:common",
    ":core:domain",
    ":core:database",
    ":core:network",
    ":core:sync",
    ":core:designsystem",
    ":core:navigation",
    ":core:testing",
    ":feature:auth",
    ":feature:dashboard",
    ":feature:masterdata",
    ":feature:campaigns",
    ":feature:stocktaking",
    ":feature:close",
    ":feature:reports",
    ":feature:administration",
    ":calculation-engine"
)

$violations = New-Object System.Collections.Generic.List[string]
$checkedDependencyCount = 0
$checkedSourceFileCount = 0

function Add-Violation {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $violations.Add($Message)
}

function Get-ModuleDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Module
    )

    $relative = $Module.TrimStart(":").Replace(
        ":",
        [string][IO.Path]::DirectorySeparatorChar
    )

    return Join-Path $repositoryRoot $relative
}

if (-not (Test-Path $settingsPath)) {
    throw "settings.gradle.kts was not found at repository root."
}

$settingsText = Get-Content `
    -Path $settingsPath `
    -Raw `
    -Encoding UTF8

$modulePattern =
    '["''](?<module>:[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+)*)["'']'

$declaredModules = @(
    [regex]::Matches(
        $settingsText,
        $modulePattern
    ) |
    ForEach-Object {
        $_.Groups["module"].Value
    } |
    Sort-Object -Unique
)

$expectedSorted = @(
    $expectedModules |
    Sort-Object
)

$declaredSorted = @(
    $declaredModules |
    Sort-Object
)

$missingModules = @(
    $expectedSorted |
    Where-Object {
        $_ -notin $declaredSorted
    }
)

$unexpectedModules = @(
    $declaredSorted |
    Where-Object {
        $_ -notin $expectedSorted
    }
)

foreach ($module in $missingModules) {
    Add-Violation "Missing frozen production module: $module"
}

foreach ($module in $unexpectedModules) {
    Add-Violation "Unexpected production module: $module"
}

if ($declaredSorted.Count -ne $expectedSorted.Count) {
    Add-Violation (
        "Frozen module count mismatch. Expected {0}; found {1}." -f
        $expectedSorted.Count,
        $declaredSorted.Count
    )
}

$dependencyPattern =
    '(?ms)(?<configuration>[A-Za-z][A-Za-z0-9]*)\s*\(\s*project\s*\(\s*(?:path\s*=\s*)?["''](?<target>:[^"'']+)["'']\s*\)\s*\)'

foreach ($sourceModule in $expectedModules) {
    $moduleDirectory =
        Get-ModuleDirectory `
            -Module $sourceModule

    $buildFile =
        Join-Path `
            $moduleDirectory `
            "build.gradle.kts"

    if (-not (Test-Path $buildFile)) {
        Add-Violation "Missing build.gradle.kts for frozen module: $sourceModule"
        continue
    }

    $buildText = Get-Content `
        -Path $buildFile `
        -Raw `
        -Encoding UTF8

    $dependencyMatches = [regex]::Matches(
        $buildText,
        $dependencyPattern
    )

    foreach ($match in $dependencyMatches) {
        $checkedDependencyCount++

        $configuration =
            $match.Groups["configuration"].Value

        $targetModule =
            $match.Groups["target"].Value

        if ($targetModule -notin $expectedModules) {
            Add-Violation (
                "$sourceModule -> $targetModule uses an unknown/non-frozen production module " +
                "in configuration '$configuration'."
            )

            continue
        }

        if (
            $sourceModule -ne ":app" -and
            $targetModule -eq ":app"
        ) {
            Add-Violation (
                "$sourceModule -> :app is forbidden. " +
                ":app is the composition root and must not be depended upon."
            )
        }

        if (
            $sourceModule.StartsWith(":core:") -and
            $targetModule.StartsWith(":feature:")
        ) {
            Add-Violation (
                "$sourceModule -> $targetModule is forbidden. " +
                "Core must never depend on Feature."
            )
        }

        if (
            $sourceModule.StartsWith(":feature:") -and
            $targetModule.StartsWith(":feature:")
        ) {
            Add-Violation (
                "$sourceModule -> $targetModule is forbidden. " +
                "Feature implementation must not depend on another Feature implementation."
            )
        }

        if (
            $sourceModule -eq ":core:database" -and
            $targetModule -eq ":core:network"
        ) {
            Add-Violation (
                ":core:database -> :core:network is forbidden."
            )
        }

        if (
            $sourceModule -eq ":core:network" -and
            $targetModule -eq ":core:database"
        ) {
            Add-Violation (
                ":core:network -> :core:database is forbidden."
            )
        }

        if ($sourceModule -eq ":calculation-engine") {
            Add-Violation (
                ":calculation-engine must remain isolated pure Kotlin/JVM; " +
                "project dependency on $targetModule was found."
            )
        }
    }
}

$calculationBuildPath =
    Join-Path `
        (Get-ModuleDirectory -Module ":calculation-engine") `
        "build.gradle.kts"

if (Test-Path $calculationBuildPath) {
    $calculationBuildText = Get-Content `
        -Path $calculationBuildPath `
        -Raw `
        -Encoding UTF8

    $forbiddenCalculationPatterns = @(
        @{
            Pattern = 'com\.android\.'
            Description = "Android Gradle plugin"
        },
        @{
            Pattern = 'androidx\.'
            Description = "AndroidX dependency"
        },
        @{
            Pattern = '\broom\b'
            Description = "Room dependency"
        },
        @{
            Pattern = '\bretrofit\b'
            Description = "Retrofit dependency"
        },
        @{
            Pattern = '\bhilt\b'
            Description = "Hilt dependency"
        },
        @{
            Pattern = 'ktor-client-android'
            Description = "Android Ktor client dependency"
        },
        @{
            Pattern = '\bwork-runtime\b'
            Description = "WorkManager dependency"
        }
    )

    foreach ($rule in $forbiddenCalculationPatterns) {
        if ($calculationBuildText -match $rule.Pattern) {
            Add-Violation (
                ":calculation-engine must remain pure Kotlin/JVM. " +
                "Forbidden $($rule.Description) detected."
            )
        }
    }
}

$coreDirectories = Get-ChildItem `
    -Path (Join-Path $repositoryRoot "core") `
    -Directory `
    -ErrorAction Stop

foreach ($coreDirectory in $coreDirectories) {
    $sourceRoot =
        Join-Path `
            $coreDirectory.FullName `
            "src"

    if (-not (Test-Path $sourceRoot)) {
        continue
    }

    $kotlinFiles = Get-ChildItem `
        -Path $sourceRoot `
        -Filter "*.kt" `
        -Recurse `
        -File

    foreach ($file in $kotlinFiles) {
        $checkedSourceFileCount++

        $matches = Select-String `
            -Path $file.FullName `
            -Pattern '^\s*import\s+com\.silomath\.feature\.' `
            -CaseSensitive:$false

        foreach ($match in $matches) {
            $relativePath =
                $file.FullName.Substring(
                    $repositoryRoot.Length
                ).TrimStart(
                    [IO.Path]::DirectorySeparatorChar,
                    [IO.Path]::AltDirectorySeparatorChar
                )

            Add-Violation (
                "Core source imports Feature code: " +
                "${relativePath}:$($match.LineNumber)"
            )
        }
    }
}

Write-Host ""
Write-Host "MODULE DEPENDENCY VALIDATION"
Write-Host ""
Write-Host "Frozen modules expected : $($expectedModules.Count)"
Write-Host "Frozen modules declared : $($declaredSorted.Count)"
Write-Host "Project dependencies    : $checkedDependencyCount"
Write-Host "Core source files       : $checkedSourceFileCount"
Write-Host "Violations found        : $($violations.Count)"
Write-Host ""

if ($violations.Count -gt 0) {
    Write-Host "VIOLATIONS:"

    foreach ($violation in $violations) {
        Write-Host " - $violation"
    }

    Write-Host ""
    Write-Host "MODULE DEPENDENCY VALIDATION: FAIL"
    exit 1
}

Write-Host "MODULE DEPENDENCY VALIDATION: PASS"
Write-Host "Frozen module graph: 19/19"
Write-Host "Core -> Feature: PROHIBITED"
Write-Host "Feature -> Feature: PROHIBITED"
Write-Host "Database <-> Network direct dependency: PROHIBITED"
Write-Host "Calculation Engine: PURE KOTLIN/JVM"
exit 0
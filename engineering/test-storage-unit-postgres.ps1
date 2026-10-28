param(
    [Parameter(Mandatory=$true)][string]$DatabaseName,
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin'
)
$ErrorActionPreference='Stop'
# Existing dedicated loopback cluster ONLY. Creates a NEW test database; never resets/drops/deletes.
if($DatabaseName -notmatch '^silomath_md003_[a-z0-9_]+$'){throw 'Dedicated MD003 database name required'}
$repo=Split-Path -Parent $PSScriptRoot
$argsBase=@('-h','127.0.0.1','-p','55483','-U','md003_test','-X','-v','ON_ERROR_STOP=1')
& (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p 55483 -U md003_test $DatabaseName
if($LASTEXITCODE -ne 0){throw 'New isolated database creation failed; no reset attempted'}
function Apply-Sql([string]$path){
    & (Join-Path $PostgresBin 'psql.exe') @argsBase -d $DatabaseName -f $path
    if($LASTEXITCODE -ne 0){throw "Failed SQL: $path"}
}
Apply-Sql "$repo\supabase\tests\migration\000_standalone_postgres_infrastructure.sql"
Apply-Sql "$repo\supabase\tests\migration\000_legacy_prebaseline_bootstrap.sql"
$files=@(Get-ChildItem "$repo\supabase\migrations" -Filter '*.sql' -File | Sort-Object Name)
foreach($file in $files){
    if($file.Name -ge '20261025001000' -and -not $script:referenceLoaded){
        Apply-Sql "$repo\supabase\tests\migration\011_md003_historical_reference_fixture.sql"
        $script:referenceLoaded=$true
    }
    Apply-Sql $file.FullName
    if($file.Name.StartsWith('20260928124803')){Apply-Sql "$repo\supabase\tests\migration\001_legacy_campaign_fixture.sql"}
}
foreach($name in @('002_v6_assertions.sql','003_v6_tenant_isolation_assertions.sql','004_v6_primary_foreign_key_assertions.sql')){
    Apply-Sql "$repo\supabase\tests\migration\$name"
}
Apply-Sql "$repo\supabase\tests\migration\012_md003_canonical_integrity_assertions.sql"
Write-Host "MD003 POSTGRES REPLAY PASS: $($files.Count) migrations; $DatabaseName"

<#
.SYNOPSIS
    Generate the `INSERT IGNORE INTO county_reference (...) VALUES ...;` block
    for migration V076, from US Census Bureau source data.

.DESCRIPTION
    Sources (retrieved 2026-07-30), both pipe (|) delimited despite the
    "Gazetteer" name:
      - County FIPS / name / state -- the Census Gazetteer county file:
        2025_Gaz_counties_national.zip
        https://www2.census.gov/geo/docs/maps-data/data/gazetteer/2025_Gazetteer/2025_Gaz_counties_national.zip
      - ZCTA5 -> county relationship, 2020 vintage:
        tab20_zcta520_county20_natl.txt
        https://www2.census.gov/geo/docs/maps-data/data/rel2020/zcta520/tab20_zcta520_county20_natl.txt

    Selection rule (area-ratio containment, B-Phase-1b): for each ZCTA-county
    pair in the relationship file, containment = AREALAND_PART / AREALAND_ZCTA5_20
    (a ZCTA lying entirely within one county scores 1.0; 0 if AREALAND_ZCTA5_20
    is zero/missing or the ZCTA only touches the county by water). For each
    county, the representative ZIP is the ZCTA with the highest containment,
    tie-broken by lowest numeric ZCTA5 -- deterministic, so a re-run against the
    same source files reproduces the same output byte-for-byte. This replaces
    B-Phase-1's "lowest numeric ZCTA5" rule, which picked boundary-straddling
    ZCTAs with no regard for which county they mostly sat in.

    National expansion to other states is this same script run again with a
    different -State (after adding that state's FIPS prefix to
    $StateFipsPrefix below).

.PARAMETER State
    Two-letter USPS state code. Defaults to TX.

.PARAMETER GazCountiesPath
    Local path to an already-downloaded Gazetteer counties file -- either the
    .zip or the extracted .txt. If omitted, downloads from the URL above.

.PARAMETER ZctaCountyRelPath
    Local path to an already-downloaded ZCTA-county relationship .txt file.
    If omitted, downloads from the URL above.

.EXAMPLE
    .\generate_county_reference.ps1
    .\generate_county_reference.ps1 -State TX -GazCountiesPath .\2025_Gaz_counties_national.txt -ZctaCountyRelPath .\tab20_zcta520_county20_natl.txt
#>
param(
    [string]$State = "TX",
    [string]$GazCountiesPath,
    [string]$ZctaCountyRelPath
)

$ErrorActionPreference = "Stop"

$GazCountiesUrl = "https://www2.census.gov/geo/docs/maps-data/data/gazetteer/2025_Gazetteer/2025_Gaz_counties_national.zip"
$ZctaCountyRelUrl = "https://www2.census.gov/geo/docs/maps-data/data/rel2020/zcta520/tab20_zcta520_county20_natl.txt"

$StateFipsPrefix = @{ "TX" = "48" }

function Import-PipeDelimited {
    param([string]$Text)
    $lines = ($Text -split "`r`n|`n") | Where-Object { $_.Length -gt 0 }
    return $lines | ConvertFrom-Csv -Delimiter '|'
}

function Get-GazCountiesText {
    param([string]$Path)

    $txtPath = $null
    $cleanupPaths = @()

    if ($Path) {
        if ($Path -like "*.zip") {
            $extractDir = Join-Path $env:TEMP ("gaz_extract_" + [guid]::NewGuid())
            New-Item -ItemType Directory -Path $extractDir | Out-Null
            Expand-Archive -Path $Path -DestinationPath $extractDir -Force
            $txtPath = (Get-ChildItem -Path $extractDir -Filter "*.txt" | Select-Object -First 1).FullName
            $cleanupPaths += $extractDir
        } else {
            $txtPath = $Path
        }
    } else {
        $tempZip = Join-Path $env:TEMP ("gaz_counties_" + [guid]::NewGuid() + ".zip")
        Invoke-WebRequest -Uri $GazCountiesUrl -OutFile $tempZip
        $extractDir = Join-Path $env:TEMP ("gaz_extract_" + [guid]::NewGuid())
        New-Item -ItemType Directory -Path $extractDir | Out-Null
        Expand-Archive -Path $tempZip -DestinationPath $extractDir -Force
        $txtPath = (Get-ChildItem -Path $extractDir -Filter "*.txt" | Select-Object -First 1).FullName
        $cleanupPaths += $tempZip, $extractDir
    }

    $text = [System.IO.File]::ReadAllText($txtPath)

    foreach ($p in $cleanupPaths) {
        if (Test-Path $p) { Remove-Item -Path $p -Recurse -Force }
    }

    return $text
}

function Get-ZctaCountyRelText {
    param([string]$Path)

    if ($Path) {
        return [System.IO.File]::ReadAllText($Path)
    }

    $tempTxt = Join-Path $env:TEMP ("zcta_county_rel_" + [guid]::NewGuid() + ".txt")
    Invoke-WebRequest -Uri $ZctaCountyRelUrl -OutFile $tempTxt
    $text = [System.IO.File]::ReadAllText($tempTxt)
    Remove-Item -Path $tempTxt -Force
    return $text
}

$state = $State.ToUpper()
if (-not $StateFipsPrefix.ContainsKey($state)) {
    Write-Error "error: no state FIPS prefix configured for '$state' -- add it to `$StateFipsPrefix"
    exit 1
}
$fipsPrefix = $StateFipsPrefix[$state]

# ---- Load counties (FIPS -> name) for the target state ----------------------
$gazRows = Import-PipeDelimited -Text (Get-GazCountiesText -Path $GazCountiesPath)
$counties = @{}
foreach ($row in $gazRows) {
    if ($row.USPS -eq $state) {
        $counties[$row.GEOID] = $row.NAME
    }
}

# ---- Load ZCTA-county relationship rows and pick highest-containment ZCTA ---
$relRows = Import-PipeDelimited -Text (Get-ZctaCountyRelText -Path $ZctaCountyRelPath)
$best = @{}
foreach ($row in $relRows) {
    $countyFips = $row.GEOID_COUNTY_20
    if (-not $countyFips -or -not $countyFips.StartsWith($fipsPrefix)) { continue }

    $zcta = $row.GEOID_ZCTA5_20
    if ([string]::IsNullOrWhiteSpace($zcta)) { continue }

    $arealandZcta = 0.0
    [void][double]::TryParse($row.AREALAND_ZCTA5_20, [ref]$arealandZcta)
    $arealandPart = 0.0
    [void][double]::TryParse($row.AREALAND_PART, [ref]$arealandPart)

    $containment = 0.0
    if ($arealandZcta -gt 0) {
        $containment = $arealandPart / $arealandZcta
    }

    $zctaNum = [int]$zcta

    if (-not $best.ContainsKey($countyFips)) {
        $best[$countyFips] = [PSCustomObject]@{ Zcta = $zcta; ZctaNum = $zctaNum; Containment = $containment }
    } else {
        $cur = $best[$countyFips]
        if (($containment -gt $cur.Containment) -or (($containment -eq $cur.Containment) -and ($zctaNum -lt $cur.ZctaNum))) {
            $best[$countyFips] = [PSCustomObject]@{ Zcta = $zcta; ZctaNum = $zctaNum; Containment = $containment }
        }
    }
}

# ---- Fatal guard: every county must have a representative ZCTA --------------
$missing = $counties.Keys | Where-Object { -not $best.ContainsKey($_) } | Sort-Object
if ($missing.Count -gt 0) {
    Write-Error "error: $($missing.Count) $state counties have no ZCTA in the relationship file: $($missing -join ', ')"
    exit 1
}

# ---- Emit the INSERT IGNORE block, ascending by county_fips -----------------
$fipsCodes = $counties.Keys | Sort-Object
$rowStrings = New-Object System.Collections.Generic.List[string]
foreach ($fips in $fipsCodes) {
    $name = $counties[$fips].Replace("'", "''")
    $zip = $best[$fips].Zcta
    $rowStrings.Add("('$fips','$state','$name','$zip')")
}

for ($i = 0; $i -lt $rowStrings.Count; $i++) {
    if ($i -lt $rowStrings.Count - 1) { $rowStrings[$i] = $rowStrings[$i] + "," }
    else { $rowStrings[$i] = $rowStrings[$i] + ";" }
}

$block = "INSERT IGNORE INTO county_reference (county_fips, state, county_name, representative_zip) VALUES`n" `
    + ($rowStrings -join "`n") + "`n"

Write-Output $block

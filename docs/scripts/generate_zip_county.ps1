<#
.SYNOPSIS
    Generate the `INSERT IGNORE INTO zip_county (...) VALUES ...;` block for
    migration V085, from US Census Bureau source data.

.DESCRIPTION
    Source (retrieved 2026-08-01), pipe (|) delimited -- the same file
    generate_county_reference.ps1 uses for V076, so the two tables are cut from
    one vintage and cannot disagree about which counties exist:

      - ZCTA5 -> county relationship, 2020 vintage:
        tab20_zcta520_county20_natl.txt
        https://www2.census.gov/geo/docs/maps-data/data/rel2020/zcta520/tab20_zcta520_county20_natl.txt

    US Census Bureau work product -- a work of the US Government, public domain
    (17 U.S.C. 105). No licence restriction on redistribution.

    ------------------------------------------------------------------------
    LIMITATION, AND IT IS NOT COSMETIC: ZCTA IS NOT ZIP
    ------------------------------------------------------------------------
    ZCTAs are Census *tabulation areas* that approximate USPS ZIP codes. They
    are not the same set. ZCTAs omit ZIPs with no residential delivery area --
    PO-box-only ZIPs and single-building/large-volume-customer ZIPs in
    particular. An agent typing a real, valid USPS ZIP may therefore find no
    row here.

    That is a MISS, not an error, and it must be presented as one: "we don't
    have that ZIP, pick a county" -- never "invalid ZIP". The consumer's miss
    path carries this weight; see docs/analysis/ichra_flow_and_handoffs.md S5.

    The better source is HUD's USPS ZIP-county crosswalk, which is built from
    actual USPS delivery data and carries a residential address ratio. It was
    attempted first on 2026-08-01 and could not be obtained: huduser.gov's file
    paths return HTTP 202 with a zero-byte body, and its API returns 401 without
    a registered access token. Switching to HUD later is this script's shape
    with a different reader and a genuine residential ratio -- the table does
    not change.

    ------------------------------------------------------------------------
    THE RATIO COLUMN IS LAND AREA, NOT POPULATION
    ------------------------------------------------------------------------
    land_area_ratio = AREALAND_PART / AREALAND_ZCTA5_20 -- the share of the
    ZCTA's land area lying inside that county. Same containment measure V076
    used to pick each county's representative ZIP.

    It is deliberately NOT named res_ratio. HUD's residential ratio is a share
    of *addresses*; this is a share of *dirt*. For a ZIP straddling a county
    line with a town on one side and ranchland on the other, the two orderings
    disagree, and land area is the wrong one. It is good enough to order the
    choices a crossing ZIP offers an agent -- and not good enough to choose for
    them, which is exactly why nothing auto-selects on it.

    Rows where AREALAND_ZCTA5_20 is zero or missing get NULL, not 0 -- an
    unknown ratio is not a zero one, and sorting must not treat it as least.

.PARAMETER State
    Two-letter USPS state code. Defaults to TX. National expansion is this same
    script with a different -State, after adding that state's FIPS prefix to
    $StateFipsPrefix below, emitted as a new V0NN data migration.

.PARAMETER ZctaCountyRelPath
    Local path to an already-downloaded ZCTA-county relationship .txt file.
    If omitted, downloads from the URL above.

.EXAMPLE
    .\generate_zip_county.ps1
    .\generate_zip_county.ps1 -State TX -ZctaCountyRelPath .\tab20_zcta520_county20_natl.txt
#>
param(
    [string]$State = "TX",
    [string]$ZctaCountyRelPath
)

$ErrorActionPreference = "Stop"

$ZctaCountyRelUrl = "https://www2.census.gov/geo/docs/maps-data/data/rel2020/zcta520/tab20_zcta520_county20_natl.txt"

$StateFipsPrefix = @{ "TX" = "48" }

if (-not $StateFipsPrefix.ContainsKey($State)) {
    throw "No FIPS prefix registered for state '$State'. Add it to `$StateFipsPrefix and re-run."
}
$prefix = $StateFipsPrefix[$State]

if ($ZctaCountyRelPath) {
    $text = Get-Content -Raw -Path $ZctaCountyRelPath
} else {
    $tempTxt = Join-Path ([System.IO.Path]::GetTempPath()) "tab20_zcta520_county20_natl.txt"
    Invoke-WebRequest -Uri $ZctaCountyRelUrl -OutFile $tempTxt
    $text = Get-Content -Raw -Path $tempTxt
}

$lines = ($text -split "`r`n|`n") | Where-Object { $_.Length -gt 0 }
$rows = $lines | ConvertFrom-Csv -Delimiter '|'

$pairs = @()
foreach ($r in $rows) {
    $zcta = $r.GEOID_ZCTA5_20
    $fips = $r.GEOID_COUNTY_20

    # County-only records (a county with no ZCTA overlap) carry an empty ZCTA
    # side. Skip rather than emit a row keyed on nothing.
    if ([string]::IsNullOrWhiteSpace($zcta)) { continue }
    if ([string]::IsNullOrWhiteSpace($fips)) { continue }
    if (-not $fips.StartsWith($prefix)) { continue }

    $zcta = $zcta.Trim()
    $fips = $fips.Trim()

    # Leading zeros are the whole point. Reject anything that is not exactly
    # five characters rather than pad it and hope.
    if ($zcta.Length -ne 5 -or $fips.Length -ne 5) {
        throw "Unexpected width: zcta='$zcta' fips='$fips' -- refusing to guess."
    }

    $ratioSql = "NULL"
    $partLand = 0.0
    $totalLand = 0.0
    if ([double]::TryParse($r.AREALAND_PART, [ref]$partLand) -and
        [double]::TryParse($r.AREALAND_ZCTA5_20, [ref]$totalLand) -and
        $totalLand -gt 0) {
        $ratio = $partLand / $totalLand
        if ($ratio -gt 1) { $ratio = 1 }      # clamp float noise, never above 1
        $ratioSql = $ratio.ToString("0.000000", [System.Globalization.CultureInfo]::InvariantCulture)
    }

    $pairs += [pscustomobject]@{ Zip = $zcta; Fips = $fips; RatioSql = $ratioSql }
}

# Deterministic order so a re-run against the same source reproduces the same
# file byte-for-byte, exactly as V076 does.
$pairs = $pairs | Sort-Object Zip, Fips

$distinctZips = ($pairs | Select-Object -ExpandProperty Zip -Unique).Count
$distinctCounties = ($pairs | Select-Object -ExpandProperty Fips -Unique).Count
$crossing = ($pairs | Group-Object Zip | Where-Object { $_.Count -gt 1 }).Count

Write-Host "State                     : $State (FIPS prefix $prefix)"
Write-Host "Crosswalk rows            : $($pairs.Count)"
Write-Host "Distinct ZCTAs            : $distinctZips"
Write-Host "Distinct counties         : $distinctCounties"
Write-Host "ZCTAs spanning >1 county  : $crossing"

$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("INSERT IGNORE INTO zip_county (zip, county_fips, land_area_ratio) VALUES")
for ($i = 0; $i -lt $pairs.Count; $i++) {
    $p = $pairs[$i]
    $terminator = if ($i -eq $pairs.Count - 1) { ";" } else { "," }
    [void]$sb.AppendLine("('$($p.Zip)','$($p.Fips)',$($p.RatioSql))$terminator")
}
$sb.ToString()

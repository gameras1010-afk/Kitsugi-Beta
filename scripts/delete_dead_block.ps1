$filePath = Join-Path $PSScriptRoot "..\app\src\main\java\com\kitsugi\animelist\ui\screens\explore\ExploreScreen.kt"
$fullPath = Resolve-Path $filePath
$lines = [System.IO.File]::ReadAllLines($fullPath)
Write-Host "Total lines before:" $lines.Length

# Keep lines 1-606 (0-indexed 0..605) and 941+ (0-indexed 940..)
$keep = @()
for ($i = 0; $i -lt 606; $i++) { $keep += $lines[$i] }
for ($i = 940; $i -lt $lines.Length; $i++) { $keep += $lines[$i] }

[System.IO.File]::WriteAllLines($fullPath, $keep, [System.Text.Encoding]::UTF8)
Write-Host "Total lines after:" $keep.Length
Write-Host "Done!"

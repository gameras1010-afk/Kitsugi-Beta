$gradleFile = "app/build.gradle.kts"
$notesFile = "RELEASE_NOTES.md"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

if (Test-Path $gradleFile) {
    $content = [System.IO.File]::ReadAllText((Get-Item $gradleFile).FullName, $utf8NoBom)
    if ($content -match 'val appVersionName = "(\d+)\.(\d+)\.(\d+)"') {
        $major = [int]$Matches[1]
        $minor = [int]$Matches[2]
        $patch = [int]$Matches[3] + 1
        $newVersion = "$major.$minor.$patch"
        
        $updatedContent = $content -replace 'val appVersionName = "\d+\.\d+\.\d+"', "val appVersionName = `"$newVersion`""
        [System.IO.File]::WriteAllText((Get-Item $gradleFile).FullName, $updatedContent, $utf8NoBom)

        if (Test-Path $notesFile) {
            $notesContent = [System.IO.File]::ReadAllText((Get-Item $notesFile).FullName, $utf8NoBom)
            # Tüm vX.Y.Z, vX.Y.Z-beta ve Kitsugi X.Y.Z ifadelerini otomatik yeni sürüme güncelle
            $updatedNotes = $notesContent -replace 'v\d+\.\d+\.\d+(-beta)?', "v$newVersion-beta"
            $updatedNotes = $updatedNotes -replace 'Kitsugi \d+\.\d+\.\d+', "Kitsugi $newVersion"
            [System.IO.File]::WriteAllText((Get-Item $notesFile).FullName, $updatedNotes, $utf8NoBom)
        }

        Write-Output $newVersion
    } else {
        Write-Error "Could not parse appVersionName from $gradleFile"
        exit 1
    }
} else {
    Write-Error "$gradleFile not found"
    exit 1
}

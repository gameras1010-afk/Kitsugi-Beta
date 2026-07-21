@echo off
setlocal enabledelayedexpansion

set "PATH=C:\Program Files\Git\cmd;C:\Program Files\GitHub CLI;%PATH%"

echo =================================================================
echo   Kitsugi-Beta - Otomatik GitHub Release Yukleyici (FOSS + GMS)
echo =================================================================
echo.

cd /d "C:\Kitsugi-Beta"

set "GIT_EXE=C:\Program Files\Git\cmd\git.exe"
set "GH_EXE=C:\Program Files\GitHub CLI\gh.exe"

if not exist "!GIT_EXE!" set "GIT_EXE=git"
if not exist "!GH_EXE!" set "GH_EXE=gh"

echo [+] 1. Surum numarasi ve Release Notlari otomatik yukseltiliyor...
FOR /F "usebackq tokens=*" %%V IN (`powershell -ExecutionPolicy Bypass -File scripts\bump_version.ps1`) DO SET "NEW_VER=%%V"

if not defined NEW_VER (
    echo [!] HATA: Surum numarasi yukseltilemedi.
    pause
    exit /b 1
)

set "TAG_NAME=v!NEW_VER!"
echo [+] Yeni Otomatik Surum: !TAG_NAME!

echo.
echo [+] 2. Kod ve surum degisiklikleri GitHub'a gonderiliyor...
"!GIT_EXE!" add .
"!GIT_EXE!" commit -m "Release !TAG_NAME!: Auto increment version and update release notes"
"!GIT_EXE!" push -u origin main

echo.
echo [+] 3. FOSS ve GMS APK'lari Derleniyor (Lutfen bekleyin)...
call gradlew.bat assembleFossDebug assembleGmsDebug

set "FOSS_APK="
for %%F in ("app\build\outputs\apk\foss\debug\*.apk") do set "FOSS_APK=%%F"
set "GMS_APK="
for %%F in ("app\build\outputs\apk\gms\debug\*.apk") do set "GMS_APK=%%F"

if exist "!FOSS_APK!" (
    echo.
    echo [+] 4. GitHub Release !TAG_NAME! - FOSS ve GMS APK'lari Yukleniyor...
    
    if exist "!GMS_APK!" (
        "!GH_EXE!" release create !TAG_NAME! "!FOSS_APK!" "!GMS_APK!" --title "Kitsugi !TAG_NAME!" --notes-file RELEASE_NOTES.md
        if !errorlevel! neq 0 (
            echo [!] Release zaten mevcut, APK'lar guncelleniyor...
            "!GH_EXE!" release upload !TAG_NAME! "!FOSS_APK!" "!GMS_APK!" --clobber
        )
    ) else (
        "!GH_EXE!" release create !TAG_NAME! "!FOSS_APK!" --title "Kitsugi !TAG_NAME!" --notes-file RELEASE_NOTES.md
        if !errorlevel! neq 0 (
            echo [!] Release zaten mevcut, APK yukleniyor...
            "!GH_EXE!" release upload !TAG_NAME! "!FOSS_APK!" --clobber
        )
    )

    echo.
    echo =================================================================
    echo   [BASARILI] !TAG_NAME! Surumu ve APK'lar GitHub'da Yayinlandi!
    echo =================================================================
) else (
    echo.
    echo [!] HATA: APK derlenemedi.
)

echo.
pause

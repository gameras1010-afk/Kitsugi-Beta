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

if not exist "%GIT_EXE%" set "GIT_EXE=git"
if not exist "%GH_EXE%" set "GH_EXE=gh"

echo [+] 1. Kod degisiklikleri GitHub'a gonderiliyor...
"%GIT_EXE%" add .
"%GIT_EXE%" commit -m "Auto Update: Source code and Release Prep"
"%GIT_EXE%" push -u origin main

echo.
echo [+] 2. FOSS ve GMS APK'lari Derleniyor (Lutfen bekleyin)...
call gradlew.bat assembleFossDebug assembleGmsDebug

set FOSS_APK=app\build\outputs\apk\foss\debug\app-foss-debug.apk
set GMS_APK=app\build\outputs\apk\gms\debug\app-gms-debug.apk

if exist "%FOSS_APK%" (
    echo.
    echo [+] 3. GitHub Release, FOSS ve GMS APK'lari Yukleniyor...
    set "TAG_NAME=v2.4.0-%date:~-4%%date:~3,2%%date:~0,2%"
    
    if exist "%GMS_APK%" (
        "%GH_EXE%" release create !TAG_NAME! "%FOSS_APK%" "%GMS_APK%" --title "Kitsugi !TAG_NAME!" --notes-file RELEASE_NOTES.md
    ) else (
        "%GH_EXE%" release create !TAG_NAME! "%FOSS_APK%" --title "Kitsugi !TAG_NAME!" --notes-file RELEASE_NOTES.md
    )

    echo.
    echo =================================================================
    echo   [BASARILI] FOSS + GMS Surumleri GitHub'da Yayinlandi!
    echo =================================================================
) else (
    echo.
    echo [!] HATA: APK derlenemedi.
)

echo.
pause

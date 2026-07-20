@echo off
setlocal enabledelayedexpansion

echo =================================================================
echo   Kitsugi-Beta - Otomatik GitHub Release Yukleyici
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
echo [+] 2. APK Derleniyor (Lutfen bekleyin)...
call gradlew.bat assembleFossDebug

if exist "app\build\outputs\apk\foss\debug\app-foss-debug.apk" (
    echo.
    echo [+] 3. GitHub Release ve APK Yukleniyor...
    "%GH_EXE%" release create v2.4.0 "app\build\outputs\apk\foss\debug\app-foss-debug.apk" --title "Kitsugi v2.4.0-beta" --notes-file RELEASE_NOTES.md
    echo.
    echo =================================================================
    echo   [BASARILI] Surum ve APK GitHub'da Yayinlandi!
    echo =================================================================
) else (
    echo.
    echo [!] HATA: APK derlenemedi.
)

echo.
pause

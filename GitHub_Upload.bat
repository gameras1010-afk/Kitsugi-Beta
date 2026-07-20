@echo off
setlocal enabledelayedexpansion

set "PATH=C:\Program Files\Git\cmd;C:\Program Files\GitHub CLI;%PATH%"

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
    set "TAG_NAME=v2.4.0-%date:~-4%%date:~3,2%%date:~0,2%"
    "%GH_EXE%" release create !TAG_NAME! "app\build\outputs\apk\foss\debug\app-foss-debug.apk" --title "Kitsugi !TAG_NAME!" --notes-file RELEASE_NOTES.md
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

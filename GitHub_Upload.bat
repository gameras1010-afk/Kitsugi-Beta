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

echo [+] 0. Surum numarasi otomatik olarak artiriliyor ve dosyalar guncelleniyor...
FOR /F "usebackq tokens=*" %%V IN (`powershell -NoProfile -ExecutionPolicy Bypass -File scripts\bump_version.ps1`) DO SET "APP_VER=%%V"
if not defined APP_VER (
    FOR /F "usebackq tokens=*" %%V IN (`powershell -NoProfile -Command "((Get-Content app/build.gradle.kts) -match 'val appVersionName =').Split([char]34)[1]"`) DO SET "APP_VER=%%V"
)
if not defined APP_VER set "APP_VER=2.4.19"
set "TAG_NAME=v!APP_VER!"

echo [+] Algilanan Surum: !TAG_NAME!

echo [+] 1. Kod degisiklikleri GitHub'a gonderiliyor...
"%GIT_EXE%" add .
"%GIT_EXE%" commit -m "Auto Update: Source code for !TAG_NAME!"
"%GIT_EXE%" push -u origin main

echo.
echo [+] 2. FOSS ve GMS APK'lari Derleniyor (Lutfen bekleyin)...
call gradlew.bat assembleFossRelease assembleGmsRelease --parallel --build-cache --configuration-cache
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Hizli derleme basarisiz. Configuration cache temizlenip tekrar deneniyor...
    call gradlew.bat --stop >nul 2>&1
    call gradlew.bat assembleFossRelease assembleGmsRelease --parallel --build-cache --no-configuration-cache
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo [!] Ikinci deneme de basarisiz. Clean build baslatiliyor - en guvenli yol...
        call gradlew.bat clean assembleFossRelease assembleGmsRelease
    )
)

set "UPLOAD_ASSETS="

for %%F in ("app\build\outputs\apk\foss\release\*.apk") do (
    set "FILENAME=%%~nxF"
    set "TARGET_FILE=app\build\outputs\apk\Kitsugi-Beta-v!APP_VER!-foss.apk"
    if NOT "!FILENAME!"=="!FILENAME:arm64=!" (
        set "TARGET_FILE=app\build\outputs\apk\Kitsugi-Beta-v!APP_VER!-foss-arm64-v8a.apk"
    ) else if NOT "!FILENAME!"=="!FILENAME:v7a=!" (
        set "TARGET_FILE=app\build\outputs\apk\Kitsugi-Beta-v!APP_VER!-foss-armeabi-v7a.apk"
    )
    copy /Y "%%F" "!TARGET_FILE!" >nul
    echo [+] Hazirlanan FOSS APK: !TARGET_FILE!
    set "UPLOAD_ASSETS=!UPLOAD_ASSETS! "!TARGET_FILE!""
)

for %%F in ("app\build\outputs\apk\gms\release\*.apk") do (
    set "FILENAME=%%~nxF"
    set "TARGET_FILE=app\build\outputs\apk\Kitsugi-Beta-v!APP_VER!-gms.apk"
    if NOT "!FILENAME!"=="!FILENAME:arm64=!" (
        set "TARGET_FILE=app\build\outputs\apk\Kitsugi-Beta-v!APP_VER!-gms-arm64-v8a.apk"
    ) else if NOT "!FILENAME!"=="!FILENAME:v7a=!" (
        set "TARGET_FILE=app\build\outputs\apk\Kitsugi-Beta-v!APP_VER!-gms-armeabi-v7a.apk"
    )
    copy /Y "%%F" "!TARGET_FILE!" >nul
    echo [+] Hazirlanan GMS APK: !TARGET_FILE!
    set "UPLOAD_ASSETS=!UPLOAD_ASSETS! "!TARGET_FILE!""
)

if defined UPLOAD_ASSETS (
    echo.
    echo [+] 3. GitHub Release !TAG_NAME! ve APK'lar Yukleniyor...
    set "SUCCESS=0"
    for /L %%I in (1,1,3) do (
        if !SUCCESS!==0 (
            echo [+] Yukleme Denemesi %%I / 3...
            "%GH_EXE%" release create !TAG_NAME! !UPLOAD_ASSETS! --title "Kitsugi !TAG_NAME!" --notes-file RELEASE_NOTES.md
            if !ERRORLEVEL!==0 (
                set "SUCCESS=1"
            ) else (
                echo [!] UYARI: Release olusturulamadi veya ag zaman asimina ugradi. Tekrar deneniyor...
                "%GH_EXE%" release upload !TAG_NAME! !UPLOAD_ASSETS! --clobber >nul 2>&1
                if !ERRORLEVEL!==0 set "SUCCESS=1"
                timeout /t 4 >nul
            )
        )
    )

    if !SUCCESS!==1 (
        echo.
        echo =================================================================
        echo   [BASARILI] !TAG_NAME! Surumu ve APK'lar GitHub'da Yayinlandi!
        echo =================================================================
    ) else (
        echo.
        echo =================================================================
        echo   [HATA] 3 denemede de release yüklemesi tamamlanamadi!
        echo   Baglantinizi veya GitHub sunucu durumunu kontrol edin.
        echo =================================================================
    )
) else (
    echo.
    echo [!] HATA: Yuklenecek APK dosyasi bulunamadi. Derleme basarisiz olabilir.
)

echo.
pause

@echo off
chcp 65001 > nul
title Kitsugi-Beta - GitHub Otomatik Kod ve APK Yayınlayıcı

echo =================================================================
echo   Kitsugi-Beta - Yapay Zeka Destekli Otomatik Yayınlayıcı
echo =================================================================
echo.

cd /d "C:\Kitsugi-Beta"

set GIT_PATH="C:\Program Files\Git\cmd\git.exe"
set GH_PATH="C:\Program Files\GitHub CLI\gh.exe"

echo [+] 1. Yapay Zeka Değişiklik Notları Kontrol Ediliyor (RELEASE_NOTES.md)...
if not exist RELEASE_NOTES.md (
    echo # Kitsugi Sürüm Notları > RELEASE_NOTES.md
    echo - Yapay zeka güncellemesi eklendi. >> RELEASE_NOTES.md
)

echo [+] 2. Kod Değişiklikleri GitHub'a Gönderiliyor...
%GIT_PATH% add .
%GIT_PATH% commit -m "AI Update & Auto Release Prep"
%GIT_PATH% branch -M main
%GIT_PATH% push -u origin main

echo.
echo =================================================================
echo   [Soru] GitHub üzerinde OTOMATİK APK & SÜRÜM (Release) oluşturulsun mu?
echo =================================================================
echo   [1] Evet - APK derle, Yapay Zeka Notları ile GitHub Release yayınla
echo   [2] Hayır - Sadece kaynak kodları yükle ve çık
echo.
set /p CHOICE="Seçiminiz (1 veya 2): "

if "%CHOICE%"=="1" (
    echo.
    echo [+] 3. APK Derleniyor (Lütfen bekleyin)...
    call gradlew.bat assembleFossDebug

    if exist "app\build\outputs\apk\foss\debug\app-foss-debug.apk" (
        echo.
        echo [+] 4. GitHub Release ve APK Otomatik Yükleniyor...
        set TAG_NAME=v2.4.0-%date:~-4%%date:~3,2%%date:~0,2%-%time:~0,2%%time:~3,2%
        set TAG_NAME=%TAG_NAME: =0%
        
        %GH_PATH% release create %TAG_NAME% "app\build\outputs\apk\foss\debug\app-foss-debug.apk" --title "Kitsugi Güncellemesi (%TAG_NAME%)" --notes-file RELEASE_NOTES.md
        
        echo.
        echo =================================================================
        echo   [BAŞARILI] Sürüm ve APK Yapay Zeka Notlarıyla GitHub'da Yayınlandı!
        echo =================================================================
    ) else (
        echo [!] APK derleme tamamlanamadı veya dosya bulunamadı.
    )
)

echo.
pause

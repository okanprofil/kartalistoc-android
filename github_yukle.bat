@echo off
title Kartal Istoc - GitHub Yukleyici
echo =============================================
echo   KARTAL ISTOC - OTOMATIK GITHUB YUKLEYICI
echo =============================================
echo.

:: Git kimlik ayarla
git config --global user.email "kartalistoc@example.com"
git config --global user.name "Kartal Istoc"

:: Eski git klasorunu sil
if exist .git rd /s /q .git

:: Yeni repo baslat
git init
git add -A
git commit -m "Uygulama guncellendi - %date% %time%"
git branch -M main

:: URL otomatik ayarli
set repo_url=https://github.com/okanprofil/kartalistoc-android.git

git remote add origin %repo_url%

echo.
echo Dosyalar GitHub'a yukleniyor...
echo Lutfen bekleyin...
echo.
git push -u origin main --force

echo.
echo =============================================
echo   YUKLEME TAMAMLANDI!
echo =============================================
echo.
echo Simdi GitHub Actions sayfasini aciyorum...
echo APK hazir olunca "Artifacts" bolumunden indirin.
echo.

:: GitHub Actions sayfasini otomatik ac
start https://github.com/okanprofil/kartalistoc-android/actions

pause

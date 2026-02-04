@echo off
title Kartal Istoc Mobil Test
echo Uygulama baslatiliyor...
echo.
echo NOT: Bu pencere, uygulamanin gerçek bir telefonda nasil 
echo görünecegini simüle eder.
echo.

:: Chrome'u uygulama modunda ve telefon boyutunda ac
start chrome --app="https://www.kartalistoc.com" --window-size=380,820 --user-data-dir="%temp%\kartal_istoc_test"

echo Test pencereleri acildi.
pause

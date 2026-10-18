@echo off
cd /d "%~dp0"
echo Building Novaris Android debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
 echo Build failed. Open this folder in Android Studio to see errors and install required SDK components.
 pause
 exit /b 1
)
echo.
echo APK ready at:
echo %~dp0app\build\outputs\apk\debug\app-debug.apk
pause

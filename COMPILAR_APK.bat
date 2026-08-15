@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo     LIMPIAAPPS ANDROID 1.0.0 - COMPILACION APK
echo ============================================================
echo.

where gradle >nul 2>nul
if %errorlevel%==0 (
    echo Gradle encontrado. Compilando...
    gradle assembleDebug
    goto FIN
)

echo No se ha encontrado Gradle en PATH.
echo.
echo Abre esta carpeta con Android Studio y usa:
echo Build ^> Build App Bundle(s) / APK(s) ^> Build APK(s)
echo.
echo Android Studio descargara las dependencias necesarias del proyecto.

:FIN
echo.
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK GENERADO:
    echo %cd%\app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Si la compilacion se realizo desde Android Studio, pulsa "locate"
    echo cuando aparezca el aviso de APK generado.
)
echo.
pause

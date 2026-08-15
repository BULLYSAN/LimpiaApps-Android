@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo ===== INICIO - LIMPIAAPPS TODO EN UNO =====
echo ============================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0TODO_EN_UNO_COMPILAR_E_INSTALAR.ps1"
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% EQU 0 (
    echo APK listo. Si hay un telefono Android conectado y autorizado,
    echo el script tambien habra intentado instalarlo.
) else (
    echo La preparacion o compilacion termino con un error.
)

echo.
echo ============================================================
echo ===== FIN - LIMPIAAPPS TODO EN UNO =====
echo ============================================================
echo.
pause
exit /b %EXITCODE%

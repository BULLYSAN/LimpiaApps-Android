$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "===== INICIO - LIMPIAAPPS: PREPARAR + COMPILAR APK =====" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

function Get-JavaMajor {
    try {
        $v = & java -version 2>&1 | Select-Object -First 1
        if ($v -match '"(\d+)(?:\.(\d+))?') {
            $major = [int]$Matches[1]
            if ($major -eq 1 -and $Matches[2]) { $major = [int]$Matches[2] }
            return $major
        }
    } catch {}
    return 0
}

function Ensure-Java {
    $major = Get-JavaMajor
    if ($major -ge 17) {
        Write-Host "[OK] Java $major detectado." -ForegroundColor Green
        return
    }

    Write-Host "[INFO] Java 17+ no encontrado." -ForegroundColor Yellow

    $studioJava = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    if (Test-Path $studioJava) {
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $studioJava)
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        Write-Host "[OK] Usando Java incluido con Android Studio: $env:JAVA_HOME" -ForegroundColor Green
        return
    }

    if (Get-Command winget -ErrorAction SilentlyContinue) {
        Write-Host "[INFO] Instalando Microsoft OpenJDK 17 con winget..." -ForegroundColor Yellow
        winget install --id Microsoft.OpenJDK.17 -e --accept-package-agreements --accept-source-agreements

        $jdk = Get-ChildItem "C:\Program Files\Microsoft" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
               Sort-Object LastWriteTime -Descending |
               Select-Object -First 1
        if ($jdk) {
            $env:JAVA_HOME = $jdk.FullName
            $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        }
    }

    $major = Get-JavaMajor
    if ($major -lt 17) {
        throw "No se pudo disponer de Java 17 o superior. Instala Android Studio o JDK 17 y vuelve a ejecutar este archivo."
    }

    Write-Host "[OK] Java $major preparado." -ForegroundColor Green
}

function Download-IfMissing([string]$Url, [string]$Destination) {
    if (Test-Path $Destination) {
        Write-Host "[OK] Ya existe: $Destination" -ForegroundColor DarkGreen
        return
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    Write-Host "[DESCARGA] $Url" -ForegroundColor Yellow

    $oldProgressPreference = $ProgressPreference
    $ProgressPreference = "SilentlyContinue"
    try {
        Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing
    } finally {
        $ProgressPreference = $oldProgressPreference
    }
}

function Ensure-AndroidSdk {
    $SdkRoot = Join-Path $ProjectRoot ".android-sdk"
    $CmdlineRoot = Join-Path $SdkRoot "cmdline-tools\latest"
    $SdkManager = Join-Path $CmdlineRoot "bin\sdkmanager.bat"

    if (-not (Test-Path $SdkManager)) {
        $Zip = Join-Path $ProjectRoot ".tools\commandlinetools-linux-NO.zip"
        # Windows package oficial de Android Command-line Tools (revisión vigente al crear esta versión).
        $Zip = Join-Path $ProjectRoot ".tools\commandlinetools-win-15859902_latest.zip"
        $Url = "https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip"

        Download-IfMissing $Url $Zip

        $Temp = Join-Path $ProjectRoot ".tools\cmdline-extract"
        if (Test-Path $Temp) { Remove-Item $Temp -Recurse -Force }
        New-Item -ItemType Directory -Force -Path $Temp | Out-Null
        Expand-Archive -Path $Zip -DestinationPath $Temp -Force

        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $CmdlineRoot) | Out-Null
        if (Test-Path $CmdlineRoot) { Remove-Item $CmdlineRoot -Recurse -Force }
        Move-Item (Join-Path $Temp "cmdline-tools") $CmdlineRoot
        Remove-Item $Temp -Recurse -Force
    }

    if (-not (Test-Path $SdkManager)) {
        throw "No se ha podido preparar sdkmanager."
    }

    $env:ANDROID_HOME = $SdkRoot
    $env:ANDROID_SDK_ROOT = $SdkRoot

    $localSdk = ($SdkRoot -replace '\\','/')
    "sdk.dir=$localSdk" | Set-Content -Path (Join-Path $ProjectRoot "local.properties") -Encoding ASCII

    Write-Host "[INFO] Aceptando licencias Android SDK..." -ForegroundColor Yellow
    1..30 | ForEach-Object { "y" } | & $SdkManager "--sdk_root=$SdkRoot" --licenses | Out-Host

    Write-Host "[INFO] Instalando Android SDK Platform 35, Build Tools 35.0.0 y Platform Tools..." -ForegroundColor Yellow
    & $SdkManager "--sdk_root=$SdkRoot" `
        "platform-tools" `
        "platforms;android-35" `
        "build-tools;35.0.0"

    if ($LASTEXITCODE -ne 0) {
        throw "sdkmanager terminó con error $LASTEXITCODE."
    }

    Write-Host "[OK] Android SDK preparado en $SdkRoot" -ForegroundColor Green
    return $SdkRoot
}

function Ensure-Gradle {
    $GradleHome = Join-Path $ProjectRoot ".tools\gradle-8.9"
    $GradleBat = Join-Path $GradleHome "bin\gradle.bat"

    if (-not (Test-Path $GradleBat)) {
        $Zip = Join-Path $ProjectRoot ".tools\gradle-8.9-bin.zip"
        $Url = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
        Download-IfMissing $Url $Zip

        $ExtractRoot = Join-Path $ProjectRoot ".tools\gradle-extract"
        if (Test-Path $ExtractRoot) { Remove-Item $ExtractRoot -Recurse -Force }
        New-Item -ItemType Directory -Force -Path $ExtractRoot | Out-Null
        Expand-Archive -Path $Zip -DestinationPath $ExtractRoot -Force

        if (Test-Path $GradleHome) { Remove-Item $GradleHome -Recurse -Force }
        Move-Item (Join-Path $ExtractRoot "gradle-8.9") $GradleHome
        Remove-Item $ExtractRoot -Recurse -Force
    }

    if (-not (Test-Path $GradleBat)) {
        throw "No se ha podido preparar Gradle 8.9."
    }

    Write-Host "[OK] Gradle 8.9 preparado." -ForegroundColor Green
    return $GradleBat
}

function Build-Apk([string]$GradleBat) {
    Write-Host ""
    Write-Host "[COMPILANDO] Limpiando proyecto..." -ForegroundColor Yellow
    & $GradleBat --no-daemon clean

    if ($LASTEXITCODE -ne 0) {
        throw "La limpieza de Gradle terminó con error $LASTEXITCODE."
    }

    Write-Host "[COMPILANDO] Generando APK debug..." -ForegroundColor Yellow
    & $GradleBat --no-daemon assembleDebug

    if ($LASTEXITCODE -ne 0) {
        throw "La compilación terminó con error $LASTEXITCODE."
    }

    $BuiltApk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $BuiltApk)) {
        throw "Gradle terminó, pero no se encuentra app-debug.apk."
    }

    $FinalApk = Join-Path $ProjectRoot "LimpiaApps_Android_1.0.0_DEBUG.apk"
    Copy-Item $BuiltApk $FinalApk -Force

    $hash = (Get-FileHash $FinalApk -Algorithm SHA256).Hash
    $sizeMb = [math]::Round((Get-Item $FinalApk).Length / 1MB, 2)

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "APK GENERADO CORRECTAMENTE" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "Archivo : $FinalApk"
    Write-Host "Tamano  : $sizeMb MB"
    Write-Host "SHA-256 : $hash"
    Write-Host ""

    return $FinalApk
}

function Install-OnConnectedAndroid([string]$SdkRoot, [string]$Apk) {
    $Adb = Join-Path $SdkRoot "platform-tools\adb.exe"
    if (-not (Test-Path $Adb)) { return }

    & $Adb start-server | Out-Null
    $devices = @(& $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "`tdevice$" })

    if ($devices.Count -eq 1) {
        Write-Host "[ANDROID] Se ha detectado 1 dispositivo autorizado. Instalando APK..." -ForegroundColor Yellow
        & $Adb install -r $Apk
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] LimpiaApps instalada en el dispositivo Android." -ForegroundColor Green
        } else {
            Write-Host "[AVISO] El APK se compiló correctamente, pero ADB no pudo instalarlo." -ForegroundColor Yellow
        }
    } elseif ($devices.Count -gt 1) {
        Write-Host "[AVISO] Hay varios dispositivos Android conectados. No se instala automáticamente para evitar elegir el incorrecto." -ForegroundColor Yellow
    } else {
        Write-Host "[INFO] No hay un Android autorizado por ADB. El APK queda listo para copiar e instalar manualmente." -ForegroundColor Cyan
    }
}

try {
    Ensure-Java
    $sdk = Ensure-AndroidSdk
    $gradle = Ensure-Gradle
    $apk = Build-Apk $gradle
    Install-OnConnectedAndroid $sdk $apk

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "===== FIN - TODO COMPLETADO CORRECTAMENTE =====" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
}
catch {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "ERROR" -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "===== FIN CON ERROR =====" -ForegroundColor Red
    Write-Host ""
    exit 1
}

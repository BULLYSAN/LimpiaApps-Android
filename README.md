# LimpiaApps Android 1.0.0

Aplicación Android nativa para analizar las apps visibles instaladas en el dispositivo y facilitar una limpieza segura.

## Funciones incluidas

- Detecta las aplicaciones con icono/lanzador instaladas en el teléfono.
- Separa las apps en:
  - Recomendadas para revisar.
  - Revisar.
  - Mantener.
  - Protegidas del sistema.
- Puntuación 0–100 basada principalmente en:
  - tiempo sin uso (cuando se concede "Acceso de uso"),
  - ausencia de uso registrado,
  - tamaño aproximado del APK,
  - señales auxiliares de utilidades tipo cleaner/booster.
- Buscador.
- Filtros.
- Ficha de detalle con motivos.
- Opción "Mantener" para excluir manualmente una app de futuras recomendaciones.
- Las apps de sistema se protegen.
- Desinstalación mediante la pantalla oficial de Android y confirmación del usuario.
- No elimina nada en segundo plano.

## Requisitos

- Android Studio reciente.
- JDK 17 o superior.
- Android SDK 35.
- Android 8.0 (API 26) o posterior en el teléfono.

## Compilar APK desde Android Studio

1. Descomprime `LimpiaApps_Android_1.0.0.zip`.
2. Abre Android Studio.
3. Elige **Open**.
4. Selecciona la carpeta `LimpiaApps_Android_1.0.0`.
5. Deja que termine la sincronización de Gradle.
6. Ve a **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.

El APK debug se generará normalmente en:

`app/build/outputs/apk/debug/app-debug.apk`

## Compilar desde terminal

Si tienes Gradle disponible:

```powershell
gradle assembleDebug
```

## Acceso de uso

En el primer análisis la aplicación puede funcionar sin permisos especiales. Para que pueda valorar cuándo se usó por última vez cada app, pulsa **ACTIVAR** y concede a LimpiaApps el permiso "Acceso de uso" desde los ajustes de Android.

Este permiso no permite borrar aplicaciones. La desinstalación siempre pasa por el diálogo oficial de Android.

## Nota sobre Google Play

Esta versión evita `QUERY_ALL_PACKAGES`: consulta las apps que tienen actividad de lanzador, lo que reduce el alcance de visibilidad y hace el diseño más respetuoso con la privacidad.


## Método automático recomendado en Windows

No necesitas preparar Gradle manualmente.

1. Descomprime el ZIP.
2. Haz doble clic en `EJECUTAR_TODO.bat`.
3. El proceso:
   - comprueba Java 17+;
   - intenta instalar JDK 17 con `winget` si fuera necesario;
   - descarga Android Command-line Tools oficiales;
   - instala Android SDK 35, Build Tools 35.0.0 y Platform Tools;
   - acepta las licencias del SDK;
   - descarga Gradle 8.9;
   - limpia y compila el proyecto;
   - copia el resultado como `LimpiaApps_Android_1.0.0_DEBUG.apk`;
   - si detecta exactamente un teléfono Android autorizado por ADB, intenta instalarlo automáticamente.

El APK final queda en la raíz del proyecto con este nombre:

`LimpiaApps_Android_1.0.0_DEBUG.apk`

## Compilar únicamente desde el móvil con GitHub

Este paquete incluye `.github/workflows/compilar-apk.yml`.

Al subir el proyecto a un repositorio GitHub, GitHub Actions configura Java 17, Gradle 8.9 y Android SDK 35, compila el APK y publica un artefacto llamado `LimpiaApps-Android-APK`.

Consulta `DESDE_EL_MOVIL_GITHUB.txt`.

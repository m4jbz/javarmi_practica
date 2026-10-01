@echo off
rem ===========================================================================
rem  Instala Java 25 (Eclipse Temurin / Adoptium) y Apache Maven 3.10.0
rem  en Windows 11, solo si todavia no existen como comando.
rem
rem  - Si "java" ya responde en la terminal, NO se instala Java.
rem  - Si "mvn"  ya responde en la terminal, NO se instala Maven.
rem
rem  No necesita permisos de administrador: todo se instala en
rem     %LOCALAPPDATA%\Programs\DevTools
rem  y se agrega al PATH del usuario (JAVA_HOME y MAVEN_HOME incluidos).
rem
rem  Uso:
rem     scripts\instalar-entorno.bat
rem
rem  Al terminar, abre una terminal NUEVA para que tome el PATH actualizado.
rem ===========================================================================
setlocal EnableExtensions

set "MAVEN_VERSION=3.10.0"
set "JAVA_FEATURE=25"
set "BASE_DIR=%LOCALAPPDATA%\Programs\DevTools"
set "DL_DIR=%TEMP%\instalar-entorno"

set "JAVA_URL=https://api.adoptium.net/v3/binary/latest/%JAVA_FEATURE%/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
set "MAVEN_ZIP=apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/%MAVEN_ZIP%"
set "MAVEN_URL_ALT=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/%MAVEN_ZIP%"

rem curl.exe y tar.exe vienen incluidos en Windows 10 (1803+) y Windows 11.
where curl >nul 2>nul || (echo [ERROR] No se encontro curl.exe. & goto :fallo)
where tar  >nul 2>nul || (echo [ERROR] No se encontro tar.exe.  & goto :fallo)

if not exist "%BASE_DIR%" mkdir "%BASE_DIR%"
if not exist "%DL_DIR%"   mkdir "%DL_DIR%"

set "CAMBIOS=0"

rem ---------------------------------------------------------------- Java ----
where java >nul 2>nul
if not errorlevel 1 (
    echo [OK] Java ya esta instalado, se omite:
    java -version 2>&1
    goto :maven
)
call :instalar_java || goto :fallo

rem --------------------------------------------------------------- Maven ----
:maven
echo.
where mvn >nul 2>nul
if not errorlevel 1 (
    echo [OK] Maven ya esta instalado, se omite:
    call mvn -v
    goto :fin
)
call :instalar_maven || goto :fallo

:fin
echo.
if "%CAMBIOS%"=="1" (
    echo ===========================================================
    echo  Listo. Abre una terminal NUEVA y verifica con:
    echo     java -version
    echo     mvn -v
    echo ===========================================================
) else (
    echo Nada que instalar: Java y Maven ya estaban disponibles.
)
rmdir /s /q "%DL_DIR%" 2>nul
endlocal
pause
exit /b 0

:fallo
echo.
echo [ERROR] La instalacion no se completo.
endlocal
pause
exit /b 1


rem ===========================================================================
rem  Subrutinas
rem ===========================================================================

:instalar_java
set "JDK_HOME="
call :buscar_jdk
if defined JDK_HOME (
    echo [INFO] Ya habia un JDK %JAVA_FEATURE% descargado en "%JDK_HOME%".
    goto :configurar_java
)
echo [INFO] Descargando Java %JAVA_FEATURE% ^(Temurin^)...
curl -fL --retry 3 -o "%DL_DIR%\jdk.zip" "%JAVA_URL%"
if errorlevel 1 (echo [ERROR] No se pudo descargar Java. & exit /b 1)
echo [INFO] Descomprimiendo Java...
tar -xf "%DL_DIR%\jdk.zip" -C "%BASE_DIR%"
if errorlevel 1 (echo [ERROR] No se pudo descomprimir Java. & exit /b 1)
call :buscar_jdk
if not defined JDK_HOME (echo [ERROR] No se encontro la carpeta del JDK. & exit /b 1)

:configurar_java
call :set_user_var JAVA_HOME "%JDK_HOME%" || exit /b 1
call :agregar_user_path "%JDK_HOME%\bin" || exit /b 1
set "JAVA_HOME=%JDK_HOME%"
set "PATH=%JDK_HOME%\bin;%PATH%"
set "CAMBIOS=1"
echo [OK] Java instalado en "%JDK_HOME%"
exit /b 0

:buscar_jdk
rem El zip de Adoptium trae una carpeta tipo "jdk-25.0.1+8".
for /d %%D in ("%BASE_DIR%\jdk-%JAVA_FEATURE%*") do (
    if exist "%%D\bin\java.exe" set "JDK_HOME=%%D"
)
exit /b 0

:instalar_maven
set "MVN_HOME=%BASE_DIR%\apache-maven-%MAVEN_VERSION%"
if exist "%MVN_HOME%\bin\mvn.cmd" (
    echo [INFO] Ya habia un Maven %MAVEN_VERSION% descargado en "%MVN_HOME%".
    goto :configurar_maven
)
echo [INFO] Descargando Maven %MAVEN_VERSION%...
curl -fL --retry 3 -o "%DL_DIR%\%MAVEN_ZIP%" "%MAVEN_URL%"
if errorlevel 1 (
    echo [INFO] Probando con el archivo historico de Apache...
    curl -fL --retry 3 -o "%DL_DIR%\%MAVEN_ZIP%" "%MAVEN_URL_ALT%"
    if errorlevel 1 (echo [ERROR] No se pudo descargar Maven. & exit /b 1)
)
echo [INFO] Descomprimiendo Maven...
tar -xf "%DL_DIR%\%MAVEN_ZIP%" -C "%BASE_DIR%"
if errorlevel 1 (echo [ERROR] No se pudo descomprimir Maven. & exit /b 1)
if not exist "%MVN_HOME%\bin\mvn.cmd" (echo [ERROR] No se encontro mvn.cmd. & exit /b 1)

:configurar_maven
call :set_user_var MAVEN_HOME "%MVN_HOME%" || exit /b 1
call :agregar_user_path "%MVN_HOME%\bin" || exit /b 1
set "PATH=%MVN_HOME%\bin;%PATH%"
set "CAMBIOS=1"
echo [OK] Maven instalado en "%MVN_HOME%"
exit /b 0

:set_user_var
rem %1 = nombre, %2 = valor. Se usa PowerShell para no depender de setx.
set "VAR_NAME=%~1"
set "VAR_VALUE=%~2"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "[Environment]::SetEnvironmentVariable($env:VAR_NAME, $env:VAR_VALUE, 'User')"
if errorlevel 1 (echo [ERROR] No se pudo definir %VAR_NAME%. & exit /b 1)
exit /b 0

:agregar_user_path
rem Agrega %1 al inicio del PATH del usuario si no estaba ya.
rem (setx truncaria el PATH a 1024 caracteres, por eso PowerShell.)
set "ADD_DIR=%~1"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$d = $env:ADD_DIR;" ^
  "$p = [Environment]::GetEnvironmentVariable('Path', 'User');" ^
  "$parts = @(); if ($p) { $parts = $p.Split(';') | Where-Object { $_ } };" ^
  "if ($parts -notcontains $d) { [Environment]::SetEnvironmentVariable('Path', ((@($d) + $parts) -join ';'), 'User') }"
if errorlevel 1 (echo [ERROR] No se pudo actualizar el PATH. & exit /b 1)
exit /b 0

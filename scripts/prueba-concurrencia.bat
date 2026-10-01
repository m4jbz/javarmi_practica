@echo off
rem ===========================================================================
rem  Ejecuta la prueba de concurrencia: N clientes simulados atacan el
rem  servidor al mismo tiempo y al final se verifica la base de datos.
rem
rem  Uso:
rem     scripts\prueba-concurrencia.bat
rem     scripts\prueba-concurrencia.bat localhost 1099 20
rem
rem  Argumentos (todos opcionales):
rem     %1 host del servidor      (por defecto localhost)
rem     %2 puerto del registro    (por defecto 1099)
rem     %3 numero de clientes     (por defecto 10)
rem     %4 archivo SQLite         (por defecto db\agenda.db)
rem ===========================================================================
setlocal

cd /d "%~dp0.."

set JAR=agenda-client\target\agenda-client.jar

if not exist "%JAR%" (
    echo [ERROR] No se encontro %JAR%
    echo         Compila primero el proyecto con:  mvn clean package
    exit /b 1
)

set HOST=%1
if "%HOST%"=="" set HOST=localhost

set PUERTO=%2
if "%PUERTO%"=="" set PUERTO=1099

set CLIENTES=%3
if "%CLIENTES%"=="" set CLIENTES=10

set BD=%4
if "%BD%"=="" set BD=db/agenda.db

java -cp "%JAR%" mx.edu.itigualapa.agenda.client.PruebaConcurrencia %HOST% %PUERTO% %CLIENTES% %BD%

endlocal

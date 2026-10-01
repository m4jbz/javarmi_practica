@echo off
rem ===========================================================================
rem  Arranca un cliente de consola de la agenda.
rem  Se puede ejecutar varias veces a la vez: cada ventana es un cliente
rem  independiente conectado al mismo servidor.
rem
rem  Uso:
rem     scripts\iniciar-cliente-consola.bat
rem     scripts\iniciar-cliente-consola.bat 192.168.1.10 1099 cliente-A
rem
rem  Argumentos (todos opcionales):
rem     %1 host del servidor      (por defecto localhost)
rem     %2 puerto del registro    (por defecto 1099)
rem     %3 nombre del cliente     (aparece en la consola del servidor)
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

set NOMBRE=%3
if "%NOMBRE%"=="" set NOMBRE=consola-%COMPUTERNAME%

java -cp "%JAR%" mx.edu.itigualapa.agenda.client.ClienteConsola %HOST% %PUERTO% %NOMBRE%

endlocal

@echo off
rem ===========================================================================
rem  Arranca el cliente grafico (JavaFX) de la agenda.
rem
rem  Uso:
rem     scripts\iniciar-cliente-fx.bat
rem     scripts\iniciar-cliente-fx.bat 192.168.1.10 1099
rem
rem  Argumentos (todos opcionales):
rem     %1 host del servidor    (por defecto localhost)
rem     %2 puerto del registro  (por defecto 1099)
rem
rem  El jar del cliente incluye las bibliotecas de JavaFX, por eso basta con
rem  ejecutar la clase Launcher (que NO extiende Application) y no hace falta
rem  indicar --module-path ni --add-modules.
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

echo Abriendo el cliente grafico conectado a %HOST%:%PUERTO% ...

java -cp "%JAR%" mx.edu.itigualapa.agenda.client.Launcher --host=%HOST% --puerto=%PUERTO%

endlocal

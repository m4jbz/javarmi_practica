@echo off
rem ===========================================================================
rem  Arranca el servidor RMI de la agenda personal.
rem
rem  Uso:
rem     scripts\iniciar-servidor.bat                      (todo en esta PC)
rem     scripts\iniciar-servidor.bat 1099 1100 192.168.3.191   (clientes en red)
rem
rem  Argumentos (todos opcionales):
rem     %1 puerto del registro RMI      (por defecto 1099)
rem     %2 puerto del objeto remoto     (por defecto 1100)
rem     %3 IP que el servidor anuncia   (por defecto 127.0.0.1)
rem
rem  IMPORTANTE - por que el tercer argumento existe:
rem  el stub que RMI entrega al cliente lleva dentro la direccion del servidor.
rem  Si no se fija, Java la deduce sola y puede anunciar una IP que despues
rem  cambie (por ejemplo al reconectarse el Wi-Fi), y entonces el cliente
rem  falla con "Connection refused / Connection timed out" aunque el servidor
rem  siga encendido. Por eso el valor por defecto es 127.0.0.1, que nunca
rem  cambia y sirve para trabajar en una sola PC.
rem  Para que se conecten clientes de OTROS equipos hay que pasar como tercer
rem  argumento la IP real del servidor en la red (la que muestra ipconfig).
rem ===========================================================================
setlocal

rem Todas las rutas del proyecto son relativas a la carpeta raiz.
cd /d "%~dp0.."

set JAR=agenda-server\target\agenda-server.jar

if not exist "%JAR%" (
    echo [ERROR] No se encontro %JAR%
    echo         Compila primero el proyecto con:  mvn clean package
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] No se encontro el comando java en el PATH.
    echo         Instala el JDK 21 y vuelve a abrir esta ventana.
    exit /b 1
)

set PUERTO_REG=%1
if "%PUERTO_REG%"=="" set PUERTO_REG=1099

set PUERTO_OBJ=%2
if "%PUERTO_OBJ%"=="" set PUERTO_OBJ=1100

set IP_ANUNCIADA=%3
if "%IP_ANUNCIADA%"=="" set IP_ANUNCIADA=127.0.0.1

echo Iniciando el servidor de la agenda...
echo Carpeta de trabajo : %CD%
echo IP anunciada       : %IP_ANUNCIADA%
echo.

java -jar "%JAR%" %PUERTO_REG% %PUERTO_OBJ% %IP_ANUNCIADA%

endlocal

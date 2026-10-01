#!/usr/bin/env bash
# ===========================================================================
#  Arranca el servidor RMI de la agenda personal.
#
#  Uso:
#     ./scripts/iniciar-servidor.sh                          (todo en esta PC)
#     ./scripts/iniciar-servidor.sh 1099 1100 192.168.3.191  (clientes en red)
#
#  Argumentos (todos opcionales):
#     $1 puerto del registro RMI      (por defecto 1099)
#     $2 puerto del objeto remoto     (por defecto 1100)
#     $3 IP que el servidor anuncia   (por defecto 127.0.0.1)
#
#  IMPORTANTE - por que el tercer argumento existe:
#  el stub que RMI entrega al cliente lleva dentro la direccion del servidor.
#  Si no se fija, Java la deduce sola y puede anunciar una IP que despues
#  cambie (por ejemplo al reconectarse el Wi-Fi), y entonces el cliente falla
#  con "Connection refused / Connection timed out" aunque el servidor siga
#  encendido. Por eso el valor por defecto es 127.0.0.1, que nunca cambia y
#  sirve para trabajar en una sola PC.
#  Para que se conecten clientes de OTROS equipos hay que pasar como tercer
#  argumento la IP real del servidor en la red (la que muestra ip addr).
# ===========================================================================
set -euo pipefail

# Todas las rutas del proyecto son relativas a la carpeta raiz.
cd "$(dirname "$0")/.."

JAR="agenda-server/target/agenda-server.jar"

if [ ! -f "$JAR" ]; then
    echo "[ERROR] No se encontro $JAR"
    echo "        Compila primero el proyecto con:  mvn clean package"
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] No se encontro el comando java en el PATH."
    echo "        Instala el JDK 21:  sudo apt install openjdk-21-jdk"
    exit 1
fi

PUERTO_REG="${1:-1099}"
PUERTO_OBJ="${2:-1100}"
IP_ANUNCIADA="${3:-127.0.0.1}"

echo "Iniciando el servidor de la agenda..."
echo "Carpeta de trabajo : $(pwd)"
echo "IP anunciada       : $IP_ANUNCIADA"
echo

exec java -jar "$JAR" "$PUERTO_REG" "$PUERTO_OBJ" "$IP_ANUNCIADA"

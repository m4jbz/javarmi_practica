#!/usr/bin/env bash
# ===========================================================================
#  Arranca un cliente de consola de la agenda.
#  Se puede ejecutar varias veces a la vez: cada terminal es un cliente
#  independiente conectado al mismo servidor.
#
#  Uso:
#     ./scripts/iniciar-cliente-consola.sh
#     ./scripts/iniciar-cliente-consola.sh 192.168.1.10 1099 cliente-A
#
#  Argumentos (todos opcionales):
#     $1 host del servidor      (por defecto localhost)
#     $2 puerto del registro    (por defecto 1099)
#     $3 nombre del cliente     (aparece en la consola del servidor)
# ===========================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

JAR="agenda-client/target/agenda-client.jar"

if [ ! -f "$JAR" ]; then
    echo "[ERROR] No se encontro $JAR"
    echo "        Compila primero el proyecto con:  mvn clean package"
    exit 1
fi

HOST="${1:-localhost}"
PUERTO="${2:-1099}"
NOMBRE="${3:-consola-$(hostname)}"

exec java -cp "$JAR" mx.edu.itigualapa.agenda.client.ClienteConsola "$HOST" "$PUERTO" "$NOMBRE"

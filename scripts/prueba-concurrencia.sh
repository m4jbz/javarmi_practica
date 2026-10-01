#!/usr/bin/env bash
# ===========================================================================
#  Ejecuta la prueba de concurrencia: N clientes simulados atacan el
#  servidor al mismo tiempo y al final se verifica la base de datos.
#
#  Uso:
#     ./scripts/prueba-concurrencia.sh
#     ./scripts/prueba-concurrencia.sh localhost 1099 20
#
#  Argumentos (todos opcionales):
#     $1 host del servidor      (por defecto localhost)
#     $2 puerto del registro    (por defecto 1099)
#     $3 numero de clientes     (por defecto 10)
#     $4 archivo SQLite         (por defecto db/agenda.db)
# ===========================================================================
set -uo pipefail

cd "$(dirname "$0")/.."

JAR="agenda-client/target/agenda-client.jar"

if [ ! -f "$JAR" ]; then
    echo "[ERROR] No se encontro $JAR"
    echo "        Compila primero el proyecto con:  mvn clean package"
    exit 1
fi

HOST="${1:-localhost}"
PUERTO="${2:-1099}"
CLIENTES="${3:-10}"
BD="${4:-db/agenda.db}"

exec java -cp "$JAR" mx.edu.itigualapa.agenda.client.PruebaConcurrencia \
    "$HOST" "$PUERTO" "$CLIENTES" "$BD"

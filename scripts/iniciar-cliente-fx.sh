#!/usr/bin/env bash
# ===========================================================================
#  Arranca el cliente grafico (JavaFX) de la agenda.
#
#  Uso:
#     ./scripts/iniciar-cliente-fx.sh
#     ./scripts/iniciar-cliente-fx.sh 192.168.1.10 1099
#
#  Argumentos (todos opcionales):
#     $1 host del servidor    (por defecto localhost)
#     $2 puerto del registro  (por defecto 1099)
#
#  Nota: el jar del cliente incluye las bibliotecas nativas de JavaFX del
#  sistema operativo donde se compilo. Si el jar se compilo en Windows y se
#  ejecuta en Linux, este script recurre automaticamente a "mvn javafx:run",
#  que descarga las bibliotecas correctas para esta plataforma.
# ===========================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

JAR="agenda-client/target/agenda-client.jar"
HOST="${1:-localhost}"
PUERTO="${2:-1099}"

echo "Abriendo el cliente grafico conectado a $HOST:$PUERTO ..."

if [ -f "$JAR" ] && java -cp "$JAR" mx.edu.itigualapa.agenda.client.Launcher \
        "--host=$HOST" "--puerto=$PUERTO"; then
    exit 0
fi

echo "[AVISO] No se pudo arrancar desde el jar; se intenta con Maven."
exec mvn -q -pl agenda-client javafx:run \
    -Djavafx.args="--host=$HOST --puerto=$PUERTO"

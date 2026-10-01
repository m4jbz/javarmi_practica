#!/usr/bin/env bash
# ===========================================================================
#  Crea la base de datos SQLite desde cero y le carga los datos de prueba.
#
#  Requiere el ejecutable sqlite3.
#  Si no lo tienes, NO es un problema: el servidor crea la tabla solo al
#  arrancar, y los datos de prueba se pueden capturar desde el cliente.
#
#  Uso:
#     ./scripts/crear-bd.sh
# ===========================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v sqlite3 >/dev/null 2>&1; then
    echo "[AVISO] No se encontro el comando sqlite3."
    echo "        El servidor creara la tabla automaticamente al arrancar,"
    echo "        asi que este paso es opcional."
    echo "        Si quieres sqlite3:  sudo apt install sqlite3"
    exit 0
fi

mkdir -p db

echo "Recreando db/agenda.db ..."
sqlite3 db/agenda.db < db/schema_sqlite.sql

echo "Cargando datos de prueba ..."
sqlite3 db/agenda.db < db/datos_prueba.sql

echo
echo "Base de datos lista en db/agenda.db"

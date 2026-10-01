@echo off
rem ===========================================================================
rem  Crea la base de datos SQLite desde cero y le carga los datos de prueba.
rem
rem  Requiere el ejecutable sqlite3 en el PATH.
rem  Si no lo tienes, NO es un problema: el servidor crea la tabla solo al
rem  arrancar, y los datos de prueba se pueden capturar desde el cliente.
rem
rem  Uso:
rem     scripts\crear-bd.bat
rem ===========================================================================
setlocal

cd /d "%~dp0.."

where sqlite3 >nul 2>nul
if errorlevel 1 (
    echo [AVISO] No se encontro el comando sqlite3 en el PATH.
    echo         El servidor creara la tabla automaticamente al arrancar,
    echo         asi que este paso es opcional.
    echo         Si quieres sqlite3:  winget install SQLite.SQLite
    exit /b 0
)

if not exist "db" mkdir "db"

echo Recreando db\agenda.db ...
sqlite3 db\agenda.db < db\schema_sqlite.sql
if errorlevel 1 (
    echo [ERROR] Fallo la creacion del esquema.
    exit /b 1
)

echo Cargando datos de prueba ...
sqlite3 db\agenda.db < db\datos_prueba.sql
if errorlevel 1 (
    echo [ERROR] Fallo la carga de datos de prueba.
    exit /b 1
)

echo.
echo Base de datos lista en db\agenda.db
endlocal

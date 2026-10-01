-- ===========================================================================
--  Sistema de Agenda Personal con RMI y Base de Datos
--  Instituto Tecnologico de Iguala
--  IFF-1019 Programacion en Ambiente Cliente Servidor
--  Tema 3 RMI - Practica 1
--
--  Script de creacion del esquema para SQLite.
--
--  Ejecucion (desde la carpeta raiz del proyecto agenda-rmi):
--      sqlite3 db/agenda.db < db/schema_sqlite.sql
--
--  El servidor tambien crea esta tabla automaticamente al arrancar si no
--  existe, de modo que este script sirve como definicion oficial del esquema
--  y para recrear la base desde cero.
-- ===========================================================================

-- Modo WAL (write-ahead logging): permite que las lecturas de unos clientes
-- no bloqueen las escrituras de otros, que es lo que necesita un servidor
-- que atiende varias peticiones al mismo tiempo.
PRAGMA journal_mode = WAL;

-- Tiempo (en milisegundos) que una conexion espera si la base esta ocupada
-- antes de reportar el error "database is locked".
PRAGMA busy_timeout = 5000;

-- Se activa la comprobacion de llaves foraneas (buena practica general).
PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------------
-- Tabla Contactos
-- ---------------------------------------------------------------------------
--  id                  : clave primaria autoincremental
--  nombre              : nombre completo (2 a 100 caracteres, solo letras)
--  telefono            : exactamente 10 digitos
--  email               : correo electronico, UNICO en toda la tabla
--  fecha_creacion      : se llena sola al insertar (hora local del equipo)
--  fecha_actualizacion : se actualiza desde el DAO en cada modificacion
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS Contactos;

CREATE TABLE Contactos (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre              TEXT    NOT NULL,
    telefono            TEXT    NOT NULL,
    email               TEXT    NOT NULL UNIQUE,
    fecha_creacion      TEXT    DEFAULT (datetime('now','localtime')),
    fecha_actualizacion TEXT    DEFAULT (datetime('now','localtime')),

    -- Las mismas reglas que aplica la clase Validador, ahora en la base de
    -- datos: aunque alguien escribiera directamente con sqlite3, los datos
    -- seguirian siendo consistentes.
    CONSTRAINT ck_nombre_longitud   CHECK (length(trim(nombre)) BETWEEN 2 AND 100),
    CONSTRAINT ck_telefono_digitos  CHECK (length(telefono) = 10 AND telefono GLOB '[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
    CONSTRAINT ck_email_formato     CHECK (email LIKE '%_@_%._%')
);

-- Indice de apoyo para la busqueda parcial por nombre.
CREATE INDEX IF NOT EXISTS idx_contactos_nombre ON Contactos(nombre);

-- Indice de apoyo para las consultas por correo (la restriccion UNIQUE ya
-- genera uno, se declara de forma explicita solo por claridad del diseno).
CREATE UNIQUE INDEX IF NOT EXISTS idx_contactos_email ON Contactos(email);

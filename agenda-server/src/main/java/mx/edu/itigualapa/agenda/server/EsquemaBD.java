package mx.edu.itigualapa.agenda.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Crea la tabla {@code Contactos} si todavia no existe.
 *
 * <p>Los scripts de la carpeta {@code db/} son la definicion oficial del
 * esquema y son los que se entregan y se documentan. Esta clase ejecuta el
 * equivalente en SQLite al arrancar el servidor, unicamente para que el
 * sistema pueda ponerse en marcha sin un paso manual previo.</p>
 */
final class EsquemaBD {

    /** Definicion de la tabla para SQLite. */
    private static final String DDL_SQLITE = """
            CREATE TABLE IF NOT EXISTS Contactos (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre              TEXT    NOT NULL,
                telefono            TEXT    NOT NULL,
                email               TEXT    NOT NULL UNIQUE,
                fecha_creacion      TEXT    DEFAULT (datetime('now','localtime')),
                fecha_actualizacion TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    /** Indice para acelerar las busquedas por nombre. */
    private static final String DDL_INDICE_NOMBRE =
            "CREATE INDEX IF NOT EXISTS idx_contactos_nombre ON Contactos(nombre)";

    /** Clase de utilidad: no se instancia. */
    private EsquemaBD() {
    }

    /**
     * Verifica el esquema y lo crea si hace falta.
     *
     * @throws SQLException si la base de datos no esta disponible o el esquema
     *                      no se puede verificar
     */
    static void inicializar() throws SQLException {
        try (Connection cn = ConexionBD.obtenerConexion();
             Statement st = cn.createStatement()) {

            st.execute(DDL_SQLITE);
            st.execute(DDL_INDICE_NOMBRE);
            System.out.println("[BD] Esquema SQLite verificado (tabla Contactos lista).");
        }
    }
}

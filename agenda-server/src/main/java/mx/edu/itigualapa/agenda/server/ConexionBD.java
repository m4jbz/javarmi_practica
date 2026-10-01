package mx.edu.itigualapa.agenda.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Puerta de entrada a la base de datos por medio de JDBC.
 *
 * <p>Lee la configuracion de {@code db.properties} y entrega una conexion
 * nueva por cada operacion. Trabajar con una conexion por operacion, siempre
 * dentro de un bloque {@code try-with-resources}, evita que una conexion
 * quede abierta y compartida entre los distintos hilos que RMI usa para
 * atender a los clientes.</p>
 *
 * <p>En cada conexion se activan dos ajustes de SQLite:</p>
 * <ul>
 *   <li><b>journal_mode = WAL</b> (registro de escritura anticipada): permite
 *       que las lecturas no bloqueen a las escrituras ni al contrario.</li>
 *   <li><b>busy_timeout</b>: si otra conexion tiene la base de datos ocupada,
 *       esta espera el tiempo configurado en lugar de fallar de inmediato.</li>
 * </ul>
 */
public final class ConexionBD {

    /** Nombre del archivo de configuracion. */
    private static final String ARCHIVO_CONFIG = "db.properties";

    /** Propiedad del sistema con la que se puede indicar otra ruta de configuracion. */
    private static final String PROP_RUTA_CONFIG = "agenda.db.config";

    /** Configuracion ya cargada. */
    private static final Properties CONFIG = cargarConfiguracion();

    /** Tiempo de espera, en milisegundos, cuando la base de datos esta ocupada. */
    private static final int BUSY_TIMEOUT_MS =
            Integer.parseInt(CONFIG.getProperty("db.busy.timeout.ms", "5000").trim());

    /** Clase de utilidad: no se instancia. */
    private ConexionBD() {
    }

    /**
     * Carga el archivo de configuracion buscandolo en tres lugares, en orden:
     * la ruta de la propiedad del sistema, el directorio de trabajo y el
     * interior del jar.
     *
     * @return las propiedades de configuracion
     */
    private static Properties cargarConfiguracion() {
        Properties p = new Properties();

        // 1) Ruta indicada de forma explicita al arrancar la maquina virtual.
        String rutaExterna = System.getProperty(PROP_RUTA_CONFIG);
        if (rutaExterna != null && new File(rutaExterna).isFile()) {
            try (InputStream in = new FileInputStream(rutaExterna)) {
                p.load(in);
                System.out.println("[BD] Configuracion leida de: " + rutaExterna);
                return p;
            } catch (IOException e) {
                System.err.println("[BD] No se pudo leer " + rutaExterna + ": " + e.getMessage());
            }
        }

        // 2) Archivo db.properties junto al ejecutable.
        File local = new File(ARCHIVO_CONFIG);
        if (local.isFile()) {
            try (InputStream in = new FileInputStream(local)) {
                p.load(in);
                System.out.println("[BD] Configuracion leida de: " + local.getAbsolutePath());
                return p;
            } catch (IOException e) {
                System.err.println("[BD] No se pudo leer el archivo local: " + e.getMessage());
            }
        }

        // 3) Valores por defecto empaquetados en el jar.
        try (InputStream in = ConexionBD.class.getClassLoader()
                .getResourceAsStream(ARCHIVO_CONFIG)) {
            if (in != null) {
                p.load(in);
                System.out.println("[BD] Configuracion leida del jar (valores por defecto).");
            } else {
                System.err.println("[BD] No se encontro " + ARCHIVO_CONFIG
                        + "; se usara SQLite en db/agenda.db");
                p.setProperty("sqlite.url", "jdbc:sqlite:db/agenda.db");
                p.setProperty("sqlite.archivo", "db/agenda.db");
            }
        } catch (IOException e) {
            System.err.println("[BD] Error al cargar la configuracion: " + e.getMessage());
        }
        return p;
    }

    /**
     * Devuelve el URL JDBC de la base de datos SQLite configurada.
     *
     * @return el URL de conexion
     */
    public static String getUrl() {
        return CONFIG.getProperty("sqlite.url", "jdbc:sqlite:db/agenda.db");
    }

    /**
     * Abre una conexion nueva a la base de datos.
     *
     * <p>Quien la recibe es responsable de cerrarla; lo habitual es usarla en
     * un bloque {@code try-with-resources}.</p>
     *
     * @return una conexion lista para usarse
     * @throws SQLException si no se puede establecer la conexion
     */
    public static Connection obtenerConexion() throws SQLException {
        asegurarCarpetaSqlite();
        Connection cn = DriverManager.getConnection(getUrl());
        aplicarAjustesSqlite(cn);
        return cn;
    }

    /**
     * Crea la carpeta que contendra el archivo de SQLite si todavia no existe;
     * de lo contrario el driver falla al intentar crear la base de datos.
     */
    private static void asegurarCarpetaSqlite() {
        String archivo = CONFIG.getProperty("sqlite.archivo", "db/agenda.db");
        Path padre = Paths.get(archivo).toAbsolutePath().getParent();
        if (padre != null && !Files.isDirectory(padre)) {
            try {
                Files.createDirectories(padre);
            } catch (IOException e) {
                System.err.println("[BD] No se pudo crear la carpeta "
                        + padre + ": " + e.getMessage());
            }
        }
    }

    /**
     * Activa el registro de escritura anticipada (WAL), el tiempo de espera por
     * base de datos ocupada y la comprobacion de llaves foraneas.
     *
     * @param cn conexion recien abierta
     * @throws SQLException si alguno de los ajustes no se puede aplicar
     */
    private static void aplicarAjustesSqlite(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA busy_timeout=" + BUSY_TIMEOUT_MS);
            st.execute("PRAGMA foreign_keys=ON");
        }
    }

    /**
     * Describe la conexion configurada, para mostrarla al arrancar el servidor.
     *
     * @return una linea con el motor y el URL en uso
     */
    public static String describir() {
        return "motor=sqlite, url=" + getUrl();
    }
}

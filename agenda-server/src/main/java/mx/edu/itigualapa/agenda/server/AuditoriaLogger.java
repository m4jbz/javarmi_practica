package mx.edu.itigualapa.agenda.server;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Registro de auditoria de todas las operaciones que atiende el servidor.
 *
 * <p>Se apoya en {@code java.util.logging}: un {@link FileHandler} sincroniza
 * internamente las escrituras, asi que varios hilos de RMI pueden registrar al
 * mismo tiempo sin que las lineas se entremezclen ni se pierdan.</p>
 *
 * <p>Cada linea del archivo {@code logs/auditoria.log} tiene el formato:</p>
 * <pre>
 * fecha-hora | cliente | hilo | operacion | parametros | resultado
 * </pre>
 */
public final class AuditoriaLogger {

    /** Carpeta donde se guarda el archivo de auditoria. */
    private static final String CARPETA_LOGS = "logs";

    /** Ruta del archivo de auditoria. */
    private static final String ARCHIVO_LOG = CARPETA_LOGS + "/auditoria.log";

    /** Formato de la marca de tiempo de cada linea. */
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Registrador subyacente. */
    private static final Logger LOG = Logger.getLogger("auditoria.agenda");

    static {
        configurar();
    }

    /** Clase de utilidad: no se instancia. */
    private AuditoriaLogger() {
    }

    /**
     * Prepara el registrador: crea la carpeta de bitacoras y le asocia un
     * manejador de archivo con formato propio de una sola linea.
     */
    private static void configurar() {
        try {
            File carpeta = new File(CARPETA_LOGS);
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                System.err.println("[LOG] No se pudo crear la carpeta " + CARPETA_LOGS);
            }

            LOG.setUseParentHandlers(false);   // no duplicar en la consola
            LOG.setLevel(Level.ALL);

            // append = true: conserva el historial entre ejecuciones.
            FileHandler manejador = new FileHandler(ARCHIVO_LOG, true);
            manejador.setEncoding("UTF-8");
            manejador.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord registro) {
                    return registro.getMessage() + System.lineSeparator();
                }
            });
            LOG.addHandler(manejador);

            System.out.println("[LOG] Auditoria activa en: "
                    + new File(ARCHIVO_LOG).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[LOG] No se pudo abrir el archivo de auditoria: " + e.getMessage());
        }
    }

    /**
     * Escribe una operacion terminada con exito.
     *
     * @param cliente    identificacion del cliente (host o nombre de sesion)
     * @param operacion  nombre del metodo remoto invocado
     * @param parametros parametros relevantes de la llamada
     * @param detalle    resumen del resultado
     */
    public static void exito(String cliente, String operacion, String parametros, String detalle) {
        escribir(cliente, operacion, parametros, "OK" + (detalle == null || detalle.isEmpty() ? "" : " - " + detalle));
    }

    /**
     * Escribe una operacion que termino con error.
     *
     * @param cliente    identificacion del cliente (host o nombre de sesion)
     * @param operacion  nombre del metodo remoto invocado
     * @param parametros parametros relevantes de la llamada
     * @param motivo     causa del error
     */
    public static void error(String cliente, String operacion, String parametros, String motivo) {
        escribir(cliente, operacion, parametros, "ERROR - " + motivo);
    }

    /**
     * Escribe un evento del propio servidor (arranque, paro, sesiones).
     *
     * @param operacion nombre del evento
     * @param detalle   descripcion del evento
     */
    public static void evento(String operacion, String detalle) {
        escribir("SERVIDOR", operacion, "-", detalle);
    }

    /**
     * Arma y envia la linea de bitacora.
     *
     * @param cliente    identificacion del cliente
     * @param operacion  nombre de la operacion
     * @param parametros parametros de la llamada
     * @param resultado  resultado ya formateado
     */
    private static void escribir(String cliente, String operacion, String parametros, String resultado) {
        String linea = String.join(" | ",
                LocalDateTime.now().format(FORMATO_FECHA),
                nn(cliente),
                Thread.currentThread().getName(),
                nn(operacion),
                nn(parametros),
                nn(resultado));
        LOG.info(linea);
    }

    /**
     * Sustituye los valores nulos o vacios por un guion, para que todas las
     * lineas tengan el mismo numero de columnas.
     *
     * @param s texto a normalizar
     * @return el texto, o {@code "-"} si venia vacio
     */
    private static String nn(String s) {
        return (s == null || s.isEmpty()) ? "-" : s.replace("\n", " ").replace("|", "/");
    }
}

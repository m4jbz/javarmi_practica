package mx.edu.itigualapa.agenda.server;

import mx.edu.itigualapa.agenda.common.AgendaRemota;

import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

/**
 * Programa principal del servidor de la agenda.
 *
 * <p>Sus responsabilidades, en orden:</p>
 * <ol>
 *   <li>Verificar el esquema de la base de datos.</li>
 *   <li>Crear el objeto remoto {@link AgendaRemotaImpl} y exportarlo en un
 *       puerto fijo.</li>
 *   <li>Localizar el registro RMI en el puerto 1099 y, si no hay ninguno
 *       activo, crear uno dentro de esta misma maquina virtual.</li>
 *   <li>Publicar el servicio con el nombre
 *       {@link AgendaRemota#NOMBRE_SERVICIO}.</li>
 *   <li>Quedarse a la espera de los clientes hasta que se interrumpa, momento
 *       en el que un <em>shutdown hook</em> retira el servicio del registro y
 *       deja de exportar el objeto.</li>
 * </ol>
 *
 * <p>Uso:</p>
 * <pre>
 *   java -jar agenda-server.jar [puertoRegistro] [puertoObjeto] [hostname]
 *   java -Djava.rmi.server.hostname=192.168.1.10 -jar agenda-server.jar
 * </pre>
 */
public final class ServidorAgenda {

    /** Puerto por defecto del registro RMI. */
    private static final int PUERTO_REGISTRO_DEF = AgendaRemota.PUERTO_REGISTRO;

    /**
     * Puerto fijo por defecto en el que se exporta el objeto remoto. Se fija en
     * lugar de dejar que RMI elija uno al azar para poder abrirlo en el
     * cortafuegos.
     */
    private static final int PUERTO_OBJETO_DEF = 1100;

    /** Formato de la marca de tiempo del encabezado. */
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Se libera cuando el servidor debe terminar. */
    private static final CountDownLatch ENCENDIDO = new CountDownLatch(1);

    /** Clase con solo el metodo principal: no se instancia. */
    private ServidorAgenda() {
    }

    /**
     * Arranca el servidor.
     *
     * @param args {@code [0]} puerto del registro, {@code [1]} puerto del
     *             objeto remoto, {@code [2]} nombre o IP a anunciar
     */
    public static void main(String[] args) {
        int puertoRegistro = leerPuerto(args, 0, PUERTO_REGISTRO_DEF);
        int puertoObjeto = leerPuerto(args, 1, PUERTO_OBJETO_DEF);

        // Direccion que el servidor anuncia dentro del stub. Es imprescindible
        // fijarla cuando el cliente esta en otra maquina, porque de lo
        // contrario RMI puede anunciar 127.0.0.1 y el cliente no sabra
        // regresar la llamada.
        if (args.length > 2 && !args[2].isBlank()) {
            System.setProperty("java.rmi.server.hostname", args[2].trim());
        }

        imprimirEncabezado(puertoRegistro, puertoObjeto);

        try {
            // 1) Base de datos.
            EsquemaBD.inicializar();

            // 2) Objeto remoto exportado en un puerto fijo.
            AgendaRemotaImpl agenda = new AgendaRemotaImpl(puertoObjeto);

            // 3) Registro RMI: se reutiliza el que ya exista o se crea uno.
            Registry registro = obtenerRegistro(puertoRegistro);

            // 4) Publicacion del servicio. rebind sustituye cualquier enlace
            //    previo con el mismo nombre, asi que el servidor se puede
            //    reiniciar sin errores de "nombre ya ligado".
            registro.rebind(AgendaRemota.NOMBRE_SERVICIO, agenda);

            System.out.println("[RMI] Servicio publicado como '"
                    + AgendaRemota.NOMBRE_SERVICIO + "'.");
            System.out.println("[RMI] Objeto remoto exportado en el puerto " + puertoObjeto + ".");
            System.out.println("[RMI] java.rmi.server.hostname = "
                    + System.getProperty("java.rmi.server.hostname", "(no fijado)"));
            System.out.println();
            System.out.println("Servidor listo. Esperando clientes... (Ctrl+C para detener)");
            System.out.println("-".repeat(78));

            AuditoriaLogger.evento("arranque",
                    "servidor activo, registro=" + puertoRegistro
                    + ", objeto=" + puertoObjeto + ", " + ConexionBD.describir());

            // 5) Cierre ordenado.
            instalarShutdownHook(registro, agenda);

            // 6) Espera indefinida: el trabajo real lo hacen los hilos de RMI.
            ENCENDIDO.await();

        } catch (SQLException e) {
            System.err.println("[ERROR] No se pudo preparar la base de datos: " + e.getMessage());
            System.err.println("        Revisa db.properties y que exista la carpeta db/.");
            AuditoriaLogger.error("SERVIDOR", "arranque", "-", "BD: " + e.getMessage());
            System.exit(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo iniciar el servidor: " + e);
            AuditoriaLogger.error("SERVIDOR", "arranque", "-", e.toString());
            System.exit(1);
        }
    }

    /**
     * Devuelve el registro RMI del puerto indicado: intenta usar uno que ya
     * este corriendo (por ejemplo, lanzado con el comando {@code rmiregistry})
     * y, si no responde, crea uno dentro de esta maquina virtual.
     *
     * @param puerto puerto del registro
     * @return un registro utilizable
     * @throws Exception si no se puede obtener ni crear el registro
     */
    private static Registry obtenerRegistro(int puerto) throws Exception {
        try {
            Registry existente = LocateRegistry.getRegistry(puerto);
            // getRegistry no conecta: hay que invocar algo para saber si vive.
            existente.list();
            System.out.println("[RMI] Se reutiliza el registro que ya escucha en el puerto "
                    + puerto + ".");
            return existente;
        } catch (Exception sinRegistro) {
            Registry nuevo = LocateRegistry.createRegistry(puerto);
            System.out.println("[RMI] No habia registro activo; se creo uno en el puerto "
                    + puerto + ".");
            return nuevo;
        }
    }

    /**
     * Instala el gancho de apagado que retira el servicio del registro y deja
     * de exportar el objeto remoto, para que el proceso termine limpio y el
     * puerto quede libre.
     *
     * @param registro registro donde esta publicado el servicio
     * @param agenda   objeto remoto exportado
     */
    private static void instalarShutdownHook(Registry registro, AgendaRemotaImpl agenda) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("[RMI] Deteniendo el servidor...");
            System.out.println("[RMI] Operaciones atendidas: " + agenda.getOperacionesAtendidas()
                    + " | sesiones abiertas al cierre: " + agenda.getSesionesActivas());
            try {
                registro.unbind(AgendaRemota.NOMBRE_SERVICIO);
                System.out.println("[RMI] Servicio retirado del registro.");
            } catch (NotBoundException | java.rmi.RemoteException e) {
                System.out.println("[RMI] El servicio ya no estaba ligado.");
            }
            try {
                UnicastRemoteObject.unexportObject(agenda, true);
                System.out.println("[RMI] Objeto remoto liberado.");
            } catch (NoSuchObjectException e) {
                System.out.println("[RMI] El objeto ya no estaba exportado.");
            }
            AuditoriaLogger.evento("paro",
                    "servidor detenido, operaciones=" + agenda.getOperacionesAtendidas());
            ENCENDIDO.countDown();
        }, "apagado-servidor"));
    }

    /**
     * Lee un puerto de los argumentos de la linea de comandos.
     *
     * @param args         argumentos recibidos
     * @param indice       posicion a leer
     * @param porDefecto   valor a usar si el argumento falta o no es un numero
     * @return el puerto a utilizar
     */
    private static int leerPuerto(String[] args, int indice, int porDefecto) {
        if (args.length > indice) {
            try {
                return Integer.parseInt(args[indice].trim());
            } catch (NumberFormatException e) {
                System.err.println("[AVISO] '" + args[indice]
                        + "' no es un puerto valido; se usara " + porDefecto + ".");
            }
        }
        return porDefecto;
    }

    /**
     * Imprime el encabezado informativo del servidor.
     *
     * @param puertoRegistro puerto del registro RMI
     * @param puertoObjeto   puerto del objeto remoto
     */
    private static void imprimirEncabezado(int puertoRegistro, int puertoObjeto) {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName()
                    + " / " + InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            host = "(no determinado)";
        }

        System.out.println("=".repeat(78));
        System.out.println("  SERVIDOR DE AGENDA PERSONAL - Java RMI + JDBC");
        System.out.println("  Instituto Tecnologico de Iguala | IFF-1019 Programacion en");
        System.out.println("  Ambiente Cliente Servidor | Tema 3 RMI - Practica 1");
        System.out.println("=".repeat(78));
        System.out.println("  Fecha y hora    : " + LocalDateTime.now().format(FECHA_HORA));
        System.out.println("  Equipo          : " + host);
        System.out.println("  Java            : " + System.getProperty("java.version"));
        System.out.println("  Base de datos   : " + ConexionBD.describir());
        System.out.println("  Registro RMI    : puerto " + puertoRegistro);
        System.out.println("  Objeto remoto   : puerto " + puertoObjeto);
        System.out.println("=".repeat(78));
    }
}

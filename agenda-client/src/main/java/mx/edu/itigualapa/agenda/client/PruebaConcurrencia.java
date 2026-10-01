package mx.edu.itigualapa.agenda.client;

import mx.edu.itigualapa.agenda.common.AgendaRemota;
import mx.edu.itigualapa.agenda.common.Contacto;
import mx.edu.itigualapa.agenda.common.ValidacionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prueba de concurrencia del servidor de la agenda.
 *
 * <p>Lanza N clientes simulados con un {@link ExecutorService}. Todos esperan
 * en una misma barrera ({@link CountDownLatch}) y arrancan a la vez, de manera
 * que el servidor recibe las llamadas realmente superpuestas y no una detras
 * de otra.</p>
 *
 * <p>Cada cliente ejecuta siempre la misma secuencia, lo que hace que el
 * resultado final sea predecible y verificable:</p>
 * <ol>
 *   <li>Abre sesion con {@code conectar}.</li>
 *   <li>Da de alta 3 contactos propios.</li>
 *   <li>Busca por nombre y lista la agenda completa (lecturas simultaneas).</li>
 *   <li>Actualiza el primero de sus contactos.</li>
 *   <li>Elimina el tercero de sus contactos.</li>
 *   <li>Cierra sesion con {@code desconectar}.</li>
 * </ol>
 *
 * <p>Saldo neto: cada cliente deja 2 contactos nuevos. Al terminar, el programa
 * comprueba tres cosas y las resume como OK o FALLA:</p>
 * <ul>
 *   <li>El total que informa {@code listarContactos} coincide con lo esperado.</li>
 *   <li>Una consulta SQL directa a la base de datos devuelve el mismo total
 *       (demuestra que los datos quedaron realmente escritos).</li>
 *   <li>Los contactos actualizados tienen el nombre nuevo y los eliminados ya
 *       no existen.</li>
 * </ul>
 *
 * <p>Uso:</p>
 * <pre>
 *   java -cp agenda-client.jar mx.edu.itigualapa.agenda.client.PruebaConcurrencia \
 *        [host] [puerto] [numeroClientes] [rutaSqlite]
 * </pre>
 */
public final class PruebaConcurrencia {

    /** Numero de clientes simulados por defecto. */
    private static final int CLIENTES_DEF = 10;

    /** Contactos que da de alta cada cliente. */
    private static final int ALTAS_POR_CLIENTE = 3;

    /** Contactos que elimina cada cliente al final. */
    private static final int BAJAS_POR_CLIENTE = 1;

    /** Ruta por defecto del archivo de SQLite para la verificacion directa. */
    private static final String SQLITE_DEF = "db/agenda.db";

    /** Prefijo de los correos generados por la prueba. */
    private static final String PREFIJO_EMAIL = "pruebacc";

    /** Ancho de las lineas decorativas. */
    private static final int ANCHO = 78;

    /** Formato de la marca de tiempo del informe. */
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Clase con solo el metodo principal: no se instancia. */
    private PruebaConcurrencia() {
    }

    /**
     * Ejecuta la prueba.
     *
     * @param args {@code [0]} host, {@code [1]} puerto, {@code [2]} numero de
     *             clientes, {@code [3]} ruta del archivo SQLite
     * @throws Exception si la prueba no puede siquiera contactar al servidor
     */
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0].trim() : "localhost";
        int puerto = args.length > 1 ? enteroODefecto(args[1], AgendaRemota.PUERTO_REGISTRO)
                : AgendaRemota.PUERTO_REGISTRO;
        int numClientes = args.length > 2 ? enteroODefecto(args[2], CLIENTES_DEF) : CLIENTES_DEF;
        String rutaSqlite = args.length > 3 ? args[3].trim() : SQLITE_DEF;

        encabezado(host, puerto, numClientes, rutaSqlite);

        // Conexion de control: mide el estado antes y despues.
        Registry registro = LocateRegistry.getRegistry(host, puerto);
        AgendaRemota control = (AgendaRemota) registro.lookup(AgendaRemota.NOMBRE_SERVICIO);

        int totalInicial = control.totalContactos();
        System.out.println("Contactos en la agenda antes de la prueba: " + totalInicial);
        System.out.println();

        // Estructuras compartidas entre los hilos.
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch meta = new CountDownLatch(numClientes);
        AtomicInteger altasOk = new AtomicInteger();
        AtomicInteger bajasOk = new AtomicInteger();
        AtomicInteger cambiosOk = new AtomicInteger();
        AtomicInteger lecturas = new AtomicInteger();
        AtomicInteger fallos = new AtomicInteger();
        Map<String, String> nombresEsperados = new ConcurrentHashMap<>();
        List<String> emailsEliminados = Collections.synchronizedList(new ArrayList<>());
        List<String> incidencias = Collections.synchronizedList(new ArrayList<>());

        ExecutorService piscina = Executors.newFixedThreadPool(numClientes);
        long inicio;

        for (int i = 1; i <= numClientes; i++) {
            final int idCliente = i;
            piscina.submit(() -> {
                try {
                    salida.await();   // todos arrancan al mismo tiempo
                    AgendaRemota agenda = (AgendaRemota) registro.lookup(AgendaRemota.NOMBRE_SERVICIO);
                    String sesion = agenda.conectar("prueba-" + idCliente);

                    List<Contacto> mios = new ArrayList<>();

                    // --- Altas ---
                    for (int k = 1; k <= ALTAS_POR_CLIENTE; k++) {
                        Contacto c = new Contacto(
                                nombreGenerado(idCliente, k),
                                telefonoGenerado(idCliente, k),
                                emailGenerado(idCliente, k));
                        try {
                            mios.add(agenda.agregarContacto(c));
                            altasOk.incrementAndGet();
                        } catch (ValidacionException e) {
                            fallos.incrementAndGet();
                            incidencias.add("cliente " + idCliente + " alta " + k + ": " + e.getMessage());
                        }
                    }

                    // --- Lecturas simultaneas ---
                    agenda.buscarPorNombre("Prueba");
                    agenda.listarContactos();
                    lecturas.addAndGet(2);

                    // --- Actualizacion del primer contacto ---
                    if (!mios.isEmpty()) {
                        Contacto primero = mios.get(0);
                        String nombreNuevo = nombreGenerado(idCliente, 1) + " Modificado";
                        primero.setNombre(nombreNuevo);
                        try {
                            if (agenda.actualizarContacto(primero)) {
                                cambiosOk.incrementAndGet();
                                nombresEsperados.put(primero.getEmail(), nombreNuevo);
                            }
                        } catch (ValidacionException e) {
                            fallos.incrementAndGet();
                            incidencias.add("cliente " + idCliente + " actualizacion: " + e.getMessage());
                        }
                    }

                    // --- Baja del ultimo contacto ---
                    if (mios.size() >= ALTAS_POR_CLIENTE) {
                        Contacto ultimo = mios.get(ALTAS_POR_CLIENTE - 1);
                        if (agenda.eliminarContacto(ultimo.getId())) {
                            bajasOk.incrementAndGet();
                            emailsEliminados.add(ultimo.getEmail());
                        }
                    }

                    agenda.desconectar(sesion);

                } catch (Exception e) {
                    fallos.incrementAndGet();
                    incidencias.add("cliente " + idCliente + ": " + e);
                } finally {
                    meta.countDown();
                }
            });
        }

        System.out.println("Lanzando " + numClientes + " clientes simultaneos...");
        inicio = System.nanoTime();
        salida.countDown();                       // se abre la barrera
        meta.await(3, TimeUnit.MINUTES);          // se espera a que todos terminen
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        piscina.shutdown();
        if (!piscina.awaitTermination(30, TimeUnit.SECONDS)) {
            piscina.shutdownNow();
        }

        // ==============================================================
        // Verificacion
        // ==============================================================
        int esperado = totalInicial
                + numClientes * (ALTAS_POR_CLIENTE - BAJAS_POR_CLIENTE);

        List<Contacto> finales = control.listarContactos();
        int totalRemoto = finales.size();
        int totalPorMetodo = control.totalContactos();
        int totalDirecto = contarDirectoEnBD(rutaSqlite);

        boolean okConteoRemoto = totalRemoto == esperado && totalPorMetodo == esperado;
        boolean okConteoDirecto = totalDirecto < 0 || totalDirecto == esperado;
        boolean okSinFallos = fallos.get() == 0;

        // Comprobacion de los nombres actualizados.
        Map<String, String> porEmail = new ConcurrentHashMap<>();
        for (Contacto c : finales) {
            porEmail.put(c.getEmail(), c.getNombre());
        }
        int cambiosVerificados = 0;
        List<String> cambiosMal = new ArrayList<>();
        for (Map.Entry<String, String> e : nombresEsperados.entrySet()) {
            String real = porEmail.get(e.getKey());
            if (e.getValue().equals(real)) {
                cambiosVerificados++;
            } else {
                cambiosMal.add(e.getKey() + " esperaba '" + e.getValue() + "' y tiene '" + real + "'");
            }
        }
        boolean okCambios = cambiosMal.isEmpty();

        // Comprobacion de que los eliminados ya no estan.
        List<String> bajasMal = new ArrayList<>();
        for (String email : emailsEliminados) {
            if (porEmail.containsKey(email)) {
                bajasMal.add(email);
            }
        }
        boolean okBajas = bajasMal.isEmpty();

        boolean todoOk = okConteoRemoto && okConteoDirecto && okSinFallos && okCambios && okBajas;

        // ==============================================================
        // Informe
        // ==============================================================
        System.out.println();
        System.out.println("=".repeat(ANCHO));
        System.out.println("  RESUMEN DE LA PRUEBA DE CONCURRENCIA");
        System.out.println("=".repeat(ANCHO));
        System.out.printf("  Clientes simultaneos        : %d%n", numClientes);
        System.out.printf("  Duracion total              : %d ms%n", ms);
        System.out.printf("  Altas realizadas            : %d de %d%n",
                altasOk.get(), numClientes * ALTAS_POR_CLIENTE);
        System.out.printf("  Actualizaciones realizadas  : %d de %d%n", cambiosOk.get(), numClientes);
        System.out.printf("  Bajas realizadas            : %d de %d%n", bajasOk.get(), numClientes);
        System.out.printf("  Lecturas realizadas         : %d%n", lecturas.get());
        System.out.printf("  Errores inesperados         : %d%n", fallos.get());
        System.out.println("-".repeat(ANCHO));
        System.out.printf("  Contactos antes             : %d%n", totalInicial);
        System.out.printf("  Contactos esperados         : %d%n", esperado);
        System.out.printf("  listarContactos()           : %d   %s%n",
                totalRemoto, marca(totalRemoto == esperado));
        System.out.printf("  totalContactos()            : %d   %s%n",
                totalPorMetodo, marca(totalPorMetodo == esperado));
        if (totalDirecto >= 0) {
            System.out.printf("  SELECT COUNT(*) directo     : %d   %s%n",
                    totalDirecto, marca(totalDirecto == esperado));
        } else {
            System.out.printf("  SELECT COUNT(*) directo     : no disponible (%s)%n", rutaSqlite);
        }
        System.out.printf("  Nombres actualizados        : %d de %d   %s%n",
                cambiosVerificados, nombresEsperados.size(), marca(okCambios));
        System.out.printf("  Eliminados ausentes         : %d de %d   %s%n",
                emailsEliminados.size() - bajasMal.size(), emailsEliminados.size(), marca(okBajas));
        System.out.println("=".repeat(ANCHO));
        System.out.println(todoOk
                ? "  RESULTADO: OK - todos los datos quedaron consistentes en la base de datos."
                : "  RESULTADO: FALLA - revisa los detalles de arriba.");
        System.out.println("=".repeat(ANCHO));

        if (!incidencias.isEmpty()) {
            System.out.println("Incidencias:");
            incidencias.forEach(s -> System.out.println("  - " + s));
        }
        if (!cambiosMal.isEmpty()) {
            System.out.println("Actualizaciones no verificadas:");
            cambiosMal.forEach(s -> System.out.println("  - " + s));
        }
        if (!bajasMal.isEmpty()) {
            System.out.println("Contactos que debieron eliminarse y siguen presentes:");
            bajasMal.forEach(s -> System.out.println("  - " + s));
        }

        System.exit(todoOk ? 0 : 1);
    }

    /**
     * Cuenta los contactos con una consulta SQL directa al archivo de SQLite,
     * sin pasar por RMI. Es la comprobacion de que la informacion quedo
     * realmente almacenada.
     *
     * @param rutaSqlite ruta del archivo de base de datos
     * @return el numero de renglones, o {@code -1} si no se pudo consultar
     */
    private static int contarDirectoEnBD(String rutaSqlite) {
        String url = "jdbc:sqlite:" + rutaSqlite;
        try (Connection cn = DriverManager.getConnection(url);
             PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM Contactos");
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException e) {
            System.out.println("[AVISO] No se pudo consultar directamente " + rutaSqlite
                    + ": " + e.getMessage());
            return -1;
        }
    }

    /**
     * Genera un nombre valido (solo letras y espacios) para el contacto de prueba.
     *
     * @param cliente numero del cliente simulado
     * @param indice  numero del contacto dentro de ese cliente
     * @return un nombre unico y valido
     */
    private static String nombreGenerado(int cliente, int indice) {
        return "Prueba Cliente " + enLetras(cliente) + " Contacto " + enLetras(indice);
    }

    /**
     * Genera un telefono de exactamente 10 digitos.
     *
     * @param cliente numero del cliente simulado
     * @param indice  numero del contacto dentro de ese cliente
     * @return un telefono valido
     */
    private static String telefonoGenerado(int cliente, int indice) {
        long base = 7330000000L + cliente * 1000L + indice;
        return String.valueOf(base);
    }

    /**
     * Genera un correo unico para el contacto de prueba.
     *
     * @param cliente numero del cliente simulado
     * @param indice  numero del contacto dentro de ese cliente
     * @return un correo unico y valido
     */
    private static String emailGenerado(int cliente, int indice) {
        return PREFIJO_EMAIL + cliente + "." + indice + "@itiguala.edu.mx";
    }

    /**
     * Convierte un numero en una cadena de solo letras, para que los nombres
     * generados cumplan la validacion (que no admite digitos).
     *
     * @param n numero a convertir
     * @return la representacion en letras (1 -> "A", 27 -> "AA")
     */
    private static String enLetras(int n) {
        StringBuilder sb = new StringBuilder();
        int v = Math.max(n, 1);
        while (v > 0) {
            int resto = (v - 1) % 26;
            sb.insert(0, (char) ('A' + resto));
            v = (v - 1) / 26;
        }
        return sb.toString();
    }

    /**
     * Convierte un texto a entero devolviendo un valor por defecto si falla.
     *
     * @param texto      texto a convertir
     * @param porDefecto valor a usar si el texto no es un numero
     * @return el entero resultante
     */
    private static int enteroODefecto(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /**
     * Devuelve la marca visual de una comprobacion.
     *
     * @param ok resultado de la comprobacion
     * @return {@code "[OK]"} o {@code "[FALLA]"}
     */
    private static String marca(boolean ok) {
        return ok ? "[OK]" : "[FALLA]";
    }

    /**
     * Imprime el encabezado de la prueba.
     *
     * @param host        servidor a probar
     * @param puerto      puerto del registro
     * @param clientes    numero de clientes simultaneos
     * @param rutaSqlite  archivo de base de datos a verificar
     */
    private static void encabezado(String host, int puerto, int clientes, String rutaSqlite) {
        System.out.println("=".repeat(ANCHO));
        System.out.println("  PRUEBA DE CONCURRENCIA - Agenda Personal RMI");
        System.out.println("  Instituto Tecnologico de Iguala | IFF-1019");
        System.out.println("=".repeat(ANCHO));
        System.out.println("  Fecha y hora : " + LocalDateTime.now().format(FECHA_HORA));
        System.out.println("  Servidor     : " + host + ":" + puerto);
        System.out.println("  Clientes     : " + clientes + " hilos simultaneos");
        System.out.println("  Secuencia    : " + ALTAS_POR_CLIENTE + " altas, 2 lecturas, "
                + "1 actualizacion y " + BAJAS_POR_CLIENTE + " baja por cliente");
        System.out.println("  BD directa   : " + rutaSqlite);
        System.out.println("=".repeat(ANCHO));
        System.out.println();
    }
}

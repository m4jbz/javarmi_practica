package mx.edu.itigualapa.agenda.client;

import mx.edu.itigualapa.agenda.common.AgendaRemota;
import mx.edu.itigualapa.agenda.common.Contacto;
import mx.edu.itigualapa.agenda.common.ValidacionException;
import mx.edu.itigualapa.agenda.common.Validador;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente de la agenda con interfaz de texto.
 *
 * <p>Localiza el servicio en el registro RMI, obtiene el stub de
 * {@link AgendaRemota} y a partir de ahi invoca los metodos remotos como si
 * fueran locales. Presenta un menu con las cinco operaciones de la practica
 * mas la salida.</p>
 *
 * <p>Uso:</p>
 * <pre>
 *   java -jar agenda-client.jar [host] [puerto] [nombreCliente]
 * </pre>
 * <p>Por defecto se conecta a {@code localhost:1099}.</p>
 */
public final class ClienteConsola {

    /** Ancho de las lineas decorativas. */
    private static final int ANCHO = 78;

    /** Stub del objeto remoto. */
    private final AgendaRemota agenda;

    /** Identificador de la sesion abierta en el servidor. */
    private final String idSesion;

    /** Lector de la entrada estandar. */
    private final Scanner entrada;

    /**
     * Crea el cliente con un servicio ya localizado.
     *
     * @param agenda   stub del servicio remoto
     * @param idSesion identificador de sesion entregado por el servidor
     * @param entrada  lector de la entrada estandar
     */
    private ClienteConsola(AgendaRemota agenda, String idSesion, Scanner entrada) {
        this.agenda = agenda;
        this.idSesion = idSesion;
        this.entrada = entrada;
    }

    /**
     * Punto de entrada del cliente de consola.
     *
     * @param args {@code [0]} host del servidor, {@code [1]} puerto del
     *             registro, {@code [2]} nombre con el que identificarse
     */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0].trim() : "localhost";
        int puerto = AgendaRemota.PUERTO_REGISTRO;
        if (args.length > 1) {
            try {
                puerto = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException e) {
                System.out.println("[AVISO] Puerto invalido; se usara "
                        + AgendaRemota.PUERTO_REGISTRO + ".");
            }
        }
        String nombreCliente = args.length > 2
                ? args[2].trim()
                : "consola-" + System.getProperty("user.name", "alumno");

        titulo(host, puerto, nombreCliente);

        AgendaRemota agenda = localizarServicio(host, puerto);
        if (agenda == null) {
            System.exit(1);
        }

        Scanner sc = new Scanner(System.in);
        String sesion = null;
        try {
            sesion = agenda.conectar(nombreCliente);
            System.out.println("Conectado. Sesion asignada: " + sesion);
            System.out.println();

            new ClienteConsola(agenda, sesion, sc).ejecutarMenu();

        } catch (RemoteException e) {
            System.out.println();
            System.out.println("[ERROR] Se perdio la comunicacion con el servidor: " + e.getMessage());
        } finally {
            cerrarSesion(agenda, sesion);
            sc.close();
        }
    }

    /**
     * Busca el servicio en el registro RMI y explica el problema si no responde.
     *
     * @param host   nombre o direccion del servidor
     * @param puerto puerto del registro RMI
     * @return el stub del servicio, o {@code null} si no se pudo localizar
     */
    private static AgendaRemota localizarServicio(String host, int puerto) {
        try {
            Registry registro = LocateRegistry.getRegistry(host, puerto);
            AgendaRemota agenda = (AgendaRemota) registro.lookup(AgendaRemota.NOMBRE_SERVICIO);
            System.out.println("Servicio '" + AgendaRemota.NOMBRE_SERVICIO + "' localizado en "
                    + host + ":" + puerto + ".");
            return agenda;

        } catch (NotBoundException e) {
            System.out.println("[ERROR] El registro RMI de " + host + ":" + puerto
                    + " responde, pero no tiene publicado el servicio '"
                    + AgendaRemota.NOMBRE_SERVICIO + "'.");
            System.out.println("        Verifica que el servidor haya terminado de arrancar.");
        } catch (ConnectException e) {
            System.out.println("[ERROR] No hay ningun servidor escuchando en " + host + ":" + puerto + ".");
            System.out.println("        Arranca primero el servidor con scripts/iniciar-servidor.bat");
            System.out.println("        (o scripts/iniciar-servidor.sh en Linux).");
        } catch (RemoteException e) {
            System.out.println("[ERROR] No se pudo contactar al servidor: " + e.getMessage());
            System.out.println("        Revisa la direccion, el puerto y el cortafuegos.");
        }
        return null;
    }

    /**
     * Cierra la sesion en el servidor, si se llego a abrir.
     *
     * @param agenda   stub del servicio
     * @param idSesion identificador de la sesion a cerrar
     */
    private static void cerrarSesion(AgendaRemota agenda, String idSesion) {
        if (agenda != null && idSesion != null) {
            try {
                agenda.desconectar(idSesion);
                System.out.println("Sesion " + idSesion + " cerrada en el servidor.");
            } catch (RemoteException e) {
                System.out.println("[AVISO] No se pudo avisar al servidor del cierre: "
                        + e.getMessage());
            }
        }
        System.out.println("Hasta luego.");
    }

    // ==================================================================
    // Menu
    // ==================================================================

    /**
     * Muestra el menu y atiende las opciones hasta que el usuario elige salir
     * o se agota la entrada estandar.
     *
     * @throws RemoteException si se pierde la comunicacion con el servidor
     */
    private void ejecutarMenu() throws RemoteException {
        boolean continuar = true;
        while (continuar) {
            mostrarMenu();
            String opcion = leerLinea("Opcion: ");
            if (opcion == null) {
                System.out.println();
                System.out.println("(Fin de la entrada; se cierra el cliente.)");
                return;
            }

            System.out.println();
            switch (opcion.trim()) {
                case "1" -> agregar();
                case "2" -> buscar();
                case "3" -> listar();
                case "4" -> actualizar();
                case "5" -> eliminar();
                case "0" -> continuar = false;
                case "" -> { /* renglon vacio: se vuelve a mostrar el menu */ }
                default -> System.out.println("Opcion no valida. Elige un numero del 0 al 5.");
            }
            System.out.println();
        }
    }

    /** Imprime las opciones disponibles. */
    private void mostrarMenu() {
        System.out.println("-".repeat(ANCHO));
        System.out.println("  AGENDA PERSONAL  |  sesion: " + idSesion);
        System.out.println("-".repeat(ANCHO));
        System.out.println("  1) Agregar contacto");
        System.out.println("  2) Buscar contacto (por id o por nombre)");
        System.out.println("  3) Listar todos los contactos");
        System.out.println("  4) Actualizar contacto");
        System.out.println("  5) Eliminar contacto");
        System.out.println("  0) Salir");
        System.out.println("-".repeat(ANCHO));
    }

    // ==================================================================
    // Operaciones
    // ==================================================================

    /**
     * Pide los datos de un contacto nuevo y lo envia al servidor.
     *
     * @throws RemoteException si se pierde la comunicacion con el servidor
     */
    private void agregar() throws RemoteException {
        System.out.println("== Agregar contacto ==");

        String nombre = leerLinea("Nombre   : ");
        if (nombre == null) {
            return;
        }
        String telefono = leerLinea("Telefono (10 digitos): ");
        if (telefono == null) {
            return;
        }
        String email = leerLinea("Email    : ");
        if (email == null) {
            return;
        }

        Contacto nuevo = new Contacto(nombre, telefono, email);

        // Validacion en el cliente: avisa de inmediato y evita un viaje por la
        // red. El servidor vuelve a validar de todos modos.
        try {
            Validador.validarYNormalizar(nuevo);
        } catch (ValidacionException e) {
            System.out.println("[DATO INVALIDO] " + e.getMessage());
            return;
        }

        try {
            Contacto guardado = agenda.agregarContacto(nuevo);
            System.out.println("[OK] Contacto guardado con id " + guardado.getId() + ".");
            imprimirTabla(List.of(guardado));
        } catch (ValidacionException e) {
            System.out.println("[RECHAZADO POR EL SERVIDOR] " + e.getMessage());
        }
    }

    /**
     * Busca por id o por nombre segun lo que escriba el usuario.
     *
     * @throws RemoteException si se pierde la comunicacion con el servidor
     */
    private void buscar() throws RemoteException {
        System.out.println("== Buscar contacto ==");
        System.out.println("  a) Por id");
        System.out.println("  b) Por nombre (coincidencia parcial)");

        String modo = leerLinea("Opcion: ");
        if (modo == null) {
            return;
        }

        switch (modo.trim().toLowerCase()) {
            case "a", "1", "id" -> {
                String texto = leerLinea("Id a buscar: ");
                if (texto == null) {
                    return;
                }
                Integer id = aEntero(texto);
                if (id == null) {
                    System.out.println("[DATO INVALIDO] El id debe ser un numero entero.");
                    return;
                }
                Contacto c = agenda.buscarPorId(id);
                if (c == null) {
                    System.out.println("No existe ningun contacto con el id " + id + ".");
                } else {
                    imprimirTabla(List.of(c));
                }
            }
            case "b", "2", "nombre" -> {
                String nombre = leerLinea("Texto a buscar en el nombre: ");
                if (nombre == null) {
                    return;
                }
                List<Contacto> lista = agenda.buscarPorNombre(nombre);
                if (lista.isEmpty()) {
                    System.out.println("Ningun contacto coincide con \"" + nombre + "\".");
                } else {
                    System.out.println(lista.size() + " coincidencia(s):");
                    imprimirTabla(lista);
                }
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    /**
     * Pide y muestra la lista completa de contactos.
     *
     * @throws RemoteException si se pierde la comunicacion con el servidor
     */
    private void listar() throws RemoteException {
        System.out.println("== Lista de contactos ==");
        List<Contacto> lista = agenda.listarContactos();
        if (lista.isEmpty()) {
            System.out.println("La agenda esta vacia.");
        } else {
            imprimirTabla(lista);
            System.out.println("Total: " + lista.size() + " contacto(s).");
        }
    }

    /**
     * Recupera un contacto, permite cambiar sus campos y envia la modificacion.
     *
     * @throws RemoteException si se pierde la comunicacion con el servidor
     */
    private void actualizar() throws RemoteException {
        System.out.println("== Actualizar contacto ==");

        String texto = leerLinea("Id del contacto a actualizar: ");
        if (texto == null) {
            return;
        }
        Integer id = aEntero(texto);
        if (id == null) {
            System.out.println("[DATO INVALIDO] El id debe ser un numero entero.");
            return;
        }

        Contacto actual = agenda.buscarPorId(id);
        if (actual == null) {
            System.out.println("No existe ningun contacto con el id " + id + ".");
            return;
        }

        System.out.println("Datos actuales:");
        imprimirTabla(List.of(actual));
        System.out.println("Deja el campo vacio para conservar el valor actual.");

        String nombre = leerLinea("Nombre   [" + actual.getNombre() + "]: ");
        if (nombre == null) {
            return;
        }
        String telefono = leerLinea("Telefono [" + actual.getTelefono() + "]: ");
        if (telefono == null) {
            return;
        }
        String email = leerLinea("Email    [" + actual.getEmail() + "]: ");
        if (email == null) {
            return;
        }

        Contacto modificado = new Contacto(
                id,
                nombre.isBlank() ? actual.getNombre() : nombre,
                telefono.isBlank() ? actual.getTelefono() : telefono,
                email.isBlank() ? actual.getEmail() : email);

        try {
            Validador.validarYNormalizar(modificado);
        } catch (ValidacionException e) {
            System.out.println("[DATO INVALIDO] " + e.getMessage());
            return;
        }

        try {
            if (agenda.actualizarContacto(modificado)) {
                System.out.println("[OK] Contacto actualizado.");
                imprimirTabla(List.of(agenda.buscarPorId(id)));
            } else {
                System.out.println("No se modifico ningun registro.");
            }
        } catch (ValidacionException e) {
            System.out.println("[RECHAZADO POR EL SERVIDOR] " + e.getMessage());
        }
    }

    /**
     * Elimina un contacto previa confirmacion del usuario.
     *
     * @throws RemoteException si se pierde la comunicacion con el servidor
     */
    private void eliminar() throws RemoteException {
        System.out.println("== Eliminar contacto ==");

        String texto = leerLinea("Id del contacto a eliminar: ");
        if (texto == null) {
            return;
        }
        Integer id = aEntero(texto);
        if (id == null) {
            System.out.println("[DATO INVALIDO] El id debe ser un numero entero.");
            return;
        }

        Contacto c = agenda.buscarPorId(id);
        if (c == null) {
            System.out.println("No existe ningun contacto con el id " + id + ".");
            return;
        }

        System.out.println("Se eliminara este contacto:");
        imprimirTabla(List.of(c));

        String confirmacion = leerLinea("Confirmas la eliminacion? (s/n): ");
        if (confirmacion == null) {
            return;
        }
        if (!confirmacion.trim().equalsIgnoreCase("s")) {
            System.out.println("Operacion cancelada. No se elimino nada.");
            return;
        }

        if (agenda.eliminarContacto(id)) {
            System.out.println("[OK] Contacto eliminado.");
        } else {
            System.out.println("No se elimino ningun registro.");
        }
    }

    // ==================================================================
    // Presentacion y lectura
    // ==================================================================

    /**
     * Imprime una lista de contactos como una tabla alineada.
     *
     * @param lista contactos a mostrar
     */
    private void imprimirTabla(List<Contacto> lista) {
        String separador = "+------+----------------------------+--------------+--------------------------------+";
        System.out.println(separador);
        System.out.printf("| %-4s | %-26s | %-12s | %-30s |%n", "ID", "NOMBRE", "TELEFONO", "EMAIL");
        System.out.println(separador);
        for (Contacto c : lista) {
            if (c == null) {
                continue;
            }
            System.out.printf("| %-4d | %-26s | %-12s | %-30s |%n",
                    c.getId(),
                    recortar(c.getNombre(), 26),
                    recortar(c.getTelefono(), 12),
                    recortar(c.getEmail(), 30));
        }
        System.out.println(separador);
    }

    /**
     * Recorta un texto para que quepa en una columna.
     *
     * @param texto texto original
     * @param max   ancho maximo de la columna
     * @return el texto recortado si hacia falta
     */
    private String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + ".";
    }

    /**
     * Lee una linea de la entrada estandar.
     *
     * @param titulo texto a mostrar antes de leer
     * @return la linea leida, o {@code null} si la entrada se agoto
     */
    private String leerLinea(String titulo) {
        System.out.print(titulo);
        System.out.flush();
        if (!entrada.hasNextLine()) {
            return null;
        }
        String linea = entrada.nextLine();
        // Eco de la entrada: hace legible la evidencia cuando el cliente se
        // alimenta desde un archivo en lugar del teclado.
        if (System.console() == null) {
            System.out.println(linea);
        }
        return linea;
    }

    /**
     * Convierte un texto en entero sin lanzar excepciones.
     *
     * @param texto texto a convertir
     * @return el entero, o {@code null} si el texto no es un numero
     */
    private Integer aEntero(String texto) {
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Imprime el encabezado del cliente.
     *
     * @param host   servidor al que se conecta
     * @param puerto puerto del registro
     * @param nombre nombre con el que se identifica el cliente
     */
    private static void titulo(String host, int puerto, String nombre) {
        System.out.println("=".repeat(ANCHO));
        System.out.println("  CLIENTE DE AGENDA PERSONAL (consola) - Java RMI");
        System.out.println("  Instituto Tecnologico de Iguala | IFF-1019");
        System.out.println("=".repeat(ANCHO));
        System.out.println("  Servidor : " + host + ":" + puerto);
        System.out.println("  Cliente  : " + nombre);
        System.out.println("=".repeat(ANCHO));
    }
}

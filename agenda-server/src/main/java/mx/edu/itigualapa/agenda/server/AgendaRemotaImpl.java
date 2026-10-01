package mx.edu.itigualapa.agenda.server;

import mx.edu.itigualapa.agenda.common.AgendaRemota;
import mx.edu.itigualapa.agenda.common.Contacto;
import mx.edu.itigualapa.agenda.common.ValidacionException;
import mx.edu.itigualapa.agenda.common.Validador;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementacion del servicio remoto de la agenda personal.
 *
 * <p>Al extender {@link UnicastRemoteObject} el objeto queda exportado: RMI le
 * crea un punto de escucha y genera el stub que reciben los clientes. Se
 * exporta en un puerto fijo (configurable) en lugar de uno aleatorio, para que
 * sea posible abrirlo en el cortafuegos.</p>
 *
 * <h2>Concurrencia</h2>
 * <p>El entorno de ejecucion de RMI atiende las llamadas entrantes en hilos
 * distintos tomados de su propio conjunto de hilos, de modo que varios
 * clientes pueden estar dentro de esta clase al mismo tiempo. Por eso la
 * implementacion es segura para hilos:</p>
 * <ul>
 *   <li>Un {@link ReentrantReadWriteLock} coordina el acceso: las consultas
 *       toman el candado de <em>lectura</em>, que admite varios lectores a la
 *       vez, y las altas, bajas y modificaciones toman el de <em>escritura</em>,
 *       que es exclusivo. Asi nadie lee un estado a medio escribir.</li>
 *   <li>Las sesiones de los clientes se guardan en un
 *       {@link ConcurrentHashMap}, que no necesita bloqueo externo.</li>
 *   <li>El total de operaciones atendidas se lleva en un {@link AtomicInteger},
 *       que incrementa sin condiciones de carrera.</li>
 * </ul>
 *
 * <p>Cada operacion se imprime en la consola del servidor y se escribe en el
 * archivo de auditoria con la hora, el host del cliente, el nombre del hilo que
 * la atendio y el resultado.</p>
 */
public class AgendaRemotaImpl extends UnicastRemoteObject implements AgendaRemota {

    /** Identificador de version de serializacion. */
    private static final long serialVersionUID = 1L;

    /** Formato de la hora que se muestra en la consola del servidor. */
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Acceso a la tabla Contactos. */
    private final ContactoDAO dao = new ContactoDAO();

    /**
     * Candado de lectura y escritura. Permite lecturas simultaneas y serializa
     * las escrituras.
     */
    private final ReentrantReadWriteLock candado = new ReentrantReadWriteLock(true);

    /** Sesiones activas, indexadas por su identificador. */
    private final Map<String, String> sesiones = new ConcurrentHashMap<>();

    /** Numero de operaciones remotas atendidas desde el arranque. */
    private final AtomicInteger operaciones = new AtomicInteger(0);

    /** Consecutivo para generar identificadores de sesion unicos. */
    private final AtomicInteger consecutivoSesion = new AtomicInteger(0);

    /**
     * Exporta el objeto remoto en el puerto indicado.
     *
     * @param puertoObjeto puerto fijo en el que se exporta el objeto
     * @throws RemoteException si el objeto no se puede exportar
     */
    public AgendaRemotaImpl(int puertoObjeto) throws RemoteException {
        super(puertoObjeto);
    }

    // ==================================================================
    // Sesiones
    // ==================================================================

    /** {@inheritDoc} */
    @Override
    public String conectar(String nombreCliente) throws RemoteException {
        String host = hostCliente();
        String nombre = (nombreCliente == null || nombreCliente.isBlank())
                ? "cliente-anonimo" : nombreCliente.trim();
        String idSesion = "S" + consecutivoSesion.incrementAndGet() + "@" + host;

        sesiones.put(idSesion, nombre + " (" + host + ")");

        registrar(host, "conectar", "cliente=" + nombre,
                "sesion=" + idSesion + ", activas=" + sesiones.size());
        System.out.println("   >> Clientes conectados ahora: " + sesiones.size());
        return idSesion;
    }

    /** {@inheritDoc} */
    @Override
    public void desconectar(String idSesion) throws RemoteException {
        String host = hostCliente();
        String eliminada = (idSesion == null) ? null : sesiones.remove(idSesion);

        registrar(host, "desconectar", "sesion=" + idSesion,
                (eliminada == null ? "sesion no encontrada" : "cerrada " + eliminada)
                        + ", activas=" + sesiones.size());
        System.out.println("   >> Clientes conectados ahora: " + sesiones.size());
    }

    /** {@inheritDoc} */
    @Override
    public String[] clientesActivos() throws RemoteException {
        List<String> lista = new ArrayList<>();
        sesiones.forEach((id, desc) -> lista.add(id + " -> " + desc));
        return lista.toArray(new String[0]);
    }

    // ==================================================================
    // Operaciones CRUD
    // ==================================================================

    /** {@inheritDoc} */
    @Override
    public Contacto agregarContacto(Contacto contacto) throws ValidacionException, RemoteException {
        String host = hostCliente();
        String params = "contacto=" + (contacto == null ? "null" : contacto.getNombre()
                + "/" + contacto.getTelefono() + "/" + contacto.getEmail());

        // El servidor es la fuente de verdad: valida aunque el cliente ya lo hizo.
        Contacto limpio;
        try {
            limpio = Validador.validarYNormalizar(contacto);
        } catch (ValidacionException e) {
            registrarError(host, "agregarContacto", params, e.getMessage());
            throw e;
        }

        // Escritura: candado exclusivo.
        candado.writeLock().lock();
        try {
            Contacto guardado = dao.insertar(limpio);
            registrar(host, "agregarContacto", params, "id=" + guardado.getId());
            return guardado;
        } catch (ValidacionException e) {
            registrarError(host, "agregarContacto", params, e.getMessage());
            throw e;
        } catch (SQLException e) {
            registrarError(host, "agregarContacto", params, "SQL: " + e.getMessage());
            throw new RemoteException("Error al guardar el contacto en la base de datos: "
                    + e.getMessage(), e);
        } finally {
            candado.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Contacto buscarPorId(int id) throws RemoteException {
        String host = hostCliente();

        // Lectura: candado compartido, varios clientes a la vez.
        candado.readLock().lock();
        try {
            Contacto c = dao.buscarPorId(id);
            registrar(host, "buscarPorId", "id=" + id,
                    c == null ? "sin resultados" : "encontrado " + c.getNombre());
            return c;
        } catch (SQLException e) {
            registrarError(host, "buscarPorId", "id=" + id, "SQL: " + e.getMessage());
            throw new RemoteException("Error al consultar la base de datos: " + e.getMessage(), e);
        } finally {
            candado.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Contacto> buscarPorNombre(String nombre) throws RemoteException {
        String host = hostCliente();

        candado.readLock().lock();
        try {
            List<Contacto> lista = dao.buscarPorNombre(nombre);
            registrar(host, "buscarPorNombre", "nombre=" + nombre,
                    lista.size() + " coincidencia(s)");
            return lista;
        } catch (SQLException e) {
            registrarError(host, "buscarPorNombre", "nombre=" + nombre, "SQL: " + e.getMessage());
            throw new RemoteException("Error al consultar la base de datos: " + e.getMessage(), e);
        } finally {
            candado.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Contacto> listarContactos() throws RemoteException {
        String host = hostCliente();

        candado.readLock().lock();
        try {
            List<Contacto> lista = dao.listar();
            registrar(host, "listarContactos", "-", lista.size() + " contacto(s)");
            return lista;
        } catch (SQLException e) {
            registrarError(host, "listarContactos", "-", "SQL: " + e.getMessage());
            throw new RemoteException("Error al consultar la base de datos: " + e.getMessage(), e);
        } finally {
            candado.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean actualizarContacto(Contacto contacto) throws ValidacionException, RemoteException {
        String host = hostCliente();
        String params = "id=" + (contacto == null ? "null" : contacto.getId());

        if (contacto == null || contacto.getId() <= 0) {
            ValidacionException e = new ValidacionException(
                    "Para actualizar se necesita el id del contacto.");
            registrarError(host, "actualizarContacto", params, e.getMessage());
            throw e;
        }

        Contacto limpio;
        try {
            limpio = Validador.validarYNormalizar(contacto);
        } catch (ValidacionException e) {
            registrarError(host, "actualizarContacto", params, e.getMessage());
            throw e;
        }

        candado.writeLock().lock();
        try {
            boolean actualizado = dao.actualizar(limpio);
            registrar(host, "actualizarContacto",
                    params + ", nombre=" + limpio.getNombre(),
                    actualizado ? "actualizado" : "el id no existe");
            return actualizado;
        } catch (ValidacionException e) {
            registrarError(host, "actualizarContacto", params, e.getMessage());
            throw e;
        } catch (SQLException e) {
            registrarError(host, "actualizarContacto", params, "SQL: " + e.getMessage());
            throw new RemoteException("Error al actualizar el contacto: " + e.getMessage(), e);
        } finally {
            candado.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean eliminarContacto(int id) throws RemoteException {
        String host = hostCliente();

        candado.writeLock().lock();
        try {
            boolean eliminado = dao.eliminar(id);
            registrar(host, "eliminarContacto", "id=" + id,
                    eliminado ? "eliminado" : "el id no existe");
            return eliminado;
        } catch (SQLException e) {
            registrarError(host, "eliminarContacto", "id=" + id, "SQL: " + e.getMessage());
            throw new RemoteException("Error al eliminar el contacto: " + e.getMessage(), e);
        } finally {
            candado.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int totalContactos() throws RemoteException {
        String host = hostCliente();

        candado.readLock().lock();
        try {
            int total = dao.contar();
            registrar(host, "totalContactos", "-", "total=" + total);
            return total;
        } catch (SQLException e) {
            registrarError(host, "totalContactos", "-", "SQL: " + e.getMessage());
            throw new RemoteException("Error al contar los contactos: " + e.getMessage(), e);
        } finally {
            candado.readLock().unlock();
        }
    }

    // ==================================================================
    // Apoyo interno
    // ==================================================================

    /**
     * Obtiene el host desde el que llega la llamada remota que se esta
     * atendiendo en este hilo.
     *
     * @return la direccion del cliente, o {@code "local"} si la llamada no
     *         proviene de RMI
     */
    private String hostCliente() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "local";
        }
    }

    /**
     * Imprime la operacion en la consola del servidor y la escribe en el
     * archivo de auditoria como exitosa.
     *
     * @param host       host del cliente
     * @param operacion  metodo remoto invocado
     * @param parametros parametros relevantes
     * @param resultado  resumen del resultado
     */
    private void registrar(String host, String operacion, String parametros, String resultado) {
        int n = operaciones.incrementAndGet();
        System.out.printf("[%s] op#%d | cliente=%s | hilo=%s | %s(%s) -> %s%n",
                LocalDateTime.now().format(HORA), n, host,
                Thread.currentThread().getName(), operacion, parametros, resultado);
        AuditoriaLogger.exito(host, operacion, parametros, resultado);
    }

    /**
     * Imprime la operacion fallida en la consola del servidor y la escribe en
     * el archivo de auditoria como error.
     *
     * @param host       host del cliente
     * @param operacion  metodo remoto invocado
     * @param parametros parametros relevantes
     * @param motivo     causa del fallo
     */
    private void registrarError(String host, String operacion, String parametros, String motivo) {
        int n = operaciones.incrementAndGet();
        System.out.printf("[%s] op#%d | cliente=%s | hilo=%s | %s(%s) -> ERROR: %s%n",
                LocalDateTime.now().format(HORA), n, host,
                Thread.currentThread().getName(), operacion, parametros, motivo);
        AuditoriaLogger.error(host, operacion, parametros, motivo);
    }

    /**
     * Numero de operaciones remotas atendidas desde que arranco el servidor.
     *
     * @return el contador de operaciones
     */
    public int getOperacionesAtendidas() {
        return operaciones.get();
    }

    /**
     * Numero de sesiones de cliente abiertas en este momento.
     *
     * @return el total de sesiones activas
     */
    public int getSesionesActivas() {
        return sesiones.size();
    }
}

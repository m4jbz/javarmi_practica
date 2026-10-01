package mx.edu.itigualapa.agenda.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Contrato remoto de la agenda personal.
 *
 * <p>Es la unica pieza que cliente y servidor comparten en tiempo de
 * ejecucion. Al extender {@link Remote} y declarar {@link RemoteException} en
 * todos sus metodos, RMI puede generar dinamicamente el <em>stub</em> (el
 * representante del objeto remoto que el cliente invoca como si fuera local) y
 * enrutar cada llamada al objeto real que vive en el servidor.</p>
 *
 * <p>El servicio se publica en el registro RMI con el nombre
 * {@link #NOMBRE_SERVICIO}.</p>
 *
 * <p>Los errores de validacion no se devuelven como codigos ni como valores
 * nulos: se lanzan como {@link ValidacionException}, con un mensaje listo para
 * mostrarse al usuario.</p>
 */
public interface AgendaRemota extends Remote {

    /** Nombre con el que el servicio queda ligado en el registro RMI. */
    String NOMBRE_SERVICIO = "AgendaPersonal";

    /** Puerto por defecto del registro RMI. */
    int PUERTO_REGISTRO = 1099;

    // ------------------------------------------------------------------
    // Gestion de sesiones (permite ver los clientes activos en el servidor)
    // ------------------------------------------------------------------

    /**
     * Registra un cliente en el servidor y le entrega un identificador de sesion.
     *
     * @param nombreCliente nombre con el que el cliente quiere identificarse
     * @return el identificador de sesion asignado
     * @throws RemoteException si falla la comunicacion remota
     */
    String conectar(String nombreCliente) throws RemoteException;

    /**
     * Da de baja la sesion indicada.
     *
     * @param idSesion identificador devuelto por {@link #conectar(String)}
     * @throws RemoteException si falla la comunicacion remota
     */
    void desconectar(String idSesion) throws RemoteException;

    /**
     * Lista las sesiones activas en el servidor.
     *
     * @return un arreglo con la descripcion de cada cliente conectado
     * @throws RemoteException si falla la comunicacion remota
     */
    String[] clientesActivos() throws RemoteException;

    // ------------------------------------------------------------------
    // Operaciones CRUD sobre la tabla Contactos
    // ------------------------------------------------------------------

    /**
     * Da de alta un contacto nuevo.
     *
     * @param contacto contacto a guardar (su id se ignora)
     * @return el mismo contacto con la clave primaria que le asigno la base de datos
     * @throws ValidacionException si algun campo es invalido o el correo ya existe
     * @throws RemoteException     si falla la comunicacion remota o el acceso a la base de datos
     */
    Contacto agregarContacto(Contacto contacto) throws ValidacionException, RemoteException;

    /**
     * Busca un contacto por su clave primaria.
     *
     * @param id clave primaria a buscar
     * @return el contacto encontrado, o {@code null} si no existe
     * @throws RemoteException si falla la comunicacion remota o el acceso a la base de datos
     */
    Contacto buscarPorId(int id) throws RemoteException;

    /**
     * Busca contactos cuyo nombre contenga el texto indicado (coincidencia parcial).
     *
     * @param nombre fragmento del nombre a buscar
     * @return la lista de coincidencias, ordenada por nombre; vacia si no hay ninguna
     * @throws RemoteException si falla la comunicacion remota o el acceso a la base de datos
     */
    List<Contacto> buscarPorNombre(String nombre) throws RemoteException;

    /**
     * Devuelve todos los contactos de la agenda.
     *
     * @return la lista completa de contactos, ordenada por nombre
     * @throws RemoteException si falla la comunicacion remota o el acceso a la base de datos
     */
    List<Contacto> listarContactos() throws RemoteException;

    /**
     * Modifica los datos de un contacto existente.
     *
     * @param contacto contacto con el id a modificar y los nuevos valores
     * @return {@code true} si se actualizo algun renglon; {@code false} si el id no existe
     * @throws ValidacionException si algun campo es invalido o el correo pertenece a otro contacto
     * @throws RemoteException     si falla la comunicacion remota o el acceso a la base de datos
     */
    boolean actualizarContacto(Contacto contacto) throws ValidacionException, RemoteException;

    /**
     * Borra un contacto por su clave primaria.
     *
     * @param id clave primaria del contacto a borrar
     * @return {@code true} si se borro algun renglon; {@code false} si el id no existe
     * @throws RemoteException si falla la comunicacion remota o el acceso a la base de datos
     */
    boolean eliminarContacto(int id) throws RemoteException;

    /**
     * Cuenta los contactos almacenados. Lo usa la prueba de concurrencia para
     * comparar el resultado remoto contra una consulta directa a la base de datos.
     *
     * @return el numero total de contactos
     * @throws RemoteException si falla la comunicacion remota o el acceso a la base de datos
     */
    int totalContactos() throws RemoteException;
}

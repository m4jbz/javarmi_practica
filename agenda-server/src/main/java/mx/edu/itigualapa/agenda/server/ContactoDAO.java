package mx.edu.itigualapa.agenda.server;

import mx.edu.itigualapa.agenda.common.Contacto;
import mx.edu.itigualapa.agenda.common.ValidacionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de acceso a datos (DAO) de la tabla {@code Contactos}.
 *
 * <p>Concentra todo el SQL del sistema. Reglas que se respetan sin excepcion:</p>
 * <ul>
 *   <li>Todas las sentencias se ejecutan con {@link PreparedStatement} y
 *       parametros; nunca se concatena la entrada del usuario dentro del SQL.
 *       Asi se evita la inyeccion de SQL y se aprovecha el plan precompilado.</li>
 *   <li>Cada metodo abre su propia conexion dentro de un
 *       {@code try-with-resources} y la cierra al terminar, de modo que dos
 *       hilos de RMI nunca comparten una conexion.</li>
 * </ul>
 */
public class ContactoDAO {

    /** Alta de un contacto. */
    private static final String SQL_INSERTAR =
            "INSERT INTO Contactos (nombre, telefono, email) VALUES (?, ?, ?)";

    /** Consulta por clave primaria. */
    private static final String SQL_POR_ID =
            "SELECT id, nombre, telefono, email, fecha_creacion, fecha_actualizacion "
            + "FROM Contactos WHERE id = ?";

    /** Consulta por coincidencia parcial del nombre. */
    private static final String SQL_POR_NOMBRE =
            "SELECT id, nombre, telefono, email, fecha_creacion, fecha_actualizacion "
            + "FROM Contactos WHERE nombre LIKE ? ORDER BY nombre";

    /** Consulta de todos los contactos. */
    private static final String SQL_LISTAR =
            "SELECT id, nombre, telefono, email, fecha_creacion, fecha_actualizacion "
            + "FROM Contactos ORDER BY nombre";

    /**
     * Modificacion de un contacto existente.
     *
     * <p>La fecha de actualizacion se envia como parametro desde Java en lugar
     * de usar {@code CURRENT_TIMESTAMP}: en SQLite esa funcion devuelve la hora
     * UTC, mientras que {@code fecha_creacion} se guarda en hora local; pasarla
     * desde la aplicacion mantiene las dos columnas en la misma zona horaria.</p>
     */
    private static final String SQL_ACTUALIZAR =
            "UPDATE Contactos SET nombre = ?, telefono = ?, email = ?, "
            + "fecha_actualizacion = ? WHERE id = ?";

    /** Formato de fecha y hora que se escribe en la columna fecha_actualizacion. */
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Baja de un contacto. */
    private static final String SQL_ELIMINAR =
            "DELETE FROM Contactos WHERE id = ?";

    /** Conteo total de contactos. */
    private static final String SQL_CONTAR =
            "SELECT COUNT(*) FROM Contactos";

    /** Busqueda de un correo repetido, excluyendo un id concreto. */
    private static final String SQL_EMAIL_DUPLICADO =
            "SELECT id FROM Contactos WHERE email = ? AND id <> ?";

    /**
     * Inserta un contacto y devuelve el objeto con la clave primaria asignada
     * por la base de datos.
     *
     * @param c contacto ya validado
     * @return el contacto con su id, su fecha de creacion y su fecha de actualizacion
     * @throws ValidacionException si el correo ya pertenece a otro contacto
     * @throws SQLException        si falla el acceso a la base de datos
     */
    public Contacto insertar(Contacto c) throws ValidacionException, SQLException {
        verificarEmailUnico(c.getEmail(), 0);

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getEmail());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    c.setId(llaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            // Segunda linea de defensa: si dos hilos insertan el mismo correo a
            // la vez, la restriccion UNIQUE de la tabla es la que decide.
            if (esViolacionDeUnicidad(e)) {
                throw new ValidacionException("email",
                        "El correo electronico '" + c.getEmail() + "' ya esta registrado en la agenda.");
            }
            throw e;
        }

        // Se relee el registro para devolver las fechas que genero la base de datos.
        Contacto guardado = buscarPorId(c.getId());
        return guardado != null ? guardado : c;
    }

    /**
     * Recupera un contacto por su clave primaria.
     *
     * @param id clave primaria a buscar
     * @return el contacto, o {@code null} si no existe
     * @throws SQLException si falla el acceso a la base de datos
     */
    public Contacto buscarPorId(int id) throws SQLException {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Busca contactos cuyo nombre contenga el texto indicado.
     *
     * @param fragmento texto a buscar dentro del nombre
     * @return la lista de coincidencias; vacia si no hay ninguna
     * @throws SQLException si falla el acceso a la base de datos
     */
    public List<Contacto> buscarPorNombre(String fragmento) throws SQLException {
        List<Contacto> lista = new ArrayList<>();
        String patron = "%" + (fragmento == null ? "" : fragmento.trim()) + "%";

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_NOMBRE)) {

            ps.setString(1, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Devuelve todos los contactos ordenados por nombre.
     *
     * @return la lista completa de contactos
     * @throws SQLException si falla el acceso a la base de datos
     */
    public List<Contacto> listar() throws SQLException {
        List<Contacto> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Modifica nombre, telefono y correo de un contacto existente.
     *
     * @param c contacto con el id a modificar y los nuevos valores
     * @return {@code true} si se modifico algun renglon
     * @throws ValidacionException si el correo ya pertenece a otro contacto
     * @throws SQLException        si falla el acceso a la base de datos
     */
    public boolean actualizar(Contacto c) throws ValidacionException, SQLException {
        verificarEmailUnico(c.getEmail(), c.getId());

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getEmail());
            ps.setString(4, LocalDateTime.now().format(FORMATO_FECHA));
            ps.setInt(5, c.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            if (esViolacionDeUnicidad(e)) {
                throw new ValidacionException("email",
                        "El correo electronico '" + c.getEmail() + "' ya esta registrado en la agenda.");
            }
            throw e;
        }
    }

    /**
     * Borra el contacto indicado.
     *
     * @param id clave primaria del contacto a borrar
     * @return {@code true} si se borro algun renglon
     * @throws SQLException si falla el acceso a la base de datos
     */
    public boolean eliminar(int id) throws SQLException {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ELIMINAR)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Cuenta los contactos almacenados.
     *
     * @return el numero total de renglones de la tabla
     * @throws SQLException si falla el acceso a la base de datos
     */
    public int contar() throws SQLException {
        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_CONTAR);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Comprueba, antes de escribir, que el correo no este usado por otro contacto.
     * Permite dar un mensaje claro al usuario en lugar de dejar que falle la
     * restriccion UNIQUE de la tabla.
     *
     * @param email      correo a comprobar
     * @param idExcluido id que no se toma en cuenta (el propio contacto al actualizar)
     * @throws ValidacionException si el correo ya esta registrado
     * @throws SQLException        si falla el acceso a la base de datos
     */
    private void verificarEmailUnico(String email, int idExcluido)
            throws ValidacionException, SQLException {

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_EMAIL_DUPLICADO)) {

            ps.setString(1, email);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new ValidacionException("email",
                            "El correo electronico '" + email
                            + "' ya esta registrado en la agenda (contacto id " + rs.getInt("id") + ").");
                }
            }
        }
    }

    /**
     * Determina si una excepcion de SQL corresponde a la violacion de una
     * restriccion de unicidad.
     *
     * @param e excepcion a examinar
     * @return {@code true} si el error es un correo duplicado
     */
    private boolean esViolacionDeUnicidad(SQLException e) {
        String estado = e.getSQLState();
        String mensaje = e.getMessage() == null ? "" : e.getMessage().toUpperCase();
        return e.getErrorCode() == 19                       // SQLITE_CONSTRAINT
                || "23000".equals(estado)                   // SQLSTATE de integridad
                || mensaje.contains("UNIQUE CONSTRAINT");
    }

    /**
     * Convierte el renglon actual del resultado en un objeto {@link Contacto}.
     *
     * @param rs resultado posicionado en un renglon valido
     * @return el contacto correspondiente
     * @throws SQLException si no se puede leer alguna columna
     */
    private Contacto mapear(ResultSet rs) throws SQLException {
        Contacto c = new Contacto();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setFechaCreacion(rs.getString("fecha_creacion"));
        c.setFechaActualizacion(rs.getString("fecha_actualizacion"));
        return c;
    }
}

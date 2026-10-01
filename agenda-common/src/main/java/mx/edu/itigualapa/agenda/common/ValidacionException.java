package mx.edu.itigualapa.agenda.common;

/**
 * Excepcion de negocio que indica que los datos de un contacto no cumplen las
 * reglas de validacion (nombre, telefono o correo electronico), o que el correo
 * ya existe en la base de datos.
 *
 * <p>Es una excepcion <em>revisada</em> (extiende {@link Exception}) y por lo
 * tanto serializable, de modo que RMI puede transportarla desde el servidor
 * hasta el cliente y este la recibe como una excepcion normal de Java. El
 * mensaje siempre esta redactado para mostrarse directamente al usuario.</p>
 */
public class ValidacionException extends Exception {

    /** Identificador de version de serializacion. */
    private static final long serialVersionUID = 1L;

    /** Nombre del campo que no paso la validacion, o {@code null} si es general. */
    private final String campo;

    /**
     * Crea la excepcion con un mensaje general.
     *
     * @param mensaje descripcion del problema, apta para el usuario final
     */
    public ValidacionException(String mensaje) {
        super(mensaje);
        this.campo = null;
    }

    /**
     * Crea la excepcion indicando el campo que fallo.
     *
     * @param campo   nombre del campo invalido (nombre, telefono o email)
     * @param mensaje descripcion del problema, apta para el usuario final
     */
    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    /**
     * Devuelve el campo que provoco el error de validacion.
     *
     * @return el nombre del campo invalido, o {@code null} si el error es general
     */
    public String getCampo() {
        return campo;
    }
}

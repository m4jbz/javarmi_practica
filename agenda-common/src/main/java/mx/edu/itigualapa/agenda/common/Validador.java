package mx.edu.itigualapa.agenda.common;

import java.util.regex.Pattern;

/**
 * Reglas de validacion de los datos de un contacto.
 *
 * <p>Esta clase vive en el modulo comun a proposito: el cliente la usa para
 * avisar al usuario de inmediato (experiencia de uso) y el servidor la usa
 * antes de tocar la base de datos. El servidor es la fuente de verdad, porque
 * un cliente manipulado podria omitir su propia validacion.</p>
 *
 * <p>Reglas aplicadas:</p>
 * <ul>
 *   <li><b>nombre</b>: obligatorio, de 2 a 100 caracteres, solo letras
 *       (incluidos acentos y la letra n con virgulilla) y espacios.</li>
 *   <li><b>telefono</b>: exactamente 10 digitos.</li>
 *   <li><b>email</b>: formato valido; la unicidad la comprueba la base de
 *       datos mediante la restriccion UNIQUE.</li>
 * </ul>
 */
public final class Validador {

    /** Longitud minima permitida para el nombre. */
    public static final int NOMBRE_MIN = 2;

    /** Longitud maxima permitida para el nombre. */
    public static final int NOMBRE_MAX = 100;

    /** Cantidad exacta de digitos que debe tener el telefono. */
    public static final int TELEFONO_DIGITOS = 10;

    /** Longitud maxima admitida para el correo electronico. */
    public static final int EMAIL_MAX = 150;

    /**
     * Solo letras del alfabeto espanol (con acentos y dieresis) y espacios.
     * Se usan rangos Unicode explicitos para no depender de la configuracion
     * regional de la maquina.
     */
    private static final Pattern PATRON_NOMBRE =
            Pattern.compile("[a-zA-ZÀ-ÿñÑ ]+");

    /** Exactamente diez digitos, sin espacios ni guiones. */
    private static final Pattern PATRON_TELEFONO =
            Pattern.compile("[0-9]{" + TELEFONO_DIGITOS + "}");

    /**
     * Formato de correo electronico: parte local sin espacios ni arrobas,
     * un dominio con al menos un punto y una extension de 2 a 24 letras.
     */
    private static final Pattern PATRON_EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)*[.][A-Za-z]{2,24}$");

    /** Clase de utilidad: no se instancia. */
    private Validador() {
    }

    /**
     * Valida el nombre de un contacto.
     *
     * @param nombre nombre a revisar
     * @throws ValidacionException si esta vacio, mide menos de 2 o mas de 100
     *                             caracteres, o contiene algo distinto de letras
     *                             y espacios
     */
    public static void validarNombre(String nombre) throws ValidacionException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidacionException("nombre", "El nombre es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() < NOMBRE_MIN || limpio.length() > NOMBRE_MAX) {
            throw new ValidacionException("nombre",
                    "El nombre debe tener entre " + NOMBRE_MIN + " y " + NOMBRE_MAX
                            + " caracteres (recibido: " + limpio.length() + ").");
        }
        if (!PATRON_NOMBRE.matcher(limpio).matches()) {
            throw new ValidacionException("nombre",
                    "El nombre solo admite letras y espacios (sin numeros ni simbolos).");
        }
    }

    /**
     * Valida el telefono de un contacto.
     *
     * @param telefono telefono a revisar
     * @throws ValidacionException si es nulo, vacio o no tiene exactamente
     *                             10 digitos
     */
    public static void validarTelefono(String telefono) throws ValidacionException {
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new ValidacionException("telefono", "El telefono es obligatorio.");
        }
        String limpio = telefono.trim();
        if (!PATRON_TELEFONO.matcher(limpio).matches()) {
            throw new ValidacionException("telefono",
                    "El telefono debe tener exactamente " + TELEFONO_DIGITOS
                            + " digitos (recibido: " + limpio.length() + " caracteres).");
        }
    }

    /**
     * Valida el formato del correo electronico.
     *
     * @param email correo a revisar
     * @throws ValidacionException si es nulo, vacio o no tiene un formato valido
     */
    public static void validarEmail(String email) throws ValidacionException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidacionException("email", "El correo electronico es obligatorio.");
        }
        String limpio = email.trim();
        if (limpio.length() > EMAIL_MAX) {
            throw new ValidacionException("email",
                    "El correo electronico no puede exceder " + EMAIL_MAX + " caracteres.");
        }
        if (!PATRON_EMAIL.matcher(limpio).matches()) {
            throw new ValidacionException("email",
                    "El formato del correo electronico no es valido (ejemplo: nombre@dominio.com).");
        }
    }

    /**
     * Valida los tres campos de un contacto de una sola vez y devuelve el mismo
     * objeto con los valores ya recortados de espacios sobrantes.
     *
     * @param contacto contacto a revisar
     * @return el mismo contacto con nombre, telefono y email normalizados
     * @throws ValidacionException si el contacto es nulo o algun campo es invalido
     */
    public static Contacto validarYNormalizar(Contacto contacto) throws ValidacionException {
        if (contacto == null) {
            throw new ValidacionException("No se recibio ningun contacto.");
        }
        validarNombre(contacto.getNombre());
        validarTelefono(contacto.getTelefono());
        validarEmail(contacto.getEmail());

        contacto.setNombre(contacto.getNombre().trim());
        contacto.setTelefono(contacto.getTelefono().trim());
        contacto.setEmail(contacto.getEmail().trim().toLowerCase());
        return contacto;
    }
}

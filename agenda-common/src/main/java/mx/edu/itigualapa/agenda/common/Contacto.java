package mx.edu.itigualapa.agenda.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representa un contacto de la agenda personal.
 *
 * <p>Esta clase viaja por la red entre el cliente y el servidor RMI, por lo que
 * implementa {@link Serializable}. RMI serializa el objeto en el cliente, lo
 * transmite y lo reconstruye en el servidor (y al contrario para los
 * resultados). El campo {@code serialVersionUID} se fija de forma explicita
 * para que cliente y servidor sigan siendo compatibles aunque se recompile el
 * proyecto.</p>
 *
 * <p>Corresponde a la tabla {@code Contactos} de la base de datos.</p>
 */
public class Contacto implements Serializable {

    /** Identificador de version de serializacion. */
    private static final long serialVersionUID = 1L;

    /** Clave primaria autoincremental. Vale 0 mientras el contacto no se guarda. */
    private int id;

    /** Nombre completo del contacto. */
    private String nombre;

    /** Telefono de exactamente 10 digitos. */
    private String telefono;

    /** Correo electronico, unico en la base de datos. */
    private String email;

    /** Fecha y hora de creacion del registro (la asigna la base de datos). */
    private String fechaCreacion;

    /** Fecha y hora de la ultima modificacion (la asigna la base de datos). */
    private String fechaActualizacion;

    /** Constructor vacio requerido para construir el objeto por pasos. */
    public Contacto() {
    }

    /**
     * Constructor para crear un contacto nuevo, aun sin id.
     *
     * @param nombre   nombre completo
     * @param telefono telefono de 10 digitos
     * @param email    correo electronico
     */
    public Contacto(String nombre, String telefono, String email) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    /**
     * Constructor completo, usado por el DAO al leer un renglon de la tabla.
     *
     * @param id       clave primaria
     * @param nombre   nombre completo
     * @param telefono telefono de 10 digitos
     * @param email    correo electronico
     */
    public Contacto(int id, String nombre, String telefono, String email) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    /** @return la clave primaria del contacto (0 si todavia no esta guardado) */
    public int getId() {
        return id;
    }

    /** @param id clave primaria a asignar */
    public void setId(int id) {
        this.id = id;
    }

    /** @return el nombre completo del contacto */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nombre completo a asignar */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return el telefono del contacto */
    public String getTelefono() {
        return telefono;
    }

    /** @param telefono telefono a asignar */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /** @return el correo electronico del contacto */
    public String getEmail() {
        return email;
    }

    /** @param email correo electronico a asignar */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return la fecha de creacion del registro */
    public String getFechaCreacion() {
        return fechaCreacion;
    }

    /** @param fechaCreacion fecha de creacion a asignar */
    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /** @return la fecha de la ultima actualizacion del registro */
    public String getFechaActualizacion() {
        return fechaActualizacion;
    }

    /** @param fechaActualizacion fecha de actualizacion a asignar */
    public void setFechaActualizacion(String fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    /**
     * Dos contactos son iguales si tienen la misma clave primaria.
     *
     * @param o objeto a comparar
     * @return {@code true} si representan el mismo registro
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Contacto)) {
            return false;
        }
        return id == ((Contacto) o).id;
    }

    /** @return el codigo hash derivado de la clave primaria */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Representacion legible del contacto, util para los registros de auditoria.
     *
     * @return una cadena con los datos del contacto
     */
    @Override
    public String toString() {
        return "Contacto{id=" + id
                + ", nombre='" + nombre + '\''
                + ", telefono='" + telefono + '\''
                + ", email='" + email + '\'' + '}';
    }
}

package mx.edu.itigualapa.agenda.client;

/**
 * Lanzador del cliente grafico.
 *
 * <p>Existe por una razon muy concreta: cuando la clase principal de un jar
 * extiende {@link javafx.application.Application}, el iniciador de Java
 * comprueba que los modulos de JavaFX esten en la ruta de modulos y, si no lo
 * estan, se niega a arrancar con el mensaje
 * <em>"JavaFX runtime components are missing"</em>. Al poner el {@code main} en
 * una clase que NO extiende {@code Application}, esa comprobacion no se
 * dispara y la interfaz puede arrancar con las bibliotecas de JavaFX
 * simplemente en la ruta de clases.</p>
 *
 * <p>Uso:</p>
 * <pre>
 *   java -cp agenda-client.jar mx.edu.itigualapa.agenda.client.Launcher [host] [puerto]
 * </pre>
 */
public final class Launcher {

    /** Clase con solo el metodo principal: no se instancia. */
    private Launcher() {
    }

    /**
     * Arranca la aplicacion JavaFX.
     *
     * @param args {@code [0]} host del servidor, {@code [1]} puerto del registro
     */
    public static void main(String[] args) {
        ClienteFX.main(args);
    }
}

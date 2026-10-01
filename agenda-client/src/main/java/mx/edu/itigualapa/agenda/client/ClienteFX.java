package mx.edu.itigualapa.agenda.client;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import mx.edu.itigualapa.agenda.common.AgendaRemota;
import mx.edu.itigualapa.agenda.common.Contacto;
import mx.edu.itigualapa.agenda.common.ValidacionException;
import mx.edu.itigualapa.agenda.common.Validador;

import java.io.InputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cliente grafico de la agenda, construido con JavaFX y con tema oscuro.
 *
 * <p>La ventana se organiza en tres zonas:</p>
 * <ul>
 *   <li>Un buscador por nombre (coincidencia parcial) o por id (clave
 *       primaria exacta).</li>
 *   <li>Un panel desplegable, oculto por defecto, para dar de alta contactos:
 *       se abre y se cierra con el boton <em>Agregar</em>.</li>
 *   <li>La tabla de contactos y, a su derecha, una tarjeta de detalle donde
 *       se edita o elimina el contacto seleccionado.</li>
 * </ul>
 *
 * <p>Los avisos y confirmaciones se muestran dentro de la misma ventana, sobre
 * un velo oscuro, en lugar de usar los dialogos nativos.</p>
 *
 * <p>Ninguna llamada remota se hace en el hilo de la interfaz. Todas se
 * envuelven en un {@link Task} que corre en un hilo aparte y devuelve el
 * resultado a la interfaz mediante {@link Platform#runLater}. Asi la ventana
 * nunca se congela mientras el servidor responde, que es justo el riesgo de
 * mezclar RMI con una interfaz grafica.</p>
 *
 * <p>Parametros (via {@code --host=...} y {@code --puerto=...} o por
 * posicion): host y puerto del registro RMI. Por defecto
 * {@code localhost:1099}.</p>
 */
public class ClienteFX extends Application {

    /** Hoja de estilos del tema oscuro. */
    private static final String TEMA =
            ClienteFX.class.getResource("tema-oscuro.css").toExternalForm();

    /** Pesos de Inter incluidos en el jar (licencia en fuentes/Inter-LICENSE.txt). */
    private static final List<String> FUENTES_INCLUIDAS =
            List.of("Inter-Regular.ttf", "Inter-Medium.ttf");

    /** Colores pastel para los avatares; se elige uno segun el id. */
    private static final List<Color> PASTELES = List.of(
            Color.web("#c9caee"), Color.web("#c3e9de"), Color.web("#f8c2d9"),
            Color.web("#c2dbe9"), Color.web("#cfdddb"), Color.web("#e4cdec"));

    /** Duracion de la animacion del panel de alta. */
    private static final Duration ANIMACION = Duration.millis(220);

    // Iconos de trazo en una rejilla de 24x24 (trazos del conjunto Lucide, licencia ISC).

    /** Icono de lupa. */
    private static final String ICONO_LUPA = "M19 11a8 8 0 1 1-16 0a8 8 0 1 1 16 0zM21 21l-4.35-4.35";

    /** Icono de mas. */
    private static final String ICONO_MAS = "M5 12h14M12 5v14";

    /** Icono de bote de basura. */
    private static final String ICONO_BORRAR =
            "M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M10 11v6M14 11v6";

    /** Icono de personas. */
    private static final String ICONO_PERSONAS =
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M13 7a4 4 0 1 1-8 0a4 4 0 1 1 8 0z"
            + "M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75";

    /** Icono de advertencia (signo de admiracion). */
    private static final String ICONO_AVISO = "M12 7v6M12 17h.01";

    /** Stub del servicio remoto. */
    private AgendaRemota agenda;

    /** Identificador de sesion entregado por el servidor. */
    private String idSesion;

    /** Host del servidor. */
    private String host = "localhost";

    /** Puerto del registro RMI. */
    private int puerto = AgendaRemota.PUERTO_REGISTRO;

    /** Datos que alimentan la tabla. */
    private final ObservableList<Contacto> datos = FXCollections.observableArrayList();

    /** Tabla de contactos. */
    private final TableView<Contacto> tabla = new TableView<>(datos);

    /** Capa de avisos que se muestra sobre la ventana. */
    private final StackPane velo = new StackPane();

    // ---------------------------------------------------------- busqueda

    /** Campo del texto a buscar. */
    private final TextField txtBuscar = new TextField();

    /** Opcion "buscar por nombre". */
    private final ToggleButton optNombre = new ToggleButton("Nombre");

    /** Opcion "buscar por id". */
    private final ToggleButton optId = new ToggleButton("ID");

    // ---------------------------------------------------------- alta

    /** Panel desplegable del alta. */
    private final VBox panelAlta = new VBox();

    /** Boton que abre y cierra el panel de alta. */
    private final Button btnAgregar = new Button("Agregar");

    /** Si el panel de alta esta abierto. */
    private boolean altaAbierta;

    /** Campos del formulario de alta. */
    private final CamposContacto alta = new CamposContacto();

    // ---------------------------------------------------------- detalle

    /** Contenedor de la tarjeta de detalle (vacia o con el contacto). */
    private final StackPane tarjetaDetalle = new StackPane();

    /** Vista con el contacto seleccionado. */
    private Node vistaDetalle;

    /** Vista que se muestra cuando no hay seleccion. */
    private Node vistaVacia;

    /** Id del contacto seleccionado (no se muestra; el subtitulo ya lo indica). */
    private final TextField txtId = new TextField();

    /** Circulo del avatar grande. */
    private final Circle avatarDetalle = new Circle(26);

    /** Iniciales del avatar grande. */
    private final Label lblIniciales = new Label();

    /** Nombre del contacto seleccionado, como encabezado. */
    private final Label lblNombreDetalle = new Label();

    /** Linea con id y fechas del contacto seleccionado. */
    private final Label lblSubDetalle = new Label();

    /** Campos del formulario de edicion. */
    private final CamposContacto edicion = new CamposContacto();

    // ---------------------------------------------------------- estado

    /** Texto del indicador de conexion. */
    private final Label lblConexion = new Label("Conectando...");

    /** Punto de color del indicador de conexion. */
    private final Circle puntoConexion = new Circle(4);

    /** Barra de estado inferior. */
    private final Label estado = new Label("Iniciando...");

    /** Cuantos contactos muestra la tabla. */
    private final Label lblTotal = new Label();

    /**
     * Construye y muestra la ventana principal.
     *
     * @param escenario escenario que entrega JavaFX
     */
    @Override
    public void start(Stage escenario) {
        leerParametros();
        cargarFuentes();

        Scene escena = new Scene(construirRaiz(), 1200, 760);
        escena.getStylesheets().add(TEMA);

        escenario.setTitle("Agenda Personal RMI (" + host + ":" + puerto + ")");
        escenario.setMinWidth(900);
        escenario.setMinHeight(600);
        escenario.setScene(escena);
        escenario.setOnCloseRequest(e -> cerrarSesion());
        escenario.show();

        txtBuscar.requestFocus();
        conectarAlServidor();
    }

    /** Lee host y puerto de los parametros de la aplicacion. */
    private void leerParametros() {
        Parameters p = getParameters();

        String h = p.getNamed().get("host");
        String pt = p.getNamed().get("puerto");

        List<String> sueltos = p.getUnnamed();
        if (h == null && !sueltos.isEmpty()) {
            h = sueltos.get(0);
        }
        if (pt == null && sueltos.size() > 1) {
            pt = sueltos.get(1);
        }

        if (h != null && !h.isBlank()) {
            host = h.trim();
        }
        if (pt != null && !pt.isBlank()) {
            try {
                puerto = Integer.parseInt(pt.trim());
            } catch (NumberFormatException ignorada) {
                // se conserva el puerto por defecto
            }
        }
    }

    /**
     * Registra los pesos de Inter incluidos en el jar. Si alguno falta, la
     * hoja de estilos recurre a la fuente del sistema.
     */
    private static void cargarFuentes() {
        for (String archivo : FUENTES_INCLUIDAS) {
            try (InputStream in = ClienteFX.class.getResourceAsStream("fuentes/" + archivo)) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (Exception ignorada) {
                // sin la fuente, JavaFX usa la del sistema
            }
        }
    }

    // ==================================================================
    // Construccion de la interfaz
    // ==================================================================

    /**
     * Arma el arbol de componentes de la ventana.
     *
     * @return el panel raiz
     */
    private StackPane construirRaiz() {
        HBox cuerpo = new HBox(16, construirTabla(), construirDetalle());
        HBox.setHgrow(tabla, Priority.ALWAYS);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        estado.getStyleClass().add("barra-estado");
        lblTotal.getStyleClass().add("barra-estado");
        HBox pie = new HBox(estado, espaciador(), lblTotal);

        VBox principal = new VBox(18,
                construirTitulo(),
                construirBarraHerramientas(),
                construirPanelAlta(),
                cuerpo,
                pie);
        principal.setStyle("-fx-padding: 24 28 14 28;");

        velo.getStyleClass().add("velo");
        velo.setVisible(false);

        return new StackPane(principal, velo);
    }

    /**
     * Fila con el titulo de la ventana y el indicador de conexion.
     *
     * @return el contenedor de la fila
     */
    private HBox construirTitulo() {
        Label titulo = new Label("Contactos");
        titulo.getStyleClass().add("titulo-pagina");

        puntoConexion.getStyleClass().add("punto");
        lblConexion.setGraphic(puntoConexion);
        lblConexion.setGraphicTextGap(8);
        lblConexion.getStyleClass().add("chip-conexion");

        HBox fila = new HBox(10, titulo, espaciador(), lblConexion);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /**
     * Barra de herramientas: buscador, selector nombre/id, "Mostrar todos" y
     * el boton que despliega el alta.
     *
     * @return el contenedor de la barra
     */
    private HBox construirBarraHerramientas() {
        ToggleGroup modo = new ToggleGroup();
        optNombre.setToggleGroup(modo);
        optId.setToggleGroup(modo);
        optNombre.setSelected(true);
        // Siempre debe quedar una opcion elegida.
        modo.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                antes.setSelected(true);
            } else {
                ajustarModoBusqueda();
            }
        });
        HBox segmento = new HBox(optNombre, optId);
        segmento.getStyleClass().add("segmento");
        segmento.setAlignment(Pos.CENTER_LEFT);

        // En modo id el campo solo acepta digitos.
        txtBuscar.setTextFormatter(new TextFormatter<String>(cambio ->
                optId.isSelected() && !cambio.getControlNewText().matches("\\d{0,9}") ? null : cambio));
        txtBuscar.setOnAction(e -> buscar());
        HBox.setHgrow(txtBuscar, Priority.ALWAYS);

        HBox buscador = new HBox(icono(ICONO_LUPA, 16, "icono", "tenue"), txtBuscar);
        buscador.setAlignment(Pos.CENTER_LEFT);
        buscador.getStyleClass().add("buscador");

        Button btnBuscar = new Button("Buscar");
        btnBuscar.getStyleClass().add("boton-gris");
        btnBuscar.setOnAction(e -> buscar());

        ajustarModoBusqueda();

        Button btnTodos = new Button("Mostrar todos");
        btnTodos.getStyleClass().add("boton-gris");
        btnTodos.setOnAction(e -> refrescar());

        btnAgregar.getStyleClass().add("boton-blanco");
        btnAgregar.setGraphic(icono(ICONO_MAS, 14, "icono"));
        btnAgregar.setGraphicTextGap(8);
        btnAgregar.setMinWidth(Region.USE_PREF_SIZE);
        btnAgregar.setOnAction(e -> alternarAlta());

        HBox barra = new HBox(10, buscador, btnBuscar, segmento, espaciador(), btnTodos, btnAgregar);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    /** Actualiza el texto de ayuda del buscador segun el modo elegido. */
    private void ajustarModoBusqueda() {
        if (optId.isSelected()) {
            // Al pasar a id se descarta lo que no sea numero.
            String digitos = txtBuscar.getText().replaceAll("\\D", "");
            txtBuscar.setText(digitos.substring(0, Math.min(9, digitos.length())));
            txtBuscar.setPromptText("Buscar por ID, ej. 12");
        } else {
            txtBuscar.setPromptText("Buscar por nombre");
        }
        txtBuscar.requestFocus();
    }

    /**
     * Panel desplegable con el formulario de alta. Empieza cerrado.
     *
     * @return el panel
     */
    private VBox construirPanelAlta() {
        Label titulo = new Label("Nuevo contacto");
        titulo.getStyleClass().add("tarjeta-titulo");
        Label ayuda = new Label("Todos los campos son obligatorios. El ID se asigna solo.");
        ayuda.getStyleClass().addAll("texto2", "mini");

        alta.nombre.setOnAction(e -> alta.telefono.requestFocus());
        alta.telefono.setOnAction(e -> alta.email.requestFocus());
        alta.email.setOnAction(e -> agregar());

        GridPane rejilla = new GridPane();
        rejilla.setHgap(12);
        rejilla.setVgap(6);
        agregarColumna(rejilla, 0, "Nombre completo", alta.nombre, alta.errNombre, 2);
        agregarColumna(rejilla, 1, "Teléfono", alta.telefono, alta.errTelefono, 1);
        agregarColumna(rejilla, 2, "Correo electrónico", alta.email, alta.errEmail, 2);

        Button btnGuardar = new Button("Guardar contacto");
        btnGuardar.getStyleClass().add("boton-blanco");
        btnGuardar.setOnAction(e -> agregar());

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("boton-contorno");
        btnCancelar.setOnAction(e -> cerrarAlta());

        HBox acciones = new HBox(10, espaciador(), btnCancelar, btnGuardar);

        VBox tarjeta = new VBox(14, new VBox(3, titulo, ayuda), rejilla, acciones);
        tarjeta.getStyleClass().addAll("tarjeta", "franja-menta");

        // El panel envuelve la tarjeta y recorta lo que sobresale mientras se
        // anima su altura.
        panelAlta.getChildren().add(tarjeta);
        panelAlta.setMinHeight(0);
        panelAlta.setMaxHeight(0);
        panelAlta.setOpacity(0);
        panelAlta.setVisible(false);
        panelAlta.setManaged(false);
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(panelAlta.widthProperty());
        recorte.heightProperty().bind(panelAlta.heightProperty());
        panelAlta.setClip(recorte);
        return panelAlta;
    }

    /**
     * Coloca un campo con su etiqueta y su mensaje de error en una columna de
     * la rejilla.
     *
     * @param rejilla  rejilla destino
     * @param columna  columna a ocupar
     * @param titulo   texto de la etiqueta
     * @param campo    campo de texto
     * @param error    etiqueta del mensaje de error
     * @param peso     ancho relativo de la columna
     */
    private static void agregarColumna(GridPane rejilla, int columna, String titulo,
                                       TextField campo, Label error, int peso) {
        rejilla.add(etiquetaCampo(titulo), columna, 0);
        rejilla.add(campo, columna, 1);
        rejilla.add(error, columna, 2);

        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(peso * 20.0);
        c.setHgrow(Priority.ALWAYS);
        rejilla.getColumnConstraints().add(c);
    }

    /**
     * Tabla de contactos con sus columnas.
     *
     * @return la tabla lista para mostrarse
     */
    private TableView<Contacto> construirTabla() {
        TableColumn<Contacto, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colId.setComparator((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
        colId.setCellFactory(col -> new CeldaTexto("#", true));
        colId.setPrefWidth(60);
        colId.setMinWidth(56);

        TableColumn<Contacto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNombre.setCellFactory(col -> new CeldaNombreConAvatar());
        colNombre.setPrefWidth(260);
        colNombre.setMinWidth(170);

        TableColumn<Contacto, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(c ->
                new SimpleStringProperty(formatearTelefono(c.getValue().getTelefono())));
        colTelefono.setPrefWidth(120);
        colTelefono.setMinWidth(112);

        TableColumn<Contacto, String> colEmail = new TableColumn<>("Correo");
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmail.setPrefWidth(240);
        colEmail.setMinWidth(150);

        // En la tabla basta la fecha; la hora completa aparece en el detalle.
        TableColumn<Contacto, String> colAlta = new TableColumn<>("Creado");
        colAlta.setCellValueFactory(c -> {
            String f = c.getValue().getFechaCreacion();
            return new SimpleStringProperty(f != null && f.length() >= 10 ? f.substring(0, 10) : f);
        });
        colAlta.setCellFactory(col -> new CeldaTexto("", true));
        colAlta.setPrefWidth(96);
        colAlta.setMinWidth(90);

        tabla.getColumns().setAll(List.of(colId, colNombre, colTelefono, colEmail, colAlta));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabla.setPlaceholder(new Label("No hay contactos que mostrar."));
        tabla.setMinHeight(160);

        // Al elegir un renglon, sus datos pasan a la tarjeta de detalle.
        tabla.getSelectionModel().selectedItemProperty()
                .addListener((obs, viejo, nuevo) -> mostrarDetalle(nuevo));
        return tabla;
    }

    /**
     * Tarjeta lateral con el detalle editable del contacto seleccionado.
     *
     * @return el contenedor de la tarjeta
     */
    private Node construirDetalle() {
        // Vista vacia.
        Label vacioTitulo = new Label("Ningún contacto seleccionado");
        vacioTitulo.getStyleClass().add("medio");
        Label vacioAyuda = new Label("Elige un renglón de la tabla para ver sus datos, "
                + "modificarlos o eliminarlo.");
        vacioAyuda.getStyleClass().add("texto2");
        vacioAyuda.setWrapText(true);
        vacioAyuda.setStyle("-fx-text-alignment: center;");
        VBox vacia = new VBox(10, icono(ICONO_PERSONAS, 28, "icono", "tenue"), vacioTitulo, vacioAyuda);
        vacia.setAlignment(Pos.CENTER);
        vistaVacia = vacia;

        // Vista con contacto.
        Label titulo = new Label("Contacto");
        titulo.getStyleClass().add("tarjeta-titulo");
        lblSubDetalle.getStyleClass().addAll("texto2", "mini");

        lblIniciales.getStyleClass().add("chip-letra");
        lblIniciales.setStyle("-fx-font-size: 17px;");
        StackPane avatar = new StackPane(avatarDetalle, lblIniciales);
        avatar.setMinSize(52, 52);

        lblNombreDetalle.getStyleClass().add("medio");
        lblNombreDetalle.setStyle("-fx-font-size: 15px;");
        lblNombreDetalle.setWrapText(true);

        HBox encabezado = new HBox(12, avatar, lblNombreDetalle);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        Region punteado = new Region();
        punteado.getStyleClass().add("separador-punteado");

        edicion.email.setOnAction(e -> actualizar());

        Button btnEliminar = new Button();
        btnEliminar.setGraphic(icono(ICONO_BORRAR, 16, "icono"));
        btnEliminar.getStyleClass().addAll("boton-icono", "peligro");
        btnEliminar.setTooltip(new Tooltip("Eliminar contacto"));
        btnEliminar.setOnAction(e -> eliminar());

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("boton-texto");
        btnCerrar.setOnAction(e -> tabla.getSelectionModel().clearSelection());

        Button btnGuardar = new Button("Guardar cambios");
        btnGuardar.getStyleClass().add("boton-blanco");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnGuardar, Priority.ALWAYS);
        btnGuardar.setOnAction(e -> actualizar());

        HBox acciones = new HBox(8, btnEliminar, btnCerrar, btnGuardar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        // Solo los campos se desplazan si la ventana es baja (o el panel de
        // alta esta abierto); encabezado y botones quedan siempre a la vista.
        VBox campos = new VBox(7,
                etiquetaCampo("Nombre completo"), edicion.nombre, edicion.errNombre,
                etiquetaCampo("Teléfono"), edicion.telefono, edicion.errTelefono,
                etiquetaCampo("Correo electrónico"), edicion.email, edicion.errEmail);
        ScrollPane desplazable = new ScrollPane(campos);
        desplazable.setFitToWidth(true);
        desplazable.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazable.setMinHeight(0);
        VBox.setVgrow(desplazable, Priority.ALWAYS);

        vistaDetalle = new VBox(14,
                new VBox(3, titulo, lblSubDetalle),
                encabezado,
                punteado,
                desplazable,
                acciones);

        tarjetaDetalle.getChildren().setAll(vistaVacia);
        tarjetaDetalle.getStyleClass().add("tarjeta");
        tarjetaDetalle.setPrefWidth(340);
        tarjetaDetalle.setMinWidth(320);
        tarjetaDetalle.setMinHeight(0);
        return tarjetaDetalle;
    }

    // ==================================================================
    // Comportamiento de la interfaz
    // ==================================================================

    /** Abre o cierra el panel de alta. */
    private void alternarAlta() {
        if (altaAbierta) {
            cerrarAlta();
        } else {
            abrirAlta();
        }
    }

    /** Despliega el panel de alta con una animacion. */
    private void abrirAlta() {
        if (altaAbierta) {
            return;
        }
        altaAbierta = true;
        btnAgregar.setText("Ocultar");
        btnAgregar.setGraphic(icono("M5 12h14", 14, "icono"));

        panelAlta.setVisible(true);
        panelAlta.setManaged(true);
        panelAlta.applyCss();
        double alto = panelAlta.getChildren().get(0).prefHeight(-1);
        animar(alto, 1, () -> {
            // Ya abierto, el panel mide lo que pida su contenido (crece si
            // aparecen mensajes de error).
            panelAlta.minHeightProperty().unbind();
            panelAlta.setMinHeight(Region.USE_PREF_SIZE);
            panelAlta.setMaxHeight(Region.USE_PREF_SIZE);
        });
        alta.nombre.requestFocus();
    }

    /** Pliega el panel de alta y limpia su formulario. */
    private void cerrarAlta() {
        if (!altaAbierta) {
            return;
        }
        altaAbierta = false;
        btnAgregar.setText("Agregar");
        btnAgregar.setGraphic(icono(ICONO_MAS, 14, "icono"));

        panelAlta.setMaxHeight(panelAlta.getHeight());
        animar(0, 0, () -> {
            panelAlta.minHeightProperty().unbind();
            panelAlta.setMinHeight(0);
            panelAlta.setVisible(false);
            panelAlta.setManaged(false);
            alta.limpiar();
        });
    }

    /**
     * Anima la altura maxima y la opacidad del panel de alta.
     *
     * @param alto     altura final
     * @param opacidad opacidad final
     * @param alFinal  accion al terminar la animacion
     */
    private void animar(double alto, double opacidad, Runnable alFinal) {
        // Durante la animacion el minimo sigue al maximo: asi el VBox no
        // puede aplastar el panel para darle espacio a la tabla.
        panelAlta.minHeightProperty().bind(panelAlta.maxHeightProperty());
        Timeline t = new Timeline(new KeyFrame(ANIMACION,
                new KeyValue(panelAlta.maxHeightProperty(), alto, Interpolator.EASE_BOTH),
                new KeyValue(panelAlta.opacityProperty(), opacidad, Interpolator.EASE_BOTH)));
        t.setOnFinished(e -> alFinal.run());
        t.play();
    }

    /**
     * Muestra en la tarjeta lateral el contacto indicado, o la vista vacia si
     * es {@code null}.
     *
     * @param c contacto seleccionado
     */
    private void mostrarDetalle(Contacto c) {
        if (c == null) {
            tarjetaDetalle.getChildren().setAll(vistaVacia);
            txtId.clear();
            edicion.limpiar();
            return;
        }
        txtId.setText(String.valueOf(c.getId()));
        edicion.nombre.setText(c.getNombre());
        edicion.telefono.setText(c.getTelefono());
        edicion.email.setText(c.getEmail());
        lblIniciales.setText(iniciales(c.getNombre()));
        avatarDetalle.setFill(pastel(c.getId()));
        lblNombreDetalle.setText(c.getNombre());

        String creado = c.getFechaCreacion() == null ? "-" : c.getFechaCreacion();
        String cambio = c.getFechaActualizacion();
        lblSubDetalle.setText("#" + c.getId() + "  •  creado " + creado
                + (cambio != null && !cambio.equals(creado) ? "\nmodificado " + cambio : ""));
        tarjetaDetalle.getChildren().setAll(vistaDetalle);
    }

    // ==================================================================
    // Conexion y operaciones remotas
    // ==================================================================

    /** Localiza el servicio y abre la sesion, sin bloquear la interfaz. */
    private void conectarAlServidor() {
        estado.setText("Conectando con " + host + ":" + puerto + "...");

        enSegundoPlano(() -> {
            Registry registro = LocateRegistry.getRegistry(host, puerto);
            agenda = (AgendaRemota) registro.lookup(AgendaRemota.NOMBRE_SERVICIO);
            idSesion = agenda.conectar("javafx-" + System.getProperty("user.name", "alumno"));
            return agenda.listarContactos();
        }, lista -> {
            mostrarLista(lista);
            indicarConexion(true, "Conectado a " + host + ":" + puerto);
            estado.setText("Sesión " + idSesion + " iniciada.");
        }, error -> {
            indicarConexion(false, "Sin conexión");
            estado.setText("No se pudo conectar con " + host + ":" + puerto + ".");
            aviso(TipoAviso.ERROR, "No se pudo conectar",
                    "No se encontró el servicio en " + host + ":" + puerto + ".\n\n"
                    + "Arranca primero el servidor (scripts/iniciar-servidor) "
                    + "y vuelve a abrir esta ventana.\n\nDetalle: " + error.getMessage());
        });
    }

    /** Vuelve a pedir la lista completa de contactos. */
    private void refrescar() {
        if (noHayConexion()) {
            return;
        }
        txtBuscar.clear();
        estado.setText("Actualizando lista...");
        enSegundoPlano(() -> agenda.listarContactos(), lista -> {
            mostrarLista(lista);
            estado.setText("Lista actualizada.");
        }, this::mostrarErrorRemoto);
    }

    /** Busca por nombre o por id, segun el modo elegido. */
    private void buscar() {
        if (noHayConexion()) {
            return;
        }
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();

        if (optId.isSelected()) {
            if (texto.isEmpty()) {
                refrescar();
                return;
            }
            int id = Integer.parseInt(texto);
            estado.setText("Buscando el ID " + id + "...");
            enSegundoPlano(() -> agenda.buscarPorId(id), encontrado -> {
                mostrarLista(encontrado == null ? List.of() : List.of(encontrado));
                if (encontrado == null) {
                    estado.setText("No existe ningún contacto con el ID " + id + ".");
                } else {
                    tabla.getSelectionModel().selectFirst();
                    estado.setText("Contacto con ID " + id + " encontrado.");
                }
            }, this::mostrarErrorRemoto);
            return;
        }

        estado.setText("Buscando \"" + texto + "\"...");
        enSegundoPlano(() -> agenda.buscarPorNombre(texto), lista -> {
            mostrarLista(lista);
            estado.setText(lista.size() + " coincidencia(s) para \"" + texto + "\".");
        }, this::mostrarErrorRemoto);
    }

    /** Envia el alta de un contacto nuevo. */
    private void agregar() {
        if (noHayConexion()) {
            return;
        }
        Contacto nuevo = new Contacto(alta.nombre.getText(), alta.telefono.getText(),
                alta.email.getText());
        if (!validarAntesDeEnviar(nuevo)) {
            return;
        }

        estado.setText("Guardando contacto...");
        enSegundoPlano(() -> agenda.agregarContacto(nuevo), guardado -> {
            cerrarAlta();
            recargarYSeleccionar(guardado.getId(),
                    "Contacto \"" + guardado.getNombre() + "\" guardado con ID " + guardado.getId() + ".");
        }, this::mostrarErrorRemoto);
    }

    /** Envia la modificacion del contacto seleccionado. */
    private void actualizar() {
        if (noHayConexion()) {
            return;
        }
        Integer id = idDelFormulario();
        if (id == null) {
            aviso(TipoAviso.ADVERTENCIA, "Selecciona un contacto",
                    "Elige primero un renglón de la tabla para actualizarlo.");
            return;
        }

        Contacto modificado = new Contacto(id, edicion.nombre.getText(),
                edicion.telefono.getText(), edicion.email.getText());
        if (!validarAntesDeEnviar(modificado)) {
            return;
        }

        estado.setText("Actualizando contacto " + id + "...");
        enSegundoPlano(() -> agenda.actualizarContacto(modificado), ok -> {
            recargarYSeleccionar(id, Boolean.TRUE.equals(ok)
                    ? "Cambios del contacto " + id + " guardados."
                    : "No se modificó ningún registro.");
        }, this::mostrarErrorRemoto);
    }

    /** Elimina el contacto seleccionado, previa confirmacion. */
    private void eliminar() {
        if (noHayConexion()) {
            return;
        }
        Integer id = idDelFormulario();
        if (id == null) {
            aviso(TipoAviso.ADVERTENCIA, "Selecciona un contacto",
                    "Elige primero un renglón de la tabla para eliminarlo.");
            return;
        }

        String nombre = lblNombreDetalle.getText();
        confirmar("¿Eliminar este contacto?",
                "Se eliminará a " + nombre + " (ID " + id + ").\nEsta acción no se puede deshacer.",
                "Eliminar", () -> {
                    estado.setText("Eliminando contacto " + id + "...");
                    enSegundoPlano(() -> agenda.eliminarContacto(id), ok -> {
                        recargarYSeleccionar(-1, Boolean.TRUE.equals(ok)
                                ? "Contacto " + id + " eliminado."
                                : "No se eliminó ningún registro.");
                    }, this::mostrarErrorRemoto);
                }, () -> estado.setText("Eliminación cancelada."));
    }

    /**
     * Recarga la lista completa y, si existe, selecciona el contacto indicado.
     *
     * @param id      id a seleccionar tras la recarga ({@code -1} para ninguno)
     * @param mensaje texto para la barra de estado
     */
    private void recargarYSeleccionar(int id, String mensaje) {
        txtBuscar.clear();
        enSegundoPlano(() -> agenda.listarContactos(), lista -> {
            mostrarLista(lista);
            datos.stream().filter(c -> c.getId() == id).findFirst().ifPresent(c -> {
                tabla.getSelectionModel().select(c);
                tabla.scrollTo(c);
            });
            estado.setText(mensaje);
        }, this::mostrarErrorRemoto);
    }

    /** Cierra la sesion en el servidor al cerrar la ventana. */
    private void cerrarSesion() {
        if (agenda != null && idSesion != null) {
            try {
                agenda.desconectar(idSesion);
            } catch (Exception ignorada) {
                // La ventana se esta cerrando: no tiene caso avisar al usuario.
            }
        }
        Platform.exit();
    }

    // ==================================================================
    // Avisos dentro de la ventana
    // ==================================================================

    /** Tipos de aviso; cambian el color del icono. */
    private enum TipoAviso {
        /** Fallo de comunicacion o de conexion. */
        ERROR("#f8c2d9"),
        /** Dato invalido o accion que requiere atencion. */
        ADVERTENCIA("#f6e3b0");

        /** Color del circulo del icono. */
        private final String color;

        TipoAviso(String color) {
            this.color = color;
        }
    }

    /**
     * Muestra un aviso con un solo boton.
     *
     * @param tipo    tipo de aviso
     * @param titulo  encabezado
     * @param mensaje texto del aviso
     */
    private void aviso(TipoAviso tipo, String titulo, String mensaje) {
        Button ok = new Button("Entendido");
        ok.getStyleClass().add("boton-blanco");
        ok.setOnAction(e -> cerrarVelo());
        abrirVelo(tipo, titulo, mensaje, ok, null, null);
    }

    /**
     * Pide confirmacion para una accion destructiva.
     *
     * @param titulo      encabezado
     * @param mensaje     texto del aviso
     * @param textoAccion texto del boton que confirma
     * @param alAceptar   accion si se confirma
     * @param alCancelar  accion si se cancela
     */
    private void confirmar(String titulo, String mensaje, String textoAccion,
                           Runnable alAceptar, Runnable alCancelar) {
        Button si = new Button(textoAccion);
        si.getStyleClass().add("boton-rosa");
        si.setOnAction(e -> {
            cerrarVelo();
            alAceptar.run();
        });
        Button no = new Button("Cancelar");
        no.getStyleClass().add("boton-contorno");
        no.setOnAction(e -> {
            cerrarVelo();
            alCancelar.run();
        });
        abrirVelo(TipoAviso.ADVERTENCIA, titulo, mensaje, si, no, alCancelar);
    }

    /**
     * Arma y muestra la tarjeta del aviso sobre el velo.
     *
     * @param tipo       tipo de aviso
     * @param titulo     encabezado
     * @param mensaje    texto
     * @param principal  boton principal
     * @param secundario boton secundario (puede ser {@code null})
     * @param alEscapar  accion al cerrar con Esc (puede ser {@code null})
     */
    private void abrirVelo(TipoAviso tipo, String titulo, String mensaje,
                           Button principal, Button secundario, Runnable alEscapar) {
        Circle circulo = new Circle(20, Color.web(tipo.color));
        StackPane insignia = new StackPane(circulo, icono(ICONO_AVISO, 18, "icono", "oscuro"));
        insignia.setMaxSize(40, 40);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dialogo-titulo");
        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("dialogo-mensaje");
        lblMensaje.setWrapText(true);

        HBox botones = new HBox(10, espaciador());
        if (secundario != null) {
            botones.getChildren().add(secundario);
        }
        botones.getChildren().add(principal);
        principal.setDefaultButton(true);

        VBox tarjeta = new VBox(insignia, lblTitulo, lblMensaje, separacion(4), botones);
        tarjeta.getStyleClass().add("dialogo");
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(tarjeta, Priority.NEVER);

        velo.getChildren().setAll(tarjeta);
        velo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cerrarVelo();
                if (alEscapar != null) {
                    alEscapar.run();
                }
            }
        });
        velo.setOpacity(0);
        velo.setVisible(true);
        FadeTransition f = new FadeTransition(Duration.millis(140), velo);
        f.setToValue(1);
        f.play();
        principal.requestFocus();
    }

    /** Oculta el velo de avisos. */
    private void cerrarVelo() {
        velo.setVisible(false);
        velo.getChildren().clear();
    }

    // ==================================================================
    // Apoyo
    // ==================================================================

    /**
     * Ejecuta una llamada remota en un hilo aparte y entrega el resultado al
     * hilo de la interfaz.
     *
     * <p>Es el mecanismo que evita que la ventana se congele: {@link Task}
     * corre {@code trabajo} fuera del hilo de JavaFX y los manejadores
     * {@code alTerminar} y {@code alFallar} se ejecutan ya de vuelta en el hilo
     * de la interfaz, donde si es valido tocar los componentes.</p>
     *
     * @param <T>        tipo del resultado
     * @param trabajo    llamada remota a ejecutar
     * @param alTerminar que hacer con el resultado
     * @param alFallar   que hacer si la llamada lanza una excepcion
     */
    private <T> void enSegundoPlano(LlamadaRemota<T> trabajo,
                                    Consumer<T> alTerminar,
                                    Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.ejecutar();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));

        Thread hilo = new Thread(tarea, "rmi-cliente-fx");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Operacion remota que puede lanzar cualquier excepcion.
     *
     * @param <T> tipo del resultado
     */
    @FunctionalInterface
    private interface LlamadaRemota<T> {
        /**
         * Ejecuta la llamada.
         *
         * @return el resultado de la llamada
         * @throws Exception si la llamada falla
         */
        T ejecutar() throws Exception;
    }

    /**
     * Carga una lista en la tabla y actualiza el contador.
     *
     * @param lista contactos a mostrar
     */
    private void mostrarLista(List<Contacto> lista) {
        datos.setAll(lista);
        lblTotal.setText(lista.size() == 1 ? "1 contacto" : lista.size() + " contactos");
    }

    /**
     * Cambia el indicador de conexion junto al titulo.
     *
     * @param ok    {@code true} si hay conexion
     * @param texto texto a mostrar
     */
    private void indicarConexion(boolean ok, String texto) {
        lblConexion.setText(texto);
        puntoConexion.getStyleClass().removeAll("conectado", "error");
        puntoConexion.getStyleClass().add(ok ? "conectado" : "error");
    }

    /**
     * Valida los datos en el cliente antes de gastar una llamada remota.
     *
     * @param c contacto a validar
     * @return {@code true} si los datos son correctos
     */
    private boolean validarAntesDeEnviar(Contacto c) {
        try {
            Validador.validarYNormalizar(c);
            return true;
        } catch (ValidacionException e) {
            aviso(TipoAviso.ADVERTENCIA, "Revisa los datos", e.getMessage());
            estado.setText("Dato inválido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Muestra el error de una llamada remota. Distingue los rechazos por
     * validacion (culpa del dato) de los fallos de comunicacion.
     *
     * @param error excepcion recibida
     */
    private void mostrarErrorRemoto(Throwable error) {
        Throwable causa = error;
        while (causa != null && !(causa instanceof ValidacionException) && causa.getCause() != null) {
            causa = causa.getCause();
        }
        if (causa instanceof ValidacionException) {
            aviso(TipoAviso.ADVERTENCIA, "El servidor rechazó los datos", causa.getMessage());
            estado.setText("Rechazado: " + causa.getMessage());
        } else {
            indicarConexion(false, "Error de comunicación");
            aviso(TipoAviso.ERROR, "Error de comunicación",
                    "No se pudo completar la operación.\n\nDetalle: " + error);
            estado.setText("Error de comunicación con el servidor.");
        }
    }

    /**
     * Comprueba que exista conexion antes de operar.
     *
     * @return {@code true} si NO hay conexion (y ya se aviso al usuario)
     */
    private boolean noHayConexion() {
        if (agenda == null) {
            aviso(TipoAviso.ERROR, "Sin conexión",
                    "Todavía no hay conexión con el servidor. Arráncalo y vuelve a abrir el cliente.");
            return true;
        }
        return false;
    }

    /**
     * Lee el id del contacto seleccionado.
     *
     * @return el id, o {@code null} si no hay contacto seleccionado
     */
    private Integer idDelFormulario() {
        try {
            return Integer.valueOf(txtId.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Crea un icono de trazo a partir de un path SVG en rejilla de 24x24.
     *
     * @param trazo  datos del path
     * @param tamano tamano final en pixeles
     * @param clases clases de estilo del trazo
     * @return el icono, listo para usarse como grafico
     */
    private static Node icono(String trazo, double tamano, String... clases) {
        SVGPath path = new SVGPath();
        path.setContent(trazo);
        path.getStyleClass().addAll(clases);
        double escala = tamano / 24.0;
        path.setScaleX(escala);
        path.setScaleY(escala);
        return new Group(path);
    }

    /**
     * Color pastel para el avatar de un contacto.
     *
     * @param id id del contacto
     * @return el color
     */
    private static Color pastel(int id) {
        return PASTELES.get(Math.floorMod(id, PASTELES.size()));
    }

    /**
     * Da formato de lectura a un telefono de 10 digitos: {@code 733 123 4567}.
     *
     * @param tel telefono tal como se guarda
     * @return el telefono con espacios, o el original si no mide 10
     */
    private static String formatearTelefono(String tel) {
        if (tel == null || tel.length() != 10) {
            return tel;
        }
        return tel.substring(0, 3) + " " + tel.substring(3, 6) + " " + tel.substring(6);
    }

    /**
     * Obtiene las iniciales (maximo dos) de un nombre.
     *
     * @param nombre nombre completo
     * @return las iniciales en mayusculas
     */
    private static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String r = partes[0].substring(0, 1);
        if (partes.length > 1) {
            r += partes[partes.length > 2 ? partes.length - 2 : 1].substring(0, 1);
        }
        return r.toUpperCase();
    }

    /**
     * Crea una etiqueta pequena para encabezar un campo.
     *
     * @param texto texto de la etiqueta
     * @return la etiqueta
     */
    private static Label etiquetaCampo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta-campo");
        return l;
    }

    /**
     * Region que se estira horizontalmente para empujar a los demas nodos.
     *
     * @return la region
     */
    private static Region espaciador() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /**
     * Region que se estira verticalmente para empujar a los demas nodos.
     *
     * @return la region
     */
    private static Region espaciadorVertical() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    /**
     * Separacion vertical fija.
     *
     * @param alto altura en pixeles
     * @return la region
     */
    private static Node separacion(double alto) {
        Region r = new Region();
        r.setMinHeight(alto);
        return r;
    }

    // ==================================================================
    // Componentes reutilizables
    // ==================================================================

    /**
     * Terna de campos nombre, telefono y correo con validacion en vivo. La
     * usan tanto el formulario de alta como la tarjeta de edicion.
     */
    private static final class CamposContacto {

        /** Campo del nombre. */
        final TextField nombre = new TextField();

        /** Campo del telefono. */
        final TextField telefono = new TextField();

        /** Campo del correo electronico. */
        final TextField email = new TextField();

        /** Mensaje de validacion del nombre. */
        final Label errNombre = etiquetaError();

        /** Mensaje de validacion del telefono. */
        final Label errTelefono = etiquetaError();

        /** Mensaje de validacion del correo. */
        final Label errEmail = etiquetaError();

        /** Configura los textos de ayuda y la validacion en vivo. */
        CamposContacto() {
            nombre.setPromptText("Ej. María López Hernández");
            telefono.setPromptText("10 dígitos, ej. 7331234567");
            email.setPromptText("nombre@dominio.com");

            // El telefono solo admite digitos y como maximo diez.
            telefono.setTextFormatter(new TextFormatter<String>(cambio ->
                    cambio.getControlNewText().matches("\\d{0,"
                            + Validador.TELEFONO_DIGITOS + "}") ? cambio : null));

            nombre.textProperty().addListener((o, a, b) ->
                    validarCampo(nombre, b, errNombre, Validador::validarNombre));
            telefono.textProperty().addListener((o, a, b) ->
                    validarCampo(telefono, b, errTelefono, Validador::validarTelefono));
            email.textProperty().addListener((o, a, b) ->
                    validarCampo(email, b, errEmail, Validador::validarEmail));
        }

        /** Vacia los campos y sus mensajes. */
        void limpiar() {
            nombre.clear();
            telefono.clear();
            email.clear();
            errNombre.setText("");
            errTelefono.setText("");
            errEmail.setText("");
        }

        /**
         * Valida un campo mientras el usuario escribe, muestra el mensaje
         * debajo y marca el borde si el valor es invalido.
         *
         * @param campo    campo que cambio
         * @param valor    contenido actual del campo
         * @param etiqueta etiqueta donde se muestra el mensaje
         * @param regla    regla de validacion a aplicar
         */
        private static void validarCampo(TextField campo, String valor, Label etiqueta,
                                         ReglaValidacion regla) {
            campo.getStyleClass().remove("invalido");
            if (valor == null || valor.isEmpty()) {
                etiqueta.setText("");
                return;
            }
            try {
                regla.validar(valor);
                etiqueta.setText("");
            } catch (ValidacionException e) {
                etiqueta.setText(e.getMessage());
                campo.getStyleClass().add("invalido");
            }
        }

        /**
         * Crea una etiqueta con el formato de los mensajes de validacion.
         *
         * @return la etiqueta, inicialmente vacia
         */
        private static Label etiquetaError() {
            Label l = new Label();
            l.getStyleClass().add("error-campo");
            l.setWrapText(true);
            l.setMinHeight(Region.USE_PREF_SIZE);
            return l;
        }
    }

    /** Regla de validacion de un campo del {@link Validador}. */
    @FunctionalInterface
    private interface ReglaValidacion {
        /**
         * Valida el valor.
         *
         * @param valor valor a revisar
         * @throws ValidacionException si el valor es invalido
         */
        void validar(String valor) throws ValidacionException;
    }

    /** Celda de texto con prefijo opcional y color secundario. */
    private static final class CeldaTexto extends TableCell<Contacto, String> {
        /** Texto que se antepone al valor. */
        private final String prefijo;

        CeldaTexto(String prefijo, boolean tenue) {
            this.prefijo = prefijo;
            if (tenue) {
                getStyleClass().add("celda-tenue");
            }
        }

        @Override
        protected void updateItem(String item, boolean vacia) {
            super.updateItem(item, vacia);
            setText(vacia || item == null ? null : prefijo + item);
        }
    }

    /** Celda que antepone al nombre un circulo pastel con sus iniciales. */
    private static final class CeldaNombreConAvatar extends TableCell<Contacto, String> {
        /** Circulo de fondo. */
        private final Circle fondo = new Circle(14);

        /** Iniciales. */
        private final Label letras = new Label();

        /** Avatar completo (circulo + iniciales). */
        private final StackPane avatar;

        CeldaNombreConAvatar() {
            letras.getStyleClass().add("chip-letra");
            avatar = new StackPane(fondo, letras);
            setGraphicTextGap(12);
        }

        @Override
        protected void updateItem(String item, boolean vacia) {
            super.updateItem(item, vacia);
            Contacto c = vacia || getTableRow() == null ? null : getTableRow().getItem();
            if (vacia || item == null || c == null) {
                setText(null);
                setGraphic(null);
            } else {
                letras.setText(iniciales(item));
                fondo.setFill(pastel(c.getId()));
                setText(item);
                setGraphic(avatar);
            }
        }
    }

    /**
     * Permite arrancar la interfaz tambien de forma directa.
     *
     * @param args argumentos de la linea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }
}

package dev.danipraivet.controlador;

import dev.danipraivet.vista.Aplicacion;
import dev.danipraivet.vista.utilidades.GestorAlertas;
import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.entidades.Fichaje;
import dev.danipraivet.modelo.servicio.ServicioAutenticacion;
import dev.danipraivet.modelo.servicio.ServicioEmpleado;
import dev.danipraivet.modelo.servicio.ServicioFichaje;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

// Controlador de la vista Admin.fxml.
// Extiende las funciones de RRHH con control total de fichajes y eliminacion de empleados.
public class ControladorAdmin implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ControladorAdmin.class);
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    // Dashboard
    @FXML
    private Label lblNombreAdmin;
    @FXML
    private Label lblTotalEmpleados;
    @FXML
    private Label lblFichadosHoy;
    @FXML
    private Label lblEstadoAdmin;
    @FXML
    private Button btnFicharAdmin;

    // Pestaña Fichajes - filtros y tabla
    @FXML
    private DatePicker dpFichajeDesde;
    @FXML
    private DatePicker dpFichajeHasta;
    @FXML
    private ComboBox<Empleado> cmbFiltroEmpleado;
    @FXML
    private Button btnBuscarFichajes;
    @FXML
    private TableView<Fichaje> tablaFichajes;
    @FXML
    private TableColumn<Fichaje, String> colFicFecha;
    @FXML
    private TableColumn<Fichaje, String> colFicEmpleado;
    @FXML
    private TableColumn<Fichaje, String> colFicEntrada;
    @FXML
    private TableColumn<Fichaje, String> colFicSalida;
    @FXML
    private TableColumn<Fichaje, String> colFicHoras;
    @FXML
    private TableColumn<Fichaje, String> colFicEstado;

    // Pestaña Fichajes - panel de edicion
    @FXML
    private TextField txtEditEntrada;
    @FXML
    private TextField txtEditSalida;
    @FXML
    private CheckBox chkEditFestivo;
    @FXML
    private CheckBox chkEditJustificado;
    @FXML
    private TextArea txtEditObservaciones;
    @FXML
    private Button btnGuardarFichaje;
    @FXML
    private Button btnEliminarFichaje;

    // Pestaña Empleados
    @FXML
    private TableView<Empleado> tablaEmpleadosAdmin;
    @FXML
    private TableColumn<Empleado, String> colEmpNombre;
    @FXML
    private TableColumn<Empleado, String> colEmpDni;
    @FXML
    private TableColumn<Empleado, String> colEmpRol;
    @FXML
    private TableColumn<Empleado, String> colEmpEstado;
    @FXML
    private TableColumn<Empleado, String> colEmpUltimoAcceso;
    @FXML
    private TextField txtBuscarAdmin;
    @FXML
    private Button btnDesbloquear;
    @FXML
    private Button btnEliminarEmpleado;

    private final ServicioFichaje servicioFichaje = new ServicioFichaje();
    private final ServicioEmpleado servicioEmpleado = new ServicioEmpleado();
    private final ServicioAutenticacion servicioAuth = new ServicioAutenticacion();

    private final ObservableList<Fichaje> fichajes = FXCollections.observableArrayList();
    private final ObservableList<Empleado> empleados = FXCollections.observableArrayList();
    private FilteredList<Empleado> empleadosFiltrados;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablaFichajes();
        configurarTablaEmpleados();
        cargarDatos();
    }

    @FXML
    public void onFicharAdmin() {
        String mensaje = servicioFichaje.fichar();
        actualizarDashboard();
        GestorAlertas.info("Fichaje", mensaje);
    }

    @FXML
    public void onBuscarFichajes() {
        LocalDate desde = dpFichajeDesde.getValue() != null
                ? dpFichajeDesde.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate hasta = dpFichajeHasta.getValue() != null
                ? dpFichajeHasta.getValue() : LocalDate.now();

        Empleado filtroEmp = cmbFiltroEmpleado.getValue();
        List<Fichaje> resultado;

        if (filtroEmp != null) {
            resultado = servicioFichaje.getHistorial(filtroEmp.getCodEmpleado(), desde, hasta);
        } else {
            resultado = servicioFichaje.getTodosConEmpleado(desde, hasta);
        }

        fichajes.setAll(resultado);
        log.info("Busqueda de fichajes: {} registros encontrados", resultado.size());
    }

    @FXML
    public void onGuardarFichaje() {
        Fichaje seleccionado = tablaFichajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            GestorAlertas.advertencia("Sin seleccion", "Selecciona un fichaje de la tabla.");
            return;
        }
        try {
            if (!txtEditEntrada.getText().isBlank()) {
                seleccionado.setEntradaHora(LocalTime.parse(txtEditEntrada.getText(), FMT_HORA));
            }
            if (!txtEditSalida.getText().isBlank()) {
                seleccionado.setSalidaHora(LocalTime.parse(txtEditSalida.getText(), FMT_HORA));
            }
            seleccionado.setFestivo(chkEditFestivo.isSelected());
            seleccionado.setJustificado(chkEditJustificado.isSelected());
            seleccionado.setObservaciones(txtEditObservaciones.getText());

            boolean ok = servicioFichaje.actualizar(seleccionado);
            if (ok) {
                GestorAlertas.info("Guardado", "Fichaje actualizado correctamente.");
                onBuscarFichajes();
            } else {
                GestorAlertas.error("Error", "No se pudo actualizar el fichaje.");
            }
        } catch (Exception e) {
            GestorAlertas.error("Error de formato", "Introduce la hora con formato HH:mm (ej. 08:30).");
        }
    }

    @FXML
    public void onEliminarFichaje() {
        Fichaje seleccionado = tablaFichajes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            GestorAlertas.advertencia("Sin seleccion", "Selecciona un fichaje de la tabla.");
            return;
        }
        boolean confirmar = GestorAlertas.confirmar(
                "Eliminar fichaje",
                "¿Seguro que quieres eliminar este fichaje?\nEsta accion no se puede deshacer."
        );
        if (!confirmar) return;

        boolean ok = servicioFichaje.eliminar(seleccionado.getId());
        if (ok) {
            GestorAlertas.info("Eliminado", "Fichaje eliminado correctamente.");
            onBuscarFichajes();
        } else {
            GestorAlertas.error("Error", "No se pudo eliminar el fichaje.");
        }
    }

    @FXML
    public void onDesbloquear() {
        Empleado seleccionado = tablaEmpleadosAdmin.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            GestorAlertas.advertencia("Sin seleccion", "Selecciona un empleado.");
            return;
        }
        ServicioEmpleado.ResultadoCRUD res = servicioEmpleado.desbloquear(seleccionado.getCodEmpleado());
        GestorAlertas.info("Resultado", res.mensaje());
        if (res.exito()) cargarEmpleados();
    }

    @FXML
    public void onEliminarEmpleado() {
        Empleado seleccionado = tablaEmpleadosAdmin.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            GestorAlertas.advertencia("Sin seleccion", "Selecciona un empleado.");
            return;
        }
        boolean confirmar = GestorAlertas.confirmar(
                "Eliminar empleado permanentemente",
                "¿Seguro que quieres eliminar a " + seleccionado.getNombreCompleto() + "?\n" +
                        "Se borran tambien TODOS sus fichajes. Esta accion es IRREVERSIBLE."
        );
        if (!confirmar) return;

        ServicioEmpleado.ResultadoCRUD res = servicioEmpleado.eliminar(seleccionado.getCodEmpleado());
        GestorAlertas.info("Resultado", res.mensaje());
        if (res.exito()) cargarEmpleados();
    }

    @FXML
    public void onLogout() {
        servicioAuth.logout();
        Aplicacion.navegarA("Login");
    }

    private void cargarDatos() {
        cargarEmpleados();
        actualizarDashboard();
        dpFichajeDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpFichajeHasta.setValue(LocalDate.now());
        lblFichadosHoy.setText(String.valueOf(servicioEmpleado.listarActivos().size()));
    }

    private void cargarEmpleados() {
        List<Empleado> lista = servicioEmpleado.listarTodos();
        empleados.setAll(lista);
        cmbFiltroEmpleado.setItems(FXCollections.observableArrayList(lista));
    }

    private void actualizarDashboard() {
        lblNombreAdmin.setText("Admin: " + GestorSesion.getNombreCompleto());
        lblTotalEmpleados.setText(String.valueOf(empleados.size()));

        boolean fichado = servicioFichaje.estaFichadoHoy();
        if (fichado) {
            btnFicharAdmin.setText("Salida");
            btnFicharAdmin.setStyle("-fx-background-color: #e53935; -fx-text-fill: white;");
            lblEstadoAdmin.setText("Trabajando");
        } else {
            btnFicharAdmin.setText("Entrada");
            btnFicharAdmin.setStyle("-fx-background-color: #43a047; -fx-text-fill: white;");
            lblEstadoAdmin.setText("No fichado");
        }
    }

    private void configurarTablaFichajes() {
        colFicFecha.setCellValueFactory(cd -> {
            LocalDate f = cd.getValue().getFecha();
            return new SimpleStringProperty(f != null ? f.format(FMT_FECHA) : "");
        });
        colFicEmpleado.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getCodEmpleado())));
        colFicEntrada.setCellValueFactory(cd -> {
            LocalTime t = cd.getValue().getEntradaHora();
            return new SimpleStringProperty(t != null ? t.format(FMT_HORA) : "--:--");
        });
        colFicSalida.setCellValueFactory(cd -> {
            LocalTime t = cd.getValue().getSalidaHora();
            return new SimpleStringProperty(t != null ? t.format(FMT_HORA) : "--:--");
        });
        colFicHoras.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getHorasFormateadas()));
        colFicEstado.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getEstado()));

        // Al seleccionar un fichaje, cargar sus datos en el panel de edicion
        tablaFichajes.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nuevo) -> {
                    if (nuevo != null) cargarFichajeEnPanel(nuevo);
                }
        );

        tablaFichajes.setItems(fichajes);
        tablaFichajes.setPlaceholder(new Label("Usa los filtros y pulsa Buscar."));
    }

    private void cargarFichajeEnPanel(Fichaje f) {
        txtEditEntrada.setText(f.getEntradaHora() != null ? f.getEntradaHora().format(FMT_HORA) : "");
        txtEditSalida.setText(f.getSalidaHora() != null ? f.getSalidaHora().format(FMT_HORA) : "");
        chkEditFestivo.setSelected(f.isFestivo());
        chkEditJustificado.setSelected(f.isJustificado());
        txtEditObservaciones.setText(f.getObservaciones() != null ? f.getObservaciones() : "");
    }

    private void configurarTablaEmpleados() {
        colEmpNombre.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNombreCompleto()));
        colEmpDni.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDni()));
        colEmpRol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getRol().getEtiqueta()));
        colEmpEstado.setCellValueFactory(cd -> {
            Empleado e = cd.getValue();
            String estado = !e.isActivo() ? "Baja" : e.isBloqueado() ? "Bloqueado" : "Activo";
            return new SimpleStringProperty(estado);
        });
        colEmpUltimoAcceso.setCellValueFactory(cd -> {
            var ua = cd.getValue().getUltimoAcceso();
            return new SimpleStringProperty(ua != null ? ua.toLocalDate().format(FMT_FECHA) : "Nunca");
        });

        // Colorear filas segun estado
        tablaEmpleadosAdmin.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Empleado e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) setStyle("");
                else if (!e.isActivo()) setStyle("-fx-background-color: #ffebee;");
                else if (e.isBloqueado()) setStyle("-fx-background-color: #fff3e0;");
                else setStyle("");
            }
        });

        // Filtro de busqueda por nombre o DNI
        empleadosFiltrados = new FilteredList<>(empleados, emp -> true);
        txtBuscarAdmin.textProperty().addListener((obs, old, texto) ->
                empleadosFiltrados.setPredicate(e -> {
                    if (texto == null || texto.isBlank()) return true;
                    String f = texto.toLowerCase();
                    return e.getNombreCompleto().toLowerCase().contains(f)
                            || e.getDni().toLowerCase().contains(f);
                })
        );
        tablaEmpleadosAdmin.setItems(empleadosFiltrados);
        tablaEmpleadosAdmin.setPlaceholder(new Label("No hay empleados."));
    }
}

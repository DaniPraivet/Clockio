package dev.danipraivet.controlador;

import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.entidades.Fichaje;
import dev.danipraivet.modelo.servicio.ServicioAutenticacion;
import dev.danipraivet.modelo.servicio.ServicioEmpleado;
import dev.danipraivet.modelo.servicio.ServicioFichaje;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import dev.danipraivet.vista.Aplicacion;
import dev.danipraivet.vista.utilidades.GestorAlertas;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import javafx.application.Platform;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControladorAdmin implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ControladorAdmin.class);
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private final ServicioFichaje servicioFichaje = new ServicioFichaje();
    private final ServicioEmpleado servicioEmpleado = new ServicioEmpleado();
    private final ServicioAutenticacion servicioAuth = new ServicioAutenticacion();
    private final ObservableList<Fichaje> fichajes = FXCollections.observableArrayList();
    private final ObservableList<Empleado> empleados = FXCollections.observableArrayList();
    @FXML
    private Label lblNombreAdmin;
    @FXML
    private Label lblTotalEmpleados;
    @FXML
    private Label lblFichadosHoy;
    @FXML
    private Label lblEstadoAdmin;
    @FXML
    private MFXButton btnFicharAdmin;
    @FXML
    private MFXDatePicker dpFichajeDesde;
    @FXML
    private MFXDatePicker dpFichajeHasta;
    @FXML
    private MFXComboBox<Empleado> cmbFiltroEmpleado;
    @FXML
    private MFXButton btnBuscarFichajes;
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
    private MFXButton btnGuardarFichaje;
    @FXML
    private MFXButton btnEliminarFichaje;
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
    private MFXButton btnDesbloquear;
    @FXML
    private MFXButton btnEliminarEmpleado;
    private FilteredList<Empleado> empleadosFiltrados;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablaFichajes();
        configurarTablaEmpleados();
        Platform.runLater(this::cargarDatos);
        Aplicacion.iniciarScheduler(() -> Platform.runLater(this::actualizarDashboard), 60);
    }

    @FXML
    public void onFicharAdmin() {
        String mensaje = servicioFichaje.fichar();
        actualizarDashboard();
        GestorAlertas.info("Fichaje", mensaje);
    }

    @FXML
    public void onBuscarFichajes() {
        LocalDate desde = dpFichajeDesde.getValue() != null ? dpFichajeDesde.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate hasta = dpFichajeHasta.getValue() != null ? dpFichajeHasta.getValue() : LocalDate.now();

        Empleado filtroEmp = cmbFiltroEmpleado.getValue();
        List<Fichaje> resultado;

        if (filtroEmp != null) {
            resultado = servicioFichaje.getHistorial(filtroEmp.getCodEmpleado(), desde, hasta);
        } else {
            resultado = servicioFichaje.getTodosConEmpleado(desde, hasta);
        }

        fichajes.setAll(resultado);
        tablaFichajes.setItems(fichajes);
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
        boolean confirmar = GestorAlertas.confirmar("Eliminar fichaje", "¿Seguro que quieres eliminar este fichaje?\nEsta accion no se puede deshacer.");
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
        boolean confirmar = GestorAlertas.confirmar("Eliminar empleado permanentemente", "¿Seguro que quieres eliminar a " + seleccionado.getNombreCompleto() + "?\n" + "Se borran tambien TODOS sus fichajes. Esta accion es IRREVERSIBLE.");
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
    }

    private void cargarEmpleados() {
        List<Empleado> lista = servicioEmpleado.listarTodos();
        empleados.setAll(lista);
        tablaEmpleadosAdmin.setItems(empleados);
        cmbFiltroEmpleado.setItems(FXCollections.observableArrayList(lista));
    }

    private void actualizarDashboard() {
        lblNombreAdmin.setText("Admin: " + GestorSesion.getNombreCompleto());

        long activos = empleados.stream().filter(Empleado::isActivo).count();
        lblTotalEmpleados.setText(String.valueOf(activos));

        lblFichadosHoy.setText(String.valueOf(servicioFichaje.contarFichadosHoy()));

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
        colFicFecha.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFecha() != null ? cd.getValue().getFecha().format(FMT_FECHA) : ""));
        colFicEmpleado.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getCodEmpleado())));
        colFicEntrada.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEntradaHora() != null ? cd.getValue().getEntradaHora().format(FMT_HORA) : "--:--"));
        colFicSalida.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSalidaHora() != null ? cd.getValue().getSalidaHora().format(FMT_HORA) : "--:--"));
        colFicHoras.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHorasFormateadas()));
        colFicEstado.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEstado()));

        tablaFichajes.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevo) -> {
            if (nuevo != null) cargarFichajeEnPanel(nuevo);
        });
        tablaFichajes.setItems(fichajes);
        tablaFichajes.setPlaceholder(new Label("Usa los filtros y pulsa Buscar."));
        tablaFichajes.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void cargarFichajeEnPanel(Fichaje f) {
        txtEditEntrada.setText(f.getEntradaHora() != null ? f.getEntradaHora().format(FMT_HORA) : "");
        txtEditSalida.setText(f.getSalidaHora() != null ? f.getSalidaHora().format(FMT_HORA) : "");
        chkEditFestivo.setSelected(f.isFestivo());
        chkEditJustificado.setSelected(f.isJustificado());
        txtEditObservaciones.setText(f.getObservaciones() != null ? f.getObservaciones() : "");
    }

    private void configurarTablaEmpleados() {
        colEmpNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombreCompleto()));
        colEmpDni.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDni()));
        colEmpRol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRol().getEtiqueta()));
        colEmpEstado.setCellValueFactory(cd -> new SimpleStringProperty(!cd.getValue().isActivo() ? "Baja" : cd.getValue().isBloqueado() ? "Bloqueado" : "Activo"));
        colEmpUltimoAcceso.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUltimoAcceso() != null ? cd.getValue().getUltimoAcceso().toLocalDate().format(FMT_FECHA) : "Nunca"));

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
        tablaEmpleadosAdmin.setItems(empleados);
        tablaEmpleadosAdmin.setPlaceholder(new Label("No hay empleados."));
        tablaEmpleadosAdmin.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
}

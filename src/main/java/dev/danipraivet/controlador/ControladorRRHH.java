package dev.danipraivet.controlador;

import dev.danipraivet.modelo.entidades.Departamento;
import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.entidades.Fichaje;
import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.servicio.ServicioAutenticacion;
import dev.danipraivet.modelo.servicio.ServicioEmpleado;
import dev.danipraivet.modelo.servicio.ServicioFichaje;
import dev.danipraivet.modelo.servicio.ServicioInformes;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import dev.danipraivet.vista.Aplicacion;
import dev.danipraivet.vista.utilidades.GestorAlertas;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ControladorRRHH implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ControladorRRHH.class);
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ServicioInformes servicioInformes = new ServicioInformes();
    private final ServicioFichaje servicioFichaje = new ServicioFichaje();
    private final ServicioEmpleado servicioEmpleado = new ServicioEmpleado();
    private final ServicioAutenticacion servicioAuth = new ServicioAutenticacion();
    private final ObservableList<Empleado> empleados = FXCollections.observableArrayList();
    private final ObservableList<Fichaje> fichajes = FXCollections.observableArrayList();
    @FXML
    public Label lblTotalHorasMes;
    @FXML
    private Label lblFechaHora;
    @FXML
    private Label lblBienvenidaRRHH;
    @FXML
    private Label lblEstadoRRHH;
    @FXML
    private MFXButton btnFicharRRHH;
    @FXML
    private TextField txtBuscar;
    @FXML
    private TableView<Empleado> tablaEmpleados;
    @FXML
    private TableColumn<Empleado, Integer> colCod;
    @FXML
    private TableColumn<Empleado, String> colNombre;
    @FXML
    private TableColumn<Empleado, String> colDni;
    @FXML
    private TableColumn<Empleado, String> colRol;
    @FXML
    private TableColumn<Empleado, String> colDep;
    @FXML
    private TableColumn<Empleado, String> colEstado;
    @FXML
    private TableView<Fichaje> tablaHistorial;
    @FXML
    private TableColumn<Fichaje, LocalDate> colFecha;
    @FXML
    private TableColumn<Fichaje, String> colEntrada;
    @FXML
    private TableColumn<Fichaje, String> colSalida;
    @FXML
    private TableColumn<Fichaje, String> colTurno;
    @FXML
    private TableColumn<Fichaje, String> colHoras;
    @FXML
    private TableColumn<Fichaje, String> colEstadoFichaje;
    @FXML
    private TextField txtFormCod;
    @FXML
    private TextField txtFormNombre;
    @FXML
    private TextField txtFormApellido1;
    @FXML
    private TextField txtFormApellido2;
    @FXML
    private TextField txtFormDni;
    @FXML
    private TextField txtFormEmail;
    @FXML
    private TextField txtFormTelefono;
    @FXML
    private TextField txtFormUsername;
    @FXML
    private MFXComboBox<Departamento> cmbFormDpto;
    @FXML
    private MFXPasswordField txtFormContrasena;
    @FXML
    private MFXComboBox<Rol> cmbFormRol;
    @FXML
    private MFXButton btnGuardar;
    @FXML
    private MFXButton btnNuevo;
    @FXML
    private MFXButton btnBaja;
    @FXML
    private Label lblFormMensaje;
    @FXML
    private MFXDatePicker dpDesde;
    @FXML
    private MFXDatePicker dpHasta;
    @FXML
    private MFXComboBox<Empleado> cmbEmpleadoInforme;
    @FXML
    private MFXButton btnGenerarPdf;
    @FXML
    private MFXButton btnGenerarExcel;
    private FilteredList<Empleado> empleadosFiltrados;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablaEmpleados();
        configurarFormulario();
        configurarInformes();
        actualizarBotonesAccion();

        txtBuscar.textProperty().addListener((obs, old, texto) -> tablaEmpleados.setItems(empleados.filtered(e -> {
            if (texto == null || texto.isBlank()) return true;
            String f = texto.toLowerCase();
            return e.getNombreCompleto().toLowerCase().contains(f) || e.getDni().toLowerCase().contains(f) || e.getUsername().toLowerCase().contains(f);
        })));
        configurarTabla();
        Platform.runLater(this::actualizarVista);
        Platform.runLater(this::cargarDatos);
    }

    private void actualizarVista() {
        lblBienvenidaRRHH.setText("Bienvenido, " + GestorSesion.getNombreCompleto());
        lblFechaHora.setText(LocalDate.now().format(FMT_FECHA));

        boolean fichado = servicioFichaje.estaFichadoHoy();
        actualizarBotonFichaje(fichado);
        cargarHistorial();
    }

    private void cargarHistorial() {
        List<Fichaje> lista = servicioFichaje.getMesActual();
        fichajes.setAll(lista);
        double totalHoras = lista.stream().filter(f -> f.getHorasTrabajadas() != null).mapToDouble(f -> f.getHorasTrabajadas().doubleValue()).sum();
        lblTotalHorasMes.setText(String.format("Total mes: %.1f h", totalHoras));
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fecha"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(FMT_FECHA));
            }
        });
        colEntrada.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEntradaHora() != null ? cd.getValue().getEntradaHora().format(FMT_HORA) : "--:--"));
        colSalida.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSalidaHora() != null ? cd.getValue().getSalidaHora().format(FMT_HORA) : "--:--"));
        colTurno.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurnoEntrada() != null ? cd.getValue().getTurnoEntrada().getEtiqueta() : ""));
        colHoras.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHorasFormateadas()));
        colEstadoFichaje.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEstado()));

        tablaHistorial.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Fichaje f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) setStyle("");
                else if ("Trabajando".equals(f.getEstado())) setStyle("-fx-background-color: #e8f5e9;");
                else if ("Ausente".equals(f.getEstado())) setStyle("-fx-background-color: #fff3e0;");
                else setStyle("");
            }
        });
        tablaHistorial.setItems(fichajes);
        tablaHistorial.setPlaceholder(new Label("No hay fichajes este mes."));
    }

    @FXML
    public void onFicharRRHH() {
        String mensaje = servicioFichaje.fichar();
        actualizarEstadoFichaje();
        GestorAlertas.info("Fichaje", mensaje);
    }

    @FXML
    public void onNuevo() {
        limpiarFormulario();
        modoEdicion = false;
        txtFormCod.setDisable(false);
        habilitarFormulario(true);
        lblFormMensaje.setVisible(false);
    }

    @FXML
    public void onGuardar() {
        Empleado e = recogerFormulario();
        if (e == null) return;

        ServicioEmpleado.ResultadoCRUD resultado;
        if (modoEdicion) {
            resultado = servicioEmpleado.actualizar(e);
        } else {
            resultado = servicioEmpleado.crear(e, txtFormContrasena.getText());
        }

        mostrarMensajeFormulario(resultado.mensaje(), resultado.exito());
        if (resultado.exito()) {
            cargarDatos();
            limpiarFormulario();
            habilitarFormulario(false);
        }
    }

    @FXML
    public void onDarDeBaja() {
        Empleado seleccionado = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            GestorAlertas.advertencia("Seleccion", "Selecciona un empleado de la tabla.");
            return;
        }
        boolean confirmar = GestorAlertas.confirmar("Dar de baja", "¿Seguro que quieres dar de baja a " + seleccionado.getNombreCompleto() + "?\n" + "Su historial de fichajes se conservara.");
        if (!confirmar) return;

        ServicioEmpleado.ResultadoCRUD resultado = servicioEmpleado.darDeBaja(seleccionado.getCodEmpleado());
        GestorAlertas.info("Resultado", resultado.mensaje());
        if (resultado.exito()) cargarDatos();
    }

    @FXML
    public void onGenerarPdf() {
        Empleado empleado = cmbEmpleadoInforme.getValue();
        LocalDate desde = dpDesde.getValue() != null ? dpDesde.getValue() : LocalDate.now();
        LocalDate hasta = dpHasta.getValue() != null ? dpHasta.getValue() : LocalDate.now();

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName("informe_asistencia_" + LocalDate.now() + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = selector.showSaveDialog(Aplicacion.getEscenarioPrincipal());
        if (destino == null) return;

        var fichajes = empleado != null ? servicioFichaje.getHistorial(empleado.getCodEmpleado(), desde, hasta) : servicioFichaje.getTodosConEmpleado(desde, hasta);

        boolean ok = servicioInformes.generarPDF(empleado, fichajes, desde, hasta, destino);
        if (ok) GestorAlertas.info("PDF generado", "Documento guardado en: \n" + destino.getAbsolutePath());
        else GestorAlertas.error("Error", "No se pudo generar el PDF");
    }

    @FXML
    public void onGenerarExcel() {
        Empleado empleado = cmbEmpleadoInforme.getValue();
        LocalDate desde = dpDesde.getValue() != null ? dpDesde.getValue() : LocalDate.now();
        LocalDate hasta = dpHasta.getValue() != null ? dpHasta.getValue() : LocalDate.now();

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar Excel");
        selector.setInitialFileName("informe_asistencia_" + LocalDate.now() + ".xlsx");
        File destino = selector.showSaveDialog(Aplicacion.getEscenarioPrincipal());
        if (destino == null) return;

        var fichajes = empleado != null ? servicioFichaje.getHistorial(empleado.getCodEmpleado(), desde, hasta) : servicioFichaje.getTodosConEmpleado(desde, hasta);
        boolean ok = servicioInformes.generarExcel(empleado, fichajes, desde, hasta, destino);
        if (ok) GestorAlertas.info("Excel generado", "Hoja guardada en:\n" + destino.getAbsolutePath());
        else GestorAlertas.error("Error", "No se pudo generar el Excel");
    }

    @FXML
    public void onLogout() {
        servicioAuth.logout();
        Aplicacion.navegarA("Login");
    }

    private void cargarDatos() {
        List<Empleado> lista = servicioEmpleado.listarActivos();
        empleados.setAll(lista);
        cmbEmpleadoInforme.setItems(FXCollections.observableArrayList(lista));
        actualizarEstadoFichaje();
    }

    private void actualizarEstadoFichaje() {
        lblBienvenidaRRHH.setText("Bienvenido, " + GestorSesion.getNombreCompleto());
        boolean fichado = servicioFichaje.estaFichadoHoy();
        if (fichado) {
            btnFicharRRHH.setText("Salida");
            btnFicharRRHH.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            lblEstadoRRHH.setText("Actualmente trabajando");
            lblEstadoRRHH.setStyle("-fx-text-fill: #e53935; -fx-font-size: 16px; -fx-font-weight: bold;");
        } else {
            btnFicharRRHH.setText("Entrada");
            btnFicharRRHH.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
            lblEstadoRRHH.setText("No fichado");
        }
    }

    private void actualizarBotonFichaje(boolean fichado) {
        if (fichado) {
            btnFicharRRHH.setText("Salida");
            btnFicharRRHH.setStyle("-fx-text-fill: #e53935; -fx-font-size: 16px; -fx-font-weight: bold;");

            Optional<Fichaje> f = servicioFichaje.getFichajeHoy();
            f.ifPresent(fi -> {
                String hora = fi.getEntradaHora() != null ? fi.getEntradaHora().format(FMT_HORA) : "--:--";
                lblEstadoRRHH.setText("Trabajando desde las " + hora);
                lblEstadoRRHH.setTextFill(Color.web("#2e7d32"));
            });
        } else {
            lblEstadoRRHH.setText("Entrada");
            lblEstadoRRHH.setStyle("-fx-text-fill: #43a047; -fx-font-size: 16px; -fx-font-weight: bold;");
            lblEstadoRRHH.setText("No estas fichado hoy.");
            lblEstadoRRHH.setTextFill(Color.web("#757575"));
        }
    }

    private void configurarTablaEmpleados() {
        colCod.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("codEmpleado"));
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombreCompleto()));
        colDni.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDni()));
        colRol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRol().getEtiqueta()));
        colDep.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDepartamento() != null ? cd.getValue().getDepartamento().getNombre() : ""));
        colEstado.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().isActivo() ? "Activo" : "Baja"));

        tablaEmpleados.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Empleado e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) setStyle("");
                else if (!e.isActivo()) setStyle("-fx-background-color: #ffebee;");
                else if (e.isBloqueado()) setStyle("-fx-background-color: #fff3e0;");
                else setStyle("");
            }
        });

        tablaEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevo) -> {
            if (nuevo != null) cargarEnFormulario(nuevo);
        });

        tablaEmpleados.setItems(empleados);
        tablaEmpleados.setPlaceholder(new Label("No hay empleados registrados."));
        tablaEmpleados.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void configurarFormulario() {
        cmbFormRol.setItems(FXCollections.observableArrayList(Rol.values()));
        List<Empleado> empleadoConDepartamentos = servicioEmpleado.listarTodos();
        List<Departamento> departamentos = new ArrayList<>();
        for (Empleado e : empleadoConDepartamentos) {
            Departamento d = e.getDepartamento();
            if (d != null && d.getNombre() != null && !d.getNombre().isBlank()) {
                departamentos.add(d);
            }
        }
        Set<Departamento> conjuntoDepartamentos = new HashSet<>(departamentos);
        conjuntoDepartamentos.removeIf(d -> d.getNombre().isBlank());
        List<Departamento> listaFinal = new ArrayList<>(conjuntoDepartamentos);
        cmbFormDpto.setItems(FXCollections.observableArrayList(listaFinal));
        habilitarFormulario(false);
    }

    private void configurarInformes() {
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
    }

    private void cargarEnFormulario(Empleado e) {
        modoEdicion = true;
        txtFormCod.setText(String.valueOf(e.getCodEmpleado()));
        txtFormCod.setDisable(true);
        txtFormNombre.setText(e.getNombre());
        txtFormApellido1.setText(e.getApellido1());
        txtFormApellido2.setText(e.getApellido2() != null ? e.getApellido2() : "");
        txtFormDni.setText(e.getDni());
        txtFormEmail.setText(e.getEmail() != null ? e.getEmail() : "");
        txtFormTelefono.setText(e.getTelefono() != null ? e.getTelefono() : "");
        cmbFormDpto.setText(e.getDepartamento() != null ? e.getDepartamento().getNombre() : "");
        txtFormUsername.setText(e.getUsername());
        txtFormContrasena.clear();
        cmbFormRol.setValue(e.getRol());
        lblFormMensaje.setVisible(false);
        habilitarFormulario(true);
    }

    private Empleado recogerFormulario() {
        if (txtFormCod.getText().isBlank() || txtFormNombre.getText().isBlank() || txtFormApellido1.getText().isBlank() || txtFormDni.getText().isBlank() || txtFormUsername.getText().isBlank()) {
            mostrarMensajeFormulario("Rellena todos los campos obligatorios.", false);
            return null;
        }
        try {
            Empleado e = new Empleado();
            e.setCodEmpleado(Integer.parseInt(txtFormCod.getText().trim()));
            e.setNombre(txtFormNombre.getText().trim());
            e.setApellido1(txtFormApellido1.getText().trim());
            e.setApellido2(txtFormApellido2.getText().trim());
            e.setDni(txtFormDni.getText().trim().toUpperCase());
            e.setEmail(txtFormEmail.getText().trim());
            e.setTelefono(txtFormTelefono.getText().trim());
            e.setDepartamento(cmbFormDpto.getValue() != null ? cmbFormDpto.getValue() : new Departamento("",""));
            e.setUsername(txtFormUsername.getText().trim().toLowerCase());
            e.setRol(cmbFormRol.getValue() != null ? cmbFormRol.getValue() : Rol.EMPLEADO);
            return e;
        } catch (NumberFormatException ex) {
            mostrarMensajeFormulario("El codigo debe ser un numero entero.", false);
            return null;
        }
    }

    private void limpiarFormulario() {
        txtFormCod.clear();
        txtFormNombre.clear();
        txtFormApellido1.clear();
        txtFormApellido2.clear();
        txtFormDni.clear();
        txtFormEmail.clear();
        txtFormTelefono.clear();
        cmbFormDpto.clear();
        txtFormUsername.clear();
        txtFormContrasena.clear();
        cmbFormRol.setValue(Rol.EMPLEADO);
        tablaEmpleados.getSelectionModel().clearSelection();
    }

    private void habilitarFormulario(boolean habilitar) {
        txtFormNombre.setDisable(!habilitar);
        txtFormApellido1.setDisable(!habilitar);
        txtFormApellido2.setDisable(!habilitar);
        txtFormDni.setDisable(!habilitar);
        txtFormEmail.setDisable(!habilitar);
        txtFormTelefono.setDisable(!habilitar);
        cmbFormDpto.setDisable(!habilitar);
        txtFormUsername.setDisable(!habilitar);
        txtFormContrasena.setDisable(!habilitar);
        cmbFormRol.setDisable(!habilitar);
        btnGuardar.setDisable(!habilitar);
        btnBaja.setDisable(!habilitar);
    }

    private void actualizarBotonesAccion() {
        btnBaja.setDisable(true);
    }

    private void mostrarMensajeFormulario(String msg, boolean exito) {
        lblFormMensaje.setText(msg);
        lblFormMensaje.setStyle(exito ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #c62828;");
        lblFormMensaje.setVisible(true);
    }
}

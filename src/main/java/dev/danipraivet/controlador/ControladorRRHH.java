package dev.danipraivet.controlador;

import dev.danipraivet.modelo.servicio.ServicioInformes;
import dev.danipraivet.vista.Aplicacion;
import dev.danipraivet.vista.utilidades.GestorAlertas;
import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.enumeraciones.Rol;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

// Controlador de la vista RRHH.fxml.
// Permite fichar, gestionar empleados (CRUD) y generar informes.
public class ControladorRRHH implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ControladorRRHH.class);
    private final ServicioInformes servicioInformes = new ServicioInformes();

    // Pestaña Inicio
    @FXML
    private Label lblBienvenidaRRHH;
    @FXML
    private Label lblEstadoRRHH;
    @FXML
    private Button btnFicharRRHH;

    // Pestaña Empleados - tabla
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

    // Pestaña Empleados - formulario lateral
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
    private PasswordField txtFormContrasena;
    @FXML
    private ComboBox<Rol> cmbFormRol;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnNuevo;
    @FXML
    private Button btnBaja;
    @FXML
    private Label lblFormMensaje;

    // Pestaña Informes
    @FXML
    private DatePicker dpDesde;
    @FXML
    private DatePicker dpHasta;
    @FXML
    private ComboBox<Empleado> cmbEmpleadoInforme;
    @FXML
    private Button btnGenerarPdf;
    @FXML
    private Button btnGenerarExcel;

    private final ServicioFichaje servicioFichaje = new ServicioFichaje();
    private final ServicioEmpleado servicioEmpleado = new ServicioEmpleado();
    private final ServicioAutenticacion servicioAuth = new ServicioAutenticacion();

    private final ObservableList<Empleado> empleados = FXCollections.observableArrayList();
    private FilteredList<Empleado> empleadosFiltrados;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablaEmpleados();
        configurarFiltro();
        configurarFormulario();
        configurarInformes();
        cargarDatos();
        actualizarBotonesAccion();
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
        boolean confirmar = GestorAlertas.confirmar(
                "Dar de baja",
                "¿Seguro que quieres dar de baja a " + seleccionado.getNombreCompleto() + "?\n" +
                        "Su historial de fichajes se conservara."
        );
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

        var fichajes = empleado != null
                ? servicioFichaje.getHistorial(empleado.getCodEmpleado(), desde, hasta)
                : servicioFichaje.getTodosConEmpleado(desde, hasta);

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

        var fichajes = empleado != null
                ? servicioFichaje.getHistorial(empleado.getCodEmpleado(), desde, hasta)
                : servicioFichaje.getTodosConEmpleado(desde, hasta);
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
            btnFicharRRHH.setStyle("-fx-background-color: #e53935; -fx-text-fill: white;");
            lblEstadoRRHH.setText("Actualmente trabajando");
        } else {
            btnFicharRRHH.setText("Entrada");
            btnFicharRRHH.setStyle("-fx-background-color: #43a047; -fx-text-fill: white;");
            lblEstadoRRHH.setText("No fichado");
        }
    }

    private void configurarTablaEmpleados() {
        colCod.setCellValueFactory(new PropertyValueFactory<>("codEmpleado"));

        colNombre.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNombreCompleto()));

        colDni.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDni()));

        colRol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRol().getEtiqueta()));

        colDep.setCellValueFactory(cellData -> {
            Empleado e = cellData.getValue();
            String dep = e.getDepartamento() != null ? e.getDepartamento().getNombre() : "";
            return new SimpleStringProperty(dep);
        });

        colEstado.setCellValueFactory(cellData -> {
            boolean activo = cellData.getValue().isActivo();
            return new SimpleStringProperty(activo ? "Activo" : "Baja");
        });

        // Colorear filas segun estado
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

        // Al seleccionar un empleado, cargarlo en el formulario
        tablaEmpleados.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nuevo) -> {
                    if (nuevo != null) cargarEnFormulario(nuevo);
                }
        );

        tablaEmpleados.setPlaceholder(new Label("No hay empleados registrados."));
    }

    private void configurarFiltro() {
        empleadosFiltrados = new FilteredList<>(empleados, e -> true);
        tablaEmpleados.setItems(empleadosFiltrados);

        txtBuscar.textProperty().addListener((obs, old, texto) -> {
            empleadosFiltrados.setPredicate(e -> {
                if (texto == null || texto.isBlank()) return true;
                String filtro = texto.toLowerCase();
                return e.getNombreCompleto().toLowerCase().contains(filtro)
                        || e.getDni().toLowerCase().contains(filtro)
                        || e.getUsername().toLowerCase().contains(filtro);
            });
        });
    }

    private void configurarFormulario() {
        cmbFormRol.setItems(FXCollections.observableArrayList(Rol.values()));
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
        txtFormUsername.setText(e.getUsername());
        txtFormContrasena.clear();
        cmbFormRol.setValue(e.getRol());
        lblFormMensaje.setVisible(false);
        habilitarFormulario(true);
    }

    private Empleado recogerFormulario() {
        if (txtFormCod.getText().isBlank() || txtFormNombre.getText().isBlank()
                || txtFormApellido1.getText().isBlank() || txtFormDni.getText().isBlank()
                || txtFormUsername.getText().isBlank()) {
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

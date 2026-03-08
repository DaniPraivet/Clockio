package dev.danipraivet.controlador;

import dev.danipraivet.vista.Aplicacion;
import dev.danipraivet.modelo.entidades.Fichaje;
import dev.danipraivet.modelo.servicio.ServicioAutenticacion;
import dev.danipraivet.modelo.servicio.ServicioFichaje;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

// Controlador de la vista Empleado.fxml.
// Muestra el estado de fichaje del dia y el historial del mes actual.
public class ControladorEmpleado implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ControladorEmpleado.class);

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private Label lblBienvenida;
    @FXML
    private Label lblFechaHora;
    @FXML
    private Label lblEstadoFichaje;
    @FXML
    private Button btnFichar;

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
    private TableColumn<Fichaje, String> colEstado;

    @FXML
    private Label lblTotalHorasMes;

    private final ServicioFichaje servicioFichaje = new ServicioFichaje();
    private final ServicioAutenticacion servicioAuth = new ServicioAutenticacion();

    private final ObservableList<Fichaje> fichajes = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabla();
        actualizarVista();
    }

    @FXML
    public void onFichar() {
        String mensaje = servicioFichaje.fichar();
        log.info("Fichaje realizado: {}", mensaje);
        actualizarVista();
        mostrarAlertaFichaje(mensaje);
    }

    @FXML
    public void onLogout() {
        servicioAuth.logout();
        Aplicacion.navegarA("Login");
    }

    // Refresca todos los componentes de la vista con el estado actual del empleado.
    private void actualizarVista() {
        lblBienvenida.setText("Bienvenido, " + GestorSesion.getNombreCompleto());
        lblFechaHora.setText(LocalDate.now().format(FMT_FECHA));

        boolean fichado = servicioFichaje.estaFichadoHoy();
        actualizarBotonFichaje(fichado);
        cargarHistorial();
    }

    private void actualizarBotonFichaje(boolean fichado) {
        if (fichado) {
            btnFichar.setText("Salida");
            btnFichar.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

            Optional<Fichaje> f = servicioFichaje.getFichajeHoy();
            f.ifPresent(fi -> {
                String hora = fi.getEntradaHora() != null ? fi.getEntradaHora().format(FMT_HORA) : "--:--";
                lblEstadoFichaje.setText("Trabajando desde las " + hora);
                lblEstadoFichaje.setTextFill(Color.web("#2e7d32"));
            });
        } else {
            btnFichar.setText("Entrada");
            btnFichar.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            lblEstadoFichaje.setText("No estas fichado hoy.");
            lblEstadoFichaje.setTextFill(Color.web("#757575"));
        }
    }

    private void cargarHistorial() {
        List<Fichaje> lista = servicioFichaje.getMesActual();
        fichajes.setAll(lista);

        double totalHoras = lista.stream()
                .filter(f -> f.getHorasTrabajadas() != null)
                .mapToDouble(f -> f.getHorasTrabajadas().doubleValue())
                .sum();
        lblTotalHorasMes.setText(String.format("Total mes: %.1f h", totalHoras));
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(FMT_FECHA));
            }
        });

        colEntrada.setCellValueFactory(cellData -> {
            Fichaje f = cellData.getValue();
            String texto = f.getEntradaHora() != null ? f.getEntradaHora().format(FMT_HORA) : "--:--";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });

        colSalida.setCellValueFactory(cellData -> {
            Fichaje f = cellData.getValue();
            String texto = f.getSalidaHora() != null ? f.getSalidaHora().format(FMT_HORA) : "--:--";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });

        colTurno.setCellValueFactory(cellData -> {
            Fichaje f = cellData.getValue();
            String turno = f.getTurnoEntrada() != null ? f.getTurnoEntrada().getEtiqueta() : "";
            return new javafx.beans.property.SimpleStringProperty(turno);
        });

        colHoras.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getHorasFormateadas())
        );

        colEstado.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado())
        );

        // Colorear fila segun estado del fichaje
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
        tablaHistorial.setPlaceholder(new Label("No hay fichajes registrados este mes."));
    }

    private void mostrarAlertaFichaje(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fichaje");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

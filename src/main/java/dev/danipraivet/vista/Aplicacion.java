package dev.danipraivet.vista;

import dev.danipraivet.modelo.datos.GestorConexiones;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

// Punto de entrada JavaFX. Gestiona la navegacion entre vistas y el ciclo de vida de la app.
public class Aplicacion extends Application {

    private static final Logger log = LoggerFactory.getLogger(Aplicacion.class);

    private static Stage escenarioPrincipal;

    private static final double ANCHO  = 1100;
    private static final double ALTO   = 700;

    @Override
    public void start(Stage stage) {
        escenarioPrincipal = stage;
        escenarioPrincipal.setTitle("Control de Asistencia");
        escenarioPrincipal.setMinWidth(900);
        escenarioPrincipal.setMinHeight(600);

        if (!GestorConexiones.testConexion()) {
            log.error("No se pudo conectar a la base de datos. Comprueba la configuracion.");
            mostrarErrorConexion();
            return;
        }

        navegarA("Login");
        escenarioPrincipal.show();
        log.info("Aplicacion iniciada correctamente");
    }

    @Override
    public void stop() {
        log.info("Cerrando aplicacion y liberando conexiones...");
        GestorConexiones.cerrarPool();
    }

    // Carga un FXML por nombre y lo establece como escena activa.
    public static void navegarA(String vista) {
        try {
            String ruta = "/dev/danipraivet/" + vista + ".fxml";
            URL fxmlUrl = Aplicacion.class.getResource(ruta);
            Objects.requireNonNull(fxmlUrl, "No se encontro el FXML en: " + ruta);

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene escena = new Scene(root, ANCHO, ALTO);
            aplicarCSS(escena);

            escenarioPrincipal.setScene(escena);
            escenarioPrincipal.setTitle("Control de Asistencia - " + vista);
            log.info("Navegando a vista: {}", vista);

        } catch (IOException | NullPointerException e) {
            log.error("Error al cargar la vista '{}': {}", vista, e.getMessage());
        }
    }

    // Sobrecarga que devuelve el controlador instanciado, util para pasar datos entre vistas.
    public static <T> T navegarA(String vista, Class<T> claseControlador) {
        try {
            String ruta = "/dev/danipraivet/" + vista + ".fxml";
            URL fxmlUrl = Aplicacion.class.getResource(ruta);
            Objects.requireNonNull(fxmlUrl, "No se encontro el FXML en: " + ruta);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            T controlador = loader.getController();

            Scene escena = new Scene(root, ANCHO, ALTO);
            aplicarCSS(escena);

            escenarioPrincipal.setScene(escena);
            escenarioPrincipal.setTitle("Control de Asistencia - " + vista);
            log.info("Navegando a vista: {} (con controlador)", vista);
            return controlador;

        } catch (IOException | NullPointerException e) {
            log.error("Error al cargar la vista '{}': {}", vista, e.getMessage());
            return null;
        }
    }

    public static Stage getEscenarioPrincipal() {
        return escenarioPrincipal;
    }

    private static void aplicarCSS(Scene escena) {
        URL cssUrl = Aplicacion.class.getResource("/styles.css");
        if (cssUrl != null) {
            escena.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            log.warn("No se encontro styles.css");
        }
    }

    private void mostrarErrorConexion() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Error de conexion");
        alert.setHeaderText("No se puede conectar a la base de datos");
        alert.setContentText(
            "Comprueba que:\n" +
            "  - MySQL esta en ejecucion\n" +
            "  - Las credenciales en ConfiguracionBD.java son correctas\n" +
            "  - El script SQL ha sido ejecutado en Workbench"
        );
        alert.showAndWait();
        escenarioPrincipal.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

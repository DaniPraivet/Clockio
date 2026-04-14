package dev.danipraivet.vista;

import dev.danipraivet.controlador.ControladorAdmin;
import dev.danipraivet.modelo.datos.GestorConexiones;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Aplicacion extends Application {

    private static final Logger log = LoggerFactory.getLogger(Aplicacion.class);
    private static final double ANCHO = 1100;
    private static final double ALTO = 700;
    private static Stage escenarioPrincipal;
    private static ScheduledExecutorService scheduler;

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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        escenarioPrincipal = stage;
        escenarioPrincipal.setTitle("Control de Asistencia");
        escenarioPrincipal.setMinWidth(900);
        escenarioPrincipal.setMinHeight(600);
        escenarioPrincipal.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/dev/danipraivet/logo/logo_negro_app.png"))));

        UserAgentBuilder.builder().themes(JavaFXThemes.MODENA).themes(MaterialFXStylesheets.forAssemble(true)).setDeploy(true).setResolveAssets(true).build().setGlobal();

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
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void mostrarErrorConexion() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de conexion");
        alert.setHeaderText("No se puede conectar a la base de datos");
        alert.setContentText("Comprueba que:\n" + "  - MySQL esta en ejecucion\n" + "  - Las credenciales en ConfiguracionBD.java son correctas\n" + "  - El script SQL ha sido ejecutado en Workbench");
        alert.showAndWait();
        escenarioPrincipal.show();
    }

    public static void iniciarScheduler(Runnable tarea, int intervaloSegundos) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(tarea, intervaloSegundos, intervaloSegundos, TimeUnit.SECONDS);
    }
}

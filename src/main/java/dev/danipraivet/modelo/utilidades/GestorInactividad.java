package dev.danipraivet.modelo.utilidades;


import dev.danipraivet.vista.Aplicacion;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.InputEvent;
import javafx.util.Duration;

public class GestorInactividad {
    private static final int TIEMPO_TOTAL = 180;
    private static final int TIEMPO_AVISO = 30;

    private static Timeline timeline;
    private static Alert alertaActual;

    public static void iniciar(Scene escena) {
        escena.addEventFilter(InputEvent.ANY, e -> resetear());
        programar();
    }

    public static void resetear() {
        if (alertaActual != null && alertaActual.isShowing()) {
            Platform.runLater(() -> alertaActual.close());
        }
        programar();
    }

    private static void programar() {
        detener();

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(TIEMPO_AVISO), e -> mostrarAviso()),
                new KeyFrame(Duration.seconds(TIEMPO_TOTAL), e -> cerrarSesion())
        );
        timeline.setCycleCount(1);
        timeline.play();
    }

    private static void mostrarAviso() {
        Platform.runLater(() -> {
            alertaActual = new Alert(Alert.AlertType.WARNING);
            alertaActual.setTitle("Inactividad");
            alertaActual.setHeaderText("Tu sesión va a expirar");
            alertaActual.setContentText("Mueve el ratón o pulsa una tecla para continuar.");
            alertaActual.show();
        });
    }

    private static void cerrarSesion() {
        if (!GestorSesion.haySesionActiva()) return;

        Platform.runLater(() -> {
            if (alertaActual != null) alertaActual.close();
            detener();
            GestorSesion.cerrarSesion();
            Aplicacion.pararScheduler();
            Aplicacion.navegarA("Login");
        });
    }

    public static void detener() {
        if (timeline != null) timeline.stop();
    }
}

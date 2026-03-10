package dev.danipraivet.vista.utilidades;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

// Utilidad para mostrar dialogos JavaFX de forma rapida y consistente.
public final class GestorAlertas {

    private GestorAlertas() {
    }

    public static void info(String titulo, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    public static void advertencia(String titulo, String mensaje) {
        mostrar(Alert.AlertType.WARNING, titulo, mensaje);
    }

    public static void error(String titulo, String mensaje) {
        mostrar(Alert.AlertType.ERROR, titulo, mensaje);
    }

    // Muestra un dialogo de confirmacion (Si / No) y devuelve la eleccion del usuario.
    public static boolean confirmar(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.YES;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

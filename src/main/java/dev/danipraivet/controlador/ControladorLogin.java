package dev.danipraivet.controlador;

import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.servicio.ServicioAutenticacion;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import dev.danipraivet.vista.Aplicacion;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class ControladorLogin implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ControladorLogin.class);
    private final ServicioAutenticacion servicioAuth = new ServicioAutenticacion();
    @FXML
    private TextField txtUsername;
    @FXML
    private MFXPasswordField txtContrasena;
    @FXML
    private MFXButton btnLogin;
    @FXML
    private Label lblError;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setVisible(false);

        // Mover foco al siguiente campo con Enter
        txtUsername.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) txtContrasena.requestFocus();
        });
        txtContrasena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onLogin();
        });

        // Limpiar el mensaje de error cuando el usuario empieza a escribir
        txtUsername.textProperty().addListener((o, old, nuevo) -> limpiarError());
        txtContrasena.textProperty().addListener((o, old, nuevo) -> limpiarError());

        Platform.runLater(() -> txtUsername.requestFocus());
    }

    @FXML
    public void onLogin() {
        String username = txtUsername.getText().trim();
        String contrasena = txtContrasena.getText();

        if (username.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Por favor, introduce usuario y contrasena.");
            return;
        }

        btnLogin.setDisable(true);
        lblError.setVisible(false);

        ServicioAutenticacion.ResultadoLogin resultado = servicioAuth.login(username, contrasena);

        if (resultado == ServicioAutenticacion.ResultadoLogin.EXITO) {
            navegarSegunRol();
        } else {
            mostrarError(servicioAuth.getMensajeError(resultado));
            txtContrasena.clear();
            txtContrasena.requestFocus();
            btnLogin.setDisable(false);
        }
    }

    private void navegarSegunRol() {
        Rol rol = GestorSesion.getRol();
        log.info("Redirigiendo a vista para rol: {}", rol);
        switch (rol) {
            case EMPLEADO -> Aplicacion.navegarA("Empleado");
            case RRHH -> Aplicacion.navegarA("RRHH");
            case ADMIN -> Aplicacion.navegarA("Admin");
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }

    private void limpiarError() {
        lblError.setVisible(false);
    }
}

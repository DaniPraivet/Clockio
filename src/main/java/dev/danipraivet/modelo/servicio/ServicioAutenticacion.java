package dev.danipraivet.modelo.servicio;

import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.repositorio.contratos.IRepositorioEmpleado;
import dev.danipraivet.modelo.repositorio.implementacion.RepositorioEmpleado;
import dev.danipraivet.modelo.seguridad.HashContrasena;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

// Logica de autenticacion: busca el empleado, verifica contrasena y gestiona la sesion.
public class ServicioAutenticacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacion.class);

    // El login usa el rol con minimos privilegios para consultar la BD
    private final IRepositorioEmpleado repo = new RepositorioEmpleado(Rol.EMPLEADO);

    public ResultadoLogin login(String username, String contrasena) {
        if (username == null || username.isBlank() || contrasena == null || contrasena.isBlank()) {
            return ResultadoLogin.CREDENCIALES_INCORRECTAS;
        }

        try {
            Optional<Empleado> opt = repo.buscarPorUsername(username.trim().toLowerCase());
            if (opt.isEmpty()) {
                log.warn("Login fallido - usuario no encontrado: '{}'", username);
                return ResultadoLogin.CREDENCIALES_INCORRECTAS;
            }

            Empleado empleado = opt.get();

            if (!empleado.isActivo()) {
                log.warn("Login denegado - cuenta inactiva: '{}'", username);
                return ResultadoLogin.CUENTA_INACTIVA;
            }

            if (empleado.isBloqueado()) {
                log.warn("Login denegado - cuenta bloqueada: '{}'", username);
                return ResultadoLogin.CUENTA_BLOQUEADA;
            }

            if (!HashContrasena.verificar(contrasena, empleado.getPasswordHash())) {
                repo.registrarIntentoFallido(username);
                log.warn("Login fallido - contrasena incorrecta: '{}' (intentos: {})", username, empleado.getIntentosFallidos() + 1);
                return ResultadoLogin.CREDENCIALES_INCORRECTAS;
            }

            repo.registrarLoginExitoso(username);
            GestorSesion.iniciarSesion(empleado);
            log.info("Login exitoso - usuario: '{}', rol: {}", username, empleado.getRol());
            return ResultadoLogin.EXITO;

        } catch (Exception e) {
            log.error("Error inesperado en login: {}", e.getMessage(), e);
            return ResultadoLogin.ERROR_SISTEMA;
        }
    }

    public void logout() {
        if (GestorSesion.haySesionActiva()) {
            log.info("Logout - usuario: '{}'", GestorSesion.getEmpleado().getUsername());
        }
        GestorSesion.cerrarSesion();
    }

    // Devuelve el mensaje de error legible para la vista segun el resultado del login
    public String getMensajeError(ResultadoLogin resultado) {
        return switch (resultado) {
            case CREDENCIALES_INCORRECTAS -> "Usuario o contrasena incorrectos.";
            case CUENTA_BLOQUEADA -> "Cuenta bloqueada. Contacta con RRHH.";
            case CUENTA_INACTIVA -> "Esta cuenta no esta activa.";
            case ERROR_SISTEMA -> "Error del sistema. Intentalo de nuevo.";
            case EXITO -> "";
        };
    }

    // Posibles resultados del intento de login
    public enum ResultadoLogin {
        EXITO, CREDENCIALES_INCORRECTAS, CUENTA_BLOQUEADA, CUENTA_INACTIVA, ERROR_SISTEMA
    }
}

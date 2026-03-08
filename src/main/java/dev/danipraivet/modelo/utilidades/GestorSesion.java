package dev.danipraivet.modelo.utilidades;

import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.enumeraciones.Rol;

// Gestiona la sesion del usuario autenticado. Solo puede haber una sesion activa a la vez.
public final class GestorSesion {

    private static Empleado empleadoActual = null;

    private GestorSesion() {}

    public static void iniciarSesion(Empleado empleado) {
        empleadoActual = empleado;
    }

    public static void cerrarSesion() {
        empleadoActual = null;
    }

    public static boolean haySesionActiva() {
        return empleadoActual != null;
    }

    public static Empleado getEmpleado() {
        return empleadoActual;
    }

    public static int getCodEmpleado() {
        verificarSesion();
        return empleadoActual.getCodEmpleado();
    }

    public static Rol getRol() {
        verificarSesion();
        return empleadoActual.getRol();
    }

    public static String getNombreCompleto() {
        verificarSesion();
        return empleadoActual.getNombreCompleto();
    }

    public static boolean esEmpleado() {
        return haySesionActiva() && empleadoActual.getRol() == Rol.EMPLEADO;
    }

    public static boolean esRRHH() {
        return haySesionActiva() && empleadoActual.getRol() == Rol.RRHH;
    }

    public static boolean esAdmin() {
        return haySesionActiva() && empleadoActual.getRol() == Rol.ADMIN;
    }

    // RRHH y ADMIN tienen permisos de gestion de empleados
    public static boolean tienePermisoGestion() {
        return esRRHH() || esAdmin();
    }

    private static void verificarSesion() {
        if (empleadoActual == null) {
            throw new IllegalStateException("No hay sesion activa. El usuario debe autenticarse primero.");
        }
    }
}

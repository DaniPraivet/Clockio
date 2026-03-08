package dev.danipraivet.modelo.servicio;

import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.repositorio.implementacion.RepositorioEmpleado;
import dev.danipraivet.modelo.repositorio.contratos.IRepositorioEmpleado;
import dev.danipraivet.modelo.seguridad.HashContrasena;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import dev.danipraivet.modelo.utilidades.ValidadorFormularios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

// Logica de negocio para la gestion de empleados. Usado desde ControladorRRHH y ControladorAdmin.
public class ServicioEmpleado {

    private static final Logger log = LoggerFactory.getLogger(ServicioEmpleado.class);

    private final IRepositorioEmpleado repo;

    public ServicioEmpleado() {
        Rol rol = GestorSesion.haySesionActiva() ? GestorSesion.getRol() : Rol.RRHH;
        this.repo = new RepositorioEmpleado(rol);
    }

    // Resultado de una operacion CRUD con exito/fallo y mensaje descriptivo
    public record ResultadoCRUD(boolean exito, String mensaje) {
        public static ResultadoCRUD ok(String msg)    { return new ResultadoCRUD(true,  msg); }
        public static ResultadoCRUD error(String msg) { return new ResultadoCRUD(false, msg); }
    }

    public List<Empleado> listarActivos() {
        return repo.listarTodos();
    }

    public List<Empleado> listarTodos() {
        return repo.listarTodosIncluyendoBajas();
    }

    public Optional<Empleado> buscarPorCodigo(int cod) {
        return repo.buscarPorCodigo(cod);
    }

    // Valida los datos y crea un nuevo empleado. Hashea la contrasena antes de persistir.
    public ResultadoCRUD crear(Empleado empleado, String contrasenaPlana) {
        String errDni = ValidadorFormularios.mensajeDni(empleado.getDni());
        if (errDni != null) return ResultadoCRUD.error("DNI invalido: " + errDni);

        if (!ValidadorFormularios.usernameValido(empleado.getUsername()))
            return ResultadoCRUD.error("Username invalido. Min. 4 chars, solo letras, numeros, puntos y guiones.");

        String errPass = ValidadorFormularios.mensajeContrasena(contrasenaPlana);
        if (errPass != null) return ResultadoCRUD.error("Contrasena: " + errPass);

        if (empleado.getEmail() != null && !empleado.getEmail().isBlank()
                && !ValidadorFormularios.emailValido(empleado.getEmail()))
            return ResultadoCRUD.error("Email con formato invalido.");

        if (repo.existeDni(empleado.getDni()))
            return ResultadoCRUD.error("Ya existe un empleado con ese DNI.");

        if (repo.existeUsername(empleado.getUsername()))
            return ResultadoCRUD.error("El username ya esta en uso.");

        empleado.setPasswordHash(HashContrasena.hashear(contrasenaPlana));

        boolean ok = repo.insertar(empleado);
        return ok
            ? ResultadoCRUD.ok("Empleado creado correctamente.")
            : ResultadoCRUD.error("Error al guardar en la base de datos.");
    }

    public ResultadoCRUD actualizar(Empleado empleado) {
        if (!ValidadorFormularios.dniValido(empleado.getDni()))
            return ResultadoCRUD.error("DNI invalido.");

        boolean ok = repo.actualizar(empleado);
        return ok
            ? ResultadoCRUD.ok("Datos actualizados correctamente.")
            : ResultadoCRUD.error("Error al actualizar en la base de datos.");
    }

    public ResultadoCRUD cambiarContrasena(int codEmpleado, String nuevaContrasenaPlana) {
        String err = ValidadorFormularios.mensajeContrasena(nuevaContrasenaPlana);
        if (err != null) return ResultadoCRUD.error(err);

        Optional<Empleado> opt = repo.buscarPorCodigo(codEmpleado);
        if (opt.isEmpty()) return ResultadoCRUD.error("Empleado no encontrado.");

        Empleado e = opt.get();
        e.setPasswordHash(HashContrasena.hashear(nuevaContrasenaPlana));
        boolean ok = repo.actualizar(e);
        return ok
            ? ResultadoCRUD.ok("Contrasena actualizada correctamente.")
            : ResultadoCRUD.error("Error al actualizar la contrasena.");
    }

    // Baja logica: desactiva el empleado sin borrar su historial de fichajes
    public ResultadoCRUD darDeBaja(int codEmpleado) {
        if (GestorSesion.getCodEmpleado() == codEmpleado)
            return ResultadoCRUD.error("No puedes darte de baja a ti mismo.");

        boolean ok = repo.darDeBaja(codEmpleado);
        return ok
            ? ResultadoCRUD.ok("Empleado dado de baja correctamente.")
            : ResultadoCRUD.error("Error al dar de baja al empleado.");
    }

    // Eliminacion fisica (solo Admin). Borra tambien todos sus fichajes en cascada.
    public ResultadoCRUD eliminar(int codEmpleado) {
        if (GestorSesion.getCodEmpleado() == codEmpleado)
            return ResultadoCRUD.error("No puedes eliminarte a ti mismo.");

        boolean ok = repo.eliminar(codEmpleado);
        return ok
            ? ResultadoCRUD.ok("Empleado eliminado definitivamente.")
            : ResultadoCRUD.error("Error al eliminar el empleado.");
    }

    public ResultadoCRUD desbloquear(int codEmpleado) {
        Optional<Empleado> opt = repo.buscarPorCodigo(codEmpleado);
        if (opt.isEmpty()) return ResultadoCRUD.error("Empleado no encontrado.");

        Empleado e = opt.get();
        e.setBloqueado(false);
        e.setIntentosFallidos(0);
        boolean ok = repo.actualizar(e);
        return ok
            ? ResultadoCRUD.ok("Cuenta desbloqueada.")
            : ResultadoCRUD.error("Error al desbloquear la cuenta.");
    }
}

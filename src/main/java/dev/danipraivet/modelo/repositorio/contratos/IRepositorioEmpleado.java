package dev.danipraivet.modelo.repositorio.contratos;

import dev.danipraivet.modelo.entidades.Empleado;

import java.util.List;
import java.util.Optional;

// Contrato de acceso a datos para empleados. Desacopla la logica de negocio de los detalles JDBC.
public interface IRepositorioEmpleado {

    Optional<Empleado> buscarPorCodigo(int codEmpleado);

    Optional<Empleado> buscarPorUsername(String username);

    Optional<Empleado> buscarPorDni(String dni);

    List<Empleado> listarTodos();

    List<Empleado> listarTodosIncluyendoBajas();

    boolean insertar(Empleado empleado);

    boolean actualizar(Empleado empleado);

    // Baja logica: desactiva el empleado sin borrar sus datos ni fichajes
    boolean darDeBaja(int codEmpleado);

    // Eliminacion fisica (solo ADMIN, con confirmacion previa)
    boolean eliminar(int codEmpleado);

    // Incrementa el contador de intentos fallidos. Bloquea la cuenta si llega a 5.
    boolean registrarIntentoFallido(String username);

    // Resetea intentos fallidos y actualiza la fecha de ultimo acceso
    boolean registrarLoginExitoso(String username);

    boolean existeUsername(String username);

    boolean existeDni(String dni);
}

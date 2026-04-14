package dev.danipraivet.modelo.repositorio.contratos;

import dev.danipraivet.modelo.entidades.Fichaje;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Contrato de acceso a datos para fichajes.
public interface IRepositorioFichaje {

    // Registra entrada o salida llamando al stored procedure de MySQL
    String registrarFichaje(int codEmpleado);

    Optional<Fichaje> buscarFichajeHoy(int codEmpleado);

    List<Fichaje> buscarPorEmpleadoYRango(int codEmpleado, LocalDate desde, LocalDate hasta);

    List<Fichaje> buscarPorFecha(LocalDate fecha);

    // Lista fichajes con datos del empleado incluidos (JOIN)
    List<Fichaje> listarTodosConEmpleado(LocalDate desde, LocalDate hasta);

    boolean actualizar(Fichaje fichaje);

    boolean eliminar(int id);

    // Comprueba si el empleado tiene un fichaje abierto hoy (sin salida registrada)
    boolean estaFichadoHoy(int codEmpleado);
    int contarFichadosHoy();
}

package dev.danipraivet.modelo.servicio;

import dev.danipraivet.modelo.entidades.Fichaje;
import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.repositorio.contratos.IRepositorioFichaje;
import dev.danipraivet.modelo.repositorio.implementacion.RepositorioFichaje;
import dev.danipraivet.modelo.utilidades.GestorSesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Logica de negocio para los fichajes. Usa el rol de la sesion para conectar con los permisos correctos.
public class ServicioFichaje {

    private static final Logger log = LoggerFactory.getLogger(ServicioFichaje.class);

    private final IRepositorioFichaje repo;

    public ServicioFichaje() {
        Rol rol = GestorSesion.haySesionActiva() ? GestorSesion.getRol() : Rol.EMPLEADO;
        this.repo = new RepositorioFichaje(rol);
    }

    // Registra la entrada o salida del empleado en sesion. Devuelve el mensaje del stored procedure.
    public String fichar() {
        int cod = GestorSesion.getCodEmpleado();
        String mensaje = repo.registrarFichaje(cod);
        log.info("Fichaje - empleado {} - {}", cod, mensaje);
        return mensaje;
    }

    // Registra el fichaje de un empleado especifico (uso de Admin/RRHH)
    public String ficharPor(int codEmpleado) {
        return repo.registrarFichaje(codEmpleado);
    }

    // Devuelve true si el empleado en sesion tiene una entrada abierta hoy
    public boolean estaFichadoHoy() {
        return repo.estaFichadoHoy(GestorSesion.getCodEmpleado());
    }

    public Optional<Fichaje> getFichajeHoy() {
        return repo.buscarFichajeHoy(GestorSesion.getCodEmpleado());
    }

    public Optional<Fichaje> getFichajeHoyDe(int codEmpleado) {
        return repo.buscarFichajeHoy(codEmpleado);
    }

    // Historial del empleado en sesion para el mes actual
    public List<Fichaje> getMesActual() {
        LocalDate hoy = LocalDate.now();
        return repo.buscarPorEmpleadoYRango(GestorSesion.getCodEmpleado(), hoy.withDayOfMonth(1), hoy);
    }

    public List<Fichaje> getHistorial(int codEmpleado, LocalDate desde, LocalDate hasta) {
        return repo.buscarPorEmpleadoYRango(codEmpleado, desde, hasta);
    }

    public List<Fichaje> getFichajesPorFecha(LocalDate fecha) {
        return repo.buscarPorFecha(fecha);
    }

    public List<Fichaje> getTodosConEmpleado(LocalDate desde, LocalDate hasta) {
        return repo.listarTodosConEmpleado(desde, hasta);
    }

    public boolean actualizar(Fichaje fichaje) {
        return repo.actualizar(fichaje);
    }

    public boolean eliminar(int id) {
        return repo.eliminar(id);
    }
}

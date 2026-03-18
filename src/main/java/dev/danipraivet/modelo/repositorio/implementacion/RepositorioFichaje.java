package dev.danipraivet.modelo.repositorio.implementacion;

import dev.danipraivet.modelo.datos.GestorConexiones;
import dev.danipraivet.modelo.entidades.Fichaje;
import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.enumeraciones.Turno;
import dev.danipraivet.modelo.repositorio.contratos.IRepositorioFichaje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Implementacion JDBC del repositorio de fichajes.
public class RepositorioFichaje implements IRepositorioFichaje {

    private static final Logger log = LoggerFactory.getLogger(RepositorioFichaje.class);
    private static final String SQL_BASE = "SELECT d.id, d.fecha, d.cod_empleado, " + "       d.entrada_hora, d.salida_hora, " + "       d.turno_entrada, d.turno_salida, " + "       d.horas_trabajadas, d.horas_extras, " + "       d.festivo, d.justificado, d.observaciones, " + "       d.creado_en, d.modificado_en " + "FROM dias d ";
    private static final String SQL_HOY = SQL_BASE + "WHERE d.cod_empleado=? AND d.fecha=CURRENT_DATE LIMIT 1";
    private static final String SQL_POR_EMPLEADO_RANGO = SQL_BASE + "WHERE d.cod_empleado=? AND d.fecha BETWEEN ? AND ? ORDER BY d.fecha DESC";
    private static final String SQL_POR_FECHA = SQL_BASE + "WHERE d.fecha=? ORDER BY d.entrada_hora";
    private static final String SQL_TODOS_CON_EMPLEADO = "SELECT d.id, d.fecha, d.cod_empleado, " + "       CONCAT(e.nombre,' ',e.apellido1) AS nombre_completo, " + "       e.rol, " + "       d.entrada_hora, d.salida_hora, " + "       d.turno_entrada, d.turno_salida, " + "       d.horas_trabajadas, d.horas_extras, " + "       d.festivo, d.justificado, d.observaciones, " + "       d.creado_en, d.modificado_en " + "FROM dias d " + "JOIN empleados e ON d.cod_empleado = e.cod_empleado " + "WHERE d.fecha BETWEEN ? AND ? ORDER BY d.fecha DESC, e.apellido1";
    private static final String SQL_ACTUALIZAR = "UPDATE dias SET entrada_hora=?, salida_hora=?, turno_entrada=?, turno_salida=?, " + "horas_trabajadas=?, horas_extras=?, festivo=?, justificado=?, observaciones=? " + "WHERE id=?";
    private static final String SQL_ELIMINAR = "DELETE FROM dias WHERE id=?";
    private static final String SQL_ESTA_FICHADO = "SELECT COUNT(*) FROM dias WHERE cod_empleado=? AND fecha=CURRENT_DATE AND salida_hora IS NULL";
    // Stored procedure de MySQL para registrar fichaje
    private static final String SQL_REGISTRAR_FICHAJE = "CALL registrar_fichaje(?, @p_mensaje, @p_tipo)";
    private static final String SQL_GET_OUT_PARAMS = "SELECT @p_mensaje AS mensaje, @p_tipo AS tipo";
    private final Rol rolConexion;
    public RepositorioFichaje(Rol rolConexion) {
        this.rolConexion = rolConexion;
    }

    @Override
    public String registrarFichaje(int codEmpleado) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);

            // Llamar al stored procedure con parametros OUT de MySQL
            PreparedStatement ps = con.prepareStatement(SQL_REGISTRAR_FICHAJE);
            ps.setInt(1, codEmpleado);
            ps.execute();

            // Recuperar los parametros OUT mediante variables de sesion
            ResultSet rs = con.createStatement().executeQuery(SQL_GET_OUT_PARAMS);
            if (rs.next()) {
                String mensaje = rs.getString("mensaje");
                String tipo = rs.getString("tipo");
                log.info("Fichaje registrado - empleado: {} - {}", codEmpleado, tipo);
                return mensaje;
            }
        } catch (SQLException e) {
            log.error("Error registrando fichaje para empleado {}: {}", codEmpleado, e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return "Error al registrar el fichaje";
    }

    @Override
    public Optional<Fichaje> buscarFichajeHoy(int codEmpleado) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_HOY);
            ps.setInt(1, codEmpleado);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            log.error("Error buscando fichaje de hoy para empleado {}: {}", codEmpleado, e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return Optional.empty();
    }

    @Override
    public List<Fichaje> buscarPorEmpleadoYRango(int codEmpleado, LocalDate desde, LocalDate hasta) {
        List<Fichaje> lista = new ArrayList<>();
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_POR_EMPLEADO_RANGO);
            ps.setInt(1, codEmpleado);
            ps.setDate(2, Date.valueOf(desde));
            ps.setDate(3, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error buscando fichajes por rango: {}", e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return lista;
    }

    @Override
    public List<Fichaje> buscarPorFecha(LocalDate fecha) {
        List<Fichaje> lista = new ArrayList<>();
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_POR_FECHA);
            ps.setDate(1, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error buscando fichajes por fecha: {}", e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return lista;
    }

    @Override
    public List<Fichaje> listarTodosConEmpleado(LocalDate desde, LocalDate hasta) {
        List<Fichaje> lista = new ArrayList<>();
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_TODOS_CON_EMPLEADO);
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error listando fichajes con empleado: {}", e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return lista;
    }

    @Override
    public boolean actualizar(Fichaje f) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR);
            ps.setTime(1, f.getEntradaHora() != null ? Time.valueOf(f.getEntradaHora()) : null);
            ps.setTime(2, f.getSalidaHora() != null ? Time.valueOf(f.getSalidaHora()) : null);
            ps.setString(3, f.getTurnoEntrada() != null ? f.getTurnoEntrada().getEtiqueta() : null);
            ps.setString(4, f.getTurnoSalida() != null ? f.getTurnoSalida().getEtiqueta() : null);
            ps.setBigDecimal(5, f.getHorasTrabajadas());
            ps.setBigDecimal(6, f.getHorasExtras());
            ps.setBoolean(7, f.isFestivo());
            ps.setBoolean(8, f.isJustificado());
            ps.setString(9, f.getObservaciones());
            ps.setInt(10, f.getId());
            boolean ok = ps.executeUpdate() > 0;
            log.info("Fichaje actualizado ID: {}", f.getId());
            return ok;
        } catch (SQLException e) {
            log.error("Error actualizando fichaje {}: {}", f.getId(), e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    @Override
    public boolean eliminar(int id) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_ELIMINAR);
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            log.warn("Fichaje eliminado ID: {}", id);
            return ok;
        } catch (SQLException e) {
            log.error("Error eliminando fichaje {}: {}", id, e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    @Override
    public boolean estaFichadoHoy(int codEmpleado) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_ESTA_FICHADO);
            ps.setInt(1, codEmpleado);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            log.error("Error comprobando estado fichaje: {}", e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    private Fichaje mapear(ResultSet rs) throws SQLException {
        Fichaje f = new Fichaje();
        f.setId(rs.getInt("id"));
        f.setCodEmpleado(rs.getInt("cod_empleado"));

        Date fecha = rs.getDate("fecha");
        if (fecha != null) f.setFecha(fecha.toLocalDate());

        Time entrada = rs.getTime("entrada_hora");
        if (entrada != null) f.setEntradaHora(entrada.toLocalTime());

        Time salida = rs.getTime("salida_hora");
        if (salida != null) f.setSalidaHora(salida.toLocalTime());

        f.setTurnoEntrada(Turno.fromString(rs.getString("turno_entrada")));
        f.setTurnoSalida(Turno.fromString(rs.getString("turno_salida")));

        BigDecimal horasTrab = rs.getBigDecimal("horas_trabajadas");
        f.setHorasTrabajadas(horasTrab != null ? horasTrab : BigDecimal.ZERO);

        BigDecimal horasExtra = rs.getBigDecimal("horas_extras");
        f.setHorasExtras(horasExtra != null ? horasExtra : BigDecimal.ZERO);

        f.setFestivo(rs.getBoolean("festivo"));
        f.setJustificado(rs.getBoolean("justificado"));
        f.setObservaciones(rs.getString("observaciones"));

        Timestamp creadoEn = rs.getTimestamp("creado_en");
        if (creadoEn != null) f.setCreadoEn(creadoEn.toLocalDateTime());

        Timestamp modEn = rs.getTimestamp("modificado_en");
        if (modEn != null) f.setModificadoEn(modEn.toLocalDateTime());

        return f;
    }
}

package dev.danipraivet.modelo.repositorio.implementacion;

import dev.danipraivet.modelo.datos.GestorConexiones;
import dev.danipraivet.modelo.entidades.Departamento;
import dev.danipraivet.modelo.entidades.Empleado;
import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.repositorio.contratos.IRepositorioEmpleado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Implementacion JDBC del repositorio de empleados.
// Cada metodo obtiene conexion del pool, ejecuta con PreparedStatement y la libera al terminar.
public class RepositorioEmpleado implements IRepositorioEmpleado {

    private static final Logger log = LoggerFactory.getLogger(RepositorioEmpleado.class);
    // SELECT base con JOIN a departamentos
    private static final String SQL_BASE = "SELECT e.cod_empleado, e.nombre, e.apellido1, e.apellido2, e.dni, " + "       e.email, e.telefono, e.username, e.password_hash, e.rol, " + "       e.activo, e.intentos_fallidos, e.bloqueado, " + "       e.fecha_alta, e.fecha_baja, e.ultimo_acceso, " + "       d.cod_departamento, d.nombre AS dep_nombre " + "FROM empleados e " + "LEFT JOIN departamentos d ON e.cod_departamento = d.cod_departamento ";
    private static final String SQL_POR_CODIGO = SQL_BASE + "WHERE e.cod_empleado = ?";
    private static final String SQL_POR_USERNAME = SQL_BASE + "WHERE e.username = ?";
    private static final String SQL_POR_DNI = SQL_BASE + "WHERE e.dni = ?";
    private static final String SQL_TODOS_ACTIVOS = SQL_BASE + "WHERE e.activo = TRUE ORDER BY e.apellido1, e.nombre";
    private static final String SQL_TODOS = SQL_BASE + "ORDER BY e.activo DESC, e.apellido1";
    private static final String SQL_INSERTAR = "INSERT INTO empleados (cod_empleado, nombre, apellido1, apellido2, dni, " + "email, telefono, username, password_hash, rol, cod_departamento) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR = "UPDATE empleados SET nombre=?, apellido1=?, apellido2=?, dni=?, " + "email=?, telefono=?, username=?, rol=?, cod_departamento=? " + "WHERE cod_empleado=?";
    private static final String SQL_BAJA_LOGICA = "UPDATE empleados SET activo=FALSE, fecha_baja=NOW() WHERE cod_empleado=?";
    private static final String SQL_AUDITAR_BAJA = "INSERT INTO auditoria (cod_empleado, username, accion, detalle, tabla_afectada, registro_id) " + "VALUES (?, ?, 'BAJA_LOGICA', ?, 'empleados', ?)";
    private static final String SQL_ELIMINAR = "DELETE FROM empleados WHERE cod_empleado=?";
    private static final String SQL_INTENTO_FALLIDO = "UPDATE empleados SET intentos_fallidos = intentos_fallidos + 1, " + "bloqueado = (intentos_fallidos + 1 >= 5) WHERE username=?";
    private static final String SQL_LOGIN_EXITOSO = "UPDATE empleados SET intentos_fallidos=0, ultimo_acceso=NOW() WHERE username=?";
    private static final String SQL_EXISTE_USERNAME = "SELECT COUNT(*) FROM empleados WHERE username=?";
    private static final String SQL_EXISTE_DNI = "SELECT COUNT(*) FROM empleados WHERE dni=?";
    private final Rol rolConexion;
    public RepositorioEmpleado(Rol rolConexion) {
        this.rolConexion = rolConexion;
    }

    @Override
    public Optional<Empleado> buscarPorCodigo(int codEmpleado) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_POR_CODIGO);
            ps.setInt(1, codEmpleado);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            log.error("Error buscando empleado por codigo {}: {}", codEmpleado, e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Empleado> buscarPorUsername(String username) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_POR_USERNAME);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            log.error("Error buscando empleado por username '{}': {}", username, e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Empleado> buscarPorDni(String dni) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_POR_DNI);
            ps.setString(1, dni.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            log.error("Error buscando empleado por DNI: {}", e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return Optional.empty();
    }

    @Override
    public List<Empleado> listarTodos() {
        return ejecutarListado(SQL_TODOS_ACTIVOS);
    }

    @Override
    public List<Empleado> listarTodosIncluyendoBajas() {
        return ejecutarListado(SQL_TODOS);
    }

    @Override
    public boolean insertar(Empleado e) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_INSERTAR);
            ps.setInt(1, e.getCodEmpleado());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getApellido1());
            ps.setString(4, e.getApellido2());
            ps.setString(5, e.getDni().toUpperCase());
            ps.setString(6, e.getEmail());
            ps.setString(7, e.getTelefono());
            ps.setString(8, e.getUsername());
            ps.setString(9, e.getPasswordHash());
            ps.setString(10, e.getRol().name());
            if (e.getDepartamento() != null) {
                ps.setInt(11, e.getDepartamento().getCodDepartamento());
            } else {
                ps.setNull(11, Types.INTEGER);
            }
            boolean ok = ps.executeUpdate() > 0;
            log.info("Empleado insertado: {} ({})", e.getNombreCompleto(), e.getCodEmpleado());
            return ok;
        } catch (SQLException ex) {
            log.error("Error insertando empleado: {}", ex.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    @Override
    public boolean actualizar(Empleado e) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR);
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getApellido1());
            ps.setString(3, e.getApellido2());
            ps.setString(4, e.getDni().toUpperCase());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getTelefono());
            ps.setString(7, e.getUsername());
            ps.setString(8, e.getRol().name());
            if (e.getDepartamento() != null) {
                ps.setInt(9, e.getDepartamento().getCodDepartamento());
            } else {
                ps.setNull(9, Types.INTEGER);
            }
            ps.setInt(10, e.getCodEmpleado());
            boolean ok = ps.executeUpdate() > 0;
            log.info("Empleado actualizado: {}", e.getCodEmpleado());
            return ok;
        } catch (SQLException ex) {
            log.error("Error actualizando empleado: {}", ex.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    /**
     * Baja logica de un empleado con transaccion explicita.
     * Las dos operaciones se ejecutan
     * de forma atomica, si alguna falla, se deshace todo con rollback.
     */
    @Override
    public boolean darDeBaja(int codEmpleado) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            con.setAutoCommit(false);

            // 1 desactivar al empleado
            PreparedStatement psBaja = con.prepareStatement(SQL_BAJA_LOGICA);
            psBaja.setInt(1, codEmpleado);
            int filasAfectadas = psBaja.executeUpdate();

            if (filasAfectadas == 0) {
                con.rollback();
                log.warn("Baja denegada: empleado {} no encontrado. Rollback aplicado.", codEmpleado);
                return false;
            }

            // 2 registrar la baja en la tabla de auditoria
            String detalle = String.format("Baja logica del empleado cod=%d registrada por el sistema", codEmpleado);
            String usuarioSesion = dev.danipraivet.modelo.utilidades.GestorSesion.haySesionActiva() ? dev.danipraivet.modelo.utilidades.GestorSesion.getEmpleado().getUsername() : "sistema";

            PreparedStatement psAudit = con.prepareStatement(SQL_AUDITAR_BAJA);
            psAudit.setInt(1, codEmpleado);
            psAudit.setString(2, usuarioSesion);
            psAudit.setString(3, detalle);
            psAudit.setInt(4, codEmpleado);
            psAudit.executeUpdate();

            con.commit();
            log.info("Baja logica completada con exito para empleado {}. Transaccion confirmada.", codEmpleado);
            return true;

        } catch (SQLException e) {
            log.error("Error en transaccion de baja para empleado {}: {}", codEmpleado, e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                    log.warn("Rollback aplicado tras error en baja del empleado {}.", codEmpleado);
                } catch (SQLException ex) {
                    log.error("Error al aplicar rollback: {}", ex.getMessage());
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    log.warn("Error al restaurar autoCommit: {}", e.getMessage());
                }
            }
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    @Override
    public boolean eliminar(int codEmpleado) {
        return ejecutarUpdate(SQL_ELIMINAR, codEmpleado);
    }

    @Override
    public boolean registrarIntentoFallido(String username) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(Rol.EMPLEADO);
            PreparedStatement ps = con.prepareStatement(SQL_INTENTO_FALLIDO);
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn("Error registrando intento fallido para '{}': {}", username, e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(Rol.EMPLEADO, con);
        }
    }

    @Override
    public boolean registrarLoginExitoso(String username) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(Rol.EMPLEADO);
            PreparedStatement ps = con.prepareStatement(SQL_LOGIN_EXITOSO);
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn("Error registrando login exitoso para '{}': {}", username, e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(Rol.EMPLEADO, con);
        }
    }

    @Override
    public boolean existeUsername(String username) {
        return existeCampo(SQL_EXISTE_USERNAME, username);
    }

    @Override
    public boolean existeDni(String dni) {
        return existeCampo(SQL_EXISTE_DNI, dni.toUpperCase());
    }

    // Mapea una fila del ResultSet a un objeto Empleado
    private Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setCodEmpleado(rs.getInt("cod_empleado"));
        e.setNombre(rs.getString("nombre"));
        e.setApellido1(rs.getString("apellido1"));
        e.setApellido2(rs.getString("apellido2"));
        e.setDni(rs.getString("dni"));
        e.setEmail(rs.getString("email"));
        e.setTelefono(rs.getString("telefono"));
        e.setUsername(rs.getString("username"));
        e.setPasswordHash(rs.getString("password_hash"));
        e.setRol(Rol.fromString(rs.getString("rol")));
        e.setActivo(rs.getBoolean("activo"));
        e.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        e.setBloqueado(rs.getBoolean("bloqueado"));

        Timestamp fechaAlta = rs.getTimestamp("fecha_alta");
        if (fechaAlta != null) e.setFechaAlta(fechaAlta.toLocalDateTime());

        Timestamp fechaBaja = rs.getTimestamp("fecha_baja");
        if (fechaBaja != null) e.setFechaBaja(fechaBaja.toLocalDateTime());

        Timestamp ultimoAcceso = rs.getTimestamp("ultimo_acceso");
        if (ultimoAcceso != null) e.setUltimoAcceso(ultimoAcceso.toLocalDateTime());

        // Departamento puede ser null si el LEFT JOIN no encontro coincidencia
        int codDep = rs.getInt("cod_departamento");
        if (!rs.wasNull()) {
            Departamento dep = new Departamento();
            dep.setCodDepartamento(codDep);
            dep.setNombre(rs.getString("dep_nombre"));
            e.setDepartamento(dep);
        }
        return e;
    }

    private List<Empleado> ejecutarListado(String sql) {
        List<Empleado> lista = new ArrayList<>();
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error listando empleados: {}", e.getMessage());
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
        return lista;
    }

    private boolean ejecutarUpdate(String sql, int parametro) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, parametro);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en update ({}): {}", sql, e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }

    private boolean existeCampo(String sql, String valor) {
        Connection con = null;
        try {
            con = GestorConexiones.getConexion(rolConexion);
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            log.error("Error comprobando existencia: {}", e.getMessage());
            return false;
        } finally {
            GestorConexiones.liberarConexion(rolConexion, con);
        }
    }
}
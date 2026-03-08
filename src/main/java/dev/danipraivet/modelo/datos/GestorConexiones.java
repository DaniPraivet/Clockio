package dev.danipraivet.modelo.datos;

import dev.danipraivet.modelo.enumeraciones.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

// Pool de conexiones a MySQL organizado por rol. Patron Singleton + Pool por rol.
// Uso: try (Connection con = GestorConexiones.getConexion(Rol.EMPLEADO)) { ... }
public class GestorConexiones {

    private static final Logger log = LoggerFactory.getLogger(GestorConexiones.class);

    // Mapa de rol a cola de conexiones disponibles
    private static final Map<Rol, Deque<Connection>> pools = new EnumMap<>(Rol.class);

    // Credenciales por rol (usuario MySQL)
    private static final Map<Rol, String[]> CREDENCIALES = new EnumMap<>(Rol.class);

    static {
        CREDENCIALES.put(Rol.EMPLEADO, new String[]{ ConfiguracionBD.EMPLEADO_USER, ConfiguracionBD.EMPLEADO_PASS });
        CREDENCIALES.put(Rol.RRHH,     new String[]{ ConfiguracionBD.RRHH_USER,     ConfiguracionBD.RRHH_PASS     });
        CREDENCIALES.put(Rol.ADMIN,    new String[]{ ConfiguracionBD.ADMIN_USER,     ConfiguracionBD.ADMIN_PASS    });

        for (Rol rol : Rol.values()) {
            pools.put(rol, new ArrayDeque<>());
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            log.info("Driver MySQL registrado correctamente");
        } catch (ClassNotFoundException e) {
            log.error("No se encontro el driver MySQL. Verifica las dependencias.", e);
        }
    }

    private GestorConexiones() {}

    // Devuelve una conexion del pool. Si esta vacia, crea una nueva.
    public static synchronized Connection getConexion(Rol rol) throws SQLException {
        Deque<Connection> pool = pools.get(rol);

        while (!pool.isEmpty()) {
            Connection con = pool.pollFirst();
            try {
                if (con != null && !con.isClosed() && con.isValid(2)) {
                    log.debug("Reutilizando conexion del pool [{}] - disponibles: {}", rol, pool.size());
                    return con;
                }
            } catch (SQLException e) {
                log.warn("Conexion invalida descartada del pool [{}]", rol);
            }
        }

        log.debug("Creando nueva conexion para rol [{}]", rol);
        return crearConexion(rol);
    }

    // Devuelve una conexion al pool para reutilizarla.
    public static synchronized void liberarConexion(Rol rol, Connection con) {
        if (con == null) return;
        try {
            if (!con.isClosed()) {
                Deque<Connection> pool = pools.get(rol);
                if (pool.size() < ConfiguracionBD.POOL_SIZE_MAX) {
                    pool.addFirst(con);
                    log.debug("Conexion devuelta al pool [{}] - disponibles: {}", rol, pool.size());
                } else {
                    con.close();
                    log.debug("Pool lleno, conexion cerrada [{}]", rol);
                }
            }
        } catch (SQLException e) {
            log.warn("Error al liberar conexion [{}]: {}", rol, e.getMessage());
        }
    }

    // Verifica si la BD es accesible al arrancar la app.
    public static boolean testConexion() {
        try (Connection con = crearConexion(Rol.EMPLEADO)) {
            boolean ok = con != null && con.isValid(5);
            log.info(ok ? "Conexion a la BD verificada" : "BD inaccesible");
            return ok;
        } catch (SQLException e) {
            log.error("Error al verificar la BD: {}", e.getMessage());
            return false;
        }
    }

    // Cierra todas las conexiones de todos los pools. Llamar en App.stop().
    public static synchronized void cerrarPool() {
        int total = 0;
        for (Map.Entry<Rol, Deque<Connection>> entry : pools.entrySet()) {
            Deque<Connection> pool = entry.getValue();
            for (Connection con : pool) {
                try { con.close(); total++; } catch (SQLException ignored) {}
            }
            pool.clear();
        }
        log.info("Pool cerrado. {} conexiones liberadas.", total);
    }

    private static Connection crearConexion(Rol rol) throws SQLException {
        String[] creds = CREDENCIALES.get(rol);
        if (creds == null) throw new SQLException("Rol no reconocido: " + rol);
        return DriverManager.getConnection(ConfiguracionBD.URL, creds[0], creds[1]);
    }
}

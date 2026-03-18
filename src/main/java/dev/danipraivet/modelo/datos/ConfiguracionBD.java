package dev.danipraivet.modelo.datos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuracion centralizada de la base de datos.
 * Los parametros se cargan desde el fichero externo {@code db.properties},
 * ubicado en el classpath ({@code src/main/resources/db.properties}).
 * Esto permite cambiar el entorno (local, pruebas, produccion) sin recompilar.
 */
public final class ConfiguracionBD {

    public static final String HOST;
    public static final String PORT;
    public static final String DATABASE;
    public static final String URL;
    public static final String EMPLEADO_USER;
    public static final String EMPLEADO_PASS;
    public static final String RRHH_USER;
    public static final String RRHH_PASS;
    public static final String ADMIN_USER;
    public static final String ADMIN_PASS;
    public static final int POOL_SIZE_MIN;
    public static final int POOL_SIZE_MAX;
    public static final int TIMEOUT_SEGUNDOS;
    private static final Logger log = LoggerFactory.getLogger(ConfiguracionBD.class);
    private static final String FICHERO_CONFIG = "db.properties";

    // Carga las propiedades al arrancar la clase (bloque estatico).
    static {
        Properties props = cargarPropiedades();

        HOST = props.getProperty("db.host", "127.0.0.1");
        PORT = props.getProperty("db.port", "3306");
        DATABASE = props.getProperty("db.name", "control_asistencia");

        String ssl = props.getProperty("db.useSSL", "false");
        String timezone = props.getProperty("db.serverTimezone", "Europe/Madrid");
        String encoding = props.getProperty("db.characterEncoding", "UTF-8");
        String publicKey = props.getProperty("db.allowPublicKeyRetrieval", "true");

        URL = String.format("jdbc:mysql://%s:%s/%s?useSSL=%s&serverTimezone=%s&characterEncoding=%s&allowPublicKeyRetrieval=%s", HOST, PORT, DATABASE, ssl, timezone, encoding, publicKey);

        EMPLEADO_USER = props.getProperty("db.empleado.user", "empleado");
        EMPLEADO_PASS = props.getProperty("db.empleado.pass", "");

        RRHH_USER = props.getProperty("db.rrhh.user", "rrhh");
        RRHH_PASS = props.getProperty("db.rrhh.pass", "");

        ADMIN_USER = props.getProperty("db.admin.user", "admin_app");
        ADMIN_PASS = props.getProperty("db.admin.pass", "");

        POOL_SIZE_MIN = Integer.parseInt(props.getProperty("db.pool.min", "2"));
        POOL_SIZE_MAX = Integer.parseInt(props.getProperty("db.pool.max", "10"));
        TIMEOUT_SEGUNDOS = Integer.parseInt(props.getProperty("db.pool.timeoutSegundos", "30"));

        log.info("Configuracion BD cargada desde '{}': {}:{}/{}", FICHERO_CONFIG, HOST, PORT, DATABASE);
    }

    /**
     * Lee el fichero db.properties desde el classpath.
     * Si no se encuentra, registra una advertencia y devuelve propiedades vacias
     * para que los valores por defecto definidos en cada getProperty() entren en juego.
     */
    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream is = ConfiguracionBD.class.getClassLoader().getResourceAsStream(FICHERO_CONFIG)) {

            if (is == null) {
                log.warn("No se encontro '{}' en el classpath. Se usaran valores por defecto.", FICHERO_CONFIG);
                return props;
            }
            props.load(is);

        } catch (IOException e) {
            log.error("Error al leer '{}': {}. Se usaran valores por defecto.", FICHERO_CONFIG, e.getMessage());
        }
        return props;
    }
}
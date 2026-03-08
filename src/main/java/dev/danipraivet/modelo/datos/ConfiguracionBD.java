package dev.danipraivet.modelo.datos;

// Configuracion centralizada de la base de datos.
// En produccion estas constantes deberian cargarse desde variables de entorno o fichero externo.
public final class ConfiguracionBD {

    private ConfiguracionBD() {}

    public static final String HOST     = "localhost";
    public static final String PORT     = "3306";
    public static final String DATABASE = "control_asistencia";

    // serverTimezone: evita problemas con TIMESTAMP en MySQL 8
    // characterEncoding: soporta tildes y enes
    // useSSL=false: desarrollo local sin certificados
    public static final String URL = String.format(
        "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Europe/Madrid&characterEncoding=UTF-8&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );

    // Cada rol usa su propio usuario MySQL con permisos distintos (minimo privilegio)
    public static final String EMPLEADO_USER = "empleado";
    public static final String EMPLEADO_PASS = "Empleado#2024!";

    public static final String RRHH_USER     = "rrhh";
    public static final String RRHH_PASS     = "RRHH#2024!";

    public static final String ADMIN_USER    = "admin_app";
    public static final String ADMIN_PASS    = "Admin#2024!";

    public static final int POOL_SIZE_MIN    = 2;   // Conexiones minimas abiertas
    public static final int POOL_SIZE_MAX    = 10;  // Conexiones maximas simultaneas
    public static final int TIMEOUT_SEGUNDOS = 30;  // Timeout de obtencion de conexion
}

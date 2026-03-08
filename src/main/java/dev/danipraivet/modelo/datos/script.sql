-- ============================================================
--  CONTROL DE ASISTENCIA - Script SQL Completo
--  TFG - Aplicación de Gestión de Asistencia Empresarial
--  Versión mejorada con seguridad, auditoría y datos de prueba
-- ============================================================

-- ============================================================
-- 1. CREACIÓN DE LA BASE DE DATOS
-- ============================================================
DROP
DATABASE IF EXISTS control_asistencia;
CREATE
DATABASE control_asistencia
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE
control_asistencia;

-- ============================================================
-- 2. TABLAS PRINCIPALES
-- ============================================================

-- -------------------------------------------------------  -----
-- 2.1 Tabla de departamentos
--     Permite agrupar empleados por área de la empresa
-- ------------------------------------------------------------
CREATE TABLE departamentos
(
    cod_departamento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    descripcion      VARCHAR(255) NULL,
    activo           BOOLEAN   DEFAULT TRUE,
    fecha_creacion   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2.2 Tabla de empleados (mejorada)
--     Incluye autenticación, rol y datos ampliados
-- ------------------------------------------------------------
CREATE TABLE empleados
(
    cod_empleado      INT UNSIGNED PRIMARY KEY,
    nombre            VARCHAR(50)        NOT NULL,
    apellido1         VARCHAR(50)        NOT NULL,
    apellido2         VARCHAR(50) NULL,
    dni               VARCHAR(9) UNIQUE  NOT NULL,
    email             VARCHAR(100) UNIQUE NULL,
    telefono          VARCHAR(15) NULL,
    -- Autenticación
    username          VARCHAR(50) UNIQUE NOT NULL,
    password_hash     VARCHAR(255)       NOT NULL, -- BCrypt desde Java
    rol               ENUM('EMPLEADO', 'RRHH', 'ADMIN')
                      NOT NULL DEFAULT 'EMPLEADO',
    -- Control de estado
    activo            BOOLEAN   DEFAULT TRUE,
    intentos_fallidos TINYINT UNSIGNED DEFAULT 0,  -- Bloqueo tras N intentos
    bloqueado         BOOLEAN   DEFAULT FALSE,
    -- Timestamps
    fecha_alta        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_baja        TIMESTAMP NULL,
    ultimo_acceso     TIMESTAMP NULL,
    -- Relación departamento
    cod_departamento  INT UNSIGNED NULL,
    CONSTRAINT FK_empleados_departamento
        FOREIGN KEY (cod_departamento)
            REFERENCES departamentos (cod_departamento)
            ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- 2.3 Tabla de días / fichajes (mejorada)
--     Registro completo de entradas y salidas
-- ------------------------------------------------------------
CREATE TABLE dias
(
    id               INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fecha            DATE NOT NULL DEFAULT (CURRENT_DATE), -- Solo fecha
    cod_empleado     INT UNSIGNED NOT NULL,
    -- Horario
    entrada_hora     TIME NULL,
    salida_hora      TIME NULL,
    turno_entrada    ENUM('Mañana', 'Tarde', 'Noche') NULL,
    turno_salida     ENUM('Mañana', 'Tarde', 'Noche') NULL,
    -- Cálculos de tiempo
    horas_trabajadas DECIMAL(5, 2) DEFAULT 0.00,
    horas_extras     DECIMAL(5, 2) DEFAULT 0.00,           -- > 8h diarias
    -- Flags
    festivo          BOOLEAN       DEFAULT FALSE,
    justificado      BOOLEAN       DEFAULT FALSE,          -- Ausencia justificada
    observaciones    VARCHAR(255) NULL,
    -- Timestamps de creación y modificación
    creado_en        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    modificado_en    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Restricción: un fichaje por día por empleado
    CONSTRAINT UK_fichaje_dia UNIQUE (cod_empleado, fecha),
    CONSTRAINT FK_dias_empleado
        FOREIGN KEY (cod_empleado)
            REFERENCES empleados (cod_empleado)
            ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 2.4 Tabla de auditoría
--     Registro de acciones críticas en el sistema
-- ------------------------------------------------------------
CREATE TABLE auditoria
(
    id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cod_empleado   INT UNSIGNED NULL, -- NULL si login fallido
    username       VARCHAR(50) NULL,  -- Para logins fallidos
    accion         VARCHAR(100) NOT NULL,
    detalle        TEXT NULL,
    tabla_afectada VARCHAR(50) NULL,
    registro_id    INT UNSIGNED NULL, -- ID del registro modificado
    ip             VARCHAR(45) NULL,  -- IPv4 e IPv6
    fecha          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_auditoria_empleado
        FOREIGN KEY (cod_empleado)
            REFERENCES empleados (cod_empleado)
            ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- 2.5 Tabla de festivos del año
--     Permite marcar días festivos automáticamente
-- ------------------------------------------------------------
CREATE TABLE festivos
(
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fecha       DATE UNIQUE  NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    ambito      ENUM('Nacional', 'Autonómico', 'Local') DEFAULT 'Nacional'
);

-- ============================================================
-- 3. ÍNDICES PARA RENDIMIENTO
-- ============================================================
CREATE INDEX idx_dias_empleado_fecha ON dias (cod_empleado, fecha);
CREATE INDEX idx_dias_fecha ON dias (fecha);
CREATE INDEX idx_auditoria_empleado ON auditoria (cod_empleado);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha);
CREATE INDEX idx_empleados_rol ON empleados (rol);
CREATE INDEX idx_empleados_username ON empleados (username);

-- ============================================================
-- 4. VISTAS ÚTILES
-- ============================================================

-- Vista completa de fichajes con datos del empleado
CREATE VIEW v_fichajes_completos AS
SELECT d.id,
       d.fecha,
       e.cod_empleado,
       CONCAT(e.nombre, ' ', e.apellido1,
              IF(e.apellido2 IS NOT NULL, CONCAT(' ', e.apellido2), '')) AS nombre_completo,
       e.dni,
       e.rol,
       dep.nombre                                                        AS departamento,
       d.entrada_hora,
       d.salida_hora,
       d.turno_entrada,
       d.turno_salida,
       d.horas_trabajadas,
       d.horas_extras,
       d.festivo,
       d.justificado,
       d.observaciones
FROM dias d
         JOIN empleados e ON d.cod_empleado = e.cod_empleado
         LEFT JOIN departamentos dep ON e.cod_departamento = dep.cod_departamento;

-- Vista de resumen mensual por empleado
CREATE VIEW v_resumen_mensual AS
SELECT e.cod_empleado,
       CONCAT(e.nombre, ' ', e.apellido1) AS nombre_completo,
        YEAR (d.fecha) AS anio,
        MONTH (d.fecha) AS mes,
        COUNT (d.id) AS dias_trabajados,
        SUM (d.horas_trabajadas) AS total_horas,
        SUM (d.horas_extras) AS total_extras,
        SUM (IF(d.festivo = TRUE, 1, 0)) AS festivos_trabajados
        FROM dias d
        JOIN empleados e ON d.cod_empleado = e.cod_empleado
        WHERE d.salida_hora IS NOT NULL
        GROUP BY e.cod_empleado, YEAR (d.fecha), MONTH (d.fecha);

-- Vista de empleados activos con estado actual (fichado o no)
CREATE VIEW v_estado_actual_empleados AS
SELECT e.cod_empleado,
       CONCAT(e.nombre, ' ', e.apellido1) AS nombre_completo,
       e.rol,
       e.ultimo_acceso,
       IF(d.entrada_hora IS NOT NULL AND d.salida_hora IS NULL,
          'FICHADO', 'NO FICHADO')        AS estado_hoy,
       d.entrada_hora                     AS entrada_hoy
FROM empleados e
         LEFT JOIN dias d
                   ON d.cod_empleado = e.cod_empleado
                       AND d.fecha = CURRENT_DATE
WHERE e.activo = TRUE;

-- ============================================================
-- 5. STORED PROCEDURES
-- ============================================================

DELIMITER
//

-- ------------------------------------------------------------
-- 5.1 Registrar fichaje (entrada / salida automática)
-- ------------------------------------------------------------
CREATE PROCEDURE registrar_fichaje(
    IN p_cod_empleado INT,
    OUT p_mensaje VARCHAR (100),
    OUT p_tipo VARCHAR (10) -- 'ENTRADA' o 'SALIDA'
)
BEGIN
    DECLARE
v_id_abierto INT;
    DECLARE
v_entrada_hora TIME;

    SET
time_zone = '+01:00';

    -- Buscar si hay un fichaje de HOY sin salida
SELECT id, entrada_hora
INTO v_id_abierto, v_entrada_hora
FROM dias
WHERE cod_empleado = p_cod_empleado
  AND fecha = CURRENT_DATE
  AND salida_hora IS NULL LIMIT 1;

IF
v_id_abierto IS NOT NULL THEN
        -- ► SALIDA: completar el registro existente
UPDATE dias
SET salida_hora = TIME (NOW()), turno_salida = CASE
    WHEN HOUR (NOW()) BETWEEN 6 AND 13 THEN 'Mañana'
    WHEN HOUR (NOW()) BETWEEN 14 AND 21 THEN 'Tarde'
    ELSE 'Noche'
END
,
               horas_trabajadas = ROUND(TIMESTAMPDIFF(MINUTE, v_entrada_hora, TIME(NOW())) / 60, 2),
               horas_extras     = GREATEST(
                                    ROUND(TIMESTAMPDIFF(MINUTE, v_entrada_hora, TIME(NOW())) / 60, 2) - 8,
                                    0
                                  ),
               festivo          = (DAYOFWEEK(CURRENT_DATE) IN (1, 7))   -- Dom=1, Sáb=7
         WHERE id = v_id_abierto;

        SET
p_mensaje = 'Salida registrada correctamente';
        SET
p_tipo    = 'SALIDA';
ELSE
        -- ► ENTRADA: insertar nuevo registro
        INSERT INTO dias (fecha, cod_empleado, entrada_hora, turno_entrada, festivo)
        VALUES (
            CURRENT_DATE,
            p_cod_empleado,
            TIME(NOW()),
            CASE
                WHEN HOUR(NOW()) BETWEEN 6  AND 13 THEN 'Mañana'
                WHEN HOUR(NOW()) BETWEEN 14 AND 21 THEN 'Tarde'
                ELSE 'Noche'
            END,
            (DAYOFWEEK(CURRENT_DATE) IN (1, 7))
        );

        SET
p_mensaje = 'Entrada registrada correctamente';
        SET
p_tipo    = 'ENTRADA';
END IF;
END
//

-- ------------------------------------------------------------
-- 5.2 Obtener resumen de un empleado en un rango de fechas
-- ------------------------------------------------------------
CREATE PROCEDURE obtener_resumen_empleado(
    IN p_cod_empleado INT,
    IN p_fecha_inicio DATE,
    IN p_fecha_fin DATE
)
BEGIN
SELECT fecha,
       entrada_hora,
       salida_hora,
       turno_entrada,
       turno_salida,
       horas_trabajadas,
       horas_extras,
       festivo,
       justificado,
       observaciones
FROM dias
WHERE cod_empleado = p_cod_empleado
  AND fecha BETWEEN p_fecha_inicio AND p_fecha_fin
ORDER BY fecha ASC;
END
//

-- ------------------------------------------------------------
-- 5.3 Bloquear usuario tras N intentos fallidos
-- ------------------------------------------------------------
CREATE PROCEDURE registrar_intento_fallido(
    IN p_username VARCHAR (50)
)
BEGIN
    DECLARE
v_intentos TINYINT;
    DECLARE
v_max_intentos TINYINT DEFAULT 5;

UPDATE empleados
SET intentos_fallidos = intentos_fallidos + 1
WHERE username = p_username;

SELECT intentos_fallidos
INTO v_intentos
FROM empleados
WHERE username = p_username;

IF
v_intentos >= v_max_intentos THEN
UPDATE empleados
SET bloqueado = TRUE
WHERE username = p_username;
END IF;
END
//

-- ------------------------------------------------------------
-- 5.4 Resetear intentos fallidos tras login exitoso
-- ------------------------------------------------------------
CREATE PROCEDURE login_exitoso(
    IN p_username VARCHAR (50)
)
BEGIN
UPDATE empleados
SET intentos_fallidos = 0,
    ultimo_acceso     = NOW()
WHERE username = p_username;
END
//

-- ------------------------------------------------------------
-- 5.5 Estadísticas globales para el dashboard de Admin
-- ------------------------------------------------------------
CREATE PROCEDURE estadisticas_dashboard()
BEGIN
    -- Total empleados activos
SELECT COUNT(*) AS total_empleados
FROM empleados
WHERE activo = TRUE;

-- Empleados fichados hoy
SELECT COUNT(*) AS fichados_hoy
FROM dias
WHERE fecha = CURRENT_DATE
  AND salida_hora IS NULL
  AND entrada_hora IS NOT NULL;

-- Horas totales trabajadas este mes
SELECT ROUND(SUM(horas_trabajadas), 2) AS horas_mes
FROM dias
WHERE MONTH (fecha) = MONTH (CURRENT_DATE)
  AND YEAR (fecha) = YEAR (CURRENT_DATE);

-- Top 5 empleados con más horas este mes
SELECT CONCAT(e.nombre, ' ', e.apellido1) AS empleado,
       ROUND(SUM(d.horas_trabajadas), 2)  AS horas
FROM dias d
         JOIN empleados e ON d.cod_empleado = e.cod_empleado
WHERE MONTH (d.fecha) = MONTH (CURRENT_DATE)
  AND YEAR (d.fecha) = YEAR (CURRENT_DATE)
GROUP BY e.cod_empleado
ORDER BY horas DESC
    LIMIT 5;
END
//

DELIMITER ;

-- ============================================================
-- 6. TRIGGERS
-- ============================================================

DELIMITER
//

-- Auditar modificaciones en la tabla días
CREATE TRIGGER trg_dias_after_update
    AFTER UPDATE
    ON dias
    FOR EACH ROW
BEGIN
    INSERT INTO auditoria (accion, tabla_afectada, registro_id, detalle)
    VALUES ('UPDATE',
            'dias',
            NEW.id,
            CONCAT('Fichaje actualizado - Empleado: ', NEW.cod_empleado,
                   ' | Salida: ', COALESCE(NEW.salida_hora, 'NULL'),
                   ' | Horas: ', NEW.horas_trabajadas));
END //

-- Auditar inserciones en la tabla días
CREATE TRIGGER trg_dias_after_insert
    AFTER INSERT
    ON dias
    FOR EACH ROW
BEGIN
    INSERT INTO auditoria (accion, tabla_afectada, registro_id, detalle)
    VALUES ('INSERT',
            'dias',
            NEW.id,
            CONCAT('Nuevo fichaje - Empleado: ', NEW.cod_empleado,
                   ' | Entrada: ', COALESCE(NEW.entrada_hora, 'NULL')));
END //

-- Auditar eliminaciones de empleados
CREATE TRIGGER trg_empleados_after_delete
    AFTER DELETE
    ON empleados
    FOR EACH ROW
BEGIN
    INSERT INTO auditoria (accion, tabla_afectada, detalle)
    VALUES ('DELETE',
            'empleados',
            CONCAT('Empleado eliminado: ', OLD.cod_empleado,
                   ' - ', OLD.nombre, ' ', OLD.apellido1,
                   ' (DNI: ', OLD.dni, ')'));
END //

DELIMITER;

-- ============================================================
-- 7. USUARIOS Y PERMISOS MySQL
-- ============================================================

-- Eliminar usuarios si ya existen (para re-ejecución limpia)
DROP
USER IF EXISTS 'empleado'@'%';
DROP
USER IF EXISTS 'rrhh'@'%';
DROP
USER IF EXISTS 'admin_app'@'%';   -- Renombrado para evitar conflicto con ADMIN MySQL
DROP
USER IF EXISTS 'empleado'@'%';
DROP
USER IF EXISTS 'rrhh'@'%';
DROP
USER IF EXISTS 'admin_app'@'%';

CREATE
USER 'empleado'@'%'  IDENTIFIED BY 'Empleado#2024!';
CREATE
USER 'rrhh'@'%'      IDENTIFIED BY 'RRHH#2024!';
CREATE
USER 'admin_app'@'%' IDENTIFIED BY 'Admin#2024!';

-- ► Permisos EMPLEADO (mínimos necesarios)
       GRANT
SELECT
ON control_asistencia.departamentos TO 'empleado'@'%';
GRANT
SELECT
ON control_asistencia.empleados TO 'empleado'@'%';
GRANT
SELECT,
INSERT
ON control_asistencia.dias TO 'empleado'@'%';
GRANT
INSERT
ON
control_asistencia
.
auditoria
TO
'empleado'@'%';
GRANT EXECUTE           ON PROCEDURE control_asistencia.registrar_fichaje
                        TO
'empleado'@'%';
GRANT EXECUTE           ON PROCEDURE control_asistencia.registrar_intento_fallido
                        TO
'empleado'@'%';
GRANT EXECUTE           ON PROCEDURE control_asistencia.login_exitoso
                        TO
'empleado'@'%';

-- ► Permisos RRHH (gestión de empleados + informes)
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON control_asistencia.empleados TO 'rrhh'@'%';
GRANT
SELECT,
INSERT
,
UPDATE
ON control_asistencia.dias TO 'rrhh'@'%';
GRANT
SELECT
ON control_asistencia.departamentos TO 'rrhh'@'%';
GRANT
SELECT
ON control_asistencia.festivos TO 'rrhh'@'%';
GRANT
SELECT
ON control_asistencia.v_fichajes_completos TO 'rrhh'@'%';
GRANT
SELECT
ON control_asistencia.v_resumen_mensual TO 'rrhh'@'%';
GRANT
INSERT
ON
control_asistencia
.
auditoria
TO
'rrhh'@'%';
GRANT EXECUTE ON PROCEDURE control_asistencia.registrar_fichaje           TO
'rrhh'@'%';
GRANT EXECUTE ON PROCEDURE control_asistencia.obtener_resumen_empleado    TO
'rrhh'@'%';
GRANT EXECUTE ON PROCEDURE control_asistencia.registrar_intento_fallido   TO
'rrhh'@'%';
GRANT EXECUTE ON PROCEDURE control_asistencia.login_exitoso               TO
'rrhh'@'%';

-- ► Permisos ADMIN (control total)
GRANT ALL PRIVILEGES ON control_asistencia.* TO
'admin_app'@'%' WITH GRANT OPTION;

FLUSH
PRIVILEGES;

-- ============================================================
-- 8. DATOS DE PRUEBA (seed data)
-- ============================================================

-- Departamentos
INSERT INTO departamentos (nombre, descripcion)
VALUES ('Tecnología', 'Departamento de desarrollo e infraestructura'),
       ('Recursos Humanos', 'Gestión de personal y nóminas'),
       ('Administración', 'Contabilidad y gestión administrativa'),
       ('Ventas', 'Equipo comercial y atención al cliente');

-- Festivos 2025 (España - nacionales)
INSERT INTO festivos (fecha, descripcion, ambito)
VALUES ('2025-01-01', 'Año Nuevo', 'Nacional'),
       ('2025-01-06', 'Reyes Magos', 'Nacional'),
       ('2025-04-17', 'Jueves Santo', 'Nacional'),
       ('2025-04-18', 'Viernes Santo', 'Nacional'),
       ('2025-05-01', 'Día del Trabajo', 'Nacional'),
       ('2025-08-15', 'Asunción de la Virgen', 'Nacional'),
       ('2025-10-12', 'Día de la Hispanidad', 'Nacional'),
       ('2025-11-01', 'Todos los Santos', 'Nacional'),
       ('2025-12-06', 'Día de la Constitución', 'Nacional'),
       ('2025-12-08', 'Inmaculada Concepción', 'Nacional'),
       ('2025-12-25', 'Navidad', 'Nacional');

-- Empleados de prueba
-- NOTA: Los password_hash son BCrypt de las contraseñas indicadas en el comentario
-- En la app Java, se generan con BCrypt.hashpw(password, BCrypt.gensalt(12))
-- Contraseñas de prueba: Admin1234!, RRHH1234!, Emp1234! (cámbialas en producción)
INSERT INTO empleados
(cod_empleado, nombre, apellido1, apellido2, dni, email, telefono,
 username, password_hash, rol, cod_departamento)
VALUES
    -- ADMIN
    (1001, 'Carlos', 'García', 'López', '12345678A', 'carlos.garcia@empresa.com', '600111222',
     'cgarcia',
     '$2a$12$examplehashADMIN000000000000000000000000000000000000000',
     'ADMIN', 3),

    -- RRHH
    (1002, 'María', 'Martínez', 'Sánchez', '23456789B', 'maria.martinez@empresa.com', '600333444',
     'mmartinez',
     '$2a$12$examplehashRRHH000000000000000000000000000000000000000',
     'RRHH', 2),

    -- EMPLEADOS
    (1003, 'Juan', 'Rodríguez', 'Pérez', '34567890C', 'juan.rodriguez@empresa.com', '600555666',
     'jrodriguez',
     '$2a$12$examplehashEMP1000000000000000000000000000000000000000',
     'EMPLEADO', 1),

    (1004, 'Ana', 'López', 'Gómez', '45678901D', 'ana.lopez@empresa.com', '600777888',
     'alopez',
     '$2a$12$examplehashEMP2000000000000000000000000000000000000000',
     'EMPLEADO', 1),

    (1005, 'Pedro', 'Fernández', NULL, '56789012E', 'pedro.fernandez@empresa.com', '600999000',
     'pfernandez',
     '$2a$12$examplehashEMP3000000000000000000000000000000000000000',
     'EMPLEADO', 4);

-- Fichajes de prueba (últimos 5 días laborables)
INSERT INTO dias
(fecha, cod_empleado, entrada_hora, salida_hora, turno_entrada, turno_salida, horas_trabajadas, horas_extras)
VALUES
    -- Empleado 1003 - semana actual
    (DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 1003, '08:00:00', '16:30:00', 'Mañana', 'Tarde', 8.50, 0.50),
    (DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 1003, '08:15:00', '16:00:00', 'Mañana', 'Tarde', 7.75, 0.00),
    (DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 1003, '07:50:00', '17:00:00', 'Mañana', 'Tarde', 9.17, 1.17),
    (DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 1003, '08:00:00', '16:00:00', 'Mañana', 'Tarde', 8.00, 0.00),

    -- Empleado 1004
    (DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 1004, '09:00:00', '17:30:00', 'Mañana', 'Tarde', 8.50, 0.50),
    (DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 1004, '09:00:00', '18:00:00', 'Mañana', 'Tarde', 9.00, 1.00),
    (DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 1004, '09:05:00', '17:00:00', 'Mañana', 'Tarde', 7.92, 0.00),

    -- Empleado 1005
    (DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 1005, '14:00:00', '22:00:00', 'Tarde', 'Noche', 8.00, 0.00),
    (DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 1005, '14:00:00', '23:00:00', 'Tarde', 'Noche', 9.00, 1.00);

-- ============================================================
-- 9. VERIFICACIÓN FINAL
-- ============================================================
SELECT '✅ Base de datos creada correctamente' AS estado;
SELECT CONCAT('   → Departamentos: ', COUNT(*)) AS info
FROM departamentos
UNION ALL
SELECT CONCAT('   → Empleados:     ', COUNT(*))
FROM empleados
UNION ALL
SELECT CONCAT('   → Festivos:      ', COUNT(*))
FROM festivos
UNION ALL
SELECT CONCAT('   → Fichajes demo: ', COUNT(*))
FROM dias;
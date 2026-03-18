package dev.danipraivet.modelo.entidades;

import dev.danipraivet.modelo.enumeraciones.Rol;
import dev.danipraivet.modelo.enumeraciones.Turno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PU-03: Pruebas unitarias de los métodos de las entidades del dominio.
 * Cubre Empleado, Fichaje y Departamento sin necesidad de conexión a BD.
 */
@DisplayName("PU-03 - Entidades del dominio")
class EntidadesTest {

    // Empleado

    @Nested
    @DisplayName("Empleado")
    class EmpleadoTest {

        private Empleado empleado;

        @BeforeEach
        void setUp() {
            empleado = new Empleado();
            empleado.setCodEmpleado(1001);
            empleado.setNombre("Carlos");
            empleado.setApellido1("García");
            empleado.setApellido2("López");
            empleado.setDni("12345678Z");
            empleado.setUsername("cgarcia");
            empleado.setRol(Rol.EMPLEADO);
            empleado.setActivo(true);
            empleado.setBloqueado(false);
        }

        @Test
        @DisplayName("getNombreCompleto con dos apellidos incluye ambos")
        void getNombreCompleto_dosApellidos_incluyeAmbos() {
            assertEquals("Carlos García López", empleado.getNombreCompleto());
        }

        @Test
        @DisplayName("getNombreCompleto sin segundo apellido no añade espacio extra")
        void getNombreCompleto_sinSegundoApellido_sinEspacioExtra() {
            empleado.setApellido2(null);
            assertEquals("Carlos García", empleado.getNombreCompleto());
        }

        @Test
        @DisplayName("getNombreCompleto con segundo apellido en blanco omite el apellido")
        void getNombreCompleto_apellido2EnBlanco_loOmite() {
            empleado.setApellido2("  ");
            assertEquals("Carlos García", empleado.getNombreCompleto());
        }

        @Test
        @DisplayName("getIniciales devuelve las iniciales en mayúsculas")
        void getIniciales_devuelveIniciales() {
            assertEquals("CG", empleado.getIniciales());
        }

        @Test
        @DisplayName("estaOperativo devuelve true si activo y no bloqueado")
        void estaOperativo_activoYNoBloqueado_devuelveTrue() {
            assertTrue(empleado.estaOperativo());
        }

        @Test
        @DisplayName("estaOperativo devuelve false si la cuenta está bloqueada")
        void estaOperativo_bloqueado_devuelveFalse() {
            empleado.setBloqueado(true);
            assertFalse(empleado.estaOperativo());
        }

        @Test
        @DisplayName("estaOperativo devuelve false si la cuenta está inactiva")
        void estaOperativo_inactivo_devuelveFalse() {
            empleado.setActivo(false);
            assertFalse(empleado.estaOperativo());
        }

        @Test
        @DisplayName("equals compara por codEmpleado, no por referencia")
        void equals_mismoCodigoDistintaInstancia_sonIguales() {
            Empleado otro = new Empleado();
            otro.setCodEmpleado(1001);
            assertEquals(empleado, otro);
        }

        @Test
        @DisplayName("equals devuelve false para códigos distintos")
        void equals_codigosDistintos_noSonIguales() {
            Empleado otro = new Empleado();
            otro.setCodEmpleado(9999);
            assertNotEquals(empleado, otro);
        }

        @Test
        @DisplayName("toString contiene el código y el DNI")
        void toString_contieneCodigoYDni() {
            String str = empleado.toString();
            assertTrue(str.contains("1001"));
            assertTrue(str.contains("12345678Z"));
        }

        @Test
        @DisplayName("Constructor con parámetros establece activo=true por defecto")
        void constructor_conParametros_activoPorDefecto() {
            Empleado e = new Empleado(1002, "Ana", "López", null, "87654321X", "alopez", "hash", Rol.RRHH);
            assertTrue(e.isActivo());
        }
    }

    // Fichaje


    @Nested
    @DisplayName("Fichaje")
    class FichajeTest {

        private Fichaje fichajeCompleto;
        private Fichaje fichajeAbierto;
        private Fichaje fichajeVacio;

        @BeforeEach
        void setUp() {
            fichajeCompleto = new Fichaje();
            fichajeCompleto.setId(1);
            fichajeCompleto.setCodEmpleado(1001);
            fichajeCompleto.setFecha(LocalDate.now());
            fichajeCompleto.setEntradaHora(LocalTime.of(8, 0));
            fichajeCompleto.setSalidaHora(LocalTime.of(16, 30));
            fichajeCompleto.setTurnoEntrada(Turno.MANANA);
            fichajeCompleto.setHorasTrabajadas(new BigDecimal("8.50"));
            fichajeCompleto.setHorasExtras(new BigDecimal("0.50"));

            fichajeAbierto = new Fichaje();
            fichajeAbierto.setEntradaHora(LocalTime.of(9, 0));

            fichajeVacio = new Fichaje();
        }

        @Test
        @DisplayName("estaCompleto devuelve true cuando hay hora de salida")
        void estaCompleto_conSalida_devuelveTrue() {
            assertTrue(fichajeCompleto.estaCompleto());
        }

        @Test
        @DisplayName("estaCompleto devuelve false cuando no hay hora de salida")
        void estaCompleto_sinSalida_devuelveFalse() {
            assertFalse(fichajeAbierto.estaCompleto());
        }

        @Test
        @DisplayName("estaFichado devuelve true con entrada abierta sin salida")
        void estaFichado_entradaAbierta_devuelveTrue() {
            assertTrue(fichajeAbierto.estaFichado());
        }

        @Test
        @DisplayName("estaFichado devuelve false cuando el fichaje está completo")
        void estaFichado_fichajeCompleto_devuelveFalse() {
            assertFalse(fichajeCompleto.estaFichado());
        }

        @Test
        @DisplayName("estaFichado devuelve false cuando no hay ninguna entrada")
        void estaFichado_sinEntrada_devuelveFalse() {
            assertFalse(fichajeVacio.estaFichado());
        }

        @Test
        @DisplayName("getHorasFormateadas formatea correctamente 8.50h como '8h 30m'")
        void getHorasFormateadas_ochoCincuenta_devuelveOchoTreinta() {
            assertEquals("8h 30m", fichajeCompleto.getHorasFormateadas());
        }

        @Test
        @DisplayName("getHorasFormateadas devuelve cadena vacía cuando hay cero horas")
        void getHorasFormateadas_ceroHoras_devuelveCadenaVacia() {
            assertEquals("", fichajeVacio.getHorasFormateadas());
        }

        @Test
        @DisplayName("getEstado devuelve 'Ausente' sin entrada")
        void getEstado_sinEntrada_devuelveAusente() {
            assertEquals("Ausente", fichajeVacio.getEstado());
        }

        @Test
        @DisplayName("getEstado devuelve 'Trabajando' con entrada abierta")
        void getEstado_entradaAbierta_devuelveTrabajando() {
            assertEquals("Trabajando", fichajeAbierto.getEstado());
        }

        @Test
        @DisplayName("getEstado devuelve 'Completado' cuando fichaje tiene entrada y salida")
        void getEstado_fichajeCompleto_devuelveCompletado() {
            assertEquals("Completado", fichajeCompleto.getEstado());
        }

        @Test
        @DisplayName("Constructor inicializa horasTrabajadas y horasExtras a cero")
        void constructor_inicializaHorasACero() {
            Fichaje f = new Fichaje();
            assertEquals(BigDecimal.ZERO, f.getHorasTrabajadas());
            assertEquals(BigDecimal.ZERO, f.getHorasExtras());
        }

        @Test
        @DisplayName("equals compara por id, no por referencia")
        void equals_mismoIdDistintaInstancia_sonIguales() {
            Fichaje otro = new Fichaje();
            otro.setId(1);
            assertEquals(fichajeCompleto, otro);
        }
    }

    // Departamento

    @Nested
    @DisplayName("Departamento")
    class DepartamentoTest {

        @Test
        @DisplayName("Constructor con nombre y descripción establece activo=true")
        void constructor_establece_activoTrue() {
            Departamento dep = new Departamento("Tecnología", "Equipo de desarrollo");
            assertTrue(dep.isActivo());
        }

        @Test
        @DisplayName("toString devuelve el nombre del departamento")
        void toString_devuelveNombre() {
            Departamento dep = new Departamento("Recursos Humanos", "Gestión de personal");
            assertEquals("Recursos Humanos", dep.toString());
        }

        @Test
        @DisplayName("equals compara por codDepartamento")
        void equals_mismoCodigoDistintaInstancia_sonIguales() {
            Departamento d1 = new Departamento(1, "Tecnología", "Desc", true, null);
            Departamento d2 = new Departamento(1, "Otro nombre", "Otra desc", false, null);
            assertEquals(d1, d2);
        }
    }

    // Enumeraciones

    @Nested
    @DisplayName("Enumeraciones - Rol y Turno")
    class EnumeracionesTest {

        @Test
        @DisplayName("Rol.fromString('ADMIN') devuelve ADMIN")
        void rolFromString_admin_devuelveAdmin() {
            assertEquals(Rol.ADMIN, Rol.fromString("ADMIN"));
        }

        @Test
        @DisplayName("Rol.fromString nulo devuelve EMPLEADO por defecto")
        void rolFromString_nulo_devuelveEmpleado() {
            assertEquals(Rol.EMPLEADO, Rol.fromString(null));
        }

        @Test
        @DisplayName("Rol.fromString valor desconocido devuelve EMPLEADO por defecto")
        void rolFromString_desconocido_devuelveEmpleado() {
            assertEquals(Rol.EMPLEADO, Rol.fromString("DESCONOCIDO"));
        }

        @Test
        @DisplayName("Turno.fromString('Mañana') devuelve MANANA")
        void turnoFromString_manana_devuelveManana() {
            assertEquals(Turno.MANANA, Turno.fromString("Mañana"));
        }

        @Test
        @DisplayName("Turno.fromString nulo devuelve null")
        void turnoFromString_nulo_devuelveNull() {
            assertNull(Turno.fromString(null));
        }

        @Test
        @DisplayName("Turno.detectar devuelve un turno no nulo para la hora actual")
        void turnoDetectar_devuelveTurnoNoNulo() {
            assertNotNull(Turno.detectar());
        }
    }
}
package dev.danipraivet.modelo.utilidades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PU-02: Pruebas unitarias de ValidadorFormularios.
 * Cubre validación de DNI (formato y letra de control), email, teléfono,
 * username, contraseña segura y campos obligatorios.
 */
@DisplayName("PU-02 - ValidadorFormularios")
class ValidadorFormulariosTest {

    // ---------------------------------------------------------------
    // DNI
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validación de DNI")
    class DniTest {

        @Test
        @DisplayName("DNI válido con letra correcta devuelve true")
        void dniValido_letraCorrecta_devuelveTrue() {
            // 12345678Z es un DNI con letra de control correcta
            assertTrue(ValidadorFormularios.dniValido("12345678Z"));
        }

        @Test
        @DisplayName("DNI válido en minúsculas es aceptado (normalización)")
        void dniValido_enMinusculas_esCorrecto() {
            assertTrue(ValidadorFormularios.dniValido("12345678z"));
        }

        @Test
        @DisplayName("DNI con letra de control incorrecta devuelve false")
        void dniValido_letraIncorrecta_devuelveFalse() {
            assertFalse(ValidadorFormularios.dniValido("12345678A"),
                    "La letra A no es la letra de control correcta para 12345678");
        }

        @ParameterizedTest
        @ValueSource(strings = {"1234567A", "123456789A", "1234567A8", "ABCDEFGHZ", ""})
        @DisplayName("DNI con formato inválido devuelve false")
        void dniValido_formatoInvalido_devuelveFalse(String dni) {
            assertFalse(ValidadorFormularios.dniValido(dni));
        }

        @Test
        @DisplayName("DNI nulo devuelve false")
        void dniValido_nulo_devuelveFalse() {
            assertFalse(ValidadorFormularios.dniValido(null));
        }

        @Test
        @DisplayName("mensajeDni devuelve null para un DNI correcto")
        void mensajeDni_dniCorrecto_devuelveNull() {
            assertNull(ValidadorFormularios.mensajeDni("12345678Z"));
        }

        @Test
        @DisplayName("mensajeDni devuelve mensaje para DNI vacío")
        void mensajeDni_vacio_devuelveMensaje() {
            String msg = ValidadorFormularios.mensajeDni("");
            assertNotNull(msg);
            assertFalse(msg.isBlank());
        }

        @Test
        @DisplayName("mensajeDni devuelve mensaje para DNI con letra incorrecta")
        void mensajeDni_letraIncorrecta_devuelveMensaje() {
            String msg = ValidadorFormularios.mensajeDni("12345678A");
            assertNotNull(msg);
            assertTrue(msg.toLowerCase().contains("letra"),
                    "El mensaje debe indicar que la letra es incorrecta");
        }
    }

    // ---------------------------------------------------------------
    // Email
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validación de email")
    class EmailTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "usuario@empresa.com",
                "nombre.apellido@correo.es",
                "user+tag@dominio.org"
        })
        @DisplayName("Email con formato válido devuelve true")
        void emailValido_formatoCorrecto_devuelveTrue(String email) {
            assertTrue(ValidadorFormularios.emailValido(email));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sinArroba",
                "sin@dominio",
                "@sinUsuario.com",
                "espacios en @medio.com",
                ""
        })
        @DisplayName("Email con formato inválido devuelve false")
        void emailValido_formatoIncorrecto_devuelveFalse(String email) {
            assertFalse(ValidadorFormularios.emailValido(email));
        }

        @Test
        @DisplayName("Email nulo devuelve false")
        void emailValido_nulo_devuelveFalse() {
            assertFalse(ValidadorFormularios.emailValido(null));
        }
    }

    // ---------------------------------------------------------------
    // Teléfono
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validación de teléfono")
    class TelefonoTest {

        @ParameterizedTest
        @ValueSource(strings = {"600111222", "712345678", "612000000"})
        @DisplayName("Teléfono móvil español válido devuelve true")
        void telefonoValido_formatoCorrecto_devuelveTrue(String telefono) {
            assertTrue(ValidadorFormularios.telefonoValido(telefono));
        }

        @Test
        @DisplayName("Teléfono nulo devuelve true (campo opcional)")
        void telefonoValido_nulo_devuelveTrue() {
            assertTrue(ValidadorFormularios.telefonoValido(null));
        }

        @Test
        @DisplayName("Teléfono vacío devuelve true (campo opcional)")
        void telefonoValido_vacio_devuelveTrue() {
            assertTrue(ValidadorFormularios.telefonoValido(""));
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789", "60011122", "6001112223", "6001abc22"})
        @DisplayName("Teléfono con formato inválido devuelve false")
        void telefonoValido_formatoIncorrecto_devuelveFalse(String telefono) {
            assertFalse(ValidadorFormularios.telefonoValido(telefono));
        }
    }

    // ---------------------------------------------------------------
    // Username
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validación de username")
    class UsernameTest {

        @ParameterizedTest
        @ValueSource(strings = {"cgarcia", "mmartinez2", "user.name", "admin_app"})
        @DisplayName("Username válido devuelve true")
        void usernameValido_correcto_devuelveTrue(String username) {
            assertTrue(ValidadorFormularios.usernameValido(username));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab", "123inicio", "", "usuario con espacios"})
        @DisplayName("Username inválido devuelve false")
        void usernameValido_incorrecto_devuelveFalse(String username) {
            assertFalse(ValidadorFormularios.usernameValido(username));
        }

        @Test
        @DisplayName("Username nulo devuelve false")
        void usernameValido_nulo_devuelveFalse() {
            assertFalse(ValidadorFormularios.usernameValido(null));
        }
    }

    // ---------------------------------------------------------------
    // Contraseña segura
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validación de contraseña segura")
    class ContrasenaTest {

        @ParameterizedTest
        @ValueSource(strings = {"Admin1234!", "Pass#2024", "Segura@99x"})
        @DisplayName("Contraseña que cumple todos los requisitos devuelve true")
        void contrasenaSegura_cumpleRequisitos_devuelveTrue(String pass) {
            assertTrue(ValidadorFormularios.contrasenaSegura(pass));
        }

        @Test
        @DisplayName("Contraseña sin mayúscula devuelve false")
        void contrasenaSegura_sinMayuscula_devuelveFalse() {
            assertFalse(ValidadorFormularios.contrasenaSegura("admin1234!"));
        }

        @Test
        @DisplayName("Contraseña sin minúscula devuelve false")
        void contrasenaSegura_sinMinuscula_devuelveFalse() {
            assertFalse(ValidadorFormularios.contrasenaSegura("ADMIN1234!"));
        }

        @Test
        @DisplayName("Contraseña sin dígito devuelve false")
        void contrasenaSegura_sinDigito_devuelveFalse() {
            assertFalse(ValidadorFormularios.contrasenaSegura("AdminPass!"));
        }

        @Test
        @DisplayName("Contraseña sin carácter especial devuelve false")
        void contrasenaSegura_sinEspecial_devuelveFalse() {
            assertFalse(ValidadorFormularios.contrasenaSegura("Admin12345"));
        }

        @Test
        @DisplayName("Contraseña con menos de 8 caracteres devuelve false")
        void contrasenaSegura_menosDe8Chars_devuelveFalse() {
            assertFalse(ValidadorFormularios.contrasenaSegura("Ad1!xyz"));
        }

        @Test
        @DisplayName("mensajeContrasena devuelve null para contraseña válida")
        void mensajeContrasena_contrasenaValida_devuelveNull() {
            assertNull(ValidadorFormularios.mensajeContrasena("Admin1234!"));
        }

        @Test
        @DisplayName("mensajeContrasena devuelve mensaje para contraseña vacía")
        void mensajeContrasena_vacia_devuelveMensaje() {
            String msg = ValidadorFormularios.mensajeContrasena("");
            assertNotNull(msg);
            assertFalse(msg.isBlank());
        }
    }

    // ---------------------------------------------------------------
    // noVacio
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validación de campo no vacío")
    class NoVacioTest {

        @Test
        @DisplayName("Cadena con contenido devuelve true")
        void noVacio_cadenaConContenido_devuelveTrue() {
            assertTrue(ValidadorFormularios.noVacio("hola"));
        }

        @Test
        @DisplayName("Cadena vacía devuelve false")
        void noVacio_cadenaVacia_devuelveFalse() {
            assertFalse(ValidadorFormularios.noVacio(""));
        }

        @Test
        @DisplayName("Cadena en blanco (espacios) devuelve false")
        void noVacio_espacios_devuelveFalse() {
            assertFalse(ValidadorFormularios.noVacio("   "));
        }

        @Test
        @DisplayName("Null devuelve false")
        void noVacio_nulo_devuelveFalse() {
            assertFalse(ValidadorFormularios.noVacio(null));
        }
    }
}
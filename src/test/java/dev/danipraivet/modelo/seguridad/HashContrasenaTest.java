package dev.danipraivet.modelo.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PU-01: Pruebas unitarias de HashContrasena.
 * Verifica que el hashing y la verificación BCrypt funcionan correctamente,
 * incluyendo casos límite: contraseña nula, vacía y hash malformado.
 */
@DisplayName("PU-01 - HashContrasena")
class HashContrasenaTest {

    // ---------------------------------------------------------------
    // hashear()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Hashear una contraseña válida genera un hash BCrypt no nulo")
    void hashear_contrasenaValida_devuelveHashNoCero() {
        String hash = HashContrasena.hashear("Admin1234!");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"), "El hash debe comenzar con el prefijo BCrypt '$2a$'");
    }

    @Test
    @DisplayName("Dos hashes de la misma contraseña son distintos (salt aleatorio)")
    void hashear_mismaContrasena_generaHashesDiferentes() {
        String hash1 = HashContrasena.hashear("Admin1234!");
        String hash2 = HashContrasena.hashear("Admin1234!");
        assertNotEquals(hash1, hash2, "BCrypt debe generar salt distinto en cada llamada");
    }

    @Test
    @DisplayName("Hashear contraseña nula lanza IllegalArgumentException")
    void hashear_contrasenaNula_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> HashContrasena.hashear(null));
    }

    @Test
    @DisplayName("Hashear contraseña vacía lanza IllegalArgumentException")
    void hashear_contrasenaVacia_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> HashContrasena.hashear(""));
    }

    @Test
    @DisplayName("Hashear contraseña en blanco (solo espacios) lanza IllegalArgumentException")
    void hashear_contrasenaEnBlanco_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> HashContrasena.hashear("   "));
    }

    // ---------------------------------------------------------------
    // verificar()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Verificar contraseña correcta contra su hash devuelve true")
    void verificar_contrasenaCorrecta_devuelveTrue() {
        String contrasena = "Empleado1234!";
        String hash = HashContrasena.hashear(contrasena);
        assertTrue(HashContrasena.verificar(contrasena, hash));
    }

    @Test
    @DisplayName("Verificar contraseña incorrecta devuelve false")
    void verificar_contrasenaIncorrecta_devuelveFalse() {
        String hash = HashContrasena.hashear("Admin1234!");
        assertFalse(HashContrasena.verificar("ContrasenaMal", hash));
    }

    @Test
    @DisplayName("Verificar contraseña vacía devuelve false")
    void verificar_contrasenaVacia_devuelveFalse() {
        String hash = HashContrasena.hashear("Admin1234!");
        assertFalse(HashContrasena.verificar("", hash));
    }

    @Test
    @DisplayName("Verificar contraseña nula devuelve false")
    void verificar_contrasenaNula_devuelveFalse() {
        String hash = HashContrasena.hashear("Admin1234!");
        assertFalse(HashContrasena.verificar(null, hash));
    }

    @Test
    @DisplayName("Verificar con hash nulo devuelve false")
    void verificar_hashNulo_devuelveFalse() {
        assertFalse(HashContrasena.verificar("Admin1234!", null));
    }

    @Test
    @DisplayName("Verificar con hash malformado devuelve false sin lanzar excepción")
    void verificar_hashMalformado_devuelveFalseSinExcepcion() {
        assertFalse(HashContrasena.verificar("Admin1234!", "hashquenoesvalido"));
    }

    @Test
    @DisplayName("Verificar con hash de placeholder (datos de prueba SQL) devuelve false")
    void verificar_hashPlaceholder_devuelveFalse() {
        String hashPlaceholder = "$2a$12$examplehashADMIN000000000000000000000000000000000000000";
        assertFalse(HashContrasena.verificar("Admin1234!", hashPlaceholder),
                "Un hash placeholder no debe verificar ninguna contraseña real");
    }
}
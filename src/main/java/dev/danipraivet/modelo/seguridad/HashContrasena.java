package dev.danipraivet.modelo.seguridad;

import org.mindrot.jbcrypt.BCrypt;

// Wrapper sobre BCrypt para hashear y verificar contrasenas.
// BCrypt incluye salt aleatorio y factor de coste configurable.
public final class HashContrasena {

    // Factor de coste 12: buen equilibrio entre seguridad y rendimiento (~300ms en hardware moderno)
    private static final int COSTE = 12;

    private HashContrasena() {}

    // Genera el hash BCrypt de una contrasena en texto plano
    public static String hashear(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia.");
        }
        return BCrypt.hashpw(contrasena, BCrypt.gensalt(COSTE));
    }

    // Verifica si una contrasena en texto plano coincide con su hash almacenado
    public static boolean verificar(String contrasenaPlana, String hash) {
        if (contrasenaPlana == null || hash == null) return false;
        try {
            return BCrypt.checkpw(contrasenaPlana, hash);
        } catch (IllegalArgumentException e) {
            // Hash malformado (campo de BD vacio o corrupto)
            return false;
        }
    }
}

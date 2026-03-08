package dev.danipraivet.modelo.utilidades;

import java.util.regex.Pattern;

// Validaciones de formulario centralizadas. Evita duplicar logica en los controladores.
public final class ValidadorFormularios {

    private static final Pattern PATRON_DNI      = Pattern.compile("^[0-9]{8}[A-HJ-NP-TV-Z]$");
    private static final Pattern PATRON_EMAIL    = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^[67][0-9]{8}$");
    private static final Pattern PATRON_USERNAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]{3,29}$");

    // Tabla de letras del DNI espanol (posicion = numero % 23)
    private static final String LETRAS_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";

    private ValidadorFormularios() {}

    // Comprueba formato y letra de control del DNI espanol
    public static boolean dniValido(String dni) {
        if (dni == null || !PATRON_DNI.matcher(dni.toUpperCase()).matches()) return false;
        int numero = Integer.parseInt(dni.substring(0, 8));
        char letraEsperada = LETRAS_DNI.charAt(numero % 23);
        return Character.toUpperCase(dni.charAt(8)) == letraEsperada;
    }

    public static boolean emailValido(String email) {
        return email != null && PATRON_EMAIL.matcher(email).matches();
    }

    public static boolean telefonoValido(String telefono) {
        return telefono == null || telefono.isBlank() || PATRON_TELEFONO.matcher(telefono).matches();
    }

    public static boolean usernameValido(String username) {
        return username != null && PATRON_USERNAME.matcher(username).matches();
    }

    // Valida que la contrasena tenga mayuscula, minuscula, digito y caracter especial (min. 8 chars)
    public static boolean contrasenaSegura(String contrasena) {
        if (contrasena == null || contrasena.length() < 8) return false;
        boolean tieneMayuscula = contrasena.chars().anyMatch(Character::isUpperCase);
        boolean tieneMinuscula = contrasena.chars().anyMatch(Character::isLowerCase);
        boolean tieneDigito    = contrasena.chars().anyMatch(Character::isDigit);
        boolean tieneEspecial  = contrasena.chars().anyMatch(c -> "!@#$%^&*()-_=+[]{}|;:',.<>?".indexOf(c) >= 0);
        return tieneMayuscula && tieneMinuscula && tieneDigito && tieneEspecial;
    }

    public static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    // Devuelve mensaje de error para un DNI invalido, o null si es correcto
    public static String mensajeDni(String dni) {
        if (!noVacio(dni))                                        return "El DNI no puede estar vacio.";
        if (!PATRON_DNI.matcher(dni.toUpperCase()).matches())     return "Formato invalido. Ejemplo: 12345678A";
        if (!dniValido(dni))                                      return "La letra del DNI no es correcta.";
        return null;
    }

    // Devuelve mensaje de error para una contrasena insegura, o null si es correcta
    public static String mensajeContrasena(String contrasena) {
        if (!noVacio(contrasena))          return "La contrasena no puede estar vacia.";
        if (contrasena.length() < 8)      return "Minimo 8 caracteres.";
        if (!contrasenaSegura(contrasena)) return "Debe incluir mayuscula, minuscula, numero y caracter especial.";
        return null;
    }
}

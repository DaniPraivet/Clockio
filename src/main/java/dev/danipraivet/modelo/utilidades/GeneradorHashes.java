package dev.danipraivet.modelo.utilidades;

import dev.danipraivet.modelo.seguridad.HashContrasena;

// Herramienta de consola para generar hashes BCrypt manualmente durante el desarrollo.
public class GeneradorHashes {

    public static void main(String[] args) {
        String[] contrasenas = {"Admin1234!", "RRHH1234!", "Emp1234!"};
        for (String c : contrasenas) {
            System.out.println(c + " -> " + HashContrasena.hashear(c));
        }
    }
}

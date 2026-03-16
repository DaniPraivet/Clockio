package dev.danipraivet.modelo.entidades;

import dev.danipraivet.modelo.enumeraciones.Rol;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Setter
@Getter
public class Empleado {

    private int codEmpleado;
    private String nombre;
    private String apellido1;
    private String apellido2;
    private String dni;
    private String email;
    private String telefono;

    private String username;
    private String passwordHash;
    private Rol rol;

    private boolean activo;
    private int intentosFallidos;
    private boolean bloqueado;

    private LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;
    private LocalDateTime ultimoAcceso;

    private Departamento departamento;

    public Empleado() {
    }

    public Empleado(int codEmpleado, String nombre, String apellido1, String apellido2, String dni, String username, String passwordHash, Rol rol) {
        this.codEmpleado = codEmpleado;
        this.nombre = nombre;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.dni = dni;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = true;
    }

    // Devuelve el nombre completo formateado: "Nombre Apellido1 [Apellido2]"
    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder(nombre).append(" ").append(apellido1);
        if (apellido2 != null && !apellido2.isBlank()) {
            sb.append(" ").append(apellido2);
        }
        return sb.toString();
    }

    // Devuelve las iniciales del nombre y primer apellido (ej. "CG" para Carlos Garcia)
    public String getIniciales() {
        String i1 = nombre != null && !nombre.isEmpty() ? String.valueOf(nombre.charAt(0)) : "";
        String i2 = apellido1 != null && !apellido1.isEmpty() ? String.valueOf(apellido1.charAt(0)) : "";
        return (i1 + i2).toUpperCase();
    }

    // Comprueba si la cuenta esta operativa (activa y no bloqueada)
    public boolean estaOperativo() {
        return activo && !bloqueado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empleado e)) return false;
        return codEmpleado == e.codEmpleado;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codEmpleado);
    }

    @Override
    public String toString() {
        return codEmpleado + " - " + dni + " - " + getNombreCompleto();
    }
}

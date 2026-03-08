package dev.danipraivet.modelo.entidades;

import dev.danipraivet.modelo.enumeraciones.Rol;

import java.time.LocalDateTime;
import java.util.Objects;

// Entidad que representa a un empleado. Mapeada a la tabla 'empleados' de MySQL.
public class Empleado {

    private int codEmpleado;
    private String nombre;
    private String apellido1;
    private String apellido2;        // Nullable
    private String dni;
    private String email;            // Nullable
    private String telefono;         // Nullable

    private String username;
    private String passwordHash;     // BCrypt - solo para autenticacion
    private Rol rol;

    private boolean activo;
    private int intentosFallidos;
    private boolean bloqueado;

    private LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;        // Nullable
    private LocalDateTime ultimoAcceso;     // Nullable

    private Departamento departamento;     // Nullable

    public Empleado() {
    }

    public Empleado(int codEmpleado, String nombre, String apellido1, String apellido2,
                    String dni, String username, String passwordHash, Rol rol) {
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

    public int getCodEmpleado() {
        return codEmpleado;
    }

    public void setCodEmpleado(int codEmpleado) {
        this.codEmpleado = codEmpleado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentos) {
        this.intentosFallidos = intentos;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(boolean bloqueado) {
        this.bloqueado = bloqueado;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public LocalDateTime getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDateTime fechaBaja) {
        this.fechaBaja = fechaBaja;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ua) {
        this.ultimoAcceso = ua;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento dep) {
        this.departamento = dep;
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

    // passwordHash excluido para no filtrarlo en logs
    @Override
    public String toString() {
        return "Empleado{cod=" + codEmpleado +
                ", nombre='" + getNombreCompleto() + '\'' +
                ", dni='" + dni + '\'' +
                ", username='" + username + '\'' +
                ", rol=" + rol +
                ", activo=" + activo + '}';
    }
}

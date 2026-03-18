package dev.danipraivet.modelo.entidades;

import java.time.LocalDateTime;
import java.util.Objects;

// Entidad que representa un departamento. Mapeada a la tabla 'departamentos' de MySQL.
public class Departamento {

    private int codDepartamento;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private LocalDateTime fechaCreacion;

    public Departamento() {
    }

    public Departamento(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = true;
    }

    // Constructor completo usado al leer desde JDBC
    public Departamento(int codDepartamento, String nombre, String descripcion, boolean activo, LocalDateTime fechaCreacion) {
        this.codDepartamento = codDepartamento;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    public int getCodDepartamento() {
        return codDepartamento;
    }

    public void setCodDepartamento(int codDepartamento) {
        this.codDepartamento = codDepartamento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Departamento d)) return false;
        return codDepartamento == d.codDepartamento;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codDepartamento);
    }

    @Override
    public String toString() {
        return nombre;
    } // Util para ComboBox en JavaFX
}

package dev.danipraivet.modelo.entidades;

import dev.danipraivet.modelo.enumeraciones.Turno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

// Entidad que representa un registro de fichaje diario. Mapeada a la tabla 'dias' de MySQL.
public class Fichaje {

    private int id;
    private LocalDate fecha;
    private int codEmpleado;

    private Empleado empleado;         // Solo presente si se hace JOIN con empleados

    private LocalTime entradaHora;      // Nullable hasta que fiche
    private LocalTime salidaHora;       // Nullable hasta que salga
    private Turno turnoEntrada;     // Nullable
    private Turno turnoSalida;      // Nullable

    private BigDecimal horasTrabajadas;
    private BigDecimal horasExtras;

    private boolean festivo;
    private boolean justificado;
    private String observaciones;

    private LocalDateTime creadoEn;
    private LocalDateTime modificadoEn;

    public Fichaje() {
        this.horasTrabajadas = BigDecimal.ZERO;
        this.horasExtras = BigDecimal.ZERO;
    }

    public Fichaje(int codEmpleado, LocalDate fecha, LocalTime entradaHora, Turno turnoEntrada) {
        this();
        this.codEmpleado = codEmpleado;
        this.fecha = fecha;
        this.entradaHora = entradaHora;
        this.turnoEntrada = turnoEntrada;
    }

    // Indica si el empleado ya ha registrado su salida
    public boolean estaCompleto() {
        return salidaHora != null;
    }

    // Indica si hay una entrada abierta sin salida (empleado dentro)
    public boolean estaFichado() {
        return entradaHora != null && salidaHora == null;
    }

    // Devuelve las horas trabajadas como "Xh Ym" para mostrar en la interfaz
    public String getHorasFormateadas() {
        if (horasTrabajadas == null || horasTrabajadas.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        int horas = horasTrabajadas.intValue();
        int minutos = (int) Math.round((horasTrabajadas.doubleValue() - horas) * 60);
        return horas + "h " + String.format("%02d", minutos) + "m";
    }

    // Estado resumido: "Trabajando", "Completado" o "Ausente"
    public String getEstado() {
        if (entradaHora == null) return "Ausente";
        if (salidaHora == null) return "Trabajando";
        return "Completado";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getCodEmpleado() {
        return codEmpleado;
    }

    public void setCodEmpleado(int codEmpleado) {
        this.codEmpleado = codEmpleado;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public LocalTime getEntradaHora() {
        return entradaHora;
    }

    public void setEntradaHora(LocalTime entradaHora) {
        this.entradaHora = entradaHora;
    }

    public LocalTime getSalidaHora() {
        return salidaHora;
    }

    public void setSalidaHora(LocalTime salidaHora) {
        this.salidaHora = salidaHora;
    }

    public Turno getTurnoEntrada() {
        return turnoEntrada;
    }

    public void setTurnoEntrada(Turno turnoEntrada) {
        this.turnoEntrada = turnoEntrada;
    }

    public Turno getTurnoSalida() {
        return turnoSalida;
    }

    public void setTurnoSalida(Turno turnoSalida) {
        this.turnoSalida = turnoSalida;
    }

    public BigDecimal getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(BigDecimal h) {
        this.horasTrabajadas = h;
    }

    public BigDecimal getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(BigDecimal h) {
        this.horasExtras = h;
    }

    public boolean isFestivo() {
        return festivo;
    }

    public void setFestivo(boolean festivo) {
        this.festivo = festivo;
    }

    public boolean isJustificado() {
        return justificado;
    }

    public void setJustificado(boolean justificado) {
        this.justificado = justificado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String obs) {
        this.observaciones = obs;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime t) {
        this.creadoEn = t;
    }

    public LocalDateTime getModificadoEn() {
        return modificadoEn;
    }

    public void setModificadoEn(LocalDateTime t) {
        this.modificadoEn = t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fichaje f)) return false;
        return id == f.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Fichaje{id=" + id + ", codEmpleado=" + codEmpleado + ", fecha=" + fecha + ", entrada=" + entradaHora + ", salida=" + salidaHora + ", horas=" + horasTrabajadas + ", estado='" + getEstado() + "'}";
    }
}

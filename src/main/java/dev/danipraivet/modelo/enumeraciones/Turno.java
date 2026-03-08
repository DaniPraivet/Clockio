package dev.danipraivet.modelo.enumeraciones;

// Turnos laborales. Coincide con el ENUM de las columnas 'turno_entrada' / 'turno_salida' en MySQL.
public enum Turno {

    MANANA("Mañana",  6, 13),
    TARDE ("Tarde",  14, 21),
    NOCHE ("Noche",  22,  5);

    private final String etiqueta;
    private final int    horaInicio;
    private final int    horaFin;

    Turno(String etiqueta, int horaInicio, int horaFin) {
        this.etiqueta   = etiqueta;
        this.horaInicio = horaInicio;
        this.horaFin    = horaFin;
    }

    public String getEtiqueta()   { return etiqueta;   }
    public int    getHoraInicio() { return horaInicio;  }
    public int    getHoraFin()    { return horaFin;     }

    // Detecta el turno segun la hora actual del sistema
    public static Turno detectar() {
        int hora = java.time.LocalTime.now().getHour();
        if (hora >= 6  && hora <= 13) return MANANA;
        if (hora >= 14 && hora <= 21) return TARDE;
        return NOCHE;
    }

    // Convierte el String de MySQL al enum
    public static Turno fromString(String valor) {
        if (valor == null) return null;
        return switch (valor) {
            case "Mañana" -> MANANA;
            case "Tarde"  -> TARDE;
            case "Noche"  -> NOCHE;
            default       -> null;
        };
    }

    @Override
    public String toString() { return etiqueta; }
}

package Sistema.Reservas.Aulas.Dto;

import Sistema.Reservas.Aulas.Model.Reserva;

import java.time.LocalDate;
import java.time.LocalTime;

/** Modelo de salida de Reserva: con identificador y los datos básicos de aula, usuario y curso. */
public record ReservaSalida(
        Long id,
        Long aulaId, String aulaNombre,
        Long usuarioId, String usuarioNombre,
        Long cursoId, String cursoNombre,
        LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
        String estado) {

    public static ReservaSalida desde(Reserva r) {
        return new ReservaSalida(
                r.getId(),
                r.getAula().getId(), r.getAula().getNombre(),
                r.getUsuario().getId(), r.getUsuario().getNombre(),
                r.getCurso() == null ? null : r.getCurso().getId(),
                r.getCurso() == null ? null : r.getCurso().getNombre(),
                r.getFecha(), r.getHoraInicio(), r.getHoraFin(),
                r.getEstado());
    }
}

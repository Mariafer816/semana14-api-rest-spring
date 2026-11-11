package Sistema.Reservas.Aulas.Dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Modelo de entrada de Reserva (POST y PUT): ids de las relaciones, sin identificador de la reserva. */
public record ReservaEntrada(
        @NotNull(message = "Debe indicar el aula (aulaId)")
        Long aulaId,

        @NotNull(message = "Debe indicar el usuario (usuarioId)")
        Long usuarioId,

        @NotNull(message = "Debe indicar el curso (cursoId)")
        Long cursoId,

        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha no puede ser anterior a hoy")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de finalización es obligatoria")
        LocalTime horaFin) {
}

package Sistema.Reservas.Aulas.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Modelo de entrada de Curso (POST y PUT): datos recibidos, sin identificador. */
public record CursoEntrada(
        @NotBlank(message = "El nombre del curso es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "Las horas son obligatorias")
        @Min(value = 1, message = "Las horas deben ser al menos 1")
        Integer horas) {
}

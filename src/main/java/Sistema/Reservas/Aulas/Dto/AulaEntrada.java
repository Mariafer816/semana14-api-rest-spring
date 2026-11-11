package Sistema.Reservas.Aulas.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Modelo de entrada de Aula (POST y PUT): datos recibidos, sin identificador. */
public record AulaEntrada(
        @NotBlank(message = "El nombre del aula es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String nombre,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        @Max(value = 500, message = "La capacidad no puede superar 500 personas")
        Integer capacidad,

        @NotBlank(message = "La ubicación es obligatoria")
        @Size(max = 100, message = "La ubicación no puede superar los 100 caracteres")
        String ubicacion,

        @NotBlank(message = "Debe seleccionar un tipo de aula")
        String tipo,

        /** Opcional: si no se envía, el aula queda disponible. */
        Boolean disponible) {
}

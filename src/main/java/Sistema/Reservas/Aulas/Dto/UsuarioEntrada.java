package Sistema.Reservas.Aulas.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Modelo de entrada de Usuario (POST y PUT): sin identificador y sin contraseña. */
public record UsuarioEntrada(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(regexp = ".+@.+\\..+", message = "El correo no tiene un formato válido (ej: nombre@dominio.com)")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
        String correo,

        @NotBlank(message = "El rol es obligatorio")
        String rol) {
}

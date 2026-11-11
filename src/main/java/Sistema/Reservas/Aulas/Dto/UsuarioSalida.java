package Sistema.Reservas.Aulas.Dto;

import Sistema.Reservas.Aulas.Model.Usuario;

/** Modelo de salida de Usuario: con identificador y nunca con la contraseña. */
public record UsuarioSalida(Long id, String nombre, String correo, String rol) {

    public static UsuarioSalida desde(Usuario u) {
        return new UsuarioSalida(u.getId(), u.getNombre(), u.getCorreo(), u.getRol());
    }
}

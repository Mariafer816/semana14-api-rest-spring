package Sistema.Reservas.Aulas.Dto;

import Sistema.Reservas.Aulas.Model.Aula;

/** Modelo de salida de Aula: el recurso con su identificador. */
public record AulaSalida(Long id, String nombre, Integer capacidad, String ubicacion, String tipo, boolean disponible) {

    public static AulaSalida desde(Aula a) {
        return new AulaSalida(a.getId(), a.getNombre(), a.getCapacidad(), a.getUbicacion(), a.getTipo(), a.isDisponible());
    }
}

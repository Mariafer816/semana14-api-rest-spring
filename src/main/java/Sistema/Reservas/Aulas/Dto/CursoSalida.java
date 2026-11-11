package Sistema.Reservas.Aulas.Dto;

import Sistema.Reservas.Aulas.Model.Curso;

/** Modelo de salida de Curso: el recurso con su identificador. */
public record CursoSalida(Long id, String nombre, int horas) {

    public static CursoSalida desde(Curso c) {
        return new CursoSalida(c.getId(), c.getNombre(), c.getHoras());
    }
}

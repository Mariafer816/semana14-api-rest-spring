package Sistema.Reservas.Aulas.Excepcion;

/** El recurso pedido por id no existe. La API la traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

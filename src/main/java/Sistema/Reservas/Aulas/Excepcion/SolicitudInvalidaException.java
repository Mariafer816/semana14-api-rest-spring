package Sistema.Reservas.Aulas.Excepcion;

import java.util.Map;

/** Los datos incumplen una regla de negocio o de validación. La API la traduce a HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {

    private final Map<String, String> errores;

    public SolicitudInvalidaException(String mensaje) {
        this(mensaje, null);
    }

    /** @param errores motivo por campo (puede ser null) */
    public SolicitudInvalidaException(String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}

package Sistema.Reservas.Aulas.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Modelo de salida de los errores de la API: { "estado": 400, "mensaje": "...", "errores": {...} } */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(int estado, String mensaje, Map<String, String> errores) {

    public static ErrorRespuesta de(int estado, String mensaje) {
        return new ErrorRespuesta(estado, mensaje, null);
    }
}

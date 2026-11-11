package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Dto.ErrorRespuesta;
import Sistema.Reservas.Aulas.Excepcion.RecursoNoEncontradoException;
import Sistema.Reservas.Aulas.Excepcion.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de la API a respuestas HTTP con cuerpo JSON:
 * 400 datos inválidos | 404 recurso inexistente | 500 error interno.
 * Solo aplica a los controladores REST, no a los controladores web (Thymeleaf).
 */
@RestControllerAdvice(assignableTypes = {
        AulaController.class, CursoController.class, UsuarioController.class, ReservaController.class})
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** 404: el servicio no encontró el recurso. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorRespuesta.de(404, ex.getMessage()));
    }

    /** 400: regla de negocio incumplida en el servicio. */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(SolicitudInvalidaException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(400, ex.getMessage(), ex.getErrores()));
    }

    /** 400: falla @Valid sobre el modelo de entrada (campo vacío, fuera de rango...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(400, "Los datos enviados no son válidos", errores));
    }

    /** 400: JSON mal formado, tipo de dato incorrecto, o id/fecha con formato incorrecto en la URL. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> formatoInvalido(Exception ex) {
        return ResponseEntity.badRequest().body(ErrorRespuesta.de(400,
                "Solicitud inválida: JSON mal formado o id/fecha con formato incorrecto"));
    }

    /** 400: respaldo ante una restricción de la base de datos (p. ej. correo duplicado). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body(ErrorRespuesta.de(400,
                "Los datos violan una restricción de la base de datos (valor duplicado o relacionado)"));
    }

    /** 500: cualquier error no previsto. El detalle se registra en el log, no se envía al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> errorInterno(Exception ex) {
        // Errores HTTP propios de Spring (405 método no permitido, 415 tipo no soportado...) conservan su código
        if (ex instanceof ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode())
                    .body(ErrorRespuesta.de(er.getStatusCode().value(), er.getBody().getDetail()));
        }
        log.error("Error interno en la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorRespuesta.de(500, "Error interno del servidor"));
    }
}

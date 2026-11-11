package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Dto.AulaEntrada;
import Sistema.Reservas.Aulas.Dto.AulaSalida;
import Sistema.Reservas.Aulas.Service.AulaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST de Aulas en /api/aulas. Solo maneja HTTP: la lógica y las validaciones están en AulaService
 * y los errores los traduce ApiExceptionHandler.
 *
 *   GET    /api/aulas        200
 *   GET    /api/aulas/{id}   200 | 404
 *   POST   /api/aulas        201 + Location | 400
 *   PUT    /api/aulas/{id}   200 | 400 | 404
 *   DELETE /api/aulas/{id}   204 | 400 | 404
 */
@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final AulaService aulaService;

    public AulaController(AulaService aulaService) {
        this.aulaService = aulaService;
    }

    @GetMapping
    public ResponseEntity<List<AulaSalida>> consultarTodas() {
        return ResponseEntity.ok(aulaService.consultarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AulaSalida> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(aulaService.consultarPorId(id));
    }

    @PostMapping
    public ResponseEntity<AulaSalida> crear(@Valid @RequestBody AulaEntrada entrada) {
        AulaSalida creada = aulaService.crear(entrada);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);   // 201 + cabecera Location
    }

    @PutMapping("/{id}")
    public ResponseEntity<AulaSalida> actualizar(@PathVariable Long id, @Valid @RequestBody AulaEntrada entrada) {
        return ResponseEntity.ok(aulaService.actualizar(id, entrada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        aulaService.eliminarPorId(id);
        return ResponseEntity.noContent().build();   // 204 sin contenido
    }
}

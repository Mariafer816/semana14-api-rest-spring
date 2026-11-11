package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Dto.CursoEntrada;
import Sistema.Reservas.Aulas.Dto.CursoSalida;
import Sistema.Reservas.Aulas.Service.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST de Cursos en /api/cursos.
 * GET 200 | GET id 200/404 | POST 201 + Location/400 | PUT 200/400/404 | DELETE 204/400/404.
 */
@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public ResponseEntity<List<CursoSalida>> consultarTodos() {
        return ResponseEntity.ok(cursoService.consultarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoSalida> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.consultarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CursoSalida> crear(@Valid @RequestBody CursoEntrada entrada) {
        CursoSalida creado = cursoService.crear(entrada);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoSalida> actualizar(@PathVariable Long id, @Valid @RequestBody CursoEntrada entrada) {
        return ResponseEntity.ok(cursoService.actualizar(id, entrada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}

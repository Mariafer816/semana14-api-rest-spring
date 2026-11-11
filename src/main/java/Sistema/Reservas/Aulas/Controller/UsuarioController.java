package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Dto.UsuarioEntrada;
import Sistema.Reservas.Aulas.Dto.UsuarioSalida;
import Sistema.Reservas.Aulas.Service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST de Usuarios en /api/usuarios. La contraseña nunca entra ni sale por la API.
 * GET 200 | GET id 200/404 | POST 201 + Location/400 | PUT 200/400/404 | DELETE 204/400/404.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioSalida>> consultarTodos() {
        return ResponseEntity.ok(usuarioService.consultarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioSalida> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.consultarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioSalida> crear(@Valid @RequestBody UsuarioEntrada entrada) {
        UsuarioSalida creado = usuarioService.crear(entrada);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioSalida> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioEntrada entrada) {
        return ResponseEntity.ok(usuarioService.actualizar(id, entrada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}

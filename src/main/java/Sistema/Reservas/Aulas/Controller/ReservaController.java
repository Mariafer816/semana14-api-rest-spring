package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Dto.ReservaEntrada;
import Sistema.Reservas.Aulas.Dto.ReservaSalida;
import Sistema.Reservas.Aulas.Service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * API REST de Reservas en /api/reservas.
 * GET 200 | GET id 200/404 | POST 201 + Location/400 | PUT 200/400/404 | DELETE 204/404.
 * Además: GET /fecha/{AAAA-MM-DD} y GET /aula/{aulaId}.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaSalida>> consultarTodas() {
        return ResponseEntity.ok(reservaService.consultarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaSalida> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.consultarPorId(id));
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<ReservaSalida>> consultarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reservaService.consultarPorFecha(fecha));
    }

    @GetMapping("/aula/{aulaId}")
    public ResponseEntity<List<ReservaSalida>> consultarPorAula(@PathVariable Long aulaId) {
        return ResponseEntity.ok(reservaService.consultarPorAula(aulaId));
    }

    @PostMapping
    public ResponseEntity<ReservaSalida> crear(@Valid @RequestBody ReservaEntrada entrada) {
        ReservaSalida creada = reservaService.crear(entrada);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaSalida> actualizar(@PathVariable Long id, @Valid @RequestBody ReservaEntrada entrada) {
        return ResponseEntity.ok(reservaService.actualizar(id, entrada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reservaService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
